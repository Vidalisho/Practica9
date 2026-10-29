package modelo;

// Punto de control donde reaparece el personaje al morir.
public class CheckPoint {
    private final String nombre;
    private final int posicionX;
    private final int posicionY;
    private boolean activado;

    public CheckPoint(String nombre, int posicionX, int posicionY) {
        this.nombre = nombre;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
        this.activado = false;
    }

    public String getNombre() { return nombre; }
    public int getPosicionX() { return posicionX; }
    public int getPosicionY() { return posicionY; }
    public boolean isActivado() { return activado; }

    public void activar() {
        activado = true;
    }
}
