package de.minekonst.mariokartwiiai.shared.network.messages;

import de.minekonst.mariokartwiiai.shared.network.types.Status;
import java.io.Serializable;

public class StatusMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final int parameter;

    public StatusMessage(Status status, int parameter) {
        this.status = status.ordinal();
        this.parameter = parameter;
    }

    public Status getStatus() {
        return Status.values()[status];
    }

    public int getParameter() {
        return parameter;
    }

}
