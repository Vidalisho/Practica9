package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//Inventario con capacidad limitada para objetos
public class Inventario {
    private final int capacidadMaxima;
    private final List<Inventariable> items = new ArrayList<>();

    public Inventario(int capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public int getCapacidadMaxima() { return capacidadMaxima; }

    // Devuelve una vista de solo lectura (encapsulamiento)
    public List<Inventariable> getItems() {
        return Collections.unmodifiableList(items);
    }

    //return true si había espacio y el item se agregó
    public boolean agregarItem(Inventariable item) {
        if (items.size() >= capacidadMaxima) {
            return false;
        }
        items.add(item);
        item.registrar();
        return true;
    }

    //return true si el item estaba en el inventario y se eliminó
    public boolean eliminarItem(Inventariable item) {
        if (items.remove(item)) {
            item.borrar();
            return true;
        }
        return false;
    }

    public void listarItems() {
        System.out.println("--- Inventario (" + items.size() + "/" + capacidadMaxima + ") ---");
        for (Inventariable item : items) {
            System.out.println(" * " + item);
        }
    }
}
