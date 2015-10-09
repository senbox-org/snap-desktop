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

import org.esa.snap.core.gpf.descriptor.SystemDependentVariable;
import org.esa.snap.core.gpf.descriptor.SystemVariable;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.esa.snap.ui.tooladapter.dialogs.SystemDependentVariableEditorDialog;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author Ramona Manda
 */
public class VariablesTable extends JTable {
    private static String[] columnNames = {"", "Key", "Value"};
    private static int[] widths = {24, 100, 250};
    private List<SystemVariable> variables;
    private MultiRenderer tableRenderer;
    private AppContext appContext;

    public VariablesTable(List<SystemVariable> variables, AppContext context) {
        this.variables = variables;
        this.appContext = context;
        tableRenderer = new MultiRenderer();
        setModel(new VariablesTableModel());
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
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
        switch (column) {
            case 0:
            case 2:
                return tableRenderer;
            default:
                return getDefaultEditor(String.class);
        }
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
        private TableCellEditor lastEditor;


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
                    lastEditor = null;
                    return delButton;
                case 2:
                    SystemVariable variable = variables.get(row);
                    if (variable instanceof SystemDependentVariable) {
                        lastEditor = new SystemDependentVariableCellEditor(appContext.getApplicationWindow(), (SystemDependentVariable)variable, null);
                        return lastEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                    } else {
                        lastEditor = getDefaultEditor(String.class);
                        return lastEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                    }
                default:
                    lastEditor = getDefaultEditor(String.class);
                    return lastEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
        }

        @Override
        public Object getCellEditorValue() {
            return lastEditor != null ? lastEditor.getCellEditorValue() : delButton; //delButton;
        }
    }

    class SystemDependentVariableCellEditor extends DefaultCellEditor implements TableCellEditor {

        private static final int CLICK_COUNT_TO_START = 2;
        private JButton button;
        private SystemDependentVariableEditorDialog dialog;
        private SystemDependentVariable variable;

        /**
         * Constructor.
         */
        public SystemDependentVariableCellEditor(Window window, SystemDependentVariable variable, String helpID) {
            super(new JTextField());
            setClickCountToStart(CLICK_COUNT_TO_START);

            // Using a JButton as the editor component
            button = new JButton();
            button.setBackground(Color.white);
            button.setFont(button.getFont().deriveFont(Font.PLAIN));
            button.setBorder(null);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setText(variable.getValue());
            this.variable = variable;
            // Dialog which will do the actual editing
            dialog = new SystemDependentVariableEditorDialog(window, this.variable, helpID);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == CLICK_COUNT_TO_START) {
                        dialog.show();
                        fireEditingStopped();
                    }
                }
            });
        }

        @Override
        public Object getCellEditorValue() {
            return variable.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            /*SwingUtilities.invokeLater(() -> {
                dialog.show();
                fireEditingStopped();
            });*/
            return button;
        }
    }
}
