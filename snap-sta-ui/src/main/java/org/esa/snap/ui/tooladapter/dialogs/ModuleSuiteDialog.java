/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
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
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.ui.tooladapter.dialogs;

import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterRegistry;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.modules.ModulePackager;
import org.esa.snap.modules.ModuleSuiteDescriptor;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.tooladapter.actions.EscapeAction;
import org.esa.snap.ui.tooladapter.model.ProgressWorker;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog for creating a module suite nbm package
 *
 * @author  Cosmin Cara
 */
public class ModuleSuiteDialog extends ModalDialog {

    private static final int YEAR;
    private JTable operatorsTable;
    private ModuleSuiteDescriptor descriptor;
    private org.esa.snap.core.gpf.descriptor.dependency.Bundle bundle;
    private EntityForm<ModuleSuiteDescriptor> descriptorForm;
    private EntityForm<org.esa.snap.core.gpf.descriptor.dependency.Bundle> bundleForm;
    private Set<ToolAdapterOperatorDescriptor> initialSelection;

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        YEAR = calendar.get(Calendar.YEAR);
    }

    ModuleSuiteDialog(AppContext appContext, String title, String helpID, Set<ToolAdapterOperatorDescriptor> selection) {
        super(appContext.getApplicationWindow(), title, ID_OK | ID_CANCEL, helpID);
        this.descriptor = new ModuleSuiteDescriptor();
        this.descriptor.setAuthors(System.getProperty("user.name"));
        this.descriptor.setVersion("1");
        this.descriptor.setCopyright("(C)" + String.valueOf(YEAR) + " " + this.descriptor.getAuthors());
        this.bundle = new org.esa.snap.core.gpf.descriptor.dependency.Bundle();
        this.initialSelection = new HashSet<>();
        if (selection != null) {
            this.initialSelection.addAll(selection);
        }
        JPanel contentPanel = createContentPanel();
        setContent(contentPanel);
        super.getJDialog().setMinimumSize(contentPanel.getPreferredSize());
        EscapeAction.register(super.getJDialog());
    }

    private JPanel createContentPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        JPanel panel = new JPanel(layout);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Suite description:"), constraints);
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Adapters to include:"), constraints);

        JPanel descriptorPanel = createDescriptorPanel();
        descriptorPanel.setPreferredSize(new Dimension(300, 250));
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(descriptorPanel, constraints);
        JTable adaptersTable = createAdaptersTable();
        JScrollPane scrollPane = new JScrollPane(adaptersTable);
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, constraints);

        JPanel bundlePanel = createBundlePanel();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(bundlePanel, constraints);

        panel.setPreferredSize(new Dimension(550, 350));
        return panel;
    }

    private JTable createAdaptersTable() {
        java.util.List<ToolAdapterOperatorDescriptor> toolboxSpis = new ArrayList<>();
        toolboxSpis.addAll(ToolAdapterRegistry.INSTANCE.getOperatorMap().values()
                .stream()
                .map(e -> (ToolAdapterOperatorDescriptor) e.getOperatorDescriptor())
                .collect(Collectors.toList()));
        toolboxSpis.sort(Comparator.comparing(ToolAdapterOperatorDescriptor::getAlias));
        Object[][] records = new Object[toolboxSpis.size()][2];
        for (int i = 0; i < toolboxSpis.size(); i++) {
            records[i][0] = false;
            records[i][1] = toolboxSpis.get(i);
        }
        AdapterListModel model = new AdapterListModel(toolboxSpis);
        operatorsTable = new JTable(model);
        TableColumn checkColumn = operatorsTable.getColumnModel().getColumn(0);
        int checkColumnWidth = 24;
        checkColumn.setMaxWidth(checkColumnWidth);
        checkColumn.setPreferredWidth(checkColumnWidth);
        checkColumn.setResizable(false);
        TableColumn aliasColumn = operatorsTable.getColumnModel().getColumn(1);
        aliasColumn.setPreferredWidth(10 * checkColumnWidth);
        return operatorsTable;
    }

    private JPanel createDescriptorPanel() {
        this.descriptorForm = new EntityForm<>(this.descriptor);
        JPanel panel = descriptorForm.getPanel();
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return panel;
    }

    private JPanel createBundlePanel() {
        this.bundleForm = new EntityForm<>(this.bundle);
        JPanel panel = this.bundleForm.getPanel();
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return panel;
    }

    @Override
    protected void onOK() {
        ToolAdapterOperatorDescriptor[] selection = ((AdapterListModel) this.operatorsTable.getModel()).getSelectedItems();
        if (selection.length > 0) {
            this.descriptor = this.descriptorForm.applyChanges();
            this.bundle = this.bundleForm.applyChanges();
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(getButton(ID_OTHER)) == JFileChooser.APPROVE_OPTION) {
                File targetFolder = fileChooser.getSelectedFile();
                final String nbmName = this.descriptor.getName() + ".nbm";
                ProgressWorker worker = new ProgressWorker("Export Module Suite", "Creating NetBeans module suite " + nbmName,
                        () -> {
                            try {
                                ModulePackager.packModules(this.descriptor, new File(targetFolder, nbmName), this.bundle, selection);
                                Dialogs.showInformation(String.format(Bundle.MSG_Export_Complete_Text(), targetFolder.getAbsolutePath()), null);
                            } catch (IOException e) {
                                SystemUtils.LOG.warning(e.getMessage());
                                Dialogs.showError(e.getMessage());
                            }
                        });
                worker.executeWithBlocking();
                super.onOK();
            }
        } else {
            Dialogs.showWarning("Please select at least one adapter");
        }
    }

    private class AdapterListModel extends AbstractTableModel {
        private Object[][] checkedList;
        private String[] columnNames;

        AdapterListModel(List<ToolAdapterOperatorDescriptor> descriptors) {
            super();
            this.checkedList = new Object[descriptors.size()][2];
            for (int i = 0; i < descriptors.size(); i++) {
                ToolAdapterOperatorDescriptor descriptor = descriptors.get(i);
                this.checkedList[i][0] = ModuleSuiteDialog.this.initialSelection.contains(descriptor);
                this.checkedList[i][1] = descriptor;
            }
            this.columnNames = new String[] { "", "Adapter Alias" };
        }

        @Override
        public String getColumnName(int column) {
            return this.columnNames[column];
        }

        @Override
        public int getRowCount() {
            return this.checkedList != null ? this.checkedList.length : 0;
        }

        @Override
        public int getColumnCount() {
            return columnNames != null ? columnNames.length : 0;
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return this.checkedList[row][0];
                case 1:
                    return ((ToolAdapterOperatorDescriptor) this.checkedList[row][1]).getAlias();
                default:
                    return null;
            }
        }

        ToolAdapterOperatorDescriptor[] getSelectedItems() {
            List<ToolAdapterOperatorDescriptor> selection = new ArrayList<>();
            for (Object[] aCheckedList : this.checkedList) {
                if (aCheckedList[0] == Boolean.TRUE) {
                    selection.add((ToolAdapterOperatorDescriptor) aCheckedList[1]);
                }
            }
            return selection.toArray(new ToolAdapterOperatorDescriptor[selection.size()]);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 0)
                return;
            this.checkedList[rowIndex][0] = aValue;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Boolean.class;
                case 1:
                    return String.class;
                default:
                    return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 1;
        }
    }
}
