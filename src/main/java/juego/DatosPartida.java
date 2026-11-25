package juego;

import java.util.List;
import java.util.Map;

import modelo.CheckPoint;
import modelo.Inventario;
import modelo.Nivel;
import modelo.Obstaculo;
import modelo.Personaje;

/**
 * Foto completa del estado del juego. Es lo que se escribe en partida.sav y
 * lo que se lee de vuelta: así GuardadoPartida no necesita conocer a Juego.
 */
public record DatosPartida(
        Inventario inventario,
        Nivel nivel,
        Personaje jugador,
        Map<Obstaculo, Integer> resistencia,
        List<Drop> drops,
        int puntos,
        int muertes,
        int armaActual,
        String mirada,
        int tick,
        CheckPoint ultimoCheckpoint) { }
