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
import com.bc.ceres.swing.binding.BindingContext;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.descriptor.*;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.esa.snap.ui.tooladapter.dialogs.TemplateParameterEditorDialog;
import org.esa.snap.ui.tooladapter.dialogs.ToolParameterEditorDialog;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;
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
        "Type_FileListClass_Text=File List",
        "Type_StringClass_Text=String",
        "Type_IntegerClass_Text=Integer",
        "Type_ListClass_Text=List",
        "Type_BooleanClass_Text=Boolean",
        "Type_FloatClass_Text=Decimal",
        "Type_ProductList_Text=Product List"
})
public class OperatorParametersTable extends JTable {

    private static String[] columnNames = {"", "Name", "Description", "Label", "Data type", "Default value", ""};
    private static String[] columnsMembers = {"del", "name", "description", "alias", "dataType", "defaultValue", "edit"};
    private static int[] widths = {27, 100, 200, 80, 100, 249, 30};
    private static final BidiMap typesMap;
    private ToolAdapterOperatorDescriptor operator = null;
    private Map<ToolParameterDescriptor, PropertyMemberUIWrapper> propertiesValueUIDescriptorMap;
    private MultiRenderer tableRenderer;
    private BindingContext context;
    private DefaultCellEditor comboCellEditor;
    private TableCellRenderer comboCellRenderer;
    private AppContext appContext;
    private DefaultTableCellRenderer labelTypeCellRenderer = new DefaultTableCellRenderer();
    private Logger logger;

    static{
        typesMap = new DualHashBidiMap();
        typesMap.put(Bundle.Type_TemplateFileClass_Text(), CustomParameterClass.TemplateFileClass);
        typesMap.put(Bundle.Type_BeforeTemplateFileClass_Text(), CustomParameterClass.BeforeTemplateFileClass);
        typesMap.put(Bundle.Type_AfterTemplateFileClass_Text(), CustomParameterClass.AfterTemplateFileClass);
        typesMap.put(Bundle.Type_RegularFileClass_Text(), CustomParameterClass.RegularFileClass);
        typesMap.put(Bundle.Type_FileListClass_Text(), CustomParameterClass.FileListClass);
        typesMap.put(Bundle.Type_StringClass_Text(), CustomParameterClass.StringClass);
        typesMap.put(Bundle.Type_IntegerClass_Text(), CustomParameterClass.IntegerClass);
        typesMap.put(Bundle.Type_ListClass_Text(), CustomParameterClass.ListClass);
        typesMap.put(Bundle.Type_BooleanClass_Text(), CustomParameterClass.BooleanClass);
        typesMap.put(Bundle.Type_FloatClass_Text(), CustomParameterClass.FloatClass);
    }

    public OperatorParametersTable(ToolAdapterOperatorDescriptor operator, AppContext appContext) {
        logger = Logger.getLogger(OperatorParametersTable.class.getName());
        this.operator = operator;
        this.appContext = appContext;
        propertiesValueUIDescriptorMap = new HashMap<>();
        JComboBox typesComboBox = new JComboBox(typesMap.keySet().toArray());
        comboCellEditor = new DefaultCellEditor(typesComboBox);
        comboCellRenderer = new DefaultTableCellRenderer();
        labelTypeCellRenderer.setText(Bundle.Type_ProductList_Text());

        List<TemplateParameterDescriptor> data = operator.getToolParameterDescriptors();
        PropertySet propertySet = new OperatorParameterSupport(operator).getPropertySet();
        //if there is an exception in the line above, can be because the default value does not match the type
        //TODO determine if (and which) param has a wrong type
        context = new BindingContext(propertySet);
        for (ToolParameterDescriptor paramDescriptor : data) {
            if(paramDescriptor.getName().equals(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID)){
                propertiesValueUIDescriptorMap.put(paramDescriptor, PropertyMemberUIWrapperFactory.buildEmptyPropertyWrapper());
            } else {
                propertiesValueUIDescriptorMap.put(paramDescriptor, PropertyMemberUIWrapperFactory.buildPropertyWrapper("defaultValue", paramDescriptor, operator, context, null));
            }
        }
        tableRenderer = new MultiRenderer();
        setModel(new OperatorParametersTableNewTableModel());
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for(int i=0; i < widths.length; i++) {
            getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        }

        this.putClientProperty("JComboBox.isTableCellEditor", Boolean.FALSE);
        this.setRowHeight(20);
    }

