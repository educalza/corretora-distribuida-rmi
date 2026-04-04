import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota implementada pelo CLIENTE.
 * O servidor chama este método para notificar mudanças de preço em tempo real (callback).
 */
public interface ClienteCallback extends Remote {

    /**
     * Chamado pelo servidor quando o preço de uma ação muda.
     *
     * @param simbolo    Símbolo da ação (ex: "BTC")
     * @param precoAntigo Preço anterior
     * @param precoNovo   Novo preço
     */
    void notificarMudancaPreco(String simbolo, double precoAntigo, double precoNovo) throws RemoteException;
}
