package org.esa.snap.rcp.metadata;

import org.netbeans.swing.outline.DefaultOutlineCellRenderer;
import org.openide.awt.HtmlRenderer;
import org.openide.nodes.Node.Property;
import org.openide.util.Exceptions;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;

/**
 * @author https://jnkjava.wordpress.com/2011/11/28/recipe-7-how-do-i-decorate-a-read-only-outlineview/
 */
class MetadataOutlineCellRenderer extends DefaultOutlineCellRenderer {

    /**
     * Gray Color for the odd lines in the view.
     */
    private static final Color VERY_LIGHT_GRAY = new Color(236, 236, 236);
    /**
     * Center the content of the cells displaying text.
     */
    protected boolean centered = System.getProperty("os.name").toLowerCase().indexOf("windows") != 0;
    /**
     * Highlight the non editable cells making the foreground lighter.
     */

    @Override
    @SuppressWarnings("unchecked")
    public Component getTableCellRendererComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {
        Component cell = null;
        Object valueToDisplay = value;
        if (value instanceof Property) {
            try {
                valueToDisplay = ((Property) value).getValue();
            } catch (IllegalAccessException | InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        if (valueToDisplay != null) {
            TableCellRenderer renderer = table.getDefaultRenderer(valueToDisplay.getClass());
            if (renderer != null) {
                cell = renderer.getTableCellRendererComponent(table, valueToDisplay, isSelected,
                                                              hasFocus, row, column);
            }
        } else {
            cell = super.getTableCellRendererComponent(table, valueToDisplay, isSelected, hasFocus, row, column);
        }
        if (cell != null) {
            if (cell instanceof HtmlRenderer.Renderer) {
                ((HtmlRenderer.Renderer) cell).setCentered(centered);
                ((HtmlRenderer.Renderer) cell).setIndent(5);
            } else if (cell instanceof DefaultTableCellRenderer.UIResource) {
                if (centered) {
                    ((DefaultTableCellRenderer.UIResource) cell).setHorizontalAlignment(JLabel.CENTER);
                } else {
                    ((DefaultTableCellRenderer.UIResource) cell).setHorizontalAlignment(JLabel.LEFT);
                }
            }
            Color foregroundColor = table.getForeground();
            cell.setForeground(foregroundColor);
            cell.setBackground(row % 2 == 0 ? Color.WHITE : VERY_LIGHT_GRAY);
            if (isSelected) {
                cell.setBackground(table.getSelectionBackground());
            }
        }
        return cell;
    }
}
