package de.minekonst.mariokartwiiai.client.emulator;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.minekonst.mariokartwiiai.shared.utils.FileUtils;
import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.main.Constants;
import de.minekonst.mariokartwiiai.main.Main;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

public class SocketConnector {

    private final String inputFile;
    private final Driver driver;
    private int count;
    private Socket connection;
    private DataInputStream in;
    private DataOutputStream out;
    private ServerSocket server;
    private volatile int inputFrame;
    private volatile int lastInputFrame;

    public SocketConnector(Driver driver) {
        this.driver = driver;
        (new File(Constants.AI_DIR)).mkdir();
        this.inputFile = String.format(Constants.INPUT_FILE, driver.getId());
    }

    public void init(ButtonState nextInput, String load, String[] title, boolean skipRendering) {
        FileUtils.writeFile(inputFile, generateInput(title));
        try {
            server = new ServerSocket(7850 + driver.getId());

            Main.log("Waiting for connecstion");
            connection = server.accept();
            Main.log("Connected to Emulator");

            in = new DataInputStream(connection.getInputStream());
            out = new DataOutputStream(connection.getOutputStream());

            int r = in.readInt();
            if (r != driver.getId()) {
                Main.log("Expected ID = %,d got %,d", driver.getId(), r);
                Main.exit();
            }
            Main.log("Emulator responded");
        }
        catch (IOException ex) {
            Main.log("Cannot connect with Emulator: %s", ex.toString());
            Main.exit();
        }
    }

    public void onNextFrame(String load, String[] title, boolean skipRendering, Function<ConnectorState, ButtonState> onFrame) {
        FileUtils.writeFile(inputFile, generateInput(title));
        try {
            out.writeInt(count); // Send count
            out.flush();

            inputFrame = in.readInt();
            if (lastInputFrame != inputFrame) { // Input Frame
                out.writeInt(1); // Request input
                ButtonState nextInput = onFrame.apply(getInput());
                //<editor-fold defaultstate="collapsed" desc="Send Output">
                out.writeInt(nextInput == null ? 0 : (nextInput.isA() ? 1 : 2));
                out.writeInt(nextInput == null ? 0 : (nextInput.isB() ? 1 : 2));
                out.writeInt(nextInput == null ? 0 : (nextInput.isTrick() ? 1 : 2));
                out.writeInt(nextInput == null ? 0 : (nextInput.isStart() ? 1 : 2));
                out.writeInt(nextInput == null ? -1 : (nextInput.getMainStickX()));
                out.writeInt(nextInput == null ? -1 : (nextInput.getMainStickY()));
                out.writeInt(skipRendering ? 1 : 0);
                sendString(load != null ? load : "/");
                out.flush();
                //</editor-fold>
            }
            else { // Other Frame
                out.writeInt(0); // Input comming
                sendString(load != null ? load : "/");
            }

            //<editor-fold defaultstate="collapsed" desc="Check mirror count">
            int c = in.readInt();
            if (c != count) { // Emulator mirrors last count
                Main.log("Unexpected count. Expected %,d  but got %,d", count, c);
                Main.exit();
            }
            //</editor-fold>

            lastInputFrame = inputFrame;
            count++;
            if (count > 10_000) {
                count = 0;
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
            Main.exit();
        }
    }

    private ConnectorState getInput() {
        try {
            return new ConnectorState(new Vector3D(in.readDouble(), in.readDouble(), in.readDouble()), new Vector3D(in.readDouble(), in.readDouble(), in.readDouble()),
                    in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                    in.readInt() == 1, in.readInt() == 1, in.readInt() == 1, in.readInt(), in.readInt(),
                    in.readInt(), in.readInt(), driver.getTrack());
        }
        catch (IOException ex) {
            Main.log("Cannot read: %s", ex.toString());
            Main.exit();
            return null;
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
            if (server != null) {
                server.close();
            }
        }
        catch (IOException ex) {

        }
    }

    private void sendString(String str) throws IOException {
        byte[] arr = str.getBytes();
        out.writeInt(arr.length);
        out.write(arr);
    }

    private String generateInput(String[] title) {
        StringBuilder s = new StringBuilder(100);

        if (title != null) {
            for (String str : title) {
                s.append(str).append("\n");
            }
        }

        return s.toString();
    }

}
