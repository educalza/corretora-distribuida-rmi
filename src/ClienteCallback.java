import java.rmi.Remote;
import java.rmi.RemoteException;

// Callback do Cliente (observer)
public interface ClienteCallback extends Remote {

    // Chamado pelo servidor quando o preço de um ativo muda.
    void notificarMudancaPreco(String nomeAtivo, double novoValor) throws RemoteException;

    // Chamado pelo servidor ANTES de encerrar, para avisar os clientes imediatamente.
    void notificarEncerramento() throws RemoteException;
}
