package de.minekonst.mariokartwiiai.shared.utils.editortable;


import java.awt.Color;
import java.util.Objects;

public class EditorHeading extends EditorBase {

    private static final long serialVersionUID = 1L;

    private final Color background;

    public EditorHeading(String name, Color background) {
        super(name);

        Objects.requireNonNull(background);

        this.background = background;
    }

    public Color getBackground() {
        return background;
    }
}
