package de.minekonst.mariokartwiiai.server.messages;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WelcomeMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;

}
