package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Nivel del juego: agrupa obstáculos, checkpoints, elementos dinámicos e inventario. */
public class Nivel {
    private final String nombre;
    private final int numero;
    private final String dificultad;
    private final List<Obstaculo> obstaculos = new ArrayList<>();
    private final List<CheckPoint> checkPoints = new ArrayList<>();
    private final List<ElementoDinamico> elementosDinamicos = new ArrayList<>();
    private final Inventario inventario;

    public Nivel(String nombre, int numero, String dificultad, Inventario inventario) {
        this.nombre = nombre;
        this.numero = numero;
        this.dificultad = dificultad;
        this.inventario = inventario;
    }

    public String getNombre() { return nombre; }
    public int getNumero() { return numero; }
    public String getDificultad() { return dificultad; }

    // Getters extra (necesarios para dibujar el nivel), de solo lectura.
    public List<Obstaculo> getObstaculos() { return Collections.unmodifiableList(obstaculos); }
    public List<CheckPoint> getCheckPoints() { return Collections.unmodifiableList(checkPoints); }
    public List<ElementoDinamico> getElementosDinamicos() { return Collections.unmodifiableList(elementosDinamicos); }
    public Inventario getInventario() { return inventario; }

    public void agregarObstaculo(Obstaculo obstaculo) { obstaculos.add(obstaculo); }
    public void agregarCheckPoint(CheckPoint checkPoint) { checkPoints.add(checkPoint); }
    public void agregarElementoDinamico(ElementoDinamico elemento) { elementosDinamicos.add(elemento); }

    /** Mueve todos los elementos dinámicos del nivel en la misma dirección. */
    public void moverElementosDinamicos(String direccion, int distancia) {
        for (ElementoDinamico elemento : elementosDinamicos) {
            elemento.mover(direccion, distancia);
        }
    }

    public void mostrarEstado() {
        long vivos = obstaculos.stream().filter(o -> !o.isDestruido()).count();
        long activados = checkPoints.stream().filter(CheckPoint::isActivado).count();
        System.out.println("=== Nivel " + numero + ": " + nombre + " (" + dificultad + ") ===");
        System.out.println("Obstáculos activos: " + vivos + "/" + obstaculos.size());
        System.out.println("Checkpoints activados: " + activados + "/" + checkPoints.size());
        System.out.println("Elementos dinámicos: " + elementosDinamicos.size());
        inventario.listarItems();
    }
}
