package de.minekonst.mariokartwiiai.shared.methods.learning;

import de.minekonst.mariokartwiiai.shared.utils.editortable.EditorValue;
import de.minekonst.mariokartwiiai.server.ai.AI;
import de.minekonst.mariokartwiiai.shared.methods.input.InputMethod;
import de.minekonst.mariokartwiiai.shared.tasks.LearningTask;
import de.minekonst.mariokartwiiai.shared.tasks.TaskResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;

/**
 * A learning method is used on the server side to construct the neural networks
 *
 * @param <N> The type of the neural network to use
 */
@Getter
@SuppressWarnings("serial")
public abstract class LearningMethod<N> implements Serializable {

    public static final int OUTPUT = 5;

    protected final InputMethod inputMethod;
    protected transient AI<? extends LearningMethod<N>, N> ai;

    public LearningMethod(InputMethod inputMethod) {
        this.inputMethod = inputMethod;
    }

    @SuppressWarnings("unchecked")
    protected List<N> rankNetworks(List<TaskResponse> responses) {
        return responses.stream()
                .sorted((a, b) -> Double.compare(b.getScore().getScorePoints(), a.getScore().getScorePoints()))
                .map(r -> ((LearningTask<LearningMethod<N>, N>) r.getTask()).getNetwork())
                .toList();
    }

    public byte[] toByteArray(N network) {
        try {
            ByteArrayOutputStream arr = new ByteArrayOutputStream();
            writeNetwork(network, arr);
            return arr.toByteArray();
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public N fromByteArray(byte[] array) {
        try {
            ByteArrayInputStream arr = new ByteArrayInputStream(array);
            N n = loadNetwork(arr);
            arr.close();
            return n;
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected LearningTask<LearningMethod<N>, N> createTask(N net, String type) {
        return createTask(net, type, 1, false);
    }

    protected LearningTask<LearningMethod<N>, N> createTask(N net, String type, int episodes, boolean evaluationEpisode) {
        return new LearningTask<>(
                createObserver(), net,
                ai.getProperties().getTrack().getValue().ordinal(), type,
                ai.getProperties(), ai.getBestTime(), false, null, episodes, evaluationEpisode);
    }

    public abstract List<LearningTask<LearningMethod<N>, N>> nextGeneration(List<TaskResponse> responses, boolean sucess, int newTasks);

    public LearningTask<LearningMethod<N>, N> evaluationTask(List<TaskResponse> responses) {
        return null;
    }

    public abstract N cloneNetwork(N network);

    public abstract List<EditorValue<?>> getEditorValues();

    public abstract LearningObserver<N> createObserver();

    public abstract void writeNetwork(N network, OutputStream stream) throws IOException;

    public abstract N loadNetwork(InputStream stream) throws IOException;

    /**
     * Called from ai and when this object is read
     *
     * @param ai The ai
     */
    public final void init(AI<? extends LearningMethod<N>, N> ai) {
        this.ai = ai;
        init();
    }

    protected abstract void init();

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    public TaskResponse getBest(List<TaskResponse> responses) {
        return responses.stream().max(Comparator.comparing(r -> r.getScore().getScorePoints())).get();
    }

    public void onMessage(Serializable object) {
        throw new UnsupportedOperationException("Not implemented in learning method");
    }
}
