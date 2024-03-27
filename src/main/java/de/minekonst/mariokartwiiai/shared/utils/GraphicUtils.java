package de.minekonst.mariokartwiiai.shared.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public class GraphicUtils {

    private GraphicUtils() {
    }

    /**
     * Draw a string centered to a canvas (in black)
     *
     * @param g    The Graphics to draw on
     * @param x    X Position of the center
     * @param y    Y Position of the center
     * @param text The text to draw
     * @param size The size of the text
     */
    public static void centerString(Graphics2D g, int x, int y, String text, int size) {
        centerString(g, x, y, text, size, Color.BLACK, null);
    }

    /**
     * Draw a string centered to a canvas
     *
     * @param g     The Graphics to draw on
     * @param x     X Position of the center
     * @param y     Y Position of the center
     * @param text  The text to draw
     * @param size  The size of the text
     * @param color The color of text
     */
    public static void centerString(Graphics2D g, int x, int y, String text, int size, Color color) {
        centerString(g, x, y, text, size, color, null);
    }

    /**
     * Draw a string centered to a canvas (with backgound)
     *
     * @param g          The Graphics to draw on
     * @param x          X Position of the center
     * @param y          Y Position of the center
     * @param text       The text to draw
     * @param size       The size of the text
     * @param color      The color of text
     * @param background Color of the background recangle
     */
    public static void centerString(Graphics2D g, int x, int y, String text, int size, Color color, Color background) {
        centerString(g, x, y, text, size, color, background, 0);
    }

    /**
     * Draw a string centered to a canvas (with backgound)
     *
     * @param g          The Graphics to draw on
     * @param x          X Position of the center
     * @param y          Y Position of the center
     * @param text       The text to draw
     * @param size       The size of the text
     * @param color      The color of text
     * @param background Color of the background recangle
     * @param angle      The angle to rotate this text (in radians)
     */
    public static void centerString(Graphics2D g, int x, int y, String text, int size, Color color, Color background, double angle) {
        AffineTransform old = g.getTransform();
        if (angle != 0) {
            g.translate(x, y);
            g.rotate(angle);
            g.translate(-x, -y);
        }

        Font font = new Font("Arial", 0, size);
        FontMetrics metrics = g.getFontMetrics(font);
        int tx = x - metrics.stringWidth(text) / 2;
        int ty = y + metrics.getHeight() / 2;
        if (background != null) {
            g.setColor(background);
            g.fillRect(tx - 5, (int) (ty - metrics.getHeight() * 0.8), metrics.stringWidth(text) + 10, metrics.getHeight());
        }
        g.setFont(font);
        g.setColor(color);

        g.drawString(text, tx, ty);
        g.setTransform(old);
    }

}
