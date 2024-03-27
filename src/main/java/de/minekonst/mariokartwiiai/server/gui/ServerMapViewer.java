package de.minekonst.mariokartwiiai.server.gui;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.server.AIServer;
import de.minekonst.mariokartwiiai.server.RemoteDriver;
import de.minekonst.mariokartwiiai.server.ai.types.Statistics;
import de.minekonst.mariokartwiiai.shared.tasks.TaskResponse;
import de.minekonst.mariokartwiiai.shared.utils.GraphicUtils;
import de.minekonst.mariokartwiiai.shared.utils.MathUtils;
import de.minekonst.mariokartwiiai.tracks.DynamicMapData;
import de.minekonst.mariokartwiiai.tracks.Hardness;
import de.minekonst.mariokartwiiai.tracks.Track;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

public class ServerMapViewer extends Canvas {

    private static final Color[] PLAYER_COLORS_BG = {Color.GREEN, Color.MAGENTA, Color.CYAN, Color.RED, Color.YELLOW, Color.BLUE.brighter(), Color.ORANGE};
    private static final Color[] PLAYER_COLORS_FG = {Color.WHITE, Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK, Color.BLACK, Color.BLACK};
    private Vector3D center;
    private int hudStartX;
    private int hudStartY;
    private int hudEntryHeight;
    private AIServer server;
    private Vector3D from;
    private double units;

    record PlayerData(RemoteDriver driver, Vector2D onScreen) {

    }

