package de.minekonst.mariokartwiiai.shared.utils.editortable;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EditorValueCollection implements Serializable, Iterable<EditorBase> {

    private static final long serialVersionUID = 1L;

    private final List<EditorBase> values;

    public EditorValueCollection() {
        this((EditorBase[]) null);
    }

    public EditorValueCollection(EditorBase... values) {
        this.values = new ArrayList<>();
        if (values != null) {
            this.values.addAll(Arrays.asList(values));
        }
    }

    public void add(EditorBase... values) {
        this.values.addAll(Arrays.asList(values));
    }

    @Override
    public Iterator<EditorBase> iterator() {
        return values.iterator();
    }

    public List<EditorBase> getEditorValues() {
        return Collections.unmodifiableList(values);
    }
    
}
