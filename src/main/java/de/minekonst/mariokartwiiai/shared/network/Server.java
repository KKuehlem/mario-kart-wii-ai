package de.minekonst.mariokartwiiai.shared.network;

import de.minekonst.mariokartwiiai.shared.network.messages.StatusMessage;
import de.minekonst.mariokartwiiai.shared.network.types.Disconnect;
import de.minekonst.mariokartwiiai.shared.network.types.Status;
import de.minekonst.mariokartwiiai.shared.utils.TimeUtils;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class Server {

    private final List<ServerClient> clients;
    private final List<ServerClient> newDisconnectedClients;
    private final ThreadGroup threadGroup;
    private final ServerSocket server;
    private final ServerListener listener;
    private volatile int nextClientID = 1;
    private volatile boolean running = true;

    /**
     * Create a new Server
     * 
     * @param port The port to listen on
     * @param listener The listener
     * @throws NetworkInitException typically, if port is bad or in use 
     */
    public Server(int port, ServerListener listener) throws NetworkInitException {
        Objects.requireNonNull(listener);

        this.clients = new CopyOnWriteArrayList<>();
        this.newDisconnectedClients = new CopyOnWriteArrayList<>();
        this.threadGroup = new ThreadGroup("Server Thread (Port " + port + ")");
        this.listener = listener;

        try {
            server = new ServerSocket(port);
        }
        catch (IllegalArgumentException ex) {
            throw new NetworkInitException("Failed to start server. Are you sure the port is in range?", ex);
        }
        catch (IOException ex) {
            throw new NetworkInitException("Failed to start server. Seems like port is already in use", ex);
        }

        waitForClients();
        startMessageQueue();
    }

    public void forEachConnectedClient(Consumer<ServerClient> c) {
        if (!running) {
            return;
        }
        
        this.clients.forEach(c);
    }
    
    public void closeConnection(ServerClient sc) {
        if (!running) {
            return;
        }
        
        if (clients.contains(sc)) {
            newDisconnectedClients.add(sc);
            sc.send(new StatusMessage(Status.ConnectionClosed, Disconnect.ClosedByServer.ordinal()));
            listener.onClientDisconnected(sc, Disconnect.ClosedByServer);
        }
        else {
            throw new UnsupportedOperationException("This client is not connected to the server");
        }
    }

    public void broadcast(Serializable s) {
        if (!running) {
            return;
        }
        
        forEachConnectedClient((ServerClient sc) -> {
            sc.send(s);
        });
    }
    
    public void close() {
        if (!running) {
            return;
        }
        
        forEachConnectedClient((ServerClient sc) -> {
            sc.send(new StatusMessage(Status.ConnectionClosed, Disconnect.ClosedByServer.ordinal()));
            sc.sendMessages();
            sc.close();
        });
        clients.clear();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
    
    ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    void onMessage(ServerClient sc, Object o) {
        listener.onMessage(sc, o);
    }

    void onDisconnect(ServerClient sc, Disconnect reason) {
        newDisconnectedClients.add(sc);
        listener.onClientDisconnected(sc, reason);
    }

    private void waitForClients() {
        Thread t = new Thread(threadGroup, () -> {
            while (running) {
                try {
                    int id = nextClientID;
                    nextClientID++;
                    ServerClient s = new ServerClient(id, server.accept(), this);
                    s.send(new StatusMessage(Status.ConnectionEstablished, id));
                    
                    clients.add(s);
                    listener.onClientConnected(s);
                }
                catch (IOException ex) {
                    throw new NetworkException("?", ex);
                }
            }
        });
        t.setDaemon(true);
        t.setName("Wait for Clients");
        t.start();
    }

    private void startMessageQueue() {
        Thread t = new Thread(threadGroup, () -> {
            while (running) {
                forEachConnectedClient((ServerClient sc) -> {
                    sc.sendMessages();
                });
                
                if (!newDisconnectedClients.isEmpty()) {
                    clients.removeAll(newDisconnectedClients);
                    newDisconnectedClients.clear();
                }
                
                TimeUtils.sleep(0, 10_000);
            }
        });
        t.setName("Server Message Queue");
        t.start();
    }

}
