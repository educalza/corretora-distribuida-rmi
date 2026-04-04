import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação da corretora no servidor.
 *
 * - Gerencia a lista de ativos (ConcurrentHashMap para thread-safety)
 * - Implementa o padrão Observer: notifica clientes ao chamar setValor
 */
public class CorretorImpl extends UnicastRemoteObject implements CorretorInterface {

    // Mapa nome → Ativo (thread-safe)
    private final ConcurrentHashMap<String, Ativo> ativos = new ConcurrentHashMap<>();

    // Lista de observadores (clientes registrados)
    private final List<ClienteCallback> observadores =
            Collections.synchronizedList(new ArrayList<>());

    protected CorretorImpl() throws RemoteException {
        super();
    }

    // ── Método auxiliar: adiciona ativo (usado pelo Servidor na inicialização) ──

    public void adicionarAtivo(String nome, double valor) {
        ativos.put(nome.toUpperCase(), new Ativo(nome.toUpperCase(), valor));
        System.out.printf("[Servidor] Ativo cadastrado: %-8s  R$ %.2f%n", nome.toUpperCase(), valor);
    }

    // ── Interface remota ────────────────────────────────────────────────────────

    @Override
    public List<Ativo> getListaAtivos() throws RemoteException {
        System.out.println("[Servidor] getListaAtivos() chamado.");
        return new ArrayList<>(ativos.values());
    }

    @Override
    public double getValor(String nome) throws RemoteException {
        Ativo ativo = ativos.get(nome.toUpperCase());
        if (ativo == null) {
            throw new RemoteException("Ativo não encontrado: " + nome);
        }
        System.out.printf("[Servidor] getValor(%s) = R$ %.2f%n", nome.toUpperCase(), ativo.getValor());
        return ativo.getValor();
    }

    @Override
    public synchronized void setValor(String nome, double novoValor) throws RemoteException {
        Ativo ativo = ativos.get(nome.toUpperCase());
        if (ativo == null) {
            throw new RemoteException("Ativo não encontrado: " + nome);
        }
        if (novoValor < 0) {
            throw new RemoteException("Valor não pode ser negativo.");
        }

        double valorAntigo = ativo.getValor();
        ativo.setValor(novoValor);

        System.out.printf("[Servidor] setValor(%s): R$ %.2f → R$ %.2f%n",
                nome.toUpperCase(), valorAntigo, novoValor);

        // Notifica todos os observadores em thread separada (não bloqueia o chamador)
        final String nomeAtivo = nome.toUpperCase();
        new Thread(() -> notificarObservadores(nomeAtivo, novoValor)).start();
    }

    @Override
    public void registrarObservador(ClienteCallback observer) throws RemoteException {
        observadores.add(observer);
        System.out.println("[Servidor] Observador registrado. Total: " + observadores.size());
    }

    @Override
    public void removerObservador(ClienteCallback observer) throws RemoteException {
        observadores.remove(observer);
        System.out.println("[Servidor] Observador removido. Total: " + observadores.size());
    }

    // ── Observer: notifica todos os clientes ───────────────────────────────────

    private void notificarObservadores(String nome, double novoValor) {
        List<ClienteCallback> inativos = new ArrayList<>();

        synchronized (observadores) {
            for (ClienteCallback obs : observadores) {
                try {
                    obs.notificar(nome, novoValor);
                } catch (RemoteException e) {
                    // Cliente desconectou — remove da lista
                    System.out.println("[Servidor] Observador inativo removido.");
                    inativos.add(obs);
                }
            }
            observadores.removeAll(inativos);
        }
    }
}
