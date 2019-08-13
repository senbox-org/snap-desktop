package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.table.AbstractTableCellRenderer;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;

/**
 * Created by jcoravu on 8/8/2019.
 */
public class QuickLookImageTableCellRenderer extends AbstractTableCellRenderer<JLabel> {

    public QuickLookImageTableCellRenderer(int horizontalAlignment, int verticalAlignment) {
        super(new JLabel(""));

        this.cellComponent.setHorizontalAlignment(horizontalAlignment);
        this.cellComponent.setVerticalAlignment(verticalAlignment);
    }

    @Override
    public JLabel getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        ImageIcon imageIcon = (ImageIcon)value;
        String text = null;
        if (imageIcon == ProductsTableModel.EMPTY_ICON) {
            imageIcon = null;
            text = "Not available";
        }
        this.cellComponent.setIcon(imageIcon);
        this.cellComponent.setText(text);

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
