package algorithms;

/**
 * How a vertex is drawn at one step of an algorithm.
 */
public enum VertexState {
    //Not reached yet.
    UNVISITED,
    //Discovered and waiting to be processed (in the queue, on the stack, or in the priority queue).
    FRONTIER,
    //The vertex the algorithm is working on right now.
    CURRENT,
    //Finished: visited, settled, or added to the spanning tree.
    VISITED,
    //Part of a reconstructed shortest path.
    PATH
}
