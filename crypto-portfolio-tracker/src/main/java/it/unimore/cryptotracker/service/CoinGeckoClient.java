package it.unimore.cryptotracker.service;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinGeckoClient {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public Map<String, Double> getCurrentPrices(List<String> coinIds) throws Exception {
        Map<String, Double> prices = new HashMap<>();
        if (coinIds.isEmpty()) return prices;

        String idsParam = String.join(",", coinIds);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + idsParam + "&vs_currencies=eur";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            for (String id : coinIds) {
                if (json.has(id)) {
                    prices.put(id, json.getJSONObject(id).getDouble("eur"));
                }
            }
        } else if (response.statusCode() == 429) {
            throw new RuntimeException("429");
        }
        return prices;
    }

    public List<double[]> getHistoricalPrices(String coinId, int days) throws Exception {
        List<double[]> history = new ArrayList<>();
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId + "/market_chart?vs_currency=eur&days=" + days;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            if (json.has("prices")) {
                JSONArray pricesArray = json.getJSONArray("prices");
                for (int i = 0; i < pricesArray.length(); i++) {
                    JSONArray point = pricesArray.getJSONArray(i);
                    history.add(new double[]{ point.getDouble(0), point.getDouble(1) });
                }
            }
        } else if (response.statusCode() == 429) {
            throw new RuntimeException("429");
        } else {
            throw new RuntimeException("NotFound");
        }
        return history;
    }
}