package de.minekonst.mariokartwiiai.shared.network;


public class NetworkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    NetworkException(String message) {
        super(message);
    }

    NetworkException(String message, Throwable cause) {
        super(message, cause);
    }

}
