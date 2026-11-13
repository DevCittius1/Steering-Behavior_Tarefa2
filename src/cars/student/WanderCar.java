package cars.student;

import cars.engine.World;

/** Cenario 4 - WANDER: passeio aleatorio, suave e sem destino. */
public class WanderCar extends BaseCar {
    public static final double CRUISE = 160;

    public WanderCar(double x, double y, long seed) {
        super(s -> s.position(x, y).hsb((seed * 0.13f) % 1f, 0.6f, 0.85f).randomOrientation());
        wander = new WanderState(seed);
    }

    @Override
    protected void steer(World world, SteeringMix mix) {
        mix.add("wander", C_WANDER, Steering.wander(this, wander, world.getSecs(), CRUISE), 1.0);
    }
}
