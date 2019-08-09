package org.esa.snap.product.library.ui.v2.table;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

/**
 * Created by jcoravu on 8/8/2019.
 */
public class LabelTableCellRenderer extends JLabel implements TableCellRenderer {

    public LabelTableCellRenderer(int horizontalAlignment) {
        super("", horizontalAlignment);

        setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText(value.toString());
        return this;
    }
}
