package de.minekonst.mariokartwiiai.client.bots;

import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Bot {

    @Setter protected boolean active;
    @Getter(AccessLevel.NONE) protected final Driver driver;
    private final String description;
    private final String name;

    public Bot(Driver driver, String name, String description) {
        this.driver = driver;
        this.description = description;
        this.name = name;
    }

    public abstract ButtonState update();

}
