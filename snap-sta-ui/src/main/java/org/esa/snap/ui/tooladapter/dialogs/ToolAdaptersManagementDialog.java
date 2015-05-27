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
package org.esa.snap.ui.tooladapter.dialogs;

import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterOp;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterRegistry;
import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.tool.ToolButtonFactory;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.tooladapter.actions.ToolAdapterActionRegistrar;
import org.esa.snap.ui.tooladapter.model.OperatorsTableModel;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Dialog that allows the management (create, edit, remove and execute) of external
 * tool adapters
 *
 * @author Ramona Manda
 * @author Cosmin Cara
 */
@NbBundle.Messages({
        "ToolTipNewOperator_Text=Define new operator",
        "ToolTipCopyOperator_Text=Duplicate the selected operator",
        "ToolTipEditOperator_Text=Edit the selected operator",
        "ToolTipExecuteOperator_Text=Execute the selected operator",
        "ToolTipDeleteOperator_Text=Delete the selected operator(s)",
        "MessageNoSelection_Text=Please select an adapter first"

})
public class ToolAdaptersManagementDialog extends ModalDialog {

    private AppContext appContext;
    private JTable operatorsTable = null;

    public ToolAdaptersManagementDialog(AppContext appContext, String title, String helpID) {
        super(appContext.getApplicationWindow(), title, ID_CLOSE, helpID);
        this.appContext = appContext;

        setContent(createContentPanel());
    }

    private JPanel createContentPanel() {
        //compute content and other buttons
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        JPanel buttonsPanel = createButtonsPanel();
        buttonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(buttonsPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(createPropertiesPanel());
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JScrollPane(createAdaptersPanel()));
        return panel;
    }

    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        AbstractButton newButton = ToolButtonFactory.createButton(TangoIcons.actions_document_new(TangoIcons.Res.R22), false);
        newButton.setToolTipText(Bundle.ToolTipNewOperator_Text());
        newButton.addActionListener(e -> {
            ToolAdapterOperatorDescriptor newOperatorSpi = new ToolAdapterOperatorDescriptor(ToolAdapterConstants.OPERATOR_NAMESPACE + "DefaultOperatorName", ToolAdapterOp.class, "DefaultOperatorName", null, null, null, null, null, null);
            ToolAdapterEditorDialog dialog = new ToolAdapterEditorDialog(appContext, newOperatorSpi, true);
            dialog.show();
            setContent(createContentPanel());
            getContent().repaint();
        });
        panel.add(newButton);

        AbstractButton copyButton = ToolButtonFactory.createButton(TangoIcons.actions_edit_copy(TangoIcons.Res.R22), false);
        copyButton.setToolTipText(Bundle.ToolTipCopyOperator_Text());
        copyButton.addActionListener(e -> {
            ToolAdapterOperatorDescriptor operatorDesc = ((OperatorsTableModel) operatorsTable.getModel()).getFirstCheckedOperator();
            if (operatorDesc != null) {
                String opName = operatorDesc.getName();
                int newNameIndex = 0;
                while (GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(opName) != null) {
                    newNameIndex++;
                    opName = operatorDesc.getName() + ToolAdapterConstants.OPERATOR_GENERATED_NAME_SEPARATOR + newNameIndex;
                }
                ToolAdapterEditorDialog dialog = new ToolAdapterEditorDialog(appContext, operatorDesc, newNameIndex);
                dialog.show();
                setContent(createContentPanel());
                getContent().repaint();
            } else {
                SnapDialogs.showWarning(Bundle.MessageNoSelection_Text());
            }
        });
        panel.add(copyButton);

        AbstractButton editButton = ToolButtonFactory.createButton(TangoIcons.actions_document_open(TangoIcons.Res.R22), false);
        editButton.setToolTipText(Bundle.ToolTipEditOperator_Text());
        editButton.addActionListener(e -> {
            ToolAdapterOperatorDescriptor operatorDesc = ((OperatorsTableModel) operatorsTable.getModel()).getFirstCheckedOperator();
            if (operatorDesc != null) {
                ToolAdapterEditorDialog dialog = new ToolAdapterEditorDialog(appContext, operatorDesc, false);
                dialog.show();
                setContent(createContentPanel());
                getContent().repaint();
            } else {
                SnapDialogs.showWarning(Bundle.MessageNoSelection_Text());
            }
        });
        panel.add(editButton);

        AbstractButton delButton = ToolButtonFactory.createButton(TangoIcons.actions_edit_delete(TangoIcons.Res.R22), false);
        delButton.setToolTipText(Bundle.ToolTipDeleteOperator_Text());
        delButton.addActionListener(e -> {
            java.util.List<ToolAdapterOperatorDescriptor> operatorDescriptors = ((OperatorsTableModel) operatorsTable.getModel()).getCheckedOperators();
            if (operatorDescriptors != null && operatorDescriptors.size() > 0) {
                if (SnapDialogs.Answer.YES == SnapDialogs.requestDecision("Confirm removal", "Are you sure you want to remove the selected adapter(s)?\nThe operation will delete also the associated folder and files", true, "Don't ask me in the future")) {
                    operatorDescriptors.stream().filter(descriptor -> descriptor != null).forEach(descriptor -> {
                        ToolAdapterActionRegistrar.removeOperatorMenu(descriptor);
                        ToolAdapterIO.removeOperator(descriptor);
                    });
                    setContent(createContentPanel());
                    getContent().repaint();
                }
            } else {
                SnapDialogs.showWarning(Bundle.MessageNoSelection_Text());
            }
        });
        panel.add(delButton);

