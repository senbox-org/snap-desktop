package org.esa.snap.ui.color;

import org.openide.awt.ColorComboBox;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.esa.snap.ui.color.ColorComboBoxUtil;

/**
 * A table cell editor for color values.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public class ColorTableCellEditor extends AbstractCellEditor implements TableCellEditor, PropertyChangeListener {
    private ColorComboBox colorComboBox;
    private boolean adjusting;

    public ColorTableCellEditor() {
//        this(new ColorComboBox());
        this(ColorComboBoxUtil.createColorCombobox());
    }

    public ColorTableCellEditor(ColorComboBox colorComboBox) {
        this.colorComboBox = colorComboBox;
//        this.colorComboBox.addPropertyChangeListener("selectedColor", this);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        adjusting = true;
        colorComboBox.setSelectedColor((Color) value);
        adjusting = false;
        colorComboBox.repaint();
        return colorComboBox;
    }

    @Override
    public Object getCellEditorValue() {
        return colorComboBox.getSelectedColor();
    }


    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!adjusting) {
            stopCellEditing();
            colorComboBox.repaint();
        }
    }
}
