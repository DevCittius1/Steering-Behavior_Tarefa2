package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static cars.engine.Vector2.*;

/**
 * Monta os cenarios de teste. Teclas 1-9 trocam de cenario, D liga/desliga o debug.
 */
public class Setup {
    public enum Scenario {
        SEEK("1", "Seek", "Os carros perseguem o cursor. Verde: leve | Laranja: pesado (massa 3) | Azul: pouca forca."),
        FLEE("2", "Flee", "Aproxime o cursor: quem estiver a menos de 200 px foge. Longe do cursor, freiam e param."),
        ARRIVE("3", "Arrive", "Clique em qualquer ponto: o carro vai ate la e desacelera suavemente (raio de frenagem 300 px)."),
        WANDER("4", "Wander", "Passeio aleatorio: um alvo se move sobre o circulo a frente de cada carro."),
        AVOID("5", "Obstacle Avoidance", "Os carros so aceleram para frente; a caixa de deteccao desvia das pedras."),
        PATH("6", "Path Following", "Carros percorrem o circuito: previsao (preto) -> ponto no caminho -> alvo (verde/vermelho)."),
        PATROL("7", "Combinacao A: Patrulha", "Avoidance(2.0) > Separation(4.0) > Path Following(1.0). Pedras na pista, velocidades diferentes."),
        HUNT("8", "Combinacao B: Caca", "Presas: Avoid(2.5) > Evade(1.6) > Separation(0.8) > Wander(1.0). Cacadores: Avoid(2.5) > Separation(1.5) > Pursuit(1.0)."),
        NAVIGATE("9", "Combinacao C: Navegador", "Clique no mapa: Avoidance(2.0) > Arrive(1.0). Contorna as pedras e para no destino.");

        final String key, title, help;

        Scenario(String key, String title, String help) {
            this.key = key;
            this.title = title;
            this.help = help;
        }
    }

    private Scenario scenario = Scenario.SEEK;
    private List<Car> cars = List.of();

    public Setup() {
        // Permite abrir direto em um cenario: java -Dscenario=7 ...
        var start = System.getProperty("scenario");
        if (start != null && !start.isEmpty()) onKey(start.charAt(0));
    }

    /** Chamado pela janela a cada tecla. Retorna true se o cenario mudou. */
    public boolean onKey(char key) {
        for (var s : Scenario.values()) {
            if (s.key.charAt(0) == key) {
                scenario = s;
                return true;
            }
        }
        return false;
    }

    /** Retorne uma lista com todos os carros que serao desenhados no exercicio. */
    public List<Car> createCars() {
        cars = build(scenario);
        return cars;
    }

    public static List<Car> build(Scenario scenario) {
        var list = new ArrayList<Car>();
        Arena.obstacles = List.of();
        Arena.path = null;
        var rnd = new Random(42);

        switch (scenario) {
            case SEEK -> {
                list.add(new SeekCar(-300, 0, 1, 300, new Color(40, 170, 40)));
                list.add(new SeekCar(0, 200, 3, 300, new Color(240, 130, 0)));
                list.add(new SeekCar(300, 0, 1, 120, new Color(40, 90, 220)));
            }
            case FLEE -> {
                for (int i = 0; i < 14; i++) {
                    list.add(new FleeCar(rnd.nextDouble(-420, 420), rnd.nextDouble(-280, 300)));
                }
            }
            case ARRIVE -> list.add(new ArriveCar(0, 0));
            case WANDER -> {
                for (int i = 0; i < 8; i++) {
                    list.add(new WanderCar(rnd.nextDouble(-350, 350), rnd.nextDouble(-250, 250), i + 1));
                }
            }
            case AVOID -> {
                Arena.obstacles = Arena.rockField();
                for (int i = 0; i < 6; i++) {
                    var y = -250 + i * 100;
                    list.add(new AvoiderCar(-500 + (i % 2) * 1000, y, i % 2 == 0 ? 0 : 180,
                        Color.getHSBColor(i / 6f, 0.7f, 0.9f)));
                }
            }
            case PATH -> {
                Arena.path = Path.circuit();
                addCarsOnPath(list, Arena.path, new double[]{140, 200, 260}, false);
            }
            case PATROL -> {
                Arena.path = Path.circuit();
                Arena.obstacles = Arena.pathBlockers(Arena.path);
                addCarsOnPath(list, Arena.path, new double[]{140, 170, 200, 230, 260, 290}, true);
            }
            case HUNT -> {
                Arena.obstacles = Arena.rockField();
                for (int i = 0; i < 8; i++) {
                    list.add(new PreyCar(rnd.nextDouble(-200, 450), rnd.nextDouble(-250, 300), 100 + i));
                }
                list.add(new HunterCar(-480, -300));
                list.add(new HunterCar(-480, 0));
                list.add(new HunterCar(-480, 300));
            }
            case NAVIGATE -> {
                Arena.obstacles = Arena.rockField();
                list.add(new NavigatorCar(-470, 330));
            }
        }
        return List.copyOf(list);
    }

