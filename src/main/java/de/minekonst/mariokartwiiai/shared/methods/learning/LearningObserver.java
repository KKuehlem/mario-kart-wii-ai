package de.minekonst.mariokartwiiai.shared.methods.learning;

import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.shared.methods.input.InputMethod;
import de.minekonst.mariokartwiiai.shared.tasks.Score;
import java.io.Serializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A learning observer is used by the client during training
 *
 * @param <N> The type of the neural network
 */
@RequiredArgsConstructor
@Getter
public abstract class LearningObserver<N> implements Serializable {

    protected final LearningMethod<N> learningMethod;
    protected final InputMethod inputMethod;
    private transient Driver driver;

    public void init(N network, Driver driver) {
        this.driver = driver;
        init(network);
    }

    protected void sendToServer(Serializable o) {
        driver.getClient().send(new LearningMethodMessage(o));
    }

    protected abstract void init(N network);

    public abstract LearningObserverResponse beforeFrame(N network, double[] input, boolean evaluationRun);

    public abstract void afterFrame(N network, Score currentScore, int checkpointsCrossed);

    public abstract void onEpisodeStart(N network);

    public abstract void onEpisodeEnd(N network, Score score);
    
}
