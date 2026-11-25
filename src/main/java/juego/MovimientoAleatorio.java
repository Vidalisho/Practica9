package juego;

import static juego.Config.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import modelo.Utileria;

/**
 * Cerebro de movimiento de UN solo botiquín. Cada instancia tiene su propio
 * generador de números aleatorios, su propia velocidad, su rumbo actual y su
 * propio contador, así que dos botiquines nunca se mueven igual (independencia).
 *
 * Funciona por "tramos": elige un rumbo (ARRIBA, ABAJO, IZQUIERDA o DERECHA) y
 * lo sigue unos cuantos frames; al terminar el tramo, o al topar con el borde
 * de la zona, elige otro rumbo al azar.
 */
public class MovimientoAleatorio {

    private static final String[] DIRECCIONES = {"ARRIBA", "ABAJO", "IZQUIERDA", "DERECHA"};

    private final Random azar = new Random();
    private final int velocidad;
    private String direccion = "DERECHA";
    private int framesRestantes;

    public MovimientoAleatorio() {
        velocidad = BOTIQUIN_VEL_MIN + azar.nextInt(BOTIQUIN_VEL_MAX - BOTIQUIN_VEL_MIN + 1);
    }

    /** Mueve el botiquín un paso. Se llama una vez por frame. */
    public void mover(Utileria botiquin) {
        if (framesRestantes <= 0 || !puedeIr(direccion, botiquin)) {
            elegirRumbo(botiquin);
        }
        botiquin.mover(direccion, velocidad); // el botiquín sigue moviéndose a sí mismo
        framesRestantes--;
    }

    /** Elige al azar uno de los rumbos permitidos y cuánto tiempo seguirlo. */
    private void elegirRumbo(Utileria botiquin) {
        List<String> posibles = new ArrayList<>();
        for (String d : DIRECCIONES) {
            if (puedeIr(d, botiquin)) {
                posibles.add(d);
            }
        }
        if (!posibles.isEmpty()) {
            direccion = posibles.get(azar.nextInt(posibles.size()));
        }
        framesRestantes = BOTIQUIN_FRAMES_MIN
                + azar.nextInt(BOTIQUIN_FRAMES_MAX - BOTIQUIN_FRAMES_MIN + 1);
    }

    /**
     * Un paso es válido si el botiquín sigue dentro de la zona. Si por algún
     * motivo ya está fuera (por ejemplo, un guardado viejo), se le permite
     * moverse en la dirección que lo regresa a la zona.
     */
    private boolean puedeIr(String dir, Utileria botiquin) {
        int x = botiquin.getPosicionX();
        int y = botiquin.getPosicionY();
        return switch (dir) {
            case "ARRIBA" -> y - velocidad >= ZONA_MIN_Y || y > ZONA_MAX_Y;
            case "ABAJO" -> y + velocidad <= ZONA_MAX_Y || y < ZONA_MIN_Y;
            case "IZQUIERDA" -> x - velocidad >= ZONA_MIN_X || x > ZONA_MAX_X;
            default -> x + velocidad <= ZONA_MAX_X || x < ZONA_MIN_X;
        };
    }
}
