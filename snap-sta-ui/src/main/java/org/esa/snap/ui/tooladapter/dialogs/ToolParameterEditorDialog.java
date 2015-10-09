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

import com.bc.ceres.binding.DefaultPropertySetDescriptor;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.converters.ArrayConverter;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.CheckBoxEditor;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.descriptor.ToolParameterDescriptor;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.tooladapter.actions.EscapeAction;
import org.esa.snap.ui.tooladapter.model.PropertyMemberUIWrapper;
import org.esa.snap.ui.tooladapter.model.PropertyMemberUIWrapperFactory;
import org.esa.snap.ui.tooladapter.validators.RequiredFieldValidator;
import org.esa.snap.ui.tooladapter.validators.TypedValueValidator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Form for displaying and editing details of a tool adapter parameter.
 *
 * @author Ramona Manda
 */
public class ToolParameterEditorDialog extends ModalDialog {

    private ToolParameterDescriptor parameter;
    private ToolParameterDescriptor oldParameter;
    private PropertyContainer container;
    private BindingContext valuesContext;
    private BindingContext paramContext;
    private PropertyMemberUIWrapper uiWrapper;
    private JComponent editorComponent;
    private JPanel mainPanel;
    public static final String helpID = "sta_editor";
    private Logger logger;

    private static final BidiMap typesMap;

    static{
        typesMap = new DualHashBidiMap();
        typesMap.put("String", String.class);
        typesMap.put("File", File.class);
        typesMap.put("Integer", Integer.class);
        typesMap.put("List", String[].class);
        typesMap.put("Boolean", Boolean.class);
    }


    public ToolParameterEditorDialog(AppContext appContext, String title, ToolParameterDescriptor parameter, Object value) throws Exception{
        super(appContext.getApplicationWindow(), parameter.getName(), ID_OK_CANCEL, helpID);
        this.oldParameter = parameter;
        this.parameter = new ToolParameterDescriptor(parameter);
        if(value != null) {
            this.parameter.setDefaultValue(value.toString());
        }
        container = PropertyContainer.createObjectBacked(parameter);
        valuesContext = new BindingContext(container);
        createContextForValueEditor();

        this.logger = Logger.getLogger(ToolAdapterEditorDialog.class.getName());
        setContent(createMainPanel());
        getJDialog().setPreferredSize(new Dimension(500, 500));
        EscapeAction.register(getJDialog());
    }

    private void createContextForValueEditor() throws Exception{
        PropertyDescriptor property = ParameterDescriptorFactory.convert(this.parameter, new ParameterDescriptorFactory().getSourceProductMap());
        DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
        try {
            property.setDefaultValue(this.parameter.getDefaultValue());
        } catch (Exception ex){
            logger.warning(ex.getMessage());
        }
        propertySetDescriptor.addPropertyDescriptor(property);
        PropertyContainer container = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
        container.getProperty(property.getName()).setValue(this.parameter.getDefaultValue());
        paramContext = new BindingContext(container);

        this.uiWrapper = PropertyMemberUIWrapperFactory.buildPropertyWrapper("defaultValue", this.parameter, null, paramContext, null);
        this.editorComponent = this.uiWrapper.getUIComponent();
    }

