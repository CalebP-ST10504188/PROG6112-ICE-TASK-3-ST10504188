package model;

/**
 * Thrown when an edit would make the graph invalid. The message is shown to the user.
 */
public class GraphException extends RuntimeException {
    public GraphException(String message) {
        super(message);
    }
}
