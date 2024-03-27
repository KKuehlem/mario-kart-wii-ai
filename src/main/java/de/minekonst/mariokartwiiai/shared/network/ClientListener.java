package de.minekonst.mariokartwiiai.shared.network;

import de.minekonst.mariokartwiiai.shared.network.types.Disconnect;

public interface ClientListener {

    public void onMessage(Object o);

    public void onDisconnect(Disconnect reason);
}
