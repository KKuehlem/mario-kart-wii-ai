package de.minekonst.mariokartwiiai.shared.utils.editortable;


import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

class CellRenderer extends DefaultTableCellRenderer {

    private final CellEditor editor;
    private final Color background;

    CellRenderer(CellEditor editor, Color background) {
        this.editor = editor;
        this.background = background != null ? background : Color.white;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (column == 1) {
            return editor.getComponents()[row];
        }
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value instanceof EditorHeading) {
            c.setBackground(((EditorHeading) value).getBackground());
        }
        else {
            c.setBackground(background);
        }

        return c;
    }
}
