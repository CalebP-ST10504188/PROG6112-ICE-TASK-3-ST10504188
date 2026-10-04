package visualizer;

import algorithms.AlgorithmResult;
import algorithms.GraphAlgorithm;
import model.Graph;
import model.GraphException;
import model.Vertex;
import storage.GraphFileFormat;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The main window. It owns the graph and connects the canvas, side panel, menus and playback.
 */
public class MainFrame extends JFrame implements GraphPanel.Host {

    private static final String[][] EXAMPLES = {
            {"Tutorial (6 vertices)", "tutorial.graph"},
            {"City grid for pathfinding (20 vertices)", "city-grid.graph"},
            {"Textbook MST (CLRS, total weight 37)", "textbook-mst.graph"},
            {"Two islands (disconnected graph)", "two-islands.graph"},
    };

    private final Graph graph = new Graph();
    private final PlaybackController playback = new PlaybackController();
    private final GraphPanel graphPanel;
    private final AlgorithmPanel algorithmPanel;

    private final JLabel modeLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel statsLabel = new JLabel();
    private final JTextField algorithmDisplay = new JTextField();
    private final Map<Mode, JToggleButton> modeButtons = new EnumMap<>(Mode.class);
    private final Map<Mode, JRadioButtonMenuItem> modeItems = new EnumMap<>(Mode.class);
    private final Map<Algorithm, JRadioButtonMenuItem> algorithmItems = new EnumMap<>(Algorithm.class);
    private final ButtonGroup algorithmGroup = new ButtonGroup();
    private final JComboBox<Object> algorithmCombo = new JComboBox<>();

    private Mode mode = Mode.ADD_A_VERTEX;
    private Algorithm algorithm;
    private Vertex pendingSource;
    private boolean syncing;
    private File currentFile;

    public MainFrame() {
        this(null);
    }

    public MainFrame(Path fileToOpen) {
        super("Graph-Algorithms Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Uses 1180x760, but never more than the usable screen area, such as a small laptop with a taskbar.
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(1000, usable.width), Math.min(640, usable.height)));
        setSize(Math.min(1180, usable.width), Math.min(760, usable.height));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        graphPanel = new GraphPanel(graph, playback, this);
        algorithmPanel = new AlgorithmPanel(playback);

        setJMenuBar(buildMenuBar());
        add(buildToolBar(), BorderLayout.NORTH);
        add(graphPanel, BorderLayout.CENTER);
        add(algorithmPanel, BorderLayout.EAST);
        add(buildBottomBar(), BorderLayout.SOUTH);
        installKeyBindings();

        graph.addChangeListener(this::onGraphChanged);
        playback.addChangeListener(this::onPlaybackChanged);

        setMode(Mode.ADD_A_VERTEX);
        updateStats();
        showStatus("Welcome! Click the canvas to add vertices, or open File \u2192 Examples.", StatusType.INFO);

