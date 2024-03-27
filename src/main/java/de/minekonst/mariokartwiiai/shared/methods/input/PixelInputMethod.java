package de.minekonst.mariokartwiiai.shared.methods.input;

import de.minekonst.mariokartwiiai.shared.utils.editortable
.EditorValue;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.client.emulator.Connector;
import de.minekonst.mariokartwiiai.shared.utils.MathUtils;
import de.minekonst.mariokartwiiai.tracks.Hardness;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public class PixelInputMethod extends InputMethod {

    private final int width;
    private final int height;
    private final double step;

    public PixelInputMethod(int width, int height, double step) {
        this.width = width;
        this.height = height;
        this.step = step;
    }

    @Override
    public int getNeededNeurons() {
        return width * height;
    }

    @Override
    public double[] calculate(Driver driver) {
        Connector connector = driver.getConnector();
        double[] fov = new double[width * height];

        Vector3D forward = new Vector3D(connector.getState().getForward().getX(), 0, connector.getState().getForward().getY() * -1);
        Vector2D right2d = MathUtils.rotate(connector.getState().getForward(), 90);
        Vector3D right = new Vector3D(right2d.getX() * -1, 0, right2d.getY());

        int i = 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                Vector3D now = connector.getState().getPosition().add(forward.scalarMultiply(z * step)).subtract(forward.scalarMultiply(width * step));
                now = now.add(right.scalarMultiply((x - width / 2) * step));

                fov[i++] = driver.getTrack().getMapData().getHardness(now);
            }
        }

        return fov;
    }
    
    @Override
    public void draw(Graphics2D g, int canvasWidth, int canvasHeight, double[] fov, Driver driver) {
        int width = canvasWidth / this.width;
        int height = canvasHeight/ this.height;

        int i = 0;
        for (int x = 0; x < this.width; x++) {
            for (int z = 0; z < this.height; z++) {
                g.setColor(driver.getTrack().getColor(Hardness.fromValue(fov[i])));
                g.fillRect(x + x * width, z + z * height, width, height);

                i++;
            }
        }
    }

    @Override
    public List<EditorValue<?>> getEditorValues() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String toString() {
        return String.format("Pixel Fov (W = %d, H = %d, Step = %.2f)", width, height, step);
    }
}
