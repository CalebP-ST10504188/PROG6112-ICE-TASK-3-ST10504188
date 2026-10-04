package algorithms;

import model.Edge;
import model.Graph;
import model.Vertex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Breadth-first search: visits vertices level by level using a queue.
 *
 * Neighbours are taken lowest weight first, with ties broken alphabetically.
 */
public class BFSAlgorithm implements GraphAlgorithm {

    @Override
    public AlgorithmResult run(Graph graph, Vertex start) {
        StepRecorder rec = new StepRecorder();
        List<Vertex> order = new ArrayList<>();
        Set<Vertex> discovered = new HashSet<>();
        Map<Vertex, Vertex> parents = new HashMap<>();
        Queue<Vertex> queue = new ArrayDeque<>();

        discovered.add(start);
        queue.offer(start);
        rec.vertex(start, VertexState.FRONTIER);
        rec.snapshot("Start at " + start + ": mark it discovered and add it to the queue.", queueText(queue));

        while (!queue.isEmpty()) {
            Vertex current = queue.poll();
            order.add(current);
            rec.vertex(current, VertexState.CURRENT);
            rec.badge(current, "#" + order.size());
            rec.snapshot("Take " + current + " from the front of the queue. It is visit #" + order.size() + ".",
                    queueText(queue));

            for (Edge edge : graph.sortedEdgesOf(current)) {
                Vertex neighbour = edge.other(current);
                if (discovered.add(neighbour)) {
                    parents.put(neighbour, current);
                    queue.offer(neighbour);
                    rec.vertex(neighbour, VertexState.FRONTIER);
                    rec.edge(edge, EdgeState.TREE);
                    rec.snapshot("Discover " + neighbour + " via " + edge.label() + " (weight " + edge.getWeight()
                            + ") and add it to the back of the queue.", queueText(queue), current, neighbour);
                }
            }
            rec.vertex(current, VertexState.VISITED);
        }

        String output = "BFS: " + Text.arrows(order);
        rec.snapshot(finalMessage("BFS", order, graph), "Queue: [ ] (empty)");
        return new AlgorithmResult(output, rec.steps(), start, parents, Map.of());
    }

    static String finalMessage(String name, List<Vertex> order, Graph graph) {
        String message = name + " complete. Visit order: " + Text.arrows(order) + ".";
        int unreachable = graph.vertexCount() - order.size();
        if (unreachable > 0) {
            message += " " + unreachable + (unreachable == 1 ? " vertex is" : " vertices are")
                    + " not reachable from " + order.get(0) + ".";
        }
        return message;
    }

    private static String queueText(Queue<Vertex> queue) {
        return "Queue: [" + Text.ids(queue) + "]" + (queue.isEmpty() ? " (empty)" : "");
    }
}
