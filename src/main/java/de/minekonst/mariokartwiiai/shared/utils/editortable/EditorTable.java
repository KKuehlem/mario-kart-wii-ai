package de.minekonst.mariokartwiiai.shared.utils.editortable;

import de.minekonst.mariokartwiiai.shared.utils.FileUtils;
import de.minekonst.mariokartwiiai.shared.utils.GuiUtils;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class EditorTable {

    private final String file;
    private final EditorBase[] values;
    private final List<EditorTableListener> listeners;
    private final boolean showDescription;
    private double[] proportions;

    /**
     * Create an Editor Table without bindings to a file
     *
     * @param values The Editor Values and HEadings
     */
    public EditorTable(EditorBase... values) {
        this(null, true, values);
    }

    public EditorTable(String file, EditorBase... values) {
        this(file, true, values);
    }

    public EditorTable(String file, boolean showDescription, EditorBase... values) {
        this.file = file;
        this.values = values;
        this.listeners = new ArrayList<>();
        if (file != null) {
            loadEditorValues();
        }
        this.showDescription = showDescription;
        if (showDescription) {
            proportions = new double[]{15, 20, 15, 50};
        }
        else {
            proportions = new double[]{40, 30, 30};
        }
    }

    public EditorTable(boolean showDescription, List<? extends EditorBase> values) {
        this(null, showDescription, values);
    }

    public EditorTable(String file, boolean showDescription, List<? extends EditorBase> values) {
        this.file = file;
        this.values = values.toArray(new EditorBase[values.size()]);
        this.listeners = new ArrayList<>();
        if (file != null) {
            loadEditorValues();
        }
        this.showDescription = showDescription;
    }

    public void addListener(EditorTableListener listener) {
        listeners.add(listener);
    }

    public void initTable(JTable table, Component parent) {
        initTable(table, parent, null, null, null);
    }

    public void initTable(JTable table, Component parent, Color foreground, Color selectionForeground, Color background) {
        table.removeEditor();;
        table.removeAll();
        //<editor-fold defaultstate="collapsed" desc="Init Table">
        table.setModel(new DefaultTableModel(
                new Object[][]{},
                showDescription ? new String[]{"Name", "Value", "Type", "Description"} : new String[]{"Name", "Value", "Type"}) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 1;
            }
        });
        //</editor-fold>

        table.getTableHeader().setReorderingAllowed(false);
        CellEditor editor = new CellEditor(values, this, table, parent, foreground, background);
        table.getColumnModel().getColumn(1).setCellEditor(editor);
        table.setDefaultRenderer(Object.class, new CellRenderer(editor, background));
        //<editor-fold defaultstate="collapsed" desc="Colors">
        if (foreground != null) {
            table.setForeground(foreground);
        }
        if (background != null) {
            table.setBackground(background);
        }
        if (selectionForeground != null) {
            table.setSelectionForeground(selectionForeground);
        }
        //</editor-fold>
        table.setRowHeight(table.getRowHeight() + 2);

        updateTable(table);
    }

    public void setProportions(JTable table, double... values) {
        if (showDescription && values.length != 4) {
            throw new IllegalArgumentException("Need 4 values");
        }
        else if (!showDescription && values.length != 3) {
            throw new IllegalArgumentException("Need 3 values");
        }

        proportions = values;
        GuiUtils.setJTableColumnsWidth(table, table.getWidth(), proportions);
    }

    void onValueUpdated(JTable table, Component parent, String value, int row) {
        EditorBase ev = values[row];

        if (!(ev instanceof EditorValue)) {
            return;
        }
        EditorValue<?> v = (EditorValue) ev;
        v.fromString(value, parent);

        writeEditorValues();
        updateTable(table);

        for (EditorTableListener l : listeners) {
            l.onValueUpdated(this, v);
        }
    }

    private void updateTable(JTable table) {
        DefaultTableModel model = GuiUtils.clearTable(table);

        for (EditorBase val : values) {
            if (val instanceof EditorValue) {
                EditorValue<?> ev = (EditorValue<?>) val;
                Object value = ev.getValue();
                if (value instanceof Integer) {
                    value = String.format("%,d", value);
                }
                
                if (showDescription) {
                    model.addRow(new Object[]{val, value, ev.getType().getSimpleName(), ev.getDescription()});
                }
                else {
                    model.addRow(new Object[]{val, value, ev.getType().getSimpleName()});
                }
            }
            else {
                model.addRow(new Object[]{val, "", ""});
            }
        }

        GuiUtils.setJTableColumnsWidth(table, table.getWidth(), proportions);
    }

    private void writeEditorValues() {
        if (file == null) {
            return;
        }

        String str = "";

        boolean first = true;
        for (EditorBase val : values) {
            if (val instanceof EditorValue) {
                str += (first ? "" : "\n") + val.getName() + "~" + ((EditorValue) val).getValue().toString();
                first = false;
            }
        }

        FileUtils.writeFile(file, str);
    }

    private void loadEditorValues() {
        List<String> f = FileUtils.readFile(file);
        HashMap<String, String> map = new HashMap<>(values.length);

        for (String s : f) {
            String[] split = s.split("~");
            map.put(split[0], split[1]);
        }

        for (EditorBase val : values) {
            if (val instanceof EditorValue) {
                String s = map.get(val.getName());
                if (s != null) {
                    ((EditorValue) val).fromString(s);
                }
            }
        }
    }

}