    public void addParameterToTable(TemplateParameterDescriptor param){
        try {
            PropertyDescriptor property =  ParameterDescriptorFactory.convert(param, new ParameterDescriptorFactory().getSourceProductMap());
            operator.getToolParameterDescriptors().add(param);
            DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
            try {
                property.setDefaultValue(param.getDefaultValue());
            } catch (Exception ex){
                logger.warning(ex.getMessage());
            }
            propertySetDescriptor.addPropertyDescriptor(property);
            PropertyContainer container = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
            context.getPropertySet().addProperties(container.getProperties());
            propertiesValueUIDescriptorMap.put(param, PropertyMemberUIWrapperFactory.buildPropertyWrapper("defaultValue", param, operator, context, null));
            revalidate();
        } catch (Exception ex){
            logger.warning(ex.getMessage());
        }
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
                if(descriptor.getName().equals(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID)){
                    return labelTypeCellRenderer;
                } else {
                    return comboCellRenderer;
                }
            default:
                return super.getCellRenderer(row, column);
        }
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        switch (column){
            case 0:
            case 5:
            case 6:
                return tableRenderer;
            case 4:
                return comboCellEditor;
            default:
                return getDefaultEditor(String.class);
        }
    }

    public BindingContext getBindingContext(){
        return context;
    }

    public boolean editCellAt(int row, int column){
        return super.editCellAt(row, column);
    }

    class OperatorParametersTableNewTableModel extends AbstractTableModel {

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
                    if(descriptor.getName().equals(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID)){
                        return Bundle.Type_ProductList_Text();
                    } else if (descriptor.getName().equals(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE)) {
                        return Bundle.Type_FileListClass_Text();
                    } else {
                        return typesMap.getKey(CustomParameterClass.getObject(descriptor.getDataType(), descriptor.getParameterType()));
                    }
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
            ToolParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(rowIndex);
            final String descriptorName = descriptor.getName();
            final Class<?> dataType = descriptor.getDataType();
            return  !ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID.equals(descriptorName) &&
                    !ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE.equals(descriptorName) &&
                    !(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE.equals(descriptorName) && (columnIndex == 0 || columnIndex == 1 || columnIndex == 4 || columnIndex == 6)) &&
                    !(dataType.isArray() && columnIndex > 4);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            TemplateParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(rowIndex);
            switch (columnIndex) {
                case 0:
                    operator.removeParamDescriptor(descriptor);
                    revalidate();
                    break;
                case 1:
                    String oldName = descriptor.getName();
                    descriptor.setName(aValue.toString());
                    //since the name is changed, the context must be changed also
                    context.getPropertySet().removeProperty(context.getPropertySet().getProperty(oldName));
                    try {
                        PropertyDescriptor property =  ParameterDescriptorFactory.convert(descriptor, new ParameterDescriptorFactory().getSourceProductMap());
                        try {
                            property.setDefaultValue(descriptor.getDefaultValue());
                        }catch (Exception ex){
                            logger.warning(ex.getMessage());
                        }
                        DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
                        propertySetDescriptor.addPropertyDescriptor(property);
                        PropertyContainer container = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
                        context.getPropertySet().addProperties(container.getProperties());
                        propertiesValueUIDescriptorMap.put(descriptor, PropertyMemberUIWrapperFactory.buildPropertyWrapper("defaultValue", descriptor, operator, context, null));
                        revalidate();
                        repaint();
                    } catch (ConversionException e) {
                        logger.warning(e.getMessage());
                        SnapDialogs.showError(e.getMessage());
                    }
                    break;
                case 4:
                    //type editing
                    CustomParameterClass customClass = (CustomParameterClass)typesMap.get(aValue);
                    if (customClass == null) {
                        customClass = CustomParameterClass.StringClass;
                    }
                    descriptor.setParameterType(customClass.getTypeMask());
                    if(descriptor.getDataType() != customClass.getParameterClass()) {
                        descriptor.setDataType(customClass.getParameterClass());
                        descriptor.setDefaultValue(descriptor.getDefaultValue());
                        rebuildEditorCell(descriptor);
                    }
                    break;
                case 5:
                    //the custom editor should handle this
                    break;
                case 6:
                    //edit details
                    int returnCode = -1;
                    if(!descriptor.isParameter() && descriptor.getDataType().equals(File.class)){
                        try {
                            TemplateParameterEditorDialog editor = new TemplateParameterEditorDialog(appContext, "", descriptor, propertiesValueUIDescriptorMap.get(descriptor), operator);
                            returnCode = editor.show();
                        }catch (Exception ex){
                            SnapDialogs.showError(ex.getMessage());
                        }
                    } else {
                        Object value = getBindingContext().getBinding(descriptor.getName()).getPropertyValue();
                        try {
                            ToolParameterEditorDialog editor = new ToolParameterEditorDialog(appContext, "Parameter editor for " + descriptor.getName(), descriptor, value);
                            returnCode = editor.show();
                        }catch (Exception ex){
                            logger.warning(ex.getMessage());
                            SnapDialogs.showError("Could not edit parameter " + descriptor.getName() + " : " + ex.getMessage());
                        }
                    }
                    if(returnCode == AbstractDialog.ID_OK){
                        rebuildEditorCell(descriptor);
                    }
                    break;
                default:
                    try {
                        descriptor.setAttribute(columnsMembers[columnIndex], aValue == null ? null : aValue.toString());
                    } catch (PropertyAttributeException e) {
                        logger.warning(e.getMessage());
                    }
            }
        }
    }

    private void rebuildEditorCell(TemplateParameterDescriptor descriptor){

        context.getPropertySet().removeProperty(context.getPropertySet().getProperty(descriptor.getName()));
        PropertyDescriptor property;
        try {
            try {
                property =  ParameterDescriptorFactory.convert(descriptor, new ParameterDescriptorFactory().getSourceProductMap());
            } catch (Exception ex){
                logger.warning(ex.getMessage());
                descriptor.setDefaultValue("");
                property =  ParameterDescriptorFactory.convert(descriptor, new ParameterDescriptorFactory().getSourceProductMap());
            }
            try {
                property.setDefaultValue(descriptor.getDefaultValue());
            } catch (Exception ex){
                logger.warning(ex.getMessage());
                property.setDefaultValue("");
            }
            DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
            propertySetDescriptor.addPropertyDescriptor(property);
            PropertyContainer container = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
            try {
                container.getProperty(property.getName()).setValue(descriptor.getDefaultValue());
            } catch (Exception ex){
                logger.warning(ex.getMessage());
                try {
                    container.getProperty(property.getName()).setValue("");
                } catch (Exception exx){}
            }
            context.getPropertySet().addProperties(container.getProperties());
            propertiesValueUIDescriptorMap.put(descriptor, PropertyMemberUIWrapperFactory.buildPropertyWrapper("defaultValue", descriptor, operator, context, null));

            revalidate();
            repaint();
        } catch (ConversionException e) {
            logger.warning(e.getMessage());
            SnapDialogs.showError(e.getMessage());
        }
    }


    class MultiRenderer extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
        private TableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
        private AbstractButton delButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/DeleteShapeTool16.gif"),
                false);
        private AbstractButton editButton = new JButton("...");

        public MultiRenderer() {
            delButton.addActionListener(e -> fireEditingStopped());
            editButton.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            ParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(row);
            switch (column) {
                case 0:
                    return delButton;
                case 5:
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
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            ParameterDescriptor descriptor = operator.getToolParameterDescriptors().get(row);
            switch (column) {
                case 0:
                    return delButton;
                case 5:
                    try {
                        return propertiesValueUIDescriptorMap.get(descriptor).getUIComponent();
                    } catch (Exception e) {
                        logger.warning(e.getMessage());
                        return null;
                    }
                case 6:
                    return editButton;
                default:
                    return getDefaultEditor(String.class).getTableCellEditorComponent(table, value, isSelected, row, column);
            }
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }
}
