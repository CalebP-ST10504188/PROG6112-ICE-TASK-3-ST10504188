package algorithms;

import model.Edge;
import model.Graph;
import model.Vertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dijkstra's shortest paths from a start vertex.
 *
 * Without a target it finds the distance to every vertex. With a target it stops once the
 * target is settled and traces the path. Negative weights are rejected because the algorithm
 * gives wrong answers with them.
 */
public class DijkstrasAlgorithm implements GraphAlgorithm {

    private static final int UNREACHED = Integer.MAX_VALUE;

    @Override
    public Optional<String> validate(Graph graph) {
        return graph.firstNegativeEdge().map(edge ->
                "Dijkstra's algorithm needs non-negative weights, but " + edge.label() + " has weight "
                        + edge.getWeight() + ".\nChange that weight, or use Prim's or Kruskal's instead.");
    }

    @Override
    public AlgorithmResult run(Graph graph, Vertex start) {
        return solve(graph, start, null);
    }

    @Override
    public AlgorithmResult run(Graph graph, Vertex start, Vertex target) {
        return solve(graph, start, target);
    }

    private AlgorithmResult solve(Graph graph, Vertex start, Vertex target) {
        StepRecorder rec = new StepRecorder();
        Map<Vertex, Integer> distance = new HashMap<>();
        Map<Vertex, Vertex> parent = new HashMap<>();
        Map<Vertex, Edge> parentEdge = new HashMap<>();
        Set<Vertex> settled = new HashSet<>();

        for (Vertex vertex : graph.getVertices()) {
            distance.put(vertex, UNREACHED);
            rec.badge(vertex, Text.INFINITY);
        }
        distance.put(start, 0);
        rec.badge(start, "0");
        rec.vertex(start, VertexState.FRONTIER);
        rec.snapshot("Set the distance to " + start + " to 0 and every other vertex to "
                        + Text.INFINITY + (target == null ? "." : ". Goal: reach " + target + "."),
                queueText(graph, distance, settled));

        while (true) {
            Vertex current = closestUnsettled(graph, distance, settled);
            if (current == null) break; //Everything reachable is settled.

            settled.add(current);
            rec.vertex(current, VertexState.CURRENT);
            Edge via = parentEdge.get(current);
            if (via != null) rec.edge(via, EdgeState.TREE);
            rec.snapshot("Settle " + current + ": it has the smallest distance in the queue ("
                            + distance.get(current) + "), so no shorter route to it can exist.",
                    queueText(graph, distance, settled), parent.get(current), current);

            if (current.equals(target)) {
                rec.vertex(current, VertexState.VISITED);
                rec.snapshot("Reached the target " + target + ". Stop early: every vertex still in the queue "
                        + "is at least as far away.", queueText(graph, distance, settled));
                break;
            }

            for (Edge edge : graph.sortedEdgesOf(current)) {
                Vertex neighbour = edge.other(current);
                if (settled.contains(neighbour)) continue;

                long candidate = (long) distance.get(current) + edge.getWeight();
                int known = distance.get(neighbour);
                String sum = distance.get(current) + " + " + edge.getWeight() + " = " + candidate;

                if (candidate < known) {
                    Edge previous = parentEdge.put(neighbour, edge);
                    if (previous != null) rec.edge(previous, EdgeState.NORMAL);
                    distance.put(neighbour, (int) candidate);
                    parent.put(neighbour, current);
                    rec.edge(edge, EdgeState.CONSIDERING);
                    rec.vertex(neighbour, VertexState.FRONTIER);
                    rec.badge(neighbour, String.valueOf(candidate));
                    rec.snapshot("Relax " + edge.label() + ": " + sum + ", which beats " + Text.distance(known)
                                    + ". Update " + neighbour + " to " + candidate + ".",
                            queueText(graph, distance, settled), current, neighbour);
                } else {
                    EdgeState before = rec.edgeState(edge);
                    rec.edge(edge, EdgeState.REJECTED);
                    rec.snapshot("Check " + edge.label() + ": " + sum + ", which is not better than "
                            + known + ". Keep " + neighbour + " at " + known + ".", queueText(graph, distance, settled));
                    rec.edge(edge, before);
                }
            }
            rec.vertex(current, VertexState.VISITED);
        }

        Map<Vertex, Vertex> finalParents = new HashMap<>();
        Map<Vertex, Integer> finalDistances = new HashMap<>();
        for (Vertex vertex : settled) {
            finalDistances.put(vertex, distance.get(vertex));
            if (parent.containsKey(vertex)) finalParents.put(vertex, parent.get(vertex));
        }

        if (target == null) {
            String output = "Dijkstra: " + graph.getVerticesSorted().stream()
                    .map(v -> v.getId() + "=" + Text.distance(distance.get(v)))
                    .collect(Collectors.joining(", "));
            long unreachable = distance.values().stream().filter(d -> d == UNREACHED).count();
            rec.snapshot("Done. Every reachable vertex is settled and green edges form the shortest-path tree."
                    + (unreachable > 0 ? " " + unreachable + " unreachable (" + Text.INFINITY + ")." : "")
                    + " Hover a vertex to trace its path.", "Queue: [ ] (empty)");
            return new AlgorithmResult(output, rec.steps(), start, finalParents, finalDistances);
        }

        return traceTarget(rec, graph, start, target, parent, parentEdge, settled, finalDistances);
    }

