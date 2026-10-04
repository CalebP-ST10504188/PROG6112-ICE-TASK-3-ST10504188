package visualizer;

import algorithms.Step;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * The side panel: algorithm details, playback controls, an explanation of the current step
 * and a clickable trace.
 */
public class AlgorithmPanel extends JPanel {

    private static final int WIDTH = 340;
    private static final int TEXT_WIDTH = 290;
    //Swing's HTML renderer scales CSS px by about 4/3, so wrap widths are given in CSS units.
    private static final int CSS_WIDTH = TEXT_WIDTH * 3 / 4 - 6;

    private final PlaybackController playback;

    private final JLabel title = new JLabel("No algorithm selected");
    private final JLabel description = new JLabel();
    private final JLabel prompt = new JLabel();
    private final JButton restartButton = button(PlaybackIcon.Kind.RESTART, "Restart (Home)");
    private final JButton backButton = button(PlaybackIcon.Kind.STEP_BACK, "Step back (Left arrow)");
    private final JButton playButton = button(PlaybackIcon.Kind.PLAY, "Play / pause (Space)");
    private final JButton forwardButton = button(PlaybackIcon.Kind.STEP_FORWARD, "Step forward (Right arrow)");
    private final JSlider scrubber = new JSlider(0, 0, 0);
    private final JLabel stepCounter = new JLabel(" ");
    private final JSlider speed = new JSlider(1, 10, 4);
    private final JLabel message = new JLabel();
    private final JLabel structure = new JLabel(" ");
    private final DefaultListModel<String> traceModel = new DefaultListModel<>();
    private final JList<String> trace = new JList<>(traceModel);

    private boolean updating;
    private Object loadedResult;

    public AlgorithmPanel(PlaybackController playback) {
        this.playback = playback;
        setName("AlgorithmPanel");
        setLayout(new BorderLayout());
        setBackground(Theme.PANEL);
        setPreferredSize(new Dimension(WIDTH, 600));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        top.add(sectionLabel("ALGORITHM"));
        top.add(Box.createVerticalStrut(4));
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.TEXT);
        top.add(left(title));
        top.add(Box.createVerticalStrut(6));
        description.setFont(Theme.UI);
        description.setForeground(Theme.MUTED);
        description.setVerticalAlignment(JLabel.TOP);
        //Fixed height so the controls below don't jump when a longer description is shown.
        description.setPreferredSize(new Dimension(TEXT_WIDTH, 72));
        description.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        top.add(left(description));
        top.add(Box.createVerticalStrut(12));

        prompt.setFont(Theme.UI_BOLD);
        prompt.setForeground(Theme.ACCENT);
        prompt.setVerticalAlignment(JLabel.TOP);
        prompt.setPreferredSize(new Dimension(TEXT_WIDTH, 36));
        prompt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        top.add(left(prompt));
        top.add(Box.createVerticalStrut(12));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.setOpaque(false);
        controls.add(restartButton);
        controls.add(backButton);
        controls.add(playButton);
        controls.add(forwardButton);
        stepCounter.setFont(Theme.UI);
        stepCounter.setForeground(Theme.MUTED);
        controls.add(Box.createHorizontalStrut(6));
        controls.add(stepCounter);
        top.add(left(controls));
        top.add(Box.createVerticalStrut(8));

        scrubber.setOpaque(false);
        scrubber.setFocusable(false); //Keeps Space and the arrow keys for the playback shortcuts.
        scrubber.setToolTipText("Drag to jump to any step");
        top.add(left(scrubber));

        JPanel speedRow = new JPanel(new BorderLayout(8, 0));
        speedRow.setOpaque(false);
        JLabel speedLabel = new JLabel("Speed");
        speedLabel.setFont(Theme.UI);
        speedLabel.setForeground(Theme.MUTED);
        speed.setOpaque(false);
        speed.setFocusable(false);
        speed.setToolTipText("Animation speed");
        speedRow.add(speedLabel, BorderLayout.WEST);
        speedRow.add(speed, BorderLayout.CENTER);
        top.add(left(speedRow));
        top.add(Box.createVerticalStrut(14));

        top.add(sectionLabel("WHAT'S HAPPENING"));
        top.add(Box.createVerticalStrut(6));
        message.setFont(Theme.UI);
        message.setForeground(Theme.TEXT);
        message.setVerticalAlignment(JLabel.TOP);
        JPanel messageBox = card(message, 96);
        top.add(left(messageBox));
        top.add(Box.createVerticalStrut(8));
        structure.setFont(Theme.MONO);
        structure.setForeground(Theme.FRONTIER);
        top.add(left(card(structure, 58)));
        top.add(Box.createVerticalStrut(14));
        top.add(sectionLabel("TRACE  (click a step to jump to it)"));
        top.add(Box.createVerticalStrut(6));

        add(top, BorderLayout.NORTH);

