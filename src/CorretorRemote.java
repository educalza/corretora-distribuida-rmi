import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// Interface Remota da Corretora
// Define todos os serviços disponíveis para os clientes via RMI
public interface CorretorRemote extends Remote {

    // Retorna a lista de todos os ativos cadastrados na corretora
    List<Ativo> listarAtivos() throws RemoteException;

    // Consulta o valor atual de um ativo pelo nome
    // Retorna -1 se o ativo não for encontrado
    double getValor(String nomeAtivo) throws RemoteException;

    // Atualiza o valor de um ativo
    // todos os clientes registrados são notificados
    void setValor(String nomeAtivo, double novoValor) throws RemoteException;

    // Registra um cliente para receber notificações de mudança de preço (padrão Observer)
    void registrarCliente(ClienteCallback cliente) throws RemoteException;

    // Remove o registro de um cliente (quando ele se desconectar)
    void desregistrarCliente(ClienteCallback cliente) throws RemoteException;
}
