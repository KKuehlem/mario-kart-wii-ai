package de.minekonst.mariokartwiiai.client.emulator;

import de.minekonst.mariokartwiiai.shared.utils.MathUtils;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.minekonst.mariokartwiiai.tracks.Track;
import lombok.Getter;

@Getter
public class ConnectorState {

    private final Vector3D position;
    private final Vector3D rotation;
    private final int checkpoint;
    private final double ground;
    private final int groundRaw;
    private final Vector2D forward;
    private final int frame;
    private final int lap;
    private final boolean a, b, trick;
    private final int x, y;
    private final int bitfield0;
    private final int bitfield1;

    public ConnectorState(Vector3D position, Vector3D rotation, int checkpoint, int groundRaw, int frame, int lap,
            boolean a, boolean b, boolean trick, int x, int y, int bitfield0, int bitfield1, Track track) {
        this.position = position.scalarMultiply(1.0 / 100);
        this.rotation = rotation;
        this.checkpoint = checkpoint;
        this.ground = track.convertHardness(groundRaw);
        this.groundRaw = groundRaw;
        this.forward = MathUtils.fromAngle(rotation.getY());
        this.frame = frame;
        this.lap = lap;
        this.a = a;
        this.b = b;
        this.trick = trick;
        this.x = x;
        this.y = y;
        this.bitfield0 = bitfield0; // https://mkwii.com/showthread.php?tid=1795
        this.bitfield1 = bitfield1;
    }

    //<editor-fold defaultstate="collapsed" desc="Bitfields / Getter">
    private static boolean getBit(int val, int bit) {
        return ((val >> bit) & 1) == 1;
    }

    public boolean isGrounded() {
        return getBit(bitfield0, 18);
    }

    public boolean isWheelie() {
        return getBit(bitfield0, 29);
    }

    public boolean isHop() {
        return getBit(bitfield0, 2);
    }

    public boolean isCollisionAllWheels() {
        return getBit(bitfield0, 12);
    }
    
    public Vector3D getPosition() {
        return position.scalarMultiply(1);
    }
    
    public Vector3D getRotation() {
        return rotation.scalarMultiply(1);
    }
    //</editor-fold>

}
