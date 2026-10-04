package algorithms;

import model.Edge;
import model.Graph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static algorithms.Fixtures.v;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpanningTreeTest {

    private final PrimsAlgorithm prim = new PrimsAlgorithm();
    private final KruskalsAlgorithm kruskal = new KruskalsAlgorithm();

    //Sum of the weights of edges marked TREE in the final step.
    private static int treeWeight(AlgorithmResult result) {
        return result.finalStep().edgeStates().entrySet().stream()
                .filter(entry -> entry.getValue() == EdgeState.TREE)
                .mapToInt(entry -> entry.getKey().getWeight()).sum();
    }

    private static long treeEdges(AlgorithmResult result) {
        return result.finalStep().edgeStates().values().stream().filter(s -> s == EdgeState.TREE).count();
    }

    @Test
    @DisplayName("Prim's output uses the brief's child=parent format, sorted by child")
    void primFormat() {
        Graph g = Fixtures.tutorial();
        //Tree from A: A-C(2), C-B(1), B-D(5), D-E(2), E-F(3).
        assertEquals("Prim: B=C, C=A, D=B, E=D, F=E", prim.run(g, v(g, "A")).output());
    }

    @Test
    @DisplayName("Both MST algorithms find weight 37 on the textbook graph")
    void textbookWeight() {
        Graph g = Fixtures.textbookMst();
        AlgorithmResult p = prim.run(g, v(g, "A"));
        AlgorithmResult k = kruskal.run(g, null);
        assertEquals(37, treeWeight(p));
        assertEquals(37, treeWeight(k));
        assertEquals(8, treeEdges(p));
        assertEquals(8, treeEdges(k));
        assertTrue(k.output().endsWith("(total 37)"));
    }

    @Test
    @DisplayName("Kruskal's output lists edges in the order they were accepted")
    void kruskalFormat() {
        Graph g = Fixtures.tutorial();
        assertEquals("Kruskal: B-C, A-C, D-E, E-F, B-D (total 13)", kruskal.run(g, null).output());
    }

    @Test
    @DisplayName("Kruskal's marks cycle-forming edges as rejected")
    void kruskalRejectsCycles() {
        Graph g = Fixtures.tutorial();
        AlgorithmResult result = kruskal.run(g, null);
        Edge ab = g.findEdge(v(g, "A"), v(g, "B")).orElseThrow();
        assertEquals(EdgeState.REJECTED, result.finalStep().stateOf(ab));
    }

    @Test
    @DisplayName("Kruskal's builds a spanning forest on a disconnected graph")
    void kruskalForest() {
        Graph g = Fixtures.disconnected();
        AlgorithmResult result = kruskal.run(g, null);
        assertEquals("Kruskal: D-E, A-B, B-C (total 9)", result.output());
        assertNull(result.source());
        assertTrue(result.finalStep().message().contains("forest"));
    }

    @Test
    @DisplayName("Prim's spans only the start vertex's component")
    void primComponent() {
        Graph g = Fixtures.disconnected();
        assertEquals("Prim: B=A, C=B", prim.run(g, v(g, "A")).output());
        assertEquals("Prim: F (no edges to span)", prim.run(g, v(g, "F")).output());
    }

    @Test
    @DisplayName("Prim's and Kruskal's agree on total weight for 50 random graphs")
    void primMatchesKruskal() {
        for (long seed = 1; seed <= 50; seed++) {
            Graph g = Fixtures.random(seed, 10, 15);
            assertEquals(treeWeight(kruskal.run(g, null)), treeWeight(prim.run(g, v(g, "V0"))), "seed " + seed);
        }
    }

    @Test
    @DisplayName("Kruskal's handles a graph with no edges")
    void noEdges() {
        Graph g = new Graph();
        g.addVertex("A", 0, 0);
        assertEquals("Kruskal: no edges", kruskal.run(g, null).output());
    }
}
