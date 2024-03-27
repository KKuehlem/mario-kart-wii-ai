package de.minekonst.mariokartwiiai.shared.methods.learning.deepq;

import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types.MemoryEntry;
import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types.Action;
import de.minekonst.mariokartwiiai.shared.utils.profiler.Profiler;
import de.minekonst.mariokartwiiai.shared.methods.learning.*;
import de.minekonst.mariokartwiiai.shared.methods.input.InputMethod;
import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.DeepQHelper.Output;
import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types.RewardMessage;
import de.minekonst.mariokartwiiai.shared.tasks.Score;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.deeplearning4j.rl4j.learning.Learning;
import org.deeplearning4j.rl4j.network.dqn.DQN;
import org.deeplearning4j.rl4j.observation.Observation;
import org.nd4j.linalg.api.ndarray.INDArray;

@SuppressWarnings("deprecation")
public class DeepQLearningObserver extends LearningObserver<DQN> {

    private static final int REMOVE_REWARD = 60 * 5;
    private static final int NO_REWARD_PENALTY_FRAMES = 60 * 7; // Make property

    private final double experimentation;
    private final double qThreshold;
    private final int inputMethodChange;
    private final int inputIntervall;
    private final int memorySteps;
    private final int memoryIntervall;

    private transient double[] lastInput;
    private transient int lastAction;
    private transient double lastScore;
    private transient List<Action> actions;
    private transient int frame;
    private transient int inputMethodFrames;
    private transient boolean driveByNetwork;
    private transient double lastQValue;
    private transient LinkedList<MemoryEntry> memory;
    private transient LinkedList<Double> lastRewards;
    private transient int lastRewardFrame;

    DeepQLearningObserver(LearningMethod<DQN> learningMethod, InputMethod inputMethod,
            double experimentation, int inputMethodChange, int inputIntervall, int memorySteps, int memoryIntervall, double qThreshold) {
        super(learningMethod, inputMethod);
        this.experimentation = experimentation;
        this.inputMethodChange = inputMethodChange;
        this.inputIntervall = inputIntervall;
        this.memorySteps = memorySteps;
        this.memoryIntervall = memoryIntervall;
        this.qThreshold = qThreshold;
    }

    @Override
    public void init(DQN dqn) {
        actions = Action.getAllPossibleActions();
        memory = new LinkedList<>();
        lastRewards = new LinkedList<>();
    }

    @Override
    public void onEpisodeStart(DQN network) {

    }

    @Override
    public LearningObserverResponse beforeFrame(DQN network, double[] input, boolean evaluationRun) {
        if (inputMethodFrames >= inputMethodChange) { // Change input method?
            driveByNetwork = Math.random() > experimentation;
            inputMethodFrames = 0;
        }
        if (inputMethodFrames % inputIntervall == 0) { // Input frame?
            Output o = DeepQHelper.getOutput(network, input);
            lastAction = o.action();
            lastQValue = o.q();

            if (driveByNetwork || evaluationRun || lastQValue > qThreshold) { // Use network to drive
                driveByNetwork = true;
            }
            else { // Random action
                lastAction = (int) (Math.random() * actions.size());
            }

            lastInput = input;
        }

        inputMethodFrames++;
        if (frame % memoryIntervall == 0) {
            memory.addFirst(new MemoryEntry(input, lastAction));
            if (memory.size() > memorySteps) {
                memory.removeLast();
            }
        }

        String title = String.format("Input: %s\n"
                + "QValue: %.2f\n",
                driveByNetwork ? "Network" : "Random", driveByNetwork ? lastQValue : 0);
        for (double r : lastRewards) {
            title += String.format("\n%s%.2f", (r > 0 ? "+" : ""), r);
        }
        return LearningObserverResponse.builder()
                .buttons(actions.get(lastAction).toButtonState())
                .title(title)
                .build();
    }

    @Override
    public void afterFrame(DQN network, Score currentScore, int checkpointsCrossed) {
        double reward = 0;
        if (checkpointsCrossed != 0) {
            reward = checkpointsCrossed > 0 ? checkpointsCrossed : checkpointsCrossed * 2;
        }
        else if (frame - lastRewardFrame > NO_REWARD_PENALTY_FRAMES) {
            reward = -1.2;
            lastRewardFrame = frame;
        }

        if (reward != 0) {
            Profiler.enter("Handle new experience");
            lastRewards.addFirst(reward);
            handleExperience(reward);

            Profiler.exit("Handle new experience");
            lastScore = currentScore.getScorePoints();
            lastRewardFrame = frame;
        }

        if (frame % REMOVE_REWARD == 0 && !lastRewards.isEmpty()) {
            lastRewards.removeLast();
        }

        frame++;
    }

    @Override
    public void onEpisodeEnd(DQN dqn, Score score) {
        Observation o = DeepQHelper.getObservation(lastInput);

        handleExperience(score.isAllLaps() ? 1 : -1);

        memory.clear();
        System.gc();
    }

    private void handleExperience(double reward) {
        super.sendToServer(new RewardMessage(new ArrayList<>(memory), reward));
    }

}
