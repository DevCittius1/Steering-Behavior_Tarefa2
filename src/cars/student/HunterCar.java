package cars.student;

import cars.engine.Car;
import cars.engine.World;

import java.awt.*;
import java.util.List;

/**
 * Cenario 8 - COMBINACAO B (cacador). Persegue a presa mais proxima.
 * <pre>
 *   prioridade 1: Obstacle Avoidance        peso 2.5
 *   prioridade 2: Separation (so cacadores) peso 1.5  (espalha a matilha)
 *   prioridade 3: Pursuit                   peso 1.0
 * </pre>
 * E um pouco mais lento que a presa (280 vs 330 px/s): so alcanca quem
 * fica encurralado entre pedras ou e pego de surpresa.
 */
public class HunterCar extends BaseCar {
    public HunterCar(double x, double y) {
        super(s -> s.position(x, y).color(new Color(150, 20, 20)).maxSpeed(280).mass(1.3).randomOrientation());
        usesAvoidance = true;
    }

    @Override
    protected void steer(World world, SteeringMix mix) {
        Car prey = null;
        double best = Double.MAX_VALUE;
        for (Car c : world.getNeighbors()) {
            if (c instanceof PreyCar && Car.distance(this, c) < best) {
                best = Car.distance(this, c);
                prey = c;
            }
        }
        List<Car> pack = world.getNeighbors(90).stream().filter(c -> c instanceof HunterCar).toList();
        targetMark = prey == null ? null : prey.getPosition();
        mix.add("avoid", C_AVOID, Steering.avoidObstacles(this, Arena.obstacles, avoidProbe), 2.5);
        mix.add("separation", C_SEPARATION, Steering.separation(this, pack, 90), 1.5);
        mix.add("pursuit", C_PURSUIT, Steering.pursuit(this, prey), 1.0);
    }
}
