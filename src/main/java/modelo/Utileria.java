package modelo;

/** Objeto de utilería móvil (en el juego: botiquines que patrullan el nivel). */
public class Utileria implements ElementoDinamico {
    private final String nombre;
    private final String descripcion;
    private int posicionX;
    private int posicionY;
    private boolean usada;

    public Utileria(String nombre, String descripcion, int posicionX, int posicionY) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
        this.usada = false;
    }

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getPosicionX() { return posicionX; }
    public int getPosicionY() { return posicionY; }
    public boolean isUsada() { return usada; }

    public void usar() {
        usada = true;
    }

    @Override
    public void mover(String direccion, int distancia) {
        switch (direccion.toUpperCase()) {
            case "ARRIBA" -> posicionY -= distancia;
            case "ABAJO" -> posicionY += distancia;
            case "IZQUIERDA" -> posicionX -= distancia;
            case "DERECHA" -> posicionX += distancia;
            default -> throw new IllegalArgumentException("Dirección inválida: " + direccion);
        }
    }
}
