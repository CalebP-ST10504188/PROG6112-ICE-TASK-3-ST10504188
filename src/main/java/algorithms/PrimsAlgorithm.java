package algorithms;

import model.Edge;
import model.Graph;
import model.Vertex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Prim's minimum spanning tree: grows one tree from the start vertex, always adding the
 * cheapest edge that leaves it.
 */
public class PrimsAlgorithm implements GraphAlgorithm {

    //Cheapest first; ties go to the alphabetically first new vertex, then the first tree vertex.
    private static final Comparator<Crossing> CHEAPEST = Comparator
            .comparingInt((Crossing c) -> c.edge.getWeight())
            .thenComparing(c -> c.outside.getId())
            .thenComparing(c -> c.inside.getId());

    private record Crossing(Edge edge, Vertex inside, Vertex outside) {
    }

    @Override
    public AlgorithmResult run(Graph graph, Vertex start) {
        StepRecorder rec = new StepRecorder();
        Set<Vertex> inTree = new HashSet<>();
        Map<Vertex, Vertex> parents = new HashMap<>();
        List<Edge> treeEdges = new ArrayList<>();
        int total = 0;

        Set<Vertex> component = reachableFrom(graph, start);
        inTree.add(start);
        rec.vertex(start, VertexState.VISITED);
        rec.snapshot("Start the tree at " + start + ". Tree weight: 0.", treeText(inTree));

        if (component.size() == 1) {
            rec.snapshot(start + " has no edges, so its spanning tree is just " + start + ".", treeText(inTree));
            return new AlgorithmResult("Prim: " + start + " (no edges to span)", rec.steps(), start, Map.of(), Map.of());
        }

        while (inTree.size() < component.size()) {
            List<Crossing> crossings = new ArrayList<>();
            for (Vertex inside : inTree) {
                for (Edge edge : graph.edgesOf(inside)) {
                    Vertex outside = edge.other(inside);
                    if (!inTree.contains(outside)) crossings.add(new Crossing(edge, inside, outside));
                }
            }
            crossings.sort(CHEAPEST);
            Crossing best = crossings.get(0);

            crossings.forEach(c -> rec.edge(c.edge, EdgeState.CONSIDERING));
            rec.snapshot("Edges leaving the tree: " + crossings.stream().map(c -> c.edge.toString())
                            .collect(Collectors.joining(", ")) + ". The cheapest is " + best.edge + ".",
                    treeText(inTree));
            crossings.forEach(c -> rec.edge(c.edge, EdgeState.NORMAL));

            inTree.add(best.outside);
            parents.put(best.outside, best.inside);
            treeEdges.add(best.edge);
            total += best.edge.getWeight();
            rec.edge(best.edge, EdgeState.TREE);
            rec.vertex(best.outside, VertexState.VISITED);
            rec.snapshot("Add " + best.outside + " to the tree via " + best.edge + ". Tree weight: " + total + ".",
                    treeText(inTree), best.inside, best.outside);
        }

        String pairs = parents.keySet().stream().sorted()
                .map(child -> child.getId() + "=" + parents.get(child).getId())
                .collect(Collectors.joining(", "));
        int skipped = graph.vertexCount() - component.size();
        rec.snapshot("Minimum spanning tree complete: " + treeEdges.size() + " edges, total weight " + total + "."
                + (skipped > 0 ? " " + skipped + " vertices in other components were not reachable." : ""),
                treeText(inTree));
        return new AlgorithmResult("Prim: " + pairs, rec.steps(), start, Map.of(), Map.of());
    }

    private static Set<Vertex> reachableFrom(Graph graph, Vertex start) {
        Set<Vertex> seen = new HashSet<>();
        Queue<Vertex> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Vertex current = queue.poll();
            for (Edge edge : graph.edgesOf(current)) {
                if (seen.add(edge.other(current))) queue.add(edge.other(current));
            }
        }
        return seen;
    }

    private static String treeText(Set<Vertex> inTree) {
        return "Tree: {" + Text.ids(inTree.stream().sorted().toList()) + "}";
    }
}