    public ServerMapViewer() {
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
        //<editor-fold defaultstate="collapsed" desc="Init">
        // In Editor preview
        try {
            Main.getLogger();
        }
        catch (Throwable t) {
            gp.setColor(Color.red);
            gp.drawLine(0, 0, getWidth(), getHeight());
            return;
        }

        if (server == null) {
            return;
        }
        if (server.getAI() == null) {
            GraphicUtils.centerString((Graphics2D) gp, getWidth() / 2, getHeight() / 2, "No AI loaded", 80, Color.red, null, 25);
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

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        //</editor-fold>

        drawRaw(g);

        bs.show();
        g.dispose();
        Toolkit.getDefaultToolkit().sync();
    }

    public void drawRaw(Graphics2D g) {
        Track track = server.getAI().getProperties().getTrack().getValue();
        g.setColor(track.getColor(Hardness.OUT_OF_BOUNDS));
        g.fillRect(0, 0, getWidth(), getHeight());
        final DynamicMapData data = server.getAI().getProperties().getTrack().getValue().getMapData();

        center = getCenter(server.getAI().getProperties().getTrack().getValue());
        units = getScale(server.getAI().getProperties().getTrack().getValue());
        from = center.subtract(new Vector3D(units * getWidth() / 2, 0, units * getHeight() / 2));

        for (int x = 0; x <= getWidth(); x++) {
            for (int z = 0; z <= getHeight(); z++) {
                //<editor-fold defaultstate="collapsed" desc="Draw">
                double h = data.getHardness(new Vector3D(x * units + from.getX(), 0, z * units + from.getZ()));
                Hardness hardness = Hardness.fromValue(h);
                if (hardness != Hardness.OUT_OF_BOUNDS) {
                    g.setColor(track.getColor(hardness));
                    g.fillRect(x, z, 1, 1);
                }
                //</editor-fold>
            }
        }

        //<editor-fold defaultstate="collapsed" desc="Player / Anwser points">
        List<PlayerData> players = new ArrayList<>();
        for (RemoteDriver d : server.getRemoteDrivers()) {
            if (d.getStatus() != null && d.getStatus().getTrackID() == server.getAI().getProperties().getTrack().getValue().ordinal()) {
                players.add(new PlayerData(d, onScreen(d.getStatus().getPosition())));
            }
        }

        for (TaskResponse r : server.getAI().getScheduler().getResponses()) {
            Vector2D s = onScreen(r.getLastPosition());
            g.setColor(Color.DARK_GRAY);
            playerIcon(g, (int) s.getX(), (int) s.getY(), null, Color.DARK_GRAY);
        }

        int x = 0;
        for (PlayerData p : players) {
            if (p.onScreen().getX() != -1) {
                drawPlayer(g, p, PLAYER_COLORS_FG[x % PLAYER_COLORS_BG.length], PLAYER_COLORS_BG[x % PLAYER_COLORS_BG.length]);
            }
            x++;
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="HUD">
        hudStartX = getWidth() - 550;
        hudStartY = 100;
        hudEntryHeight = 32;
        List<Statistics> archive = server.getAI().getProperties().getStatistics();
        final int amountOfLastStats = Math.min(10, archive.size() - 1);

        int entryPosition = 0;
        for (PlayerData p : players) {
            legend(g, entryPosition, PLAYER_COLORS_BG[entryPosition % PLAYER_COLORS_BG.length],
                    String.format("Driver #%d  %-8s%s",
                            p.driver().getServerClient().getID(),
                            p.driver().getMessage().equals("/") ? "" : "(" + p.driver().getMessage() + ")",
                            p.driver().getStatus().getDrivingSince() != null ? " | " + p.driver().getStatus().getDrivingSince() : ""));
            entryPosition++;
        }
        // End Positions
        entryPosition++;
        legend(g, entryPosition++, Color.DARK_GRAY, "End Positions");

        List<Statistics> stats = new ArrayList<>();
        double max = 0;
        for (int index = archive.size() - 1 - amountOfLastStats; index < archive.size(); index++) {
            Statistics s = archive.get(index);
            stats.add(s);
            if (s.getScore() > max) {
                max = s.getScore();
            }
        }

        int i = 0;
        for (Statistics s : stats) {
            // Archive
            Color c;
            Vector2D os = onScreen(s.getEndPosition());
            if (s.getScore() == max) {
                c = new Color(128, 0, 128);
            }
            else if (i != stats.size() - 1) {
                int alpha = Math.min(255, (int) (100 + 150.0 * i / (amountOfLastStats - 1)));
                c = new Color(226, 176, 7, alpha);
            }
            else {
                c = Color.GREEN.darker();
            }
            g.setColor(c);

            String str;
            if (s.getEndTime() == null) {
                str = String.format("Era %d Spe. %d Gen. %2d (%.2f)",
                        s.getEra(), s.getSpecies(), s.getGeneration(), s.getScore());
            }
            else {
                str = String.format("Era %d Spe. %d Gen. %2d | %s",
                        s.getEra(), s.getSpecies(), s.getGeneration(),
                        s.getEndTime());
            }

            if (stats.size() - i <= 7) {
                playerIcon(g, (int) os.getX(), (int) os.getY(), Color.white, null, str);
            }
            else {
                playerIcon(g, (int) os.getX(), (int) os.getY());
            }

            if (s.getEndTime() == null) {
                legend(g, entryPosition++, c, str);
            }
            else {
                legend(g, entryPosition++, c, str);
            }
            i++;
        }
        //</editor-fold>

        GraphicUtils.centerString(g, getWidth() - 420, 50, server.getAI().getState(), 30, Color.green, Color.darkGray);
    }

    private void legend(Graphics2D g, int position, Color color, String text) {
        g.setColor(color);
        playerIcon(g, hudStartX, hudStartY + position * hudEntryHeight, Color.white, color);
        g.setFont(new Font("DejaVu Sans Mono", 0, 25));
        g.drawString(text,
                hudStartX + 25, hudStartY + position * hudEntryHeight + 10);
    }

    private void drawPlayer(Graphics2D g, PlayerData p, Color fg, Color bg) {
        int x = (int) p.onScreen().getX();
        int z = (int) p.onScreen().getY();

        playerIcon(g, x, z, fg, bg, String.format("Driver #%d %s%s",
                p.driver().getServerClient().getID(),
                p.driver().getMessage().equals("/") ? "" : "(" + p.driver().getMessage() + ")",
                p.driver().getStatus().getDrivingSince() != null ? " | " + p.driver().getStatus().getDrivingSince() : ""));
    }

    private void playerIcon(Graphics2D g, int x, int z) {
        playerIcon(g, x, z, Color.white, null);
    }

    private void playerIcon(Graphics2D g, int x, int z, Color fg, Color bg) {
        playerIcon(g, x, z, fg, bg, null);
    }

    private void playerIcon(Graphics2D g, int x, int z, Color fg, Color bg, String msg) {
        if (bg != null) {
            g.setColor(bg);
        }
        else {
            bg = g.getColor();
        }

        g.setStroke(new BasicStroke(2));
        final int inner = 14;
        final int outer = 22;
        g.fillOval(x - inner / 2, z - inner / 2, inner, inner);
        g.drawOval(x - outer / 2, z - outer / 2, outer, outer);

        if (msg != null) {
            int boxX = x + 30;
            int boxZ = z - 30;
            g.setStroke(new BasicStroke(3));
            g.drawLine(x, z, boxX, boxZ);
            GraphicUtils.centerString(g, boxX + 50, boxZ, msg, 20, fg, bg);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Helper">
    private Color transparent(Color color, double tranperency) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) MathUtils.clamp(tranperency * 255 + 0.5, 0, 255));
    }

    private Vector2D onScreen(Vector2D position) {
        return new Vector2D(position.getX() - from.getX(),
                position.getY() - from.getY()).scalarMultiply(1 / units);
    }

    private Vector2D onScreen(Vector3D position) {
        return new Vector2D(position.getX() - from.getX(),
                position.getZ() - from.getZ()).scalarMultiply(1 / units);
    }

    private void drawRect(Graphics2D g, Vector3D min, Vector3D max, Color stroke, Color fill) {
        Vector2D a = onScreen(min);
        Vector2D b = onScreen(max);

        int w = (int) (b.getX() - a.getX() + 0.5);
        int h = (int) (b.getY() - a.getY() + 0.5);

        if (stroke != null) {
            g.setColor(stroke);
            g.drawRect((int) (a.getX() + 0.5), (int) (a.getY() + 0.5), w, h);
        }
        if (fill != null) {
            g.setColor(fill);
            g.fillRect((int) (a.getX() + 0.5), (int) (a.getY() + 0.5), w, h);
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Based on Track">
    private Vector3D getCenter(Track value) {
        return switch (value) {
            case BowsersCastle_3 ->
                new Vector3D(200, 0, 50);
            case GhostValley ->
                new Vector3D(0, 0, -20);
            case MarioRaceway_N64 ->
                new Vector3D(100, 0, 0);
            default ->
                new Vector3D(200, 0, 380);
        };
    }

    private double getScale(Track value) { // Lower values -> bigger
        return switch (value) {
            case BowsersCastle_3 ->
                0.7;
            case GhostValley ->
                0.42;
            case MarioRaceway_N64 ->
                0.6;
            default ->
                0.5;
        };
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Getter / Setter">
    void setServer(AIServer server) {
        this.server = server;
    }
    //</editor-fold>
}
