import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// Implementação do Callback do Cliente.
// Quando o servidor avisa que está encerrando (notificarEncerramento),
public class ClienteCallbackImpl extends UnicastRemoteObject implements ClienteCallback {

    private static final int INTERVALO_RECONEXAO_MS = 3000;

    // Cores ANSI
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED   = "\u001B[31m";

    private final String nomeCliente;
    private final AtomicReference<CorretorRemote> corretorRef;
    private final AtomicBoolean reconectando;

    public ClienteCallbackImpl(String nomeCliente,
                               AtomicReference<CorretorRemote> corretorRef,
                               AtomicBoolean reconectando) throws RemoteException {
        super();
        this.nomeCliente = nomeCliente;
        this.corretorRef = corretorRef;
        this.reconectando = reconectando;
    }

    // Chamado pelo servidor quando o preço de um ativo muda.
    @Override
    public void notificarMudancaPreco(String nomeAtivo, double novoValor) throws RemoteException {
        System.out.println("\n>>> [NOTIFICAÇÃO] " + nomeAtivo
                + " mudou para R$ " + String.format("%.2f", novoValor));
        System.out.print("Escolha uma opção: ");
    }

    // Chamado pelo servidor ANTES de encerrar — avisa imediatamente e dispara reconexão.
    @Override
    public void notificarEncerramento() throws RemoteException {
        System.out.println(ANSI_RED + "\n╔══════════════════════════════════════════╗");
        System.out.println("║  ⚠  SERVIDOR ENCERRADO!                 ║");
        System.out.println("║  Tentando reconectar automaticamente... ║");
        System.out.println("╚══════════════════════════════════════════╝" + ANSI_RESET);
        iniciarReconexaoBackground();
    }

    // thread de reconexão em background.
    public void iniciarReconexaoBackground() {
        if (reconectando.compareAndSet(false, true)) {
            corretorRef.set(null); // invalida referência atual
            Thread t = new Thread(this::loopReconexao, "Thread-Reconexao");
            t.setDaemon(true);    // morre junto com o processo principal
            t.start();
        }
    }

    // Loop de reconexão
    private void loopReconexao() {
        try {
            while (true) {
                System.out.println(ANSI_RED + "Servidor indisponível. Tentando novamente em "
                        + (INTERVALO_RECONEXAO_MS / 1000) + "s..." + ANSI_RESET);
                try {
                    Thread.sleep(INTERVALO_RECONEXAO_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    CorretorRemote novoCorretor = (CorretorRemote) registry.lookup("CorretorService");
                    novoCorretor.registrarCliente(this); // re-registra callback
                    corretorRef.set(novoCorretor);       // atualiza ref compartilhada

                    System.out.println("\n╔══════════════════════════════════════════╗");
                    System.out.println("║  ✔  RECONECTADO AO SERVIDOR!            ║");
                    System.out.println("║  Você pode continuar operando.          ║");
                    System.out.println("╚══════════════════════════════════════════╝");
                    System.out.println("\n========== CORRETORA RMI ==========");
                    System.out.println("1. Listar ativos");
                    System.out.println("2. Consultar preço de um ativo");
                    System.out.println("3. Comprar ativo (preço sobe 5%)");
                    System.out.println("4. Vender ativo (preço cai 5%)");
                    System.out.println("0. Sair");
                    System.out.print("Escolha uma opção: ");
                    return; // reconexão concluída
                } catch (Exception e) {
                    // servidor ainda fora, continua tentando
                }
            }
        } finally {
            reconectando.set(false);
        }
    }

    public String getNomeCliente() {
        return nomeCliente;
    }
}
