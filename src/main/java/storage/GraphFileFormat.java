package storage;

import model.Edge;
import model.Graph;
import model.GraphException;
import model.Vertex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves and loads graphs as readable .graph text files, one vertex or edge per line,
 * such as "vertex A 120 200" or "edge A B 4".
 */
public final class GraphFileFormat {

    public static final String EXTENSION = "graph";
    private static final String HEADER = "# Graph Algorithms Visualizer";

    private GraphFileFormat() {
    }

    public static String write(Graph graph) {
        StringBuilder out = new StringBuilder(HEADER).append(System.lineSeparator());
        for (Vertex vertex : graph.getVertices()) {
            out.append("vertex ").append(vertex.getId()).append(' ')
                    .append(vertex.getX()).append(' ').append(vertex.getY()).append(System.lineSeparator());
        }
        for (Edge edge : graph.getEdges()) {
            out.append("edge ").append(edge.getA().getId()).append(' ').append(edge.getB().getId())
                    .append(' ').append(edge.getWeight()).append(System.lineSeparator());
        }
        return out.toString();
    }

    public static void save(Graph graph, Path file) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(write(graph));
        }
    }

    //Reads a graph into target. Nothing changes unless the whole file is valid, so a broken file
    //never leaves the canvas half-loaded.
    public static void read(Reader source, Graph target) throws IOException {
        Graph parsed = new Graph();
        BufferedReader reader = new BufferedReader(source);
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            String[] parts = trimmed.split("\\s+");
            try {
                switch (parts[0].toLowerCase()) {
                    case "vertex" -> {
                        expectParts(parts, 4, "vertex <id> <x> <y>");
                        parsed.addVertex(parts[1], parseInt(parts[2], "x"), parseInt(parts[3], "y"));
                    }
                    case "edge" -> {
                        expectParts(parts, 4, "edge <from> <to> <weight>");
                        Vertex a = parsed.getVertex(parts[1])
                                .orElseThrow(() -> new GraphException("Unknown vertex \"" + parts[1] + "\"."));
                        Vertex b = parsed.getVertex(parts[2])
                                .orElseThrow(() -> new GraphException("Unknown vertex \"" + parts[2] + "\"."));
                        parsed.addEdge(a, b, parseInt(parts[3], "weight"));
                    }
                    default -> throw new GraphException("Expected \"vertex\" or \"edge\" but found \"" + parts[0] + "\".");
                }
            } catch (GraphException e) {
                throw new GraphFormatException(lineNumber, e.getMessage());
            }
        }
        copyInto(parsed, target);
    }

    public static void load(Path file, Graph target) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            read(reader, target);
        }
    }

    public static Graph parse(String text) {
        Graph graph = new Graph();
        try {
            read(new StringReader(text), graph);
        } catch (IOException e) {
            throw new IllegalStateException("StringReader cannot fail", e);
        }
        return graph;
    }

    //Loads a bundled example from src/main/resources/examples.
    public static void loadExample(String resourceName, Graph target) throws IOException {
        InputStream stream = GraphFileFormat.class.getResourceAsStream("/examples/" + resourceName);
        if (stream == null) throw new IOException("Missing example: " + resourceName);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            read(reader, target);
        }
    }

    private static void copyInto(Graph source, Graph target) {
        target.clear();
        for (Vertex vertex : source.getVertices()) {
            target.addVertex(vertex.getId(), vertex.getX(), vertex.getY());
        }
        for (Edge edge : source.getEdges()) {
            target.addEdge(target.getVertex(edge.getA().getId()).orElseThrow(),
                    target.getVertex(edge.getB().getId()).orElseThrow(), edge.getWeight());
        }
    }

    private static void expectParts(String[] parts, int count, String usage) {
        if (parts.length != count) throw new GraphException("Expected \"" + usage + "\".");
    }

    private static int parseInt(String text, String field) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new GraphException("The " + field + " \"" + text + "\" is not a whole number.");
        }
    }

    //A problem in a graph file, with the line number it occurred on.
    public static class GraphFormatException extends GraphException {
        private final int line;

        public GraphFormatException(int line, String message) {
            super("Line " + line + ": " + message);
            this.line = line;
        }

        public int getLine() {
            return line;
        }
    }
}
