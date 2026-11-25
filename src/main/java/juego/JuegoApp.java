package juego;

import static juego.Config.*;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Punto de unión con JavaFX: crea la ventana, conecta el teclado y ejecuta el
 * ciclo del juego (actualizar + dibujar) unas 60 veces por segundo.
 */
public class JuegoApp extends Application {

    private final Teclado teclado = new Teclado();
    private final Renderizador renderizador = new Renderizador();
    private Juego juego;

    @Override
    public void start(Stage stage) {
        juego = new Juego(teclado);
        juego.iniciarNivel();

        Canvas canvas = new Canvas(ANCHO, ALTO);
        Scene scene = new Scene(new StackPane(canvas));
        scene.setOnKeyPressed(e -> {
            teclado.presionar(e.getCode());
            alPresionar(e.getCode());
        });
        scene.setOnKeyReleased(e -> teclado.soltar(e.getCode()));

        stage.setTitle("Práctica 9 - Juego JavaFx");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> juego.guardarPartida());
        stage.setResizable(false);
        stage.show();

        juego.recuperarSesionAnterior();

        GraphicsContext g = canvas.getGraphicsContext2D();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                juego.actualizar();
                renderizador.dibujar(g, juego);
            }
        }.start();
    }

    /** Traduce las teclas de una sola pulsación a acciones del juego. */
    private void alPresionar(KeyCode tecla) {
        switch (tecla) {
            case Q -> juego.cambiarArma();
            case E -> juego.usarBotiquin();
            case F -> juego.venderRecompensa();
            case I -> juego.mostrarEstado();
            case ENTER -> {
                if (juego.isVictoria()) {
                    juego.iniciarNivel();
                }
            }
            case K -> juego.matarJugador();
            case F5 -> juego.guardarPartida();
            case F9 -> juego.cargarPartida();
            case N -> juego.iniciarNivel();
            default -> { }
        }
    }
}
