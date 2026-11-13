package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;

import java.util.List;

import static cars.engine.Vector2.*;

/**
 * Biblioteca de Steering Behaviors (Reynolds, 1999).
 * <p>
 * Todos os metodos sao "puros": recebem o carro e o que ele percebe do mundo
 * e devolvem uma FORCA de steering (em unidades da engine, limitada depois por
 * maxForce). Nenhum metodo altera o carro. Assim cada comportamento pode ser
 * testado sozinho e depois combinado com pesos em {@link SteeringMix}.
 * <p>
 * Ideia central de Reynolds: steering = velocidadeDesejada - velocidadeAtual.
 * Aqui multiplicamos essa diferenca por (massa / TEMPO_DE_REACAO): e a forca
 * que corrigiria a velocidade em TEMPO_DE_REACAO segundos. Sem esse fator a
 * engine (a = F*dt/m) levaria cerca de 1 s para reagir, o que parece "lento".
 */
public final class Steering {
    /** Segundos que o carro "deseja" levar para alcancar a velocidade desejada. */
    public static final double REACTION_TIME = 0.25;

    private Steering() { }

    // ---------------------------------------------------------------------
    // Utilitario comum
    // ---------------------------------------------------------------------

    /** Forca necessaria para levar a velocidade atual ate {@code desired}. */
    public static Vector2 steerToVelocity(Car car, Vector2 desired) {
        return subtract(desired, car.getVelocity()).multiply(car.getMass() / REACTION_TIME);
    }

    /** Mantem a direcao atual, ajustando apenas o modulo da velocidade. */
    public static Vector2 cruise(Car car, double speed) {
        return steerToVelocity(car, multiply(car.getDirection(), speed));
    }

    // ---------------------------------------------------------------------
    // 1) SEEK - ir em direcao ao alvo a toda velocidade
    // ---------------------------------------------------------------------
    public static Vector2 seek(Car car, Vector2 target) {
        return seek(car, target, car.getMaxSpeed());
    }

    public static Vector2 seek(Car car, Vector2 target, double speed) {
        if (target == null) return vec2();
        var toTarget = subtract(target, car.getPosition());
        if (toTarget.isZero()) return vec2();
        var desired = toTarget.resize(speed);
        return steerToVelocity(car, desired);
    }

    // ---------------------------------------------------------------------
    // 2) FLEE - o oposto do seek: velocidade desejada aponta PARA LONGE da ameaca.
    //    Com panicRadius > 0 a fuga so acontece quando a ameaca esta perto;
    //    fora do raio o comportamento devolve zero (nao interfere).
    // ---------------------------------------------------------------------
    public static Vector2 flee(Car car, Vector2 threat) {
        return flee(car, threat, Double.POSITIVE_INFINITY);
    }

    public static Vector2 flee(Car car, Vector2 threat, double panicRadius) {
        if (threat == null) return vec2();
        var away = subtract(car.getPosition(), threat);
        if (away.size() > panicRadius) return vec2();
        if (away.isZero()) away = car.getDirection();
        var desired = away.resize(car.getMaxSpeed());
        return steerToVelocity(car, desired);
    }

    // ---------------------------------------------------------------------
    // 3) ARRIVE - seek com desaceleracao: dentro do raio de frenagem a
    //    velocidade desejada cai linearmente ate zero no alvo.
    // ---------------------------------------------------------------------
    public static Vector2 arrive(Car car, Vector2 target, double slowingRadius) {
        if (target == null) return vec2();
        var toTarget = subtract(target, car.getPosition());
        var dist = toTarget.size();
        if (dist < 1.0) {
            // chegou: freia ate parar
            return steerToVelocity(car, vec2());
        }
        var speed = car.getMaxSpeed() * Math.min(1.0, dist / slowingRadius);
        var desired = toTarget.resize(speed);
        return steerToVelocity(car, desired);
    }

