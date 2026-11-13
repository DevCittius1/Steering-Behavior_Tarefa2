package cars.student;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mapa de testes: obstaculos e caminho do cenario atual.
 * Coordenadas com origem no centro da janela (1024 x 768).
 */
public final class Arena {
    private Arena() { }

    /** Obstaculos do cenario atual (lidos pelos carros com Obstacle Avoidance). */
    public static volatile List<Obstacle> obstacles = List.of();
    /** Caminho do cenario atual (lido pelos carros com Path Following). */
    public static volatile Path path = null;

    /** Campo de "pedras" espalhadas, gerado com semente fixa (sempre igual). */
    public static List<Obstacle> rockField() {
        var list = new ArrayList<Obstacle>();
        var rnd = new Random(7);
        int tries = 0;
        while (list.size() < 14 && tries++ < 2000) {
            var x = rnd.nextDouble(-400, 400);
            var y = rnd.nextDouble(-190, 310);
            var r = rnd.nextDouble(18, 42);
            var ok = true;
            for (var o : list) {
                var dx = o.center().x - x;
                var dy = o.center().y - y;
                // deixa sempre um vao de pelo menos ~70 px entre pedras
                if (Math.sqrt(dx * dx + dy * dy) < o.radius() + r + 70) { ok = false; break; }
            }
            if (ok) list.add(Obstacle.of(x, y, r));
        }
        return List.copyOf(list);
    }

    /** Obstaculos colocados SOBRE o circuito (cenario de patrulha). */
    public static List<Obstacle> pathBlockers(Path path) {
        var pts = path.points();
        var n = pts.size();
        var list = new ArrayList<Obstacle>();
        int[] idx = {n / 8, n * 3 / 8 + 2, n * 5 / 8, n * 7 / 8 - 1};
        double[] r = {26, 20, 30, 22};
        double[] off = {0, 10, -8, 4};   // levemente fora do centro, para variar o lado do desvio
        for (int i = 0; i < idx.length; i++) {
            var p = pts.get(idx[i]);
            list.add(Obstacle.of(p.x + off[i], p.y + off[i], r[i]));
        }
        return List.copyOf(list);
    }
}
