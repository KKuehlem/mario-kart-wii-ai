package de.minekonst.mariokartwiiai.shared.utils;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class GuiUtils {

    private GuiUtils() {
    }

    public static void setJTableColumnsWidth(JTable table, double... percentages) {
        setJTableColumnsWidth(table, table.getWidth(), percentages);
    }
    
    public static void setJTableColumnsWidth(JTable table, int tablePreferredWidth,
            double... percentages) {
        double total = 0;
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            total += percentages[i];
        }

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth((int) (tablePreferredWidth * (percentages[i] / total)));
        }
    }
    
    public static DefaultTableModel clearTable(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int rowCount = model.getRowCount();
        for (int x = 0; x < rowCount; x++) {
            model.removeRow(0);
        }
        
        return model;
    }
}
