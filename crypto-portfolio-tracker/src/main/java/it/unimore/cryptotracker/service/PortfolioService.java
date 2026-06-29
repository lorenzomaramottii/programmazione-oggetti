package it.unimore.cryptotracker.service;

import it.unimore.cryptotracker.database.AssetDAO;
import it.unimore.cryptotracker.model.Asset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PortfolioService {
    private final AssetDAO assetDao;
    private final CoinGeckoClient apiSimpleClient;

    public PortfolioService() {
        this.assetDao = new AssetDAO();
        this.apiSimpleClient = new CoinGeckoClient();
    }

    /**
     * Recupera tutti gli asset dal database e ne aggiorna il prezzo corrente
     * facendo una singola chiamata cumulativa alle API di CoinGecko.
     */
    public List<Asset> getUpdatedPortfolio() throws Exception {
        List<Asset> assets = assetDao.getAllAssets();

        // Estrae solo gli ID delle crypto presenti nel portafoglio per la chiamata REST
        List<String> ids = new ArrayList<>();
        for (Asset a : assets) {
            ids.add(a.getId());
        }

        // Recupera i prezzi in tempo reale tramite HTTP/JSON
        Map<String, Double> livePrices = apiSimpleClient.getCurrentPrices(ids);

        // Aggiorna ogni asset con il rispettivo prezzo corrente di mercato
        for (Asset a : assets) {
            if (livePrices.containsKey(a.getId())) {
                a.setCurrentPrice(livePrices.get(a.getId()));
            }
        }
        return assets;
    }

    // --- CALCOLI STATISTICI GLOBALI DEL PORTAFOGLIO ---

    public double getPortfolioTotalCost(List<Asset> assets) {
        double total = 0;
        for (Asset a : assets) {
            total += a.getTotalCost();
        }
        return total;
    }

    public double getPortfolioCurrentValue(List<Asset> assets) {
        double total = 0;
        for (Asset a : assets) {
            total += a.getCurrentValue();
        }
        return total;
    }

    public double getPortfolioTotalProfitLoss(List<Asset> assets) {
        return getPortfolioCurrentValue(assets) - getPortfolioTotalCost(assets);
    }

    public double getPortfolioGlobalROI(List<Asset> assets) {
        double totalCost = getPortfolioTotalCost(assets);
        if (totalCost == 0) return 0.0;
        return (getPortfolioTotalProfitLoss(assets) / totalCost) * 100.0;
    }
}