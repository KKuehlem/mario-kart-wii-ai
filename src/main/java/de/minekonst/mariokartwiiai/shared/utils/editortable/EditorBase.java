package de.minekonst.mariokartwiiai.shared.utils.editortable;


import java.io.Serializable;
import java.util.Objects;

public abstract class EditorBase implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String name;

    public EditorBase(String name) {
        Objects.requireNonNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}
