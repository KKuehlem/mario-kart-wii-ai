package de.minekonst.mariokartwiiai.shared.utils.editortable;


@FunctionalInterface
public interface EditorTableListener {

    public void onValueUpdated(EditorTable table, EditorValue<?> value);
}
