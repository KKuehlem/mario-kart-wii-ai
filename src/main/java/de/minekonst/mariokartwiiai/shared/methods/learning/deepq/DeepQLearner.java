package de.minekonst.mariokartwiiai.shared.methods.learning.deepq;

import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.DeepQHelper.Output;
import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types.MemoryEntry;
import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types.RewardMessage;
import de.minekonst.mariokartwiiai.shared.utils.TimeUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.deeplearning4j.rl4j.agent.learning.behavior.ILearningBehavior;
import org.deeplearning4j.rl4j.learning.Learning;
import org.deeplearning4j.rl4j.learning.configuration.QLearningConfiguration;
import org.deeplearning4j.rl4j.network.dqn.DQN;
import org.deeplearning4j.rl4j.observation.Observation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * Server side
 */
public class DeepQLearner {

    private final DQN currentNet;
    private final ILearningBehavior<Integer> learningBehavior;
    private final List<RewardMessage> messages = Collections.synchronizedList(new ArrayList<>());
    private final DeepQLearning learning;

    public DeepQLearner(DQN init, DeepQLearning learning) {
        this.learning = learning;
        currentNet = init;
        QLearningConfiguration conf = learningCfg();
        learningBehavior = DeepQHelper.buildLearningBehavior(currentNet, conf, Nd4j.getRandomFactory().getNewRandomInstance(conf.getSeed()));

        learningBehavior.handleEpisodeStart();
        learningBehavior.notifyBeforeStep();

        Thread t = new Thread(() -> {
            while (true) {
                if (!messages.isEmpty()) {
                    System.out.println("Starting batch");
                    long start = System.nanoTime();
                    handleExperience(messages.remove(0));
                    System.out.printf("End: %,.2f ms\n", (System.nanoTime() - start) / 1_000_000.0);
                }
                else {
                    TimeUtils.sleep(10);
                }
            }
        }, "Learning Thread");
        t.setDaemon(true);
        t.start();
    }

    void onMessage(RewardMessage msg) {
        messages.add(msg);
    }

    private synchronized void handleExperience(RewardMessage msg) {
        if (msg.getMemory().isEmpty()) {
            return;
        }

        final double MAX_DISCOUNT = 0.5;

        System.out.println("=== Before ===");
        measure(msg.getMemory().get(0), msg.getReward());

        int x = 0;
        for (MemoryEntry m : msg.getMemory()) {
            Output former = DeepQHelper.getOutput(currentNet, m.getInput());
            
            double likelyhood = learning.getKeepExperience().getValue();
            if (former.q() > 0.6) {
                likelyhood *= 1 - former.q();
            }
            if (Math.random() <= likelyhood || former.action() != m.getActionTaken()) {

                // 0 is the last action taken <=> closest to the reward
                double discount = 1 - (MAX_DISCOUNT * x / msg.getMemory().size());

                learningBehavior.notifyBeforeStep();
                Observation o = DeepQHelper.getObservation(m.getInput());
                learningBehavior.handleNewExperience(o, m.getActionTaken(), msg.getReward() * discount, false);
            }
            x++;
        }

        System.out.println("==== After ====");
        measure(msg.getMemory().get(0), msg.getReward());
    }

    private void measure(MemoryEntry m, double reward) {
        Output o = DeepQHelper.getOutput(currentNet, m.getInput());

        System.out.printf("Action in Memory   : %d\n", m.getActionTaken());
        System.out.printf("Action from Network: %d\n", o.action());
        System.out.printf("Q-Value            : %.2f%s\n", o.q(), o.q() < 0.9 ? "" : " ".repeat(10) + "|");
        System.out.printf("Reward             : %.2f\n", reward);
    }

    void onEnd() {
        learningBehavior.handleEpisodeStart();
    }

    private QLearningConfiguration learningCfg() {
        return QLearningConfiguration.builder()
                .seed((long) (Math.random() * 100_000_000))
                .maxEpochStep(20_000) // Max steps per epoch
                .maxStep(150_000) // Max steps in general
                .expRepMaxSize(150_000) // Experience Replay max size
                .batchSize(128) // Old: 256
                .targetDqnUpdateFreq(5_000) // Old: 500
                .updateStart(0) // Number of steps with noop warmup
                .rewardFactor(0.001) // 0.01
                .gamma(0.99) // 0.99
                .errorClamp(1.0) // td-error cliping
                .minEpsilon(0.1) // 0.1
                .epsilonNbStep(10_000) // Number of steps for epsilon greedy | 1_000
                .doubleDQN(true)
                .build();
    }

    synchronized DQN getCurrentNetwork() {
        return currentNet;
    }
}
