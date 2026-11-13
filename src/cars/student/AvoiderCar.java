package cars.student;

import cars.engine.World;

import java.awt.*;

/**
 * Cenario 5 - OBSTACLE AVOIDANCE: o carro apenas "acelera para frente"
 * (cruise) e desvia das pedras. O cruise e so a propulsao; quem decide a
 * trajetoria e o desvio de obstaculos, que tem prioridade.
 */
public class AvoiderCar extends BaseCar {
    public static final double CRUISE = 230;
    /** Peso do desvio (0 desliga; usado no benchmark). */
    public static double W_AVOID = 1.0;

    public AvoiderCar(double x, double y, double degrees, Color color) {
        super(s -> s.position(x, y).orientationDegrees(degrees).color(color));
        usesAvoidance = true;
    }

    @Override
    protected void steer(World world, SteeringMix mix) {
        mix.add("avoid", C_AVOID, Steering.avoidObstacles(this, Arena.obstacles, avoidProbe), W_AVOID);
        mix.add("cruise", C_CRUISE, Steering.cruise(this, CRUISE), 1.0);
    }
}
