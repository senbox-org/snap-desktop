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

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.converters.ArrayConverter;
import com.bc.ceres.binding.converters.FloatConverter;
import com.bc.ceres.binding.converters.IntegerConverter;
import com.bc.ceres.swing.binding.BindingContext;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.descriptor.PropertyAttributeException;
import org.esa.snap.core.gpf.descriptor.TemplateParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolParameterDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.esa.snap.ui.tooladapter.dialogs.TemplateParameterEditorDialog;
import org.esa.snap.ui.tooladapter.dialogs.ToolParameterEditorDialog;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Table holding the operator parameter descriptors
 *
 * @author Ramona Manda
 * @author Cosmin Cara
 */
@NbBundle.Messages({
        "Column_Name_Text=Name",
        "Column_Description_Text=Description",
        "Column_Label_Text=Label",
        "Column_DataType_Text=Data type",
        "Column_DefaultValue_Text=Default value",
        "Type_TemplateFileClass_Text=Template Parameter",
        "Type_BeforeTemplateFileClass_Text=Template Before",
        "Type_AfterTemplateFileClass_Text=Template After",
        "Type_RegularFileClass_Text=File",
        "Type_FolderClass_Text=Folder",
        "Type_FileListClass_Text=File List",
        "Type_StringClass_Text=String",
        "Type_IntegerClass_Text=Integer",
        "Type_ListClass_Text=List",
        "Type_BooleanClass_Text=Boolean",
        "Type_FloatClass_Text=Decimal",
        "Type_ProductList_Text=Product List"
})
public class OperatorParametersTable extends JTable {
    private static final Logger logger = Logger.getLogger(OperatorParametersTable.class.getName());

    private static String[] columnNames = {"", "Name", "Description", "Label", "Data type", "Default value", ""};
    private static String[] columnsMembers = {"del", "name", "description", "label", "dataType", "defaultValue", "edit"};
    private static int[] widths = {27, 100, 200, 80, 100, 249, 30};
    private static final BidiMap typesMap = new DualHashBidiMap();

    static {
        typesMap.put(Bundle.Type_TemplateFileClass_Text(), CustomParameterClass.TemplateFileClass);
        typesMap.put(Bundle.Type_BeforeTemplateFileClass_Text(), CustomParameterClass.BeforeTemplateFileClass);
        typesMap.put(Bundle.Type_AfterTemplateFileClass_Text(), CustomParameterClass.AfterTemplateFileClass);
        typesMap.put(Bundle.Type_RegularFileClass_Text(), CustomParameterClass.RegularFileClass);
        typesMap.put(Bundle.Type_FolderClass_Text(), CustomParameterClass.FolderClass);
        typesMap.put(Bundle.Type_FileListClass_Text(), CustomParameterClass.FileListClass);
        typesMap.put(Bundle.Type_StringClass_Text(), CustomParameterClass.StringClass);
        typesMap.put(Bundle.Type_IntegerClass_Text(), CustomParameterClass.IntegerClass);
        typesMap.put(Bundle.Type_ListClass_Text(), CustomParameterClass.ListClass);
        typesMap.put(Bundle.Type_BooleanClass_Text(), CustomParameterClass.BooleanClass);
        typesMap.put(Bundle.Type_FloatClass_Text(), CustomParameterClass.FloatClass);
    }

    private ToolAdapterOperatorDescriptor operator = null;
    private Map<ToolParameterDescriptor, PropertyMemberUIWrapper> propertiesValueUIDescriptorMap;
    private MultiRenderer tableRenderer;
    private BindingContext context;
    private final TableCellRenderer dataTypesComboCellRenderer;
    private final AppContext appContext;
    private final DefaultTableCellRenderer labelTypeCellRenderer;

    private JComboBox cellDefaultValueComboBox;
    private JCheckBox cellDefaultValueCheckBox;
    private JScrollPane cellDefaultValueList;
    private FilePanel cellDefaultValueFile;
    private JComboBox cellDataTypesComboBox;
    private JTextField cellTextComponent;
    private JComponent currentDisplayedCellComponent;
    private int cellComponentColumnIndex;
    private int cellComponentRowIndex;

