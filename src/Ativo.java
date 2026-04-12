import java.io.Serializable;

/**
 * Classe ativo financeiro
 */
public class Ativo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nome;
    private double valor;

    public Ativo(String nome, double valor) {
        this.nome = nome;
        this.valor = valor;
    }

    public String getNome() {
        return nome;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    @Override
    public String toString() {
        return String.format("%-6s -> R$ %.2f", nome, valor);
    }
}
