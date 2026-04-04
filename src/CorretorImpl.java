import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação da interface remota CorretorInterface.
 *
 * Características:
 *  - Thread-safe: usa ConcurrentHashMap para preços e listas sincronizadas para callbacks
 *  - Callbacks: notifica clientes em threads separadas para não bloquear quem atualiza
 *  - Controle de acesso: valida símbolos cadastrados antes de qualquer operação
 */
public class CorretorImpl extends UnicastRemoteObject implements CorretorInterface {

    // Mapa de símbolo → preço atual (thread-safe)
    private final ConcurrentHashMap<String, Double> precos = new ConcurrentHashMap<>();

    // Mapa de símbolo → lista de callbacks registrados
    private final ConcurrentHashMap<String, List<ClienteCallback>> callbacks = new ConcurrentHashMap<>();

    protected CorretorImpl() throws RemoteException {
        super();
    }

    // -----------------------------------------------------------------------
    // Serviços da interface remota
    // -----------------------------------------------------------------------

    @Override
    public synchronized void cadastrarAcao(String simbolo, double precoInicial) throws RemoteException {
        String s = simbolo.toUpperCase().trim();
        if (precos.containsKey(s)) {
            throw new RemoteException("Ação já cadastrada: " + s);
        }
        if (precoInicial < 0) {
            throw new RemoteException("Preço inicial não pode ser negativo.");
        }
        precos.put(s, precoInicial);
        callbacks.put(s, Collections.synchronizedList(new ArrayList<>()));
        System.out.printf("[Servidor] Ação cadastrada: %-6s  Preço inicial: R$ %.2f%n", s, precoInicial);
    }

    @Override
    public double consultarPreco(String simbolo) throws RemoteException {
        String s = simbolo.toUpperCase().trim();
        Double preco = precos.get(s);
        if (preco == null) {
            throw new RemoteException("Ação não encontrada: " + s);
        }
        System.out.printf("[Servidor] Consulta de preço: %-6s  R$ %.2f%n", s, preco);
        return preco;
    }

    @Override
    public Map<String, Double> listarAcoes() throws RemoteException {
        System.out.println("[Servidor] Listagem de ações solicitada.");
        return new HashMap<>(precos); // Retorna cópia para evitar acesso externo direto
    }

    @Override
    public void atualizarPreco(String simbolo, double novoPreco) throws RemoteException {
        String s = simbolo.toUpperCase().trim();
        if (!precos.containsKey(s)) {
            throw new RemoteException("Ação não encontrada: " + s);
        }
        if (novoPreco < 0) {
            throw new RemoteException("Preço não pode ser negativo.");
        }

        double precoAntigo = precos.get(s);
        precos.put(s, novoPreco);

        System.out.printf("[Servidor] Preço atualizado: %-6s  R$ %.2f → R$ %.2f%n", s, precoAntigo, novoPreco);

        // Notifica callbacks em thread separada para não bloquear o chamador
        List<ClienteCallback> lista = callbacks.get(s);
        if (lista != null && !lista.isEmpty()) {
            new Thread(() -> notificarClientes(s, precoAntigo, novoPreco, lista)).start();
        }
    }

    @Override
    public void registrarCallback(String simbolo, ClienteCallback callback) throws RemoteException {
        String s = simbolo.toUpperCase().trim();
        if (!callbacks.containsKey(s)) {
            throw new RemoteException("Ação não encontrada para registro de callback: " + s);
        }
        callbacks.get(s).add(callback);
        System.out.printf("[Servidor] Callback registrado para: %s  (total: %d)%n",
                s, callbacks.get(s).size());
    }

    @Override
    public void cancelarCallback(String simbolo, ClienteCallback callback) throws RemoteException {
        String s = simbolo.toUpperCase().trim();
        List<ClienteCallback> lista = callbacks.get(s);
        if (lista != null) {
            lista.remove(callback);
            System.out.printf("[Servidor] Callback cancelado para: %s  (restantes: %d)%n",
                    s, lista.size());
        }
    }

    // -----------------------------------------------------------------------
    // Método auxiliar — notificação de callbacks
    // -----------------------------------------------------------------------

    /**
     * Percorre a lista de callbacks e notifica cada cliente.
     * Callbacks defeituosos (cliente desconectado) são removidos automaticamente.
     */
    private void notificarClientes(String simbolo, double precoAntigo, double precoNovo,
                                   List<ClienteCallback> lista) {
        List<ClienteCallback> inativos = new ArrayList<>();

        synchronized (lista) {
            for (ClienteCallback cb : lista) {
                try {
                    cb.notificarMudancaPreco(simbolo, precoAntigo, precoNovo);
                } catch (RemoteException e) {
                    // Cliente provavelmente desconectou — marca para remover
                    System.out.printf("[Servidor] Callback inativo removido para: %s%n", simbolo);
                    inativos.add(cb);
                }
            }
            lista.removeAll(inativos);
        }
    }
}
