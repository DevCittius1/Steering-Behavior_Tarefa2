package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static cars.engine.Vector2.*;

/**
 * Combinador de comportamentos.
 * <p>
 * Dois modos (Buckland, 2005):
 * <ul>
 *   <li>{@link Mode#WEIGHTED_SUM}: soma ponderada simples, truncada em maxForce no final.
 *   Simples, mas forcas opostas podem se cancelar e um comportamento de baixa
 *   prioridade pode "diluir" um desvio urgente.</li>
 *   <li>{@link Mode#PRIORITIZED} (padrao): soma ponderada truncada com prioridade.
 *   As forcas sao adicionadas na ordem em que {@link #add} e chamado e cada uma
 *   consome parte do "orcamento" maxForce. Quando o orcamento acaba, as de menor
 *   prioridade sao descartadas. Assim, desviar de um obstaculo sempre vence
 *   "seguir o caminho" quando os dois disputam a forca disponivel.</li>
 * </ul>
 */
public final class SteeringMix {
    public enum Mode { WEIGHTED_SUM, PRIORITIZED }

    /** Modo global (alterado pelo benchmark para comparar as estrategias). */
    public static Mode mode = Mode.PRIORITIZED;

    public record Component(String name, Color color, Vector2 force) { }

    private final double budget;
    private double remaining;
    private final Vector2 total = vec2();
    private final List<Component> components = new ArrayList<>();

    public SteeringMix(Car car) {
        this.budget = car.getMaxForce();
        this.remaining = budget;
    }

    public SteeringMix add(String name, Color color, Vector2 force, double weight) {
        var f = multiply(force, weight);
        if (mode == Mode.PRIORITIZED) {
            if (remaining <= 0) f = vec2();
            else if (f.size() > remaining) f = f.resize(remaining);
            remaining -= f.size();
        }
        total.add(f);
        components.add(new Component(name, color, f));
        return this;
    }

    public Vector2 result() {
        return truncate(total, budget);
    }

    public List<Component> components() { return components; }
}
