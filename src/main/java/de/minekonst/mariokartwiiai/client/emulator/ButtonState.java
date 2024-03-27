package de.minekonst.mariokartwiiai.client.emulator;

import de.minekonst.mariokartwiiai.client.recorder.RecordFrame;
import lombok.Getter;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

@Getter
public class ButtonState {

    public static final ButtonState NEUTRAL = new ButtonState(false, false, false, false, new Vector2D(50, 50));

    private final boolean a, b, trick, start;
    private final int mainStickX, mainStickY;
    private final Vector2D stick;

    /**
     * Create a ButtonState
     *
     * @param a     Is A pressed?
     * @param b     Is B pressed?
     * @param start Is Start pressed?
     * @param stick The state of the main stick. (50|50) is the center
     */
    public ButtonState(boolean a, boolean b, boolean start, Vector2D stick) {
        this(a, b, start, false, stick);
    }

    /**
     * Create a ButtonState
     *
     * @param a     Is A pressed?
     * @param b     Is B pressed?
     * @param start Is Start pressed?
     * @param trick Is the trick button pressed
     * @param stick The state of the main stick. (50|50) is the center
     */
    public ButtonState(boolean a, boolean b, boolean start, boolean trick, Vector2D stick) {
        this.a = a;
        this.b = b;
        this.start = start;
        this.mainStickX = toStick(stick.getX());
        this.mainStickY = toStick(stick.getY());
        this.stick = new Vector2D(1, stick);
        this.trick = trick;
    }

    public ButtonState(boolean a, boolean b, boolean start, boolean trick, byte x, byte y) {
        this.a = a;
        this.b = b;
        this.start = start;
        this.mainStickX = x;
        this.mainStickY = y;
        this.stick = new Vector2D(toPercent(x), toPercent(y)); // TODO
        this.trick = trick;
    }

    /**
     * Get the control stick, (50 | 50) is the center. Y is effectively unused
     * @return The control stick
     */
    public Vector2D getStick() {
        return new Vector2D(1, stick);
    }

    private int toStick(double percent) {
        return (int) (59 + 69 * percent / 50 + 0.5);
    }

    private double toPercent(int stick) {
        return 50.0 / 69 * (stick - 59);
    }

    public RecordFrame toRecordFrame() {
        return new RecordFrame(a, b, trick, mainStickX, mainStickY);
    }

}
