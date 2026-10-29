package modelo;

// Obstáculo fijo que daña al personaje por contacto y puede destruirse
public class Obstaculo implements Destruible {
    private final String nombre;
    private final int daño;
    private final int posicionX;
    private final int posicionY;
    private boolean destruido;

    public Obstaculo(String nombre, int daño, int posicionX, int posicionY) {
        this.nombre = nombre;
        this.daño = daño;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
        this.destruido = false;
    }

    public String getNombre() { return nombre; }
    public int getDaño() { return daño; }
    public int getPosicionX() { return posicionX; }
    public int getPosicionY() { return posicionY; }
    public boolean isDestruido() { return destruido; }

    @Override
    public void destruye() {
        destruido = true;
    }
}
