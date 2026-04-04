import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Scanner;

/**
 * Cliente da Corretora Distribuída.
 *
 * Funcionalidades:
 *  - Conecta ao servidor via RMI com reconexão automática (tolerância a falhas básica)
 *  - Implementa ClienteCallback para receber notificações de mudança de preço em tempo real
 *  - Menu interativo no terminal para todas as operações disponíveis
 */
public class Cliente extends UnicastRemoteObject implements ClienteCallback {

    private static final String HOST          = "localhost";
    private static final int    PORTA         = 1099;
    private static final String NOME_SERVICO  = "CorretorService";
    private static final int    MAX_TENTATIVAS = 5;
    private static final long   ESPERA_MS      = 3000; // 3 segundos entre tentativas

    // Referência ao stub remoto do servidor
    private transient CorretorInterface corretora;

    // Scanner compartilhado para leitura do terminal
    private final transient Scanner scanner = new Scanner(System.in);

    // -----------------------------------------------------------------------
    // Construtor — exporta este objeto como callback remoto
    // -----------------------------------------------------------------------

    protected Cliente() throws RemoteException {
        super();
    }

    // -----------------------------------------------------------------------
    // Implementação do callback (chamado pelo servidor)
    // -----------------------------------------------------------------------

    @Override
    public void notificarMudancaPreco(String simbolo, double precoAntigo, double precoNovo)
            throws RemoteException {
        double variacao = precoNovo - precoAntigo;
        String seta = variacao >= 0 ? "▲" : "▼";
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────┐");
        System.out.printf ("│ 🔔 ALERTA: %-6s  R$ %.2f → R$ %.2f  %s %.2f │%n",
                simbolo, precoAntigo, precoNovo, seta, Math.abs(variacao));
        System.out.println("└─────────────────────────────────────────────┘");
        System.out.print("Opção: "); // Reaparece o prompt após o alerta
    }

    // -----------------------------------------------------------------------
    // Conexão com reconexão automática
    // -----------------------------------------------------------------------

