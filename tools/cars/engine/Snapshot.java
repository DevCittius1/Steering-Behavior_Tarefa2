package cars.engine;

import cars.student.Setup;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Ferramenta opcional (fora do jogo): simula um cenario sem janela, com passo
 * fixo, e salva um PNG com rastros das trajetorias. Usada para gerar as figuras
 * do README e do artigo.
 *
 * Uso: java -cp out cars.engine.Snapshot <cenario 1-9> <segundos> <saida.png> [mouseX mouseY] [clickX clickY]
 */
public final class Snapshot {
    public static void main(String[] args) throws Exception {
        var key = args[0].charAt(0);
        var secs = Double.parseDouble(args[1]);
        var out = new File(args[2]);
        Vector2 mouse = args.length >= 5 ? new Vector2(Double.parseDouble(args[3]), Double.parseDouble(args[4])) : null;
        Vector2 click = args.length >= 7 ? new Vector2(Double.parseDouble(args[5]), Double.parseDouble(args[6])) : null;

        var setup = new Setup();
        setup.onKey(key);
        List<Car> cars = setup.createCars();

        final int W = 1024, H = 720;
        final double dt = 1.0 / 60.0;
        var trails = new ArrayList<List<Vector2>>();
        for (int i = 0; i < cars.size(); i++) trails.add(new ArrayList<>());

        int steps = (int) (secs / dt);
        for (int s = 0; s < steps; s++) {
            for (var c : cars) c.update(new World(dt, c, cars, mouse, click, W, H));
            if (s % 3 == 0) {
                for (int i = 0; i < cars.size(); i++) trails.get(i).add(cars.get(i).getPosition());
            }
        }

        var img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setBackground(new Color(220, 220, 220));
        g.clearRect(0, 0, W, H);
        g.translate(W / 2.0, H / 2.0);
        setup.drawWorld(g, true);

        // rastros (ignora "saltos" causados pelo wrap das bordas)
        g.setStroke(new BasicStroke(1.6f));
        for (int i = 0; i < cars.size(); i++) {
            var t = trails.get(i);
            var col = cars.get(i) instanceof Car ? colorOf(cars.get(i)) : Color.GRAY;
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 120));
            for (int k = 1; k < t.size(); k++) {
                var a = t.get(k - 1);
                var b = t.get(k);
                if (Vector2.distance(a, b) > 60) continue;
                g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
            }
        }
        if (mouse != null) {
            g.setColor(Color.BLACK);
            g.drawLine((int) mouse.x - 8, (int) mouse.y, (int) mouse.x + 8, (int) mouse.y);
            g.drawLine((int) mouse.x, (int) mouse.y - 8, (int) mouse.x, (int) mouse.y + 8);
        }
        if (click != null) {
            g.setColor(Color.GRAY);
            g.fillOval((int) click.x - 5, (int) click.y - 5, 10, 10);
        }
        for (var c : cars) c.draw(g, false);
        setup.drawOverlay(g, true, W, H - 30);
        g.dispose();
        ImageIO.write(img, "png", out);
        System.out.println("salvo " + out);
    }

    private static Color colorOf(Car c) {
        try {
            var f = Car.class.getDeclaredField("color");
            f.setAccessible(true);
            return (Color) f.get(c);
        } catch (ReflectiveOperationException e) {
            return Color.GRAY;
        }
    }
}
