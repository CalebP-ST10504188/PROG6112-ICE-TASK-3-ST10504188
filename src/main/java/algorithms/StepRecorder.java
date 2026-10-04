package algorithms;

import model.Edge;
import model.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records an algorithm's progress. Each call to snapshot() saves the current picture as a Step.
 */
final class StepRecorder {

    private final Map<Vertex, VertexState> vertexStates = new HashMap<>();
    private final Map<Edge, EdgeState> edgeStates = new HashMap<>();
    private final Map<Vertex, String> badges = new HashMap<>();
    private final List<Step> steps = new ArrayList<>();

    void vertex(Vertex vertex, VertexState state) {
        if (state == VertexState.UNVISITED) vertexStates.remove(vertex);
        else vertexStates.put(vertex, state);
    }

    VertexState vertexState(Vertex vertex) {
        return vertexStates.getOrDefault(vertex, VertexState.UNVISITED);
    }

    void edge(Edge edge, EdgeState state) {
        if (state == EdgeState.NORMAL) edgeStates.remove(edge);
        else edgeStates.put(edge, state);
    }

    EdgeState edgeState(Edge edge) {
        return edgeStates.getOrDefault(edge, EdgeState.NORMAL);
    }

    void badge(Vertex vertex, String text) {
        if (text == null) badges.remove(vertex);
        else badges.put(vertex, text);
    }

    void snapshot(String message, String structure) {
        snapshot(message, structure, null, null);
    }

    void snapshot(String message, String structure, Vertex from, Vertex to) {
        steps.add(new Step(message, structure, vertexStates, edgeStates, badges, from, to));
    }

    List<Step> steps() {
        return List.copyOf(steps);
    }
}
