package de.minekonst.mariokartwiiai.client.gui;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import de.minekonst.mariokartwiiai.shared.utils.profiler.Profiler;
import de.minekonst.mariokartwiiai.main.Main;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;

public class StickViewer extends Canvas {

    private Vector2D position;

    public StickViewer() {
        super();

        super.setSize(200, 200);
        super.setVisible(true);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 200);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics gp) {
        // In Editor preview
        try {
            Main.getLogger();
        }
        catch (Throwable t) {
            gp.setColor(Color.red);
            gp.drawLine(0, 0, getWidth(), getHeight());
            return;
        }

        if (position == null) {
            return;
        }

        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(4);
            repaint();
            return;
        }

        Graphics2D g;
        try {
            g = (Graphics2D) bs.getDrawGraphics().create();
        }
        catch (Exception ex) {
            return;
        }

        Profiler.enter("Render Stick");

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        g.clearRect(0, 0, super.getWidth(), super.getHeight());

        final int BORDER = 20; // Per SIDE
        final int mx = getWidth() / 2;
        final int my = getHeight() / 2;
        final int WIDTH = super.getWidth() - BORDER * 2;
        final int HEIGHT = super.getHeight() - BORDER * 2;

        int middleSize = 10;
        g.setColor(Color.black);
        g.fillRect(mx - middleSize / 2, my - middleSize / 2, middleSize, middleSize);
        g.setStroke(new BasicStroke(2));
        g.drawOval(BORDER, BORDER, WIDTH - 2, HEIGHT - 2);

        // Faulty subtract method
        Vector2D stick = position.add(new Vector2D(-50, -50)).scalarMultiply(1.0 / 100); // Every component is in [-0.5, 0.5]
        final int sx = mx + (int) (stick.getX() * WIDTH + 0.5);
        final int sy = my - (int) (stick.getY() * HEIGHT + 0.5) ;

        // Pointer
        final int pointSize = 18;
        g.setColor(Color.red);
        g.fillOval(sx - pointSize / 2, sy - pointSize / 2, pointSize, pointSize);
        g.drawLine(mx, my, sx, sy);

        bs.show();
        g.dispose();
        Toolkit.getDefaultToolkit().sync();

        Profiler.exit("Render Stick");
        Profiler.getRootNode("Render Stick").nextIteration();
    }

    public Vector2D getPosition() {
        return position;
    }

    public void setPosition(Vector2D position) {
        this.position = position;
    }

}
