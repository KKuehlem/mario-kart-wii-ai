package de.minekonst.mariokartwiiai.tracks;

import de.minekonst.mariokartwiiai.shared.utils.profiler.Profiler;
import de.minekonst.mariokartwiiai.shared.utils.FileUtils;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class DynamicMapData extends MapData {

    /**
     * Directly stores the hardness value at a internal position
     */
    private final Map<Position, Double> map; // Actual data
    /**
     * Captured how often a ground type has been recorded at an position
     */
    private final Map<Position, Integer[]> avg; // Average data
    private final String file;
    @Getter private Position min;
    @Getter private Position max;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DynamicMapData(Track track, String file) {
        super(track);

        this.file = file;
        HashMap hardnessMap = FileUtils.readObject(file, HashMap.class);
        HashMap debugMap = FileUtils.readObject(file + "_debug", HashMap.class);
        if (debugMap != null) {
            avg = debugMap;
        }
        else {
            avg = new HashMap<>();
        }

        if (hardnessMap == null) {
            map = new HashMap<>();
        }
        else {
            map = hardnessMap;

            for (Position p : map.keySet()) {
                updateBounds(p);
            }
        }
    }

    public synchronized void save() {
        FileUtils.writeObject(file, (Serializable) map);
        FileUtils.writeObject(file + "_debug", (Serializable) avg);
    }

    @Override
    public double getHardnessAt(Position position) {
        return map.getOrDefault(position, Hardness.OUT_OF_BOUNDS.getValue());
    }

    public void setHardness(Vector3D position, double value) {
        setHardness(toMapPosition(position), value);
    }

    private synchronized void setHardness(Position p, double value) {
        Hardness h = Hardness.fromValue(value);

        if (value != Hardness.OUT_OF_BOUNDS.getValue()) {
            if (!avg.containsKey(p)) {
                setHardnessRaw(p, value);
                Integer[] arr = new Integer[Hardness.values().length];
                Arrays.fill(arr, 0);
                arr[h.ordinal()] = 1;
                avg.put(p, arr);
            }
            else {
                Integer[] arr = avg.get(p);
                arr[h.ordinal()]++;
                avg.put(p, arr);
                int max = 0;
                int index = 0;
                for (int x = 0; x < arr.length; x++) {
                    if (arr[x] > max) {
                        max = arr[x];
                        index = x;
                    }
                }

                setHardnessRaw(p, Hardness.values()[index].getValue());
            }
        }
    }

    public synchronized void setHardnessRaw(Position p, double value) {
        map.put(p, value);
        updateBounds(p);
    }

    public synchronized void closeGaps() {
        Profiler.enter("Close Gaps");

        for (int x = min.getX() - 10; x <= max.getX() + 10; x++) {
            for (int z = min.getZ() - 10; z <= max.getZ() + 10; z++) {
                for (int y = min.getY() - 10; y <= max.getY() + 10; y++) {
                    double t = getHardnessAt(new Position(x, y, z));

                    double tn = getHardnessAt(new Position(x, y, z + 1));
                    double ts = getHardnessAt(new Position(x, y, z - 1));
                    double te = getHardnessAt(new Position(x + 1, y, z));
                    double tw = getHardnessAt(new Position(x - 1, y, z));

                    if (tn == ts && t < tn) {
                        map.put(new Position(x, y, z), tn);
                        continue;
                    }
                    else if (te == tw && t < te) {
                        map.put(new Position(x, y, z), te);
                        continue;
                    }

                    double tn2 = getHardnessAt(new Position(x, y, z + 2));
                    double te2 = getHardnessAt(new Position(x + 2, y, z));
                    if (t == tn && tn2 == ts && t < tn2) {
                        map.put(new Position(x, y, z), tn2);
                        map.put(new Position(x, y, z + 1), tn2);
                    }
                    else if (t == te && te2 == tw && t < te2) {
                        map.put(new Position(x, y, z), te2);
                        map.put(new Position(x + 1, y, z), te2);
                    }
                } // End for y
            } // End for z
        } // End for x

        Profiler.exit("Close Gaps");
    }

    private void updateBounds(Position p) {
        if (min == null && max == null) {
            min = p.clone();
            max = p.clone();
        }

        if (p.getX() < min.getX()) {
            min.setX(p.getX());
        }
        if (p.getY() < min.getY()) {
            min.setY(p.getY());
        }
        if (p.getZ() < min.getZ()) {
            min.setZ(p.getZ());
        }
        if (p.getX() > max.getX()) {
            max.setX(p.getX());
        }
        if (p.getY() > max.getY()) {
            max.setY(p.getY());
        }
        if (p.getZ() > max.getZ()) {
            max.setZ(p.getZ());
        }
    }

}
