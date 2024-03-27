package de.minekonst.mariokartwiiai.client.recorder;

import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.client.emulator.ConnectorState;
import java.util.ArrayList;
import java.util.List;
import de.minekonst.mariokartwiiai.tracks.Track;

public class Recorder {

    private static boolean recording;
    private static final List<RecordFrame> frames = new ArrayList<>(1_000);
    private static Track track;
    private static boolean load;

    private Recorder() {

    }

    public static void update(Driver driver) {
        if (load) {
            load = false;
            driver.loadTrack(track);
        }

        if (recording) {
            ConnectorState state = driver.getConnector().getState();
            frames.add(new RecordFrame(state.isA(), state.isB(), state.isTrick(), state.getX(), state.getY()));
        }
    }

    public static boolean isRecording() {
        return recording;
    }

    public static void start(Track t) {
        recording = true;
        load = true;
        track = t;
        frames.clear();
    }

    public static SavedRecord stop() {
        recording = false;
        return new SavedRecord(track, frames);
    }

    public static String currentTime() {
        return SavedRecord.toTimeString(frames.size());
    }
}