    private boolean conectar() {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                Registry registry = LocateRegistry.getRegistry(HOST, PORTA);
                corretora = (CorretorInterface) registry.lookup(NOME_SERVICO);
                System.out.println("[OK] Conectado ao servidor " + HOST + ":" + PORTA);
                return true;
            } catch (Exception e) {
                System.out.printf("[Tentativa %d/%d] Servidor indisponível. Aguardando %ds...%n",
                        tentativa, MAX_TENTATIVAS, ESPERA_MS / 1000);
                try {
                    Thread.sleep(ESPERA_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        System.err.println("[ERRO] Não foi possível conectar ao servidor após " + MAX_TENTATIVAS + " tentativas.");
        return false;
    }

    /**
     * Tenta reconectar em caso de perda de conexão durante uma operação.
     */
    private boolean reconectar() {
        System.out.println("[AVISO] Conexão perdida. Tentando reconectar...");
        return conectar();
    }

    // -----------------------------------------------------------------------
    // Operações do menu
    // -----------------------------------------------------------------------

    private void listarAcoes() {
        try {
            Map<String, Double> acoes = corretora.listarAcoes();
            System.out.println();
            System.out.println("┌────────────────────────────────┐");
            System.out.println("│       AÇÕES DISPONÍVEIS        │");
            System.out.println("├──────────┬─────────────────────┤");
            System.out.println("│ Símbolo  │   Preço Atual       │");
            System.out.println("├──────────┼─────────────────────┤");
            acoes.entrySet().stream()
                 .sorted(Map.Entry.comparingByKey())
                 .forEach(e -> System.out.printf("│ %-8s │  R$ %12.2f    │%n", e.getKey(), e.getValue()));
            System.out.println("└──────────┴─────────────────────┘");
        } catch (RemoteException e) {
            System.err.println("[ERRO] Falha ao listar ações: " + e.getMessage());
            reconectar();
        }
    }

    private void consultarPreco() {
        System.out.print("Símbolo da ação: ");
        String simbolo = scanner.nextLine().trim();
        try {
            double preco = corretora.consultarPreco(simbolo);
            System.out.printf("Preço de %-6s: R$ %.2f%n", simbolo.toUpperCase(), preco);
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
            if (e.getCause() != null) reconectar();
        }
    }

    private void atualizarPreco() {
        System.out.print("Símbolo da ação: ");
        String simbolo = scanner.nextLine().trim();
        System.out.print("Novo preço (R$): ");
        try {
            double novoPreco = Double.parseDouble(scanner.nextLine().trim().replace(",", "."));
            corretora.atualizarPreco(simbolo, novoPreco);
            System.out.printf("[OK] Preço de %s atualizado para R$ %.2f%n",
                    simbolo.toUpperCase(), novoPreco);
        } catch (NumberFormatException e) {
            System.err.println("[ERRO] Valor inválido. Use números (ex: 1234.56).");
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
            if (e.getCause() != null) reconectar();
        }
    }

    private void assinarNotificacoes() {
        System.out.print("Símbolo para monitorar: ");
        String simbolo = scanner.nextLine().trim();
        try {
            corretora.registrarCallback(simbolo, this);
            System.out.printf("[OK] Assinado! Você receberá alertas para: %s%n",
                    simbolo.toUpperCase());
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
            if (e.getCause() != null) reconectar();
        }
    }

    private void cancelarAssinatura() {
        System.out.print("Símbolo para cancelar: ");
        String simbolo = scanner.nextLine().trim();
        try {
            corretora.cancelarCallback(simbolo, this);
            System.out.printf("[OK] Assinatura cancelada para: %s%n", simbolo.toUpperCase());
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
            if (e.getCause() != null) reconectar();
        }
    }

    private void cadastrarAcao() {
        System.out.print("Símbolo da nova ação: ");
        String simbolo = scanner.nextLine().trim();
        System.out.print("Preço inicial (R$): ");
        try {
            double preco = Double.parseDouble(scanner.nextLine().trim().replace(",", "."));
            corretora.cadastrarAcao(simbolo, preco);
            System.out.printf("[OK] Ação %s cadastrada com R$ %.2f%n",
                    simbolo.toUpperCase(), preco);
        } catch (NumberFormatException e) {
            System.err.println("[ERRO] Valor inválido.");
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
            if (e.getCause() != null) reconectar();
        }
    }

    // -----------------------------------------------------------------------
    // Loop principal — menu interativo
    // -----------------------------------------------------------------------

    private void executar() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Corretora Distribuída — Cliente RMI    ║");
        System.out.println("╚══════════════════════════════════════════╝");

        if (!conectar()) return;

        boolean rodando = true;
        while (rodando) {
            System.out.println();
            System.out.println("┌─────────────────────────────────────┐");
            System.out.println("│            MENU PRINCIPAL           │");
            System.out.println("├─────────────────────────────────────┤");
            System.out.println("│  [1] Listar ações disponíveis       │");
            System.out.println("│  [2] Consultar preço de uma ação    │");
            System.out.println("│  [3] Atualizar preço de uma ação    │");
            System.out.println("│  [4] Assinar notificações de ação   │");
            System.out.println("│  [5] Cancelar assinatura            │");
            System.out.println("│  [6] Cadastrar nova ação            │");
            System.out.println("│  [0] Sair                           │");
            System.out.println("└─────────────────────────────────────┘");
            System.out.print("Opção: ");

            String opcao = scanner.nextLine().trim();

            switch (opcao) {
                case "1" -> listarAcoes();
                case "2" -> consultarPreco();
                case "3" -> atualizarPreco();
                case "4" -> assinarNotificacoes();
                case "5" -> cancelarAssinatura();
                case "6" -> cadastrarAcao();
                case "0" -> {
                    System.out.println("Encerrando cliente. Até logo!");
                    rodando = false;
                }
                default  -> System.out.println("[AVISO] Opção inválida. Tente novamente.");
            }
        }
    }

    // -----------------------------------------------------------------------
    // Main
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        try {
            Cliente cliente = new Cliente();
            cliente.executar();
        } catch (RemoteException e) {
            System.err.println("[ERRO FATAL] Não foi possível exportar o cliente como objeto remoto.");
            e.printStackTrace();
        }
    }
}
