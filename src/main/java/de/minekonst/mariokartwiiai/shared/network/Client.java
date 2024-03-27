package de.minekonst.mariokartwiiai.shared.network;

import de.minekonst.mariokartwiiai.shared.network.messages.StatusMessage;
import de.minekonst.mariokartwiiai.shared.network.types.Disconnect;
import de.minekonst.mariokartwiiai.shared.network.types.Status;
import de.minekonst.mariokartwiiai.shared.utils.TimeUtils;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client {

    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final List<Serializable> queue;
    private final ClientListener listener;
    private volatile boolean connected;
    private volatile boolean running = true;
    private int id;

    /**
     * Create a client and connect to a server
     *
     * @param hostname The hostname of the server, might be domain, ip, ...
     * @param port     The port of the server
     * @param listener The listener
     *
     * @throws NetworkInitException typically, if no server is listening on that
     *                              port on that hostname
     */
    public Client(String hostname, int port, ClientListener listener) throws NetworkInitException {
        Objects.requireNonNull(hostname);
        Objects.requireNonNull(listener);
        this.listener = listener;
        queue = new CopyOnWriteArrayList<>();

        try {
            socket = new Socket(hostname, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            in = new ObjectInputStream(socket.getInputStream());
        }
        catch (IllegalArgumentException ex) {
            throw new NetworkInitException("Failed to connect to server. Port might be out of bounds", ex);
        }
        catch (IOException ex) {
            throw new NetworkInitException("Failed to connect to server", ex);
        }

        waitForMessages();
        sendMessages();
    }

    public int getID() {
        return id;
    }

    public void send(Serializable s) {
        if (connected) {
            queue.add(s);
        }
    }

    public void disconnect() {
        if (connected) {
            send(new StatusMessage(Status.ConnectionClosed, Disconnect.ClosedByClient.ordinal()));
            while (connected) {
                TimeUtils.sleep(1);
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private void sendMessages() {
        Thread t = new Thread(() -> {
            while (running) {
                if (connected) {
                    queue.removeIf((Serializable s) -> {
                        try {
                            if (s instanceof StatusMessage) {
                                if (((StatusMessage) s).getStatus() == Status.ConnectionClosed) {
                                    connected = running = false;
                                }
                            }
                            out.writeObject(s);
                            out.flush();

                            try {
                                out.reset();
                            }
                            catch (IOException ex) {
                            }
                        }
                        catch (IOException ex) {
                            throw new NetworkException("?", ex);
                        }

                        return true;
                    });
                }
                TimeUtils.sleep(0, 10_000);
            }
        });
        t.setName("Message Queue");
        t.start();
    }

    private void waitForMessages() {
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    Object o = in.readObject();
                    if (o instanceof StatusMessage) {
                        StatusMessage status = ((StatusMessage) o);
                        if (status.getStatus() == Status.ConnectionEstablished) {
                            connected = true;
                            id = status.getParameter();
                        }
                        else if (status.getStatus() == Status.ConnectionClosed) {
                            connected = running = false;
                            listener.onDisconnect(Disconnect.values()[status.getParameter()]);
                            return;
                        }
                    }
                    else {
                        listener.onMessage(o);
                    }
                }
                catch (EOFException | SocketException ex) {
                    listener.onDisconnect(Disconnect.ConnectionLost);
                    connected = running = false;
                    return;
                }
                catch (IOException ex) {
                    throw new NetworkException("?", ex);
                }
                catch (ClassNotFoundException ex) {
                    throw new NetworkException("?", ex);
                }

            }
        });
        t.setName("Wait for Messages");
        t.setDaemon(true);
        t.start();
    }
}