    // ---------------------------------------------------------------------
    // 4) WANDER - passeio aleatorio suave. Um alvo se move sobre um circulo
    //    projetado a frente do carro; o angulo do alvo sofre pequenas
    //    perturbacoes aleatorias a cada quadro (passeio aleatorio).
    // ---------------------------------------------------------------------
    public static Vector2 wander(Car car, WanderState w, double dt, double speed) {
        // Perturbacao proporcional a sqrt(dt): mantem a "aleatoriedade por segundo"
        // independente da taxa de quadros (passeio aleatorio / movimento browniano).
        w.angle += w.random.nextGaussian() * w.jitter * Math.sqrt(dt);

        var dir = car.getDirection();
        w.circleCenter = add(car.getPosition(), multiply(dir, w.distance));
        w.target = add(w.circleCenter, rotate(dir, w.angle).multiply(w.radius));
        return seek(car, w.target, speed);
    }

    // ---------------------------------------------------------------------
    // 5) PURSUIT / EVADE - seek/flee na posicao FUTURA estimada do outro carro.
    // ---------------------------------------------------------------------
    public static Vector2 pursuit(Car car, Car quarry) {
        if (quarry == null) return vec2();
        return seek(car, predictPosition(car, quarry));
    }

    public static Vector2 evade(Car car, Car threat, double panicRadius) {
        if (threat == null) return vec2();
        if (Car.distance(car, threat) > panicRadius) return vec2();
        return flee(car, predictPosition(car, threat));
    }

    private static Vector2 predictPosition(Car self, Car other) {
        var dist = Car.distance(self, other);
        // tempo de previsao: quanto mais longe, mais a frente olhamos (limitado)
        var t = Math.min(dist / Math.max(self.getMaxSpeed(), 1.0), 1.0);
        return add(other.getPosition(), multiply(other.getVelocity(), t));
    }

    // ---------------------------------------------------------------------
    // 6) OBSTACLE AVOIDANCE - "caixa de deteccao" (Reynolds 1999 / Buckland 2005)
    //    Um retangulo a frente do carro, com comprimento proporcional a
    //    velocidade, procura o obstaculo circular mais proximo que o intercepta.
    //    A forca gerada tem duas componentes, no espaco local do carro:
    //     - lateral: empurra para o lado oposto ao centro do obstaculo, mais
    //       forte quanto mais perto e quanto mais centralizado o obstaculo;
    //     - frenagem: reduz a velocidade quando o obstaculo esta perto.
    // ---------------------------------------------------------------------
    /** Fracao minima da forca lateral (mesmo para obstaculos que mal tocam a caixa). */
    public static double AVOID_LATERAL_FLOOR = 0.35;
    /** Intensidade da frenagem do desvio (fracao de maxForce). */
    public static double AVOID_BRAKE = 0.35;

    public static Vector2 avoidObstacles(Car car, List<Obstacle> obstacles, AvoidanceProbe probe) {
        var pos = car.getPosition();
        var forward = car.getDirection();
        var side = perp(forward);
        var speedRatio = car.getSpeed() / car.getMaxSpeed();
        var boxLength = AvoidanceProbe.MIN_LENGTH * (1.0 + speedRatio);

        Obstacle closest = null;
        double closestX = Double.MAX_VALUE, closestY = 0, closestR = 0;

        for (var o : obstacles) {
            var rel = subtract(o.center(), pos);
            var expanded = o.radius() + AvoidanceProbe.CAR_HALF_WIDTH;

            // Ja esta dentro (colisao): empurra radialmente para fora com forca maxima
            if (rel.size() < expanded * 0.9) {
                if (probe != null) { probe.hit = o; probe.length = boxLength; }
                var out = rel.isZero() ? negate(forward) : negate(rel);
                return out.resize(car.getMaxForce() * 2);
            }

            // Transforma para o espaco local: x = a frente, y = ao lado
            var x = rel.dot(forward);
            var y = rel.dot(side);
            if (x + expanded < 0) continue;              // atras do carro
            if (x - expanded > boxLength) continue;      // alem da caixa
            if (Math.abs(y) >= expanded) continue;       // nao cruza a caixa

            // Ponto de intersecao mais proximo ao longo do eixo x
            var sq = Math.sqrt(expanded * expanded - y * y);
            var ip = x - sq;
            if (ip <= 0) ip = x + sq;
            if (ip < closestX) {
                closestX = ip; closestY = y; closestR = expanded; closest = o;
            }
        }

        if (probe != null) { probe.hit = closest; probe.length = boxLength; }
        if (closest == null) return vec2();

        // Urgencia: 1 (no fim da caixa) ... 2 (encostado)
        var urgency = 1.0 + Math.max(0, (boxLength - closestX) / boxLength);
        // Quanto mais centralizado o obstaculo, maior o desvio necessario
        var overlap = (closestR - Math.abs(closestY)) / closestR;       // 0..1
        var sideSign = closestY >= 0 ? -1.0 : 1.0;                       // lado oposto
        var lateral = sideSign * car.getMaxForce() * urgency * (AVOID_LATERAL_FLOOR + (1 - AVOID_LATERAL_FLOOR) * overlap);
        var braking = -car.getMaxForce() * AVOID_BRAKE * (urgency - 1.0);

        return add(multiply(side, lateral), multiply(forward, braking));
    }