    private static void addCarsOnPath(List<Car> list, Path path, double[] speeds, boolean patrol) {
        var pts = path.points();
        for (int i = 0; i < speeds.length; i++) {
            var idx = (i * pts.size() / speeds.length) % pts.size();
            var a = pts.get(idx);
            var b = pts.get((idx + 1) % pts.size());
            var deg = Math.toDegrees(Math.atan2(b.y - a.y, b.x - a.x));
            var color = Color.getHSBColor(i / (float) speeds.length, 0.75f, 0.9f);
            list.add(patrol
                ? new PatrolCar(a.clone(), deg, speeds[i], color, PatrolCar.Weights.DEFAULT)
                : new PathCar(a.clone(), deg, speeds[i], color));
        }
    }

    // ---------------------------------------------------------------------
    // Desenho do mapa (antes dos carros)
    // ---------------------------------------------------------------------
    public void drawWorld(Graphics2D g, boolean debug) {
        var path = Arena.path;
        if (path != null) drawPath(g, path, debug);
        for (var o : Arena.obstacles) drawRock(g, o);
    }

    private static void drawPath(Graphics2D g, Path path, boolean debug) {
        var pts = path.points();
        var shape = new Path2D.Double();
        shape.moveTo(pts.get(0).x, pts.get(0).y);
        for (int i = 1; i < pts.size(); i++) shape.lineTo(pts.get(i).x, pts.get(i).y);
        shape.closePath();

        var old = g.getStroke();
        // "asfalto" com a largura do corredor (2 * raio)
        g.setColor(new Color(170, 170, 175));
        g.setStroke(new BasicStroke((float) (path.radius() * 2 + 14), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
        g.setColor(new Color(195, 195, 200));
        g.setStroke(new BasicStroke((float) (path.radius() * 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
        // linha central tracejada
        g.setColor(new Color(255, 255, 255, 220));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{10, 10}, 0));
        g.draw(shape);
        // setas indicando o sentido
        if (debug) {
            g.setColor(new Color(60, 60, 60, 150));
            g.setStroke(new BasicStroke(2f));
            for (int i = 0; i < pts.size(); i += 6) {
                var a = pts.get(i);
                var d = normalize(subtract(pts.get((i + 1) % pts.size()), a));
                var l = rotate(d, Math.toRadians(150)).multiply(8);
                var r = rotate(d, Math.toRadians(-150)).multiply(8);
                g.draw(new Line2D.Double(a.x, a.y, a.x + l.x, a.y + l.y));
                g.draw(new Line2D.Double(a.x, a.y, a.x + r.x, a.y + r.y));
            }
        }
        g.setStroke(old);
    }

    private static void drawRock(Graphics2D g, Obstacle o) {
        var c = o.center();
        var r = o.radius();
        g.setPaint(new RadialGradientPaint(
            (float) (c.x - r * 0.3), (float) (c.y - r * 0.3), (float) (r * 1.3),
            new float[]{0f, 1f}, new Color[]{new Color(150, 130, 110), new Color(90, 75, 60)}));
        g.fill(new Ellipse2D.Double(c.x - r, c.y - r, 2 * r, 2 * r));
        g.setColor(new Color(60, 50, 40));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Ellipse2D.Double(c.x - r, c.y - r, 2 * r, 2 * r));
    }

    // ---------------------------------------------------------------------
    // Debug + HUD (depois dos carros)
    // ---------------------------------------------------------------------
    public void drawOverlay(Graphics2D g, boolean debug, double width, double height) {
        if (debug) {
            for (var c : cars) {
                if (c instanceof BaseCar bc) bc.drawDebug(g);
            }
        }

        var x = (float) (-width / 2 + 16);
        var y = (float) (-height / 2 + 48);
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRoundRect((int) x - 8, (int) y - 18, (int) Math.min(width - 32, 760), debug ? 76 : 58, 10, 10);
        g.setColor(new Color(30, 30, 30));
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString("[%s] %s".formatted(scenario.key, scenario.title), x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString(scenario.help, x, y + 18);
        g.setColor(new Color(90, 90, 90));
        g.drawString("Teclas: 1 Seek  2 Flee  3 Arrive  4 Wander  5 Avoidance  6 Path  7-9 Combinacoes  |  D debug", x, y + 34);
        if (debug) {
            legend(g, x, y + 52);
        }
    }

    private static void legend(Graphics2D g, float x, float y) {
        Object[][] items = {
            {"seek", BaseCar.C_SEEK}, {"flee", BaseCar.C_FLEE}, {"arrive", BaseCar.C_ARRIVE},
            {"wander", BaseCar.C_WANDER}, {"avoid", BaseCar.C_AVOID}, {"path", BaseCar.C_PATH},
            {"separation", BaseCar.C_SEPARATION}, {"pursuit", BaseCar.C_PURSUIT}, {"evade", BaseCar.C_EVADE},
        };
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        var cx = x;
        for (var it : items) {
            g.setColor((Color) it[1]);
            g.fillRect((int) cx, (int) y - 8, 10, 10);
            g.setColor(new Color(60, 60, 60));
            g.drawString((String) it[0], cx + 13, y + 1);
            cx += 14 + g.getFontMetrics().stringWidth((String) it[0]) + 12;
        }
    }
}
