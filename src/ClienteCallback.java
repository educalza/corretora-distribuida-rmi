import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface do observador (padrão Observer).
 * Implementada pelo cliente — o servidor chama este método
 * para notificar mudanças de preço em tempo real.
 */
public interface ClienteCallback extends Remote {

    /** Chamado pelo servidor quando o valor de um ativo é alterado. */
    void notificar(String nome, double novoValor) throws RemoteException;
}
