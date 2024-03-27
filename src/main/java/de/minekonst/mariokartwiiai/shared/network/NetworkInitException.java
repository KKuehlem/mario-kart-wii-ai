package de.minekonst.mariokartwiiai.shared.network;


public class NetworkInitException extends Exception {

    private static final long serialVersionUID = 1L;

    NetworkInitException(String message) {
        super(message);
    }

    NetworkInitException(String message, Throwable cause) {
        super(message, cause);
    }

}