        setVisible(true);
        //Open after the window is showing, so any error dialog appears on top of it.
        if (fileToOpen != null) SwingUtilities.invokeLater(() -> openFile(fileToOpen.toFile()));
    }

    @Override
    public void showStatus(String message, StatusType type) {
        statusLabel.setText(message);
        statusLabel.setForeground(switch (type) {
            case SUCCESS -> Theme.SUCCESS;
            case ERROR -> Theme.ERROR;
            default -> Theme.MUTED;
        });
    }

    @Override
    public boolean isPickingVertex() {
        return algorithm != null && mode == Mode.NONE;
    }

    @Override
    public String selectedAlgorithmLabel() {
        return algorithm == null ? null : algorithm.getLabel();
    }

    @Override
    public void vertexPicked(Vertex vertex) {
        if (algorithm == null) return;
        if (mode != Mode.NONE) setMode(Mode.NONE);
        GraphAlgorithm instance = algorithm.getAlgorithmInstance();

        if (!instance.needsStartVertex()) {
            runAlgorithm(null, null);
        } else if (!instance.needsTargetVertex()) {
            runAlgorithm(vertex, null);
        } else if (pendingSource == null || playback.hasResult()) {
            playback.clear();
            pendingSource = vertex;
            graphPanel.setMarkers(vertex, null);
            algorithmDisplay.setText("Source " + vertex + " selected. Now click the target vertex.");
            algorithmPanel.showPrompt("Source: " + vertex + ". Now click the target vertex.");
            showStatus("Source set to " + vertex + ". Click the target vertex (Esc to cancel).", StatusType.INFO);
        } else if (vertex.equals(pendingSource)) {
            showStatus("The target must be different from the source.", StatusType.ERROR);
        } else {
            Vertex source = pendingSource;
            pendingSource = null;
            runAlgorithm(source, vertex);
        }
    }

    private void setMode(Mode newMode) {
        mode = newMode;
        graphPanel.setMode(newMode);
        pendingSource = null;
        if (!playback.hasResult()) graphPanel.setMarkers(null, null);

        syncing = true;
        modeButtons.get(newMode).setSelected(true);
        modeItems.get(newMode).setSelected(true);
        syncing = false;

        modeLabel.setText("Current Mode -> " + newMode.getDescription());
        if (newMode == Mode.NONE && algorithm != null) {
            showPrompt();
        } else {
            algorithmPanel.showPrompt(algorithm == null ? "" : "Switch to Mode \u2192 None (or pick the algorithm again) to run it.");
            showStatus(newMode.getHint(), StatusType.INFO);
        }
    }

    private void selectAlgorithm(Algorithm selected) {
        algorithm = selected;
        syncing = true;
        algorithmItems.get(selected).setSelected(true);
        algorithmCombo.setSelectedItem(selected);
        syncing = false;

        playback.clear();
        graphPanel.setMarkers(null, null);
        algorithmPanel.showAlgorithm(selected);
        setMode(Mode.NONE); //As in the original, choosing an algorithm switches to None.

        if (graph.isEmpty()) {
            showStatus("The graph is empty. Add vertices first, or open File \u2192 Examples.", StatusType.ERROR);
        } else if (!selected.getAlgorithmInstance().needsStartVertex()) {
            runAlgorithm(null, null);
        }
    }

    private void showPrompt() {
        GraphAlgorithm instance = algorithm.getAlgorithmInstance();
        String text;
        if (!instance.needsStartVertex()) text = "Click any vertex (or choose it again) to re-run.";
        else if (instance.needsTargetVertex()) text = "Click the source vertex, then the target vertex.";
        else text = "Please choose a starting vertex.";
        algorithmPanel.showPrompt(text);
        algorithmDisplay.setText(text);
        showStatus(text, StatusType.INFO);
    }

    private void runAlgorithm(Vertex start, Vertex target) {
        GraphAlgorithm instance = algorithm.getAlgorithmInstance();
        if (graph.isEmpty()) {
            showStatus("The graph is empty. Add vertices first.", StatusType.ERROR);
            return;
        }
        Optional<String> problem = instance.validate(graph);
        if (problem.isPresent()) {
            graphPanel.setMarkers(null, null);
            showStatus(problem.get().replace("\n", " "), StatusType.ERROR);
            JOptionPane.showMessageDialog(this, problem.get(), algorithm.getLabel() + " can't run",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        AlgorithmResult result = target == null ? instance.run(graph, start) : instance.run(graph, start, target);
        graphPanel.setMarkers(start, target);
        algorithmDisplay.setText("Running\u2026");
        algorithmPanel.showPrompt("Playing. Use the controls below, or click another vertex to run again.");
        showStatus(algorithm.getLabel() + (start == null ? "" : " from " + start)
                + (target == null ? "" : " to " + target) + ": " + result.steps().size() + " steps recorded.",
                StatusType.SUCCESS);
        playback.load(result);
    }

    private void onPlaybackChanged() {
        if (!playback.hasResult()) return;
        if (playback.isAtEnd()) {
            AlgorithmResult result = playback.getResult();
            algorithmDisplay.setText(result.output());
            algorithmDisplay.setCaretPosition(0);
            String prompt;
            if (result.supportsPathQueries()) prompt = "Done. Hover any vertex to trace its path, or click one to run again.";
            else if (algorithm.getAlgorithmInstance().needsTargetVertex()) prompt = "Done. Click a new source vertex to search again.";
            else prompt = "Done. Click a vertex to run again.";
            algorithmPanel.showPrompt(prompt);
        } else {
            algorithmDisplay.setText("Running\u2026 step " + (playback.getIndex() + 1) + " of " + playback.getStepCount());
        }
    }

    private void onGraphChanged() {
        boolean hadResult = playback.hasResult();
        playback.clear();
        pendingSource = null;
        graphPanel.setMarkers(null, null);
        updateStats();
        if (algorithm != null && mode == Mode.NONE) showPrompt();
        else algorithmDisplay.setText(hadResult ? "Graph changed, so the previous result was cleared." : "");
    }

    private void updateStats() {
        int v = graph.vertexCount();
        int e = graph.edgeCount();
        statsLabel.setText(v + (v == 1 ? " vertex" : " vertices") + "  \u00B7  " + e + (e == 1 ? " edge" : " edges"));
    }

    private boolean confirmDiscard() {
        if (graph.isEmpty()) return true;
        return JOptionPane.showConfirmDialog(this, "Discard the current graph?", "Replace graph",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void newGraph() {
        if (!confirmDiscard()) return;
        graph.clear();
        currentFile = null;
        setMode(Mode.ADD_A_VERTEX);
        showStatus("Started a new graph.", StatusType.SUCCESS);
    }

    private void openWithChooser() {
        if (!confirmDiscard()) return;
        JFileChooser chooser = fileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openFile(chooser.getSelectedFile());
        }
    }

    private void openFile(File file) {
        if (!file.isFile()) {
            showError("File not found:\n" + file.getAbsolutePath());
            return;
        }
        try {
            GraphFileFormat.load(file.toPath(), graph);
            currentFile = file;
            afterLoad("Opened " + file.getName() + ".");
        } catch (GraphException e) {
            showError("That file isn't a valid graph.\n\n" + e.getMessage());
        } catch (IOException e) {
            showError("Couldn't read " + file.getName() + ":\n" + e.getMessage());
        }
    }

    private void openExample(String label, String resource) {
        if (!confirmDiscard()) return;
        try {
            GraphFileFormat.loadExample(resource, graph);
            currentFile = null;
            afterLoad("Loaded example: " + label + ".");
        } catch (IOException | GraphException e) {
            showError("Couldn't load the example: " + e.getMessage());
        }
    }

    private void afterLoad(String message) {
        setMode(Mode.NONE);
        //After layout, pull the graph into view if it was drawn on a bigger screen.
        SwingUtilities.invokeLater(() -> graphPanel.fitGraph(true));
        if (algorithm == null) {
            algorithmPanel.showPrompt("Now pick an algorithm from the Algorithms menu or the toolbar.");
        }
        showStatus(message + (algorithm == null ? " Pick an algorithm to explore it." : ""), StatusType.SUCCESS);
    }

    private void saveWithChooser() {
        JFileChooser chooser = fileChooser();
        if (currentFile != null) chooser.setSelectedFile(currentFile);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().contains(".")) file = new File(file.getPath() + "." + GraphFileFormat.EXTENSION);
        if (!confirmOverwrite(file)) return;
        try {
            GraphFileFormat.save(graph, file.toPath());
            currentFile = file;
            showStatus("Saved " + file.getName() + ".", StatusType.SUCCESS);
        } catch (IOException e) {
            showError("Couldn't save the file:\n" + e.getMessage());
        }
    }

    private void exportPng() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PNG image (*.png)", "png"));
        chooser.setSelectedFile(new File("graph.png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) file = new File(file.getPath() + ".png");
        if (!confirmOverwrite(file)) return;
        try {
            ImageIO.write(graphPanel.snapshot(), "png", file);
            showStatus("Exported " + file.getName() + ".", StatusType.SUCCESS);
        } catch (IOException e) {
            showError("Couldn't export the image:\n" + e.getMessage());
        }
    }

    private boolean confirmOverwrite(File file) {
        return !file.exists() || JOptionPane.showConfirmDialog(this, file.getName() + " already exists. Replace it?",
                "Replace file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private JFileChooser fileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Graph files (*.graph)", GraphFileFormat.EXTENSION));
        return chooser;
    }

    private void showError(String message) {
        showStatus(message.replace("\n", " "), StatusType.ERROR);
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(menuItem("New", KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK, this::newGraph));
        fileMenu.add(menuItem("Open\u2026", KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK, this::openWithChooser));
        fileMenu.add(menuItem("Save As\u2026", KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, this::saveWithChooser));
        JMenu examples = new JMenu("Examples");
        for (String[] example : EXAMPLES) {
            examples.add(menuItem(example[0], 0, 0, () -> openExample(example[0], example[1])));
        }
        fileMenu.add(examples);
        fileMenu.add(menuItem("Export as PNG\u2026", KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK, this::exportPng));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Exit", KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK,
                () -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))));
        menuBar.add(fileMenu);

        JMenu modeMenu = new JMenu("Mode");
        ButtonGroup modeGroup = new ButtonGroup();
        Mode[] order = {Mode.ADD_A_VERTEX, Mode.ADD_AN_EDGE, Mode.REMOVE_A_VERTEX, Mode.REMOVE_AN_EDGE, Mode.NONE};
        for (int i = 0; i < order.length; i++) {
            Mode m = order[i];
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(m.getDescription());
            item.setName(m.getDescription());
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, InputEvent.CTRL_DOWN_MASK));
            item.addActionListener(e -> {
                if (!syncing) setMode(m);
            });
            modeGroup.add(item);
            modeItems.put(m, item);
            modeMenu.add(item);
        }
        menuBar.add(modeMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.add(menuItem("Fit Graph to Window", KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, () -> {
            graphPanel.fitGraph(false);
            showStatus("Graph fitted to the window.", StatusType.INFO);
        }));
        menuBar.add(viewMenu);

        JMenu algorithmsMenu = new JMenu("Algorithms");
        for (Algorithm a : Algorithm.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(a.getLabel());
            item.setName(a.getLabel());
            item.addActionListener(e -> {
                if (!syncing) selectAlgorithm(a);
            });
            algorithmGroup.add(item);
            algorithmItems.put(a, item);
            algorithmsMenu.add(item);
            if (a == Algorithm.SHORTEST_PATH) algorithmsMenu.addSeparator();
        }
        menuBar.add(algorithmsMenu);

        JMenu playbackMenu = new JMenu("Playback");
        playbackMenu.add(menuItem("Play / Pause", KeyEvent.VK_SPACE, 0, playback::togglePlay));
        playbackMenu.add(menuItem("Step Forward", KeyEvent.VK_RIGHT, 0, playback::stepForward));
        playbackMenu.add(menuItem("Step Back", KeyEvent.VK_LEFT, 0, playback::stepBack));
        playbackMenu.add(menuItem("Restart", KeyEvent.VK_HOME, 0, playback::restart));
        playbackMenu.add(menuItem("Skip to Result", KeyEvent.VK_END, 0, playback::jumpToEnd));
        playbackMenu.addSeparator();
        playbackMenu.add(menuItem("Faster", KeyEvent.VK_CLOSE_BRACKET, 0, algorithmPanel::faster));
        playbackMenu.add(menuItem("Slower", KeyEvent.VK_OPEN_BRACKET, 0, algorithmPanel::slower));
        menuBar.add(playbackMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(menuItem("How to Use", KeyEvent.VK_F1, 0, this::showHelp));
        helpMenu.add(menuItem("About", 0, 0, () -> JOptionPane.showMessageDialog(this,
                "Graph Algorithms Visualizer\nJava 21 + Swing\n\nBFS, DFS, Dijkstra's, shortest path, Prim's and Kruskal's,\n"
                        + "animated step by step.", "About", JOptionPane.INFORMATION_MESSAGE)));
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JToolBar buildToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        ButtonGroup group = new ButtonGroup();
        String[][] labels = {{"ADD_A_VERTEX", "Add Vertex"}, {"ADD_AN_EDGE", "Add Edge"},
                {"REMOVE_A_VERTEX", "Remove Vertex"}, {"REMOVE_AN_EDGE", "Remove Edge"}, {"NONE", "Select / Run"}};
        for (String[] entry : labels) {
            Mode m = Mode.valueOf(entry[0]);
            JToggleButton button = new JToggleButton(entry[1]);
            button.setFocusable(false);
            button.setToolTipText(m.getHint());
            button.addActionListener(e -> {
                if (!syncing) setMode(m);
            });
            group.add(button);
            modeButtons.put(m, button);
            toolBar.add(button);
            toolBar.add(Box.createHorizontalStrut(4));
        }

        toolBar.addSeparator(new Dimension(16, 24));
        JLabel algorithmLabel = new JLabel("Algorithm: ");
        algorithmLabel.setForeground(Theme.MUTED);
        toolBar.add(algorithmLabel);
        algorithmCombo.addItem("Choose\u2026");
        for (Algorithm a : Algorithm.values()) algorithmCombo.addItem(a);
        algorithmCombo.setMaximumSize(new Dimension(230, 30));
        algorithmCombo.setFocusable(false);
        algorithmCombo.addActionListener(e -> {
            if (!syncing && algorithmCombo.getSelectedItem() instanceof Algorithm a) selectAlgorithm(a);
        });
        toolBar.add(algorithmCombo);

        toolBar.add(Box.createHorizontalGlue());
        modeLabel.setName("Mode");
        modeLabel.setForeground(Theme.TEXT);
        modeLabel.setFont(Theme.UI_BOLD);
        toolBar.add(modeLabel);
        return toolBar;
    }

    private JComponent buildBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Theme.PANEL);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));

        JPanel outputRow = new JPanel(new BorderLayout(10, 0));
        outputRow.setOpaque(false);
        outputRow.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        JLabel outputLabel = new JLabel("OUTPUT");
        outputLabel.setFont(Theme.SMALL_CAPS);
        outputLabel.setForeground(new java.awt.Color(0x6B7280));
        algorithmDisplay.setName("Display");
        algorithmDisplay.setEditable(false);
        algorithmDisplay.setFont(Theme.MONO.deriveFont(Font.BOLD, 14f));
        algorithmDisplay.setForeground(Theme.TEXT);
        algorithmDisplay.setBackground(Theme.BACKGROUND);
        algorithmDisplay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        algorithmDisplay.setToolTipText("The final result. Select the text to copy it.");
        outputRow.add(outputLabel, BorderLayout.WEST);
        outputRow.add(algorithmDisplay, BorderLayout.CENTER);

        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        statusRow.setBorder(BorderFactory.createEmptyBorder(2, 12, 8, 12));
        statusLabel.setFont(Theme.UI);
        statsLabel.setFont(Theme.UI);
        statsLabel.setForeground(Theme.MUTED);
        statusRow.add(statusLabel, BorderLayout.CENTER);
        statusRow.add(statsLabel, BorderLayout.EAST);

        bottom.add(outputRow, BorderLayout.NORTH);
        bottom.add(statusRow, BorderLayout.SOUTH);
        return bottom;
    }

    private void installKeyBindings() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        root.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                graphPanel.resetInteraction();
                if (pendingSource != null) {
                    pendingSource = null;
                    graphPanel.setMarkers(null, null);
                    if (algorithm != null) showPrompt();
                }
                showStatus("Cancelled.", StatusType.INFO);
            }
        });
    }

    private JMenuItem menuItem(String text, int key, int modifiers, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.setName(text);
        if (key != 0) item.setAccelerator(KeyStroke.getKeyStroke(key, modifiers));
        item.addActionListener(e -> action.run());
        return item;
    }

    private void showHelp() {
        String help = """
                <html><body style='width:420px'>
                <h3>Build a graph</h3>
                <b>Add Vertex</b>: click empty space. The next free ID is suggested.<br>
                <b>Add Edge</b>: click two vertices, or drag from one to the other, then enter a weight.<br>
                <b>Move</b>: drag any vertex (in every mode except the Remove modes).<br>
                <b>Edit a weight</b>: double-click an edge, or right-click it.<br>
                <b>Remove</b>: use the Remove modes, or right-click a vertex or edge.<br>
                <b>Fit</b>: View &rarr; Fit Graph to Window (Ctrl+F) if part of the graph is off-screen.<br>
                <h3>Run an algorithm</h3>
                Pick one from <b>Algorithms</b> or the toolbar, then click a start vertex.
                <b>Shortest Path</b> needs two clicks: source, then target. <b>Kruskal's</b> runs straight away.
                <h3>Playback</h3>
                <b>Space</b> play/pause &nbsp; <b>&larr; / &rarr;</b> step &nbsp; <b>Home</b> restart &nbsp;
                <b>End</b> skip to result &nbsp; <b>[ / ]</b> slower/faster &nbsp; <b>Esc</b> cancel.<br>
                Click any line in the trace to jump to it. After BFS or Dijkstra's finishes, hover a vertex
                to trace the path to it.
                <h3>Files</h3>
                Graphs save as readable <code>.graph</code> text files. Try <b>File &rarr; Examples</b>.
                </body></html>""";
        JOptionPane.showMessageDialog(this, help, "How to use", JOptionPane.INFORMATION_MESSAGE);
    }
}
