package juego;

import java.util.HashSet;
import java.util.Set;
import javafx.scene.input.KeyCode;

// Guarda qué teclas están presionadas y qué acción representan.
public class Teclado {

    private final Set<KeyCode> presionadas = new HashSet<>();

    public void presionar(KeyCode tecla) { presionadas.add(tecla); }
    public void soltar(KeyCode tecla) { presionadas.remove(tecla); }

    public boolean arriba() { return estaPresionada(KeyCode.W, KeyCode.UP); }
    public boolean abajo() { return estaPresionada(KeyCode.S, KeyCode.DOWN); }
    public boolean izquierda() { return estaPresionada(KeyCode.A, KeyCode.LEFT); }
    public boolean derecha() { return estaPresionada(KeyCode.D, KeyCode.RIGHT); }
    public boolean disparo() { return estaPresionada(KeyCode.SPACE); }

    private boolean estaPresionada(KeyCode... teclas) {
        for (KeyCode t : teclas) {
            if (presionadas.contains(t)) {
                return true;
            }
        }
        return false;
    }
}