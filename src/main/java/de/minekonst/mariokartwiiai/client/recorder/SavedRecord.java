package de.minekonst.mariokartwiiai.client.recorder;

import de.minekonst.mariokartwiiai.shared.utils.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.tracks.Track;
import lombok.Getter;

@Getter
public class SavedRecord {

    private static final String LOCATION = Main.getDataDir() + File.separator + "Records" + File.separator;
    @Getter private static final List<SavedRecord> records = new ArrayList<>(10);

    private final int track;
    private final List<? extends RecordFrame> frames;
    private transient String name;
    private transient int fileSize;

    public SavedRecord(Track track, List<? extends RecordFrame> frames) {
        this.track = track.ordinal();
        this.frames = frames;
        name = "";
    }

    public Track getTrack() {
        return Track.values()[track];
    }

    public List<? extends RecordFrame> getFrames() {
        return frames;
    }

    public void save(String file) {
        byte[][] arr = new byte[frames.size() + 1][3];
        int x = 1;
        arr[0][0] = (byte) track;
        for (RecordFrame f : frames) {
            arr[x][0] = f.getButtons();
            arr[x][1] = f.getX();
            arr[x][2] = f.getY();
            x++;
        }
        File f = new File(LOCATION);
        if (!f.exists()) {
            f.mkdir();
        }
        FileUtils.writeObject(LOCATION + File.separator + file, arr);
    }

    public String getTimeString() {
        return toTimeString(frames.size());
    }

    public static String toTimeString(int frames) {
        String t = "";
        double time = frames / 60.0;
        int min = (int) (time / 60);
        if (min > 0) {
            t = min + " min ";
        }
        t += ((int) (time % 60)) + " s";
        return t;
    }

    public static void reload() {
        records.clear();

        File dir = new File(LOCATION);
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".record")) {
                    SavedRecord r = load(f.getAbsolutePath());
                    r.name = f.getName();
                    r.fileSize = (int) (f.length() / 1_000);
                    records.add(r);
                }
            }
        }
    }

    private static SavedRecord load(String file) {
        byte[][] arr = FileUtils.readObject(file, byte[][].class);
        List<RecordFrame> frames = new ArrayList<>(arr.length - 1);
        for (int x = 1; x < arr.length; x++) {
            frames.add(new RecordFrame(arr[x][0], arr[x][1], arr[x][2]));
        }

        return new SavedRecord(Track.values()[arr[0][0]], frames);
    }

}
