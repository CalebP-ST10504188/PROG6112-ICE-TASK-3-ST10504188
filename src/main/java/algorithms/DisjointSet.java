package algorithms;

import java.util.HashMap;
import java.util.Map;

/**
 * Union-find with path compression, used by Kruskal's algorithm to check whether two vertices
 * are already connected.
 */
final class DisjointSet<T> {

    private final Map<T, T> parent = new HashMap<>();
    private final Map<T, Integer> size = new HashMap<>();

    void add(T item) {
        parent.putIfAbsent(item, item);
        size.putIfAbsent(item, 1);
    }

    T find(T item) {
        T root = parent.get(item);
        if (root == null) throw new IllegalArgumentException("Unknown item: " + item);
        if (!root.equals(item)) {
            root = find(root);
            parent.put(item, root); //Path compression.
        }
        return root;
    }

    //Merges the two sets. Returns false if they were already one set.
    boolean union(T x, T y) {
        T rootX = find(x);
        T rootY = find(y);
        if (rootX.equals(rootY)) return false;
        if (size.get(rootX) < size.get(rootY)) {
            T swap = rootX;
            rootX = rootY;
            rootY = swap;
        }
        parent.put(rootY, rootX);
        size.put(rootX, size.get(rootX) + size.get(rootY));
        return true;
    }
}
