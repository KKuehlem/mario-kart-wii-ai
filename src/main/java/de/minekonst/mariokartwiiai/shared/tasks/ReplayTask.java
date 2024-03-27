package de.minekonst.mariokartwiiai.shared.tasks;


import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import de.minekonst.mariokartwiiai.client.recorder.RecordFrame;
import de.minekonst.mariokartwiiai.client.recorder.SavedRecord;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.server.ai.properties.AiProperties;
import de.minekonst.mariokartwiiai.shared.methods.input.InputMethod;
import java.util.List;

public class ReplayTask extends Task {

    private final List<RecordFrame> replay;
    private final SavedRecord record;
    private final AiProperties properties;
    private transient int pointer;
    private transient int inputFrame;

    public ReplayTask(int track, InputMethod fov, List<RecordFrame> replay, AiProperties properties) {
        super(track, fov, "- Replay -");

        this.replay = replay;
        this.record = null;
        this.pointer = 0;
        this.properties = properties;
    }

    public ReplayTask(int track, InputMethod fov, SavedRecord replay) {
        super(track, fov, "- Replay -");

        this.record = replay;
        this.replay = null;
        this.pointer = 0;
        properties = null;
    }

    @Override
    public ButtonState onNextFrame(Driver driver) {
        if (pointer == 0) {
            inputFrame = driver.getConnector().getState().getFrame();
            Main.log("Replay | First Frame = %,d", inputFrame);
        }
        if (driver.getConnector().getState().getFrame() != inputFrame) {
            Main.log("Expected frame %,d got %,d", inputFrame,
                    driver.getConnector().getState().getFrame());
        }
        inputFrame++;

        ButtonState bs = null;
        if (isActive()) {
            if (replay != null) {
                bs = replay.get(pointer).toButtonState();
            }
            else {
                bs = record.getFrames().get(pointer).toButtonState();
            }
            pointer++;
        }

        score = properties != null ? Score.calculateScore(driver, pointer, properties) : null;

        return bs;
    }

    @Override
    public TaskResponse checkFinished(Driver driver) {
        if (!isActive()) {
            return new TaskResponse(this, score, null, driver.getConnector().getState().getPosition(),
                    driver.getConnector().getState().getRotation(), "Replay finished");
        }
        else {
            return null;
        }
    }

    private boolean isActive() {
        if (replay != null) {
            return pointer < replay.size();
        }
        else {
            return pointer < record.getFrames().size();
        }
    }

}
