package de.minekonst.mariokartwiiai.shared.utils.commandline;

public class CommandLineParameter<T> extends CommandLineOption {

    private final Class<T> type;
    private final boolean requiered;
    private T value;

    public CommandLineParameter(String longName, String shortName, String description, Class<T> type) {
        this(longName, shortName, description, type, false);
    }

    public CommandLineParameter(String longName, String shortName, String description, Class<T> type, boolean requiered) {
        super(longName, shortName, description);
        this.type = type;
        this.requiered = requiered;
    }

    public boolean isRequiered() {
        return requiered;
    }

    public T getValue() {
        super.checkReady();
        return value;
    }

    @SuppressWarnings("unchecked")
    void setValue(Object value) {
        super.checkReady();
        this.value = (T) value;
    }

    Class<T> getType() {
        return type;
    }

    boolean checkRequierd() {
        return isRequiered() && value == null;
    }

}
