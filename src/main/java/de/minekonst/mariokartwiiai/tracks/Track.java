package de.minekonst.mariokartwiiai.tracks;

import de.minekonst.mariokartwiiai.main.Main;
import java.awt.Color;
import java.io.File;
import lombok.Getter;

public enum Track {
    ToadsFactory(29, 0, new GT(Color.ORANGE), new GT(Color.LIGHT_GRAY), new GT(Color.BLUE), new GT(Color.GREEN), 10),
    MarioRaceway_N64(25, 60, new GT(Color.ORANGE), new GT(Color.LIGHT_GRAY, 1), new GT(Color.BLUE), new GT(Color.GREEN, 8), 0),
    MarioCircuit_3_SNES(79, 45, new GT(Color.ORANGE, 0x40), new GT(Color.LIGHT_GRAY, 1), new GT(Color.BLUE), new GT(Color.GREEN, 8), 0),
    BowsersCastle_N64(42, 90, new GT(Color.ORANGE, 0x80, 0x800000), new GT(Color.LIGHT_GRAY, 1, 0x800000, 0x400000), new GT(Color.BLUE), new GT(Color.GREEN, 8), 10),
    GhostValley(42, 34, new GT(Color.ORANGE, 0x40), new GT(Color.LIGHT_GRAY, 1), new GT(Color.BLUE), new GT(Color.GREEN, 8), 0),
    DelfinoSquare(70, 80, new GT(Color.ORANGE, 0x40), new GT(Color.LIGHT_GRAY, 1), new GT(Color.GREEN, 8), new GT(Color.RED, 0x10), 0),
    BowsersCastle_3(57, 70, new GT(Color.ORANGE, 0x100, 0x80), new GT(Color.LIGHT_GRAY, 1), new GT(Color.BLUE), new GT(new Color(205, 133, 63), 8), 0);

    @Getter private final DynamicMapData mapData;
    /**
     * The intervals in y (height) dimension in which to sample the ground. The
     * y radius needs to be smaller than the height difference of map parts
     * which are on top of another. If set to 0, the map is considered flat -
     * this should be used if map parts never overlap.
     */
    @Getter private final int yRadius;
    /**
     * A sample lap time in seconds. Used when tasks are taking too long
     */
    @Getter private final int lapTime;
    /**
     * The number of checkpoints in one lap
     */
    @Getter private final int checkpoints;
    private final GT road;
    private final GT hardRoad;
    private final GT offroad;
    private final GT special;

    private Track(int checkpoints, int lapTimeS, GT special, GT road, GT hardRoad, GT offroad, int yRadius) {
        this.special = special;
        this.road = road;
        this.hardRoad = hardRoad;
        this.offroad = offroad;
        this.yRadius = yRadius;
        this.lapTime = lapTimeS;

        mapData = new DynamicMapData(this, Main.getDataDir() + File.separator + "Tracks" + File.separator + toString() + File.separator + "Map.data");
        this.checkpoints = checkpoints;
    }

    public double convertHardness(int value) {
        if (contains(special.values(), value)) {
            return Hardness.SPECIAL.getValue();
        }
        else if (contains(road.values(), value)) {
            return Hardness.ROAD.getValue();
        }
        else if (contains(hardRoad.values(), value)) {
            return Hardness.HARD_GROUND.getValue();
        }
        else if (contains(offroad.values(), value)) {
            return Hardness.OFF_ROAD.getValue();
        }

        return Hardness.OUT_OF_BOUNDS.getValue();
    }

    private static boolean contains(int[] array, int value) {
        for (int v : array) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    public Color getColor(Hardness h) {
        return switch (h) {
            case SPECIAL ->
                special.color();
            case ROAD ->
                road.color();
            case HARD_GROUND ->
                hardRoad.color();
            case OFF_ROAD ->
                offroad.color();
            case OUT_OF_BOUNDS ->
                Color.BLACK;
            default ->
                null;
        };
    }

    public boolean isFlat() {
        return yRadius == 0;
    }

    public String getLoadState(boolean countdown) {
        return "../../../Data/Tracks/" + toString() + "/" + (countdown ? "Save_Countdown.state" : "Save.state");
    }

    record GT(Color color, int... values) {

    }
}
