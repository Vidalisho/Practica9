package modelo;

/** Arma que el jugador lleva en su inventario. */
public class Arma implements Inventariable {
    private final String nombre;
    private final int daño;
    private final double alcance;

    public Arma(String nombre, int daño, double alcance) {
        this.nombre = nombre;
        this.daño = daño;
        this.alcance = alcance;
    }

    public String getNombre() { return nombre; }
    public int getDaño() { return daño; }
    public double getAlcance() { return alcance; }

    @Override
    public void registrar() {
        System.out.println("[Inventario] Arma registrada: " + this);
    }

    @Override
    public void borrar() {
        System.out.println("[Inventario] Arma eliminada: " + nombre);
    }

    @Override
    public String toString() {
        return "Arma " + nombre + " (daño " + daño + ", alcance " + alcance + ")";
    }
}
