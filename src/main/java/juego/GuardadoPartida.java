package juego;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import modelo.Arma;
import modelo.CheckPoint;
import modelo.Inventariable;
import modelo.Inventario;
import modelo.Nivel;
import modelo.Obstaculo;
import modelo.Personaje;
import modelo.Recompensa;
import modelo.Utileria;
import modelo.ElementoDinamico;

/**
 * Lee y escribe partida.sav (texto, una línea por objeto, campos separados por |).
 * Solo trabaja con DatosPartida: no sabe nada de cómo funciona el juego.
 */
public final class GuardadoPartida {

    private static final Path ARCHIVO = Path.of("partida.sav");

    private GuardadoPartida() { }

    public static boolean existe() {
        return Files.exists(ARCHIVO);
    }

    /** Escribe el estado completo del juego en partida.sav. */
    public static void guardar(DatosPartida datos) throws IOException {
        List<String> lineas = new ArrayList<>();
        Inventario inventario = datos.inventario();
        Nivel nivel = datos.nivel();
        Personaje jugador = datos.jugador();

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
        lineas.add("ESTADO|" + datos.puntos() + "|" + datos.muertes() + "|" + datos.armaActual()
                + "|" + datos.mirada() + "|" + datos.tick());
        lineas.add("CHECKPOINT_ACTUAL|"
                + (datos.ultimoCheckpoint() == null ? "" : datos.ultimoCheckpoint().getNombre()));

        for (Obstaculo o : nivel.getObstaculos()) {
            lineas.add("OBSTACULO|" + o.getNombre() + "|" + o.getDaño() + "|" + o.getPosicionX()
                    + "|" + o.getPosicionY() + "|" + o.isDestruido() + "|" + datos.resistencia().get(o));
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
        for (Drop d : datos.drops()) {
            lineas.add("DROP|" + d.recompensa().getNombre() + "|" + d.recompensa().getValor()
                    + "|" + d.recompensa().getTipo() + "|" + d.x() + "|" + d.y());
        }

        Files.write(ARCHIVO, lineas);
    }

    /**
     * Reconstruye el estado desde partida.sav. Todo se arma en variables locales
     * y se devuelve hasta el final, así un archivo dañado nunca deja un estado a medias.
     *
     * @throws IOException si no se puede leer el archivo
     * @throws RuntimeException (NumberFormatException, IllegalStateException...) si el contenido es inválido
     */
    public static DatosPartida cargar() throws IOException {
        Inventario inv = null;
        Nivel niv = null;
        Personaje jug = null;
        Map<Obstaculo, Integer> res = new HashMap<>();
        List<Drop> nuevosDrops = new ArrayList<>();
        String nombreCheckpoint = "";
        int pts = 0, mue = 0, arma = 0, tk = 0;
        String mir = "DERECHA";

        for (String linea : Files.readAllLines(ARCHIVO)) {
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

        // El último checkpoint se busca por nombre en el nivel ya reconstruido
        CheckPoint ultimo = null;
        for (CheckPoint cp : niv.getCheckPoints()) {
            if (cp.getNombre().equals(nombreCheckpoint)) {
                ultimo = cp;
            }
        }
        return new DatosPartida(inv, niv, jug, res, nuevosDrops, pts, mue, arma, mir, tk, ultimo);
    }

    public static void borrar() {
        try {
            Files.deleteIfExists(ARCHIVO);
        } catch (IOException ex) {
            System.out.println("No se pudo borrar el guardado: " + ex.getMessage());
        }
    }
}
