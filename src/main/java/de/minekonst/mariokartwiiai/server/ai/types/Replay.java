package de.minekonst.mariokartwiiai.server.ai.types;

import de.minekonst.mariokartwiiai.client.recorder.RecordFrame;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Replay implements Serializable {

    private final Statistics statistics;
    private final List<RecordFrame> replay;
    
    @Override
    public String toString() {
        return String.format("Era: %d | Spec: %d | Gen: %d | Score: %.2f (%s) | Duration: %.2fs", 
                statistics.getEra(), statistics.getSpecies(), statistics.getGeneration(), statistics.getScore(),
                statistics.getTimeString(), replay.size() / 60.0);
    }
}
