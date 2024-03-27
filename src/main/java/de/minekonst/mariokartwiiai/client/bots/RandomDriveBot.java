package de.minekonst.mariokartwiiai.client.bots;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import de.minekonst.mariokartwiiai.shared.utils.MathUtils;

public class RandomDriveBot extends Bot {

    private static final ButtonState LEFT = new ButtonState(true, true, false, new Vector2D(0, 50));
    private static final ButtonState RIGHT = new ButtonState(true, true, false, new Vector2D(100, 50));
    private static final ButtonState FORWARD = new ButtonState(true, false, false, new Vector2D(50, 50));

    private long start;
    private double time;
    private ButtonState action;

    public RandomDriveBot(Driver driver) {
        super(driver, "Random Drive Bot", "Drives randomly (Explore Tracks alone)");
    }

    @Override
    public ButtonState update() {
        long now = System.currentTimeMillis();

        if (now - start > time) {
            // Change action
            double r = Math.random();
            if (r < 0.4) {
                action = r < 0.2 ? LEFT : RIGHT;
                if (Math.random() < 0.9) {
                    time = MathUtils.random(500, 1_000);
                }
                else {
                    time = MathUtils.random(1_000, 2_000);
                }
            }
            else {
                action = FORWARD;
                time = MathUtils.random(500, 2_500);
            }

            start = now;
        }

        return action;
    }

}
