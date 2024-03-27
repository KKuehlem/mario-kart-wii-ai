package de.minekonst.mariokartwiiai.main.config;

import de.minekonst.mariokartwiiai.main.ProgramInstance;


public abstract class Config {
    public abstract ProgramInstance create();
    public abstract boolean isServer();
}
