package it.unimore.cryptotracker.service;

import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinGeckoClient {
    private final HttpClient client;

    public CoinGeckoClient() {
        // Inizializza il client HTTP nativo di Java
        this.client = HttpClient.newBuilder().build();
    }

    /**
     * Riceve una lista di ID (es. ["bitcoin", "ethereum"]) e restituisce una mappa
     * contenente il prezzo corrente in EUR per ciascun ID.
     */
    public Map<String, Double> getCurrentPrices(List<String> coinIds) throws Exception {
        Map<String, Double> pricesMap = new HashMap<>();

        // Se il portafoglio è vuoto, evita di fare una chiamata API inutile
        if (coinIds.isEmpty()) {
            return pricesMap;
        }

        // Unisce la lista di ID separandoli con una virgola (es. "bitcoin,ethereum")
        String idsParam = String.join(",", coinIds);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + idsParam + "&vs_currencies=eur";

        // Costruisce la richiesta HTTP GET ricalcando i pattern del corso
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        // Invia la richiesta in modo sincrono
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Se l'API risponde con un codice diverso da 200 (OK), lancia un'eccezione gestita
        if (response.statusCode() != 200) {
            throw new RuntimeException("Impossibile recuperare i dati di mercato. HTTP code: " + response.statusCode());
        }

        // Logica di parsing del JSON
        JSONObject json = new JSONObject(response.body());
        for (String id : coinIds) {
            if (json.has(id)) {
                JSONObject coinJson = json.getJSONObject(id);
                double price = coinJson.optDouble("eur", 0.0);
                pricesMap.put(id, price);
            }
        }

        return pricesMap;
    }
}