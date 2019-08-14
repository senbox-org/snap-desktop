package org.esa.snap.product.library.ui.v2.table;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;

/**
 * Created by jcoravu on 8/8/2019.
 */
public class TextLabelTableCellRenderer extends AbstractTableCellRenderer<JLabel> {

    public TextLabelTableCellRenderer(int horizontalAlignment) {
        super(new JLabel("", horizontalAlignment));
    }

    @Override
    public JLabel getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.cellComponent.setText((value == null) ? "" : value.toString());

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
