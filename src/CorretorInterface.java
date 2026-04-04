import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface remota da Corretora.
 * Define os 3 serviços disponíveis para os clientes, conforme especificação.
 */
public interface CorretorInterface extends Remote {

    /** Retorna a lista de todos os ativos cadastrados. */
    List<Ativo> getListaAtivos() throws RemoteException;

    /** Retorna o valor atual de um ativo pelo nome. */
    double getValor(String nome) throws RemoteException;

    /**
     * Atualiza o valor de um ativo.
     * O servidor notifica todos os clientes registrados (observer).
     */
    void setValor(String nome, double novoValor) throws RemoteException;

    /** Registra um cliente para receber notificações de mudança de preço. */
    void registrarObservador(ClienteCallback observer) throws RemoteException;

    /** Remove o registro de um cliente observador. */
    void removerObservador(ClienteCallback observer) throws RemoteException;
}
