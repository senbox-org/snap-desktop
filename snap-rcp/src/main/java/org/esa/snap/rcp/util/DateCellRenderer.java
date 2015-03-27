package org.esa.snap.rcp.util;

import javax.swing.JTable;
import java.awt.Component;
import java.text.DateFormat;

/**
 * DateCellRenderer to render a date in a table cell.
 * It extends {@link org.jfree.ui.DateCellRenderer} to fix the selection foreground color.
 *
 * @author Marco Peters
 */
public class DateCellRenderer extends org.jfree.ui.DateCellRenderer {

    public DateCellRenderer(DateFormat dateFormat) {
        super(dateFormat);
        this.setHorizontalAlignment(LEADING);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (isSelected) {
            this.setForeground(table.getSelectionForeground());
        } else {
            this.setForeground(null);
        }
        return this;
    }
}
