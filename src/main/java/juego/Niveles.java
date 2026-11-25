package juego;

import static juego.Config.*;
import java.util.Map;

import modelo.Arma;
import modelo.Personaje;
import modelo.CheckPoint;
import modelo.Inventario;
import modelo.Nivel;
import modelo.Utileria;
import modelo.Obstaculo;

// Contenido inicial del juego: armas, jugador, obstáculos, checkpoints y botiquines.
public class Niveles {
    private Niveles() {}

    public static Inventario inventarioInicial() {
        Inventario inventario = new Inventario(7);
        inventario.agregarItem(new Arma("Pistola", 10, 200));
        inventario.agregarItem(new Arma("Escopeta", 25, 120));
        inventario.agregarItem(new Arma("Rifle", 20, 250));
        return inventario;
    }

    public static Personaje jugadorInicial() {
        return new Personaje("Jugador", 100, INICIO_X, INICIO_Y);
    }

    // La resistencia de cada obstáculo se guarda en el mapa que recibe.
    public static Nivel arenaDePrueba(Inventario inventario, Map<Obstaculo, Integer> resistencia) {
        Nivel nivel = new Nivel("Arena de prueba", 1, "Fácil", inventario);

        agregarObstaculo(nivel, resistencia, "Cofre Trampa", 5, 200, 120, 30);
        agregarObstaculo(nivel, resistencia, "Púas", 10, 320, 200, 20);
        agregarObstaculo(nivel, resistencia, "Demonio", 15, 500, 120, 40);
        agregarObstaculo(nivel, resistencia, "Caja Trampa", 5, 620, 260, 30);
        agregarObstaculo(nivel, resistencia, "Púas", 10, 250, 380, 20);
        agregarObstaculo(nivel, resistencia, "Demonio", 15, 450, 420, 40);
        agregarObstaculo(nivel, resistencia, "Cofre Trampa", 5, 140, 300, 20);
        agregarObstaculo(nivel, resistencia, "Cofre Trampa", 5, 680, 440, 30);

        nivel.agregarCheckPoint(new CheckPoint("CP1", 60, 400));
        nivel.agregarCheckPoint(new CheckPoint("CP2", 400, 300));
        nivel.agregarCheckPoint(new CheckPoint("CP3", 720, 60));

        nivel.agregarElementoDinamico(new Utileria("Botiquín", "Restaura 25 de vida", 300, 470));
        nivel.agregarElementoDinamico(new Utileria("Botiquín", "Restaura 25 de vida", 450, 220));
        nivel.agregarElementoDinamico(new Utileria("Botiquín", "Restaura 25 de vida", 560, 350));

        return nivel;
    }

    private static void agregarObstaculo(Nivel nivel, Map<Obstaculo, Integer> resistencia,
                                         String nombre, int daño, int x, int y, int resistenciaInicial) {
        Obstaculo o = new Obstaculo(nombre, daño, x, y);
        nivel.agregarObstaculo(o);
        resistencia.put(o, resistenciaInicial);
    }
}
