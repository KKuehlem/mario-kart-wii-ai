package de.minekonst.mariokartwiiai.client.emulator;

import de.minekonst.mariokartwiiai.shared.utils.TimeUtils;
import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.main.Constants;
import de.minekonst.mariokartwiiai.main.Main;
import java.io.File;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

@Getter
public class Connector {

    private final Driver driver;
    private final double speed;
    private final SocketConnector connector;
    private Process emulator;
    @Setter private int sleep;

    // Synchronized 
    private volatile ConnectorState state;
    private volatile ButtonState nextInput;
    private boolean shutdown;
    private volatile String load;
    private volatile String[] title;
    private volatile double distance;
    private volatile int frame;
    private volatile Vector3D lastPos;
    private volatile boolean connected;

    public Connector(Driver driver, double speed) {
        this.driver = driver;
        this.speed = speed;
        this.connector = new SocketConnector(driver);

        (new Thread(() -> {
            run();
        }, "Connector Thread")).start();
    }

    public void load(String loadState) {
        load = loadState;
        frame = 0;
        distance = 0;
        lastPos = null;
    }

    public void closeEmulator() {
        if (emulator == null ? true : !emulator.isAlive()) {
            connector.close();
            return;
        }

        // Ensure emulator has no open file to clean up directory
        shutdown = true;
        TimeUtils.sleep(100);

        try {
            emulator.destroyForcibly();
            Main.log("Emulator closed");
        }
        catch (Exception ex) {
            Main.log("Cannot close emulator: %s", ex.toString());
        }

        TimeUtils.sleep(500);

        connector.close();
    }

    private void run() {
        startEmulator();
        connector.init(nextInput, load, title, driver.isSkipRendering());
        connected = true;

        TimeUtils.sleep(2_000);
        while (!shutdown) {
            String l = load;
            load = null;
            connector.onNextFrame(l, title, driver.isSkipRendering(),
                    (ConnectorState state) -> {
                        this.state = state;

                        /*
                         * Ensure all tasks start at same frame of input. Frame
                         * > 10 because of loading
                         */
                        if (frame > 10 && state.getFrame() >= 200) {
                            nextInput = driver.onNextFrame();
                            //<editor-fold defaultstate="collapsed" desc="Calc Distance">
                            Vector3D pos = state.getPosition();
                            if (lastPos != null) {
                                distance += pos.distance(lastPos);
                            }
                            lastPos = pos;
                            //</editor-fold>
                        }
                        else {
                            nextInput = null;
                        }

                        frame++;

                        return nextInput;
                    });

            if (sleep > 0) {
                TimeUtils.sleep(sleep);
            }
        }
    }

    private void startEmulator() {
        //<editor-fold defaultstate="collapsed" desc="Dolphin Watcher Thread">
        (new Thread(() -> {
            Main.log("Starting Dolphin with: %s", Constants.DOLPHIN_PATH + " --exec=" + Constants.ISO_NAME + " --id=" + driver.getId() + " --speed=" + speed);
            ProcessBuilder pb;
            if (Main.isUseWine()) {
                pb = new ProcessBuilder("wine", Constants.DOLPHIN_PATH, "--exec=" + Constants.ISO_NAME,
                        "--id=" + driver.getId(), "--speed=" + speed);
            }
            else {
                pb = new ProcessBuilder(Constants.DOLPHIN_PATH, "--exec=" + Constants.ISO_NAME,
                        "--id=" + driver.getId(), "--speed=" + speed);
            }
            try {
                pb.directory(new File(Main.getProjectDir() + File.separator + "Dolphin"));
                emulator = pb.start();

                while (emulator.isAlive()) { // Wait for Emulator. Avoid using emulator.waitFor() to let thread sleep
                    TimeUtils.sleep(1_000); // 1 s
                }
                int r = emulator.exitValue();
                Main.log("Emulator exit with code %d", r);

                TimeUtils.sleep(1_000);
                Main.exit();
            }
            catch (IOException ex) {
                Main.log("Cannot start emulator: %s (Stacktrace in console)", ex.toString());
                ex.printStackTrace();

                Main.log("Program will exit, because the emulator cannot be started");
                Main.exit();
            }
        }, "Dolphin Watcher")).start();
        //</editor-fold>
    }

    //<editor-fold defaultstate="collapsed" desc="Getter / Setter">
    public double getAverageSpeed() {
        return frame > 5 * 60 ? 100 * distance / frame : 0;
    }

    public void setTitle(String... title) {
        this.title = title;
    }
    //</editor-fold>
}
