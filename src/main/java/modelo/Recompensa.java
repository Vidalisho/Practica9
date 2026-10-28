package modelo;

public class Recompensa implements Inventariable {
    private final String nombre;
    private final int valor;
    private final String tipo;

    public Recompensa(String nombre, int valor, String tipo) {
        this.nombre = nombre;
        this.valor = valor;
        this.tipo = tipo;
    }

    //getters
    public String getNombre() { return nombre; }
    public int getValor() { return valor; }
    public String getTipo() { return tipo; }

    //metodos implementados de inventario
    @Override
    public void registrar() {
        System.out.println("[Inventario] Recompensa registrada: " + this);
    }

    @Override
    public void borrar() {
        System.out.println("[Inventario] Recompensa eliminada: " + nombre);
    }

    @Override
    public String toString() {
        return "Recompensa " + nombre + " (" + tipo + ", valor " + valor + ")";
    }
}
