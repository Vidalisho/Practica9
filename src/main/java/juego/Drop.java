package juego;

import modelo.Recompensa;

// Recompensa tirada en el piso esperando a ser recogida.
public record Drop(Recompensa recompensa, double x, double y) { }