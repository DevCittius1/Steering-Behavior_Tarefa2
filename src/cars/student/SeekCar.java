package cars.student;

import cars.engine.World;

import java.awt.*;

/** Cenario 1 - SEEK: persegue o cursor do mouse a toda velocidade. */
public class SeekCar extends BaseCar {
    public SeekCar(double x, double y, double mass, double maxForce, Color color) {
        super(s -> s.position(x, y).mass(mass).maxForce(maxForce).color(color).randomOrientation());
    }

    @Override
    protected void steer(World world, SteeringMix mix) {
        targetMark = world.getMousePos();
        mix.add("seek", C_SEEK, Steering.seek(this, world.getMousePos()), 1.0);
    }
}
