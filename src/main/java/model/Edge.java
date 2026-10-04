package model;

import java.awt.geom.Line2D;
import java.util.Objects;

/**
 * An undirected, weighted edge between two vertices.
 *
 * A-B and B-A are the same edge, and equals() and hashCode() both treat them that way.
 */
public final class Edge {

    private final Vertex a;
    private final Vertex b;
    private int weight;

    public Edge(Vertex a, Vertex b, int weight) {
        this.a = Objects.requireNonNull(a, "a");
        this.b = Objects.requireNonNull(b, "b");
        if (a.equals(b)) {
            throw new IllegalArgumentException("An edge must connect two different vertices");
        }
        this.weight = weight;
    }

    public Vertex getA() {
        return a;
    }

    public Vertex getB() {
        return b;
    }

    public int getWeight() {
        return weight;
    }

    //Package-private so weights can only be changed through Graph.setWeight().
    void setWeight(int weight) {
        this.weight = weight;
    }

    //Given one endpoint, returns the vertex at the other end.
    public Vertex other(Vertex v) {
        if (v.equals(a)) return b;
        if (v.equals(b)) return a;
        throw new IllegalArgumentException(v + " is not an endpoint of " + this);
    }

    public boolean touches(Vertex v) {
        return a.equals(v) || b.equals(v);
    }

    public boolean connects(Vertex x, Vertex y) {
        return (a.equals(x) && b.equals(y)) || (a.equals(y) && b.equals(x));
    }

    //Returns the endpoint with the alphabetically smaller ID, so labels always read A-B, never B-A.
    public Vertex first() {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public Vertex second() {
        return other(first());
    }

    public String label() {
        return first().getId() + "-" + second().getId();
    }

    public double distanceTo(int px, int py) {
        return Line2D.ptSegDist(a.getX(), a.getY(), b.getX(), b.getY(), px, py);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Edge that)) return false;
        return connects(that.a, that.b);
    }

    @Override
    public int hashCode() {
        //Adding the two hashes gives A-B and B-A the same hash code, which matches equals().
        return a.hashCode() + b.hashCode();
    }

    @Override
    public String toString() {
        return label() + "(" + weight + ")";
    }
}
