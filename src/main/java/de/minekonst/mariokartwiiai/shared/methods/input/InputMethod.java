package de.minekonst.mariokartwiiai.shared.methods.input;

import de.minekonst.mariokartwiiai.shared.utils.editortable
.EditorValue;
import de.minekonst.mariokartwiiai.client.Driver;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public abstract class InputMethod implements Serializable {

    public static final InputMethod[] DEFAULT = {new PixelInputMethod(30, 40, 2), new RayInputMethod(50, 150, 150)};

    public abstract int getNeededNeurons();

    public abstract double[] calculate(Driver driver);

    public abstract List<EditorValue<?>> getEditorValues();

    public abstract void draw(Graphics2D g, int canvasWidth, int canvasHeight, double[] fov, Driver driver);

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        getEditorValues();
    }
}
