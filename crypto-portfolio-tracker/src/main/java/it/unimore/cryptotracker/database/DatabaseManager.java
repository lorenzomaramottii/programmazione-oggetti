package it.unimore.cryptotracker.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // Il file del database SQLite verrà creato direttamente nella cartella principale del progetto
    private static final String URL = "jdbc:sqlite:portfolio.db";

    // Metodo per ottenere la connessione al database (Stile JDBC usato dal prof)
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // Crea la tabella degli asset se è la prima volta che si avvia l'applicazione
    public static void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS assets (" +
                "id TEXT PRIMARY KEY, " +
                "symbol TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "buy_price REAL NOT NULL" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database inizializzato correttamente.");
        } catch (SQLException e) {
            System.err.println("Errore di inizializzazione del database: " + e.getMessage());
        }
    }
}