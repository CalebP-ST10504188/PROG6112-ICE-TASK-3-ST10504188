package visualizer;

import algorithms.AlgorithmResult;
import algorithms.EdgeState;
import algorithms.Step;
import algorithms.VertexState;
import model.Edge;
import model.Graph;
import model.GraphException;
import model.Vertex;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The drawing canvas: paints the graph and handles all mouse input.
 *
 * Colours come from the current algorithm step, so the canvas redraws as playback moves.
 */
public class GraphPanel extends JPanel {

    //What the canvas needs from the window around it.
    public interface Host {
        void showStatus(String message, StatusType type);

        //Called when the user clicks a vertex while an algorithm is waiting for one.
        void vertexPicked(Vertex vertex);

        boolean isPickingVertex();

        //Label of the selected algorithm, or null if none is selected.
        String selectedAlgorithmLabel();
    }

    private static final int MIN_VERTEX_GAP = Vertex.RADIUS * 2 + 12;
    private static final double EDGE_HIT_TOLERANCE = 7;
    private static final int DRAG_THRESHOLD = 4;
    private static final String WEIGHT_PATTERN = "-?\\d{1,9}";

    private final Graph graph;
    private final PlaybackController playback;
    private final Host host;
    private Mode mode = Mode.ADD_A_VERTEX;

    private Vertex edgeStart;
    private Vertex dragVertex;
    private Point pressPoint;
    private boolean dragging;
    private Point mouse;
    private Vertex hoverVertex;
    private Edge hoverEdge;

    private Vertex sourceMarker;
    private Vertex targetMarker;

    //The path shown when hovering over a vertex after a run has finished.
    private List<Vertex> pathPreview = List.of();
    private String pathPreviewLabel;

    //Animates a highlight travelling along the edge used in the current step.
    private final Timer animationTimer;
    private long animationStart;
    private int animationDuration;
    private int lastStepIndex = -1;

    public GraphPanel(Graph graph, PlaybackController playback, Host host) {
        this.graph = graph;
        this.playback = playback;
        this.host = host;
        setName("Graph");
        setBackground(Theme.BACKGROUND);
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);

        animationTimer = new Timer(15, e -> {
            if (animationProgress() >= 1) ((Timer) e.getSource()).stop();
            repaint();
        });

        playback.addChangeListener(this::onPlaybackChanged);

