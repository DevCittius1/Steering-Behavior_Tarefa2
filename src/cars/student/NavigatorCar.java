package cars.student;

import cars.engine.World;

import java.awt.*;

/**
 * Cenario 9 - COMBINACAO C "Navegador": vai ate o ponto clicado
 * contornando as pedras e para suavemente no destino.
 * <pre>
 *   prioridade 1: Obstacle Avoidance  peso 2.0
 *   prioridade 2: Arrive (clique)     peso 1.0
 * </pre>
 */
public class NavigatorCar extends BaseCar {
    public NavigatorCar(double x, double y) {
        super(s -> s.position(x, y).color(new Color(30, 60, 200)).orientationDegrees(0));
        usesAvoidance = true;
    }

    @Override
    protected void steer(World world, SteeringMix mix) {
        targetMark = world.getClickPos();
        mix.add("avoid", C_AVOID, Steering.avoidObstacles(this, Arena.obstacles, avoidProbe), 2.0);
        mix.add("arrive", C_ARRIVE, Steering.arrive(this, world.getClickPos(), ArriveCar.SLOWING_RADIUS), 1.0);
    }
}
