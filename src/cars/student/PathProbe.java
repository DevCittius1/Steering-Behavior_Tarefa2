package cars.student;

import cars.engine.Vector2;

/** Dados de percepcao do Path Following (apenas para o modo debug). */
public final class PathProbe {
    Vector2 predicted;
    Vector2 normalPoint;
    Vector2 target;
    boolean correcting;
}
