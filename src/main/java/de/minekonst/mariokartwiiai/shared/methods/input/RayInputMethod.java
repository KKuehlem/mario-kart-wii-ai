package de.minekonst.mariokartwiiai.shared.methods.input;

import de.minekonst.mariokartwiiai.shared.utils.editortable.EditorValue;
import de.minekonst.mariokartwiiai.shared.utils.editortable.Validator;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.shared.utils.MathUtils;
import de.minekonst.mariokartwiiai.tracks.Hardness;
import de.minekonst.mariokartwiiai.tracks.MapData;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import lombok.Getter;

@Getter
public class RayInputMethod extends InputMethod {

    private final int angles;
    private final int viewAngle;
    private transient EditorValue<Double> maxDistance;

    public RayInputMethod(int angles, int viewAngle, double maxDistance) {
        this.angles = angles;
        this.viewAngle = viewAngle;
        getEditorValues();
    }

    @Override
    public List<EditorValue<?>> getEditorValues() {
        if (maxDistance == null) {
            maxDistance = new EditorValue<>("Max Distance", 200.0, Double.class, Validator.NON_NEG);
        }
        return List.of(maxDistance);
    }

    @Override
    public int getNeededNeurons() {
        return angles * 2;
    }

    @Override
    public double[] calculate(Driver driver) {
        double[] fov = new double[angles * 2];
        MapData data = driver.getTrack().getMapData();
        Vector2D playerForward = driver.getConnector().getState().getForward();
        playerForward = new Vector2D(playerForward.getX() * -1, playerForward.getY());
        int i = 0;

        outer:
        for (int x = 0; x < angles; x++) {
            double angle = -this.viewAngle / 2 + (1.0 * x / angles) * this.viewAngle;
            angle %= 360;
            Vector2D now = MathUtils.rotate(playerForward, angle);
            Vector3D pos = driver.getConnector().getState().getPosition();

            double hardness = data.getHardness(pos);

            now = now.scalarMultiply(0.5);
            for (double d = 0; d < maxDistance.getValue(); d += 0.5) {
                pos = pos.add(new Vector3D(now.getX(), 0, now.getY()));

                double h = data.getHardness(pos);
                if (hardness != h) {
                    fov[i++] = d / 100;
                    fov[i++] = h;
                    continue outer;
                }
            }

            fov[i++] = maxDistance.getValue() / 100;
            fov[i++] = hardness;
        }
        return fov;
    }

    @Override
    public String toString() {
        return String.format("Ray Fov (%d° width %d Angles, Max Distance = %.2f)", viewAngle, angles, maxDistance.getValue());
    }

    @Override
    public void draw(Graphics2D g, int canvasWidth, int canvasHeight, double[] fov, Driver driver) {
        double units = canvasHeight / maxDistance.getValue();
        final int dotSize = 4;
        final int y0 = canvasHeight;

        final Color selfColor = driver.getTrack().getColor(Hardness.fromValue(driver.getTrack().getMapData().getHardness(driver.getConnector().getState().getPosition())));

        for (int x = 0; x < fov.length; x += 2) {
            double d = fov[x] * 100;
            double h = fov[x + 1];

            double angle = -viewAngle / 2 + (1.0 * (x / 2) / angles) * viewAngle;
            angle %= 360;
            Vector2D forward = MathUtils.fromAngle(angle);
            forward = forward.scalarMultiply(d);
            int dx = (int) (canvasWidth / 2 - units * forward.getX() + 0.5);
            int dy = y0 - (int) (units * forward.getY() + 0.5);

            g.setColor(selfColor);
            if (d != maxDistance.getValue()) {
                g.drawLine(canvasWidth / 2, y0, dx, dy);
            }
            g.setColor(driver.getTrack().getColor(Hardness.fromValue(h)));
            g.fillRect(dx - dotSize / 2, dy - dotSize / 2, dotSize, dotSize);
        }
    }
}
