package de.minekonst.mariokartwiiai.shared.utils.editortable;


import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

class CellEditor extends AbstractCellEditor implements TableCellEditor {

    private final TableCellEditor[] editors;
    private final Component[] components;
    private TableCellEditor currentEditor;

    CellEditor(EditorBase[] values, EditorTable editorTable, JTable table, Component parent, Color foreground, Color background) {
        editors = new TableCellEditor[values.length];
        components = new Component[values.length];
        for (int x = 0; x < values.length; x++) {
            final int fx = x;

            if (values[x] instanceof EditorValue<?>) {
                EditorValue<?> editorValue = (EditorValue<?>) values[x];
                if (editorValue.getType() == Boolean.class) {
                    //<editor-fold defaultstate="collapsed" desc="CheckBox for Booleans">
                    JCheckBox cb = new JCheckBox();
                    components[x] = cb;
                    editors[x] = new DefaultCellEditor(cb);
                    cb.setSelected((boolean) editorValue.getValue());

                    cb.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            editorTable.onValueUpdated(table, parent, cb.isSelected() + "", fx);
                        }
                    });
                    //</editor-fold>
                }
                else if (editorValue.getType().isEnum()) {
                    //<editor-fold defaultstate="collapsed" desc="ComboBox for Enums">
                    JComboBox<Object> comboBox = new JComboBox<>();
                    for (Object o : editorValue.getType().getEnumConstants()) {
                        comboBox.addItem(o);
                    }
                    components[x] = comboBox;
                    editors[x] = new DefaultCellEditor(comboBox);
                    comboBox.setSelectedItem(editorValue.getValue());

                    comboBox.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            editorTable.onValueUpdated(table, parent, comboBox.getSelectedItem().toString(), fx);
                        }
                    });
                    comboBox.setBorder(null);
                    //</editor-fold>
                }
                else {
                    //<editor-fold defaultstate="collapsed" desc="Text Field for everything else">
                    JTextField tf = new JTextField();
                    components[x] = tf;
                    editors[x] = new DefaultCellEditor(tf);

                    updateTF(tf, editorValue);

                    tf.addFocusListener(new FocusListener() {
                        @Override
                        public void focusGained(FocusEvent e) {
                        }

                        @Override
                        public void focusLost(FocusEvent e) {
                            editorTable.onValueUpdated(table, parent, tf.getText(), fx);
                            updateTF(tf, editorValue);
                        }
                    });
                    tf.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                                editorTable.onValueUpdated(table, parent, tf.getText(), fx);
                                updateTF(tf, editorValue);
                            }
                        }
                    });
                    tf.setBorder(null);
                    //</editor-fold>
                }

                if (foreground != null) {
                    components[x].setForeground(foreground);
                }
                if (background != null) {
                    components[x].setBackground(background);
                }
            }
        }
    }

    private void updateTF(JTextField tf, EditorValue<?> val) {
        String str = val.getValue().toString();
        if (val.getValue() instanceof Integer) {
            str = String.format("%,d", val.getValue());
        }
        tf.setText(str);
    }

    @Override
    public Object getCellEditorValue() {
        return currentEditor != null ? currentEditor.getCellEditorValue() : null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        currentEditor = editors[row];
        return currentEditor != null ? currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column) : null;
    }

    Component[] getComponents() {
        return components;
    }
}