    public OperatorParametersTable(ToolAdapterOperatorDescriptor operator, AppContext appContext) {
        this.operator = operator;
        this.appContext = appContext;

        propertiesValueUIDescriptorMap = new HashMap<>();
        dataTypesComboCellRenderer = new DefaultTableCellRenderer();
        labelTypeCellRenderer = new DefaultTableCellRenderer();
        labelTypeCellRenderer.setText(Bundle.Type_ProductList_Text());

        List<ToolParameterDescriptor> data = operator.getToolParameterDescriptors();
        PropertySet propertySet = new OperatorParameterSupport(operator).getPropertySet();
        //if there is an exception in the line above, can be because the default value does not match the type
        //TODO determine if (and which) param has a wrong type
        context = new BindingContext(propertySet);
        for (ToolParameterDescriptor paramDescriptor : data) {
            if (paramDescriptor.getName().equals(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID)) {
                propertiesValueUIDescriptorMap.put(paramDescriptor, PropertyMemberUIWrapperFactory.buildEmptyPropertyWrapper());
            } else {
                propertiesValueUIDescriptorMap.put(paramDescriptor, PropertyMemberUIWrapperFactory.buildPropertyWrapper("defaultValue", paramDescriptor, operator, context, null));
            }
        }
        tableRenderer = new MultiRenderer();
        setModel(new OperatorParametersTableNewTableModel());
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < widths.length; i++) {
            getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        this.putClientProperty("JComboBox.isTableCellEditor", Boolean.FALSE);
        this.setRowHeight(20);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int clickedColumn = columnAtPoint(event.getPoint());
                int clickedRow = rowAtPoint(event.getPoint());
                if (clickedColumn >= 0 && clickedRow >= 0 && isTableCellEditable(clickedRow, clickedColumn)) {
                    mouseClickOnCell(clickedColumn, clickedRow);
                }
            }
        });

        FocusListener focusListener = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent focusEvent) {
                hideCurrentDisplayedCellComponent(focusEvent);
            }
        };

        createCellDefaultValueList(focusListener);
        createCellDefaultValueFileChooser(focusListener);
        createCellDefaultValueCheckBox(focusListener);
        createCellDefaultValueComboBox(focusListener);
        createCellTextComponent(focusListener);

        createCellDataTypesComponent();

        resetCellComponentPosition();
    }

    private void createCellDefaultValueFileChooser(FocusListener focusListener) {
        this.cellDefaultValueFile = new FilePanel();
        this.cellDefaultValueFile.setBackground(Color.white);
        this.cellDefaultValueFile.setOpaque(true);
        this.cellDefaultValueFile.setVisible(false);
        this.cellDefaultValueFile.addTextComponentFocusListener(focusListener);
        this.cellDefaultValueFile.addBrowseButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(cellComponentRowIndex);
                String parameterType = descriptor.getParameterType();
                int selectionMode = JFileChooser.FILES_ONLY;
                FileFilter filter = null;
                switch (parameterType) {
                    case ToolAdapterConstants.FOLDER_PARAM_MASK:
                        selectionMode = JFileChooser.DIRECTORIES_ONLY;
                        break;
                    case ToolAdapterConstants.TEMPLATE_BEFORE_MASK:
                    case ToolAdapterConstants.TEMPLATE_AFTER_MASK:
                        filter = new FileFilter() {
                            @Override
                            public boolean accept(File file) {
                                return file.isDirectory() || file.getName().toLowerCase().endsWith(".vm");
                            }

                            @Override
                            public String getDescription() {
                                return "*.vm files";
                            }
                        };
                        break;
                    case ToolAdapterConstants.TEMPLATE_PARAM_MASK:
                        filter = new FileFilter() {
                            @Override
                            public boolean accept(File file) {
                                return file.isDirectory() ||
                                        file.getName().toLowerCase().endsWith(".vm") ||
                                        file.getName().toLowerCase().endsWith(".xml");
                            }

                            @Override
                            public String getDescription() {
                                return "*.vm files; *.xml files";
                            }
                        };
                        break;
                }

                File selectedFile = cellDefaultValueFile.showFileChooserDialog(selectionMode, filter);
                if (selectedFile != null) {
                    descriptor.setDefaultValue(selectedFile.getAbsolutePath());
                    fireTableRowsChanged();
                }
            }
        });
    }

    private void createCellDefaultValueList(FocusListener focusListener) {
        JList list = new JList();
        list.addFocusListener(focusListener);
        this.cellDefaultValueList = new JScrollPane(list);
        this.cellDefaultValueList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        this.cellDefaultValueList.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.cellDefaultValueList.setOpaque(true);
        this.cellDefaultValueList.setVisible(false);
        this.cellDefaultValueList.setBackground(Color.white);
        this.cellDefaultValueList.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                cellDefaultValueList.getViewport().getView().requestFocusInWindow();
            }
        });
    }

    private void createCellDefaultValueCheckBox(FocusListener focusListener) {
        this.cellDefaultValueCheckBox = new JCheckBox();
        this.cellDefaultValueCheckBox.setBackground(Color.white);
        this.cellDefaultValueCheckBox.setOpaque(true);
        this.cellDefaultValueCheckBox.setVisible(false);
        this.cellDefaultValueCheckBox.addFocusListener(focusListener);
    }

    private void createCellDefaultValueComboBox(FocusListener focusListener) {
        this.cellDefaultValueComboBox = new JComboBox();
        this.cellDefaultValueComboBox.setOpaque(true);
        this.cellDefaultValueComboBox.setVisible(false);
        this.cellDefaultValueComboBox.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.cellDefaultValueComboBox.addFocusListener(focusListener);
    }

    private void createCellDataTypesComponent() {
        this.cellDataTypesComboBox = new JComboBox(typesMap.keySet().toArray());
        this.cellDataTypesComboBox.setOpaque(true);
        this.cellDataTypesComboBox.setVisible(false);
        this.cellDataTypesComboBox.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.cellDataTypesComboBox.addActionListener(ev -> {
            String typeName = (String) cellDataTypesComboBox.getSelectedItem();
            cellDataTypeChanged(typeName, this.cellComponentRowIndex, this.cellComponentColumnIndex);
        });
        this.cellDataTypesComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent focusEvent) {
                hideCurrentDisplayedCellComponent();
            }
        });
    }

    private void createCellTextComponent(FocusListener focusListener) {
        this.cellTextComponent = new JTextField();
        this.cellTextComponent.setOpaque(true);
        this.cellTextComponent.setVisible(false);
        this.cellTextComponent.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.cellTextComponent.addFocusListener(focusListener);
    }

    private void mouseClickOnCell(int clickedColumn, int clickedRow) {
        if (clickedColumn == 0) {
            if (canRemoveRow(clickedRow)) {
                removeRow(clickedRow);
            }
        }
        else if (clickedColumn == 6) {
            showToolParameterEditorDialog(clickedRow);
        } else if (clickedColumn == 4) {
            showCellDataTypeComboBoxEditor(clickedColumn, clickedRow);
        } else if (clickedColumn == 5) {
            showCellDefaultValueComponent(clickedColumn, clickedRow);
        } else if (clickedColumn == 1 || clickedColumn == 2 || clickedColumn == 3) {
            showCellTextComponentEditor(clickedColumn, clickedRow);
        }
    }

    private void showCellDefaultValueComponent(int clickedColumn, int clickedRow) {
        ToolParameterDescriptor descriptor = this.operator.getToolParameterDescriptors().get(clickedRow);
        String defaultValue = descriptor.getDefaultValue();
        if (descriptor.getDataType() == String.class || descriptor.getDataType() == Integer.class || descriptor.getDataType() == Float.class) {
            String[] valueSet = descriptor.getValueSet();
            if (valueSet != null && valueSet.length > 0) {
                DefaultComboBoxModel model = new DefaultComboBoxModel();
                Object itemToSelect = null;
                for (int i=0; i<valueSet.length; i++) {
                    model.addElement(valueSet[i]);
                    if (defaultValue != null && defaultValue.equals(valueSet[i])) {
                        itemToSelect = valueSet[i];
                    }
                }
                model.setSelectedItem(itemToSelect);
                this.cellDefaultValueComboBox.setModel(model);
                setCurrentCellComponentEditor(clickedColumn, clickedRow, this.cellDefaultValueComboBox);
            } else {
                this.cellTextComponent.setText(defaultValue);
                setCurrentCellComponentEditor(clickedColumn, clickedRow, this.cellTextComponent);
            }
        } else if (descriptor.getDataType() == Boolean.class) {
            this.cellDefaultValueCheckBox.setSelected(Boolean.parseBoolean(defaultValue));
            setCurrentCellComponentEditor(clickedColumn, clickedRow, this.cellDefaultValueCheckBox);
        } else if (descriptor.getDataType() == File.class) {
            this.cellDefaultValueFile.setText(defaultValue);
            setCurrentCellComponentEditor(clickedColumn, clickedRow, this.cellDefaultValueFile);
        } else if (descriptor.getDataType() == String[].class) {
            showCellDefaultValueList(clickedColumn, clickedRow);
        }
    }

    private void showCellDefaultValueList(int clickedColumn, int clickedRow) {
        ToolParameterDescriptor descriptor = this.operator.getToolParameterDescriptors().get(clickedRow);
        String defaultValue = descriptor.getDefaultValue();
        String[] valueSet = descriptor.getValueSet();
        JList list = (JList)this.cellDefaultValueList.getViewport().getView();
        DefaultListModel model = new DefaultListModel();
        java.util.List<Integer> selectedIndices = new ArrayList<Integer>();
        if (valueSet != null && valueSet.length > 0) {
            for (int i=0; i<valueSet.length; i++) {
                model.addElement(valueSet[i]);
            }
            String[] itemsToSelect = null;
            if (!StringUtils.isNullOrEmpty(defaultValue)) {
                itemsToSelect = defaultValue.split(ArrayConverter.SEPARATOR);
            }
            if (itemsToSelect != null && itemsToSelect.length > 0) {
                for (int i=0; i<model.getSize(); i++) {
                    Object item = model.getElementAt(i);
                    for (int k=0; k<itemsToSelect.length; k++) {
                        if (item.equals(itemsToSelect[k])) {
                            selectedIndices.add(i);
                            break;
                        }
                    }
                }
            }
        }
        list.setModel(model);
        int[] indices = new int[selectedIndices.size()];
        for (int i=0; i<selectedIndices.size(); i++) {
            indices[i] = selectedIndices.get(i).intValue();
        }
        list.setSelectedIndices(indices);

        setCurrentCellComponentEditor(clickedColumn, clickedRow, this.cellDefaultValueList);
    }

    @Override
    public void doLayout() {
        super.doLayout();

        if (hasValidCellComponent()) {
            Rectangle tableCellBounds = getCellRect(this.cellComponentRowIndex, this.cellComponentColumnIndex, false);
            this.currentDisplayedCellComponent.setBounds(tableCellBounds.x, tableCellBounds.y, tableCellBounds.width, tableCellBounds.height);
        }
    }

    private boolean hasValidCellComponent() {
        return (this.currentDisplayedCellComponent != null) && (this.cellComponentColumnIndex >= 0) && (this.cellComponentRowIndex >= 0);
    }

    private final void showCellDataTypeComboBoxEditor(int columnIndex, int rowIndex) {
        Object cellValue = getValueAt(rowIndex, columnIndex);
        String cellValueAsString = (cellValue == null) ? "" : cellValue.toString();
        ActionListener[] listeners = this.cellDataTypesComboBox.getActionListeners();

        for (int i=0; i<listeners.length; i++) {
            this.cellDataTypesComboBox.removeActionListener(listeners[i]);
        }

        this.cellDataTypesComboBox.setSelectedItem(cellValueAsString);

        for (int i=0; i<listeners.length; i++) {
            this.cellDataTypesComboBox.addActionListener(listeners[i]);
        }

        setCurrentCellComponentEditor(columnIndex, rowIndex, this.cellDataTypesComboBox);
    }

    private void showCellTextComponentEditor(int columnIndex, int rowIndex) {
        Object cellValue = getValueAt(rowIndex, columnIndex);
        String cellValueAsString = (cellValue == null) ? "" : cellValue.toString();
        this.cellTextComponent.setText(cellValueAsString);
        setCurrentCellComponentEditor(columnIndex, rowIndex, this.cellTextComponent);
    }

    private final void setCurrentCellComponentEditor(int columnIndex, int rowIndex, JComponent cellComponent) {
        if (this.currentDisplayedCellComponent != null && cellComponent != this.currentDisplayedCellComponent) {
            this.currentDisplayedCellComponent.setVisible(false);
            remove(this.currentDisplayedCellComponent);
            fireTableRowsChanged();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentDisplayedCellComponent = cellComponent;
                cellComponentColumnIndex = columnIndex;
                cellComponentRowIndex = rowIndex;

                add(currentDisplayedCellComponent);

                Rectangle tableCellBounds = getCellRect(cellComponentRowIndex, cellComponentColumnIndex, false);
                currentDisplayedCellComponent.setBounds(tableCellBounds.x, tableCellBounds.y, tableCellBounds.width, tableCellBounds.height);
                currentDisplayedCellComponent.setVisible(true);
                currentDisplayedCellComponent.requestFocusInWindow();
            }
        });
    }

    private void hideCurrentDisplayedCellComponent(FocusEvent focusEvent) {
        if (hasValidCellComponent()) {
            ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(this.cellComponentRowIndex);
            if (this.currentDisplayedCellComponent instanceof JTextField) {
                String cellText = ((JTextField)this.currentDisplayedCellComponent).getText();
                if (this.cellComponentColumnIndex == 1) {
                    setParameterNameAt(cellText, this.cellComponentRowIndex, this.cellComponentColumnIndex);
                } else if (this.cellComponentColumnIndex == 2) {
                    descriptor.setDescription(cellText);
                } else if (this.cellComponentColumnIndex == 3) {
                    descriptor.setLabel(cellText);
                } else if (this.cellComponentColumnIndex == 5) {
                    descriptor.setDefaultValue(cellText);
                }
            } else if (this.currentDisplayedCellComponent instanceof JCheckBox) {
                boolean isSelected = ((JCheckBox)this.currentDisplayedCellComponent).isSelected();
                if (this.cellComponentColumnIndex == 5) {
                    descriptor.setDefaultValue(Boolean.toString(isSelected));
                }
            } else if (this.currentDisplayedCellComponent instanceof JComboBox) {
                if (this.cellComponentColumnIndex == 5) {
                    String selectedItem = (String) ((JComboBox)this.currentDisplayedCellComponent).getSelectedItem();
                    descriptor.setDefaultValue(selectedItem);
                }
            } else if (this.currentDisplayedCellComponent instanceof FilePanel) {
                if (this.cellComponentColumnIndex == 5) {
                    String filePath = ((FilePanel)this.currentDisplayedCellComponent).getText();
                    descriptor.setDefaultValue(filePath);
                }
            } else if ((this.currentDisplayedCellComponent instanceof JList) || (this.currentDisplayedCellComponent instanceof JScrollPane)) {
                if (this.cellComponentColumnIndex == 5) {
                    JList list = null;
                    if (this.currentDisplayedCellComponent instanceof JList) {
                        list = (JList)this.currentDisplayedCellComponent;
                    } else {
                        JScrollPane scrollPane = (JScrollPane)this.currentDisplayedCellComponent;
                        list = (JList)scrollPane.getViewport().getView();
                    }
                    int[] indices = list.getSelectedIndices();
                    String defaultValue = "";
                    for (int i=0; i<indices.length; i++) {
                        if (i > 0) {
                            defaultValue += ",";
                        }
                        defaultValue += (String)list.getModel().getElementAt(indices[i]);
                    }
                    descriptor.setDefaultValue(defaultValue);
                }
            }

            hideCurrentDisplayedCellComponent();
        }
    }

    private void hideCurrentDisplayedCellComponent() {
        remove(this.currentDisplayedCellComponent);
        this.currentDisplayedCellComponent.setVisible(false);
        resetCellComponentPosition();
        requestFocusInWindow();
        this.currentDisplayedCellComponent = null;
        fireTableRowsChanged();
    }

    private void resetCellComponentPosition() {
        this.cellComponentColumnIndex = -1;
        this.cellComponentRowIndex = -1;
    }

    public void stopVariablesTableEditing() {
        if (getEditingRow() >= 0 && getEditingColumn() >= 0) {
            getCellEditor(getEditingRow(), getEditingColumn()).stopCellEditing();
        }
    }

    private void removeRow(int rowIndex) {
        ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(rowIndex);
        operator.removeParamDescriptor(descriptor);
        fireTableRowsChanged();
    }

    private boolean canRemoveRow(int rowIndex) {
        ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(rowIndex);
        String descriptorName = descriptor.getName();
        return  !ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID.equals(descriptorName) &&
                !ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE.equals(descriptorName) &&
                !ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE.equals(descriptorName);
    }

    private boolean isTableCellEditable(int rowIndex, int columnIndex) {
        ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(rowIndex);
        String descriptorName = descriptor.getName();
        if (ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID.equals(descriptorName) || ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE.equals(descriptorName)) {
            return false;
        }
        if (ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE.equals(descriptorName) && (columnIndex == 0 || columnIndex == 1 || columnIndex == 4 || columnIndex == 6)) {
            return false;
        }
        return true;
    }

    public void addParameterToTable() {
        String defaultParameterName = null;
        int count = this.operator.getToolParameterDescriptors().size();
        boolean canContinue = true;
        int index = 0;
        while (canContinue) {
            index++;
            canContinue = false;
            defaultParameterName = "parameterName" + Integer.toString(index);
            for (int i=0; i<count && !canContinue; i++) {
                ToolParameterDescriptor param = this.operator.getToolParameterDescriptors().get(i);
                if (param.getName().equals(defaultParameterName)) {
                    canContinue = true;
                }
            }
        }
        TemplateParameterDescriptor newParameter = new TemplateParameterDescriptor(defaultParameterName, String.class);
        addParameterToTable(newParameter);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int rowIndex = operator.getToolParameterDescriptors().size() - 1;
                showCellTextComponentEditor(1, rowIndex);
            }
        });
    }

    public void addParameterToTable(ToolParameterDescriptor param) {
        try {
            operator.getToolParameterDescriptors().add(param);

            PropertyDescriptor propertyDescriptor = ParameterDescriptorFactory.convert(param, new ParameterDescriptorFactory().getSourceProductMap());
            propertyDescriptor.setDefaultValue(param.getDefaultValue());
            DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
            propertySetDescriptor.addPropertyDescriptor(propertyDescriptor);
            PropertyContainer container = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
            context.getPropertySet().addProperties(container.getProperties());

            createDefaultComponent(param, propertyDescriptor);
            fireTableRowsChanged();
        } catch (Exception ex){
            logger.warning(ex.getMessage());
        }
    }

    private void rebuildEditorCell(ToolParameterDescriptor descriptor, Map<String, Object> attributes) {
        PropertySet propertySet = context.getPropertySet();
        Property actualProperty = propertySet.getProperty(descriptor.getName());
        propertySet.removeProperty(actualProperty);

        PropertyDescriptor propertyDescriptor = null;
        try {
            propertyDescriptor = ParameterDescriptorFactory.convert(descriptor, new ParameterDescriptorFactory().getSourceProductMap());
        } catch (ConversionException ex) {
            logger.warning(ex.getMessage());
        }
        propertyDescriptor.setDefaultValue(descriptor.getDefaultValue());
        if (attributes != null && attributes.size() > 0) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                propertyDescriptor.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
        propertySetDescriptor.addPropertyDescriptor(propertyDescriptor);

        PropertyContainer container = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
        Object defaultValue = null;
        if (descriptor.getDefaultValue() != null) {
            defaultValue = descriptor.getDefaultTypedValue();
        }
        try {
            container.getProperty(propertyDescriptor.getName()).setValue(defaultValue);
        } catch (ValidationException ex) {
            logger.warning(ex.getMessage());
        }
        propertySet.addProperties(container.getProperties());

        createDefaultComponent(descriptor, propertyDescriptor);

        fireTableRowsChanged();
    }

    private void createDefaultComponent(ToolParameterDescriptor descriptor, PropertyDescriptor propertyDescriptor) {
        if (descriptor.getDataType() == String.class || descriptor.getDataType() == Integer.class || descriptor.getDataType() == Float.class) {
        } else if (descriptor.getDataType() == Boolean.class) {
        } else if (descriptor.getDataType() == String[].class) {
        } else if (descriptor.getDataType() == File.class) {
        } else {
            this.propertiesValueUIDescriptorMap.put(descriptor, PropertyMemberUIWrapperFactory.buildPropertyWrapper("defaultValue", descriptor, operator, context, null));
        }
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(row);
        switch (descriptor.getName()) {
            case ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID:
            case ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE:
            case ToolAdapterConstants.TOOL_TARGET_PRODUCT_ID:
            case ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE:
                component.setBackground(Color.lightGray);
                break;
            default:
                component.setBackground(SystemColor.text);
                component.setForeground(SystemColor.textText);
                break;
        }
        return component;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        switch (column){
            case 0:
            case 5:
            case 6:
                return tableRenderer;
            case 4:
                ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(row);
                if (descriptor.getName().equals(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID)) {
                    return labelTypeCellRenderer;
                } else {
                    return dataTypesComboCellRenderer;
                }
            default:
                return super.getCellRenderer(row, column);
        }
    }

    public BindingContext getBindingContext(){
        return context;
    }

    public void showToolParameterEditorDialog(int rowIndex) {
        ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(rowIndex);
        if (!descriptor.isParameter() && descriptor.getDataType().equals(File.class)) {
            try {
                TemplateParameterEditorDialog editor = new TemplateParameterEditorDialog(appContext,(TemplateParameterDescriptor) descriptor, operator);
                int returnCode = editor.show();
                if (returnCode == AbstractDialog.ID_OK) {
                    rebuildEditorCell(descriptor, null);
                }
            }catch (Exception ex){
                ex.printStackTrace();
                Dialogs.showError(ex.getMessage());
            }
        } else {
            ToolParameterEditorDialog editor = new ToolParameterEditorDialog(appContext, this.operator, descriptor);
            int returnCode = editor.show();
            if (returnCode == AbstractDialog.ID_OK) {
                rebuildEditorCell(descriptor, null);
            }
        }
    }

    private void fireTableRowsChanged() {
        ((AbstractTableModel)getModel()).fireTableDataChanged();
    }

    public static boolean checkUniqueParameterName(ToolAdapterOperatorDescriptor operator, String parameterName, ToolParameterDescriptor descriptorToEdit) {
        int count = operator.getToolParameterDescriptors().size();
        for (int i=0; i<count; i++) {
            ToolParameterDescriptor param = operator.getToolParameterDescriptors().get(i);
            if (descriptorToEdit != param) {
                if (param.getName().equals(parameterName)) {
                    Dialogs.showInformation("Duplicate parameter name.");
                    return false;
                }
            }
        }
        return true;
    }

    private void setParameterNameAt(String parameterName, int rowIndex, int columnIndex) {
        ToolParameterDescriptor descriptor = this.operator.getToolParameterDescriptors().get(rowIndex);
        if (!checkUniqueParameterName(this.operator, parameterName, descriptor)) {
            return;
        }
        String oldName = descriptor.getName();
        PropertySet propertySet = this.context.getPropertySet();
        Property oldProperty = propertySet.getProperty(oldName);
        Object defaultValue = (oldProperty == null) ? null : oldProperty.getValue();
        descriptor.setName(parameterName);
        //since the name is changed, the context must be changed also
        propertySet.removeProperty(oldProperty);
        try {
            PropertyDescriptor propertyDescriptor = ParameterDescriptorFactory.convert(descriptor, new ParameterDescriptorFactory().getSourceProductMap());
            if (defaultValue != null) {
                String defaultValueAsString = ToolParameterEditorDialog.processDefaultValue(defaultValue);
                descriptor.setDefaultValue(defaultValueAsString);
                propertyDescriptor.setDefaultValue(defaultValue);
            }
            if (descriptor.getParameterType().equals(ToolAdapterConstants.FOLDER_PARAM_MASK)) {
                propertyDescriptor.setAttribute("directory", true);
            }
            DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
            propertySetDescriptor.addPropertyDescriptor(propertyDescriptor);
            PropertyContainer container = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
            try {
                container.setDefaultValues();
            }catch (IllegalStateException ex){
                logger.warning(ex.getMessage());
            }
            propertySet.addProperties(container.getProperties());

            createDefaultComponent(descriptor, propertyDescriptor);

            fireTableRowsChanged();
        } catch (ConversionException e) {
            logger.warning(e.getMessage());
            Dialogs.showError(e.getMessage());
        }
    }

    private void cellDataTypeChanged(Object aValue, int rowIndex, int columnIndex) {
        ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(rowIndex);
        if (descriptor.isTemplateParameter() &&
                ToolAdapterConstants.TEMPLATE_PARAM_MASK.equals(descriptor.getParameterType()) &&
                (((TemplateParameterDescriptor)descriptor).getTemplate() != null ||
                        ((TemplateParameterDescriptor)descriptor).getParameterDescriptors().stream().findFirst().isPresent())) {
            return;
        }

        CustomParameterClass customClass = (CustomParameterClass)typesMap.get(aValue);
        if (customClass == null) {
            customClass = CustomParameterClass.StringClass;
        }
        Map<String, Object> extra = null;
        if (customClass.equals(CustomParameterClass.FolderClass)) {
            extra = new HashMap<>();
            extra.put("directory", Boolean.TRUE);
        }
        descriptor.setParameterType(customClass.getTypeMask());
        if (descriptor.getDataType() != customClass.getParameterClass()) {
            descriptor.setDataType(customClass.getParameterClass());
            boolean canResetDefaultValue = true;
            if (customClass.getParameterClass() == String.class || customClass.getParameterClass() == String[].class) {
                canResetDefaultValue = false;
            } else if (customClass.getParameterClass() == Integer.class) {
                if (!StringUtils.isNullOrEmpty(descriptor.getDefaultValue())) {
                    IntegerConverter integerConverter = new IntegerConverter();
                    try {
                        Number number = integerConverter.parse(descriptor.getDefaultValue());
                        if (number != null) {
                            descriptor.setDefaultValue(number.toString());
                            canResetDefaultValue = false;
                        }
                    } catch (ConversionException e) {
                        // ignore exception and reset the default value
                    }
                }
            } else if (customClass.getParameterClass() == Float.class) {
                if (!StringUtils.isNullOrEmpty(descriptor.getDefaultValue())) {
                    FloatConverter integerConverter = new FloatConverter();
                    try {
                        Number number = integerConverter.parse(descriptor.getDefaultValue());
                        if (number != null) {
                            descriptor.setDefaultValue(number.toString());
                            canResetDefaultValue = false;
                        }
                    } catch (ConversionException e) {
                        // ignore exception and reset the default value
                    }
                }
            }
            if (canResetDefaultValue) {
                descriptor.setDefaultValue(null); // reset the default value
            }
            descriptor.setValueSet(null); // reset the value set
            rebuildEditorCell(descriptor, extra);
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    private class OperatorParametersTableNewTableModel extends AbstractTableModel {

        private OperatorParametersTableNewTableModel() {
            super();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return operator.getToolParameterDescriptors().size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(row);
            switch (column) {
                case 0:
                    return false;
                case 4:
                    String cellValue = null;
                    switch (descriptor.getName()) {
                        case ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID:
                            cellValue = Bundle.Type_ProductList_Text();
                            break;
                        case ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE:
                            cellValue = Bundle.Type_FileListClass_Text();
                            break;
                        case ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE:
                            cellValue = Bundle.Type_RegularFileClass_Text();
                            break;
                        default:
                            if (CustomParameterClass.FolderClass.equals(CustomParameterClass.getObject(descriptor.getDataType(), descriptor.getParameterType()))) {
                                cellValue = Bundle.Type_FolderClass_Text();
                            } else {
                                CustomParameterClass item = CustomParameterClass.getObject(descriptor.getDataType(), descriptor.getParameterType());
                                cellValue = (String) typesMap.getKey(item);
                            }
                            break;
                    }
                    return cellValue;
                case 5:
                    return descriptor.getDefaultValue();
                case 6:
                    return false;
                default:
                    try {
                        return descriptor.getAttribute(columnsMembers[column]);
                    } catch (PropertyAttributeException e) {
                        logger.warning(e.getMessage());
                        return String.format("Error: %s", e.getMessage());
                    }
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private class MultiRenderer extends AbstractCellEditor implements TableCellRenderer {
        private TableCellRenderer defaultRenderer;
        private AbstractButton deleteButton;
        private AbstractButton editButton;
        private JCheckBox checkBoxRenderer;
        private JTextField textComponentRenderer;

        public MultiRenderer() {
            checkBoxRenderer = new JCheckBox();
            textComponentRenderer = new JTextField();
            textComponentRenderer.setBorder(new EmptyBorder(0, 0, 0, 0));

            defaultRenderer = new DefaultTableCellRenderer();
            deleteButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/DeleteShapeTool16.gif"), false);
            editButton = new JButton("...");
            deleteButton.addActionListener(e -> fireEditingStopped());
            editButton.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(row);
            switch (column) {
                case 0:
                    return deleteButton;
                case 5:
                    String defaultValue = descriptor.getDefaultValue();
                    if (descriptor.getDataType() == Boolean.class) {
                        this.checkBoxRenderer.setSelected(Boolean.parseBoolean(defaultValue));
                        return this.checkBoxRenderer;
                    } else if (descriptor.getDataType() == String.class || descriptor.getDataType() == Integer.class || descriptor.getDataType() == Float.class) {
                        this.textComponentRenderer.setText(defaultValue);
                        return this.textComponentRenderer;
                    } else if (descriptor.getDataType() == String[].class) {
                        this.textComponentRenderer.setText(defaultValue);
                        return this.textComponentRenderer;
                    } else if (descriptor.getDataType() == File.class) {
                        this.textComponentRenderer.setText(defaultValue);
                        return this.textComponentRenderer;
                    }
                    try {
                        return propertiesValueUIDescriptorMap.get(descriptor).getUIComponent();
                    } catch (Exception e) {
                        logger.warning(e.getMessage());
                        return null;
                    }
                case 6:
                    return editButton;
                default:
                    return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }
}
