package modelo;

/** Contrato para elementos que se desplazan por el nivel. */
public interface ElementoDinamico {
    /**
     * @param direccion "ARRIBA", "ABAJO", "IZQUIERDA" o "DERECHA"
     * @param distancia cantidad de unidades (píxeles) a desplazarse
     */
    void mover(String direccion, int distancia);
}
