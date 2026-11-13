package cars.engine;

import cars.student.*;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Ferramenta opcional (fora do jogo): roda os cenarios sem janela, com passo
 * fixo de 1/60 s, e mede metricas objetivas para justificar o ajuste de pesos.
 *
 * Uso: java -cp out cars.engine.Benchmark
 */
public final class Benchmark {
    static final double DT = 1.0 / 60.0;
    static final int W = 1024, H = 768;
    static final double CAR_RADIUS = 10;   // meia largura aproximada do carro desenhado

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("sweep")) {
            for (var len : new double[]{50, 70, 90, 110}) {
                AvoidanceProbe.MIN_LENGTH = len;
                System.out.println("\n##### MIN_LENGTH = " + len);
                patrolExperiment();
                avoidExperiment();
            }
            return;
        }
        patrolExperiment();
        avoidExperiment();
        arriveExperiment();
        huntExperiment();
    }

    // ---------------------------------------------------------------------
    static void step(List<Car> cars, Vector2 mouse, Vector2 click) {
        for (var c : cars) c.update(new World(DT, c, cars, mouse, click, W, H));
    }

    /** Conta "eventos" de contato (transicao sem contato -> contato) entre carros e obstaculos. */
    static final class ContactCounter {
        final Set<String> inContact = new HashSet<>();
        int events = 0;
        long frames = 0;

        void check(String key, boolean contact) {
            if (contact) {
                frames++;
                if (inContact.add(key)) events++;
            } else {
                inContact.remove(key);
            }
        }
    }

    // ---------------------------------------------------------------------
    // E1: Combinacao A (patrulha) - variando pesos e modo de combinacao
    // ---------------------------------------------------------------------
    static void patrolExperiment() {
        System.out.println("\n=== E1 Patrulha (6 carros, 120 s) ===");
        System.out.println("config;modo;colisoes_obstaculo;frames_obst;contatos_carros;erro_lateral_medio;pct_fora_corredor;vel_media");
        Object[][] configs = {
            {"so path", new PatrolCar.Weights(0, 0, 1), SteeringMix.Mode.PRIORITIZED},
            {"path+avoid (sep 0)", new PatrolCar.Weights(2.0, 0, 1), SteeringMix.Mode.PRIORITIZED},
            {"avoid fraco 0.5", new PatrolCar.Weights(0.5, 4.0, 1), SteeringMix.Mode.PRIORITIZED},
            {"padrao 2.0/4.0/1.0", PatrolCar.Weights.DEFAULT, SteeringMix.Mode.PRIORITIZED},
            {"avoid forte 5.0", new PatrolCar.Weights(5.0, 4.0, 1), SteeringMix.Mode.PRIORITIZED},
            {"separation 1.2", new PatrolCar.Weights(2.0, 1.2, 1), SteeringMix.Mode.PRIORITIZED},
            {"separation 2.5", new PatrolCar.Weights(2.0, 2.5, 1), SteeringMix.Mode.PRIORITIZED},
            {"separation 8.0", new PatrolCar.Weights(2.0, 8.0, 1), SteeringMix.Mode.PRIORITIZED},
            {"padrao 2.0/4.0/1.0", PatrolCar.Weights.DEFAULT, SteeringMix.Mode.WEIGHTED_SUM},
        };
        for (var cfg : configs) {
            SteeringMix.mode = (SteeringMix.Mode) cfg[2];
            var w = (PatrolCar.Weights) cfg[1];
            var path = Path.circuit();
            Arena.path = path;
            Arena.obstacles = Arena.pathBlockers(path);
            var speeds = new double[]{140, 170, 200, 230, 260, 290};
            var cars = new ArrayList<Car>();
            var pts = path.points();
            for (int i = 0; i < speeds.length; i++) {
                var idx = (i * pts.size() / speeds.length) % pts.size();
                var a = pts.get(idx);
                var b = pts.get((idx + 1) % pts.size());
                var deg = Math.toDegrees(Math.atan2(b.y - a.y, b.x - a.x));
                cars.add(new PatrolCar(a.clone(), deg, speeds[i], Color.RED, w));
            }
            var obst = new ContactCounter();
            var carc = new ContactCounter();
            double errSum = 0, speedSum = 0;
            long samples = 0, outside = 0;
            int steps = (int) (120 / DT);
            for (int s = 0; s < steps; s++) {
                step(cars, null, null);
                for (int i = 0; i < cars.size(); i++) {
                    var c = cars.get(i);
                    var p = c.getPosition();
                    for (int k = 0; k < Arena.obstacles.size(); k++) {
                        var o = Arena.obstacles.get(k);
                        obst.check(i + "-" + k, Vector2.distance(p, o.center()) < o.radius() + CAR_RADIUS);
                    }
                    for (int j = i + 1; j < cars.size(); j++) {
                        carc.check(i + "x" + j, Car.distance(c, cars.get(j)) < 2 * CAR_RADIUS);
                    }
                    var e = path.distanceTo(p);
                    errSum += e;
                    if (e > path.radius()) outside++;
                    speedSum += c.getSpeed();
                    samples++;
                }
            }
            System.out.printf(Locale.ROOT, "%s;%s;%d;%d;%d;%.1f;%.1f;%.0f%n", cfg[0], cfg[2], obst.events, obst.frames,
                carc.events, errSum / samples, 100.0 * outside / samples, speedSum / samples);
        }
        SteeringMix.mode = SteeringMix.Mode.PRIORITIZED;
    }

    // ---------------------------------------------------------------------
    // E2: Obstacle Avoidance isolado (cenario 5) - com e sem desvio
    // ---------------------------------------------------------------------
    static void avoidExperiment() {
        System.out.println("\n=== E2 Obstacle Avoidance (6 carros, 120 s) ===");
        System.out.println("peso_avoid;colisoes;frames_em_colisao");
        for (var wAvoid : new double[]{0.0, 0.5, 1.0, 2.0}) {
            AvoiderCar.W_AVOID = wAvoid;
            var cars = Setup.build(Setup.Scenario.AVOID);
            var obst = new ContactCounter();
            int steps = (int) (120 / DT);
            for (int s = 0; s < steps; s++) {
                step(cars, null, null);
                for (int i = 0; i < cars.size(); i++) {
                    var p = cars.get(i).getPosition();
                    for (int k = 0; k < Arena.obstacles.size(); k++) {
                        var o = Arena.obstacles.get(k);
                        obst.check(i + "-" + k, Vector2.distance(p, o.center()) < o.radius() + CAR_RADIUS);
                    }
                }
            }
            System.out.printf(Locale.ROOT, "%.1f;%d;%d%n", wAvoid, obst.events, obst.frames);
        }
        AvoiderCar.W_AVOID = 1.0;
    }

    // ---------------------------------------------------------------------
    // E3: Arrive - variando o raio de frenagem
    // ---------------------------------------------------------------------
    static void arriveExperiment() {
        System.out.println("\n=== E3 Arrive (partida em (-400,0), alvo em (300,0)) ===");
        System.out.println("raio_frenagem;overshoot_px;tempo_para_parar_s;oscilacoes");
        var target = new Vector2(300, 0);
        for (var r : new double[]{1, 60, 120, 220, 300, 400}) {
            ArriveCar.SLOWING_RADIUS = r;
            List<Car> cars = List.of(new ArriveCar(-400, 0));
            var car = cars.get(0);
            double overshoot = 0, settle = -1;
            int crossings = 0;
            double lastSign = -1;
            for (int s = 0; s < (int) (15 / DT); s++) {
                step(cars, null, target);
                var p = car.getPosition();
                overshoot = Math.max(overshoot, p.x - target.x);
                var sign = Math.signum(p.x - target.x);
                if (sign != 0 && sign != lastSign) { crossings++; lastSign = sign; }
                if (settle < 0 && Vector2.distance(p, target) < 3 && car.getSpeed() < 5) settle = s * DT;
            }
            System.out.printf(Locale.ROOT, "%.0f;%.1f;%s;%d%n", r, overshoot,
                settle < 0 ? ">15" : String.format(Locale.ROOT, "%.2f", settle), crossings);
        }
        ArriveCar.SLOWING_RADIUS = 300;
    }

    // ---------------------------------------------------------------------
    // E4: Combinacao B (caca) - efeito do peso do Evade nas capturas
    // ---------------------------------------------------------------------
    static void huntExperiment() {
        System.out.println("\n=== E4 Caca (8 presas x 3 cacadores, 60 s, media de 5 sementes de wander) ===");
        System.out.println("peso_evade;capturas_media;colisoes_presas_media");
        for (var wEvade : new double[]{0.0, 0.8, 1.6, 3.0}) {
            PreyCar.W_EVADE = wEvade;
            double caps = 0, cols = 0;
            for (int seed = 0; seed < 5; seed++) {
                var cars = new ArrayList<Car>();
                Arena.obstacles = Arena.rockField();
                Arena.path = null;
                var rnd = new Random(42 + seed);
                for (int i = 0; i < 8; i++) {
                    cars.add(new PreyCar(rnd.nextDouble(-200, 450), rnd.nextDouble(-250, 300), 100 + i + seed * 10));
                }
                cars.add(new HunterCar(-480, -300));
                cars.add(new HunterCar(-480, 0));
                cars.add(new HunterCar(-480, 300));
                var capture = new ContactCounter();
                var obst = new ContactCounter();
                for (int s = 0; s < (int) (60 / DT); s++) {
                    step(cars, null, null);
                    for (int i = 0; i < cars.size(); i++) {
                        if (!(cars.get(i) instanceof PreyCar)) continue;
                        var p = cars.get(i).getPosition();
                        for (int j = 0; j < cars.size(); j++) {
                            if (cars.get(j) instanceof HunterCar) {
                                capture.check(i + "c" + j, Car.distance(cars.get(i), cars.get(j)) < 25);
                            }
                        }
                        for (int k = 0; k < Arena.obstacles.size(); k++) {
                            var o = Arena.obstacles.get(k);
                            obst.check(i + "-" + k, Vector2.distance(p, o.center()) < o.radius() + CAR_RADIUS);
                        }
                    }
                }
                caps += capture.events;
                cols += obst.events;
            }
            System.out.printf(Locale.ROOT, "%.1f;%.1f;%.1f%n", wEvade, caps / 5, cols / 5);
        }
        PreyCar.W_EVADE = 1.6;
    }
}
