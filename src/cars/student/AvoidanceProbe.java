package cars.student;

/** Dados da caixa de deteccao do Obstacle Avoidance (usados no debug e nos parametros). */
public final class AvoidanceProbe {
    /** Comprimento minimo da caixa de deteccao (px). Cresce ate 2x com a velocidade. */
    public static double MIN_LENGTH = 110;
    /** Meia largura do carro (px) somada ao raio dos obstaculos. */
    public static final double CAR_HALF_WIDTH = 16;

    double length;
    Obstacle hit;
}
