package storage;

import model.Graph;
import model.GraphException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphFileFormatTest {

    @Test
    @DisplayName("Saving and loading gives back the same graph")
    void roundTrip() {
        Graph original = new Graph();
        var a = original.addVertex("A", 10, 20);
        var b = original.addVertex("B", 30, 40);
        original.addEdge(a, b, -7);
        Graph copy = GraphFileFormat.parse(GraphFileFormat.write(original));
        assertEquals(GraphFileFormat.write(original), GraphFileFormat.write(copy));
        assertEquals(-7, copy.getEdges().get(0).getWeight());
        assertEquals(40, copy.getVertex("B").orElseThrow().getY());
    }

    @Test
    @DisplayName("Comments and blank lines are ignored")
    void comments() {
        Graph g = GraphFileFormat.parse("# hello\n\nvertex A 1 2\n   # indented comment\n");
        assertEquals(1, g.vertexCount());
    }

    @Test
    @DisplayName("Errors report the line number")
    void lineNumbers() {
        var e = assertThrows(GraphFileFormat.GraphFormatException.class,
                () -> GraphFileFormat.parse("vertex A 1 2\nedge A Q 3\n"));
        assertEquals(2, e.getLine());
        assertTrue(e.getMessage().contains("Unknown vertex \"Q\""));
        assertThrows(GraphException.class, () -> GraphFileFormat.parse("vertex A one 2"));
        assertThrows(GraphException.class, () -> GraphFileFormat.parse("circle A 1 2"));
        var tooHeavy = assertThrows(GraphFileFormat.GraphFormatException.class,
                () -> GraphFileFormat.parse("vertex A 1 1\nvertex B 2 2\nedge A B 2000000000"));
        assertEquals(3, tooHeavy.getLine());
    }

    @Test
    @DisplayName("A broken file leaves the existing graph untouched")
    void atomicLoad() throws Exception {
        Graph target = GraphFileFormat.parse("vertex Z 5 5");
        assertThrows(GraphException.class,
                () -> GraphFileFormat.read(new StringReader("vertex A 1 1\nvertex A 2 2"), target));
        assertEquals(1, target.vertexCount());
        assertTrue(target.getVertex("Z").isPresent());
    }

    @Test
    @DisplayName("All bundled examples load")
    void examplesLoad() throws Exception {
        for (String name : new String[]{"tutorial.graph", "city-grid.graph", "textbook-mst.graph", "two-islands.graph"}) {
            Graph g = new Graph();
            GraphFileFormat.loadExample(name, g);
            assertTrue(g.vertexCount() > 0, name);
        }
    }
}
