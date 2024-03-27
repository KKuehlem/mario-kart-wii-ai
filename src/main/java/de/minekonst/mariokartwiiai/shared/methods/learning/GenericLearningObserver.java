package de.minekonst.mariokartwiiai.shared.methods.learning;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import de.minekonst.mariokartwiiai.shared.methods.input.InputMethod;
import de.minekonst.mariokartwiiai.shared.tasks.Score;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.factory.Nd4j;

public class GenericLearningObserver extends LearningObserver<MultiLayerNetwork> {

    GenericLearningObserver(LearningMethod<MultiLayerNetwork> learningMethod, InputMethod inputMethod) {
        super(learningMethod, inputMethod);
    }

    @Override
    public void init(MultiLayerNetwork network) {
    }

    @Override
    public LearningObserverResponse beforeFrame(MultiLayerNetwork network, double[] input, boolean evaluationRun) {
        double[][] matrix = new double[1][];
        matrix[0] = input;
        double[] out = network.output(Nd4j.create(matrix)).toDoubleVector();
        ButtonState bs = new ButtonState(out[0] > 0, out[1] > 0, false, out[2] > 0, calcStick(out[3], out[4]));
        
        return LearningObserverResponse.builder()
                .buttons(bs)
                .build();
    }

    private Vector2D calcStick(double x, double y) {
        return new Vector2D(stickPos(x) * 50 + 50, stickPos(y) * 50 + 50);
    }

    /**
     * Helps pressing the stick easier
     *
     * @param x The value of the stick [-1, 1]
     *
     * @return The new value for the stick [-1, 1]
     */
    private double stickPos(double x) {
        double nx = x;
        if (nx > 0.01) {
            nx = Math.min(5 * x + 0.2, 1);
        }
        else if (nx < -0.01) {
            nx = Math.max(5 * x - 0.2, -1);
        }
        return nx;
    }

    @Override
    public void afterFrame(MultiLayerNetwork network, Score currentScore, int checkpointsCrossed) {
    }

    @Override
    public void onEpisodeStart(MultiLayerNetwork network) {
    }

    @Override
    public void onEpisodeEnd(MultiLayerNetwork network, Score score) {
    }

}
