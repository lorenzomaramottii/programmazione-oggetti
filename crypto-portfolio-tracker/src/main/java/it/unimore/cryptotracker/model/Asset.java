package it.unimore.cryptotracker.model;

public class Asset {
    private final String id;       // es. "bitcoin" (identificativo per l'API)
    private final String symbol;   // es. "BTC"
    private final double amount;   // Quantità posseduta (es. 0.05)
    private final double buyPrice; // Prezzo medio di acquisto in EUR
    private double currentPrice;   // Prezzo aggiornato in tempo reale dalle API

    public Asset(String id, String symbol, double amount, double buyPrice) {
        this.id = id;
        this.symbol = symbol.toUpperCase();
        this.amount = amount;
        this.buyPrice = buyPrice;
    }

    // --- BUSINESS LOGIC (Calcoli matematici fondamentali) ---

    // Calcola quanto è stato speso inizialmente per questo asset
    public double getTotalCost() {
        return amount * buyPrice;
    }

    // Calcola il valore attuale di mercato dell'asset
    public double getCurrentValue() {
        return amount * currentPrice;
    }

    // Calcola il guadagno o la perdita in EUR (Profit & Loss)
    public double getProfitLoss() {
        return getCurrentValue() - getTotalCost();
    }

    // Calcola il ritorno sull'investimento in percentuale (ROI)
    public double getPercentageROI() {
        if (getTotalCost() == 0) return 0.0;
        return (getProfitLoss() / getTotalCost()) * 100.0;
    }

    // --- GETTER E SETTER ---

    public String getId() { return id; }
    public String getSymbol() { return symbol; }
    public double getAmount() { return amount; }
    public double getBuyPrice() { return buyPrice; }
    public double getCurrentPrice() { return currentPrice; }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
}