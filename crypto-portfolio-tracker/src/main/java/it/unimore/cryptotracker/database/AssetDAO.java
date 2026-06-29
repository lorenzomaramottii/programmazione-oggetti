package it.unimore.cryptotracker.database;

import it.unimore.cryptotracker.model.Asset;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssetDAO {

    // Recupera tutte le crypto salvate nel database locale
    public List<Asset> getAllAssets() throws SQLException {
        List<Asset> assets = new ArrayList<>();
        String sql = "SELECT id, symbol, amount, buy_price FROM assets";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                assets.add(new Asset(
                        rs.getString("id"),
                        rs.getString("symbol"),
                        rs.getDouble("amount"),
                        rs.getDouble("buy_price")
                ));
            }
        }
        return assets;
    }

    // Inserisce un nuovo asset o aggiorna quantità e prezzo medio se esiste già (Transazionale)
    public void saveOrUpdateAsset(Asset asset) throws SQLException {
        String checkSql = "SELECT amount, buy_price FROM assets WHERE id = ?";
        String insertSql = "INSERT INTO assets (id, symbol, name, amount, buy_price) VALUES (?, ?, ?, ?, ?)";
        String updateSql = "UPDATE assets SET amount = ?, buy_price = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Avvia una transazione per garantire la consistenza dei dati

            try {
                double currentAmount = 0;
                double currentBuyPrice = 0;
                boolean exists = false;

                // 1. Controlla se l'asset è già presente
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, asset.getId());
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            exists = true;
                            currentAmount = rs.getDouble("amount");
                            currentBuyPrice = rs.getDouble("buy_price");
                        }
                    }
                }

                if (exists) {
                    // 2. Se esiste, calcola la nuova quantità totale e il prezzo medio di carico ponderato
                    double newAmount = currentAmount + asset.getAmount();
                    double newBuyPrice = ((currentAmount * currentBuyPrice) + (asset.getAmount() * asset.getBuyPrice())) / newAmount;

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setDouble(1, newAmount);
                        updateStmt.setDouble(2, newBuyPrice);
                        updateStmt.setString(3, asset.getId());
                        updateStmt.executeUpdate();
                    }
                } else {
                    // 3. Se non esiste, fai un normale inserimento
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, asset.getId());
                        insertStmt.setString(2, asset.getSymbol());
                        insertStmt.setString(3, asset.getSymbol()); // Usiamo il simbolo anche come nome per semplicità
                        insertStmt.setDouble(4, asset.getAmount());
                        insertStmt.setDouble(5, asset.getBuyPrice());
                        insertStmt.executeUpdate();
                    }
                }
                conn.commit(); // Conferma i cambiamenti sul file
            } catch (SQLException e) {
                conn.rollback(); // Se qualcosa fallisce, annulla tutto per evitare dati corrotti
                throw e;
            }
        }
    }

    // Rimuove completamente una criptovaluta dal portafoglio
    public void deleteAsset(String id) throws SQLException {
        String sql = "DELETE FROM assets WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        }
    }
}