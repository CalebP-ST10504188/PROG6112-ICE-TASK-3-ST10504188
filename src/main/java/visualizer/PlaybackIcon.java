package visualizer;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

/**
 * The media control icons, drawn with shapes so they look the same on every system.
 */
public final class PlaybackIcon implements Icon {

    public enum Kind { PLAY, PAUSE, STEP_FORWARD, STEP_BACK, RESTART }

    private static final int SIZE = 16;
    private final Kind kind;
    private final Color color;

    public PlaybackIcon(Kind kind, Color color) {
        this.kind = kind;
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        g2.setColor(c.isEnabled() ? color : new Color(0x4B5563));
        switch (kind) {
            case PLAY -> g2.fill(triangle(4, 2, 14, 8, 4, 14));
            case PAUSE -> {
                g2.fillRoundRect(3, 2, 4, 12, 2, 2);
                g2.fillRoundRect(9, 2, 4, 12, 2, 2);
            }
            case STEP_FORWARD -> {
                g2.fill(triangle(2, 2, 11, 8, 2, 14));
                g2.fillRoundRect(11, 2, 3, 12, 2, 2);
            }
            case STEP_BACK -> {
                g2.fill(triangle(14, 2, 5, 8, 14, 14));
                g2.fillRoundRect(2, 2, 3, 12, 2, 2);
            }
            case RESTART -> {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Double(2.5, 2.5, 11, 11, 100, 290, Arc2D.OPEN));
                g2.fill(triangle(3, 0, 9, 3, 4, 7));
            }
        }
        g2.dispose();
    }

    private static Path2D triangle(double x1, double y1, double x2, double y2, double x3, double y3) {
        Path2D path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.closePath();
        return path;
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
