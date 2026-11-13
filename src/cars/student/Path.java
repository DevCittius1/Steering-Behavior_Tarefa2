package cars.student;

import cars.engine.Vector2;

import java.util.List;

import static cars.engine.Vector2.*;

/**
 * Caminho fechado (poligono) com um "corredor" de largura 2*radius.
 * A ordem dos pontos define o sentido de percurso.
 */
public final class Path {
    public record Projection(Vector2 point, Vector2 tangent, int segment) { }

    private final List<Vector2> points;
    private final double radius;
    private double lookAhead;

    public Path(List<Vector2> points, double radius, double lookAhead) {
        this.points = List.copyOf(points);
        this.radius = radius;
        this.lookAhead = lookAhead;
    }

    public List<Vector2> points() { return points; }
    public double radius() { return radius; }
    public double lookAhead() { return lookAhead; }
    public void setLookAhead(double v) { this.lookAhead = v; }

    /** Projecao ortogonal de p no segmento mais proximo do caminho. */
    public Projection project(Vector2 p) {
        Projection best = null;
        var bestDist = Double.MAX_VALUE;
        var n = points.size();
        for (int i = 0; i < n; i++) {
            var a = points.get(i);
            var b = points.get((i + 1) % n);
            var ab = subtract(b, a);
            var len2 = ab.sizeSqr();
            var t = len2 == 0 ? 0 : Math.clamp(subtract(p, a).dot(ab) / len2, 0.0, 1.0);
            var q = add(a, multiply(ab, t));
            var d = distanceSqr(p, q);
            if (d < bestDist) {
                bestDist = d;
                best = new Projection(q, normalize(ab), i);
            }
        }
        return best;
    }

    /** Distancia do ponto ao caminho (erro lateral). */
    public double distanceTo(Vector2 p) {
        return Vector2.distance(p, project(p).point());
    }

    /**
     * Tangente do caminho {@code distance} pixels a frente da projecao dada.
     * Usada para "enxergar" a curva que vem pela frente e reduzir a velocidade.
     */
    public Vector2 tangentAhead(Projection from, double distance) {
        var n = points.size();
        var i = from.segment();
        var remaining = distance + Vector2.distance(points.get(i), from.point());
        for (int k = 0; k < n; k++) {
            var a = points.get(i);
            var b = points.get((i + 1) % n);
            var len = Vector2.distance(a, b);
            if (remaining <= len) return normalize(subtract(b, a));
            remaining -= len;
            i = (i + 1) % n;
        }
        return from.tangent();
    }

    /**
     * Circuito usado nos cenarios 6 e 7: um "estadio" (duas retas e duas curvas
     * de raio 205 px) com uma chicane suave na reta de baixo.
     * Raio de curvatura minimo ~130 px.
     */
    public static Path circuit() {
        final double L = 170, R = 205, bump = 45, offsetY = 40;
        final int n = 96;
        var per = 4 * L + 2 * Math.PI * R;
        var pts = new java.util.ArrayList<Vector2>();
        for (int i = 0; i < n; i++) {
            var v = per * i / n;
            double x, y, nx, ny, off = 0;
            if (v < 2 * L) {                                   // reta de baixo (com chicane)
                x = -L + v; y = R; nx = 0; ny = 1;
                var u = v / (2 * L);
                off = -bump * Math.pow(Math.sin(Math.PI * u), 2);
            } else if (v < 2 * L + Math.PI * R) {              // curva da direita
                var a = Math.PI / 2 - (v - 2 * L) / R;
                x = L + R * Math.cos(a); y = R * Math.sin(a); nx = Math.cos(a); ny = Math.sin(a);
            } else if (v < 4 * L + Math.PI * R) {              // reta de cima
                x = L - (v - 2 * L - Math.PI * R); y = -R; nx = 0; ny = -1;
            } else {                                           // curva da esquerda
                var a = -Math.PI / 2 - (v - 4 * L - Math.PI * R) / R;
                x = -L + R * Math.cos(a); y = R * Math.sin(a); nx = Math.cos(a); ny = Math.sin(a);
            }
            pts.add(vec2(x + nx * off, y + ny * off + offsetY));
        }
        return new Path(pts, 22, 45);
    }
}
