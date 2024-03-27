package de.minekonst.mariokartwiiai.server.ai.types;

import de.minekonst.mariokartwiiai.shared.methods.learning.LearningMethod;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ArchivedNetwork<L extends LearningMethod<N>, N> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter private final Statistics statistics;
    private final byte[] data;

    public ArchivedNetwork(Statistics statistics, LearningMethod<N> method, N network) {
        this.statistics = statistics;
        this.data = method.toByteArray(network);
    }
    

    @Override
    public String toString() {
        return String.format("Era: %d | Spec: %d | Gen: %d | Score: %.2f (%s)",
                statistics.getEra(), statistics.getSpecies(), statistics.getGeneration(), statistics.getScore(),
                statistics.getTimeString());
    }
    
    @SuppressWarnings("unchecked")
    public N getNetworkUnchecked(LearningMethod<?> method) {
        return getNetwork((LearningMethod<N>) method);
    }
    
    public N getNetwork(LearningMethod<N> method) {
        return method.fromByteArray(data);
    }
}
