package de.minekonst.mariokartwiiai.shared.methods.learning;

import de.minekonst.mariokartwiiai.shared.utils.editortable.EditorValue;
import de.minekonst.mariokartwiiai.shared.utils.editortable.Validator;

import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.shared.methods.input.InputMethod;
import de.minekonst.mariokartwiiai.shared.tasks.LearningTask;
import de.minekonst.mariokartwiiai.shared.tasks.TaskResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

@SuppressWarnings("serial")
public class MutationalLearning extends LearningMethod<MultiLayerNetwork> {

    private transient EditorValue<Integer> weightBase;
    private transient EditorValue<Integer> biasBase;

    public MutationalLearning(InputMethod inputMethod) {
        super(inputMethod);
    }

    @Override
    protected void init() {
        weightBase = new EditorValue<>("Weight Base", 1_500, Integer.class, "Lower value => More mutations (for weights)", Validator.NON_NEG);
        biasBase = new EditorValue<>("Bias Base", 4_500, Integer.class, "Lower value => More mutations (for biases)", Validator.NON_NEG);
    }

    @Override
    public List<EditorValue<?>> getEditorValues() {
        return List.of(weightBase, biasBase);
    }

    @Override
    public List<LearningTask<LearningMethod<MultiLayerNetwork>, MultiLayerNetwork>> nextGeneration(List<TaskResponse> responses, boolean sucess, int newTasks) {
        List<MultiLayerNetwork> nets = super.rankNetworks(responses);

        if (sucess) {
            return mutation(nets.get(0), newTasks);
        }
        else if (ai.getProperties().getDataHolder().getBaseNetwork() != null) {
            return mutation(ai.getProperties().getDataHolder().getBaseNetwork(), newTasks);
        }
        else {
            return randomTasks(newTasks);
        }
    }

    private List<LearningTask<LearningMethod<MultiLayerNetwork>, MultiLayerNetwork>> mutation(MultiLayerNetwork base, int newTasks) {
        List<LearningTask<LearningMethod<MultiLayerNetwork>, MultiLayerNetwork>> list = new ArrayList<>();

        for (int x = 0; x < newTasks; x++) {
            MultiLayerNetwork net = base.clone();
            mutate(net, weightBase.getValue(), biasBase.getValue());
            list.add(super.createTask(net, "Mutaional"));
        }

        Main.log("Created %d Tasks", ai.getProperties().getGenomesPerGeneration().getValue());
        return list;
    }

    private List<LearningTask<LearningMethod<MultiLayerNetwork>, MultiLayerNetwork>> randomTasks(int newTasks) {
        List<LearningTask<LearningMethod<MultiLayerNetwork>, MultiLayerNetwork>> list = new ArrayList<>();
        for (int x = 0; x < newTasks; x++) {
            MultiLayerNetwork net = buildNetwork();
            for (int it = 0; it < 10; it++) {
                mutate(net, 800, 1_000);
            }
            list.add(super.createTask(net, "Mutaional / Random"));
        }

        Main.log("Created %d Tasks §0(All random)", ai.getProperties().getGenomesPerGeneration().getValue());
        return list;
    }

    @Override
    public MultiLayerNetwork cloneNetwork(MultiLayerNetwork network) {
        return network.clone();
    }

    @Override
    public LearningObserver<MultiLayerNetwork> createObserver() {
        return new GenericLearningObserver(this, inputMethod);
    }

    @Override
    public void writeNetwork(MultiLayerNetwork network, OutputStream stream) throws IOException {
        ModelSerializer.writeModel(network, stream, false);
    }

    @Override
    public MultiLayerNetwork loadNetwork(InputStream stream) throws IOException {
        return ModelSerializer.restoreMultiLayerNetwork(stream);
    }

    private MultiLayerNetwork buildNetwork() {
        int[] layers = ai.getProperties().getDataHolder().getLayers();

        NeuralNetConfiguration.ListBuilder list = new NeuralNetConfiguration.Builder()
                .activation(Activation.TANH)
                .weightInit(WeightInit.XAVIER)
                .l2(0.0001)
                .list();

        for (int x = 0; x < layers.length - 1; x++) {
            list.layer(x, new DenseLayer.Builder().nIn(layers[x]).nOut(layers[x + 1]).build());
        }
        int l = layers.length - 1;
        list.layer(l, new OutputLayer.Builder()
                .activation(Activation.TANH)
                .lossFunction(LossFunction.SQUARED_LOSS)
                .nIn(layers[l - 1]).nOut(layers[l]).build());

        MultiLayerNetwork net = new MultiLayerNetwork(list.build());
        net.init();
        return net;
    }

    //<editor-fold defaultstate="collapsed" desc="Mutating">
    private void mutate(MultiLayerNetwork net, double weights, double biases) {
        int layers = ai.getProperties().getDataHolder().getLayers().length;
        for (int x = 0; x < layers; x++) {
            net.setParam(x + "_W", mutate(net.getParam(x + "_W"), weights));
            net.setParam(x + "_b", mutate(net.getParam(x + "_b"), biases));
        }
    }

    private static INDArray mutate(INDArray array, double base) {
        double[][] mat = array.toDoubleMatrix();
        for (double[] arr : mat) {
            for (int y = 0; y < arr.length; y++) {
                arr[y] = mutate(arr[y], base);
            }
        }
        return Nd4j.create(mat);
    }

    private static double mutate(double old, double base) {
        double r = Math.random() * base;
        double weight = old;

        if (r <= 2) {
            weight *= -1;
        }
        else if (r <= 4) {
            weight = Math.random() - 0.5;
        }
        else if (r <= 6) {
            weight *= Math.random() + 1;
        }
        else if (r <= 8) {
            weight *= Math.random();
        }
        else if (r <= 10) {
            weight += Math.random() - 0.5;
        }

        return weight;
    }
    //</editor-fold>

}
