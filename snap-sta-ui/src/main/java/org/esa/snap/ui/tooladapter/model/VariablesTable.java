/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.ui.tooladapter.model;

import org.esa.snap.framework.gpf.descriptor.SystemVariable;
import org.esa.snap.framework.ui.UIUtils;
import org.esa.snap.framework.ui.tool.ToolButtonFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * @author Ramona Manda
 */
public class VariablesTable extends JTable {
    private static String[] columnNames = {"", "Key", "Value"};
    private static int[] widths = {27, 100, 250};
    private List<SystemVariable> variables;
    private MultiRenderer tableRenderer;

    public VariablesTable(List<SystemVariable> variables) {
        this.variables = variables;
        tableRenderer = new MultiRenderer();
        setModel(new VariablesTableModel());
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for(int i=0; i < widths.length; i++) {
            getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == 0) {
            return tableRenderer;
        }
        return super.getCellRenderer(row, column);
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (column == 0) {
            return tableRenderer;
        }
        return getDefaultEditor(String.class);
    }

    class VariablesTableModel extends AbstractTableModel {

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return variables.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return false;
                case 1:
                    return variables.get(row).getKey();
                case 2:
                    return variables.get(row).getValue();
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (aValue != null) {
                switch (columnIndex) {
                    case 0:
                        variables.remove(variables.get(rowIndex));
                        fireTableDataChanged();
                        break;
                    case 1:
                        variables.get(rowIndex).setKey(aValue.toString());
                        break;
                    case 2:
                        variables.get(rowIndex).setValue(aValue.toString());
                        break;
                }
            }
        }
    }

    class MultiRenderer extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
        private TableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
        private AbstractButton delButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/DeleteShapeTool16.gif"),
                false);

        public MultiRenderer() {
            delButton.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            switch (column) {
                case 0:
                    return delButton;
                default:
                    return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            switch (column) {
                case 0:
                    return delButton;
                default:
                    return getDefaultEditor(String.class).getTableCellEditorComponent(table, value, isSelected, row, column);
            }
        }

        @Override
        public Object getCellEditorValue() {
            return delButton;
        }
    }
}
