package algorithms;

import model.Graph;
import model.Vertex;

import java.util.Optional;

/**
 * The interface every algorithm implements, so the UI can run any of them the same way.
 */
public interface GraphAlgorithm {

    //Runs the algorithm and records every step.
    AlgorithmResult run(Graph graph, Vertex start);

    //Returns an error message if the algorithm cannot run on this graph, or empty if it can.
    default Optional<String> validate(Graph graph) {
        return Optional.empty();
    }

    //Kruskal's works on the whole graph, so it overrides this to return false.
    default boolean needsStartVertex() {
        return true;
    }

    //Only the shortest path algorithm needs a target vertex.
    default boolean needsTargetVertex() {
        return false;
    }

    //Runs with a target vertex. Algorithms that don't use one simply ignore it.
    default AlgorithmResult run(Graph graph, Vertex start, Vertex target) {
        return run(graph, start);
    }
}
