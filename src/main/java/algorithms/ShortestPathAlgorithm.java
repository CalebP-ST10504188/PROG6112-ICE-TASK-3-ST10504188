package algorithms;

/**
 * Dijkstra's algorithm from a chosen source vertex to a chosen target vertex.
 */
public class ShortestPathAlgorithm extends DijkstrasAlgorithm {

    @Override
    public boolean needsTargetVertex() {
        return true;
    }
}
