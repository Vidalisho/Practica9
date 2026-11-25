package juego;

import static juego.Config.*;

import java.util.IdentityHashMap;
import java.util.Map;

import modelo.ElementoDinamico;
import modelo.Nivel;
import modelo.Personaje;
import modelo.Utileria;

/**
 * Todo lo relacionado con los botiquines del nivel: moverlos y usarlos.
 * Los botiquines (Utileria) siguen viviendo en el Nivel; esta clase solo les
 * asigna un MovimientoAleatorio propio a cada uno la primera vez que los ve.
 * Por eso funciona igual después de cargar una partida.
 */
public class Botiquines {

    private final Map<Utileria, MovimientoAleatorio> movimientos = new IdentityHashMap<>();

    /** Mueve cada botiquín no usado de forma independiente. Se llama una vez por frame. */
    public void actualizar(Nivel nivel) {
        for (ElementoDinamico e : nivel.getElementosDinamicos()) {
            if (e instanceof Utileria u && !u.isUsada()) {
                movimientos.computeIfAbsent(u, k -> new MovimientoAleatorio()).mover(u);
            }
        }
    }

    /**
     * Usa el primer botiquín que toque el jugador: lo marca como usado y cura.
     *
     * @return el botiquín usado, o null si no había ninguno al alcance
     */
    public Utileria usarSiToca(Nivel nivel, Personaje jugador) {
        for (ElementoDinamico e : nivel.getElementosDinamicos()) {
            if (e instanceof Utileria u && !u.isUsada()
                    && Colisiones.choca(jugador.getPosicionX(), jugador.getPosicionY(), TAM_JUGADOR, TAM_JUGADOR,
                    u.getPosicionX(), u.getPosicionY(), TAM_UTILERIA, TAM_UTILERIA)) {
                u.usar();
                jugador.curar(CURACION_BOTIQUIN);
                return u;
            }
        }
        return null;
    }

    /** Olvida los movimientos (al empezar o cargar una partida los botiquines son otros objetos). */
    public void reiniciar() {
        movimientos.clear();
    }
}
