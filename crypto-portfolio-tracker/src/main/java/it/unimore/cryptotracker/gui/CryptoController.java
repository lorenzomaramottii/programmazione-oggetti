package it.unimore.cryptotracker.gui;

import it.unimore.cryptotracker.database.AssetDAO;
import it.unimore.cryptotracker.model.Asset;
import it.unimore.cryptotracker.service.CoinGeckoClient;
import it.unimore.cryptotracker.service.PortfolioService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CryptoController {
    private final PortfolioService portfolioService = new PortfolioService();
    private final CoinGeckoClient apiSimpleClient = new CoinGeckoClient();
    private final AssetDAO assetDao = new AssetDAO();

    private TableView<Asset> table = new TableView<>();
    private Label totalValueLabel = new Label("Valore Totale: 0.00 €");
    private Label globalRoiLabel = new Label("ROI Globale: 0.00 %");

    private TextField idInput = new TextField();
    private TextField symbolInput = new TextField();
    private TextField amountInput = new TextField();
    private TextField priceInput = new TextField();

    private TextField searchIdInput = new TextField();
    private Label searchResultLabel = new Label("Sposta il mouse sulla linea del grafico per i dettagli storici.");
    private LineChart<String, Number> lineChart;
    private XYChart.Series<String, Number> chartSeries;

    private ContextMenu autocompleteMenu = new ContextMenu();
    private Popup graphPopup = new Popup();
    private Label popupLabel = new Label();

    private final List<String> popularCoins = Arrays.asList(
            "bitcoin", "ethereum", "tether", "binancecoin", "solana", "ripple", "cardano", "dogecoin",
            "shiba-inu", "avalanche-2", "polkadot", "chainlink", "tron", "near", "polygon", "litecoin",
            "uniswap", "stellar", "monero", "aptos", "cosmos", "filecoin", "fantom", "vechain", "aave"
    );

    public TabPane getView() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        popupLabel.getStyleClass().add("custom-graph-popup");
        graphPopup.getContent().add(popupLabel);

        // --- TAB 1: PORTAFOGLIO ---
        Tab portfolioTab = new Tab("💼 Il Mio Portafoglio");
        VBox portfolioLayout = new VBox(20);
        portfolioLayout.setPadding(new Insets(20));
        portfolioLayout.getStyleClass().add("main-bg");

        TableColumn<Asset, String> symCol = new TableColumn<>("Simbolo");
        symCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        TableColumn<Asset, Double> amtCol = new TableColumn<>("Quantità");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Asset, Double> buyCol = new TableColumn<>("Prezzo d'Acquisto");
        buyCol.setCellValueFactory(new PropertyValueFactory<>("buyPrice"));
        TableColumn<Asset, Double> currCol = new TableColumn<>("Prezzo Corrente");
        currCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        table.getColumns().addAll(symCol, amtCol, buyCol, currCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

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

        totalValueLabel.getStyleClass().add("label-top-stats");
        globalRoiLabel.getStyleClass().add("label-top-stats");
        HBox statsBar = new HBox(40, totalValueLabel, globalRoiLabel, refreshButton);
        statsBar.setAlignment(Pos.CENTER_LEFT);

        portfolioLayout.getChildren().addAll(statsBar, table, inputForm);
        portfolioTab.setContent(portfolioLayout);

        // --- TAB 2: ANALISI E GRAFICI ---
        Tab searchTab = new Tab("📈 Analisi e Grafici");
        VBox searchLayout = new VBox(15);
        searchLayout.setPadding(new Insets(20));
        searchLayout.getStyleClass().add("main-bg");

        searchIdInput.setPromptText("Cerca una moneta (es. bitcoin)...");
        searchIdInput.setPrefWidth(300);
        setupAutocomplete();

        HBox searchForm = new HBox(10, searchIdInput);
        searchForm.setAlignment(Pos.CENTER_LEFT);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Cronologia Temporale (30 Giorni)");
        xAxis.setTickLabelsVisible(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Prezzo (€)");
        yAxis.setForceZeroInRange(false);

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Andamento di Mercato");

        // RIMOZIONE DELLA LEGENDA IN BASSO (QUELLA CHE COPRIVI COL REQUADRO BIANCO)
        lineChart.setLegendVisible(false);

        lineChart.setCreateSymbols(true);
        VBox.setVgrow(lineChart, Priority.ALWAYS);

        chartSeries = new XYChart.Series<>();
        chartSeries.setName("Quotazione EUR");
        lineChart.getData().add(chartSeries);

        searchResultLabel.getStyleClass().add("label-bottom-stats");

        searchLayout.getChildren().addAll(searchForm, searchResultLabel, lineChart);
        searchTab.setContent(searchLayout);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> graphPopup.hide());
        tabPane.getTabs().addAll(portfolioTab, searchTab);
        refreshData();

        return tabPane;
    }

    private void agganciaListenerPunti() {
        for (XYChart.Data<String, Number> data : chartSeries.getData()) {
            Node node = data.getNode();
            if (node != null) {
                node.setOnMouseEntered(e -> {
                    String dateStr = data.getXValue();
                    double priceVal = data.getYValue().doubleValue();

                    popupLabel.setText(String.format("%s\nEUR %.2f", dateStr, priceVal));
                    lineChart.setCursor(Cursor.CROSSHAIR);

                    if (!graphPopup.isShowing()) {
                        graphPopup.show(lineChart.getScene().getWindow());
                    }
                    graphPopup.setX(e.getScreenX() + 12);
                    graphPopup.setY(e.getScreenY() - 45);
                    searchResultLabel.setText(String.format("📊 Prezzo selezionato: %.2f €", priceVal));
                });

                node.setOnMouseExited(e -> {
                    graphPopup.hide();
                    lineChart.setCursor(Cursor.DEFAULT);
                });
            }
        }
    }

    private void setupAutocomplete() {
        searchIdInput.textProperty().addListener((observable, oldValue, newValue) -> {
            String enteredText = newValue.trim().toLowerCase();
            if (enteredText.isEmpty()) {
                autocompleteMenu.hide();
                return;
            }

            List<MenuItem> filteredItems = popularCoins.stream()
                    .filter(id -> id.startsWith(enteredText))
                    .limit(8)
                    .map(id -> {
                        MenuItem item = new MenuItem(id);
                        item.setOnAction(e -> {
                            searchIdInput.setText(id);
                            autocompleteMenu.hide();
                            updateChart();
                        });
                        return item;
                    })
                    .collect(Collectors.toList());

            if (!filteredItems.isEmpty()) {
                autocompleteMenu.getItems().setAll(filteredItems);
                if (!autocompleteMenu.isShowing()) {
                    autocompleteMenu.show(searchIdInput, javafx.geometry.Side.BOTTOM, 0, 0);
                }
            } else {
                autocompleteMenu.hide();
            }
        });
    }

    private void updateChart() {
        String coinId = searchIdInput.getText().trim().toLowerCase();
        if (coinId.isEmpty()) return;

        searchResultLabel.setText("⏳ Caricamento dati in corso...");
        graphPopup.hide();

        new Thread(() -> {
            try {
                Map<String, Double> current = apiSimpleClient.getCurrentPrices(List.of(coinId));
                List<double[]> history = apiSimpleClient.getHistoricalPrices(coinId, 30);

                Platform.runLater(() -> {
                    if (current.containsKey(coinId)) {
                        chartSeries.getData().clear();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

                        for (int i = 0; i < history.size(); i++) {
                            double timestamp = history.get(i)[0];
                            double priceValue = history.get(i)[1];
                            String formattedDate = sdf.format(new Date((long) timestamp));

                            chartSeries.getData().add(new XYChart.Data<>(formattedDate, priceValue));
                        }
                        searchResultLabel.setText(String.format("🎯 %s: %.2f € (Prezzo Corrente)", coinId.toUpperCase(), current.get(coinId)));
                        agganciaListenerPunti();
                    } else {
                        searchResultLabel.setText("❌ Criptovaluta non trovata.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> searchResultLabel.setText("❌ Errore di rete con le API."));
            }
        }).start();
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
                        globalRoiLabel.setStyle("-fx-text-fill: #48bb78 !important;");
                    } else {
                        globalRoiLabel.setText(String.format("ROI Globale: %.2f %%", globalRoi));
                        globalRoiLabel.setStyle("-fx-text-fill: #f56565 !important;");
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void handleAddAsset() {
        String id = idInput.getText().trim().toLowerCase();
        String symbol = symbolInput.getText().trim().toUpperCase();
        String amountStr = amountInput.getText().trim();
        String priceStr = priceInput.getText().trim();

        if (id.isEmpty() || symbol.isEmpty() || amountStr.isEmpty() || priceStr.isEmpty()) return;

        new Thread(() -> {
            try {
                Map<String, Double> checkCoin = apiSimpleClient.getCurrentPrices(List.of(id));
                Platform.runLater(() -> {
                    if (checkCoin != null && checkCoin.containsKey(id)) {
                        try {
                            double amount = Double.parseDouble(amountStr);
                            double price = Double.parseDouble(priceStr);
                            assetDao.saveOrUpdateAsset(new Asset(id, symbol, amount, price));
                            idInput.clear(); symbolInput.clear(); amountInput.clear(); priceInput.clear();
                            refreshData();
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }
}