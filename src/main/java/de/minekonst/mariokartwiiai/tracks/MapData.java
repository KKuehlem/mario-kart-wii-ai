package de.minekonst.mariokartwiiai.tracks;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;


public abstract class MapData {

    public static final double XZ_RESOLUTION = 1;
    public static final double Y_RESOLUTION = 2.5;
    protected final Track track;

    public MapData(Track track) {
        this.track = track;
    }

    /**
     * Get the hardness at an actual position in the map as returned by the
     * emulator.
     *
     * @param position The position in the map
     *
     * @return The hardness at this position as returned by the assigned
     *         hardness using {@link Hardness#getValue()}
     */
    public final double getHardness(Vector3D position) {
        return getHardness(toMapPosition(position));
    }

    /**
     * Get the hardness at an specified internal map position
     *
     * @param position The position in tiles
     *
     * @return The hardness at this position
     *
     * @see MapData#toMapPosition(org.apache.commons.math3.geometry.euclidean.threed.Vector3D) 
     */
    public final double getHardness(Position position) {
        return track.isFlat() ? getHardnessAt(position) : getHardnessRange(position);
    }

    private double getHardnessRange(Position p) {
        double current = Hardness.OUT_OF_BOUNDS.getValue();
        int startY = p.getY();

        for (int y = startY - track.getYRadius(); y <= startY + track.getYRadius(); y++) {
            p.setY(y);
            double sampled = getHardnessAt(p);

            if (sampled > current) {
                current = sampled;

                if (current == Hardness.SPECIAL.getValue()) {
                    break; // Cannot get higher than special
                }
            }
        }

        return current;
    }

    public abstract double getHardnessAt(Position position);

    /**
     * Convert a Vector3 to a map position (depends on Resolution)
     *
     * @param pos The position (the emulator gives you)
     *
     * @return The Map Position
     */
    public Position toMapPosition(Vector3D pos) {
        return new Position((int) (pos.getX() / XZ_RESOLUTION + 0.5), (track.isFlat() ? 0 : (int) (pos.getY() / Y_RESOLUTION + 0.5)), (int) (pos.getZ() / XZ_RESOLUTION + 0.5));
    }
}
