import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cliente da corretora RMI
 */
public class Cliente {

    private static final double VARIACAO_PERCENTUAL = 0.05;
    private static final int    INTERVALO_RECONEXAO_MS = 3000;

    // Cores ANSI
    private static final String ANSI_RESET  = "\u001B[0m";
    private static final String ANSI_RED    = "\u001B[31m";
    private static final String ANSI_GREEN  = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Digite seu nome: ");
        String nomeCliente = scanner.nextLine();

        // thread principal e o callback
        AtomicReference<CorretorRemote> corretorRef = new AtomicReference<>();
        AtomicBoolean reconectando = new AtomicBoolean(false);

        // callback
        ClienteCallbackImpl meuCallback;
        try {
            meuCallback = new ClienteCallbackImpl(nomeCliente, corretorRef, reconectando);
        } catch (Exception e) {
            System.out.println("Erro ao criar callback: " + e.getMessage());
            return;
        }

        // conecta ao servidor
        conectarERegistrar(nomeCliente, corretorRef, meuCallback);

        // menu
        boolean rodando = true;
        while (rodando) {

            aguardarConexao(corretorRef);

            System.out.println("\n========== CORRETORA RMI ==========");
            System.out.println("1. Listar ativos");
            System.out.println("2. Consultar preço de um ativo");
            System.out.println("3. Comprar ativo (preço sobe 5%)");
            System.out.println("4. Vender ativo (preço cai 5%)");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opção: ");

            String opcao = scanner.nextLine().trim();

            // aguarda reconexão
            if (corretorRef.get() == null) {
                aguardarConexao(corretorRef);
            }

            try {
                CorretorRemote corretor = corretorRef.get();
                switch (opcao) {
                    case "1":
                        listarAtivos(corretor);
                        break;

                    case "2":
                        System.out.print("Nome do ativo: ");
                        consultarPreco(corretor, scanner.nextLine().trim().toUpperCase());
                        break;

                    case "3":
                        System.out.print("Nome do ativo para COMPRAR: ");
                        comprarAtivo(corretor, scanner.nextLine().trim().toUpperCase());
                        break;

                    case "4":
                        System.out.print("Nome do ativo para VENDER: ");
                        venderAtivo(corretor, scanner.nextLine().trim().toUpperCase());
                        break;

                    case "0":
                        rodando = false;
                        break;

                    case "":
                        break; // relança o menu sem mensagem de erro

                    default:
                        System.out.println("Opção inválida!");
                }

            } catch (Exception e) {
                // crash, inicia reconexão manual
                System.out.println(ANSI_RED + "\n*** ATENÇÃO: SERVIDOR FORA DO AR! ***" + ANSI_RESET);
                System.out.println(ANSI_RED + "Tentando reconectar automaticamente..." + ANSI_RESET);
                meuCallback.iniciarReconexaoBackground();
                aguardarConexao(corretorRef);
            }
        }

        // desregistra o callback
        try {
            CorretorRemote corretor = corretorRef.get();
            if (corretor != null) {
                corretor.desregistrarCliente(meuCallback);
            }
        } catch (Exception e) {
            // ignora erros ao desconectar
        }

        System.out.println("Até logo, " + nomeCliente + "!");
        scanner.close();
    }

    // Operações
    private static void listarAtivos(CorretorRemote corretor) throws Exception {
        List<Ativo> ativos = corretor.listarAtivos();
        System.out.println("\n--- Ativos disponíveis ---");
        for (Ativo a : ativos) {
            System.out.println("  " + a);
        }
    }

    private static void consultarPreco(CorretorRemote corretor, String nome) throws Exception {
        double valor = corretor.getValor(nome);
        if (valor < 0) {
            System.out.println("Ativo '" + nome + "' não encontrado.");
        } else {
            System.out.printf("Preço de %s: R$ %.2f%n", nome, valor);
        }
    }

    private static void comprarAtivo(CorretorRemote corretor, String nome) throws Exception {
        double valorAtual = corretor.getValor(nome);
        if (valorAtual < 0) { System.out.println("Ativo '" + nome + "' não encontrado."); return; }
        double novoValor = valorAtual * (1 + VARIACAO_PERCENTUAL);
        corretor.setValor(nome, novoValor);
        System.out.printf(ANSI_GREEN + "COMPRA realizada! %s: R$ %.2f -> R$ %.2f%n" + ANSI_RESET, nome, valorAtual, novoValor);
    }

    private static void venderAtivo(CorretorRemote corretor, String nome) throws Exception {
        double valorAtual = corretor.getValor(nome);
        if (valorAtual < 0) { System.out.println("Ativo '" + nome + "' não encontrado."); return; }
        double novoValor = valorAtual * (1 - VARIACAO_PERCENTUAL);
        corretor.setValor(nome, novoValor);
        System.out.printf(ANSI_YELLOW + "VENDA realizada! %s: R$ %.2f -> R$ %.2f%n" + ANSI_RESET, nome, valorAtual, novoValor);
    }

    // Bloqueia a thread principal até a referência do corretor estar válida.
    private static void aguardarConexao(AtomicReference<CorretorRemote> corretorRef) {
        while (corretorRef.get() == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // tenta conectar indefinidamente até conseguir conectar e registrar o callback.
    private static void conectarERegistrar(String nomeCliente,
                                           AtomicReference<CorretorRemote> corretorRef,
                                           ClienteCallbackImpl callback) {
        while (true) {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                CorretorRemote corretor = (CorretorRemote) registry.lookup("CorretorService");
                corretor.registrarCliente(callback);
                corretorRef.set(corretor);
                System.out.println("[" + nomeCliente + "] Conectado e registrado com sucesso!");
                return;
            } catch (Exception e) {
                System.out.println(ANSI_RED + "Servidor indisponível. Tentando novamente em "
                        + (INTERVALO_RECONEXAO_MS / 1000) + "s..." + ANSI_RESET);
                try {
                    Thread.sleep(INTERVALO_RECONEXAO_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
