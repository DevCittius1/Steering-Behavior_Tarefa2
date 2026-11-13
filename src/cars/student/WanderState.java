package cars.student;

import cars.engine.Vector2;

import java.util.Random;

/** Estado persistente do Wander (cada carro tem o seu). */
public final class WanderState {
    /** Distancia do centro do circulo a frente do carro (px). */
    public double distance = 90;
    /** Raio do circulo de wander (px). */
    public double radius = 45;
    /** Intensidade da perturbacao aleatoria (rad / sqrt(s)). */
    public double jitter = 2.2;

    double angle;
    final Random random;

    // Apenas para desenhar no modo debug
    Vector2 circleCenter;
    Vector2 target;

    public WanderState(long seed) {
        this.random = new Random(seed);
        this.angle = random.nextDouble(-Math.PI, Math.PI) * 0.5;
    }
}
