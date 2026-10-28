package juego;

import javafx.application.Application;

/**
 * Punto de entrada. Se usa una clase aparte que NO extiende Application para
 * evitar el error "JavaFX runtime components are missing" al ejecutar desde IntelliJ.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(JuegoApp.class, args);
    }
}
