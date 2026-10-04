package algorithms;

import model.Edge;
import model.Graph;
import model.Vertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Kruskal's minimum spanning tree: takes edges cheapest first and skips any edge that would
 * form a cycle.
 *
 * It works on the whole graph, so it builds a spanning forest when the graph is disconnected.
 */
public class KruskalsAlgorithm implements GraphAlgorithm {

    @Override
    public boolean needsStartVertex() {
        return false;
    }

    @Override
    public AlgorithmResult run(Graph graph, Vertex ignoredStart) {
        StepRecorder rec = new StepRecorder();
        DisjointSet<Vertex> components = new DisjointSet<>();
        graph.getVertices().forEach(components::add);

        List<Edge> sorted = new ArrayList<>(graph.getEdges());
        sorted.sort(Comparator.comparingInt(Edge::getWeight).thenComparing(Edge::label));

        if (sorted.isEmpty()) {
            rec.snapshot("The graph has no edges, so there is nothing to connect.", componentText(graph, components));
            return new AlgorithmResult("Kruskal: no edges", rec.steps(), null, Map.of(), Map.of());
        }

        rec.snapshot("Sort every edge by weight: " + sorted.stream().map(Edge::toString)
                        .collect(Collectors.joining(", ")) + ". Each vertex starts as its own component.",
                componentText(graph, components));

        List<Edge> accepted = new ArrayList<>();
        int total = 0;
        int needed = graph.vertexCount() - 1;

        for (Edge edge : sorted) {
            if (accepted.size() == needed) break; //A spanning tree of a connected graph has V-1 edges.

            rec.edge(edge, EdgeState.CONSIDERING);
            rec.snapshot("Next cheapest edge: " + edge + ".", componentText(graph, components));

            if (components.union(edge.getA(), edge.getB())) {
                accepted.add(edge);
                total += edge.getWeight();
                rec.edge(edge, EdgeState.TREE);
                rec.vertex(edge.getA(), VertexState.VISITED);
                rec.vertex(edge.getB(), VertexState.VISITED);
                rec.snapshot(edge.getA() + " and " + edge.getB() + " were in different components, so keep "
                                + edge.label() + " and merge them. Total weight: " + total + ".",
                        componentText(graph, components), edge.first(), edge.second());
            } else {
                rec.edge(edge, EdgeState.REJECTED);
                rec.snapshot(edge.getA() + " and " + edge.getB() + " are already connected, so "
                        + edge.label() + " would create a cycle. Skip it.", componentText(graph, components));
            }
        }

        long trees = graph.getVertices().stream().map(components::find).distinct().count();
        String list = accepted.stream().map(Edge::label).collect(Collectors.joining(", "));
        rec.snapshot((trees == 1 ? "Minimum spanning tree" : "Minimum spanning forest (" + trees + " components)")
                + " complete: " + accepted.size() + " edges, total weight " + total + ".",
                componentText(graph, components));
        return new AlgorithmResult("Kruskal: " + list + " (total " + total + ")", rec.steps(), null, Map.of(), Map.of());
    }

    //Groups vertices by component, such as "Components: {A, B, D} {C}".
    private static String componentText(Graph graph, DisjointSet<Vertex> components) {
        Map<String, List<String>> groups = new TreeMap<>();
        for (Vertex vertex : graph.getVerticesSorted()) {
            groups.computeIfAbsent(components.find(vertex).getId(), k -> new ArrayList<>()).add(vertex.getId());
        }
        return "Components: " + groups.values().stream()
                .sorted(Comparator.comparing(group -> group.get(0)))
                .map(group -> "{" + String.join(", ", group) + "}")
                .collect(Collectors.joining(" "));
    }
}