    private AlgorithmResult traceTarget(StepRecorder rec, Graph graph, Vertex start, Vertex target,
                                        Map<Vertex, Vertex> parent, Map<Vertex, Edge> parentEdge,
                                        Set<Vertex> settled,
                                        Map<Vertex, Integer> finalDistances) {
        String header = "Shortest path " + start + " -> " + target + ": ";
        if (!settled.contains(target)) {
            rec.snapshot(target + " can't be reached from " + start + ": the queue ran out first.", "Queue: [ ] (empty)");
            return new AlgorithmResult(header + "no path (" + target + " is unreachable)", rec.steps(), start,
                    Map.of(), finalDistances);
        }

        //Walk the parent links back from the target, then replay them forwards.
        List<Vertex> path = new ArrayList<>();
        for (Vertex v = target; v != null; v = parent.get(v)) path.add(0, v);

        rec.vertex(start, VertexState.PATH);
        rec.snapshot("Trace the path backwards through each vertex's parent, then draw it from " + start + ".",
                "Path: " + start);
        for (int i = 1; i < path.size(); i++) {
            Vertex from = path.get(i - 1);
            Vertex to = path.get(i);
            rec.edge(parentEdge.get(to), EdgeState.PATH);
            rec.vertex(to, VertexState.PATH);
            rec.snapshot(from + " -> " + to + " (weight " + parentEdge.get(to).getWeight() + "), running total "
                    + finalDistances.get(to) + ".", "Path: " + Text.arrows(path.subList(0, i + 1)), from, to);
        }
        int cost = finalDistances.get(target);
        rec.snapshot("Shortest path from " + start + " to " + target + ": " + Text.arrows(path) + ", total cost "
                + cost + ". Settled " + settled.size() + " of " + graph.vertexCount() + " vertices.",
                "Path: " + Text.arrows(path));
        //Parents are left out on purpose: the answer is already drawn, so hover paths are switched off.
        return new AlgorithmResult(header + Text.arrows(path) + " (cost " + cost + ")", rec.steps(), start,
                Map.of(), finalDistances);
    }

    //Unsettled vertex with the smallest finite distance; ties go to the alphabetically first ID.
    private static Vertex closestUnsettled(Graph graph, Map<Vertex, Integer> distance, Set<Vertex> settled) {
        return graph.getVertices().stream()
                .filter(v -> !settled.contains(v) && distance.get(v) != UNREACHED)
                .min(Comparator.<Vertex>comparingInt(distance::get).thenComparing(Vertex::getId))
                .orElse(null);
    }

    //Lists the frontier by distance, such as "Queue: D(2), B(4)".
    private static String queueText(Graph graph, Map<Vertex, Integer> distance, Set<Vertex> settled) {
        List<String> entries = graph.getVertices().stream()
                .filter(v -> !settled.contains(v) && distance.get(v) != UNREACHED)
                .sorted(Comparator.<Vertex>comparingInt(distance::get).thenComparing(Vertex::getId))
                .map(v -> v.getId() + "(" + distance.get(v) + ")")
                .toList();
        return "Queue: " + (entries.isEmpty() ? "[ ] (empty)" : String.join(", ", entries));
    }
}
