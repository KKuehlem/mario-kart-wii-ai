package de.minekonst.mariokartwiiai.shared.network;

import de.minekonst.mariokartwiiai.shared.network.types.Disconnect;

public interface ServerListener {

    public void onClientConnected(ServerClient sc);

    public void onClientDisconnected(ServerClient sc, Disconnect reason);

    public void onMessage(ServerClient sc, Object o);
}
