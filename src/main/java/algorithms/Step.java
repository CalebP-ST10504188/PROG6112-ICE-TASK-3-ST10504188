package algorithms;

import model.Edge;
import model.Vertex;

import java.util.Map;

/**
 * A snapshot of an algorithm at one moment in time.
 *
 * Every step is a complete picture rather than a change from the last one, so the UI can jump
 * to any step, including backwards.
 */
public record Step(String message,
                   String structure,
                   Map<Vertex, VertexState> vertexStates,
                   Map<Edge, EdgeState> edgeStates,
                   Map<Vertex, String> badges,
                   Vertex from,
                   Vertex to) {

    public Step {
        vertexStates = Map.copyOf(vertexStates);
        edgeStates = Map.copyOf(edgeStates);
        badges = Map.copyOf(badges);
    }

    public VertexState stateOf(Vertex vertex) {
        return vertexStates.getOrDefault(vertex, VertexState.UNVISITED);
    }

    public EdgeState stateOf(Edge edge) {
        return edgeStates.getOrDefault(edge, EdgeState.NORMAL);
    }

    public boolean hasTraversal() {
        return from != null && to != null;
    }
}
