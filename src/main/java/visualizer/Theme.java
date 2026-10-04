package visualizer;

import javax.swing.JComponent;
import javax.swing.Painter;
import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.Color;
import java.awt.Font;

/**
 * The colours and fonts used across the application.
 */
public final class Theme {

    public static final Color BACKGROUND = new Color(0x0B1120);
    public static final Color GRID_DOT = new Color(0x1A2436);

    public static final Color PANEL = new Color(0x111827);
    public static final Color PANEL_RAISED = new Color(0x1F2937);
    public static final Color BORDER = new Color(0x2D3748);
    public static final Color TEXT = new Color(0xE5E7EB);
    public static final Color MUTED = new Color(0x9CA3AF);
    public static final Color ACCENT = new Color(0x60A5FA);

    public static final Color EDGE = new Color(0x4B5563);
    public static final Color VERTEX_FILL = new Color(0x1F2937);
    public static final Color VERTEX_BORDER = new Color(0xCBD5E1);

    public static final Color FRONTIER = new Color(0xF59E0B);
    public static final Color FRONTIER_FILL = new Color(0x4A3410);
    public static final Color CURRENT = new Color(0x22D3EE);
    public static final Color VISITED = new Color(0x22C55E);
    public static final Color VISITED_FILL = new Color(0x14532D);
    public static final Color REJECTED = new Color(0xEF4444);
    public static final Color PATH = new Color(0xF472B6);
    public static final Color PATH_FILL = new Color(0x831843);

    public static final Color SUCCESS = new Color(0x4ADE80);
    public static final Color ERROR = new Color(0xF87171);

    public static final Font UI = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font UI_BOLD = UI.deriveFont(Font.BOLD);
    public static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    public static final Font SMALL_CAPS = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    public static final Font VERTEX_LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    public static final Font EDGE_LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font BADGE = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    private Theme() {
    }

    //Applies a dark Nimbus theme so menus, dialogs and scrollbars match the canvas.
    public static void install() {
        try {
            UIManager.put("control", PANEL);
            UIManager.put("info", PANEL_RAISED);
            UIManager.put("nimbusBase", new Color(0x1E293B));
            UIManager.put("nimbusBlueGrey", new Color(0x374151));
            UIManager.put("nimbusLightBackground", PANEL);
            UIManager.put("nimbusFocus", ACCENT);
            UIManager.put("nimbusSelectionBackground", new Color(0x2563EB));
            UIManager.put("nimbusSelectedText", Color.WHITE);
            UIManager.put("text", TEXT);
            UIManager.put("menu", PANEL);
            UIManager.put("menuText", TEXT);
            UIManager.put("textForeground", TEXT);
            UIManager.put("textBackground", PANEL);
            UIManager.put("nimbusDisabledText", new Color(0x6B7280));
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
            //Nimbus paints popup menus with its own light painter, so replace it with a flat dark fill.
            Painter<JComponent> popup = (g, component, width, height) -> {
                g.setColor(PANEL_RAISED);
                g.fillRect(0, 0, width, height);
                g.setColor(BORDER);
                g.drawRect(0, 0, width - 1, height - 1);
            };
            UIManager.getLookAndFeelDefaults().put("PopupMenu[Enabled].backgroundPainter", popup);
            for (String key : new String[]{"MenuItem", "RadioButtonMenuItem", "CheckBoxMenuItem", "Menu"}) {
                UIManager.getLookAndFeelDefaults().put(key + "[Enabled].textForeground", TEXT);
                UIManager.getLookAndFeelDefaults().put(key + ".textForeground", TEXT);
            }
        } catch (Exception ignored) {
            //The default look and feel still works; it just won't be dark.
        }
    }
}
