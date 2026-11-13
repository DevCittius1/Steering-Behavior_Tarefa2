package cars.student;

import cars.engine.World;

import java.awt.*;

import static cars.engine.Vector2.vec2;

/**
 * Cenario 2 - FLEE: foge do cursor quando ele entra no raio de panico.
 * Fora do raio, freia suavemente ate parar (como um animal que se acalma).
 */
public class FleeCar extends BaseCar {
    public static final double PANIC_RADIUS = 200;

    public FleeCar(double x, double y) {
        super(s -> s.position(x, y).color(new Color(210, 60, 60)).randomOrientation());
    }

    @Override
    protected void steer(World world, SteeringMix mix) {
        var flee = Steering.flee(this, world.getMousePos(), PANIC_RADIUS);
        mix.add("flee", C_FLEE, flee, 1.0);
        if (flee.isZero()) {
            mix.add("acalmar", C_CRUISE, Steering.steerToVelocity(this, vec2()), 0.25);
        }
    }
}
