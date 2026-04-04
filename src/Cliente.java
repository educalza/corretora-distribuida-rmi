import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Scanner;

/**
 * Cliente da Corretora Distribuída.
 *
 * - Conecta ao servidor via RMI com reconexão automática
 * - Implementa ClienteCallback (padrão Observer) para receber notificações
 * - Menu: listar ativos, consultar valor, comprar (sobe preço), vender (baixa preço)
 */
public class Cliente extends UnicastRemoteObject implements ClienteCallback {

    private static final String HOST         = "localhost";
    private static final int    PORTA        = 1099;
    private static final String NOME_SERVICO = "CorretorService";

    /** Percentual de variação por operação de compra/venda */
    private static final double VARIACAO_PERCENTUAL = 0.01; // 1%

    private transient CorretorInterface corretora;
    private final transient Scanner scanner = new Scanner(System.in);

    protected Cliente() throws RemoteException {
        super();
    }

    // ── Observer: chamado pelo servidor quando um preço muda ──────────────────

    @Override
    public void notificar(String nome, double novoValor) throws RemoteException {
        System.out.println();
        System.out.println("╔════════════════════════════════════════╗");
        System.out.printf ("║  ALERTA: %-6s  novo valor: R$ %.2f%n", nome, novoValor);
        System.out.println("╚════════════════════════════════════════╝");
        System.out.print("Opção: ");
    }

    // ── Conexão com reconexão automática ─────────────────────────────────────

    private boolean conectar() {
        int tentativas = 5;
        for (int i = 1; i <= tentativas; i++) {
            try {
                Registry registry = LocateRegistry.getRegistry(HOST, PORTA);
                corretora = (CorretorInterface) registry.lookup(NOME_SERVICO);
                System.out.println("[OK] Conectado ao servidor.");
                return true;
            } catch (Exception e) {
                System.out.printf("[%d/%d] Servidor indisponível. Aguardando 3s...%n", i, tentativas);
                try { Thread.sleep(3000); } catch (InterruptedException ie) { break; }
            }
        }
        System.err.println("[ERRO] Não foi possível conectar.");
        return false;
    }

    private void reconectar() {
        System.out.println("[AVISO] Reconectando...");
        conectar();
    }

    // ── Operações do menu ─────────────────────────────────────────────────────

    private void listarAtivos() {
        try {
            List<Ativo> lista = corretora.getListaAtivos();
            System.out.println();
            System.out.println("┌──────────┬──────────────────────┐");
            System.out.println("│  Ativo   │   Valor Atual        │");
            System.out.println("├──────────┼──────────────────────┤");
            lista.stream()
                 .sorted((a, b) -> a.getNome().compareTo(b.getNome()))
                 .forEach(a -> System.out.printf("│ %-8s │  R$ %13.2f   │%n",
                         a.getNome(), a.getValor()));
            System.out.println("└──────────┴──────────────────────┘");
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
            reconectar();
        }
    }

    private void consultarValor() {
        System.out.print("Nome do ativo: ");
        String nome = scanner.nextLine().trim();
        try {
            double valor = corretora.getValor(nome);
            System.out.printf("Valor de %s: R$ %.2f%n", nome.toUpperCase(), valor);
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
        }
    }

    /**
     * Comprar: aumenta o valor do ativo em 1%.
     * Simula aumento de demanda pela compra.
     */
    private void comprar() {
        System.out.print("Nome do ativo para comprar: ");
        String nome = scanner.nextLine().trim();
        try {
            double valorAtual = corretora.getValor(nome);
            double novoValor  = valorAtual * (1 + VARIACAO_PERCENTUAL);
            corretora.setValor(nome, novoValor);
            System.out.printf("[COMPRA] %s: R$ %.2f → R$ %.2f (+1%%)%n",
                    nome.toUpperCase(), valorAtual, novoValor);
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
        }
    }

    /**
     * Vender: reduz o valor do ativo em 1%.
     * Simula aumento de oferta pela venda.
     */
    private void vender() {
        System.out.print("Nome do ativo para vender: ");
        String nome = scanner.nextLine().trim();
        try {
            double valorAtual = corretora.getValor(nome);
            double novoValor  = valorAtual * (1 - VARIACAO_PERCENTUAL);
            corretora.setValor(nome, novoValor);
            System.out.printf("[VENDA]  %s: R$ %.2f → R$ %.2f (-1%%)%n",
                    nome.toUpperCase(), valorAtual, novoValor);
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
        }
    }

    private void registrarComoObservador() {
        try {
            corretora.registrarObservador(this);
            System.out.println("[OK] Registrado como observador. Você receberá alertas de mudança de preço.");
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
        }
    }

    private void cancelarObservador() {
        try {
            corretora.removerObservador(this);
            System.out.println("[OK] Removido dos observadores.");
        } catch (RemoteException e) {
            System.err.println("[ERRO] " + e.getMessage());
        }
    }

    // ── Loop principal ────────────────────────────────────────────────────────

    private void executar() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Corretora Distribuída — Cliente RMI    ║");
        System.out.println("╚══════════════════════════════════════════╝");

        if (!conectar()) return;

        // Registra automaticamente como observador ao conectar
        registrarComoObservador();

        boolean rodando = true;
        while (rodando) {
            System.out.println();
            System.out.println("┌──────────────────────────────────────┐");
            System.out.println("│            MENU PRINCIPAL            │");
            System.out.println("├──────────────────────────────────────┤");
            System.out.println("│  [1] Listar ativos                   │");
            System.out.println("│  [2] Consultar valor de um ativo     │");
            System.out.println("│  [3] Comprar ativo  (+1% no valor)   │");
            System.out.println("│  [4] Vender ativo   (-1% no valor)   │");
            System.out.println("│  [5] Cancelar notificações           │");
            System.out.println("│  [0] Sair                            │");
            System.out.println("└──────────────────────────────────────┘");
            System.out.print("Opção: ");

            String opcao = scanner.nextLine().trim();

            switch (opcao) {
                case "1" -> listarAtivos();
                case "2" -> consultarValor();
                case "3" -> comprar();
                case "4" -> vender();
                case "5" -> cancelarObservador();
                case "0" -> {
                    cancelarObservador();
                    System.out.println("Até logo!");
                    rodando = false;
                }
                default -> System.out.println("[AVISO] Opção inválida.");
            }
        }
    }

    // ── Main ──────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            new Cliente().executar();
        } catch (RemoteException e) {
            System.err.println("[ERRO FATAL] Não foi possível exportar o cliente como objeto remoto.");
            e.printStackTrace();
        }
    }
}
