package cars.student;

import cars.engine.Vector2;
import cars.engine.World;

import java.awt.*;

/**
 * Cenario 7 - COMBINACAO A "Patrulha no circuito".
 * <pre>
 *   prioridade 1: Obstacle Avoidance  peso 2.0  (nunca bater)
 *   prioridade 2: Separation          peso 4.0  (nao encostar nos outros carros)
 *   prioridade 3: Path Following      peso 1.0  (objetivo principal)
 * </pre>
 * Carros com velocidades de cruzeiro diferentes dividem a mesma pista, com
 * pedras sobre ela. Precisam desviar, ultrapassar sem encostar e voltar ao
 * caminho logo depois.
 */
public class PatrolCar extends BaseCar {
    public record Weights(double avoid, double separation, double path) {
        public static final Weights DEFAULT = new Weights(2.0, 4.0, 1.0);
    }

    public static final double SEPARATION_RADIUS = 50;
    private final double cruise;
    private final Weights w;

    public PatrolCar(Vector2 start, double degrees, double cruise, Color color, Weights w) {
        super(s -> s.position(start).orientationDegrees(degrees).color(color));
        this.cruise = cruise;
        this.w = w;
        usesAvoidance = true;
    }

    @Override
    protected void steer(World world, SteeringMix mix) {
        mix.add("avoid", C_AVOID, Steering.avoidObstacles(this, Arena.obstacles, avoidProbe), w.avoid());
        mix.add("separation", C_SEPARATION,
            Steering.separation(this, world.getNeighbors((int) SEPARATION_RADIUS), SEPARATION_RADIUS), w.separation());
        mix.add("path", C_PATH, Steering.followPath(this, Arena.path, cruise, pathProbe), w.path());
    }
}
