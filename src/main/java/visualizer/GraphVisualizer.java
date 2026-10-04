package visualizer;

import javax.swing.SwingUtilities;
import java.nio.file.Path;

/**
 * Entry point. A .graph file can be passed as the first argument to open it on start-up.
 */
public class GraphVisualizer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainFrame(args.length > 0 ? Path.of(args[0]) : null);
        });
    }
}
