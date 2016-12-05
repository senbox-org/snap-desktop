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

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.*;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;
import org.esa.snap.ui.tooladapter.actions.EscapeAction;
import org.esa.snap.ui.tooladapter.actions.ToolAdapterActionRegistrar;
import org.esa.snap.ui.tooladapter.model.OperationType;
import org.esa.snap.ui.tooladapter.model.OperatorsTableModel;
import org.esa.snap.utils.AdapterWatcher;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.esa.snap.utils.SpringUtilities.DEFAULT_PADDING;
import static org.esa.snap.utils.SpringUtilities.makeCompactGrid;

/**
 * Dialog that allows the management (create, edit, remove and execute) of external
 * tool adapters
 *
 * @author Ramona Manda
 * @author Cosmin Cara
 */
@NbBundle.Messages({
        "Dialog_Title=External Tools",
        "ToolTipNewOperator_Text=Define new operator",
        "ToolTipCopyOperator_Text=Duplicate the selected operator",
        "ToolTipEditOperator_Text=Edit the selected operator",
        "ToolTipExport_Text=Create an installable module",
        "ToolTipExecuteOperator_Text=Execute the selected operator",
        "ToolTipDeleteOperator_Text=Delete the selected operator",
        "PathLabel_Text=Adapters location",
        "MessageNoSelection_Text=Please select an adapter first",
        "MessageConfirmRemoval_TitleText=Confirm removal",
        "MessageConfirmRemoval_Text=Are you sure you want to remove the selected adapter?\nThe operation will delete also the associated folder and files",
        "MessageConfirmRemovalDontAsk_Text=Don't ask me in the future",
        "MessagePackageModules_Text=The adapter %s was installed as a NetBeans module and cannot be removed from here.%nPlease uninstall the module."

})
public class ToolAdaptersManagementDialog extends ModelessDialog {

    final int CHECK_COLUMN_WIDTH = 20;
    final int LABEL_COLUMN_WIDTH = 250;
    final int COLUMN_WIDTH = 270;
    final int PATH_ROW_HEIGHT = 20;
    final int BUTTON_HEIGHT = 32;
    final Dimension buttonDimension = new Dimension((CHECK_COLUMN_WIDTH + LABEL_COLUMN_WIDTH + COLUMN_WIDTH) / 5, BUTTON_HEIGHT);
    private AppContext appContext;
    private JTable operatorsTable = null;

    private static ToolAdaptersManagementDialog instance;

    public static void showDialog(AppContext appContext, String helpID) {
        if (instance == null) {
            instance = new ToolAdaptersManagementDialog(appContext, Bundle.Dialog_Title(), helpID);
        }
        instance.show();
    }

    private ToolAdaptersManagementDialog(AppContext appContext, String title, String helpID) {
        super(appContext.getApplicationWindow(), title, 0, helpID);
        this.appContext = appContext;
        JPanel contentPanel = createContentPanel();
        setContent(contentPanel);
        super.getJDialog().setMinimumSize(contentPanel.getPreferredSize());
        EscapeAction.register(super.getJDialog());
        ToolAdapterRegistry.INSTANCE.addListener(new ToolAdapterListener() {
            @Override
            public void adapterAdded(ToolAdapterOperatorDescriptor operatorDescriptor) {
                refreshContent();
            }

            @Override
            public void adapterRemoved(ToolAdapterOperatorDescriptor operatorDescriptor) {
                refreshContent();
            }
        });
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(DEFAULT_PADDING, DEFAULT_PADDING));
        int panelHeight = 0;
        JTable propertiesPanel = createPropertiesPanel();
        panelHeight += propertiesPanel.getPreferredSize().getHeight();
        panel.add(propertiesPanel, BorderLayout.PAGE_START);
        panelHeight += 10;

