package de.minekonst.mariokartwiiai.shared.network;

import de.minekonst.mariokartwiiai.shared.network.messages.StatusMessage;
import de.minekonst.mariokartwiiai.shared.network.types.Disconnect;
import de.minekonst.mariokartwiiai.shared.network.types.Status;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerClient {

    private final Socket socket;
    private final Server server;
    private final int id;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private final List<Serializable> messageQueue;
    private volatile boolean connected = true;
    private Object userObject;

    ServerClient(int id, Socket accept, Server server) throws IOException {
        this.socket = accept;
        this.server = server;
        this.id = id;

        this.outputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.outputStream.flush(); // Important
        this.inputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
        this.messageQueue = new CopyOnWriteArrayList<>();

        waitForMessages();
    }

    public void send(Serializable s) {
        messageQueue.add(s);
    }

    public int getID() {
        return id;
    }

    public Object getUserObject() {
        return userObject;
    }

    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public String getHostname() {
        return socket.getInetAddress().getHostName();
    }
    
    public String getIP() {
        return socket.getInetAddress().getHostAddress();
    }

    void sendMessages() {
        if (!connected) {
            return;
        }
        
        messageQueue.removeIf((Serializable s) -> {
            try {
                this.outputStream.writeObject(s);
                this.outputStream.flush();
                //this.outputStream.reset(); // Important to avoid memory crashes
                return true;
            }
            catch (SocketException ex) {
                server.onDisconnect(this, Disconnect.ConnectionLost);
                connected = false;
                return true;
            }
            catch (IOException ex) {
                throw new NetworkException("?", ex);
            }
        });
    }

    void close() {
        connected = false;
    }

    private void waitForMessages() {
        Thread t = new Thread(server.getThreadGroup(), () -> {
            while (connected) {
                try {
                    Object o = inputStream.readObject();
                    if (o instanceof StatusMessage) {
                        StatusMessage status = (StatusMessage) o;
                        if (status.getStatus() == Status.ConnectionClosed) {
                            server.onDisconnect(this, Disconnect.values()[status.getParameter()]);
                            return;
                        }
                    }
                    else {
                        server.onMessage(this, o);
                    }
                }
                catch (EOFException | OptionalDataException | StreamCorruptedException | ClassCastException ex) {
                    ex.printStackTrace();
                    server.onDisconnect(this, Disconnect.ConnectionLost);
                    return;
                }
                catch (SocketException ex) {
                    ex.printStackTrace();
                    return;
                }
                catch (IOException | ClassNotFoundException ex) {
                    throw new NetworkException("?", ex);
                }
            }
        });

        t.setName("Client #" + id + " wait for messages");
        t.setDaemon(true);
        t.start();
    }

}
