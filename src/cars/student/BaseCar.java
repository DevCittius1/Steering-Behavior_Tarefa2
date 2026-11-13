package cars.student;

import cars.engine.Car;
import cars.engine.Settings;
import cars.engine.Vector2;
import cars.engine.World;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.function.Consumer;

import static cars.engine.Vector2.*;

/**
 * Base de todos os carros do trabalho. Cada subclasse descreve, em
 * {@link #steer}, quais comportamentos usa e com quais pesos/prioridades.
 * Esta classe tambem desenha, no modo debug, as forcas de cada
 * comportamento (uma seta por cor) e as estruturas de percepcao
 * (circulo do wander, caixa de deteccao, previsao do path following).
 */
public abstract class BaseCar extends Car {
    protected WanderState wander;
    protected final AvoidanceProbe avoidProbe = new AvoidanceProbe();
    protected final PathProbe pathProbe = new PathProbe();
    protected Vector2 targetMark;
    protected boolean usesAvoidance;

    private List<SteeringMix.Component> lastComponents = List.of();

    protected BaseCar(Consumer<Settings> settings) {
        super(settings);
    }

    @Override
    public final Vector2 calculateSteering(World world) {
        var mix = new SteeringMix(this);
        steer(world, mix);
        lastComponents = mix.components();
        return mix.result();
    }

    /** Adiciona ao mix os comportamentos do carro, em ordem de prioridade. */
    protected abstract void steer(World world, SteeringMix mix);

    // Cores padronizadas por comportamento (usadas no debug e no README)
    static final Color C_SEEK = new Color(0, 150, 0);
    static final Color C_FLEE = new Color(200, 0, 0);
    static final Color C_ARRIVE = new Color(0, 140, 140);
    static final Color C_WANDER = new Color(140, 0, 200);
    static final Color C_AVOID = new Color(255, 120, 0);
    static final Color C_PATH = new Color(0, 90, 220);
    static final Color C_SEPARATION = new Color(230, 0, 150);
    static final Color C_PURSUIT = new Color(120, 0, 0);
    static final Color C_EVADE = new Color(200, 160, 0);
    static final Color C_CRUISE = new Color(110, 110, 110);

    // ---------------------------------------------------------------------
    // Debug
    // ---------------------------------------------------------------------
    void drawDebug(Graphics2D g) {
        var pos = getPosition();
        var dir = getDirection();
        var old = g.getStroke();

        if (usesAvoidance && avoidProbe.length > 0) {
            var side = perp(dir).multiply(AvoidanceProbe.CAR_HALF_WIDTH);
            var front = multiply(dir, avoidProbe.length);
            var box = new Path2D.Double();
            box.moveTo(pos.x + side.x, pos.y + side.y);
            box.lineTo(pos.x + side.x + front.x, pos.y + side.y + front.y);
            box.lineTo(pos.x - side.x + front.x, pos.y - side.y + front.y);
            box.lineTo(pos.x - side.x, pos.y - side.y);
            box.closePath();
            g.setColor(avoidProbe.hit == null ? new Color(0, 0, 0, 40) : new Color(255, 120, 0, 110));
            g.setStroke(new BasicStroke(1f));
            g.draw(box);
            if (avoidProbe.hit != null) {
                var c = avoidProbe.hit.center();
                var r = avoidProbe.hit.radius() + 3;
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Double(c.x - r, c.y - r, 2 * r, 2 * r));
            }
        }

        if (wander != null && wander.circleCenter != null) {
            var c = wander.circleCenter;
            var r = wander.radius;
            g.setColor(new Color(140, 0, 200, 90));
            g.setStroke(new BasicStroke(1f));
            g.draw(new Ellipse2D.Double(c.x - r, c.y - r, 2 * r, 2 * r));
            g.draw(new Line2D.Double(pos.x, pos.y, c.x, c.y));
            dot(g, wander.target, 6, new Color(140, 0, 200, 200));
        }

        if (pathProbe.predicted != null) {
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(0, 0, 0, 90));
            g.draw(new Line2D.Double(pos.x, pos.y, pathProbe.predicted.x, pathProbe.predicted.y));
            g.draw(new Line2D.Double(pathProbe.predicted.x, pathProbe.predicted.y,
                pathProbe.normalPoint.x, pathProbe.normalPoint.y));
            dot(g, pathProbe.predicted, 5, new Color(0, 0, 0, 140));
            dot(g, pathProbe.target, 6, pathProbe.correcting ? new Color(220, 0, 0, 200) : new Color(0, 160, 0, 200));
        }

        if (targetMark != null) {
            g.setColor(new Color(0, 0, 0, 60));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4, 4}, 0));
            g.draw(new Line2D.Double(pos.x, pos.y, targetMark.x, targetMark.y));
        }

        // Uma seta por comportamento (forca ponderada efetivamente aplicada)
        for (var comp : lastComponents) {
            arrow(g, pos, comp.force(), 0.12, comp.color());
        }
        g.setStroke(old);
    }

    private static void dot(Graphics2D g, Vector2 p, double size, Color color) {
        if (p == null) return;
        g.setColor(color);
        g.fill(new Ellipse2D.Double(p.x - size / 2, p.y - size / 2, size, size));
    }

    private static void arrow(Graphics2D g, Vector2 origin, Vector2 v, double scale, Color color) {
        if (Vector2.isZero(v) || v.size() < 1) return;
        var tip = add(origin, multiply(v, scale));
        var d = normalize(v);
        var left = rotate(d, Math.toRadians(150)).multiply(7);
        var right = rotate(d, Math.toRadians(-150)).multiply(7);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
        g.setStroke(new BasicStroke(2.2f));
        g.draw(new Line2D.Double(origin.x, origin.y, tip.x, tip.y));
        g.draw(new Line2D.Double(tip.x, tip.y, tip.x + left.x, tip.y + left.y));
        g.draw(new Line2D.Double(tip.x, tip.y, tip.x + right.x, tip.y + right.y));
    }
}
