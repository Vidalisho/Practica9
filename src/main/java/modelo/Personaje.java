package modelo;

//personaje controlado por el jugador
public class Personaje implements Destruible, ElementoDinamico {
    private final String nombre;
    private final int vidaMaxima; // extra al diseño: tope para curar()
    private int vida;
    private int posicionX;
    private int posicionY;

    public Personaje(String nombre, int vida, int posicionX, int posicionY) {
        this.nombre = nombre;
        this.vida = vida;
        this.vidaMaxima = vida;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
    }

    public String getNombre() { return nombre; }
    public int getVida() { return vida; }
    public int getVidaMaxima() { return vidaMaxima; }
    public int getPosicionX() { return posicionX; }
    public int getPosicionY() { return posicionY; }

    public void setPosicion(int x, int y) {
        this.posicionX = x;
        this.posicionY = y;
    }

    public void recibirDaño(int daño) {
        vida = Math.max(0, vida - daño);
    }

    //restaura vida sin pasar de la vida máxima
    public void curar(int cantidad) {
        vida = Math.min(vidaMaxima, vida + cantidad);
    }

    //metodo implementado de ElementoDinamico
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

    //Metodo implementrado de destruible
    @Override
    public void destruye() {
        vida = 0;
    }
}
