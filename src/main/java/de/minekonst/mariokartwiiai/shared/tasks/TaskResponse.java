package de.minekonst.mariokartwiiai.shared.tasks;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.minekonst.mariokartwiiai.client.recorder.RecordFrame;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Task task;
    private final Score score;
    private final List<RecordFrame> replay;
    private final String type;
    private final Vector3D lastPosition;
    private final Vector3D lastRotation;
    private final String endReason;

    public TaskResponse(Task task, Score score, List<RecordFrame> replay, Vector3D lastPosition, Vector3D lastRotation, String endReason) {
        this(task, score, replay, null, lastPosition, lastRotation, endReason);
    }

    @Override
    public String toString() {
        return String.format("Score: %s", score.toString());
    }
}
