package juego;

//imports para el savestate
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//imports de javafx
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

//imports de la carpeta de modelo
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

//imports de java necesarios
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class JuegoApp extends Application {

    //datos de ancho y alto para el fondo
    private static final int ANCHO = 800;
    private static final int ALTO = 600;

    //tamanos de los diferentes objetos del entorno
    private static final int TAM_JUGADOR = 28;
    private static final int TAM_OBSTACULO = 36;
    private static final int TAM_ITEM = 20;
    private static final int TAM_CHECKPOINT = 24;
    private static final int TAM_UTILERIA = 22;

    //velocidad de movimiento y de las balas
    private static final int VELOCIDAD = 3;
    private static final int VELOCIDAD_BALA = 8;

    private static final Path ARCHIVO_GUARDADO = Path.of("partida.sav"); //constante del archivo
    private static final int META_PUNTOS = 100;

    // Tipos de recompensa que sueltan los obstáculos al destruirse
    private static final String[] RECOMPENSA_NOMBRE = {"Moneda", "Gema", "Llave"};
    private static final int[] RECOMPENSA_VALOR = {10, 25, 15};
    private static final String[] RECOMPENSA_TIPO = {"dinero", "joya", "objeto"};

    //proyectil en vuelo
    private static class Bala {
        double x, y, dx, dy, recorrido;
        final Arma arma;

        Bala(double x, double y, double dx, double dy, Arma arma) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.arma = arma;
        }
    }

    //Recompensa tirada en el piso esperando a ser recogida.
    private record Drop(Recompensa recompensa, double x, double y) { }

    // Estado del juego
    private Nivel nivel;
    private Inventario inventario;
    private Personaje jugador;
    private final Map<Obstaculo, Integer> resistencia = new HashMap<>();
    private final List<Bala> balas = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();
    private final Set<KeyCode> teclas = new HashSet<>();
    private final Random azar = new Random();

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

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(ANCHO, ALTO);
        Scene scene = new Scene(new StackPane(canvas));
        scene.setOnKeyPressed(e -> {
            teclas.add(e.getCode());
            alPresionar(e.getCode());
        });
        scene.setOnKeyReleased(e -> teclas.remove(e.getCode()));

        stage.setTitle("Práctica 9 - Juego JavaFx");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> guardarPartida());
        stage.setResizable(false);
        stage.show();

        iniciarNivel();
        // checar si el archivo existe y si se puede cargar la partida
        if (Files.exists(ARCHIVO_GUARDADO) && cargarPartida()) {
            avisar("Sesión anterior recuperada (N = nueva partida)");
        }

        GraphicsContext g = canvas.getGraphicsContext2D();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!victoria) {
                    actualizar();
                }
                dibujar(g);
            }
        }.start();
    }

    // Construcción del nivel
    private void iniciarNivel() {
        inventario = new Inventario(7);
        inventario.agregarItem(new Arma("Pistola", 10, 200));
        inventario.agregarItem(new Arma("Escopeta", 25, 120));
        inventario.agregarItem(new Arma("Rifle", 20, 250));

        nivel = new Nivel("Arena de prueba", 1, "Fácil", inventario);
        jugador = new Personaje("Jugador", 100, 60, 60);

        //inicializar los elementos del nivel
        resistencia.clear();
        balas.clear();
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
        avisar("Ganas juntando recompensas y vendiendolas (F) hasta juntar " + META_PUNTOS + " puntos " +
                "o destruyendo todos los obstaculos");

        //crear las posiciones de los obstaculos
        crearObstaculo("Cofre Trampa", 5, 200, 120, 30);
        crearObstaculo("Púas", 10, 320, 200, 20);
        crearObstaculo("Demonio", 15, 500, 120, 40);
        crearObstaculo("Caja Trampa", 5, 620, 260, 30);
        crearObstaculo("Púas", 10, 250, 380, 20);
        crearObstaculo("Demonio", 15, 450, 420, 40);
        crearObstaculo("Cofre Trampa", 5, 140, 300, 20);
        crearObstaculo("Cofre Trampa", 5, 680, 440, 30);

        //crear las posiciones los CheckPoints
        nivel.agregarCheckPoint(new CheckPoint("CP1", 60, 400));
        nivel.agregarCheckPoint(new CheckPoint("CP2", 400, 300));
        nivel.agregarCheckPoint(new CheckPoint("CP3", 720, 60));

        //crear las posiciones los Botiquines
        nivel.agregarElementoDinamico(new Utileria("Botiquín", "Restaura 25 de vida", 300, 530));
        nivel.agregarElementoDinamico(new Utileria("Botiquín", "Restaura 25 de vida", 450, 220));
        nivel.agregarElementoDinamico(new Utileria("Botiquín", "Restaura 25 de vida", 560, 350));
    }

    //metodo para crear los obstaculos y anadirlos a las posiciones asignadas anteriormente
    private void crearObstaculo(String nombre, int daño, int x, int y, int resistenciaInicial) {
        Obstaculo o = new Obstaculo(nombre, daño, x, y);
        nivel.agregarObstaculo(o);
        resistencia.put(o, resistenciaInicial);
    }

    // Entrada del usuario
    // Acciones de una sola pulsación.
    private void alPresionar(KeyCode tecla) {
        switch (tecla) {
            case Q -> cambiarArma();
            case E -> usarBotiquin();
            case F -> venderRecompensa();
            case I -> nivel.mostrarEstado(); // imprime el estado en consola
            case ENTER -> {
                if (victoria) {
                    iniciarNivel();
                }
            }
            case K -> jugador.destruye();
            case F5 -> guardarPartida(); //casos para el guardado y cargado de partida asi como
            case F9 -> cargarPartida(); // para iniciar una nueva
            case N -> iniciarNivel();
            default -> { }
        }
    }

    private List<Arma> armas() {
        List<Arma> resultado = new ArrayList<>();
        for (Inventariable item : inventario.getItems()) {
            if (item instanceof Arma a) {
                resultado.add(a);
            }
        }
        return resultado;
    }

    private void cambiarArma() {
        List<Arma> armas = armas();
        if (armas.isEmpty()) {
            return;
        }
        armaActual = (armaActual + 1) % armas.size();
        avisar("Arma: " + armas.get(armaActual).getNombre());
    }

    private void usarBotiquin() {
        for (ElementoDinamico e : nivel.getElementosDinamicos()) {
            if (e instanceof Utileria u && !u.isUsada()
                    && choca(jugador.getPosicionX(), jugador.getPosicionY(), TAM_JUGADOR, TAM_JUGADOR,
                             u.getPosicionX(), u.getPosicionY(), TAM_UTILERIA, TAM_UTILERIA)) {
                u.usar();
                jugador.curar(25);
                avisar(u.getNombre() + " usado: " + u.getDescripcion());
                return;
            }
        }
    }

    //eliminar las recompensas que tengas en el inventario y sumarle los puntos al contador
    private void venderRecompensa() {
        for (Inventariable item : inventario.getItems()) {
            if (item instanceof Recompensa r) {
                inventario.eliminarItem(r);
                puntos += r.getValor();
                avisar("Vendiste " + r.getNombre() + " por " + r.getValor() + " puntos");
                if (puntos >= META_PUNTOS) {
                    victoria = true;
                }
                return; // se modificó la lista: salir inmediatamente
            }
        }
        avisar("No tienes recompensas para vender");
    }


    // Lógica por frame
    private void actualizar() {
        tick++;
        if (enfriamiento > 0) enfriamiento--;
        if (invulnerable > 0) invulnerable--;
        if (mensajeTimer > 0) mensajeTimer--;

        moverJugador();
        if (teclas.contains(KeyCode.SPACE)) {
            disparar();
        }

        // Los botiquines patrullan 120 px a la derecha y 120 px a la izquierda
        //nivel.moverElementosDinamicos((tick % 240 < 120) ? "DERECHA" : "IZQUIERDA", 1);
        nivel.moverElementosDinamicosRan(tick);

        actualizarBalas();
        colisionesConObstaculos();
        recogerDrops();
        revisarCheckpoints();
        revisarMuerte();
    }

    private void moverJugador() {
        if (teclas.contains(KeyCode.W) || teclas.contains(KeyCode.UP)) {
            jugador.mover("ARRIBA", VELOCIDAD);
            mirada = "ARRIBA";
        }
        if (teclas.contains(KeyCode.S) || teclas.contains(KeyCode.DOWN)) {
            jugador.mover("ABAJO", VELOCIDAD);
            mirada = "ABAJO";
        }
        if (teclas.contains(KeyCode.A) || teclas.contains(KeyCode.LEFT)) {
            jugador.mover("IZQUIERDA", VELOCIDAD);
            mirada = "IZQUIERDA";
        }
        if (teclas.contains(KeyCode.D) || teclas.contains(KeyCode.RIGHT)) {
            jugador.mover("DERECHA", VELOCIDAD);
            mirada = "DERECHA";
        }
        // Mantener al jugador dentro de la arena
        int x = Math.max(0, Math.min(ANCHO - TAM_JUGADOR, jugador.getPosicionX()));
        int y = Math.max(0, Math.min(ALTO - TAM_JUGADOR, jugador.getPosicionY()));
        jugador.setPosicion(x, y);
    }

    private void disparar() {
        List<Arma> armas = armas();
        if (armas.isEmpty() || enfriamiento > 0) {
            return;
        }
        Arma arma = armas.get(armaActual % armas.size());
        double cx = jugador.getPosicionX() + TAM_JUGADOR / 2.0; // Calcula el centro del jugador
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

    private void golpear(Obstaculo o, int daño) {
        int restante = resistencia.get(o) - daño;
        resistencia.put(o, restante);
        if (restante <= 0) {
            o.destruye();
            int rndom = azar.nextInt(RECOMPENSA_NOMBRE.length);
            Recompensa r = new Recompensa(RECOMPENSA_NOMBRE[rndom], RECOMPENSA_VALOR[rndom], RECOMPENSA_TIPO[rndom]);
            drops.add(new Drop(r, o.getPosicionX() + 8, o.getPosicionY() + 8));

            // Se acaba la partida si destruyes todods los obstaculos
            if (nivel.getObstaculos().stream().allMatch(Obstaculo::isDestruido)) {
                victoria = true;
            }
        }
    }

    //triangular las posiciones de los objetos para ver si estan chocando y hacerle dano al jugador
    private void colisionesConObstaculos() {
        if (invulnerable > 0) {
            return;
        }
        for (Obstaculo o : nivel.getObstaculos()) {
            if (!o.isDestruido()
                    && choca(jugador.getPosicionX(), jugador.getPosicionY(), TAM_JUGADOR, TAM_JUGADOR,
                             o.getPosicionX(), o.getPosicionY(), TAM_OBSTACULO, TAM_OBSTACULO)) {
                jugador.recibirDaño(o.getDaño());
                invulnerable = 60;
                avisar(o.getNombre() + " te hizo " + o.getDaño() + " de daño");
                return;
            }
        }
    }

    //revisa si el jugador choca con la recompnesa y checa si ay espacio para guardarla
    private void recogerDrops() {
        Iterator<Drop> it = drops.iterator();
        while (it.hasNext()) {
            Drop d = it.next();
            if (choca(jugador.getPosicionX(), jugador.getPosicionY(), TAM_JUGADOR, TAM_JUGADOR,
                      d.x(), d.y(), TAM_ITEM, TAM_ITEM)) {
                if (inventario.agregarItem(d.recompensa())) {
                    it.remove();
                    avisar("Recogiste: " + d.recompensa().getNombre());
                } else {
                    avisar("Inventario lleno: vende una recompensa con F");
                }
            }
        }
    }

    //revisa si el jugador choca con el checkpoint y si no esta activado lo activa
    private void revisarCheckpoints() {
        for (CheckPoint cp : nivel.getCheckPoints()) {
            if (!cp.isActivado()
                    && choca(jugador.getPosicionX(), jugador.getPosicionY(), TAM_JUGADOR, TAM_JUGADOR,
                             cp.getPosicionX(), cp.getPosicionY(), TAM_CHECKPOINT, TAM_CHECKPOINT)) {
                cp.activar();
                ultimoCheckpoint = cp;
                avisar("Checkpoint " + cp.getNombre() + " activado");
            }
        }
    }

    //revisa si el jugador murio asi como su ultimo checkpoint para respawnearlo, si no hay lo respawnea en la posicion original
    private void revisarMuerte() {
        if (jugador.getVida() > 0) {
            return;
        }
        muertes++;
        int x = 60;
        int y = 60;
        if (ultimoCheckpoint != null) {
            x = ultimoCheckpoint.getPosicionX();
            y = ultimoCheckpoint.getPosicionY();
        }
        jugador.setPosicion(x, y);
        jugador.curar(jugador.getVidaMaxima());
        avisar("Moriste: reapareces en el último checkpoint");
    }

    private void avisar(String texto) {
        mensaje = texto;
        mensajeTimer = 180;
    }

    // se comparan los rectángulos de los objetos para ver si se traslapan
    private boolean choca(double x1, double y1, double w1, double h1,
                          double x2, double y2, double w2, double h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    // Guardar partida
    // este metodo escribe el estado completo del juego en partida.sav una línea por objeto
    private void guardarPartida() {
        if (victoria) {
            avisar("La partida ya terminó: no hay nada que guardar");
            return;
        }
        List<String> lineas = new ArrayList<>();

        // El inventario va primero porque el nivel lo necesita al reconstruirse
        lineas.add("INVENTARIO|" + inventario.getCapacidadMaxima());
        for (Inventariable item : inventario.getItems()) {
            if (item instanceof Arma a) {
                lineas.add("ARMA|" + a.getNombre() + "|" + a.getDaño() + "|" + a.getAlcance());
            } else if (item instanceof Recompensa r) {
                lineas.add("RECOMPENSA|" + r.getNombre() + "|" + r.getValor() + "|" + r.getTipo());
            }
        }

        lineas.add("NIVEL|" + nivel.getNombre() + "|" + nivel.getNumero() + "|" + nivel.getDificultad());
        lineas.add("JUGADOR|" + jugador.getNombre() + "|" + jugador.getVida() + "|" + jugador.getVidaMaxima()
                + "|" + jugador.getPosicionX() + "|" + jugador.getPosicionY());
        lineas.add("ESTADO|" + puntos + "|" + muertes + "|" + armaActual + "|" + mirada + "|" + tick);
        lineas.add("CHECKPOINT_ACTUAL|" + (ultimoCheckpoint == null ? "" : ultimoCheckpoint.getNombre()));

        for (Obstaculo o : nivel.getObstaculos()) {
            lineas.add("OBSTACULO|" + o.getNombre() + "|" + o.getDaño() + "|" + o.getPosicionX()
                    + "|" + o.getPosicionY() + "|" + o.isDestruido() + "|" + resistencia.get(o));
        }
        for (CheckPoint cp : nivel.getCheckPoints()) {
            lineas.add("CHECKPOINT|" + cp.getNombre() + "|" + cp.getPosicionX() + "|" + cp.getPosicionY()
                    + "|" + cp.isActivado());
        }
        for (ElementoDinamico e : nivel.getElementosDinamicos()) {
            if (e instanceof Utileria u) {
                lineas.add("UTILERIA|" + u.getNombre() + "|" + u.getDescripcion() + "|" + u.getPosicionX()
                        + "|" + u.getPosicionY() + "|" + u.isUsada());
            }
        }
        for (Drop d : drops) {
            lineas.add("DROP|" + d.recompensa().getNombre() + "|" + d.recompensa().getValor()
                    + "|" + d.recompensa().getTipo() + "|" + d.x() + "|" + d.y());
        }

        try {
            Files.write(ARCHIVO_GUARDADO, lineas);
            avisar("Partida guardada");
        } catch (IOException ex) {
            avisar("No se pudo guardar: " + ex.getMessage());
        }
    }

    // Cargar la partida
    /* Reconstruye el juego desde partida.sav armando todo en variables locales
       el estado actual solo se reemplaza si el archivo se leyo completo,
       para que un archivo danado no corrompa la partida */
    private boolean cargarPartida() {
        if (!Files.exists(ARCHIVO_GUARDADO)) {
            avisar("No hay partida guardada");
            return false;
        }
        try {
            Inventario inv = null;
            Nivel niv = null;
            Personaje jug = null;
            Map<Obstaculo, Integer> res = new HashMap<>();
            List<Drop> nuevosDrops = new ArrayList<>();
            String nombreCheckpoint = "";
            int pts = 0, mue = 0, arma = 0, tk = 0;
            String mir = "DERECHA";

            for (String linea : Files.readAllLines(ARCHIVO_GUARDADO)) {
                String[] p = linea.split("\\|", -1);
                switch (p[0]) {
                    case "INVENTARIO" -> inv = new Inventario(Integer.parseInt(p[1]));
                    case "ARMA" -> inv.agregarItem(new Arma(p[1], Integer.parseInt(p[2]), Double.parseDouble(p[3])));
                    case "RECOMPENSA" -> inv.agregarItem(new Recompensa(p[1], Integer.parseInt(p[2]), p[3]));
                    case "NIVEL" -> niv = new Nivel(p[1], Integer.parseInt(p[2]), p[3], inv);
                    case "JUGADOR" -> {
                        int max = Integer.parseInt(p[3]);
                        jug = new Personaje(p[1], max, Integer.parseInt(p[4]), Integer.parseInt(p[5]));
                        jug.recibirDaño(max - Integer.parseInt(p[2])); // deja la vida como estaba
                    }
                    case "ESTADO" -> {
                        pts = Integer.parseInt(p[1]);
                        mue = Integer.parseInt(p[2]);
                        arma = Integer.parseInt(p[3]);
                        mir = p[4];
                        tk = Integer.parseInt(p[5]);
                    }
                    case "CHECKPOINT_ACTUAL" -> nombreCheckpoint = p[1];
                    case "OBSTACULO" -> {
                        Obstaculo o = new Obstaculo(p[1], Integer.parseInt(p[2]),
                                Integer.parseInt(p[3]), Integer.parseInt(p[4]));
                        if (Boolean.parseBoolean(p[5])) {
                            o.destruye();
                        }
                        niv.agregarObstaculo(o);
                        res.put(o, Integer.parseInt(p[6]));
                    }
                    case "CHECKPOINT" -> {
                        CheckPoint cp = new CheckPoint(p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                        if (Boolean.parseBoolean(p[4])) {
                            cp.activar();
                        }
                        niv.agregarCheckPoint(cp);
                    }
                    case "UTILERIA" -> {
                        Utileria u = new Utileria(p[1], p[2], Integer.parseInt(p[3]), Integer.parseInt(p[4]));
                        if (Boolean.parseBoolean(p[5])) {
                            u.usar();
                        }
                        niv.agregarElementoDinamico(u);
                    }
                    case "DROP" -> nuevosDrops.add(new Drop(
                            new Recompensa(p[1], Integer.parseInt(p[2]), p[3]),
                            Double.parseDouble(p[4]), Double.parseDouble(p[5])));
                    default -> { }
                }
            }
            if (inv == null || niv == null || jug == null) {
                throw new IllegalStateException("Archivo incompleto");
            }

            // Si salio bien ahora se reemplaza el estado del juego
            inventario = inv;
            nivel = niv;
            jugador = jug;
            resistencia.clear();
            resistencia.putAll(res);
            drops.clear();
            drops.addAll(nuevosDrops);
            balas.clear();
            puntos = pts;
            muertes = mue;
            armaActual = arma;
            mirada = mir;
            tick = tk;
            enfriamiento = 0;
            invulnerable = 0;
            victoria = false;
            ultimoCheckpoint = null;
            for (CheckPoint cp : nivel.getCheckPoints()) {
                if (cp.getNombre().equals(nombreCheckpoint)) {
                    ultimoCheckpoint = cp;
                }
            }
            avisar("Partida cargada");
            return true;
        } catch (IOException | RuntimeException ex) {
            avisar("El archivo de guardado está dañado: no se pudo cargar");
            return false;
        }
    }

    // Borrar el guardado
    private void borrarGuardado() {
        try {
            Files.deleteIfExists(ARCHIVO_GUARDADO);
        } catch (IOException ex) {
            System.out.println("No se pudo borrar el guardado: " + ex.getMessage());
        }
    }

    // Dibujo
    private void dibujar(GraphicsContext g) {
        // Piso con cuadrícula
        g.setFill(Color.web("#1e2430"));
        g.fillRect(0, 0, ANCHO, ALTO);
        g.setStroke(Color.web("#2a3242"));
        g.setLineWidth(1);
        for (int x = 0; x <= ANCHO; x += 40) g.strokeLine(x, 0, x, ALTO);
        for (int y = 0; y <= ALTO; y += 40) g.strokeLine(0, y, ANCHO, y);

        g.setFont(Font.font(11));

        // Checkpoints
        for (CheckPoint cp : nivel.getCheckPoints()) {
            g.setFill(cp.isActivado() ? Color.web("#3ddc84") : Color.web("#5b6b82"));
            g.fillRect(cp.getPosicionX(), cp.getPosicionY(), TAM_CHECKPOINT, TAM_CHECKPOINT);
            g.setFill(Color.WHITE);
            g.fillText(cp.getNombre(), cp.getPosicionX(), cp.getPosicionY() - 3);
        }

        // Obstáculos
        for (Obstaculo o : nivel.getObstaculos()) {
            if (o.isDestruido()) continue;
            g.setFill(Color.web("#b5523b"));
            g.fillRect(o.getPosicionX(), o.getPosicionY(), TAM_OBSTACULO, TAM_OBSTACULO);
            g.setFill(Color.WHITE);
            g.fillText(o.getNombre(), o.getPosicionX(), o.getPosicionY() - 3);
            g.fillText("HP " + resistencia.get(o), o.getPosicionX() + 4, o.getPosicionY() + 22);
        }

        // Recompensas en el piso
        for (Drop d : drops) {
            g.setFill(Color.GOLD);
            g.fillOval(d.x(), d.y(), TAM_ITEM, TAM_ITEM);
            g.setFill(Color.WHITE);
            g.fillText(d.recompensa().getNombre(), d.x() - 4, d.y() - 3);
        }

        // Utilería (botiquines)
        for (ElementoDinamico e : nivel.getElementosDinamicos()) {
            if (e instanceof Utileria u && !u.isUsada()) {
                g.setFill(Color.web("#e8e8e8"));
                g.fillRect(u.getPosicionX(), u.getPosicionY(), TAM_UTILERIA, TAM_UTILERIA);
                g.setFill(Color.RED);
                g.fillRect(u.getPosicionX() + 9, u.getPosicionY() + 4, 4, 14);
                g.fillRect(u.getPosicionX() + 4, u.getPosicionY() + 9, 14, 4);
            }
        }

        // Balas
        g.setFill(Color.YELLOW);
        for (Bala b : balas) {
            g.fillOval(b.x - 3, b.y - 3, 6, 6);
        }

        // Jugador (parpadea cuando es invulnerable)
        if (invulnerable == 0 || (tick / 4) % 2 == 0) {
            int px = jugador.getPosicionX();
            int py = jugador.getPosicionY();
            g.setFill(Color.web("#4aa3ff"));
            g.fillRect(px, py, TAM_JUGADOR, TAM_JUGADOR);
            g.setFill(Color.WHITE);
            switch (mirada) {
                case "ARRIBA" -> g.fillRect(px + 10, py, 8, 6);
                case "ABAJO" -> g.fillRect(px + 10, py + TAM_JUGADOR - 6, 8, 6);
                case "IZQUIERDA" -> g.fillRect(px, py + 10, 6, 8);
                default -> g.fillRect(px + TAM_JUGADOR - 6, py + 10, 6, 8);
            }
        }

        dibujarHud(g);

        if (victoria) {
            g.setFill(Color.rgb(0, 0, 0, 0.75));
            g.fillRect(0, 0, ANCHO, ALTO);
            g.setFill(Color.WHITE);
            g.setFont(Font.font(36));
            g.fillText("¡NIVEL COMPLETADO!", 220, 270);
            g.setFont(Font.font(18));
            g.fillText("Puntos: " + puntos + "   Muertes: " + muertes, 285, 310);
            g.fillText("ENTER para jugar de nuevo", 275, 345);
        }
    }

    //es el hud que se muestra en pantalla para el jugador
    private void dibujarHud(GraphicsContext g) {
        // Barra de vida
        g.setFill(Color.web("#5a1d1d"));
        g.fillRect(20, 15, 200, 16);
        g.setFill(Color.web("#3ddc84"));
        g.fillRect(20, 15, 200.0 * jugador.getVida() / jugador.getVidaMaxima(), 16);
        g.setFill(Color.WHITE);
        g.setFont(Font.font(13));
        g.fillText("Vida " + jugador.getVida() + "/" + jugador.getVidaMaxima(), 28, 28);
        g.fillText("Puntos: " + puntos + "/" + META_PUNTOS + "   Muertes: " + muertes, 240, 28);

        if (mensajeTimer > 0) {
            g.setFill(Color.web("#ffd34d"));
            g.fillText(mensaje, 20, 52);
        }

        // Controles
        g.setFill(Color.web("#9aa7bd"));
        g.setFont(Font.font(11));
        g.fillText("WASD/Flechas mover | ESPACIO disparar | Q cambiar arma | E usar botiquín | F vender | I estado en consola",
                20, ALTO - 62);
        g.fillText("F5 guardar | F9 cargar | N nueva partida", 20, ALTO - 74); //mostrar las teclas para la partida
                                                                                            // cargar, guardar, nueva partida

        // Ranuras del inventario
        List<Inventariable> items = inventario.getItems();
        List<Arma> armas = armas();
        for (int i = 0; i < inventario.getCapacidadMaxima(); i++) {
            int sx = 20 + i * 105;
            int sy = ALTO - 52;
            g.setFill(Color.web("#2d3646"));
            g.fillRect(sx, sy, 100, 38);
            Inventariable item = i < items.size() ? items.get(i) : null;
            boolean esActual = item instanceof Arma a && !armas.isEmpty()
                    && a == armas.get(armaActual % armas.size());
            g.setStroke(esActual ? Color.GOLD : Color.web("#4a566b"));
            g.setLineWidth(esActual ? 2 : 1);
            g.strokeRect(sx, sy, 100, 38);
            if (item != null) {
                g.setFill(Color.WHITE);
                String texto;
                if (item instanceof Arma a) {
                    texto = a.getNombre() + " d" + a.getDaño();
                } else if (item instanceof Recompensa r) {
                    texto = r.getNombre() + " $" + r.getValor();
                } else {
                    texto = item.toString();
                }
                g.fillText(texto, sx + 6, sy + 23);
            }
        }
    }
}
