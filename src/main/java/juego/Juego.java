package juego;

import static juego.Colisiones.choca;
import static juego.Config.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import modelo.Arma;
import modelo.CheckPoint;
import modelo.ElementoDinamico;
import modelo.Inventariable;
import modelo.Inventario;
import modelo.Nivel;
import modelo.Obstaculo;
import modelo.Personaje;
import modelo.Recompensa;
import modelo.Utileria;

/**
 * Las reglas y el estado del juego: mover, disparar, chocar, recoger, vender,
 * morir y ganar. No dibuja (Renderizador), no maneja la ventana (JuegoApp),
 * no lee el teclado de JavaFX (Teclado) y no sabe escribir archivos (GuardadoPartida).
 */
public class Juego {

    private final Teclado teclado;
    private final Random azar = new Random();
    private final Botiquines botiquines = new Botiquines();

    // Estado del juego
    private Nivel nivel;
    private Inventario inventario;
    private Personaje jugador;
    private final Map<Obstaculo, Integer> resistencia = new HashMap<>();
    private final List<Bala> balas = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();

    private CheckPoint ultimoCheckpoint;
    private String mirada = "DERECHA";
    private int armaActual;
    private int enfriamiento;
    private int invulnerable;
    private int tick;
    private int puntos;
    private int muertes;
    private String mensaje = "";
    private int mensajeTimer;
    private boolean victoria;

    public Juego(Teclado teclado) {
        this.teclado = teclado;
    }

    // ------------------------------------------------------------------
    // Inicio de partida
    // ------------------------------------------------------------------

    /** Deja todo en estado inicial (también lo usa "N = nueva partida"). */
    public void iniciarNivel() {
        inventario = Niveles.inventarioInicial();
        resistencia.clear();
        nivel = Niveles.arenaDePrueba(inventario, resistencia);
        jugador = Niveles.jugadorInicial();

        balas.clear();
        botiquines.reiniciar();
        drops.clear();
        ultimoCheckpoint = null;
        mirada = "DERECHA";
        armaActual = 0;
        enfriamiento = 0;
        invulnerable = 0;
        tick = 0;
        puntos = 0;
        muertes = 0;
        victoria = false;
        avisar("Ganas juntando recompensas y vendiéndolas (F) hasta juntar " + META_PUNTOS
                + " puntos o destruyendo todos los obstáculos");
    }

    /** Si hay una partida guardada, la recupera al abrir el juego. */
    public void recuperarSesionAnterior() {
        if (GuardadoPartida.existe() && cargarPartida()) {
            avisar("Sesión anterior recuperada (N = nueva partida)");
        }
    }

    // ------------------------------------------------------------------
    // Acciones del jugador (una sola pulsación)
    // ------------------------------------------------------------------

    public void cambiarArma() {
        List<Arma> armas = armas();
        if (armas.isEmpty()) {
            return;
        }
        armaActual = (armaActual + 1) % armas.size();
        avisar("Arma: " + armas.get(armaActual).getNombre());
    }

    public void usarBotiquin() {
        Utileria usado = botiquines.usarSiToca(nivel, jugador);
        if (usado != null) {
            avisar(usado.getNombre() + " usado: " + usado.getDescripcion());
        }
    }

    /** Elimina la primera recompensa del inventario y suma su valor a los puntos. */
    public void venderRecompensa() {
        for (Inventariable item : inventario.getItems()) {
            if (item instanceof Recompensa r) {
                inventario.eliminarItem(r);
                puntos += r.getValor();
                avisar("Vendiste " + r.getNombre() + " por " + r.getValor() + " puntos");
                if (puntos >= META_PUNTOS) {
                    declararVictoria();
                }
                return; // se modificó la lista: salir inmediatamente
            }
        }
        avisar("No tienes recompensas para vender");
    }

    /** Imprime el estado del nivel en la consola. */
    public void mostrarEstado() {
        nivel.mostrarEstado();
    }

    /** Prueba de Destruible: deja al jugador sin vida (reaparece en el último checkpoint). */
    public void matarJugador() {
        jugador.destruye();
    }

    // ------------------------------------------------------------------
    // Lógica por frame
    // ------------------------------------------------------------------

    public void actualizar() {
        if (victoria) {
            return;
        }
        tick++;
        if (enfriamiento > 0) enfriamiento--;
        if (invulnerable > 0) invulnerable--;
        if (mensajeTimer > 0) mensajeTimer--;

        moverJugador();
        if (teclado.disparo()) {
            disparar();
        }

        botiquines.actualizar(nivel); // cada botiquín se mueve por su cuenta

        actualizarBalas();
        colisionesConObstaculos();
        recogerDrops();
        revisarCheckpoints();
        revisarMuerte();
    }

