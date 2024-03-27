package de.minekonst.mariokartwiiai.shared.tasks;

import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.server.ai.properties.AiProperties;
import de.minekonst.mariokartwiiai.shared.utils.IngameTime;
import java.io.Serializable;
import lombok.Getter;

@Getter
public class Score implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double scorePoints;
    private final IngameTime time;
    private final String string;
    private final boolean allLaps;

    private Score(double score, IngameTime time, boolean allLaps, String string) {
        this.scorePoints = score;
        this.time = time;
        this.allLaps = allLaps;
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }

    public static Score calculateScore(Driver driver, int frames, AiProperties<?, ?> properties) {
        boolean hasFinished = driver.getConnector().getState().getLap() > properties.getLaps().getValue();
        // Progress
        double progress;
        if (!hasFinished) { // Race not complete
            double progressInLap = 1.0 * driver.getConnector().getState().getCheckpoint() / driver.getTrack().getCheckpoints();
            progress = (driver.getConnector().getState().getLap() - 1.0) * 1 / 3 + progressInLap * 1 / 3;
        }
        else {
            progress = properties.getLaps().getValue() / 3.0;
        }
        double progressBonus = progress * properties.getMaxProgressScore().getValue();

        // Speed
        double speedBonus = 0;
        if (hasFinished) { // If finished, give full speed bonus to rely only on finish time
            speedBonus = properties.getMaxSpeedScore().getValue();
        }
        else if (driver.getConnector().getState().getCheckpoint() > 0
                || driver.getConnector().getState().getLap() > 1) { // Only add timebonus, if progress has been made at all
            speedBonus = driver.getConnector().getAverageSpeed() / properties.getMaxSpeed().getValue() * properties.getMaxSpeedScore().getValue();
        }

        double timeBonus = 0;
        if (hasFinished) { // Only if race completed
            double high = properties.getBadTime().getValue().getFrames();
            double low = properties.getGoodTime().getValue().getFrames();
            if (frames < high) {
                timeBonus = (high - frames) / (high - low) * properties.getMaxTimeScore().getValue();
            }
        }

        return timeBonus > 0
                ? new Score(progressBonus + timeBonus + speedBonus, new IngameTime(frames), hasFinished, String.format("Progress: %.2f + Time: %.2f + Speed: %.2f", progressBonus, timeBonus, speedBonus))
                : new Score(progressBonus + speedBonus, new IngameTime(frames), hasFinished, String.format("Progress: %.2f + Speed: %.2f", progressBonus, speedBonus));
    }
}
