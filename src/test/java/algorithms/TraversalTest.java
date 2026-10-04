package algorithms;

import model.Graph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static algorithms.Fixtures.v;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraversalTest {

    private final BFSAlgorithm bfs = new BFSAlgorithm();
    private final DFSAlgorithm dfs = new DFSAlgorithm();

    @Test
    @DisplayName("BFS visits level by level, cheapest neighbour first")
    void bfsOrder() {
        Graph g = Fixtures.tutorial();
        //From A: C(2) before B(4), then C's new neighbours D(8) and E(10), then F.
        assertEquals("BFS: A -> C -> B -> D -> E -> F", bfs.run(g, v(g, "A")).output());
    }

    @Test
    @DisplayName("DFS goes deep along the cheapest edge first")
    void dfsOrder() {
        Graph g = Fixtures.tutorial();
        //A -> C(2) -> B(1) -> D(5) -> E(2) -> F(3).
        assertEquals("DFS: A -> C -> B -> D -> E -> F", dfs.run(g, v(g, "A")).output());
    }

    @Test
    @DisplayName("Traversals only visit the start vertex's component")
    void onlyReachable() {
        Graph g = Fixtures.disconnected();
        assertEquals("BFS: A -> B -> C", bfs.run(g, v(g, "A")).output());
        assertEquals("DFS: D -> E", dfs.run(g, v(g, "D")).output());
    }

    @Test
    @DisplayName("An isolated start vertex gives just that vertex (same format as other runs)")
    void isolatedStart() {
        Graph g = Fixtures.disconnected();
        assertEquals("BFS: F", bfs.run(g, v(g, "F")).output());
        assertEquals("DFS: F", dfs.run(g, v(g, "F")).output());
    }

    @Test
    @DisplayName("Final step shows every reached vertex as done")
    void finalStepState() {
        Graph g = Fixtures.tutorial();
        for (GraphAlgorithm algorithm : new GraphAlgorithm[]{bfs, dfs}) {
            Step last = algorithm.run(g, v(g, "A")).finalStep();
            g.getVertices().forEach(vertex -> assertEquals(VertexState.VISITED, last.stateOf(vertex)));
            assertEquals(5, last.edgeStates().size()); //A traversal tree of 6 vertices has 5 edges.
        }
    }

    @Test
    @DisplayName("BFS parents give the fewest-hops path")
    void bfsPath() {
        Graph g = Fixtures.tutorial();
        AlgorithmResult result = bfs.run(g, v(g, "A"));
        assertEquals("[A, C, E]", result.pathTo(v(g, "E")).orElseThrow().toString());
        assertTrue(result.supportsPathQueries());
    }

    @Test
    @DisplayName("Running an algorithm never reorders the graph's own edge lists")
    void noSideEffects() {
        Graph g = Fixtures.tutorial();
        String before = g.edgesOf(v(g, "A")).toString();
        bfs.run(g, v(g, "A"));
        dfs.run(g, v(g, "A"));
        assertEquals(before, g.edgesOf(v(g, "A")).toString());
    }
}
