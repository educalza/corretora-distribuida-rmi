import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Interface remota da Corretora (exposta pelo servidor via RMI).
 * Define todos os serviços disponíveis para os clientes.
 */
public interface CorretorInterface extends Remote {

    /**
     * Cadastra uma nova ação no sistema.
     *
     * @param simbolo      Símbolo da ação (ex: "BTC", "PETR4")
     * @param precoInicial Preço inicial da ação
     * @throws RemoteException se a ação já estiver cadastrada
     */
    void cadastrarAcao(String simbolo, double precoInicial) throws RemoteException;

    /**
     * Consulta o preço atual de uma ação.
     *
     * @param simbolo Símbolo da ação
     * @return Preço atual
     * @throws RemoteException se o símbolo não existir
     */
    double consultarPreco(String simbolo) throws RemoteException;

    /**
     * Lista todas as ações cadastradas e seus respectivos preços.
     *
     * @return Mapa de símbolo → preço
     */
    Map<String, Double> listarAcoes() throws RemoteException;

    /**
     * Atualiza o preço de uma ação e notifica todos os clientes cadastrados via callback.
     *
     * @param simbolo   Símbolo da ação
     * @param novoPreco Novo preço
     * @throws RemoteException se o símbolo não existir
     */
    void atualizarPreco(String simbolo, double novoPreco) throws RemoteException;

    /**
     * Registra um callback para receber notificações de mudança de preço.
     *
     * @param simbolo  Símbolo da ação a monitorar
     * @param callback Objeto remoto do cliente
     * @throws RemoteException se o símbolo não existir
     */
    void registrarCallback(String simbolo, ClienteCallback callback) throws RemoteException;

    /**
     * Cancela o registro de um callback.
     *
     * @param simbolo  Símbolo da ação
     * @param callback Objeto remoto a remover
     */
    void cancelarCallback(String simbolo, ClienteCallback callback) throws RemoteException;
}
