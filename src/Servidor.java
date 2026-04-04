import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Ponto de entrada do Servidor da Corretora.
 *
 * - Registra o objeto remoto no RMI Registry (porta 1099)
 * - Busca preços reais de BTC, ETH e SOL via API do CoinGecko ao iniciar
 * - Atualiza preços de cripto automaticamente a cada 30 segundos
 *   (dispara callbacks para todos os clientes inscritos)
 */
public class Servidor {

    private static final int    PORTA           = 1099;
    private static final String NOME_SERVICO    = "CorretorService";

    /** Intervalo de atualização automática dos preços de cripto (segundos) */
    private static final int INTERVALO_ATUALIZACAO_S = 30;

    public static void main(String[] args) {
        try {
            CorretorImpl corretora = new CorretorImpl();
            CoinGeckoService coinGecko = new CoinGeckoService();

            // Cria o RMI Registry na porta definida
            Registry registry = LocateRegistry.createRegistry(PORTA);
            registry.rebind(NOME_SERVICO, corretora);

            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║   Corretora Distribuída — Servidor RMI   ║");
            System.out.println("╚══════════════════════════════════════════╝");
            System.out.println("Serviço registrado em: rmi://localhost:" + PORTA + "/" + NOME_SERVICO);
            System.out.println();

            // ── Cadastra ações com preços reais de cripto ──────────────────
            System.out.println("=== Cadastrando ações ===");
            System.out.println("[CoinGecko] Buscando preços em tempo real...");

            Map<String, Double> precosCripto = coinGecko.buscarPrecos("BTC", "ETH", "SOL");

            double precoBtc = precosCripto.getOrDefault("BTC", 350_000.00);
            double precoEth = precosCripto.getOrDefault("ETH",  18_000.00);
            double precoSol = precosCripto.getOrDefault("SOL",     900.00);

            corretora.cadastrarAcao("BTC",   precoBtc);
            corretora.cadastrarAcao("ETH",   precoEth);
            corretora.cadastrarAcao("SOL",   precoSol);
            corretora.cadastrarAcao("PETR4",  38.75);   // ações BR: preço fixo
            corretora.cadastrarAcao("VALE3",  62.10);

            System.out.println("[CoinGecko] Preços obtidos com sucesso!");
            System.out.println();

            // ── Atualização automática periódica de cripto ─────────────────
            ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "atualizador-precos");
                t.setDaemon(true); // Não impede o servidor de encerrar
                return t;
            });

            agendador.scheduleAtFixedRate(() -> {
                System.out.println("[Atualizador] Buscando novos preços no CoinGecko...");
                Map<String, Double> novosPrecos = coinGecko.buscarPrecos("BTC", "ETH", "SOL");

                novosPrecos.forEach((simbolo, preco) -> {
                    try {
                        corretora.atualizarPreco(simbolo, preco);
                        System.out.printf("[Atualizador] %s → R$ %.2f%n", simbolo, preco);
                    } catch (RemoteException e) {
                        System.err.println("[Atualizador] Erro ao atualizar " + simbolo + ": " + e.getMessage());
                    }
                });
            }, INTERVALO_ATUALIZACAO_S, INTERVALO_ATUALIZACAO_S, TimeUnit.SECONDS);

            System.out.printf("Atualização automática de cripto a cada %ds via CoinGecko.%n",
                    INTERVALO_ATUALIZACAO_S);
            System.out.println("Servidor pronto. Aguardando conexões...");
            System.out.println("(Pressione Ctrl+C para encerrar)");

        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao iniciar o servidor:");
            e.printStackTrace();
        }
    }
}
