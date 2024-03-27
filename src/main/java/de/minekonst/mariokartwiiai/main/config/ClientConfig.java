package de.minekonst.mariokartwiiai.main.config;

import de.minekonst.mariokartwiiai.client.Driver;
import java.io.File;
import java.io.IOException;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.main.ProgramInstance;

public class ClientConfig extends Config {

    private final String host;
    private final boolean optimize;
    private final double speed;
    private final int port;
    private final int n;

    public ClientConfig(String host, int port, double speed, int n, boolean optimize) {
        this.host = host;
        this.port = port;
        this.speed = speed;
        this.n = n;
        this.optimize = optimize;
    }

    @Override
    public ProgramInstance create() {
        for (int x = 0; x < n - 1; x++) {
            String m = optimize ? "--optimize" : "";
            
            ProcessBuilder pb = new ProcessBuilder("java", "--illegal-access=permit", "-jar", "Mario_Kart_Wii_AI.jar",
                    "--remote", host, "--port", "" + port, "--speed", "" + speed, m);
            pb.directory(new File(Main.getProjectDir() + File.separator + "dist"));
            try {
                pb.start();
            }
            catch (IOException ex) {
                Main.log("Cannot start other clients: %s", ex.toString());
                break;
            }
        }

        return new Driver(host, port, speed, optimize);
    }

    @Override
    public boolean isServer() {
        return false;
    }

}
