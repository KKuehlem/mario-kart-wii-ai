package de.minekonst.mariokartwiiai.shared.tasks;

import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import java.io.Serializable;
import de.minekonst.mariokartwiiai.shared.utils.IngameTime;
import de.minekonst.mariokartwiiai.tracks.Track;
import de.minekonst.mariokartwiiai.shared.methods.input.InputMethod;
import lombok.Getter;

@Getter
public abstract class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Score score;
    protected volatile String additionalTitle;
    private int clientID;
    private int taskID;
    private final int trackID;
    private final InputMethod fovMode;
    private long creationTime;

    public Task(int track, InputMethod fovMode, String additionalTitle) {
        this.trackID = track;
        this.fovMode = fovMode;
        this.additionalTitle = additionalTitle;
    }

    /**
     * This method is called when the task get send to the driver. This method
     * will set the creation time
     *
     * @param taskID   The internal ID of the task (server side)
     * @param driverID The internal ID of the Driver (server side)
     */
    public void onActivate(int taskID, int driverID) {
        this.taskID = taskID;
        this.clientID = driverID;
        this.creationTime = System.currentTimeMillis();
    }

    public abstract ButtonState onNextFrame(Driver driver);

    public abstract TaskResponse checkFinished(Driver driver);

    //<editor-fold defaultstate="collapsed" desc="Getter">
    /**
     * Get the track for this task
     *
     * @return The track
     */
    public Track getTrack() {
        return Track.values()[trackID];
    }

    public IngameTime getDrivingSince() {
        return null; // Can be overriden
    }
    //</editor-fold>

}
