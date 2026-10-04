package algorithms;

import model.Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything an algorithm run produces: the final output, every recorded step, and the data
 * needed to trace paths afterwards.
 */
public record AlgorithmResult(String output,
                              List<Step> steps,
                              Vertex source,
                              Map<Vertex, Vertex> parents,
                              Map<Vertex, Integer> distances) {

    public AlgorithmResult {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("An algorithm must record at least one step");
        }
        steps = List.copyOf(steps);
        parents = Map.copyOf(parents);
        distances = Map.copyOf(distances);
    }

    //True if the UI can show paths when hovering over a vertex.
    public boolean supportsPathQueries() {
        return source != null && !parents.isEmpty();
    }

    //Walks the parent links back from target to the source. Empty if the target was not reached.
    public Optional<List<Vertex>> pathTo(Vertex target) {
        if (source == null || target == null) return Optional.empty();
        if (target.equals(source)) return Optional.of(List.of(source));
        if (!parents.containsKey(target)) return Optional.empty();

        List<Vertex> path = new ArrayList<>();
        Vertex current = target;
        while (current != null && !current.equals(source)) {
            path.add(current);
            current = parents.get(current);
            if (path.size() > parents.size() + 1) return Optional.empty(); //Guards against a cycle in the parent links.
        }
        if (current == null) return Optional.empty();
        path.add(source);
        Collections.reverse(path);
        return Optional.of(path);
    }

    public Step finalStep() {
        return steps.get(steps.size() - 1);
    }
}
