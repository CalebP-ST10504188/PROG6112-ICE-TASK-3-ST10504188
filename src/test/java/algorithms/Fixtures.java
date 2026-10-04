package algorithms;

import model.Graph;
import model.Vertex;
import storage.GraphFileFormat;

import java.util.Random;

/**
 * Shared test graphs.
 */
final class Fixtures {

    private Fixtures() {
    }

    //Same as examples/tutorial.graph.
    static Graph tutorial() {
        return GraphFileFormat.parse("""
                vertex A 120 300
                vertex B 300 150
                vertex C 300 450
                vertex D 500 150
                vertex E 500 450
                vertex F 680 300
                edge A B 4
                edge A C 2
                edge B C 1
                edge B D 5
                edge C D 8
                edge C E 10
                edge D E 2
                edge D F 6
                edge E F 3
                """);
    }

    //Cormen et al.
    static Graph textbookMst() {
        return GraphFileFormat.parse("""
                vertex A 0 0
                vertex B 0 0
                vertex C 0 0
                vertex D 0 0
                vertex E 0 0
                vertex F 0 0
                vertex G 0 0
                vertex H 0 0
                vertex I 0 0
                edge A B 4
                edge A H 8
                edge B C 8
                edge B H 11
                edge C I 2
                edge C F 4
                edge C D 7
                edge D F 14
                edge D E 9
                edge E F 10
                edge F G 2
                edge G I 6
                edge G H 1
                edge H I 7
                """);
    }

    //A-B-C triangle, D-E pair, isolated F.
    static Graph disconnected() {
        return GraphFileFormat.parse("""
                vertex A 0 0
                vertex B 0 0
                vertex C 0 0
                vertex D 0 0
                vertex E 0 0
                vertex F 0 0
                edge A B 3
                edge B C 4
                edge A C 6
                edge D E 2
                """);
    }

    //Random connected graph with non-negative weights, reproducible from the seed.
    static Graph random(long seed, int vertices, int extraEdges) {
        Random random = new Random(seed);
        Graph graph = new Graph();
        for (int i = 0; i < vertices; i++) graph.addVertex("V" + i, 0, 0);
        Vertex[] all = graph.getVertices().toArray(new Vertex[0]);
        for (int i = 1; i < vertices; i++) { //spanning chain keeps it connected
            graph.addEdge(all[i], all[random.nextInt(i)], random.nextInt(20));
        }
        for (int i = 0; i < extraEdges; i++) {
            Vertex x = all[random.nextInt(vertices)];
            Vertex y = all[random.nextInt(vertices)];
            if (!x.equals(y) && graph.findEdge(x, y).isEmpty()) graph.addEdge(x, y, random.nextInt(20));
        }
        return graph;
    }

    static Vertex v(Graph graph, String id) {
        return graph.getVertex(id).orElseThrow();
    }
}
