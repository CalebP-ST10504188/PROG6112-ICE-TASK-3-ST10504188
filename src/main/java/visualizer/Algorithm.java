package visualizer;

import algorithms.BFSAlgorithm;
import algorithms.DFSAlgorithm;
import algorithms.DijkstrasAlgorithm;
import algorithms.GraphAlgorithm;
import algorithms.KruskalsAlgorithm;
import algorithms.PrimsAlgorithm;
import algorithms.ShortestPathAlgorithm;

/**
 * The algorithms offered in the UI, each paired with its implementation.
 */
public enum Algorithm {

    BFS("Breadth-First Search", new BFSAlgorithm(),
            "Explores level by level with a FIFO queue. Neighbours are taken lowest weight first. "
                    + "Hover a vertex afterwards to see its fewest-hops path."),
    DFS("Depth-First Search", new DFSAlgorithm(),
            "Goes as deep as possible before backtracking. The stack shows the current branch."),
    DIJKSTRAS("Dijkstra's Algorithm", new DijkstrasAlgorithm(),
            "Finds the cheapest distance from the start to every vertex. Hover a vertex afterwards "
                    + "to trace its shortest path."),
    SHORTEST_PATH("Shortest Path (A to B)", new ShortestPathAlgorithm(),
            "Real-time pathfinding: Dijkstra's search spreads out from the source and stops as soon "
                    + "as it reaches the target."),
    PRIMS("Prim's Algorithm", new PrimsAlgorithm(),
            "Grows a minimum spanning tree from the start vertex, always adding the cheapest edge "
                    + "that leaves the tree."),
    KRUSKALS("Kruskal's Algorithm", new KruskalsAlgorithm(),
            "Builds a minimum spanning tree by taking edges cheapest first and skipping any edge "
                    + "that would form a cycle.");

    private final String label;
    private final GraphAlgorithm algorithmInstance;
    private final String description;

    Algorithm(String label, GraphAlgorithm algorithmInstance, String description) {
        this.label = label;
        this.algorithmInstance = algorithmInstance;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public GraphAlgorithm getAlgorithmInstance() {
        return algorithmInstance;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return label;
    }
}
