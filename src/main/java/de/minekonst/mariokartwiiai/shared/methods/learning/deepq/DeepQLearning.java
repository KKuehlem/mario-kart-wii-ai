package de.minekonst.mariokartwiiai.shared.methods.learning.deepq;

import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types.Action;
import de.minekonst.mariokartwiiai.shared.utils.editortable.EditorValue;
import de.minekonst.mariokartwiiai.shared.utils.editortable.Validator;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.shared.methods.input.InputMethod;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningMethod;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningObserver;
import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types.RewardMessage;
import de.minekonst.mariokartwiiai.shared.tasks.LearningTask;
import de.minekonst.mariokartwiiai.shared.tasks.TaskResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.rl4j.network.dqn.DQN;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

@SuppressWarnings("deprecation")
@Getter
public class DeepQLearning extends LearningMethod<DQN> {

    private transient EditorValue<Double> dropout;
    private transient EditorValue<Integer> episodesPerGenome;
    private transient EditorValue<Double> experimentation;
    private transient EditorValue<Integer> inputMethodChange;
    private transient EditorValue<Integer> inputIntervall;
    private transient EditorValue<Integer> memorySteps;
    private transient DeepQLearner learner;
    private transient EditorValue<Integer> memoryIntervall;
    private transient EditorValue<Double> qThreshold;
    private transient EditorValue<Double> keepExperience;

    public DeepQLearning(InputMethod inputMethod) {
        super(inputMethod);
    }

    @Override
    protected void init() {
        dropout = new EditorValue<>("Dropout", 0.9, Double.class, "Retain value", Validator.NON_NEG, Validator.lessOrEqual(1));
        experimentation = new EditorValue<>("Experimentation", 0.2, Double.class, "How likly is the agent to experiment", Validator.NON_NEG, Validator.lessOrEqual(1));
        inputMethodChange = new EditorValue<>("Input Method Change", 60, Integer.class, "How long does the input method when decided randomly using 'Experimentation'? (In frames)", Validator.NON_NEG);
        inputIntervall = new EditorValue<>("Input Intervall", 5, Integer.class, "Get a new input every n frames", Validator.greaterOrEqual(1));
        episodesPerGenome = new EditorValue<>("Episodes per genome", 4, Integer.class, "Episodes every genome takes", Validator.NON_NEG);
        memorySteps = new EditorValue<>("Memory Steps", 8, Integer.class, "Memory buffer has a size of this steps * intervall", Validator.NON_NEG);
        memoryIntervall = new EditorValue<>("Memory Intervall", 5, Integer.class, "Sample every n frames the memory", Validator.NON_NEG);
        qThreshold = new EditorValue<>("Q threshold", 0.9, Double.class, "If q value is higher, always use network", Validator.NON_NEG);
        keepExperience = new EditorValue<>("Keep Experience", 0.5, Double.class, "How likely to keep a single experience", Validator.NON_NEG, Validator.lessOrEqual(1));

        DQN base = ai.getProperties().getDataHolder().getBaseNetwork();
        learner = new DeepQLearner(base != null ? base : buildNetwork(), this);
    }

    @Override
    public List<EditorValue<?>> getEditorValues() {
        return List.of(dropout, experimentation, inputMethodChange, inputIntervall, episodesPerGenome, memorySteps, memoryIntervall, qThreshold, keepExperience);
    }

    @Override
    public List<LearningTask<LearningMethod<DQN>, DQN>> nextGeneration(List<TaskResponse> responses, boolean sucess, int newTasks) {
        return tasksOf(learner.getCurrentNetwork(), newTasks);
    }

    @Override
    public void onMessage(Serializable object) {
        learner.onMessage((RewardMessage) object);
    }

    @Override
    public TaskResponse getBest(List<TaskResponse> responses) {
        TaskResponse eval = responses.stream().filter(r -> r.getType().equals("Eval")).findAny().get();
        return Objects.requireNonNull(eval);
    }

    @Override
    public LearningTask<LearningMethod<DQN>, DQN> evaluationTask(List<TaskResponse> responses) {
        return new LearningTask<>(
                createObserver(), learner.getCurrentNetwork(),
                ai.getProperties().getTrack().getValue().ordinal(), "Eval",
                ai.getProperties(), ai.getBestTime(), false, null, 0, true);
    }

    @Override
    public DQN cloneNetwork(DQN network) {
        return network.clone();
    }

    @Override
    public LearningObserver<DQN> createObserver() {
        return new DeepQLearningObserver(this, inputMethod,
                experimentation.getValue(), inputMethodChange.getValue(), inputIntervall.getValue(),
                memorySteps.getValue(), memoryIntervall.getValue(), qThreshold.getValue());
    }

    @Override
    public void writeNetwork(DQN network, OutputStream stream) throws IOException {
        network.save(stream);
    }

    @Override
    public DQN loadNetwork(InputStream stream) throws IOException {
        MultiLayerNetwork mln = ModelSerializer.restoreMultiLayerNetwork(stream);
        return new DQN(mln);
    }

    private DQN buildNetwork() {
        int[] layers = ai.getProperties().getDataHolder().getLayers();
        layers[layers.length - 1] = Action.POSSIBLILITIES;

        NeuralNetConfiguration.ListBuilder list = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam.Builder()
                        .learningRate(0.001)
                        .build())
                .activation(Activation.TANH)
                .weightInit(WeightInit.XAVIER)
                .dropOut(dropout.getValue())
                .l2(0.0001)
                .list();

        list.backpropType(BackpropType.Standard);

        for (int x = 0; x < layers.length - 1; x++) {
            list.layer(x, new DenseLayer.Builder().nIn(layers[x]).nOut(layers[x + 1]).build());
        }
        int l = layers.length - 1;
        list.layer(l, new OutputLayer.Builder()
                .activation(Activation.TANH)
                .lossFunction(LossFunctions.LossFunction.HINGE)
                .nIn(layers[l - 1]).nOut(layers[l]).build());

        MultiLayerNetwork net = new MultiLayerNetwork(list.build());
        net.init();
        return new DQN(net);
    }

    private List<LearningTask<LearningMethod<DQN>, DQN>> tasksOf(DQN baseNetwork, int newTasks) {
        List<LearningTask<LearningMethod<DQN>, DQN>> list = new ArrayList<>();
        for (int x = 0; x < newTasks; x++) {
            list.add(super.createTask(baseNetwork, "DQN", episodesPerGenome.getValue(), false));
        }

        Main.log("Created %d Tasks §0(Based on last best)", ai.getProperties().getGenomesPerGeneration().getValue());
        return list;
    }
}
