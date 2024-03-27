package de.minekonst.mariokartwiiai.client.gui;

import de.minekonst.mariokartwiiai.shared.utils.profiler.Profiler;
import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.main.Main;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;

public class InputViewer extends Canvas {

    private Driver driver;

    public InputViewer() {
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

        if (driver == null || driver.getConnector() == null || driver.getConnector().getState() == null) {
            return;
        }

        double[] fov = driver.getFov();
        if (fov == null) {
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

        Profiler.enter("Render Fov");

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        g.clearRect(0, 0, getWidth(), getHeight());

        driver.getIntputMethod().draw(g, getWidth(), getHeight(), fov, driver);

        bs.show();
        g.dispose();
        Toolkit.getDefaultToolkit().sync();

        Profiler.exit("Render Fov");
        Profiler.getRootNode("Render Fov").nextIteration();
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

}
