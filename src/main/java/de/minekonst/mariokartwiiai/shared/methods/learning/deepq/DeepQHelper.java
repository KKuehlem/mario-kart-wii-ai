package de.minekonst.mariokartwiiai.shared.methods.learning.deepq;

import org.deeplearning4j.rl4j.agent.learning.algorithm.dqn.BaseDQNAlgorithm;
import org.deeplearning4j.rl4j.agent.learning.algorithm.dqn.BaseTransitionTDAlgorithm;
import org.deeplearning4j.rl4j.agent.learning.algorithm.dqn.DoubleDQN;
import org.deeplearning4j.rl4j.agent.learning.algorithm.dqn.StandardDQN;
import org.deeplearning4j.rl4j.agent.learning.behavior.ILearningBehavior;
import org.deeplearning4j.rl4j.agent.learning.behavior.LearningBehavior;
import org.deeplearning4j.rl4j.agent.learning.update.FeaturesLabels;
import org.deeplearning4j.rl4j.agent.learning.update.UpdateRule;
import org.deeplearning4j.rl4j.agent.learning.update.updater.NeuralNetUpdaterConfiguration;
import org.deeplearning4j.rl4j.agent.learning.update.updater.sync.SyncLabelsNeuralNetUpdater;
import org.deeplearning4j.rl4j.experience.ReplayMemoryExperienceHandler;
import org.deeplearning4j.rl4j.experience.StateActionRewardState;
import org.deeplearning4j.rl4j.learning.Learning;
import org.deeplearning4j.rl4j.learning.configuration.QLearningConfiguration;
import org.deeplearning4j.rl4j.network.ITrainableNeuralNet;
import org.deeplearning4j.rl4j.network.dqn.DQN;
import org.deeplearning4j.rl4j.network.dqn.IDQN;
import org.deeplearning4j.rl4j.observation.Observation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.rng.Random;
import org.nd4j.linalg.factory.Nd4j;

public class DeepQHelper {

    public record Output(int action, double q){}
    
    private DeepQHelper() {

    }

    public static Observation getObservation(double[] input) {
        return new Observation(Nd4j.create(new double[][]{input}));
    }
    
    public static Output getOutput(DQN net, double[] input) {
        Observation o = getObservation(input);
        INDArray output = net.output(o).get("Q");
        o.getData().close();
        int action = Learning.getMaxAction(output);
        double q = output.getDouble(action);
        output.close();
        
        return new Output(action, q);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ILearningBehavior<Integer> buildLearningBehavior(IDQN qNetwork, QLearningConfiguration conf, Random random) {
        ITrainableNeuralNet target = qNetwork.clone();
        BaseTransitionTDAlgorithm.Configuration algConfig = BaseTransitionTDAlgorithm.Configuration.builder()
                .gamma(conf.getGamma())
                .errorClamp(conf.getErrorClamp())
                .build();

        BaseDQNAlgorithm updateAlgorithm = conf.isDoubleDQN()
                ? new DoubleDQN(qNetwork, target, algConfig)
                : new StandardDQN(qNetwork, target, algConfig);

        NeuralNetUpdaterConfiguration neuralNetUpdaterConfiguration = NeuralNetUpdaterConfiguration.builder()
                .targetUpdateFrequency(conf.getTargetDqnUpdateFreq())
                .build();
        
        SyncLabelsNeuralNetUpdater updater = new SyncLabelsNeuralNetUpdater(qNetwork, target, neuralNetUpdaterConfiguration);
        UpdateRule<FeaturesLabels, StateActionRewardState<Integer>> updateRule = new UpdateRule<>(updateAlgorithm, updater);

        ReplayMemoryExperienceHandler.Configuration expConfig = ReplayMemoryExperienceHandler.Configuration.builder()
                .maxReplayMemorySize(conf.getExpRepMaxSize())
                .batchSize(conf.getBatchSize())
                .build();

        ReplayMemoryExperienceHandler experienceHandler = new ReplayMemoryExperienceHandler(expConfig, random);
        return LearningBehavior.builder()
                .experienceHandler(experienceHandler)
                .updateRule(updateRule)
                .build();
    }
}