        MouseAdapter mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        resetInteraction();
    }

    //Cancels a half-drawn edge, a drag, and any hover highlight.
    public void resetInteraction() {
        edgeStart = null;
        dragVertex = null;
        dragging = false;
        clearPathPreview();
        repaint();
    }

    public void setMarkers(Vertex source, Vertex target) {
        this.sourceMarker = source;
        this.targetMarker = target;
        repaint();
    }

    public Vertex getSourceMarker() {
        return sourceMarker;
    }

    //Moves vertices so the whole graph is visible. With onlyIfNeeded, it only acts when a vertex
    //is off the canvas.
    public void fitGraph(boolean onlyIfNeeded) {
        if (graph.isEmpty() || getWidth() == 0 || getHeight() == 0) return;
        int margin = Vertex.RADIUS + 14;
        int bottomMargin = margin + 22; //Room for the START and TARGET tags.
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Vertex v : graph.getVertices()) {
            minX = Math.min(minX, v.getX());
            minY = Math.min(minY, v.getY());
            maxX = Math.max(maxX, v.getX());
            maxY = Math.max(maxY, v.getY());
        }
        int availableW = getWidth() - 2 * margin;
        int availableH = getHeight() - margin - bottomMargin;
        boolean fits = minX >= margin && minY >= margin && maxX <= getWidth() - margin && maxY <= getHeight() - bottomMargin;
        if ((onlyIfNeeded && fits) || availableW <= 0 || availableH <= 0) return;

        double scale = Math.min(1.0, Math.min(
                maxX == minX ? 1.0 : availableW / (double) (maxX - minX),
                maxY == minY ? 1.0 : availableH / (double) (maxY - minY)));
        double offsetX = margin + (availableW - (maxX - minX) * scale) / 2;
        double offsetY = margin + (availableH - (maxY - minY) * scale) / 2;
        for (Vertex v : graph.getVertices()) {
            graph.moveVertex(v, (int) Math.round(offsetX + (v.getX() - minX) * scale),
                    (int) Math.round(offsetY + (v.getY() - minY) * scale));
        }
        repaint();
    }

    private void onPlaybackChanged() {
        int index = playback.getIndex();
        Step step = playback.currentStep();
        boolean movedForwardOneStep = index == lastStepIndex + 1;
        lastStepIndex = index;

        if (step != null && step.hasTraversal() && movedForwardOneStep) {
            animationDuration = Math.max(120, Math.min(480, (int) (playback.getDelay() * 0.65)));
            animationStart = System.currentTimeMillis();
            animationTimer.restart();
        } else {
            animationTimer.stop();
            animationStart = 0;
        }
        if (!playback.isAtEnd()) clearPathPreview();
        repaint();
    }

    private double animationProgress() {
        if (animationStart == 0) return 1;
        return Math.min(1.0, (System.currentTimeMillis() - animationStart) / (double) animationDuration);
    }

    private boolean pathQueriesEnabled() {
        AlgorithmResult result = playback.getResult();
        return result != null && result.supportsPathQueries() && playback.isAtEnd();
    }

    private void updatePathPreview(Vertex vertex) {
        AlgorithmResult result = playback.getResult();
        if (vertex == null || !pathQueriesEnabled()) {
            clearPathPreview();
            return;
        }
        Optional<List<Vertex>> path = result.pathTo(vertex);
        if (path.isEmpty()) {
            pathPreview = List.of();
            pathPreviewLabel = vertex + " is unreachable from " + result.source();
        } else {
            pathPreview = path.get();
            Integer cost = result.distances().get(vertex);
            String joined = String.join(" \u2192 ", pathPreview.stream().map(Vertex::getId).toList());
            int hops = pathPreview.size() - 1;
            pathPreviewLabel = joined + "  \u00B7  " + (cost != null ? "cost " + cost : hops + (hops == 1 ? " hop" : " hops"));
        }
    }

    private void clearPathPreview() {
        pathPreview = List.of();
        pathPreviewLabel = null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        paintGrid(g2);

        Step step = playback.currentStep();
        Set<Edge> previewEdges = edgesAlong(pathPreview);
        Edge animatedEdge = animatedEdge(step);

        for (Edge edge : graph.getEdges()) {
            if (edge.equals(animatedEdge)) continue;
            paintEdgeLine(g2, edge, edgeStateFor(edge, step, previewEdges));
        }
        if (animatedEdge != null) paintTraversal(g2, step, animatedEdge);
        paintRubberBand(g2);
        for (Edge edge : graph.getEdges()) {
            paintWeight(g2, edge, edgeStateFor(edge, step, previewEdges));
        }
        Set<Vertex> previewVertices = new HashSet<>(pathPreview);
        for (Vertex vertex : graph.getVertices()) {
            VertexState state = previewVertices.contains(vertex) ? VertexState.PATH
                    : step == null ? VertexState.UNVISITED : step.stateOf(vertex);
            paintVertex(g2, vertex, state, step == null ? null : step.badges().get(vertex));
        }

        if (graph.isEmpty()) paintEmptyHint(g2);
        if (step != null) paintLegend(g2);
        if (pathPreviewLabel != null && mouse != null) paintPathLabel(g2);
        g2.dispose();
    }

    private void paintGrid(Graphics2D g2) {
        g2.setColor(Theme.GRID_DOT);
        for (int x = 12; x < getWidth(); x += 24) {
            for (int y = 12; y < getHeight(); y += 24) {
                g2.fillRect(x, y, 2, 2);
            }
        }
    }

    private EdgeState edgeStateFor(Edge edge, Step step, Set<Edge> previewEdges) {
        if (previewEdges.contains(edge)) return EdgeState.PATH;
        return step == null ? EdgeState.NORMAL : step.stateOf(edge);
    }

    private void paintEdgeLine(Graphics2D g2, Edge edge, EdgeState state) {
        Vertex a = edge.getA();
        Vertex b = edge.getB();
        if (mode == Mode.REMOVE_AN_EDGE && edge.equals(hoverEdge)) {
            g2.setColor(Theme.REJECTED);
            g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        } else {
            g2.setColor(edgeColor(state, edge.equals(hoverEdge)));
            g2.setStroke(edgeStroke(state));
        }
        g2.draw(new Line2D.Double(a.getX(), a.getY(), b.getX(), b.getY()));
    }

    //Draws the edge being followed in this step as a line that grows from one vertex to the other.
    private void paintTraversal(Graphics2D g2, Step step, Edge edge) {
        double t = easeOut(animationProgress());
        Vertex from = step.from();
        Vertex to = step.to();
        double x = from.getX() + (to.getX() - from.getX()) * t;
        double y = from.getY() + (to.getY() - from.getY()) * t;
        EdgeState state = step.stateOf(edge);

        g2.setColor(edgeColor(EdgeState.NORMAL, false));
        g2.setStroke(edgeStroke(EdgeState.NORMAL));
        g2.draw(new Line2D.Double(from.getX(), from.getY(), to.getX(), to.getY()));

        g2.setColor(edgeColor(state, false));
        g2.setStroke(edgeStroke(state == EdgeState.NORMAL ? EdgeState.TREE : state));
        g2.draw(new Line2D.Double(from.getX(), from.getY(), x, y));

        if (t < 1) {
            g2.setColor(withAlpha(Color.WHITE, 90));
            g2.fill(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(x - 4.5, y - 4.5, 9, 9));
        }
    }

    private void paintRubberBand(Graphics2D g2) {
        if (edgeStart == null || mouse == null) return;
        g2.setColor(Theme.ACCENT);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{7f, 6f}, 0f));
        g2.draw(new Line2D.Double(edgeStart.getX(), edgeStart.getY(), mouse.x, mouse.y));
    }

    private void paintWeight(Graphics2D g2, Edge edge, EdgeState state) {
        String text = String.valueOf(edge.getWeight());
        double mx = (edge.getA().getX() + edge.getB().getX()) / 2.0;
        double my = (edge.getA().getY() + edge.getB().getY()) / 2.0;
        g2.setFont(Theme.EDGE_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        double w = Math.max(22, fm.stringWidth(text) + 12);
        double h = 20;
        RoundRectangle2D pill = new RoundRectangle2D.Double(mx - w / 2, my - h / 2, w, h, h, h);
        g2.setColor(Theme.PANEL);
        g2.fill(pill);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(state == EdgeState.NORMAL ? Theme.BORDER : edgeColor(state, false));
        g2.draw(pill);
        g2.setColor(state == EdgeState.REJECTED ? Theme.REJECTED : Theme.TEXT);
        g2.drawString(text, (float) (mx - fm.stringWidth(text) / 2.0), (float) (my + fm.getAscent() / 2.0 - 1));
    }

    private void paintVertex(Graphics2D g2, Vertex vertex, VertexState state, String badge) {
        int r = Vertex.RADIUS;
        double x = vertex.getX();
        double y = vertex.getY();
        Color fill;
        Color border;
        Color text = Theme.TEXT;
        switch (state) {
            case FRONTIER -> { fill = Theme.FRONTIER_FILL; border = Theme.FRONTIER; }
            case CURRENT -> { fill = Theme.CURRENT; border = Color.WHITE; text = Theme.BACKGROUND; }
            case VISITED -> { fill = Theme.VISITED_FILL; border = Theme.VISITED; }
            case PATH -> { fill = Theme.PATH_FILL; border = Theme.PATH; text = Color.WHITE; }
            default -> { fill = Theme.VERTEX_FILL; border = Theme.VERTEX_BORDER; }
        }
        if (vertex.equals(hoverVertex) && state == VertexState.UNVISITED) {
            border = mode == Mode.REMOVE_A_VERTEX ? Theme.REJECTED : Theme.ACCENT;
        }

        if (state == VertexState.CURRENT) {
            g2.setColor(withAlpha(Theme.CURRENT, 70));
            g2.fill(new Ellipse2D.Double(x - r - 9, y - r - 9, 2 * r + 18, 2 * r + 18));
        }
        Ellipse2D circle = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
        g2.setColor(fill);
        g2.fill(circle);
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(border);
        g2.draw(circle);

        if (vertex.equals(edgeStart)) {
            dashedRing(g2, x, y, r + 7, Theme.ACCENT);
        }

        g2.setFont(Theme.VERTEX_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(text);
        g2.drawString(vertex.getId(), (float) (x - fm.stringWidth(vertex.getId()) / 2.0),
                (float) (y + fm.getAscent() / 2.0 - 2));

        if (vertex.equals(sourceMarker)) paintTag(g2, vertex, targetMarker != null || isShortestPathPending() ? "SOURCE" : "START");
        if (vertex.equals(targetMarker)) paintTag(g2, vertex, "TARGET");
        if (badge != null) paintBadge(g2, vertex, badge, border);
    }

    private boolean isShortestPathPending() {
        String label = host.selectedAlgorithmLabel();
        return label != null && label.equals(Algorithm.SHORTEST_PATH.getLabel());
    }

    private void paintBadge(Graphics2D g2, Vertex vertex, String text, Color accent) {
        g2.setFont(Theme.BADGE);
        FontMetrics fm = g2.getFontMetrics();
        double w = Math.max(20, fm.stringWidth(text) + 10);
        double h = 17;
        double bx = vertex.getX() + Vertex.RADIUS * 0.55;
        double by = vertex.getY() - Vertex.RADIUS - h + 4;
        RoundRectangle2D pill = new RoundRectangle2D.Double(bx, by, w, h, h, h);
        g2.setColor(Theme.BACKGROUND);
        g2.fill(pill);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(accent.equals(Color.WHITE) ? Theme.CURRENT : accent);
        g2.draw(pill);
        g2.setColor(Theme.TEXT);
        g2.drawString(text, (float) (bx + (w - fm.stringWidth(text)) / 2), (float) (by + h / 2 + fm.getAscent() / 2.0 - 1.5));
    }

    private void paintTag(Graphics2D g2, Vertex vertex, String text) {
        g2.setFont(Theme.BADGE);
        FontMetrics fm = g2.getFontMetrics();
        float tx = (float) (vertex.getX() - fm.stringWidth(text) / 2.0);
        float ty = vertex.getY() + Vertex.RADIUS + 16;
        g2.setColor(Theme.BACKGROUND);
        g2.fillRect((int) tx - 3, (int) ty - fm.getAscent(), fm.stringWidth(text) + 6, fm.getHeight());
        g2.setColor(text.equals("TARGET") ? Theme.PATH : Theme.CURRENT);
        g2.drawString(text, tx, ty);
    }

    private void paintEmptyHint(Graphics2D g2) {
        String line1 = mode == Mode.ADD_A_VERTEX ? "Click anywhere to add your first vertex"
                : "The canvas is empty";
        String line2 = "or open a ready-made graph from File \u2192 Examples";
        g2.setFont(Theme.TITLE);
        FontMetrics fm1 = g2.getFontMetrics();
        g2.setColor(Theme.MUTED);
        g2.drawString(line1, (getWidth() - fm1.stringWidth(line1)) / 2, getHeight() / 2 - 6);
        g2.setFont(Theme.UI);
        FontMetrics fm2 = g2.getFontMetrics();
        g2.setColor(new Color(0x6B7280));
        g2.drawString(line2, (getWidth() - fm2.stringWidth(line2)) / 2, getHeight() / 2 + 20);
    }

    private void paintLegend(Graphics2D g2) {
        String[] vertexLabels = {"Unvisited", "Frontier", "Current", "Done", "Path"};
        Color[][] vertexColors = {
                {Theme.VERTEX_FILL, Theme.VERTEX_BORDER}, {Theme.FRONTIER_FILL, Theme.FRONTIER},
                {Theme.CURRENT, Color.WHITE}, {Theme.VISITED_FILL, Theme.VISITED}, {Theme.PATH_FILL, Theme.PATH}};
        String[] edgeLabels = {"Accepted", "Checking", "Rejected", "Path"};
        EdgeState[] edgeStates = {EdgeState.TREE, EdgeState.CONSIDERING, EdgeState.REJECTED, EdgeState.PATH};

        g2.setFont(Theme.BADGE);
        FontMetrics fm = g2.getFontMetrics();
        int x0 = 12;
        int width = 0;
        for (String label : vertexLabels) width += 18 + fm.stringWidth(label) + 12;
        int height = 50;
        int y0 = getHeight() - height - 12;
        g2.setColor(withAlpha(Theme.PANEL, 225));
        g2.fill(new RoundRectangle2D.Double(x0, y0, width + 16, height, 12, 12));
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(x0, y0, width + 16, height, 12, 12));

        int x = x0 + 12;
        int rowY = y0 + 18;
        for (int i = 0; i < vertexLabels.length; i++) {
            g2.setColor(vertexColors[i][0]);
            g2.fill(new Ellipse2D.Double(x, rowY - 9, 12, 12));
            g2.setColor(vertexColors[i][1]);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Ellipse2D.Double(x, rowY - 9, 12, 12));
            g2.setColor(Theme.MUTED);
            g2.drawString(vertexLabels[i], x + 17, rowY + 1);
            x += 18 + fm.stringWidth(vertexLabels[i]) + 12;
        }
        x = x0 + 12;
        rowY += 20;
        for (int i = 0; i < edgeLabels.length; i++) {
            g2.setColor(edgeColor(edgeStates[i], false));
            g2.setStroke(edgeStates[i] == EdgeState.REJECTED
                    ? new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 3f}, 0f)
                    : new BasicStroke(3f));
            g2.draw(new Line2D.Double(x, rowY - 3, x + 14, rowY - 3));
            g2.setColor(Theme.MUTED);
            g2.drawString(edgeLabels[i], x + 19, rowY + 1);
            x += 20 + fm.stringWidth(edgeLabels[i]) + 14;
        }
    }

    private void paintPathLabel(Graphics2D g2) {
        g2.setFont(Theme.UI_BOLD);
        FontMetrics fm = g2.getFontMetrics();
        double w = fm.stringWidth(pathPreviewLabel) + 20;
        double h = 26;
        double x = Math.min(mouse.x + 16, getWidth() - w - 8);
        double y = Math.max(8, mouse.y - h - 14);
        RoundRectangle2D box = new RoundRectangle2D.Double(x, y, w, h, 10, 10);
        g2.setColor(withAlpha(Theme.PANEL, 240));
        g2.fill(box);
        g2.setColor(Theme.PATH);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(box);
        g2.setColor(Theme.TEXT);
        g2.drawString(pathPreviewLabel, (float) (x + 10), (float) (y + h / 2 + fm.getAscent() / 2.0 - 2));
    }

    private Edge animatedEdge(Step step) {
        if (step == null || !step.hasTraversal() || animationProgress() >= 1) return null;
        return graph.findEdge(step.from(), step.to()).orElse(null);
    }

    private Set<Edge> edgesAlong(List<Vertex> path) {
        Set<Edge> edges = new HashSet<>();
        for (int i = 1; i < path.size(); i++) {
            graph.findEdge(path.get(i - 1), path.get(i)).ifPresent(edges::add);
        }
        return edges;
    }

    private static Color edgeColor(EdgeState state, boolean hovered) {
        return switch (state) {
            case CONSIDERING -> Theme.FRONTIER;
            case TREE -> Theme.VISITED;
            case REJECTED -> Theme.REJECTED;
            case PATH -> Theme.PATH;
            default -> hovered ? new Color(0x94A3B8) : Theme.EDGE;
        };
    }

    private static Stroke edgeStroke(EdgeState state) {
        return switch (state) {
            case CONSIDERING -> new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            case TREE -> new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            case REJECTED -> new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{9f, 7f}, 0f);
            case PATH -> new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            default -> new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        };
    }

    private static void dashedRing(Graphics2D g2, double x, double y, double radius, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 4f}, 0f));
        g2.draw(new Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius));
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static double easeOut(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    private void addVertexAt(int clickX, int clickY) {
        //Keep the whole circle on the canvas, then check spacing at the position actually used.
        int x = clamp(clickX, Vertex.RADIUS + 4, getWidth() - Vertex.RADIUS - 4);
        int y = clamp(clickY, Vertex.RADIUS + 4, getHeight() - Vertex.RADIUS - 4);
        Optional<Edge> edge = graph.edgeNear(x, y, EDGE_HIT_TOLERANCE);
        if (edge.isPresent()) {
            host.showStatus("That's edge " + edge.get().label() + ". Double-click an edge to change its weight.",
                    StatusType.ERROR);
            return;
        }
        for (Vertex other : graph.getVertices()) {
            if (Point.distance(x, y, other.getX(), other.getY()) < MIN_VERTEX_GAP) {
                host.showStatus("Too close to vertex " + other + ". Leave a little space between vertices.",
                        StatusType.ERROR);
                return;
            }
        }

        String suggestion = graph.suggestNextId();
        String prompt = "Enter the vertex ID (1 to 3 letters or digits):";
        while (true) {
            Object input = JOptionPane.showInputDialog(this, prompt, "New vertex",
                    JOptionPane.QUESTION_MESSAGE, null, null, suggestion);
            if (input == null) {
                host.showStatus("Cancelled: no vertex added.", StatusType.INFO);
                return;
            }
            try {
                Vertex vertex = graph.addVertex(input.toString(), x, y);
                host.showStatus("Added vertex " + vertex + ".", StatusType.SUCCESS);
                return;
            } catch (GraphException e) {
                prompt = e.getMessage() + "\nEnter the vertex ID (1 to 3 letters or digits):";
                suggestion = input.toString().trim();
            }
        }
    }

    private void connect(Vertex a, Vertex b) {
        if (a.equals(b)) {
            host.showStatus("An edge needs two different vertices. Click a second vertex.", StatusType.ERROR);
            return;
        }
        if (graph.findEdge(a, b).isPresent()) {
            host.showStatus(a + " and " + b + " are already connected. Double-click the edge to change its weight.",
                    StatusType.ERROR);
            return;
        }
        OptionalInt weight = promptWeight("Weight for edge " + a + "-" + b, "1");
        if (weight.isEmpty()) {
            host.showStatus("Cancelled: no edge added.", StatusType.INFO);
            return;
        }
        Edge edge = graph.addEdge(a, b, weight.getAsInt());
        host.showStatus("Added edge " + edge + ".", StatusType.SUCCESS);
    }

    public void editWeight(Edge edge) {
        OptionalInt weight = promptWeight("New weight for edge " + edge.label(), String.valueOf(edge.getWeight()));
        if (weight.isPresent()) {
            graph.setWeight(edge, weight.getAsInt());
            host.showStatus("Edge " + edge.label() + " now has weight " + weight.getAsInt() + ".", StatusType.SUCCESS);
        }
    }

    private OptionalInt promptWeight(String title, String initial) {
        String range = "Enter a whole number from " + Graph.MIN_WEIGHT + " to " + Graph.MAX_WEIGHT + ":";
        String prompt = range;
        String value = initial;
        while (true) {
            Object input = JOptionPane.showInputDialog(this, prompt, title,
                    JOptionPane.QUESTION_MESSAGE, null, null, value);
            if (input == null) return OptionalInt.empty();
            String text = input.toString().trim();
            if (text.matches(WEIGHT_PATTERN)) {
                int weight = Integer.parseInt(text);
                if (weight >= Graph.MIN_WEIGHT && weight <= Graph.MAX_WEIGHT) return OptionalInt.of(weight);
            }
            prompt = "\"" + text + "\" isn't a valid weight.\n" + range;
            value = text;
        }
    }

    private void removeVertex(Vertex vertex) {
        int edges = graph.edgesOf(vertex).size();
        graph.removeVertex(vertex);
        host.showStatus("Removed vertex " + vertex + (edges > 0 ? " and its " + edges + (edges == 1 ? " edge." : " edges.") : "."),
                StatusType.SUCCESS);
    }

    private void removeEdge(Edge edge) {
        graph.removeEdge(edge);
        host.showStatus("Removed edge " + edge.label() + ".", StatusType.SUCCESS);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private final class MouseHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
            if (e.isPopupTrigger()) {
                showContextMenu(e);
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            pressPoint = e.getPoint();
            dragging = false;
            Vertex vertex = graph.vertexAt(e.getX(), e.getY()).orElse(null);

            if (mode == Mode.ADD_AN_EDGE) {
                if (vertex != null && edgeStart == null) edgeStart = vertex;
                repaint();
            } else if (!mode.removes()) {
                dragVertex = vertex;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouse = e.getPoint();
            if (pressPoint != null && pressPoint.distance(mouse) > DRAG_THRESHOLD) dragging = true;

            if (mode == Mode.ADD_AN_EDGE) {
                hoverVertex = graph.vertexAt(e.getX(), e.getY()).orElse(null);
            } else if (dragVertex != null && dragging) {
                int x = clamp(e.getX(), Vertex.RADIUS + 4, getWidth() - Vertex.RADIUS - 4);
                int y = clamp(e.getY(), Vertex.RADIUS + 4, getHeight() - Vertex.RADIUS - 4);
                graph.moveVertex(dragVertex, x, y);
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            setCursor(Cursor.getDefaultCursor());
            Vertex vertex = graph.vertexAt(e.getX(), e.getY()).orElse(null);

            if (mode == Mode.ADD_AN_EDGE) {
                releaseInEdgeMode(vertex);
            } else if (dragging && dragVertex != null) {
                host.showStatus("Moved vertex " + dragVertex + ".", StatusType.INFO);
            } else if (!dragging) {
                click(e, vertex);
            }
            dragVertex = null;
            dragging = false;
            pressPoint = null;
            repaint();
        }

        private void releaseInEdgeMode(Vertex vertex) {
            if (edgeStart == null) {
                host.showStatus("Click a vertex to start an edge.", StatusType.INFO);
                return;
            }
            if (vertex == null) {
                if (dragging) {
                    edgeStart = null;
                    host.showStatus("Edge cancelled. Release on a vertex to connect.", StatusType.INFO);
                } else {
                    edgeStart = null;
                    host.showStatus("Edge cancelled.", StatusType.INFO);
                }
                return;
            }
            if (vertex.equals(edgeStart)) {
                if (dragging) edgeStart = null;
                else host.showStatus("Now click the vertex to connect " + edgeStart + " to (Esc to cancel).",
                        StatusType.INFO);
                return;
            }
            Vertex start = edgeStart;
            edgeStart = null;
            connect(start, vertex);
        }

        private void click(MouseEvent e, Vertex vertex) {
            Edge edge = vertex == null ? graph.edgeNear(e.getX(), e.getY(), EDGE_HIT_TOLERANCE).orElse(null) : null;

            if (e.getClickCount() == 2 && edge != null && !mode.removes()) {
                editWeight(edge);
                return;
            }
            switch (mode) {
                case ADD_A_VERTEX -> {
                    if (vertex == null) addVertexAt(e.getX(), e.getY());
                }
                case REMOVE_A_VERTEX -> {
                    if (vertex != null) removeVertex(vertex);
                }
                case REMOVE_AN_EDGE -> {
                    if (edge != null) removeEdge(edge);
                    else host.showStatus("No edge there. Click directly on a line.", StatusType.INFO);
                }
                case NONE -> {
                    if (vertex != null && host.isPickingVertex()) host.vertexPicked(vertex);
                    else if (vertex != null) host.showStatus("Choose an algorithm first (Algorithms menu or toolbar).",
                            StatusType.INFO);
                }
                default -> {
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouse = e.getPoint();
            Vertex vertex = graph.vertexAt(e.getX(), e.getY()).orElse(null);
            Edge edge = vertex == null ? graph.edgeNear(e.getX(), e.getY(), EDGE_HIT_TOLERANCE).orElse(null) : null;
            boolean changed = vertex != hoverVertex || !Objects.equals(edge, hoverEdge)
                    || edgeStart != null || pathPreviewLabel != null;
            hoverVertex = vertex;
            hoverEdge = edge;
            updatePathPreview(vertex);
            if (changed || pathPreviewLabel != null) repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            hoverVertex = null;
            hoverEdge = null;
            clearPathPreview();
            repaint();
        }

        private void showContextMenu(MouseEvent e) {
            JPopupMenu menu = new JPopupMenu();
            Vertex vertex = graph.vertexAt(e.getX(), e.getY()).orElse(null);
            Edge edge = vertex == null ? graph.edgeNear(e.getX(), e.getY(), EDGE_HIT_TOLERANCE).orElse(null) : null;

            if (vertex != null) {
                String algorithm = host.selectedAlgorithmLabel();
                if (algorithm != null && host.isPickingVertex()) {
                    menu.add(item("Pick " + vertex + " for " + algorithm, () -> host.vertexPicked(vertex)));
                }
                menu.add(item("Remove vertex " + vertex, () -> removeVertex(vertex)));
            } else if (edge != null) {
                menu.add(item("Edit weight of " + edge.label() + "\u2026", () -> editWeight(edge)));
                menu.add(item("Remove edge " + edge.label(), () -> removeEdge(edge)));
            } else {
                menu.add(item("Add vertex here\u2026", () -> addVertexAt(e.getX(), e.getY())));
            }
            menu.show(GraphPanel.this, e.getX(), e.getY());
        }

        private JMenuItem item(String text, Runnable action) {
            JMenuItem item = new JMenuItem(text);
            item.addActionListener(ev -> action.run());
            return item;
        }
    }

    //Renders the canvas to an image for Export as PNG.
    public BufferedImage snapshot() {
        BufferedImage image = new BufferedImage(
                Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        paint(g2);
        g2.dispose();
        return image;
    }
}
