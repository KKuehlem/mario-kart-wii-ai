package de.minekonst.mariokartwiiai.tracks;

import java.io.Serializable;

/**
 * Internal position of tiles which contain ground information. See {@link MapData#XZ_RESOLUTION}, {@link MapData#Y_RESOLUTION}, {@link MapData#toMapPosition(baselib.math.Vector3)
 * }
 */
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    private int x, y, z;

    public Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public Position add(int x, int y, int z) {
        return new Position(this.x + x, this.y + y, this.z + z);
    }

    public Position clone() {
        return new Position(x, y, z);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + this.x;
        hash = 17 * hash + this.y;
        hash = 17 * hash + this.z;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Position other = (Position) obj;
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        if (this.z != other.z) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("( %d | %d | %d )", x, y, z);
    }

}
