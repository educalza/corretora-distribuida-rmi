import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Ponto de entrada do Servidor da Corretora.
 *
 * - Registra o serviço no RMI Registry (porta 1099)
 * - Pré-cadastra ativos com preços reais de cripto via CoinGecko
 * - Atualiza preços de cripto automaticamente a cada 30 segundos
 */
public class Servidor {

    private static final int    PORTA        = 1099;
    private static final String NOME_SERVICO = "CorretorService";

    public static void main(String[] args) {
        try {
            CorretorImpl corretora = new CorretorImpl();
            CoinGeckoService coinGecko = new CoinGeckoService();

            Registry registry = LocateRegistry.createRegistry(PORTA);
            registry.rebind(NOME_SERVICO, corretora);

            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║   Corretora Distribuída — Servidor RMI   ║");
            System.out.println("╚══════════════════════════════════════════╝");
            System.out.println("Serviço: rmi://localhost:" + PORTA + "/" + NOME_SERVICO);
            System.out.println();

            // Busca preços reais de cripto ao iniciar
            System.out.println("[CoinGecko] Buscando preços em tempo real...");
            Map<String, Double> precosCripto = coinGecko.buscarPrecos("BTC", "ETH", "SOL");

            corretora.adicionarAtivo("BTC",   precosCripto.getOrDefault("BTC", 350_000.0));
            corretora.adicionarAtivo("ETH",   precosCripto.getOrDefault("ETH",  18_000.0));
            corretora.adicionarAtivo("SOL",   precosCripto.getOrDefault("SOL",     900.0));
            corretora.adicionarAtivo("PETR4", 38.75);
            corretora.adicionarAtivo("VALE3", 62.10);
            System.out.println();

            // Atualiza preços de cripto a cada 30 segundos (notifica observadores automaticamente)
            ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "atualizador");
                t.setDaemon(true);
                return t;
            });

            agendador.scheduleAtFixedRate(() -> {
                System.out.println("[Atualizador] Atualizando preços via CoinGecko...");
                Map<String, Double> novos = coinGecko.buscarPrecos("BTC", "ETH", "SOL");
                novos.forEach((nome, preco) -> {
                    try {
                        corretora.setValor(nome, preco);
                    } catch (Exception e) {
                        System.err.println("[Atualizador] Erro: " + e.getMessage());
                    }
                });
            }, 30, 30, TimeUnit.SECONDS);

            System.out.println("Servidor pronto. Aguardando conexões...");

        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao iniciar o servidor:");
            e.printStackTrace();
        }
    }
}
