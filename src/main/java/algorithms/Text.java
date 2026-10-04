package algorithms;

import model.Vertex;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Formatting helpers shared by the algorithms.
 */
final class Text {

    static final String INFINITY = "\u221E"; //The infinity symbol.

    private Text() {
    }

    //Formats vertices as "A, B, C".
    static String ids(Collection<Vertex> vertices) {
        return vertices.stream().map(Vertex::getId).collect(Collectors.joining(", "));
    }

    //Formats vertices as "A -> B -> C".
    static String arrows(Collection<Vertex> vertices) {
        return vertices.stream().map(Vertex::getId).collect(Collectors.joining(" -> "));
    }

    static String distance(int distance) {
        return distance == Integer.MAX_VALUE ? INFINITY : String.valueOf(distance);
    }
}
