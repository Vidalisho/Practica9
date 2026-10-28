package modelo;

/** Contrato para todo objeto que puede guardarse en un {@link Inventario}. */
public interface Inventariable {
    void registrar();
    void borrar();
}
