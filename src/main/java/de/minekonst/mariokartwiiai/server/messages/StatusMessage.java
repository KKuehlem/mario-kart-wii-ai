package de.minekonst.mariokartwiiai.server.messages;

import java.io.Serializable;

public class StatusMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private final int code;

    public StatusMessage(StatusCode statusCode) {
        code = statusCode.ordinal();
    }
    
    public StatusCode getCode() {
        return StatusCode.values()[code];
    }

}
