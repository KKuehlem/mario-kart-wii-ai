package de.minekonst.mariokartwiiai.client;

import de.minekonst.mariokartwiiai.shared.network.Client;
import de.minekonst.mariokartwiiai.shared.network.ClientListener;
import de.minekonst.mariokartwiiai.shared.network.NetworkInitException;
import de.minekonst.mariokartwiiai.shared.network.types.Disconnect;
import de.minekonst.mariokartwiiai.shared.utils.profiler.Profiler;
import de.minekonst.mariokartwiiai.shared.utils.TimeUtils;
import de.minekonst.mariokartwiiai.client.bots.Bot;
import de.minekonst.mariokartwiiai.client.bots.GroundRecordBot;
import de.minekonst.mariokartwiiai.client.bots.RandomDriveBot;
import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import de.minekonst.mariokartwiiai.client.emulator.Connector;
import de.minekonst.mariokartwiiai.client.emulator.ConnectorState;
import de.minekonst.mariokartwiiai.client.gui.WorkerGui;
import de.minekonst.mariokartwiiai.client.recorder.Recorder;
import de.minekonst.mariokartwiiai.main.Constants;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.main.ProgramInstance;
import de.minekonst.mariokartwiiai.server.messages.StatusAnswer;
import de.minekonst.mariokartwiiai.server.messages.StatusCode;
import de.minekonst.mariokartwiiai.server.messages.StatusMessage;
import de.minekonst.mariokartwiiai.server.messages.WelcomeMessage;
import de.minekonst.mariokartwiiai.shared.methods.input.InputMethod;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningMethodMessage;
import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types.MemoryEntry;
import de.minekonst.mariokartwiiai.shared.methods.learning.deepq.types.RewardMessage;
import de.minekonst.mariokartwiiai.shared.tasks.Task;
import de.minekonst.mariokartwiiai.shared.tasks.TaskResponse;
import de.minekonst.mariokartwiiai.tracks.Track;
import java.awt.Frame;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

@Getter
public class Driver extends ProgramInstance implements ClientListener {

    private final Bot[] bots = {new RandomDriveBot(this), new GroundRecordBot(this)};
    private final WorkerGui gui;
    private final double speed;
    private final long startTime;
    private final Client client;
    private volatile Connector connector;
    private volatile String currentMessage = "/";
    private volatile double[] fov;
    private volatile Task task;
    private DriverState state = DriverState.Initialising;
    private Track track = Track.MarioRaceway_N64;
    private boolean ready;
    private int id;
    private int fps;
    private long lastFrameEnd;
    private int frame;
    @Setter private volatile InputMethod intputMethod = InputMethod.DEFAULT[0];
    @Setter private boolean skipRendering;

    public Driver(String host, int port, double speed, boolean optimize) {
        this.speed = speed;
        this.skipRendering = optimize;
        gui = new WorkerGui(this);

        //<editor-fold defaultstate="collapsed" desc="Connect to server">
        try {
            client = new Client(host, port, this);
            Main.log("Connected to server");
        }
        catch (NetworkInitException ex) {
            Main.log("Failed to connect to the server");
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to connect to the remote host: "
                    + ex.toString(),
                    "Error connecting to the server", JOptionPane.ERROR_MESSAGE);
            Main.exit();
            startTime = 0;
            throw new IllegalStateException();
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="GUI">
        gui.setVisible(true);
        if (optimize) {
            SwingUtilities.invokeLater(() -> {
                TimeUtils.sleep(5_000);
                gui.setState(Frame.ICONIFIED);
            });
        }
        //</editor-fold>

        startTime = System.currentTimeMillis();
    }

    @Override
    public void update() {
        // Wait for emulator
        while (connector == null ? true : !connector.isConnected()) {
            TimeUtils.sleep(10);
        }

        while (true) {
            long now = System.currentTimeMillis();
            if (!ready && now - startTime > 4_000) {
                ready = true;
                TimeUtils.sleep(2_000);
                loadTrack(track);
                TimeUtils.sleep(500);
                loadTrack(track);

                TimeUtils.sleep(2_000);

                state = DriverState.Waiting;
            }

            TimeUtils.sleep(50);
        }
    }

