package de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Action {

    public static final int X_STICK_RESOLUTION = 5; // Per direction
    public static final int POSSIBLILITIES = getAllPossibleActions().size();

    private final boolean a;
    private final boolean b;
    private final boolean trick;
    @Getter(AccessLevel.NONE) private final int xStick;

    public Action(boolean a, boolean b, boolean trick, int xStick) {
        if (xStick < -X_STICK_RESOLUTION || xStick > X_STICK_RESOLUTION) {
            throw new IllegalArgumentException("Illegal x stick: " + xStick);
        }
        this.a = a;
        this.b = b;
        this.trick = trick;
        this.xStick = xStick;
    }

    public ButtonState toButtonState() {
        return new ButtonState(a, b, false, trick, new Vector2D(50.0 * xStick / X_STICK_RESOLUTION + 50, 50));
    }

    public static List<Action> getAllPossibleActions() {
        List<Action> list = new ArrayList<>();
        list.add(new Action(true, false, false, 0)); // NOOP

        for (int a = 1; a <= 1; a++) { // Always a
            for (int b = 0; b <= 1; b++) {
                for (int trick = 0; trick <= 1; trick++) {
                    for (int x = -X_STICK_RESOLUTION; x <= X_STICK_RESOLUTION; x++) {
                        list.add(new Action(a == 1, b == 1, trick == 1, x));
                    }
                }
            }
        }

        return list;
    }
}
