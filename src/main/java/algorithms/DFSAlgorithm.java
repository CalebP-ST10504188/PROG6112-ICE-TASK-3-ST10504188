package algorithms;

import model.Edge;
import model.Graph;
import model.Vertex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Depth-first search: follows one branch as deep as possible before backtracking.
 *
 * Only vertices reachable from the start vertex are visited, which matches BFS.
 */
public class DFSAlgorithm implements GraphAlgorithm {

    @Override
    public AlgorithmResult run(Graph graph, Vertex start) {
        StepRecorder rec = new StepRecorder();
        List<Vertex> order = new ArrayList<>();
        Set<Vertex> visited = new HashSet<>();
        Deque<Vertex> stack = new ArrayDeque<>();

        visit(graph, start, null, null, visited, order, stack, rec);

        String output = "DFS: " + Text.arrows(order);
        rec.snapshot(BFSAlgorithm.finalMessage("DFS", order, graph), "Stack: [ ] (empty)");
        return new AlgorithmResult(output, rec.steps(), start, Map.of(), Map.of());
    }

    private void visit(Graph graph, Vertex vertex, Vertex parent, Edge via, Set<Vertex> visited,
                       List<Vertex> order, Deque<Vertex> stack, StepRecorder rec) {
        visited.add(vertex);
        order.add(vertex);
        stack.push(vertex);
        if (parent != null) rec.vertex(parent, VertexState.FRONTIER);
        if (via != null) rec.edge(via, EdgeState.TREE);
        rec.vertex(vertex, VertexState.CURRENT);
        rec.badge(vertex, "#" + order.size());
        rec.snapshot(parent == null
                        ? "Start at " + vertex + " (visit #1) and push it onto the stack."
                        : "Go deeper: visit " + vertex + " via " + via.label() + " (weight " + via.getWeight()
                        + "). Visit #" + order.size() + ".",
                stackText(stack), parent, vertex);

        for (Edge edge : graph.sortedEdgesOf(vertex)) {
            Vertex neighbour = edge.other(vertex);
            if (!visited.contains(neighbour)) {
                visit(graph, neighbour, vertex, edge, visited, order, stack, rec);
                //The child has been fully explored and popped: we are back at this vertex.
                rec.vertex(vertex, VertexState.CURRENT);
                rec.snapshot(neighbour + " has no unvisited neighbours left, so pop it and backtrack to "
                        + vertex + ".", stackText(stack));
            }
        }

        stack.pop();
        rec.vertex(vertex, VertexState.VISITED);
    }

    //Shows the stack from bottom to top, such as "Stack: A > B > D (top)".
    private static String stackText(Deque<Vertex> stack) {
        if (stack.isEmpty()) return "Stack: [ ] (empty)";
        StringBuilder text = new StringBuilder("Stack: ");
        Iterator<Vertex> bottomUp = stack.descendingIterator();
        while (bottomUp.hasNext()) {
            text.append(bottomUp.next().getId());
            if (bottomUp.hasNext()) text.append(" > ");
        }
        return text.append(" (top)").toString();
    }
}
