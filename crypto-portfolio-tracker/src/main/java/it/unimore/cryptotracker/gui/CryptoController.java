package it.unimore.cryptotracker.gui;

import it.unimore.cryptotracker.database.AssetDAO;
import it.unimore.cryptotracker.model.Asset;
import it.unimore.cryptotracker.service.CoinGeckoClient;
import it.unimore.cryptotracker.service.PortfolioService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class CryptoController {
    private final PortfolioService portfolioService = new PortfolioService();
    private final CoinGeckoClient apiSimpleClient = new CoinGeckoClient();
    private final AssetDAO assetDao = new AssetDAO();

    private TableView<Asset> table = new TableView<>();
    private Label totalValueLabel = new Label("Valore Totale: 0.00 €");
    private Label globalRoiLabel = new Label("ROI Globale: 0.00 %");

    // Campi per inserimento portafoglio
    private TextField idInput = new TextField();
    private TextField symbolInput = new TextField();
    private TextField amountInput = new TextField();
    private TextField priceInput = new TextField();

    // Campi per la nuova area di consultazione prezzi live
    private TextField searchIdInput = new TextField();
    private Label searchResultLabel = new Label("Inserisci un ID e clicca Cerca");

    public TabPane getView() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // --- SCHEDA 1: PORTAFOGLIO ---
        Tab portfolioTab = new Tab("💼 Il Mio Portafoglio");
        VBox portfolioLayout = new VBox(20);
        portfolioLayout.setPadding(new Insets(20));
        portfolioLayout.getStyleClass().add("main-bg");

        // Configurazione Colonne Tabella
        TableColumn<Asset, String> symCol = new TableColumn<>("Simbolo");
        symCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        symCol.setPrefWidth(120);

        TableColumn<Asset, Double> amtCol = new TableColumn<>("Quantità");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setPrefWidth(150);

        TableColumn<Asset, Double> buyCol = new TableColumn<>("Prezzo d'Acquisto");
        buyCol.setCellValueFactory(new PropertyValueFactory<>("buyPrice"));
        buyCol.setPrefWidth(180);

        TableColumn<Asset, Double> currCol = new TableColumn<>("Prezzo Corrente");
        currCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        currCol.setPrefWidth(180);

        table.getColumns().addAll(symCol, amtCol, buyCol, currCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Form inserimento
        idInput.setPromptText("ID (es. bitcoin)");
        symbolInput.setPromptText("Simbolo (es. BTC)");
        amountInput.setPromptText("Quantità");
        priceInput.setPromptText("Prezzo (€)");

        Button addButton = new Button("Aggiungi Transazione");
        addButton.getStyleClass().add("button-success");
        addButton.setOnAction(e -> handleAddAsset());

        Button refreshButton = new Button("🔄 Aggiorna Prezzi Live");
        refreshButton.getStyleClass().add("button-primary");
        refreshButton.setOnAction(e -> refreshData());

        HBox inputForm = new HBox(12, idInput, symbolInput, amountInput, priceInput, addButton);
        inputForm.setAlignment(Pos.CENTER);

        totalValueLabel.getStyleClass().add("label-stats");
        globalRoiLabel.getStyleClass().add("label-stats");
        HBox statsBar = new HBox(40, totalValueLabel, globalRoiLabel, refreshButton);
        statsBar.setAlignment(Pos.CENTER_LEFT);

        portfolioLayout.getChildren().addAll(statsBar, table, inputForm);
        portfolioTab.setContent(portfolioLayout);

        // --- SCHEDA 2: CONSULTAZIONE PREZZI LIVE ---
        Tab searchTab = new Tab("🔍 Consultazione Prezzi");
        VBox searchLayout = new VBox(25);
        searchLayout.setPadding(new Insets(30));
        searchLayout.setAlignment(Pos.TOP_CENTER);
        searchLayout.getStyleClass().add("main-bg");

        Label searchTitle = new Label("Verifica Quotazioni in Tempo Reale");
        searchTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        searchIdInput.setPromptText("Inserisci ID Cripto (es. ethereum, ripple, cardano)");
        searchIdInput.setPrefWidth(300);
        searchIdInput.setMaxWidth(300);

        Button searchButton = new Button("🔍 Cerca Quotazione");
        searchButton.getStyleClass().add("button-primary");
        searchButton.setOnAction(e -> handleSearchPrice());

        searchResultLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #a0aec0; -fx-padding: 15;");
        searchResultLabel.getStyleClass().add("search-result-box");

        searchLayout.getChildren().addAll(searchTitle, searchIdInput, searchButton, searchResultLabel);
        searchTab.setContent(searchLayout);

        // Aggiunta delle schede al TabPane principale
        tabPane.getTabs().addAll(portfolioTab, searchTab);

        // Caricamento dati iniziale
        refreshData();

        return tabPane;
    }

    private void refreshData() {
        new Thread(() -> {
            try {
                List<Asset> updatedList = portfolioService.getUpdatedPortfolio();
                double totalValue = portfolioService.getPortfolioCurrentValue(updatedList);
                double globalRoi = portfolioService.getPortfolioGlobalROI(updatedList);

                Platform.runLater(() -> {
                    table.setItems(FXCollections.observableArrayList(updatedList));
                    totalValueLabel.setText(String.format("Valore Totale: %.2f €", totalValue));

                    if (globalRoi >= 0) {
                        globalRoiLabel.setText(String.format("ROI Globale: +%.2f %%", globalRoi));
                        globalRoiLabel.setStyle("-fx-text-fill: #48bb78;"); // Verde per ROI positivo
                    } else {
                        globalRoiLabel.setText(String.format("ROI Globale: %.2f %%", globalRoi));
                        globalRoiLabel.setStyle("-fx-text-fill: #f56565;"); // Rosso per ROI negativo
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> System.err.println("Errore API: " + e.getMessage()));
            }
        }).start();
    }

    private void handleAddAsset() {
        try {
            String id = idInput.getText().trim().toLowerCase();
            String symbol = symbolInput.getText().trim().toUpperCase();
            double amount = Double.parseDouble(amountInput.getText().trim());
            double price = Double.parseDouble(priceInput.getText().trim());

            if (!id.isEmpty() && !symbol.isEmpty() && amount > 0 && price > 0) {
                Asset newAsset = new Asset(id, symbol, amount, price);
                assetDao.saveOrUpdateAsset(newAsset);

                idInput.clear(); symbolInput.clear(); amountInput.clear(); priceInput.clear();
                refreshData();
            }
        } catch (NumberFormatException | SQLException ex) {
            System.err.println("Errore input: " + ex.getMessage());
        }
    }

    private void handleSearchPrice() {
        String searchId = searchIdInput.getText().trim().toLowerCase();
        if (searchId.isEmpty()) {
            searchResultLabel.setText("⚠️ Inserisci un ID valido prima di cercare.");
            return;
        }

        searchResultLabel.setText("⏳ Ricerca in corso sui server di CoinGecko...");

        new Thread(() -> {
            try {
                Map<String, Double> result = apiSimpleClient.getCurrentPrices(List.of(searchId));
                Platform.runLater(() -> {
                    if (result.containsKey(searchId)) {
                        double price = result.get(searchId);
                        searchResultLabel.setText(String.format("🎯 Criptovaluta: %s\n💰 Prezzo Attuale: %.2f €",
                                searchId.toUpperCase(), price));
                        searchResultLabel.setStyle("-fx-text-fill: #4fd1c5; -fx-font-weight: bold; -fx-font-size: 18px;");
                    } else {
                        searchResultLabel.setText("❌ Criptovaluta non trovata. Controlla che l'ID sia corretto (es. 'ethereum').");
                        searchResultLabel.setStyle("-fx-text-fill: #f56565;");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    searchResultLabel.setText("⚠️ Errore di rete o CoinGecko offline.");
                    searchResultLabel.setStyle("-fx-text-fill: #f56565;");
                });
            }
        }).start();
    }
}