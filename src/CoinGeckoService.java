import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço que busca preços de criptomoedas em tempo real via API pública do CoinGecko.
 *
 * Endpoint: https://api.coingecko.com/api/v3/simple/price
 * Sem necessidade de chave de API (free tier).
 * Preços retornados em BRL (Real Brasileiro).
 */
public class CoinGeckoService {

    private static final String BASE_URL =
            "https://api.coingecko.com/api/v3/simple/price?vs_currencies=brl&ids=";

    // Mapeamento: símbolo usado no sistema → ID do CoinGecko
    private static final Map<String, String> SIMBOLO_PARA_ID = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "SOL", "solana"
    );

    private final HttpClient httpClient;

    public CoinGeckoService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Busca o preço atual em BRL de um conjunto de símbolos.
     *
     * @param simbolos Array de símbolos (ex: "BTC", "ETH", "SOL")
     * @return Mapa de símbolo → preço em BRL; símbolos não encontrados são omitidos
     */
    public Map<String, Double> buscarPrecos(String... simbolos) {
        Map<String, Double> resultado = new HashMap<>();

        // Monta a lista de IDs para a query string
        StringBuilder ids = new StringBuilder();
        Map<String, String> idParaSimbolo = new HashMap<>();

        for (String simbolo : simbolos) {
            String id = SIMBOLO_PARA_ID.get(simbolo.toUpperCase());
            if (id != null) {
                if (!ids.isEmpty()) ids.append(",");
                ids.append(id);
                idParaSimbolo.put(id, simbolo.toUpperCase());
            }
        }

        if (ids.isEmpty()) return resultado;

        String url = BASE_URL + ids;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                parseResposta(response.body(), idParaSimbolo, resultado);
            } else {
                System.err.printf("[CoinGecko] Erro HTTP %d ao buscar preços.%n", response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("[CoinGecko] Falha na requisição: " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Parse simples do JSON retornado pelo CoinGecko.
     *
     * Exemplo de resposta:
     *   {"bitcoin":{"brl":350000.0},"solana":{"brl":920.5},"ethereum":{"brl":18200.0}}
     *
     * Usa parsing manual para evitar dependência de biblioteca externa.
     */
    private void parseResposta(String json, Map<String, String> idParaSimbolo,
                                Map<String, Double> resultado) {
        // Remove espaços e chaves externas
        json = json.trim();

        for (Map.Entry<String, String> entry : idParaSimbolo.entrySet()) {
            String id = entry.getKey();
            String simbolo = entry.getValue();

            // Procura: "bitcoin":{"brl":VALOR}
            String chave = "\"" + id + "\":{\"brl\":";
            int idx = json.indexOf(chave);
            if (idx == -1) continue;

            int inicio = idx + chave.length();
            int fim = json.indexOf("}", inicio);
            if (fim == -1) continue;

            String valorStr = json.substring(inicio, fim).trim();
            try {
                double preco = Double.parseDouble(valorStr);
                resultado.put(simbolo, preco);
            } catch (NumberFormatException e) {
                System.err.printf("[CoinGecko] Não foi possível converter preço de %s: '%s'%n",
                        simbolo, valorStr);
            }
        }
    }
}
