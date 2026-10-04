package visualizer;

/**
 * The editing modes available from the Mode menu and the toolbar.
 */
public enum Mode {

    NONE("None", "Choose an algorithm, then click a start vertex. Drag vertices to tidy the layout."),
    ADD_A_VERTEX("Add a Vertex", "Click empty space to add a vertex. Drag a vertex to move it."),
    ADD_AN_EDGE("Add an Edge", "Click two vertices, or drag from one to another, to connect them."),
    REMOVE_A_VERTEX("Remove a Vertex", "Click a vertex to delete it along with its edges."),
    REMOVE_AN_EDGE("Remove an Edge", "Click an edge to delete it.");

    private final String description;
    private final String hint;

    Mode(String description, String hint) {
        this.description = description;
        this.hint = hint;
    }

    public String getDescription() {
        return description;
    }

    public String getHint() {
        return hint;
    }

    public boolean removes() {
        return this == REMOVE_A_VERTEX || this == REMOVE_AN_EDGE;
    }
}
