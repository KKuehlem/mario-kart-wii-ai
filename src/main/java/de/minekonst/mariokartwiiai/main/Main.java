package de.minekonst.mariokartwiiai.main;

import de.minekonst.mariokartwiiai.main.config.ClientConfig;
import de.minekonst.mariokartwiiai.main.config.Config;
import de.minekonst.mariokartwiiai.main.config.ServerConfig;
import de.minekonst.mariokartwiiai.server.gui.dialogs.Launcher;
import de.minekonst.mariokartwiiai.shared.utils.GlobalKeyListener;
import de.minekonst.mariokartwiiai.shared.utils.commandline.CommandLineFlag;
import de.minekonst.mariokartwiiai.shared.utils.commandline.CommandLineParameter;
import de.minekonst.mariokartwiiai.shared.utils.commandline.CommandLineSystem;
import java.io.File;
import java.util.logging.Logger;
import lombok.Getter;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class Main {

    @Getter private static final Logger logger = Logger.getGlobal();
    @Getter private static final String dataDir = findDataDir();
    @Getter private static final String projectDir = new File(dataDir + File.separator + "..").getAbsolutePath();
    @Getter private static boolean server = false;
    @Getter private static boolean useWine = false;
    @Getter private static ProgramInstance instance;
    @Getter private static GlobalKeyListener keyListener;
    
    public static void main(String[] args) {
        CommandLineSystem cls = new CommandLineSystem(logger);
        CommandLineFlag server = new CommandLineFlag("server", "s", "Set this flag to skip the launcher and start this program as a server. Cannot be combiened with the --remote flag");
        CommandLineFlag optimize = new CommandLineFlag("optimize", "o", "Minimize the window on startup and disable rendering by default. Only for client");
        CommandLineFlag wine = new CommandLineFlag("wine", "w", "Use wine to start Dolphin");
        cls.addFlags(server, optimize, wine);
        CommandLineParameter<String> remote = new CommandLineParameter<>("remote", "r", "Set this flag to skip the launcher and start this program as a client and connect to the server. Cannot be combiened with the --server flag", String.class);
        CommandLineParameter<Integer> port = new CommandLineParameter<>("port", "p", "The port to start the server / to connect to. If not set, using default port " + Constants.DEFAULT_PORT, Integer.class);
        CommandLineParameter<Integer> n = new CommandLineParameter<>("n", "n", "The amount of times this configuration should launch. Only for client", Integer.class);
        CommandLineParameter<Double> speed = new CommandLineParameter<>("speed", "f", "The speed of the Emulator. Default: " + String.format("%.1f", Constants.DEFAULT_EMULATOR_SPEED), Double.class);
        cls.addParamter(remote, port, speed, n);
        if (!cls.scan(args)) {
            return;
        }

        initDL4J();

        useWine = wine.isSet();

        Config config = null;
        if (server.isSet()) {
            //<editor-fold defaultstate="collapsed" desc="Server">
            if (remote.getValue() != null) {
                logger.severe("--server and --remote can not be bouth present");
                return;
            }
            if (speed.getValue() != null) {
                logger.severe("Cannot take --speed for a server");
                return;
            }

            config = new ServerConfig(port.getValue() != null ? port.getValue() : Constants.DEFAULT_PORT);
            //</editor-fold>
        }
        else if (remote.getValue() != null) {
            //<editor-fold defaultstate="collapsed" desc="Client">
            int nv = 1;
            if (n.getValue() != null) {
                if (n.getValue() < 1) {
                    logger.severe("--n must be greater than 0");
                    return;
                }
                else {
                    nv = n.getValue();
                }
            }

            config = new ClientConfig(
                    remote.getValue(),
                    port.getValue() != null ? port.getValue() : Constants.DEFAULT_PORT,
                    speed.getValue() != null ? speed.getValue() : Constants.DEFAULT_EMULATOR_SPEED,
                    nv, optimize.isSet()
            );
            //</editor-fold>
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            exit(false);
        }));
        Thread.setDefaultUncaughtExceptionHandler(Main::onError);

        if (config == null) {
            Launcher launcher = new Launcher();
            config = launcher.askForInstance();
        }

        //<editor-fold defaultstate="collapsed" desc="Key Listener">
        try {
            if (!config.isServer()) {
                java.util.logging.Logger l = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
                l.setLevel(java.util.logging.Level.WARNING);
                l.setUseParentHandlers(false);

                GlobalScreen.registerNativeHook();
                keyListener = new GlobalKeyListener();
                GlobalScreen.addNativeKeyListener(keyListener);
            }
        }
        catch (NativeHookException | UnsatisfiedLinkError ex) {
            
        }
        //</editor-fold>

        if (config != null) {
            Main.server = config.isServer();
            instance = config.create();
            instance.update();
        }
        else {
            exit();
        }
    }
    
    public static void log(String msg, Object... format) {
        logger.info(String.format(msg, format));
    }

    public static void exit() {
        exit(true);
    }

    private static void initDL4J() {
        int[] layers = {2, 2, 2};

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
                .lossFunction(LossFunctions.LossFunction.SQUARED_LOSS)
                .nIn(layers[l - 1]).nOut(layers[l]).build());

        MultiLayerNetwork net = new MultiLayerNetwork(list.build());
        net.init();
    }

    private static void exit(boolean shutdown) {
        if (instance != null) {
            instance.onExit();
        }

        if (shutdown) {
            System.exit(0);
        }
    }

    private static void onError(Thread thread, Throwable ex) {
        logger.severe(String.format("Exception \"%s\" thrown in Thread \"%s\" could not be caught",
                ex.toString(), thread.toString()));
        ex.printStackTrace();
        logger.severe("The programm will exit, because of this error");

        exit();
    }

    private static String findDataDir() {
        String[] possible = {"", ".." + File.separator};
        for (String p : possible) {
            File f = new File(p + "Data");
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }

        throw new ExceptionInInitializerError("Cannot find data dir");
    }

}
