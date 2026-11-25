package juego;

import static juego.Config.*;

import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import modelo.Arma;
import modelo.CheckPoint;
import modelo.ElementoDinamico;
import modelo.Inventariable;
import modelo.Inventario;
import modelo.Obstaculo;
import modelo.Personaje;
import modelo.Recompensa;
import modelo.Utileria;

/**
 * Se encarga solo de dibujar. El Canvas no recuerda nada, así que en cada frame
 * se redibuja todo por capas, de atrás hacia adelante. Solo lee el estado de Juego.
 */
public class Renderizador {

    public void dibujar(GraphicsContext g, Juego juego) {
        dibujarPiso(g);
        g.setFont(Font.font(11));
        dibujarCheckpoints(g, juego);
        dibujarObstaculos(g, juego);
        dibujarDrops(g, juego);
        dibujarBotiquines(g, juego);
        dibujarBalas(g, juego);
        dibujarJugador(g, juego);
        dibujarHud(g, juego);
        if (juego.isVictoria()) {
            dibujarVictoria(g, juego);
        }
    }

    private void dibujarPiso(GraphicsContext g) {
        g.setFill(Color.web("#1e2430"));
        g.fillRect(0, 0, ANCHO, ALTO);
        g.setStroke(Color.web("#2a3242"));
        g.setLineWidth(1);
        for (int x = 0; x <= ANCHO; x += 40) g.strokeLine(x, 0, x, ALTO);
        for (int y = 0; y <= ALTO; y += 40) g.strokeLine(0, y, ANCHO, y);
    }

    private void dibujarCheckpoints(GraphicsContext g, Juego juego) {
        for (CheckPoint cp : juego.getNivel().getCheckPoints()) {
            g.setFill(cp.isActivado() ? Color.web("#3ddc84") : Color.web("#5b6b82"));
            g.fillRect(cp.getPosicionX(), cp.getPosicionY(), TAM_CHECKPOINT, TAM_CHECKPOINT);
            g.setFill(Color.WHITE);
            g.fillText(cp.getNombre(), cp.getPosicionX(), cp.getPosicionY() - 3);
        }
    }

    private void dibujarObstaculos(GraphicsContext g, Juego juego) {
        for (Obstaculo o : juego.getNivel().getObstaculos()) {
            if (o.isDestruido()) continue;
            g.setFill(Color.web("#b5523b"));
            g.fillRect(o.getPosicionX(), o.getPosicionY(), TAM_OBSTACULO, TAM_OBSTACULO);
            g.setFill(Color.WHITE);
            g.fillText(o.getNombre(), o.getPosicionX(), o.getPosicionY() - 3);
            g.fillText("HP " + juego.getResistencia(o), o.getPosicionX() + 4, o.getPosicionY() + 22);
        }
    }

    private void dibujarDrops(GraphicsContext g, Juego juego) {
        for (Drop d : juego.getDrops()) {
            g.setFill(Color.GOLD);
            g.fillOval(d.x(), d.y(), TAM_ITEM, TAM_ITEM);
            g.setFill(Color.WHITE);
            g.fillText(d.recompensa().getNombre(), d.x() - 4, d.y() - 3);
        }
    }

    private void dibujarBotiquines(GraphicsContext g, Juego juego) {
        for (ElementoDinamico e : juego.getNivel().getElementosDinamicos()) {
            if (e instanceof Utileria u && !u.isUsada()) {
                g.setFill(Color.web("#e8e8e8"));
                g.fillRect(u.getPosicionX(), u.getPosicionY(), TAM_UTILERIA, TAM_UTILERIA);
                g.setFill(Color.RED);
                g.fillRect(u.getPosicionX() + 9, u.getPosicionY() + 4, 4, 14);
                g.fillRect(u.getPosicionX() + 4, u.getPosicionY() + 9, 14, 4);
            }
        }
    }

    private void dibujarBalas(GraphicsContext g, Juego juego) {
        g.setFill(Color.YELLOW);
        for (Bala b : juego.getBalas()) {
            g.fillOval(b.x - 3, b.y - 3, 6, 6);
        }
    }

    /** El jugador parpadea mientras es invulnerable. */
    private void dibujarJugador(GraphicsContext g, Juego juego) {
        if (juego.isInvulnerable() && (juego.getTick() / 4) % 2 != 0) {
            return;
        }
        Personaje jugador = juego.getJugador();
        int px = jugador.getPosicionX();
        int py = jugador.getPosicionY();
        g.setFill(Color.web("#4aa3ff"));
        g.fillRect(px, py, TAM_JUGADOR, TAM_JUGADOR);
        g.setFill(Color.WHITE); // cuadrito que indica hacia dónde mira
        switch (juego.getMirada()) {
            case "ARRIBA" -> g.fillRect(px + 10, py, 8, 6);
            case "ABAJO" -> g.fillRect(px + 10, py + TAM_JUGADOR - 6, 8, 6);
            case "IZQUIERDA" -> g.fillRect(px, py + 10, 6, 8);
            default -> g.fillRect(px + TAM_JUGADOR - 6, py + 10, 6, 8);
        }
    }

    private void dibujarHud(GraphicsContext g, Juego juego) {
        Personaje jugador = juego.getJugador();
        Inventario inventario = juego.getInventario();

        // Barra de vida
        g.setFill(Color.web("#5a1d1d"));
        g.fillRect(20, 15, 200, 16);
        g.setFill(Color.web("#3ddc84"));
        g.fillRect(20, 15, 200.0 * jugador.getVida() / jugador.getVidaMaxima(), 16);
        g.setFill(Color.WHITE);
        g.setFont(Font.font(13));
        g.fillText("Vida " + jugador.getVida() + "/" + jugador.getVidaMaxima(), 28, 28);
        g.fillText("Puntos: " + juego.getPuntos() + "/" + META_PUNTOS
                + "   Muertes: " + juego.getMuertes(), 240, 28);

        String mensaje = juego.getMensajeActivo();
        if (mensaje != null) {
            g.setFill(Color.web("#ffd34d"));
            g.fillText(mensaje, 20, 52);
        }

        // Controles
        g.setFill(Color.web("#9aa7bd"));
        g.setFont(Font.font(11));
        g.fillText("WASD/Flechas mover | ESPACIO disparar | Q cambiar arma | E usar botiquín | F vender | I estado en consola",
                20, ALTO - 62);
        g.fillText("F5 guardar | F9 cargar | N nueva partida", 20, ALTO - 74);

        // Ranuras del inventario
        List<Inventariable> items = inventario.getItems();
        Arma seleccionada = juego.getArmaSeleccionada();
        for (int i = 0; i < inventario.getCapacidadMaxima(); i++) {
            int sx = 20 + i * 105;
            int sy = ALTO - 52;
            g.setFill(Color.web("#2d3646"));
            g.fillRect(sx, sy, 100, 38);
            Inventariable item = i < items.size() ? items.get(i) : null;
            boolean esActual = item instanceof Arma a && a == seleccionada;
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

    private void dibujarVictoria(GraphicsContext g, Juego juego) {
        g.setFill(Color.rgb(0, 0, 0, 0.75));
        g.fillRect(0, 0, ANCHO, ALTO);
        g.setFill(Color.WHITE);
        g.setFont(Font.font(36));
        g.fillText("¡NIVEL COMPLETADO!", 220, 270);
        g.setFont(Font.font(18));
        g.fillText("Puntos: " + juego.getPuntos() + "   Muertes: " + juego.getMuertes(), 285, 310);
        g.fillText("ENTER para jugar de nuevo", 275, 345);
    }
}
