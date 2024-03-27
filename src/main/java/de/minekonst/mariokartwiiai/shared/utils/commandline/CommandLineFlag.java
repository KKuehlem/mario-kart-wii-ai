package de.minekonst.mariokartwiiai.shared.utils.commandline;

public class CommandLineFlag extends CommandLineOption {

    private boolean isSet;

    public CommandLineFlag(String longName, String shortName, String desciption) {
        super(longName, shortName, desciption);
    }

    public boolean isSet() {
        super.checkReady();
        return isSet;
    }

    void set() {
        super.checkReady();
        this.isSet = true;
    }

}
