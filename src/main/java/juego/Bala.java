package juego;

import modelo.Arma;

//proyectil en vuelo
public class Bala {
    double x, y, dx, dy, recorrido;
    final Arma arma;

    public Bala(double x, double y, double dx, double dy, Arma arma) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.arma = arma;
    }
}
