package cars.student;

import cars.engine.World;

import java.awt.*;

/** Cenario 3 - ARRIVE: vai ate o ultimo ponto clicado e para suavemente nele. */
public class ArriveCar extends BaseCar {
    public static double SLOWING_RADIUS = 300;

    public ArriveCar(double x, double y) {
        super(s -> s.position(x, y).color(new Color(0, 150, 150)).orientationDegrees(0));
    }

    @Override
    protected void steer(World world, SteeringMix mix) {
        targetMark = world.getClickPos();
        mix.add("arrive", C_ARRIVE, Steering.arrive(this, world.getClickPos(), SLOWING_RADIUS), 1.0);
    }
}
