package model;

import java.util.Objects;

/**
 * A vertex in the graph.
 *
 * Vertices are equal when their IDs match, so moving a vertex does not break map lookups.
 */
public final class Vertex implements Comparable<Vertex> {

    public static final int RADIUS = 24;

    private final String id;
    private int x;
    private int y;

    public Vertex(String id, int x, int y) {
        this.id = Objects.requireNonNull(id, "id");
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    //Package-private so positions can only be changed through Graph.moveVertex().
    void moveTo(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    public boolean contains(int px, int py) {
        long dx = px - x;
        long dy = py - y;
        return dx * dx + dy * dy <= (long) RADIUS * RADIUS;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Vertex that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int compareTo(Vertex other) {
        return id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return id;
    }
}
