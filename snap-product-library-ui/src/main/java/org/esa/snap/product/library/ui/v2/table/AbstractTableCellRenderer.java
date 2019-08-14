package org.esa.snap.product.library.ui.v2.table;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Created by jcoravu on 8/8/2019.
 */
public class AbstractTableCellRenderer<CellComponentType extends JComponent> implements TableCellRenderer {

    protected final CellComponentType cellComponent;

    protected AbstractTableCellRenderer(CellComponentType cellComponent) {
        this.cellComponent = cellComponent;
        this.cellComponent.setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public CellComponentType getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Color fg = null;
        Color bg = null;
        Color backgroundColor;
        Color foregroundColor;
        if (isSelected) {
            foregroundColor = (fg == null) ? table.getSelectionForeground() : fg;
            backgroundColor = (bg == null) ? table.getSelectionBackground() : bg;
        }
        else {
            foregroundColor = table.getForeground();
            backgroundColor = table.getBackground();
            if (backgroundColor == null || backgroundColor instanceof javax.swing.plaf.UIResource) {
                Color alternateColor = UIManager.getColor("Table.alternateRowColor", this.cellComponent.getLocale());
                if (alternateColor != null && row % 2 == 0) {
                    backgroundColor = alternateColor;
                }
            }
        }
        this.cellComponent.setOpaque(table.isOpaque());
        this.cellComponent.setEnabled(table.isEnabled());
        this.cellComponent.setForeground(foregroundColor);
        this.cellComponent.setBackground(backgroundColor);

        return this.cellComponent;
    }
}
