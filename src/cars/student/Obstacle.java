package cars.student;

import cars.engine.Vector2;

/** Obstaculo circular do mapa de testes. */
public record Obstacle(Vector2 center, double radius) {
    public static Obstacle of(double x, double y, double r) {
        return new Obstacle(new Vector2(x, y), r);
    }
}
