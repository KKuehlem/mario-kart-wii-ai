package de.minekonst.mariokartwiiai.server;


import de.minekonst.mariokartwiiai.shared.network.NetworkInitException;
import de.minekonst.mariokartwiiai.shared.network.Server;
import de.minekonst.mariokartwiiai.shared.network.ServerClient;
import de.minekonst.mariokartwiiai.shared.network.ServerListener;
import de.minekonst.mariokartwiiai.shared.network.types.Disconnect;
import de.minekonst.mariokartwiiai.shared.utils.TimeUtils;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.main.ProgramInstance;
import de.minekonst.mariokartwiiai.server.ai.AI;
import de.minekonst.mariokartwiiai.server.gui.ServerGui;
import de.minekonst.mariokartwiiai.server.messages.StatusAnswer;
import de.minekonst.mariokartwiiai.server.messages.StatusCode;
import de.minekonst.mariokartwiiai.server.messages.StatusMessage;
import de.minekonst.mariokartwiiai.server.messages.WelcomeMessage;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningMethodMessage;
import de.minekonst.mariokartwiiai.shared.tasks.TaskResponse;
import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JOptionPane;

public class AIServer extends ProgramInstance implements ServerListener {
    
    private static final int STATUS_REQUEST_INTERVALL_MS = 100;

    private static AIServer instance;

    private final List<RemoteDriver> drivers = new CopyOnWriteArrayList<>();
    private final ServerGui gui;
    private final Server server;
    private volatile AI<?, ?> ai;
    private volatile boolean aiActive;

    public AIServer(int port) {
        instance = this;
        //<editor-fold defaultstate="collapsed" desc="Start Server">
        Server s;
        try {
            s = new Server(port, this);
            Main.log("Server listening on port %d", port);
        }
        catch (NetworkInitException ex) {
            Main.log("Failed to start server");
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to start the server: "
                    + ex.toString(),
                    "Error starting server", JOptionPane.ERROR_MESSAGE);

            gui = null;
            server = null;
            Main.exit();
            return;
        }
        server = s;
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="GUI / Telegram">
        Telegram.init(this);
        Telegram.broadcast("Mario Kart Wii AI Server started");

        gui = new ServerGui(this);
        gui.setTitle("Mario Kart Wii AI - Server");
        gui.setVisible(true);
        //</editor-fold>
    }

    @Override
    public void update() {
        while (true) {
            //<editor-fold defaultstate="collapsed" desc="Status of RemoteDrivers">
            long now = System.currentTimeMillis();
            server.forEachConnectedClient((ServerClient sc) -> {
                if (sc.getUserObject() != null) {
                    RemoteDriver r = (RemoteDriver) sc.getUserObject();
                    if (now - r.getLastStatusRequest() > STATUS_REQUEST_INTERVALL_MS) {
                        sc.send(new StatusMessage(StatusCode.STATUS_REQUESTED));
                    }
                }
            });
            //</editor-fold>

            if (aiActive) {
                ai.update();
            }

            TimeUtils.sleep(5);
        }
    }

    @Override
    public void onExit() {
        if (server != null) {
            server.close();
        }

        TimeUtils.sleep(500);
        //<editor-fold defaultstate="collapsed" desc="Clean Up files">
        File dir = new File(Main.getDataDir() + File.separator + ".."
                + File.separator + "Dolphin" + File.separator + "AI");

        Main.log("Cleaning up AI dir");
        for (File f : dir.listFiles()) {
            f.delete();
        }
        (new File(Main.getProjectDir() + File.separator + "Dolphin" + File.separator + "User"
                + File.separator + "Wii" + File.separator + "shared2" + File.separator
                + "sys" + File.separator + "SYSCONF")).delete();
        //</editor-fold>
    }

    @Override
    public void onMessage(ServerClient sc, Object o) {
        long now = System.currentTimeMillis();

        RemoteDriver s = (RemoteDriver) sc.getUserObject();

        if (o instanceof StatusAnswer) {
            StatusAnswer ans = (StatusAnswer) o;
            s.setPing((int) (now - s.getLastStatusRequest())); // Depracted since throtteled

            s.setMessage(ans.getMessage());
            s.setState(ans.getState());
            s.setFps(ans.getFps());
            s.setStatus(ans);
            s.setLastStatusRequest(now);
        }
        else if (o instanceof StatusMessage) {
            StatusCode code = ((StatusMessage) o).getCode();
            switch (code) {
                case TASK_DENIED -> {
                    if (ai != null) {
                        ai.takeBackTasksFor(s, false);
                    }
                }
                default ->
                    Main.log("Recived unexpected code: %s (Client #%d)",
                            code.toString(), sc.getID());
            }
        }
        else if (o instanceof TaskResponse) {
            TaskSupplier.onReponse((TaskResponse) o);
        }
        else if (o instanceof LearningMethodMessage l && ai != null) {
            ai.getLearningMethod().onMessage(l.getObject());
        }
    }

    @Override
    public void onClientConnected(ServerClient sc) {
        Main.log("Client connected: ID %d", sc.getID());
        RemoteDriver r = new RemoteDriver(sc);
        sc.setUserObject(r);
        drivers.add(r);
        sc.send(new WelcomeMessage(sc.getID()));
        sc.send(new StatusMessage(StatusCode.STATUS_REQUESTED));
    }

    @Override
    public void onClientDisconnected(ServerClient sc, Disconnect reason) {
        Main.log("Client #%d disconnected: %s", sc.getID(), reason.toString());
        drivers.removeIf((RemoteDriver d) -> {
            return d.getServerClient() == sc;
        });
        if (ai != null) {
            ai.takeBackTasksFor((RemoteDriver) sc.getUserObject(), true);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Getter / Setter">
    public AI<?, ?> getAI() {
        return ai;
    }

    public boolean isAIActive() {
        return aiActive;
    }

    public void setAIActive(boolean b) {
        this.aiActive = b;
    }

    public List<RemoteDriver> getRemoteDrivers() {
        return drivers;
    }

    public static AIServer getInstance() {
        return instance;
    }

    public ServerGui getGui() {
        return gui;
    }

    public void setAI(AI ai, Component parent) {
        if (aiActive) {
            JOptionPane.showMessageDialog(null,
                    "Cannot change AI while active",
                    "Error cahnging AI", JOptionPane.ERROR_MESSAGE);
        }
        else {
            this.ai = ai;
            if (ai != null) {
                ai.onLoad();
            }
        }
    }
    //</editor-fold>

}