    public synchronized ButtonState onNextFrame() {
        if (frame >= 100) {
            fps = (int) (1000.0 / (System.currentTimeMillis() - lastFrameEnd) * 100 + 0.5);
            frame = 0;
            lastFrameEnd = System.currentTimeMillis();
        }
        else {
            frame++;
        }

        Profiler.enter("Next Frame");
        Profiler.enter("FOV");

        ButtonState nextInput = null;
        if (task != null) {
            intputMethod = task.getFovMode();
            state = DriverState.ServerControlled;
        }
        fov = intputMethod.calculate(this);
        Profiler.exit("FOV");

        if (state == DriverState.ServerControlled) {
            //<editor-fold defaultstate="collapsed" desc="Update if controlled by server">
            Profiler.enter("Task update");
            nextInput = task.onNextFrame(this);
            Profiler.exit("Task update");
            Profiler.enter("Task Check finished");
            TaskResponse r = task.checkFinished(this);
            Profiler.exit("Task Check finished");

            if (r != null) {
                Main.log("Task #%d finished with a score of %.2f", task.getTaskID(), task.getScore().getScorePoints());
                task = null;
                state = DriverState.Waiting;
                client.send(r);

                nextInput = new ButtonState(false, false, true, false, new Vector2D(50, 50));
            }
            else {
                currentMessage = task.getScore() != null ? String.format("%.2f", task.getScore().getScorePoints()) : "/";
            }
            //</editor-fold>
        }
        else if (state == DriverState.Waiting || state == DriverState.ClientControlled) {
            //<editor-fold defaultstate="collapsed" desc="Update Bots">
            Profiler.enter("Bots");

            int activeBots = 0;
            for (Bot bot : bots) {
                if (bot.isActive()) {
                    activeBots++;
                    ButtonState bs = bot.update();

                    if (nextInput == null) {
                        nextInput = bs;
                    }
                }
            }

            if (activeBots > 0) {
                currentMessage = activeBots + " active Bots";
                state = DriverState.ClientControlled;
            }
            else {
                currentMessage = "/";
                state = DriverState.Waiting;
            }

            Profiler.exit("Bots");
            //</editor-fold>
        }

        Recorder.update(this);

        //<editor-fold defaultstate="collapsed" desc="Update Text in Emulator">
        if (task != null ? task.getScore() != null : false) {
//            connector.setTitle(String.format("=== Driver #%d ===", id),
//                    String.format("Speed: %.2f", connector.getAverageSpeed()),
//                    String.format("Score: %.2f", task.getScore().getScorePoints()),
//                    task.getScore().toString(), task.getAdditionalTitle());

            connector.setTitle(String.format("=== Driver #%d ===", id),
                    String.format("Score: %.2f", task.getScore().getScorePoints()),
                    task.getAdditionalTitle());
        }
        else {
            connector.setTitle(String.format("=== Driver #%d ===", id),
                    String.format("Speed: %.2f", connector.getAverageSpeed()));
        }
        //</editor-fold>

        Profiler.exit("Next Frame");
        Profiler.getRootNode("Next Frame").nextIteration();

        return nextInput;
    }

    @Override
    public void onExit() {
        if (connector != null) {
            connector.closeEmulator();
        }

        if (client != null) {
            client.disconnect();
        }
    }

    public void loadTrack(Track track) {
        loadTrack(track, Constants.DEFAULT_LOAD_WITH_COUNTDOWN);
    }

    public void loadTrack(Track track, boolean countdown) {
        if (!ready) {
            return;
        }

        connector.load(track.getLoadState(countdown));
        this.track = track;
        Main.log("Track \"%s\" loaded", track.toString());
    }

    public synchronized void cancelTask() {
        if (task != null) {
            state = DriverState.Waiting;
            task = null;
            client.send(new StatusMessage(StatusCode.TASK_DENIED));
        }
    }

    @Override
    public void onMessage(Object o) {
        if (o instanceof WelcomeMessage welcomeMessage) {
            this.id = welcomeMessage.getId();
            gui.setTitle("Mario Kart Wii AI - Driver #" + id);
            connector = new Connector(this, speed);
        }
        else if (o instanceof StatusMessage statusMessage) {
            StatusCode code = statusMessage.getCode();
            ConnectorState s = connector.getState();
            switch (code) {
                case STATUS_REQUESTED ->
                    client.send(new StatusAnswer(state, currentMessage, fps,
                            s != null ? s.getPosition() : Vector3D.ZERO, s != null ? s.getForward() : Vector2D.ZERO,
                            track != null ? track.ordinal() : -1,
                            task != null ? task.getDrivingSince() : null));
                default ->
                    Main.log("Recived unexpected code: %s", code.toString());
            }
        }
        else if (o instanceof Task) {
            if (state != DriverState.Waiting) {
                Main.log("Recieved Task in state %s, sending it back", state.toString());
                client.send(new StatusMessage(StatusCode.TASK_DENIED));
                return;
            }
            recieveTask((Task) o);
        }
    }

    @Override
    public void onDisconnect(Disconnect reason) {
        Main.log("Server disconnected: %s. Program will exit", reason.toString());
        Main.exit();
    }

    public synchronized void recieveTask(Task task) {
        Main.log("Recived Task from Server (TaskID #%d)", task.getTaskID());
        boolean extraWait = task.getTrack() != track;
        loadTrack(task.getTrack());

        for (Bot b : bots) {
            b.setActive(false);
        }
        gui.updateBotTable();
        state = DriverState.ServerControlled;
        this.task = task;
    }
}