    public JPanel createMainPanel(){
        GridBagLayout layout = new GridBagLayout();
        layout.columnWidths = new int[]{100, 390};

        mainPanel = new JPanel(layout);

        addTextPropertyEditor(mainPanel, "Name: ", "name", parameter.getName(), 0, true);
        addTextPropertyEditor(mainPanel, "Alias: ", "alias", parameter.getAlias(), 1, true);

        //dataType
        mainPanel.add(new JLabel("Type"), getConstraints(2, 0, 1));
        JComboBox comboEditor = new JComboBox(typesMap.keySet().toArray());
        comboEditor.setSelectedItem(typesMap.getKey(parameter.getDataType()));
        comboEditor.addActionListener(ev -> {
            JComboBox cb = (JComboBox) ev.getSource();
            String typeName = (String) cb.getSelectedItem();
            if (!parameter.getDataType().equals(typesMap.get(typeName))) {
                parameter.setDataType((Class<?>) typesMap.get(typeName));
                //reset value set
                parameter.setValueSet(null);
                paramContext.getPropertySet().getProperty(parameter.getName()).getDescriptor().setValueSet(null);
                try {
                    valuesContext.getPropertySet().getProperty("valueSet").setValue(null);
                } catch (ValidationException e) {
                    logger.warning(e.getMessage());
                }
                //editor must updated
                try {
                    if(editorComponent != null) {
                        mainPanel.remove(editorComponent);
                    }
                    editorComponent = uiWrapper.reloadUIComponent((Class<?>) typesMap.get(typeName));
                    if (!("File".equals(typeName) || "List".equals(typeName))) {
                        editorComponent.setInputVerifier(new TypedValueValidator("The value entered is not of the specified data type", parameter.getDataType()));
                    }
                    mainPanel.add(editorComponent, getConstraints(3, 1, 1));
                    mainPanel.revalidate();
                } catch (Exception e) {
                    logger.warning(e.getMessage());
                    SnapDialogs.showError(e.getMessage());
                }
            }
        });
        mainPanel.add(comboEditor, getConstraints(2, 1, 1));

        //defaultValue
        mainPanel.add(new JLabel("Default value"), getConstraints(3, 0, 1));
        try {
            editorComponent = uiWrapper.getUIComponent();
            mainPanel.add(editorComponent, getConstraints(3, 1, 1));
        } catch (Exception e) {
            e.printStackTrace();
        }

        addTextPropertyEditor(mainPanel, "Description: ", "description", parameter.getDescription(), 4, false);
        addTextPropertyEditor(mainPanel, "Label: ", "label", parameter.getLabel(), 5, false);
        addTextPropertyEditor(mainPanel, "Unit: ", "unit", parameter.getUnit(), 6, false);
        addTextPropertyEditor(mainPanel, "Interval: ", "interval", parameter.getInterval(), 7, false);
        JComponent valueSetEditor =  addTextPropertyEditor(mainPanel, "Value set: ", "valueSet", StringUtils.join(parameter.getValueSet(), ArrayConverter.SEPARATOR), 8, false);
        valueSetEditor.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent ev) {
                //the value set may impact the editor
                try {
                    String newValueSet = ((JTextField) valueSetEditor).getText();
                    if (newValueSet.isEmpty()) {
                        parameter.setValueSet(null);
                        valuesContext.getPropertySet().getProperty("valueSet").setValue(null);
                    } else {
                        parameter.setValueSet(newValueSet.split(ArrayConverter.SEPARATOR));
                        valuesContext.getPropertySet().getProperty("valueSet").setValue(newValueSet.split(ArrayConverter.SEPARATOR));
                    }
                    if (editorComponent != null) {
                        mainPanel.remove(editorComponent);
                    }
                    createContextForValueEditor();
                    if (!(File.class.equals(parameter.getDataType()) || parameter.getDataType().isArray())) {
                        editorComponent.setInputVerifier(new TypedValueValidator("The value entered is not of the specified data type", parameter.getDataType()));
                    }
                    mainPanel.add(editorComponent, getConstraints(3, 1, 1));
                    mainPanel.revalidate();
                } catch (Exception e) {
                    logger.warning(e.getMessage());
                    SnapDialogs.showError(e.getMessage());
                }
            }
        });
        addTextPropertyEditor(mainPanel, "Condition: ", "condition", parameter.getCondition(), 9, false);
        addTextPropertyEditor(mainPanel, "Pattern: ", "pattern", parameter.getPattern(), 10, false);
        addTextPropertyEditor(mainPanel, "Format: ", "format", parameter.getFormat(), 11, false);
        addBoolPropertyEditor(mainPanel, "Not null", "notNull", parameter.isNotNull(), 12);
        addBoolPropertyEditor(mainPanel, "Not empty", "notEmpty", parameter.isNotEmpty(), 13);
        addTextPropertyEditor(mainPanel, "ItemAlias: ", "itemAlias", parameter.getItemAlias(), 14, false);
        addBoolPropertyEditor(mainPanel, "Deprecated", "deprecated", parameter.isDeprecated(), 15);

        return mainPanel;
    }

    private JComponent addTextPropertyEditor(JPanel parent, String label, String propertyName, String value, int line, boolean isRequired){
        parent.add(new JLabel(label), getConstraints(line, 0, 1));
        PropertyDescriptor propertyDescriptor = container.getDescriptor(propertyName);
        TextFieldEditor textEditor = new TextFieldEditor();
        JComponent editorComponent = textEditor.createEditorComponent(propertyDescriptor, valuesContext);
        ((JTextField) editorComponent).setText(value);
        if (isRequired) {
            editorComponent.setInputVerifier(new RequiredFieldValidator("This field is required"));
        }
        parent.add(editorComponent, getConstraints(line, 1, 1));
        return editorComponent;
    }

    private JComponent addBoolPropertyEditor(JPanel parent, String label, String propertyName, Boolean value, int line){
        parent.add(new JLabel(label), getConstraints(line, 1, 1));
        PropertyDescriptor propertyDescriptor = container.getDescriptor(propertyName);
        CheckBoxEditor boolEditor = new CheckBoxEditor();
        JComponent editorComponent = boolEditor.createEditorComponent(propertyDescriptor, valuesContext);
        ((JCheckBox) editorComponent).setSelected(value);
        editorComponent.setPreferredSize(new Dimension(30, 30));
        GridBagConstraints constraints = getConstraints(line, 0, 1);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor  = GridBagConstraints.LINE_END;
        parent.add(editorComponent, constraints);
        return editorComponent;
    }

    private GridBagConstraints getConstraints(int row, int col, int noCells) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = col;
        c.gridy = row;
        if(noCells != -1){
            c.gridwidth = noCells;
        }
        c.insets = new Insets(2, 10, 2, 10);
        return c;
    }

    protected void onOK(){
        super.onOK();
        if(parameter.getName() != null) {
            oldParameter.setName(parameter.getName());
        }
        if(parameter.getAlias() != null) {
            oldParameter.setAlias(parameter.getAlias());
        }
        if(parameter.getDataType() != null) {
            oldParameter.setDataType(parameter.getDataType());
        }
        if(paramContext.getBinding(parameter.getName()).getPropertyValue() != null) {
            oldParameter.setDefaultValue(paramContext.getBinding(parameter.getName()).getPropertyValue().toString());
        }
        if(parameter.getDescription() != null) {
            oldParameter.setDescription(parameter.getDescription());
        }
        if(parameter.getLabel() != null) {
            oldParameter.setLabel(parameter.getLabel());
        }
        if(parameter.getUnit() != null) {
            oldParameter.setUnit(parameter.getUnit());
        }
        if(parameter.getInterval() != null) {
            oldParameter.setInterval(parameter.getInterval());
        }
        if(parameter.getValueSet() != null) {
            oldParameter.setValueSet(parameter.getValueSet());
        }
        if(parameter.getCondition() != null) {
            oldParameter.setCondition(parameter.getCondition());
        }
        if(parameter.getPattern() != null) {
            oldParameter.setPattern(parameter.getPattern());
        }
        if(parameter.getFormat() != null) {
            oldParameter.setFormat(parameter.getFormat());
        }
        oldParameter.setNotNull(parameter.isNotNull());
        oldParameter.setNotEmpty(parameter.isNotEmpty());
        if(parameter.getRasterDataNodeClass() != null) {
            oldParameter.setRasterDataNodeClass(parameter.getRasterDataNodeClass());
        }
        if(parameter.getValidatorClass() != null) {
            oldParameter.setValidatorClass(parameter.getValidatorClass());
        }
        if(parameter.getConverterClass() != null) {
            oldParameter.setConverterClass(parameter.getConverterClass());
        }
        if(parameter.getDomConverterClass() != null) {
            oldParameter.setDomConverterClass(parameter.getDomConverterClass());
        }
        if(parameter.getItemAlias() != null) {
            oldParameter.setItemAlias(parameter.getItemAlias());
        }
        oldParameter.setDeprecated(parameter.isDeprecated());
        oldParameter.setParameterType(parameter.getParameterType());
    }

}
