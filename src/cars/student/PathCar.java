package cars.student;

import cars.engine.Vector2;
import cars.engine.World;

import java.awt.*;

/** Cenario 6 - PATH FOLLOWING: percorre o circuito dentro do corredor. */
public class PathCar extends BaseCar {
    private final double cruise;

    public PathCar(Vector2 start, double degrees, double cruise, Color color) {
        super(s -> s.position(start).orientationDegrees(degrees).color(color));
        this.cruise = cruise;
    }

    @Override
    protected void steer(World world, SteeringMix mix) {
        mix.add("path", C_PATH, Steering.followPath(this, Arena.path, cruise, pathProbe), 1.0);
    }
}
