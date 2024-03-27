package de.minekonst.mariokartwiiai.server.messages;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.minekonst.mariokartwiiai.client.DriverState;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import de.minekonst.mariokartwiiai.shared.utils.IngameTime;

@Getter
@AllArgsConstructor
public class StatusAnswer implements Serializable {

    private static final long serialVersionUID = 1L;

    private final DriverState state;
    private final String message;
    private final int fps;
    private final Vector3D position;
    private final Vector2D foward;
    private final int trackID;
    private final IngameTime drivingSince;

    public StatusAnswer(DriverState state, String message, int fps, Vector3D position, Vector2D foward, int trackID) {
        this(state, message, fps, position, foward, trackID, null);
    }

}
