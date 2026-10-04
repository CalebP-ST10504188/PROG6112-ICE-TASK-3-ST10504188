package algorithms;

/**
 * How an edge is drawn at one step of an algorithm.
 */
public enum EdgeState {
    NORMAL,
    //Being examined or currently the best known option.
    CONSIDERING,
    //Accepted: part of the traversal tree, shortest-path tree, or spanning tree.
    TREE,
    //Examined and rejected (for example, it would create a cycle in Kruskal's algorithm).
    REJECTED,
    //Part of a reconstructed shortest path.
    PATH
}