    // ---------------------------------------------------------------------
    // 7) PATH FOLLOWING - Reynolds: prever onde o carro estara, achar o ponto
    //    mais proximo do caminho (projecao ortogonal no segmento). Se a previsao
    //    sair do "corredor" (raio do caminho), faz seek em um ponto um pouco a
    //    frente no caminho; senao apenas mantem a velocidade de cruzeiro.
    //    Extra de realismo: o carro olha CURVE_LOOKAHEAD px adiante, estima o
    //    raio r da curva que vem e limita a velocidade a sqrt(aLat * r), onde
    //    aLat e uma fracao da aceleracao maxima (maxForce/massa). E a mesma
    //    conta de um piloto real: v^2 / r nao pode passar da aderencia.
    // ---------------------------------------------------------------------
    public static final double CURVE_LOOKAHEAD = 120;
    public static final double LATERAL_GRIP = 0.8;
    /** true = sempre persegue o alvo no caminho; false = Reynolds original (so corrige fora do corredor). */
    public static boolean PATH_ALWAYS_SEEK = false;

    public static Vector2 followPath(Car car, Path path, double speed, PathProbe probe) {
        var pos = car.getPosition();
        var predictDist = 20 + 0.35 * car.getSpeed();
        var predicted = add(pos, multiply(car.getDirection(), predictDist));

        var proj = path.project(predicted);
        var ahead = path.tangentAhead(proj, CURVE_LOOKAHEAD);
        var turn = Math.acos(Math.clamp(ahead.dot(proj.tangent()), -1.0, 1.0));   // rad
        if (turn > 1e-3) {
            var radius = CURVE_LOOKAHEAD / turn;
            var aLat = LATERAL_GRIP * car.getMaxForce() / car.getMass();
            speed = Math.min(speed, Math.sqrt(aLat * radius));
        }

        var target = add(proj.point(), multiply(proj.tangent(), path.lookAhead()));
        var outside = Vector2.distance(predicted, proj.point()) > path.radius();

        if (probe != null) {
            probe.predicted = predicted;
            probe.normalPoint = proj.point();
            probe.target = target;
            probe.correcting = outside;
        }

        // Fora do corredor: corrige rumo. Tambem corrige se o carro estiver
        // indo na contramao (tangente oposta a direcao atual).
        var wrongWay = car.getDirection().dot(proj.tangent()) < 0;
        if (PATH_ALWAYS_SEEK || outside || wrongWay) return seek(car, target, speed);
        return cruise(car, speed);
    }

    // ---------------------------------------------------------------------
    // 8) SEPARATION - repulsao de vizinhos proximos (Reynolds 1987).
    // ---------------------------------------------------------------------
    public static Vector2 separation(Car car, List<Car> neighbors, double radius) {
        var pos = car.getPosition();
        var sum = vec2();
        for (var n : neighbors) {
            var away = subtract(pos, n.getPosition());
            var d = away.size();
            if (d >= radius) continue;
            if (d < 1e-6) away = perp(car.getDirection());
            sum.add(away.resize((radius - d) / radius));   // 0 longe ... 1 encostado
        }
        if (sum.isZero()) return vec2();
        var strength = Math.min(1.0, sum.size());
        return sum.resize(car.getMaxForce() * strength);
    }
}
