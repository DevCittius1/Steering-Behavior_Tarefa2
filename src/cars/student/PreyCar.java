package cars.student;

import cars.engine.Car;
import cars.engine.World;

import java.awt.*;

/**
 * Cenario 8 - COMBINACAO B (presa). Passeia pelo campo e foge dos cacadores.
 * <pre>
 *   prioridade 1: Obstacle Avoidance  peso 2.5
 *   prioridade 2: Evade (cacador)     peso 1.6  (so dentro do raio de panico)
 *   prioridade 3: Separation          peso 0.8
 *   prioridade 4: Wander              peso 1.0
 * </pre>
 */
public class PreyCar extends BaseCar {
    public static final double PANIC_RADIUS = 190;
    /** Pesos ajustaveis (public para o benchmark poder variar). */
    public static double W_AVOID = 2.5, W_EVADE = 1.6, W_SEPARATION = 0.8, W_WANDER = 1.0;

    public PreyCar(double x, double y, long seed) {
        super(s -> s.position(x, y).color(new Color(60, 180, 75)).maxSpeed(330).randomOrientation());
        wander = new WanderState(seed);
        usesAvoidance = true;
    }

    @Override
    protected void steer(World world, SteeringMix mix) {
        HunterCar nearest = null;
        double best = Double.MAX_VALUE;
        for (Car c : world.getNeighbors()) {
            if (c instanceof HunterCar h && Car.distance(this, h) < best) {
                best = Car.distance(this, h);
                nearest = h;
            }
        }
        targetMark = best < PANIC_RADIUS ? nearest.getPosition() : null;
        mix.add("avoid", C_AVOID, Steering.avoidObstacles(this, Arena.obstacles, avoidProbe), W_AVOID);
        mix.add("evade", C_EVADE, Steering.evade(this, nearest, PANIC_RADIUS), W_EVADE);
        mix.add("separation", C_SEPARATION, Steering.separation(this, world.getNeighbors(40), 40), W_SEPARATION);
        mix.add("wander", C_WANDER, Steering.wander(this, wander, world.getSecs(), 150), W_WANDER);
    }
}
