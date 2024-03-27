package de.minekonst.mariokartwiiai.shared.utils.commandline;

import java.util.Objects;

class CommandLineOption {

    private final String longName;
    private final String shortName;
    private boolean ready;
    private final String description;

    CommandLineOption(String longName, String shortName, String description) {
        Objects.requireNonNull(longName);
        Objects.requireNonNull(shortName);

        this.longName = longName;
        this.shortName = shortName;
        this.description = description != null ? description : "/";

    }

    public String getLongName() {
        return longName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    void onReady() {
        if (ready) {
            throw new IllegalStateException("Already set to ready");
        }
        ready = true;
    }

    void checkReady() {
        if (!ready) {
            throw new IllegalStateException("Not ready yet");
        }
    }

    @Override
    public String toString() {
        return longName;
    }
}
