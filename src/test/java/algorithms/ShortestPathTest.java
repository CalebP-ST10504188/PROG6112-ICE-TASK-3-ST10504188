package algorithms;

import model.Graph;
import model.Vertex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static algorithms.Fixtures.v;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortestPathTest {

    private final DijkstrasAlgorithm dijkstra = new DijkstrasAlgorithm();

    @Test
    @DisplayName("Dijkstra finds the detour A->C->B (3) instead of A->B (4)")
    void distances() {
        Graph g = Fixtures.tutorial();
        assertEquals("Dijkstra: A=0, B=3, C=2, D=8, E=10, F=13", dijkstra.run(g, v(g, "A")).output());
    }

    @Test
    @DisplayName("Unreachable vertices are shown as infinity")
    void unreachable() {
        Graph g = Fixtures.disconnected();
        assertEquals("Dijkstra: A=0, B=3, C=6, D=\u221E, E=\u221E, F=\u221E", dijkstra.run(g, v(g, "A")).output());
    }

    @Test
    @DisplayName("Regression: a single-vertex graph no longer crashes")
    void singleVertex() {
        Graph g = new Graph();
        Vertex a = g.addVertex("A", 0, 0);
        assertEquals("Dijkstra: A=0", dijkstra.run(g, a).output());
    }

    @Test
    @DisplayName("Negative weights are refused with a helpful message")
    void negativeWeights() {
        Graph g = Fixtures.tutorial();
        g.setWeight(g.findEdge(v(g, "B"), v(g, "D")).orElseThrow(), -3);
        String message = dijkstra.validate(g).orElseThrow();
        assertTrue(message.contains("B-D"));
        assertFalse(new PrimsAlgorithm().validate(g).isPresent());
    }

    @Test
    @DisplayName("Source-to-target search traces the winning path")
    void sourceToTarget() {
        Graph g = Fixtures.tutorial();
        AlgorithmResult result = new ShortestPathAlgorithm().run(g, v(g, "A"), v(g, "F"));
        assertEquals("Shortest path A -> F: A -> C -> B -> D -> E -> F (cost 13)", result.output());
        long pathEdges = result.finalStep().edgeStates().values().stream().filter(s -> s == EdgeState.PATH).count();
        assertEquals(5, pathEdges);
    }

    @Test
    @DisplayName("Source-to-target search stops early once the target is settled")
    void stopsEarly() {
        Graph g = Fixtures.tutorial();
        AlgorithmResult full = dijkstra.run(g, v(g, "A"));
        AlgorithmResult early = new ShortestPathAlgorithm().run(g, v(g, "A"), v(g, "B"));
        assertEquals("Shortest path A -> B: A -> C -> B (cost 3)", early.output());
        assertTrue(early.distances().size() < full.distances().size());
    }

    @Test
    @DisplayName("Unreachable target is reported, not crashed on")
    void unreachableTarget() {
        Graph g = Fixtures.disconnected();
        AlgorithmResult result = new ShortestPathAlgorithm().run(g, v(g, "A"), v(g, "E"));
        assertEquals("Shortest path A -> E: no path (E is unreachable)", result.output());
    }

    @Test
    @DisplayName("Path reconstruction matches the reported distance")
    void hoverPathMatchesDistance() {
        Graph g = Fixtures.tutorial();
        AlgorithmResult result = dijkstra.run(g, v(g, "A"));
        List<Vertex> path = result.pathTo(v(g, "F")).orElseThrow();
        int cost = 0;
        for (int i = 1; i < path.size(); i++) cost += g.findEdge(path.get(i - 1), path.get(i)).orElseThrow().getWeight();
        assertEquals(result.distances().get(v(g, "F")), cost);
    }

    @Test
    @DisplayName("Dijkstra agrees with Floyd-Warshall on 50 random graphs")
    void matchesFloydWarshall() {
        for (long seed = 1; seed <= 50; seed++) {
            Graph g = Fixtures.random(seed, 9, 14);
            List<Vertex> vs = g.getVerticesSorted();
            Map<Vertex, Integer> index = new HashMap<>();
            for (int i = 0; i < vs.size(); i++) index.put(vs.get(i), i);
            long[][] d = new long[vs.size()][vs.size()];
            for (long[] row : d) java.util.Arrays.fill(row, Long.MAX_VALUE / 4);
            for (int i = 0; i < vs.size(); i++) d[i][i] = 0;
            g.getEdges().forEach(e -> {
                int x = index.get(e.getA()), y = index.get(e.getB());
                d[x][y] = Math.min(d[x][y], e.getWeight());
                d[y][x] = Math.min(d[y][x], e.getWeight());
            });
            for (int k = 0; k < vs.size(); k++)
                for (int i = 0; i < vs.size(); i++)
                    for (int j = 0; j < vs.size(); j++)
                        d[i][j] = Math.min(d[i][j], d[i][k] + d[k][j]);

            AlgorithmResult result = dijkstra.run(g, vs.get(0));
            for (Vertex target : vs) {
                assertEquals((int) d[0][index.get(target)], result.distances().get(target),
                        "seed " + seed + ", vertex " + target);
            }
        }
    }
}
