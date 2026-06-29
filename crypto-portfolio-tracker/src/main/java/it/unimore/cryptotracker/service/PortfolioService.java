package it.unimore.cryptotracker.service;

import it.unimore.cryptotracker.database.AssetDAO;
import it.unimore.cryptotracker.model.Asset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PortfolioService {

    private final AssetDAO assetDao = new AssetDAO();
    private final CoinGeckoClient apiClient = new CoinGeckoClient();

    /**
     * Recupera il portafoglio aggiornando i prezzi correnti con UNA SOLA chiamata HTTP cumulativa.
     */
    public List<Asset> getUpdatedPortfolio() throws Exception {
        List<Asset> localAssets = assetDao.getAllAssets();
        if (localAssets.isEmpty()) {
            return localAssets;
        }

        // Estrae tutti gli ID unici dal database locale
        List<String> coinIds = new ArrayList<>();
        for (Asset asset : localAssets) {
            if (!coinIds.contains(asset.getId())) {
                coinIds.add(asset.getId());
            }
        }

        try {
            // Unica chiamata di gruppo per tutte le monete presenti nel portafoglio
            Map<String, Double> currentPrices = apiClient.getCurrentPrices(coinIds);

            // Aggiorna i prezzi correnti degli oggetti in memoria
            for (Asset asset : localAssets) {
                if (currentPrices.containsKey(asset.getId())) {
                    asset.setCurrentPrice(currentPrices.get(asset.getId()));
                }
            }
        } catch (Exception e) {
            System.err.println("Impossibile aggiornare i prezzi live del portafoglio (Rate Limit). Uso i prezzi in cache.");
        }

        return localAssets;
    }

    public double getPortfolioCurrentValue(List<Asset> assets) {
        double total = 0;
        for (Asset a : assets) {
            total += a.getAmount() * a.getCurrentPrice();
        }
        return total;
    }

    public double getPortfolioGlobalROI(List<Asset> assets) {
        double totalCost = 0;
        double currentValue = 0;

        for (Asset a : assets) {
            totalCost += a.getAmount() * a.getBuyPrice();
            currentValue += a.getAmount() * a.getCurrentPrice();
        }

        if (totalCost == 0) return 0;
        return ((currentValue - totalCost) / totalCost) * 100.0;
    }
}