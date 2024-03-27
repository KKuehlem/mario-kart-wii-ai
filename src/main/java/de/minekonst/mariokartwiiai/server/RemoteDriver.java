package de.minekonst.mariokartwiiai.server;

import de.minekonst.mariokartwiiai.shared.network.ServerClient;
import de.minekonst.mariokartwiiai.client.DriverState;
import de.minekonst.mariokartwiiai.server.messages.StatusAnswer;

public class RemoteDriver {

    private final ServerClient sc;
    private String message;
    private boolean usedForAI;
    private long lastStatusRequest;
    private int ping;
    private DriverState state = DriverState.Initialising;
    private int fps;
    private StatusAnswer status;

    RemoteDriver(ServerClient sc) {
        this.message = "/";
        this.usedForAI = true;
        this.sc = sc;
        lastStatusRequest = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }

    public boolean isUsedForAI() {
        return usedForAI;
    }

    public void setUsedForAI(boolean usedForAI) {
        this.usedForAI = usedForAI;
    }

    public ServerClient getServerClient() {
        return sc;
    }

    public long getLastStatusRequest() {
        return lastStatusRequest;
    }
    
    public StatusAnswer getStatus() {
        return status;
    }

    void setLastStatusRequest(long lastStatusRequest) {
        this.lastStatusRequest = lastStatusRequest;
    }

    public int getPing() {
        return ping;
    }

    void setPing(int ping) {
        this.ping = ping;
    }

    public DriverState getState() {
        return state;
    }

    void setState(DriverState state) {
        this.state = state;
    }

    public int getFps() {
        return fps;
    }

    void setFps(int fps) {
        this.fps = fps;
    }

    void setStatus(StatusAnswer ans) {
        status = ans;
    }

}
