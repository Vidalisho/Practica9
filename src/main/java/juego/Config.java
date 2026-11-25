package juego;

/** Constantes del juego. */
public final class Config {

    private Config() { }

    // Ventana
    public static final int ANCHO = 800;
    public static final int ALTO = 600;

    // Tamaños de los objetos
    public static final int TAM_JUGADOR = 28;
    public static final int TAM_OBSTACULO = 36;
    public static final int TAM_ITEM = 20;
    public static final int TAM_CHECKPOINT = 24;
    public static final int TAM_UTILERIA = 22;

    // Posición inicial del jugador (también donde reaparece sin checkpoint)
    public static final int INICIO_X = 60;
    public static final int INICIO_Y = 60;

    // Movimiento
    public static final int VELOCIDAD = 3;
    public static final int VELOCIDAD_BALA = 8;

    // Reglas
    public static final int META_PUNTOS = 100;
    public static final int CURACION_BOTIQUIN = 25;
    public static final int FRAMES_INVULNERABLE = 60; // 1 segundo sin recibir daño tras un golpe
    public static final int FRAMES_MENSAJE = 180;     // 3 segundos de aviso en pantalla

    // Recompensas que sueltan los obstáculos (arreglos paralelos)
    public static final String[] RECOMPENSA_NOMBRE = {"Moneda", "Gema", "Llave"};
    public static final int[] RECOMPENSA_VALOR = {10, 25, 15};
    public static final String[] RECOMPENSA_TIPO = {"dinero", "joya", "objeto"};

    // Zona por donde merodean los botiquines (sin tapar el HUD)
    public static final int ZONA_MIN_X = 0;
    public static final int ZONA_MAX_X = ANCHO - TAM_UTILERIA;
    public static final int ZONA_MIN_Y = 60;                        // debajo del texto superior del HUD
    public static final int ZONA_MAX_Y = ALTO - 90 - TAM_UTILERIA;  // encima de los controles del HUD
    public static final int BOTIQUIN_VEL_MIN = 1;                   // píxeles por frame
    public static final int BOTIQUIN_VEL_MAX = 2;
    public static final int BOTIQUIN_FRAMES_MIN = 30;               // cuánto dura cada rumbo
    public static final int BOTIQUIN_FRAMES_MAX = 120;
}