    private void moverJugador() {
        if (teclado.arriba()) {
            jugador.mover("ARRIBA", VELOCIDAD);
            mirada = "ARRIBA";
        }
        if (teclado.abajo()) {
            jugador.mover("ABAJO", VELOCIDAD);
            mirada = "ABAJO";
        }
        if (teclado.izquierda()) {
            jugador.mover("IZQUIERDA", VELOCIDAD);
            mirada = "IZQUIERDA";
        }
        if (teclado.derecha()) {
            jugador.mover("DERECHA", VELOCIDAD);
            mirada = "DERECHA";
        }
        // Mantener al jugador dentro de la arena
        int x = Math.max(0, Math.min(ANCHO - TAM_JUGADOR, jugador.getPosicionX()));
        int y = Math.max(0, Math.min(ALTO - TAM_JUGADOR, jugador.getPosicionY()));
        jugador.setPosicion(x, y);
    }

    private void disparar() {
        Arma arma = getArmaSeleccionada();
        if (arma == null || enfriamiento > 0) {
            return;
        }
        double cx = jugador.getPosicionX() + TAM_JUGADOR / 2.0; // centro del jugador
        double cy = jugador.getPosicionY() + TAM_JUGADOR / 2.0;
        double dx = 0;
        double dy = 0;
        switch (mirada) {
            case "ARRIBA" -> dy = -1;
            case "ABAJO" -> dy = 1;
            case "IZQUIERDA" -> dx = -1;
            default -> dx = 1;
        }
        balas.add(new Bala(cx, cy, dx, dy, arma));
        enfriamiento = 10 + arma.getDaño(); // armas más fuertes disparan más lento
    }

    private void actualizarBalas() {
        Iterator<Bala> it = balas.iterator();
        while (it.hasNext()) {
            Bala b = it.next();
            b.x += b.dx * VELOCIDAD_BALA;
            b.y += b.dy * VELOCIDAD_BALA;
            b.recorrido += VELOCIDAD_BALA;

            boolean elimina = b.recorrido >= b.arma.getAlcance()
                    || b.x < 0 || b.x > ANCHO || b.y < 0 || b.y > ALTO;

            if (!elimina) {
                for (Obstaculo o : nivel.getObstaculos()) {
                    if (!o.isDestruido()
                            && choca(b.x - 3, b.y - 3, 6, 6,
                            o.getPosicionX(), o.getPosicionY(), TAM_OBSTACULO, TAM_OBSTACULO)) {
                        golpear(o, b.arma.getDaño());
                        elimina = true;
                        break;
                    }
                }
            }
            if (elimina) {
                it.remove();
            }
        }
    }

    /** Resta resistencia al obstáculo; si llega a 0 lo destruye y suelta una recompensa. */
    private void golpear(Obstaculo o, int daño) {
        int restante = resistencia.get(o) - daño;
        resistencia.put(o, restante);
        if (restante <= 0) {
            o.destruye();
            int i = azar.nextInt(RECOMPENSA_NOMBRE.length);
            Recompensa r = new Recompensa(RECOMPENSA_NOMBRE[i], RECOMPENSA_VALOR[i], RECOMPENSA_TIPO[i]);
            drops.add(new Drop(r, o.getPosicionX() + 8, o.getPosicionY() + 8));

            // Se acaba la partida si destruyes todos los obstáculos
            if (nivel.getObstaculos().stream().allMatch(Obstaculo::isDestruido)) {
                declararVictoria();
            }
        }
    }

    /** Daña al jugador si toca un obstáculo (con invulnerabilidad temporal después del golpe). */
    private void colisionesConObstaculos() {
        if (invulnerable > 0) {
            return;
        }
        for (Obstaculo o : nivel.getObstaculos()) {
            if (!o.isDestruido()
                    && jugadorChocaCon(o.getPosicionX(), o.getPosicionY(), TAM_OBSTACULO)) {
                jugador.recibirDaño(o.getDaño());
                invulnerable = FRAMES_INVULNERABLE;
                avisar(o.getNombre() + " te hizo " + o.getDaño() + " de daño");
                return;
            }
        }
    }

    /** Si el jugador toca una recompensa del piso y hay espacio, la guarda en el inventario. */
    private void recogerDrops() {
        Iterator<Drop> it = drops.iterator();
        while (it.hasNext()) {
            Drop d = it.next();
            if (jugadorChocaCon(d.x(), d.y(), TAM_ITEM)) {
                if (inventario.agregarItem(d.recompensa())) {
                    it.remove();
                    avisar("Recogiste: " + d.recompensa().getNombre());
                } else {
                    avisar("Inventario lleno: vende una recompensa con F");
                }
            }
        }
    }

    /** Activa los checkpoints que toque el jugador. */
    private void revisarCheckpoints() {
        for (CheckPoint cp : nivel.getCheckPoints()) {
            if (!cp.isActivado()
                    && jugadorChocaCon(cp.getPosicionX(), cp.getPosicionY(), TAM_CHECKPOINT)) {
                cp.activar();
                ultimoCheckpoint = cp;
                avisar("Checkpoint " + cp.getNombre() + " activado");
            }
        }
    }

