package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * The graph data model: vertices, edges and the adjacency lists that connect them.
 *
 * One instance owns all of the graph's state and rejects any edit that would make it invalid.
 */
public class Graph {

    //Weights are limited so that path totals can never overflow an int.
    public static final int MIN_WEIGHT = -9999;
    public static final int MAX_WEIGHT = 9999;

    //IDs are 1 to 3 letters or digits, so they fit inside a vertex circle.
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9]{1,3}");

    //Neighbour order used by every algorithm: lowest weight first, then alphabetical by ID.
    private static Comparator<Edge> byWeightThenNeighbour(Vertex from) {
        return Comparator.comparingInt(Edge::getWeight)
                .thenComparing(edge -> edge.other(from).getId());
    }

    private final Map<String, Vertex> vertices = new LinkedHashMap<>();
    private final Map<Vertex, List<Edge>> adjacency = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    public Vertex addVertex(String id, int x, int y) {
        String cleanId = id == null ? "" : id.trim();
        if (!VALID_ID.matcher(cleanId).matches()) {
            throw new GraphException("Vertex IDs must be 1 to 3 letters or digits (got \"" + cleanId + "\").");
        }
        if (vertices.containsKey(cleanId)) {
            throw new GraphException("A vertex called " + cleanId + " already exists.");
        }
        Vertex vertex = new Vertex(cleanId, x, y);
        vertices.put(cleanId, vertex);
        adjacency.put(vertex, new ArrayList<>());
        fireChanged();
        return vertex;
    }

    public void removeVertex(Vertex vertex) {
        requireVertex(vertex);
        for (Edge edge : new ArrayList<>(adjacency.get(vertex))) {
            unlinkEdge(edge);
        }
        adjacency.remove(vertex);
        vertices.remove(vertex.getId());
        fireChanged();
    }

    //Moving a vertex changes the layout, not the structure, so listeners are not notified.
    public void moveVertex(Vertex vertex, int x, int y) {
        requireVertex(vertex);
        vertex.moveTo(x, y);
    }

    public Optional<Vertex> getVertex(String id) {
        return Optional.ofNullable(vertices.get(id));
    }

    public Collection<Vertex> getVertices() {
        return Collections.unmodifiableCollection(vertices.values());
    }

    public List<Vertex> getVerticesSorted() {
        List<Vertex> sorted = new ArrayList<>(vertices.values());
        Collections.sort(sorted);
        return sorted;
    }

    public boolean containsVertex(Vertex vertex) {
        return vertex != null && vertices.get(vertex.getId()) == vertex;
    }

    public int vertexCount() {
        return vertices.size();
    }

    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    public Edge addEdge(Vertex a, Vertex b, int weight) {
        requireVertex(a);
        requireVertex(b);
        if (a.equals(b)) {
            throw new GraphException("An edge can't connect " + a.getId() + " to itself.");
        }
        if (findEdge(a, b).isPresent()) {
            throw new GraphException(a.getId() + " and " + b.getId() + " are already connected.");
        }
        requireWeightInRange(weight);
        Edge edge = new Edge(a, b, weight);
        edges.add(edge);
        adjacency.get(a).add(edge);
        adjacency.get(b).add(edge);
        fireChanged();
        return edge;
    }

    public void removeEdge(Edge edge) {
        if (!edges.contains(edge)) {
            throw new GraphException("That edge is not part of this graph.");
        }
        unlinkEdge(edge);
        fireChanged();
    }

    public void setWeight(Edge edge, int weight) {
        if (!edges.contains(edge)) {
            throw new GraphException("That edge is not part of this graph.");
        }
        requireWeightInRange(weight);
        edge.setWeight(weight);
        fireChanged();
    }

    public Optional<Edge> findEdge(Vertex a, Vertex b) {
        if (!adjacency.containsKey(a)) return Optional.empty();
        for (Edge edge : adjacency.get(a)) {
            if (edge.connects(a, b)) return Optional.of(edge);
        }
        return Optional.empty();
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public int edgeCount() {
        return edges.size();
    }

    public List<Edge> edgesOf(Vertex vertex) {
        requireVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    //Sorts a copy, so the graph's own edge lists are never reordered.
    public List<Edge> sortedEdgesOf(Vertex vertex) {
        List<Edge> sorted = new ArrayList<>(edgesOf(vertex));
        sorted.sort(byWeightThenNeighbour(vertex));
        return sorted;
    }

    public Optional<Edge> firstNegativeEdge() {
        return edges.stream().filter(edge -> edge.getWeight() < 0).findFirst();
    }

    //Checks the newest vertex first, so overlapping circles return the one drawn on top.
    public Optional<Vertex> vertexAt(int x, int y) {
        List<Vertex> all = new ArrayList<>(vertices.values());
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).contains(x, y)) return Optional.of(all.get(i));
        }
        return Optional.empty();
    }

    public Optional<Edge> edgeNear(int x, int y, double tolerance) {
        Edge best = null;
        double bestDistance = tolerance;
        for (Edge edge : edges) {
            double distance = edge.distanceTo(x, y);
            if (distance <= bestDistance) {
                best = edge;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    //Suggests the first unused ID: A to Z, then A1 to Z99.
    public String suggestNextId() {
        for (char c = 'A'; c <= 'Z'; c++) {
            String id = String.valueOf(c);
            if (!vertices.containsKey(id)) return id;
        }
        for (int n = 1; n <= 99; n++) {
            for (char c = 'A'; c <= 'Z'; c++) {
                String id = c + String.valueOf(n);
                if (!vertices.containsKey(id)) return id;
            }
        }
        return "";
    }

    public void clear() {
        vertices.clear();
        adjacency.clear();
        edges.clear();
        fireChanged();
    }

    //Listeners are called whenever vertices, edges or weights are added, removed or edited.
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    private void fireChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    private void unlinkEdge(Edge edge) {
        edges.remove(edge);
        adjacency.get(edge.getA()).remove(edge);
        adjacency.get(edge.getB()).remove(edge);
    }

    private static void requireWeightInRange(int weight) {
        if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
            throw new GraphException("Weights must be between " + MIN_WEIGHT + " and " + MAX_WEIGHT
                    + " (got " + weight + ").");
        }
    }

    private void requireVertex(Vertex vertex) {
        if (!containsVertex(vertex)) {
            throw new GraphException("Vertex " + vertex + " is not part of this graph.");
        }
    }
}
