package de.minekonst.mariokartwiiai.client.gui;

import de.minekonst.mariokartwiiai.shared.utils.profiler.Profiler;
import de.minekonst.mariokartwiiai.client.Driver;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.tracks.DynamicMapData;
import de.minekonst.mariokartwiiai.tracks.Hardness;
import de.minekonst.mariokartwiiai.tracks.Position;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class MapViewer extends Canvas {

    private Driver driver;
    private boolean focusPlayer = true;
    private Vector3D position;
    private int step = 1;
    private int boxWidth = 10;
    private int dX = 0;
    private int dY = 0;
    private boolean useRange;
    private volatile Point click, areaStart, areaEnd;
    private volatile boolean fillArea;
    private int yPos;
    private boolean editor;
    private double editorGround = Hardness.values()[0].getValue();

    public MapViewer() {
        super();

        super.setSize(200, 200);
        super.setVisible(true);

        //<editor-fold defaultstate="collapsed" desc="Listeners">
        super.addMouseMotionListener(new MouseMotionListener() {
            private Point dragStart;

            @Override
            public void mouseDragged(MouseEvent e) {
                if (editor) {
                    if (!e.isShiftDown()) { // Drawing
                        click = e.getPoint();
                        repaint();
                    }
                    else if (areaStart != null) {
                        areaEnd = e.getPoint();
                        repaint();
                    }

                    return;
                }
                else if (focusPlayer) {
                    return;
                }

                if (dragStart != null) {
                    double cx = dragStart.getX() - e.getPoint().getX();
                    double cy = dragStart.getY() - e.getPoint().getY();

                    dX -= cx / 2;
                    dY -= cy / 2;

                    dragStart = e.getPoint();
                    click = null;
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (focusPlayer || editor) {
                    return;
                }

                dragStart = e.getPoint();
            }
        });

        super.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (step == 1 && editor) {
                    if (e.isShiftDown()) {
                        areaStart = e.getPoint();
                    }
                    else {
                        click = e.getPoint();
                        areaStart = areaEnd = null;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (editor && e.isShiftDown() && areaStart != null) {
                    areaEnd = e.getPoint();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        //</editor-fold>
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

        if (driver == null || driver.getConnector() == null || driver.getConnector().getState() == null) {
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

        Profiler.enter("Render Map");

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        final DynamicMapData data = driver.getTrack().getMapData();
        final int spaceBetween = 0;
        final int boxCountAxisX = step * (getWidth() / (boxWidth + spaceBetween) - 2) + 4;
        final int boxCountAxisZ = step * (getHeight() / (boxWidth + spaceBetween) - 2) + 4;
        Vector3D player = driver.getConnector().getState().getPosition();
        if (focusPlayer || position == null) {
            position = new Vector3D(1, player);
        }

        Position center = data.toMapPosition(position);
        if (!focusPlayer) {
            center = center.add(-dX, 0, -dY);
            center.setY(yPos);
        }
        else {
            yPos = center.getY();
        }
        final Position playermp = data.toMapPosition(player);

        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        //</editor-fold>

        int screenX = 0, screenZ = 0;
        int playerX = -1, playerZ = -1;
        for (int mapX = center.getX() - boxCountAxisX / 2; mapX <= center.getX() + boxCountAxisX / 2; mapX += step) {
            for (int mapZ = center.getZ() - boxCountAxisZ / 2; mapZ <= center.getZ() + boxCountAxisZ / 2; mapZ += step) {
                boolean inSelectionArea = isAreaSelected() ? checkRect(areaStart, areaEnd, screenX, screenZ) : false;
                //<editor-fold defaultstate="collapsed" desc="Draw">
                Position now = new Position(mapX, center.getY(), mapZ);
                double h;
                if (useRange) {
                    h = data.getHardness(now);
                }
                else {
                    h = data.getHardnessAt(now);
                }
                Hardness hardness = Hardness.fromValue(h);
                if (hardness != Hardness.OUT_OF_BOUNDS) {
                    g.setColor(driver.getTrack().getColor(hardness));
                    g.fillRect(screenX, screenZ, boxWidth, boxWidth);
                }
                
                if (inSelectionArea) {
                    g.setColor(new Color(255, 0, 0, 150));
                    g.fillRect(screenX, screenZ, boxWidth, boxWidth);
                }

                if (playermp.getX() == now.getX() && playermp.getZ() == now.getZ()) {
                    playerX = screenX;
                    playerZ = screenZ;
                }
                //</editor-fold>

                //<editor-fold defaultstate="collapsed" desc="Editor">
                if (click != null) {
                    if (checkPoint(click, screenX, screenZ)) {
                        data.setHardnessRaw(now, editorGround);
                        click = null;
                    }
                }
                if (fillArea) {
                    if (inSelectionArea) {
                        data.setHardnessRaw(now, editorGround);
                    }
                }
                //</editor-fold>

                screenZ += boxWidth + spaceBetween;
            }

            screenX += boxWidth + spaceBetween;
            screenZ = 0;
        }
        
        if (fillArea) {
            areaStart = areaEnd = null;
            fillArea = false;
        }

        //<editor-fold defaultstate="collapsed" desc="Draw Player">
        g.setStroke(new BasicStroke(4));
        Vector2D forward = driver.getConnector().getState().getForward().scalarMultiply(30);
        if (playerX != -1 && playerZ != -1) {
            g.setColor(Color.magenta);
            g.drawRect(playerX, playerZ, boxWidth, boxWidth);

            g.drawLine(playerX + boxWidth / 2, playerZ + boxWidth / 2,
                    (int) (playerX - forward.getX() + 0.5 + boxWidth / 2), (int) (playerZ + forward.getY() + 0.5 + boxWidth / 2));
        }
        //</editor-fold>

        bs.show();
        g.dispose();
        Toolkit.getDefaultToolkit().sync();

        Profiler.exit("Render Map");
        Profiler.getRootNode("Render Map").nextIteration();
    }

    private boolean checkPoint(Point p, int x, int z) {
        return checkRect(p, p, x, z);
    }

    private boolean checkRect(Point start, Point end, int x, int z) {
        double minX = Math.min(start.getX(), end.getX());
        double maxX = Math.max(start.getX(), end.getX());
        double minY = Math.min(start.getY(), end.getY());
        double maxY = Math.max(start.getY(), end.getY());
        
        return maxX > x && minX < x + boxWidth
                && maxY > z && minY < z + boxWidth;
    }

    void fill() {
        fillArea = true;
    }

    boolean isAreaSelected() {
        return areaStart != null && areaEnd != null;
    }

    //<editor-fold defaultstate="collapsed" desc="Getter / Setter">
    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public int getBoxWidth() {
        return boxWidth;
    }

    public void setBoxWidth(int boxWidth) {
        this.boxWidth = boxWidth;
    }

    public int getyPos() {
        return yPos;
    }

    public void setyPos(int yPos) {
        this.yPos = yPos;
    }

    void setDriver(Driver driver) {
        this.driver = driver;
    }

    void setFocusPlayer(boolean focusPlayer) {
        if (!focusPlayer) {
            dX = dY = 0;
        }
        this.focusPlayer = focusPlayer;
    }

    void setUseRange(boolean useRange) {
        this.useRange = useRange;
    }

    void setEditor(boolean editor) {
        this.editor = editor;
    }

    boolean isEditor() {
        return editor;
    }

    void setEditorGround(double editorGround) {
        this.editorGround = editorGround;
    }
    //</editor-fold>

}