        SpringLayout springLayout = new SpringLayout();
        JPanel adaptersAndButtonsPanel = new JPanel(springLayout);
        JScrollPane scrollPane = new JScrollPane(createAdaptersPanel());
        panelHeight += scrollPane.getPreferredSize().getHeight();
        adaptersAndButtonsPanel.add(scrollPane);
        panelHeight += 10;
        JPanel buttonsPanel = createButtonsPanel();
        panelHeight += buttonsPanel.getPreferredSize().getHeight();
        adaptersAndButtonsPanel.add(buttonsPanel);
        springLayout.putConstraint(SpringLayout.NORTH, adaptersAndButtonsPanel, DEFAULT_PADDING, SpringLayout.NORTH, scrollPane);
        springLayout.putConstraint(SpringLayout.WEST, adaptersAndButtonsPanel, DEFAULT_PADDING, SpringLayout.WEST, scrollPane);
        springLayout.putConstraint(SpringLayout.EAST, adaptersAndButtonsPanel, DEFAULT_PADDING, SpringLayout.EAST, scrollPane);
        springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, DEFAULT_PADDING, SpringLayout.NORTH, buttonsPanel);
        springLayout.putConstraint(SpringLayout.EAST, adaptersAndButtonsPanel, DEFAULT_PADDING, SpringLayout.EAST, buttonsPanel);
        springLayout.putConstraint(SpringLayout.WEST, adaptersAndButtonsPanel, DEFAULT_PADDING, SpringLayout.WEST, buttonsPanel);
        springLayout.putConstraint(SpringLayout.SOUTH, adaptersAndButtonsPanel, DEFAULT_PADDING, SpringLayout.SOUTH, buttonsPanel);
        makeCompactGrid(adaptersAndButtonsPanel, 2, 1, 0, 0, DEFAULT_PADDING, DEFAULT_PADDING);

        panel.add(adaptersAndButtonsPanel, BorderLayout.CENTER);

        JPanel sideButtonsPanel = createSideButtonsPanel();
        panel.add(sideButtonsPanel, BorderLayout.LINE_END);

        panel.setPreferredSize(new Dimension(CHECK_COLUMN_WIDTH + LABEL_COLUMN_WIDTH + COLUMN_WIDTH - 32, panelHeight + DEFAULT_PADDING));

        return panel;
    }

    private JPanel createSideButtonsPanel() {
        JPanel panel = new JPanel(new SpringLayout());
        /**
         * New adapter button
         */
        panel.add(createButton(null, //"New",
                TangoIcons.actions_list_add(TangoIcons.Res.R22),
                Bundle.ToolTipNewOperator_Text(),
                e -> {
                    ToolAdapterOperatorDescriptor newOperatorSpi = new ToolAdapterOperatorDescriptor(ToolAdapterConstants.OPERATOR_NAMESPACE + "NewOperator", ToolAdapterOp.class, "NewOperator", null, null, null, null, null, null);
                    AbstractAdapterEditor dialog = AbstractAdapterEditor.createEditorDialog(appContext, getJDialog(), newOperatorSpi, OperationType.NEW);
                    dialog.show();
                    refreshContent();
                }));
        /**
         * Duplicate adapter button
         */
        panel.add(createButton(null, //"Copy",
                TangoIcons.actions_edit_copy(TangoIcons.Res.R22),
                Bundle.ToolTipCopyOperator_Text(),
                e -> {
                    ToolAdapterOperatorDescriptor operatorDesc = requestSelection();
                    if (operatorDesc != null) {
                        String opName = operatorDesc.getName();
                        int newNameIndex = 0;
                        while (GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(opName) != null) {
                            newNameIndex++;
                            opName = operatorDesc.getName() + ToolAdapterConstants.OPERATOR_GENERATED_NAME_SEPARATOR + newNameIndex;
                        }
                        AbstractAdapterEditor dialog = AbstractAdapterEditor.createEditorDialog(appContext, getJDialog(), operatorDesc, newNameIndex, OperationType.COPY);
                        dialog.show();
                        refreshContent();
                    }
                }));
        /**
         * Edit adapter button
         */
        panel.add(createButton(null, //"Edit",
                TangoIcons.apps_accessories_text_editor(TangoIcons.Res.R22),
                Bundle.ToolTipEditOperator_Text(),
                e -> {
                    ToolAdapterOperatorDescriptor operatorDesc = requestSelection();
                    if (operatorDesc != null) {
                        AbstractAdapterEditor dialog = AbstractAdapterEditor.createEditorDialog(appContext, getJDialog(), operatorDesc, OperationType.EDIT);
                        dialog.show();
                        refreshContent();
                    }
                }));
        /**
         * Delete adapter button
         */
        panel.add(createButton(null, //"Delete",
                TangoIcons.actions_list_remove(TangoIcons.Res.R22),
                Bundle.ToolTipDeleteOperator_Text(),
                e -> {
                    ToolAdapterOperatorDescriptor operatorDescriptor = requestSelection();
                    if (operatorDescriptor != null) {
                        if (Dialogs.Answer.YES == Dialogs.requestDecision(Bundle.MessageConfirmRemoval_TitleText(),
                                Bundle.MessageConfirmRemoval_Text(), true,
                                Bundle.MessageConfirmRemovalDontAsk_Text())) {
                            if (operatorDescriptor.isFromPackage()) {
                                Dialogs.showWarning(String.format(Bundle.MessagePackageModules_Text(), operatorDescriptor.getName()));
                            } else {
                                ToolAdapterIO.removeOperator(operatorDescriptor);
                            }
                            refreshContent();
                        }
                    }
                }));
        makeCompactGrid(panel, 4, 1, 0, 0, DEFAULT_PADDING, DEFAULT_PADDING);
        return panel;
    }

    private JPanel createButtonsPanel() {
        FlowLayout layout = new FlowLayout(FlowLayout.TRAILING);
        JPanel panel = new JPanel();

        /**
         * Execute adapter button
         */
        AbstractButton runButton = createButton("Run",
                TangoIcons.actions_media_playback_start(TangoIcons.Res.R22),
                Bundle.ToolTipExecuteOperator_Text(),
                e -> {
                    ToolAdapterOperatorDescriptor operatorDesc = requestSelection();
                    if (operatorDesc != null) {
                        //close();
                        final ToolAdapterExecutionDialog operatorDialog = new ToolAdapterExecutionDialog(
                                operatorDesc,
                                appContext,
                                operatorDesc.getLabel());
                        operatorDialog.getJDialog().setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
                        operatorDialog.show();
                    }
                });
        panel.add(runButton);
        /**
         * Create suite button
         */
        AbstractButton packButton = createButton("Pack",
                TangoIcons.apps_system_installer(TangoIcons.Res.R22),
                Bundle.ToolTipExport_Text(),
                e -> {
                    ModuleSuiteDialog dialog = new ModuleSuiteDialog(appContext, "Create Adapter Suite", null, getSelection());
                    dialog.show();
                    refreshContent();
                });
        panel.add(packButton);
        AbstractButton closeButton = createButton("Close",
                TangoIcons.actions_system_log_out(TangoIcons.Res.R22),
                null,
                e -> {
                    ToolAdaptersManagementDialog.this.onClose();
                });
        panel.add(closeButton);

        return panel;
    }

    private AbstractButton createButton(String text, ImageIcon icon, String toolTip, ActionListener actionListener) {
        AbstractButton button = StringUtils.isNullOrEmpty(text) ? new JButton(icon) : new JButton(text, icon);
        Dimension dimension = StringUtils.isNullOrEmpty(text) ?
                new Dimension(24, 24) :
                buttonDimension;
        button.setMinimumSize(dimension);
        button.setMaximumSize(dimension);
        button.setPreferredSize(dimension);
        if (toolTip != null) {
            button.setToolTipText(toolTip);
        }
        if (actionListener != null) {
            button.addActionListener(actionListener);
        }
        return button;
    }

    private JTable createPropertiesPanel() {
        DefaultTableModel model = new DefaultTableModel(1, 2) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };
        model.setValueAt(Bundle.PathLabel_Text(), 0, 0);
        model.setValueAt(ToolAdapterIO.getAdaptersPath(), 0, 1);
        model.addTableModelListener(l -> {
            String newPath = model.getValueAt(0, 1).toString();
            Path path = Paths.get(newPath);
            Path oldPath = ToolAdapterIO.getAdaptersPath();
            try {
                if (Files.isSameFile(oldPath, path)) {
                    return;
                }
            } catch (IOException ignored) {
            }
            if (!Files.exists(path) &&
                Dialogs.Answer.YES == Dialogs.requestDecision("Path does not exist", "The path you have entered does not exist.\nDo you want to create it?", true, "Don't ask me in the future")) {
                try {
                    Files.createDirectories(Files.isDirectory(path) ? path : path.getParent());
                } catch (IOException ex) {
                    Dialogs.showError("Path could not be created!");
                }
            }
            if (Files.exists(path)) {
                ToolAdapterOperatorDescriptor[] operatorDescriptors = ToolAdapterActionRegistrar.getActionMap().values()
                        .toArray(new ToolAdapterOperatorDescriptor[ToolAdapterActionRegistrar.getActionMap().values().size()]);
                for (ToolAdapterOperatorDescriptor descriptor : operatorDescriptors) {
                    ToolAdapterIO.removeOperator(descriptor, false);
                }
                AdapterWatcher.INSTANCE.unmonitorPath(oldPath);
                ToolAdapterIO.setAdaptersPath(path);
                if (!newPath.equals(oldPath.toAbsolutePath().toString())) {
                    ToolAdapterIO.searchAndRegisterAdapters();
                    refreshContent();
                }
                try {
                    AdapterWatcher.INSTANCE.monitorPath(path);
                } catch (IOException e) {
                    SystemUtils.LOG.warning(String.format("Could not watch for the new adapter path %s [%s]", path.toString(), e.getMessage()));
                }
            }
        });
        JTable table = new JTable(model);
        TableColumn labelColumn = table.getColumnModel().getColumn(0);
        labelColumn.setPreferredWidth((CHECK_COLUMN_WIDTH + LABEL_COLUMN_WIDTH)/2);
        TableColumn pathColumn = table.getColumnModel().getColumn(1);
        pathColumn.setPreferredWidth(COLUMN_WIDTH);
        pathColumn.setCellEditor(new FileChooserCellEditor());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setRowHeight(PATH_ROW_HEIGHT);
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
                .map(e -> (ToolAdapterOperatorDescriptor) e.getOperatorDescriptor())
                .collect(Collectors.toList()));
        toolboxSpis.sort((o1, o2) -> o1.getAlias().compareTo(o2.getAlias()));
        OperatorsTableModel model = new OperatorsTableModel(toolboxSpis);
        operatorsTable = new JTable(model);
        operatorsTable.getColumnModel().getColumn(0).setPreferredWidth(LABEL_COLUMN_WIDTH);
        operatorsTable.getColumnModel().getColumn(0).setMaxWidth(300);
        operatorsTable.getColumnModel().getColumn(1).setResizable(true);
        operatorsTable.getColumnModel().getColumn(1).setPreferredWidth(LABEL_COLUMN_WIDTH);
        operatorsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        operatorsTable.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    int selectedRow = operatorsTable.getSelectedRow();
                    operatorsTable.repaint();
                    ToolAdapterOperatorDescriptor operatorDesc = ((OperatorsTableModel) operatorsTable.getModel()).getObjectAt(selectedRow);
                    AbstractAdapterEditor dialog = AbstractAdapterEditor.createEditorDialog(appContext, getJDialog(), operatorDesc, OperationType.EDIT);
                    dialog.show();
                    refreshContent();
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

    private Set<ToolAdapterOperatorDescriptor> getSelection() {
        Set<ToolAdapterOperatorDescriptor> selection = new HashSet<>();
        int[] selectedRows = operatorsTable.getSelectedRows();
        for (int idx : selectedRows) {
            selection.add(((OperatorsTableModel) operatorsTable.getModel()).getObjectAt(idx));
        }
        return selection;
    }

    private ToolAdapterOperatorDescriptor requestSelection() {
        ToolAdapterOperatorDescriptor selected = null;
        int selectedRow = operatorsTable.getSelectedRow();
        if (selectedRow >= 0) {
             selected = ((OperatorsTableModel) operatorsTable.getModel()).getObjectAt(selectedRow);
        } else {
            Dialogs.showWarning(Bundle.MessageNoSelection_Text());
        }
        return selected;
    }

    private void refreshContent() {
        setContent(createContentPanel());
        getContent().repaint();
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