    /** Si el jugador murió, lo reaparece en el último checkpoint (o en el inicio) con la vida llena. */
    private void revisarMuerte() {
        if (jugador.getVida() > 0) {
            return;
        }
        muertes++;
        int x = INICIO_X;
        int y = INICIO_Y;
        if (ultimoCheckpoint != null) {
            x = ultimoCheckpoint.getPosicionX();
            y = ultimoCheckpoint.getPosicionY();
        }
        jugador.setPosicion(x, y);
        jugador.curar(jugador.getVidaMaxima());
        avisar("Moriste: reapareces en el último checkpoint");
    }

    private void declararVictoria() {
        victoria = true;
        GuardadoPartida.borrar(); // una partida ganada ya no se recupera
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private void avisar(String texto) {
        mensaje = texto;
        mensajeTimer = FRAMES_MENSAJE;
    }

    /** @return true si el jugador se traslapa con un objeto cuadrado de lado "tam" en (x, y) */
    private boolean jugadorChocaCon(double x, double y, double tam) {
        return choca(jugador.getPosicionX(), jugador.getPosicionY(), TAM_JUGADOR, TAM_JUGADOR,
                x, y, tam, tam);
    }

    /** El inventario guarda Inventariable (armas y recompensas); aquí se filtran solo las armas. */
    private List<Arma> armas() {
        List<Arma> resultado = new ArrayList<>();
        for (Inventariable item : inventario.getItems()) {
            if (item instanceof Arma a) {
                resultado.add(a);
            }
        }
        return resultado;
    }

    // ------------------------------------------------------------------
    // Guardar y cargar (el formato del archivo vive en GuardadoPartida)
    // ------------------------------------------------------------------

    public void guardarPartida() {
        if (victoria) {
            avisar("La partida ya terminó: no hay nada que guardar");
            return;
        }
        try {
            GuardadoPartida.guardar(capturar());
            avisar("Partida guardada");
        } catch (IOException ex) {
            avisar("No se pudo guardar: " + ex.getMessage());
        }
    }

    public boolean cargarPartida() {
        if (!GuardadoPartida.existe()) {
            avisar("No hay partida guardada");
            return false;
        }
        try {
            restaurar(GuardadoPartida.cargar());
            avisar("Partida cargada");
            return true;
        } catch (IOException | RuntimeException ex) {
            avisar("El archivo de guardado está dañado: no se pudo cargar");
            return false;
        }
    }

    /** Toma una "foto" del estado actual. */
    private DatosPartida capturar() {
        return new DatosPartida(inventario, nivel, jugador, resistencia, drops,
                puntos, muertes, armaActual, mirada, tick, ultimoCheckpoint);
    }

    /** Reemplaza el estado actual por el de una "foto" ya validada. */
    private void restaurar(DatosPartida datos) {
        inventario = datos.inventario();
        nivel = datos.nivel();
        jugador = datos.jugador();
        resistencia.clear();
        resistencia.putAll(datos.resistencia());
        drops.clear();
        drops.addAll(datos.drops());
        balas.clear();
        botiquines.reiniciar();
        puntos = datos.puntos();
        muertes = datos.muertes();
        armaActual = datos.armaActual();
        mirada = datos.mirada();
        tick = datos.tick();
        ultimoCheckpoint = datos.ultimoCheckpoint();
        enfriamiento = 0;
        invulnerable = 0;
        victoria = false;
    }

    // ------------------------------------------------------------------
    // Consultas (las usa Renderizador para dibujar)
    // ------------------------------------------------------------------

    public Nivel getNivel() { return nivel; }
    public Inventario getInventario() { return inventario; }
    public Personaje getJugador() { return jugador; }
    public List<Bala> getBalas() { return Collections.unmodifiableList(balas); }
    public List<Drop> getDrops() { return Collections.unmodifiableList(drops); }
    public int getResistencia(Obstaculo o) { return resistencia.getOrDefault(o, 0); }
    public int getPuntos() { return puntos; }
    public int getMuertes() { return muertes; }
    public String getMirada() { return mirada; }
    public int getTick() { return tick; }
    public boolean isInvulnerable() { return invulnerable > 0; }
    public boolean isVictoria() { return victoria; }

    /** @return el aviso actual, o null si ya expiró */
    public String getMensajeActivo() {
        return mensajeTimer > 0 ? mensaje : null;
    }

    /** @return el arma seleccionada, o null si no hay armas en el inventario */
    public Arma getArmaSeleccionada() {
        List<Arma> armas = armas();
        return armas.isEmpty() ? null : armas.get(armaActual % armas.size());
    }
}
