package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphTest {

    private Graph graph;
    private Vertex a;
    private Vertex b;
    private Vertex c;

    @BeforeEach
    void setUp() {
        graph = new Graph();
        a = graph.addVertex("A", 0, 0);
        b = graph.addVertex("B", 100, 0);
        c = graph.addVertex("C", 0, 100);
    }

    @Test
    @DisplayName("Duplicate vertex IDs are rejected")
    void duplicateVertexId() {
        GraphException e = assertThrows(GraphException.class, () -> graph.addVertex("A", 50, 50));
        assertTrue(e.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Vertex IDs must be 1-3 letters or digits")
    void invalidVertexIds() {
        assertThrows(GraphException.class, () -> graph.addVertex("", 1, 1));
        assertThrows(GraphException.class, () -> graph.addVertex("ABCD", 1, 1));
        assertThrows(GraphException.class, () -> graph.addVertex("A!", 1, 1));
        assertEquals("D1", graph.addVertex(" D1 ", 1, 1).getId()); //IDs are trimmed.
    }

    @Test
    @DisplayName("Self-loops and duplicate edges are rejected")
    void invalidEdges() {
        assertThrows(GraphException.class, () -> graph.addEdge(a, a, 1));
        graph.addEdge(a, b, 1);
        assertThrows(GraphException.class, () -> graph.addEdge(b, a, 5));
    }

    @Test
    @DisplayName("Weights outside -9999..9999 are rejected, so path totals can't overflow")
    void weightRange() {
        graph.addEdge(a, b, Graph.MAX_WEIGHT);
        graph.addEdge(a, c, Graph.MIN_WEIGHT);
        assertThrows(GraphException.class, () -> graph.addEdge(b, c, Graph.MAX_WEIGHT + 1));
        Edge ab = graph.findEdge(a, b).orElseThrow();
        assertThrows(GraphException.class, () -> graph.setWeight(ab, Integer.MIN_VALUE));
        assertEquals(Graph.MAX_WEIGHT, ab.getWeight()); //Unchanged after the failed edit.
    }

    @Test
    @DisplayName("Removing a vertex removes its edges")
    void removeVertexRemovesEdges() {
        graph.addEdge(a, b, 1);
        graph.addEdge(a, c, 2);
        graph.addEdge(b, c, 3);
        graph.removeVertex(a);
        assertEquals(2, graph.vertexCount());
        assertEquals(1, graph.edgeCount());
        assertEquals(1, graph.edgesOf(b).size());
    }

    @Test
    @DisplayName("Regression: A-B and B-A are equal AND have the same hashCode")
    void edgeEqualsAndHashCodeAreConsistent() {
        Edge ab = new Edge(a, b, 4);
        Edge ba = new Edge(b, a, 4);
        assertEquals(ab, ba);
        assertEquals(ab.hashCode(), ba.hashCode());
        Set<Edge> set = new HashSet<>();
        set.add(ab);
        assertTrue(set.contains(ba));
    }

    @Test
    @DisplayName("Regression: moving a vertex does not break map lookups")
    void movingVertexKeepsIdentity() {
        Set<Vertex> set = new HashSet<>();
        set.add(a);
        graph.moveVertex(a, 500, 500);
        assertTrue(set.contains(a));
        assertEquals(500, a.getX());
    }

    @Test
    @DisplayName("Neighbours are sorted by weight, then alphabetically")
    void sortedEdges() {
        Vertex d = graph.addVertex("D", 5, 5);
        graph.addEdge(a, d, 2);
        graph.addEdge(a, c, 2);
        graph.addEdge(a, b, 1);
        assertEquals("[A-B(1), A-C(2), A-D(2)]", graph.sortedEdgesOf(a).toString());
        assertEquals("[A-D(2), A-C(2), A-B(1)]", graph.edgesOf(a).toString()); //The original order is untouched.
    }

    @Test
    @DisplayName("Suggested IDs fill the first gap")
    void suggestNextId() {
        assertEquals("D", graph.suggestNextId());
        graph.removeVertex(b);
        assertEquals("B", graph.suggestNextId());
    }

    @Test
    @DisplayName("Change listeners fire on structural edits but not on moves")
    void changeListeners() {
        int[] calls = {0};
        graph.addChangeListener(() -> calls[0]++);
        graph.addEdge(a, b, 1);
        graph.moveVertex(a, 9, 9);
        graph.setWeight(graph.findEdge(a, b).orElseThrow(), 7);
        assertEquals(2, calls[0]);
    }

    @Test
    @DisplayName("Hit-testing finds vertices and edges near a point")
    void hitTesting() {
        graph.addEdge(a, b, 1);
        assertEquals(a, graph.vertexAt(5, 5).orElseThrow());
        assertFalse(graph.vertexAt(50, 50).isPresent());
        assertTrue(graph.edgeNear(50, 4, 7).isPresent());
        assertFalse(graph.edgeNear(50, 20, 7).isPresent());
    }
}
