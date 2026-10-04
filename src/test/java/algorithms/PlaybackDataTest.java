package algorithms;

import model.Graph;
import model.Vertex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static algorithms.Fixtures.v;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaybackDataTest {

    @Test
    @DisplayName("Steps are immutable snapshots, so later steps can't change earlier ones")
    void snapshotsAreIndependent() {
        Graph g = Fixtures.tutorial();
        List<Step> steps = new BFSAlgorithm().run(g, v(g, "A")).steps();
        assertEquals(VertexState.FRONTIER, steps.get(0).stateOf(v(g, "A")));
        assertEquals(VertexState.VISITED, steps.get(steps.size() - 1).stateOf(v(g, "A")));
        assertThrows(UnsupportedOperationException.class,
                () -> steps.get(0).vertexStates().put(v(g, "B"), VertexState.CURRENT));
    }

    @Test
    @DisplayName("Every algorithm records at least one explained step")
    void everyStepHasAMessage() {
        Graph g = Fixtures.tutorial();
        Vertex a = v(g, "A");
        GraphAlgorithm[] all = {new BFSAlgorithm(), new DFSAlgorithm(), new DijkstrasAlgorithm(),
                new PrimsAlgorithm(), new KruskalsAlgorithm()};
        for (GraphAlgorithm algorithm : all) {
            List<Step> steps = algorithm.run(g, a).steps();
            assertFalse(steps.isEmpty());
            steps.forEach(step -> assertFalse(step.message().isBlank()));
        }
    }

    @Test
    @DisplayName("Traversal steps point along real edges")
    void traversalsFollowEdges() {
        Graph g = Fixtures.textbookMst();
        for (Step step : new DijkstrasAlgorithm().run(g, v(g, "A")).steps()) {
            if (step.hasTraversal()) assertTrue(g.findEdge(step.from(), step.to()).isPresent());
        }
    }

    @Test
    @DisplayName("pathTo returns just the source for the source itself")
    void pathToSource() {
        Graph g = Fixtures.tutorial();
        AlgorithmResult result = new DijkstrasAlgorithm().run(g, v(g, "A"));
        assertEquals(List.of(v(g, "A")), result.pathTo(v(g, "A")).orElseThrow());
    }
}