        panel.add(Box.createHorizontalStrut(22));

        AbstractButton runButton = ToolButtonFactory.createButton(TangoIcons.actions_view_refresh(TangoIcons.Res.R22), false);
        runButton.setToolTipText(Bundle.ToolTipExecuteOperator_Text());
        runButton.addActionListener(e -> {
            ToolAdapterOperatorDescriptor operatorDesc = null;
            int[] selectedRows = operatorsTable.getSelectedRows();
            if (selectedRows != null && selectedRows.length > 0) {
                operatorDesc = ((OperatorsTableModel) operatorsTable.getModel()).getObjectAt(selectedRows[0]);
            } else {
                operatorDesc = ((OperatorsTableModel) operatorsTable.getModel()).getFirstCheckedOperator();
            }
            if (operatorDesc != null) {
                close();
                final ToolAdapterExecutionDialog operatorDialog = new ToolAdapterExecutionDialog(
                        operatorDesc,
                        appContext,
                        operatorDesc.getLabel());
                operatorDialog.show();
            } else {
                SnapDialogs.showWarning(Bundle.MessageNoSelection_Text());
            }
        });
        panel.add(runButton);

        return panel;
    }

    private JTable createPropertiesPanel() {
        DefaultTableModel model = new DefaultTableModel(1, 2) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };
        model.setValueAt("Adapters location", 0, 0);
        model.setValueAt(ToolAdapterIO.getUserAdapterPath(), 0, 1);
        model.addTableModelListener(l -> {
            String newPath = model.getValueAt(0, 1).toString();
            File path = new File(newPath);
            if (!path.exists() &&
                    SnapDialogs.Answer.YES == SnapDialogs.requestDecision("Path does not exist", "The path you have entered does not exist.\nDo you want to create it?", true, "Don't ask me in the future")) {
                if (!path.mkdirs()) {
                    SnapDialogs.showError("Path could not be created!");
                }
            }
            if (path.exists()) {
                try {
                    Preferences modulePrefs = NbPreferences.forModule(ToolAdapterIO.class);
                    modulePrefs.put("user.module.path", newPath);
                    modulePrefs.sync();
                    //SnapDialogs.showInformation("The path for user adapters will be considered next time the application is opened.", "Don't show this dialog");
                } catch (BackingStoreException e1) {
                    SnapDialogs.showError(e1.getMessage());
                }
            }
        });
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(250);
        TableColumn pathColumn = table.getColumnModel().getColumn(1);
        pathColumn.setMaxWidth(570);
        pathColumn.setCellEditor(new FileChooserCellEditor());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setRowHeight(20);
        table.setBorder(BorderFactory.createLineBorder(Color.black));
        table.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                Object source = e.getSource();
                if (!table.equals(source)) {
                    table.editingCanceled(new ChangeEvent(source));
                    table.clearSelection();
                }
            }
        });
        return table;
    }

    private JTable createAdaptersPanel() {
        java.util.List<ToolAdapterOperatorDescriptor> toolboxSpis = new ArrayList<>();
        toolboxSpis.addAll(ToolAdapterRegistry.INSTANCE.getOperatorMap().values()
                                .stream()
                                .map(e -> (ToolAdapterOperatorDescriptor)e.getOperatorDescriptor())
                                .collect(Collectors.toList()));
        toolboxSpis.sort((o1, o2) -> o1.getAlias().compareTo(o2.getAlias()));
        OperatorsTableModel model = new OperatorsTableModel(toolboxSpis);
        operatorsTable = new JTable(model);
        operatorsTable.getColumnModel().getColumn(0).setMaxWidth(20);
        operatorsTable.getColumnModel().getColumn(1).setMaxWidth(250);
        operatorsTable.getColumnModel().getColumn(2).setMaxWidth(500);
        operatorsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        operatorsTable.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    int selectedRow = operatorsTable.getSelectedRow();
                    operatorsTable.getModel().setValueAt(true, selectedRow, 0);
                    operatorsTable.repaint();
                    ToolAdapterOperatorDescriptor operatorDesc = ((OperatorsTableModel) operatorsTable.getModel()).getFirstCheckedOperator();
                    ToolAdapterEditorDialog dialog = new ToolAdapterEditorDialog(appContext, operatorDesc, false);
                    dialog.show();
                    setContent(createContentPanel());
                    getContent().repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        return operatorsTable;
    }

    public class FileChooserCellEditor extends DefaultCellEditor implements TableCellEditor {

        /** Number of clicks to start editing */
        private static final int CLICK_COUNT_TO_START = 2;
        /** Editor component */
        private JButton button;
        /** File chooser */
        private JFileChooser fileChooser;
        /** Selected file */
        private String file = "";

        /**
         * Constructor.
         */
        public FileChooserCellEditor() {
            super(new JTextField());
            setClickCountToStart(CLICK_COUNT_TO_START);

            // Using a JButton as the editor component
            button = new JButton();
            button.setBackground(Color.white);
            button.setFont(button.getFont().deriveFont(Font.PLAIN));
            button.setBorder(null);

            // Dialog which will do the actual editing
            fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }

        @Override
        public Object getCellEditorValue() {
            return file;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            file = value.toString();
            SwingUtilities.invokeLater(() -> {
                fileChooser.setSelectedFile(new File(file));
                if (fileChooser.showOpenDialog(button) == JFileChooser.APPROVE_OPTION) {
                    file = fileChooser.getSelectedFile().getAbsolutePath();
                }
                fireEditingStopped();
            });
            button.setText(file);
            return button;
        }
    }

}