        trace.setBackground(Theme.BACKGROUND);
        trace.setForeground(Theme.TEXT);
        trace.setFont(Theme.UI.deriveFont(12f));
        trace.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trace.setCellRenderer(new TraceRenderer());
        trace.setFocusable(false);
        trace.addListSelectionListener(e -> {
            if (!updating && !e.getValueIsAdjusting() && trace.getSelectedIndex() >= 0) {
                playback.jumpTo(trace.getSelectedIndex());
            }
        });
        JScrollPane scroll = new JScrollPane(trace);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        add(scroll, BorderLayout.CENTER);

        restartButton.addActionListener(e -> playback.restart());
        backButton.addActionListener(e -> playback.stepBack());
        playButton.addActionListener(e -> playback.togglePlay());
        forwardButton.addActionListener(e -> playback.stepForward());
        scrubber.addChangeListener(e -> {
            if (!updating) playback.jumpTo(scrubber.getValue());
        });
        speed.addChangeListener(e -> playback.setDelay(delayFor(speed.getValue())));
        playback.setDelay(delayFor(speed.getValue()));

        playback.addChangeListener(this::refresh);
        showAlgorithm(null);
        refresh();
    }

    //Maps the speed slider (1 = slow, 10 = fast) to milliseconds per step.
    static int delayFor(int speedValue) {
        return (int) Math.round(1500 * Math.pow(0.755, speedValue - 1));
    }

    public void showAlgorithm(Algorithm algorithm) {
        if (algorithm == null) {
            title.setText("No algorithm selected");
            description.setText(html("Pick one from the Algorithms menu or the toolbar. Each run is recorded "
                    + "step by step so you can play, pause, rewind and scrub through it."));
        } else {
            title.setText(algorithm.getLabel());
            description.setText(html(algorithm.getDescription()));
        }
    }

    public void showPrompt(String text) {
        prompt.setText(text == null || text.isEmpty() ? " " : html(text));
    }

    public void faster() {
        speed.setValue(Math.min(speed.getMaximum(), speed.getValue() + 1));
    }

    public void slower() {
        speed.setValue(Math.max(speed.getMinimum(), speed.getValue() - 1));
    }

    private void refresh() {
        updating = true;
        try {
            boolean has = playback.hasResult();
            restartButton.setEnabled(has);
            backButton.setEnabled(has && playback.getIndex() > 0);
            playButton.setEnabled(has);
            forwardButton.setEnabled(has && !playback.isAtEnd());
            scrubber.setEnabled(has);
            playButton.setIcon(new PlaybackIcon(playback.isPlaying() ? PlaybackIcon.Kind.PAUSE : PlaybackIcon.Kind.PLAY,
                    Theme.TEXT));

            if (!has) {
                loadedResult = null;
                traceModel.clear();
                scrubber.setMaximum(0);
                scrubber.setValue(0);
                stepCounter.setText(" ");
                message.setText(html("Results are explained here, one step at a time."));
                structure.setText(" ");
                return;
            }

            if (loadedResult != playback.getResult()) {
                loadedResult = playback.getResult();
                traceModel.clear();
                for (Step step : playback.getResult().steps()) traceModel.addElement(step.message());
                scrubber.setMaximum(playback.getStepCount() - 1);
            }

            int index = playback.getIndex();
            Step step = playback.currentStep();
            scrubber.setValue(index);
            stepCounter.setText("Step " + (index + 1) + " of " + playback.getStepCount());
            message.setText(html(step.message()));
            structure.setText(step.structure() == null || step.structure().isBlank() ? " " : html(step.structure()));
            trace.setSelectedIndex(index);
            trace.ensureIndexIsVisible(index);
        } finally {
            updating = false;
        }
    }

    private static String html(String text) {
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html><body style='width:" + CSS_WIDTH + "px'>" + escaped + "</body></html>";
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL_CAPS);
        label.setForeground(new Color(0x6B7280));
        return left(label);
    }

    private static <T extends JComponent> T left(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    private static JPanel card(JComponent content, int height) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.PANEL_RAISED);
        Border line = BorderFactory.createLineBorder(Theme.BORDER);
        card.setBorder(BorderFactory.createCompoundBorder(line, BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        card.add(content, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(TEXT_WIDTH + 20, height));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return card;
    }

    private static JButton button(PlaybackIcon.Kind kind, String tooltip) {
        JButton button = new JButton(new PlaybackIcon(kind, Theme.TEXT));
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(40, 32));
        return button;
    }

    //Numbers each trace line and wraps long messages.
    private static final class TraceRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
            String text = String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            label.setText("<html><body style='width:185px'><b>" + (index + 1) + ".</b>&nbsp;" + text + "</body></html>");
            label.setFont(list.getFont().deriveFont(Font.PLAIN));
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            label.setBackground(selected ? new Color(0x1E3A5F) : Theme.BACKGROUND);
            label.setForeground(selected ? Color.WHITE : Theme.MUTED);
            return label;
        }
    }
}
