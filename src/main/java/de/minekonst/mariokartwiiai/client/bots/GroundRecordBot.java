package de.minekonst.mariokartwiiai.client.bots;


import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import de.minekonst.mariokartwiiai.client.emulator.ConnectorState;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import de.minekonst.mariokartwiiai.main.Main;

public class GroundRecordBot extends Bot {

    /**
     * Autosave every x seconds
     */
    private static final int AUTOSAVE = 50;

    private long lastSave;

    public GroundRecordBot(Driver driver) {
        super(driver, "Ground Record Bot", "Records the ground of the current track");
    }

    @Override
    public ButtonState update() {
        ConnectorState state = driver.getConnector().getState();
        if (state.isCollisionAllWheels()) {
            driver.getTrack().getMapData().setHardness(state.getPosition(), state.getGround());
        }

        long now = System.currentTimeMillis();
        if (now - lastSave > AUTOSAVE * 1000) {
            driver.getTrack().getMapData().save();
            Main.log("[%s] Map has been autosaved",
                    new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
            lastSave = now;
        }

        return null;
    }

}
