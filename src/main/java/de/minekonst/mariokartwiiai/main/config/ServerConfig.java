package de.minekonst.mariokartwiiai.main.config;

import de.minekonst.mariokartwiiai.main.ProgramInstance;
import de.minekonst.mariokartwiiai.server.AIServer;


public class ServerConfig extends Config {
    
    private final int port;

    public ServerConfig(int port) {
        this.port = port;
    }
    
    @Override
    public ProgramInstance create() {
        return new AIServer(port);
    }

    @Override
    public boolean isServer() {
        return true;
    }
    
}
