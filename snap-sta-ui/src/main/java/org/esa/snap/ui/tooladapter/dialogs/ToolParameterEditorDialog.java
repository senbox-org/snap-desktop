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

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.converters.ArrayConverter;
import com.bc.ceres.binding.converters.FloatConverter;
import com.bc.ceres.binding.converters.IntegerConverter;
import com.bc.ceres.binding.converters.NumberConverter;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.internal.*;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolParameterDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.tooladapter.actions.EscapeAction;
import org.esa.snap.ui.tooladapter.model.OperatorParametersTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Form for displaying and editing details of a tool adapter parameter.
 *
 * @author Ramona Manda
 */
public class ToolParameterEditorDialog extends ModalDialog {
    private static final Logger logger = Logger.getLogger(ToolAdapterEditorDialog.class.getName());

    public static final String helpID = "sta_editor";

    private static final Map<String, Class<?>> typesMap = new LinkedHashMap<String, Class<?>>();
    static {
        typesMap.put("String", String.class);
        typesMap.put("File", File.class);
        typesMap.put("Folder", File.class);
        typesMap.put("Integer", Integer.class);
        typesMap.put("Decimal", Float.class);
        typesMap.put("List", String[].class);
        typesMap.put("Boolean", Boolean.class);
    }

    private ToolParameterDescriptor parameter;
    private ToolParameterDescriptor oldParameter;
    private PropertyContainer container;
    private BindingContext valuesContext;
    private BindingContext paramContext;
    private JComponent defaultValueComponent;
    private JPanel mainPanel;
    private JTextField valueSetTextComponent;
    private final ToolAdapterOperatorDescriptor operator;

    public ToolParameterEditorDialog(AppContext appContext, ToolAdapterOperatorDescriptor operator, ToolParameterDescriptor inputParameter) {
        super(appContext.getApplicationWindow(), inputParameter.getName(), ID_OK_CANCEL, helpID);

        this.operator = operator;
        this.oldParameter = inputParameter;

        this.parameter = new ToolParameterDescriptor(inputParameter);
        this.parameter.setDeprecated(inputParameter.isDeprecated()); // copy the value

        this.container = PropertyContainer.createObjectBacked(this.parameter);
        this.valuesContext = new BindingContext(this.container);

        addComponents();

        EscapeAction.register(getJDialog());
    }

    @Override
    protected void onOK() {
        if (!OperatorParametersTable.checkUniqueParameterName(this.operator, parameter.getName(), this.oldParameter)) {
            return;
        }

        super.onOK();

        oldParameter.setName(parameter.getName());
        oldParameter.setAlias(parameter.getAlias());
        oldParameter.setDataType(parameter.getDataType());
        Object defaultValue = getProperty().getValue();
        String defaultValueAsString = processDefaultValue(defaultValue);
        oldParameter.setDefaultValue(defaultValueAsString);
        oldParameter.setDescription(parameter.getDescription());
        oldParameter.setLabel(parameter.getLabel());
        oldParameter.setUnit(parameter.getUnit());
        oldParameter.setInterval(parameter.getInterval());
        oldParameter.setValueSet(parameter.getValueSet());
        oldParameter.setCondition(parameter.getCondition());
        oldParameter.setPattern(parameter.getPattern());
        oldParameter.setFormat(parameter.getFormat());
        oldParameter.setNotNull(parameter.isNotNull());
        oldParameter.setNotEmpty(parameter.isNotEmpty());
        oldParameter.setRasterDataNodeClass(parameter.getRasterDataNodeClass());
        oldParameter.setValidatorClass(parameter.getValidatorClass());
        oldParameter.setConverterClass(parameter.getConverterClass());
        oldParameter.setDomConverterClass(parameter.getDomConverterClass());
        oldParameter.setItemAlias(parameter.getItemAlias());
        oldParameter.setDeprecated(parameter.isDeprecated());
        oldParameter.setParameterType(parameter.getParameterType());
    }

    private void addComponents() {
        GridBagLayout layout = new GridBagLayout();
        layout.columnWidths = new int[]{100, 390};

        this.mainPanel = new JPanel(layout);

        addTextPropertyEditor(mainPanel, "Name: ", "name", 0, "The 'Name' field is required.");
        addTextPropertyEditor(mainPanel, "Alias: ", "alias", 1, "The 'Alias' field is required.");

        String itemNameToSelect = null;
        if (this.parameter.getDataType() == File.class) {
            itemNameToSelect = "File";
            if (this.parameter.getDefaultValue() != null) {
                File f = new File(this.parameter.getDefaultValue());
                if (f.isDirectory()) {
                    itemNameToSelect = "Folder";
                }
            }
        } else {
            Iterator<Map.Entry<String, Class<?>>> it = typesMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Class<?>> entry = it.next();
                if (entry.getValue() == this.parameter.getDataType()) {
                    itemNameToSelect = entry.getKey();
                    break;
                }
            }
        }
        //dataType
        JComboBox comboEditor = new JComboBox(typesMap.keySet().toArray());
        comboEditor.setSelectedItem(itemNameToSelect);
        comboEditor.addActionListener(ev -> {
            JComboBox cb = (JComboBox) ev.getSource();
            String selectedTypeName = (String) cb.getSelectedItem();
            Class<?> selectedTypeClass = typesMap.get(selectedTypeName);
            if (!parameter.getDataType().equals(selectedTypeClass)) {
                Class<?> previousTypeClass = parameter.getDataType();
                parameter.setDataType(selectedTypeClass);
                try {
                    valuesContext.getPropertySet().getProperty("defaultValue").setValue(null); // reset the default value
                } catch (ValidationException e) {
                    logger.warning(e.getMessage());
                }
                Object defaultValue = getProperty().getValue();
                String defaultValueAsString = processDefaultValue(defaultValue);

                boolean canResetValueSet = true;
                if (selectedTypeClass == String.class || selectedTypeClass == String[].class) {
                    canResetValueSet = false;
                    if (previousTypeClass == Float.class) {
                        defaultValueAsString = null; // reset the default value
                    }
                } else if (selectedTypeClass == Integer.class) {
                    if (previousTypeClass == Float.class || previousTypeClass == String.class || previousTypeClass == String[].class) {
                        if (canConvertArrayToNumber(new IntegerConverter(), parameter.getValueSet())) {
                            canResetValueSet = false;
                            if (previousTypeClass == Float.class) {
                                defaultValueAsString = null; // reset the default value
                            }
                        }
                    }
                } else if (selectedTypeClass == Float.class) {
                    if (previousTypeClass == Integer.class || previousTypeClass == String.class || previousTypeClass == String[].class) {
                        if (canConvertArrayToNumber(new FloatConverter(), parameter.getValueSet())) {
                            canResetValueSet = false;
                        }
                    }
                }
                if (canResetValueSet) {
                    try {
                        valuesContext.getPropertySet().getProperty("valueSet").setValue(null); // reset the value set
                    } catch (ValidationException e) {
                        logger.warning(e.getMessage());
                    }
                }

                newDataTypeSelected(selectedTypeName, selectedTypeClass, defaultValueAsString);
            }
        });

        this.mainPanel.add(new JLabel("Data type"), getConstraints(2, 0, 1));
        this.mainPanel.add(comboEditor, getConstraints(2, 1, 1));

        addTextPropertyEditor(mainPanel, "Description: ", "description", 4, null);
        addTextPropertyEditor(mainPanel, "Label: ", "label", 5, null);
        addTextPropertyEditor(mainPanel, "Unit: ", "unit", 6, null);
        addTextPropertyEditor(mainPanel, "Interval: ", "interval", 7, null);

        this.valueSetTextComponent = new JTextField();
        ValidateTextComponentAdapter adapter = new ValidateTextComponentAdapter(this.valueSetTextComponent) {
            @Override
            protected boolean validateText(String valueSetToValidate) {
                return validateValueSetText(valueSetToValidate);
            }
        };
        addTextPropertyEditor(mainPanel, adapter, "Value set: ", "valueSet", 8);

        addTextPropertyEditor(mainPanel, "Condition: ", "condition", 9, null);
        addTextPropertyEditor(mainPanel, "Pattern: ", "pattern", 10, null);
        addTextPropertyEditor(mainPanel, "Format: ", "format", 11, null);
        addBoolPropertyEditor(mainPanel, "Not null", "notNull", 12);
        addBoolPropertyEditor(mainPanel, "Not empty", "notEmpty", 13);
        addTextPropertyEditor(mainPanel, "ItemAlias: ", "itemAlias", 14, null);
        addBoolPropertyEditor(mainPanel, "Deprecated", "deprecated", 15);

        //defaultValue
        JLabel label = new JLabel("Default value");
        label.setPreferredSize(new Dimension(150, 35));
        this.mainPanel.add(label, getConstraints(3, 0, 1));
        newDataTypeSelected(itemNameToSelect, this.parameter.getDataType(), this.parameter.getDefaultValue());

        setContent(this.mainPanel);
    }

    private boolean validateDefaultValueText(String textToValidate) {
        if (!StringUtils.isNullOrEmpty(textToValidate)) {
            if (this.parameter.getDataType() == Integer.class) {
                try {
                    Integer.parseInt(textToValidate);
                } catch (NumberFormatException ex) {
                    Dialogs.showError("Failed to convert '" + textToValidate + "' to integer number.");
                    return false;
                }
            } else if (this.parameter.getDataType() == Float.class) {
                try {
                    Float.parseFloat(textToValidate);
                } catch (NumberFormatException ex) {
                    Dialogs.showError("Failed to convert '" + textToValidate + "' to decimal number.");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validateValueSetText(String valueSetToValidate) {
        String[] valueSet = null;
        if (!StringUtils.isNullOrEmpty(valueSetToValidate)) {
            valueSet = valueSetToValidate.split(ArrayConverter.SEPARATOR);
        }
        if (this.parameter.getDataType() == Integer.class) {
            String wrongValue = null;
            Integer[] numbers = null;
            if (valueSet != null && valueSet.length > 0) {
                numbers = new Integer[valueSet.length];
                IntegerConverter integerConverter = new IntegerConverter();
                for (int i = 0; i < valueSet.length && wrongValue == null; i++) {
                    try {
                        numbers[i] = integerConverter.parse(valueSet[i]);
                        if (numbers[i] == null) {
                            wrongValue = ""; // an empty value
                        }
                    } catch (ConversionException ex) {
                        wrongValue = valueSet[i];
                    }
                }
            }
            if (wrongValue == null) {
                populateDefaultValueComponent(numbers, null);
            } else {
                Dialogs.showError("Failed to convert '" + wrongValue + "' to integer number.");
                return false;
            }
        } else if (this.parameter.getDataType() == Float.class) {
            String wrongValue = null;
            Float[] numbers = null;
            if (valueSet != null && valueSet.length > 0) {
                numbers = new Float[valueSet.length];
                FloatConverter floatConverter = new FloatConverter();
                for (int i = 0; i < valueSet.length && wrongValue == null; i++) {
                    try {
                        numbers[i] = floatConverter.parse(valueSet[i]);
                        if (numbers[i] == null) {
                            wrongValue = ""; // an empty value
                        }
                    } catch (ConversionException ex) {
                        wrongValue = valueSet[i];
                    }
                }
            }
            if (wrongValue == null) {
                populateDefaultValueComponent(numbers, null);
            } else {
                Dialogs.showError("Failed to convert '" + wrongValue + "' to decimal number.");
                return false;
            }
        } else if (this.parameter.getDataType() == String.class) {
            populateDefaultValueComponent(valueSet, null);
        } else if (this.parameter.getDataType() == String[].class) {
            populateListComponent(valueSet);
        } else {
            throw new IllegalArgumentException("Unknown parameter data type '" + this.parameter.getDataType().getName() + "'.");
        }
        return true;
    }

    private void populateDefaultValueComponent(Object[] valueSet, Object valueToSelect) {
        if (valueSet == null || valueSet.length == 0) {
            removeDefaultValueComponent();
            createDefaultValueTextComponent();
            addDefaultValueComponent();
        } else {
            if (!(defaultValueComponent instanceof JComboBox)) {
                removeDefaultValueComponent();
                createDefaultValueComboBoxComponent();
                addDefaultValueComponent();
            }
            populateDefaultValueComboBoxComponent(valueSet, valueToSelect);
        }
    }

    private void createDefaultValueTextComponent() {
        this.defaultValueComponent = new JTextField();
        ValidateTextComponentAdapter adapter = new ValidateTextComponentAdapter((JTextField)this.defaultValueComponent) {
            @Override
            protected boolean validateText(String textToValidate) {
                return validateDefaultValueText(textToValidate);
            }
        };
        PropertyDescriptor descriptor = getProperty().getDescriptor();
        this.paramContext.bind(descriptor.getName(), adapter);
    }

    private void createDefaultValueComboBoxComponent() {
        PropertyDescriptor descriptor = getProperty().getDescriptor();
        SingleSelectionEditor singleSelectionEditor = new SingleSelectionEditor();
        this.defaultValueComponent = singleSelectionEditor.createEditorComponent(descriptor, this.paramContext);
    }

    private void createDefaultValueComponent(Object[] valueSet, Object valueToSelect) {
        if (valueSet == null || valueSet.length == 0) {
            createDefaultValueTextComponent();
        } else {
            createDefaultValueComboBoxComponent();
        }
    }

    private void addDefaultValueComponent() {
        this.mainPanel.add(this.defaultValueComponent, getConstraints(3, 1, 1));
        this.mainPanel.revalidate();
    }

    private void removeDefaultValueComponent() {
        if (this.defaultValueComponent != null) {
            Property property = getProperty();
            Binding binding = this.paramContext.getBinding(property.getName());
            binding.getComponentAdapter().unbindComponents();

            this.mainPanel.remove(this.defaultValueComponent);
        }
    }

    private void populateDefaultValueComboBoxComponent(Object[] valueSet, Object valueToSelect) {
        JComboBox comboBox = (JComboBox)defaultValueComponent;
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        if (valueSet != null && valueSet.length > 0) {
            Object itemToSelect = null;
            for (int i=0; i<valueSet.length; i++) {
                model.addElement(valueSet[i]);
                if (valueToSelect != null && valueToSelect.equals(valueSet[i])) {
                    itemToSelect = valueSet[i];
                }
            }
            model.setSelectedItem(itemToSelect);
        }
        comboBox.setModel(model);
    }

    private void populateListComponent(String[] valueSet) {
        JScrollPane scrollPane = (JScrollPane)this.defaultValueComponent;
        JList list = (JList)scrollPane.getViewport().getView();
        DefaultListModel model = new DefaultListModel();
        if (valueSet != null) {
            for (int i=0; i<valueSet.length; i++) {
                model.addElement(valueSet[i]);
            }
        }
        list.setModel(model);
    }

    private Property getProperty() {
        Property[] properties = this.paramContext.getPropertySet().getProperties();
        return properties[0];
    }

    private void newDataTypeSelected(String typeName, Class<?> typeClass, String defaultValue) {
        removeDefaultValueComponent();

        // recreate context of the default value
        try {
            if (this.paramContext != null) {
                // remove the old properties
                PropertySet propertySet = this.paramContext.getPropertySet();
                Property property = getProperty();
                propertySet.removeProperty(property);
            }
            PropertyDescriptor propertyDescriptor = ParameterDescriptorFactory.convert(this.parameter, new ParameterDescriptorFactory().getSourceProductMap());

            DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
            propertySetDescriptor.addPropertyDescriptor(propertyDescriptor);
            PropertyContainer paramContainer = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
            this.paramContext = new BindingContext(paramContainer);
        } catch (ConversionException e) {
            logger.warning(e.getMessage());
        }

        boolean enabled = false;
        String parameterType = ToolAdapterConstants.REGULAR_PARAM_MASK;
        if (typeClass == Boolean.class) {
            changeBooleanDataType(defaultValue);
        } else if (typeClass == String.class) {
            enabled = true;
            changeStringDataType(defaultValue);
        } else if (typeClass == String[].class) {
            enabled = true;
            changeListDataType(defaultValue);
        } else if (typeName.equals("File")) {
            changeFileDataType(defaultValue);
        } else if (typeName.equals("Folder")) {
            parameterType = ToolAdapterConstants.FOLDER_PARAM_MASK;
            changeFolderDataType(defaultValue);
        } else if (typeClass == Integer.class) {
            enabled = true;
            changeIntegerDataType(defaultValue);
        } else if (typeClass == Float.class) {
            enabled = true;
            changeFloatDataType(defaultValue);
        } else {
            throw new IllegalArgumentException("Unknown type name '"+ typeName+"' and type class '" + typeClass.getName() + "'.");
        }
        this.parameter.setParameterType(parameterType);

        this.valueSetTextComponent.setEnabled(enabled);

        addDefaultValueComponent();
    }

    private void changeBooleanDataType(String defaultValue) {
        Property property = getProperty();
        ValueSet valueSet = new ValueSet(new Object[]{true, false});
        property.getDescriptor().setValueSet(valueSet);
        boolean isSelected = Boolean.parseBoolean(defaultValue);
        try {
            property.setValue(isSelected);
        } catch (ValidationException e) {
            logger.warning(e.getMessage());
        }
        CheckBoxEditor checkBoxEditor = new CheckBoxEditor();
        defaultValueComponent = checkBoxEditor.createEditorComponent(property.getDescriptor(), this.paramContext);
        defaultValueComponent.setBorder(new EmptyBorder(1, 0, 1, 0));
    }

    private void changeListDataType(String defaultValue) {
        String[] valueSet = null;
        if (!StringUtils.isNullOrEmpty(defaultValue)) {
            valueSet = defaultValue.split(ArrayConverter.SEPARATOR);
        }
        try {
            getProperty().setValue(valueSet);
        } catch (ValidationException e) {
            logger.warning(e.getMessage());
        }
        MultiSelectionEditor multiSelectionEditor = new MultiSelectionEditor();
        this.defaultValueComponent = multiSelectionEditor.createEditorComponent(getProperty().getDescriptor(), this.paramContext);
        JScrollPane scrollPane = (JScrollPane)this.defaultValueComponent;
        JList list = (JList)scrollPane.getViewport().getView();
        list.setVisibleRowCount(2);
        if (valueSet != null && valueSet.length > 0) {
            java.util.List<Integer> selectedIndices = new ArrayList<Integer>();
            for (int i=0; i<list.getModel().getSize(); i++) {
                Object item = list.getModel().getElementAt(i);
                for (int k=0; k<valueSet.length; k++) {
                    if (item.equals(valueSet[k])) {
                        selectedIndices.add(i);
                        break;
                    }
                }
            }
            int[] indices = new int[selectedIndices.size()];
            for (int i=0; i<selectedIndices.size(); i++) {
                indices[i] = selectedIndices.get(i).intValue();
            }
            list.setSelectedIndices(indices);
        }
    }

    private void changeStringDataType(String defaultValue) {
        try {
            getProperty().setValue(defaultValue);
        } catch (ValidationException e) {
            logger.warning(e.getMessage());
        }
        String[] valueSet = this.valuesContext.getPropertySet().getProperty("valueSet").getValue();
        createDefaultValueComponent(valueSet, defaultValue);
    }

    private void changeFolderDataType(String defaultValue) {
        File file = null;
        if (!StringUtils.isNullOrEmpty(defaultValue)) {
            file = new File(defaultValue);
        }
        Property property = getProperty();
        try {
            property.setValue(file);
        } catch (ValidationException e) {
            logger.warning(e.getMessage());
        }
        DirectoryEditor folderEditor = new DirectoryEditor();
        this.defaultValueComponent = folderEditor.createEditorComponent(property.getDescriptor(), this.paramContext);
    }

    private void changeFileDataType(String defaultValue) {
        File file = null;
        if (!StringUtils.isNullOrEmpty(defaultValue)) {
            file = new File(defaultValue);
        }
        Property property = getProperty();
        try {
            property.setValue(file);
        } catch (ValidationException e) {
            logger.warning(e.getMessage());
        }
        FileEditor fileEditor = new FileEditor();
        this.defaultValueComponent = fileEditor.createEditorComponent(property.getDescriptor(), this.paramContext);
    }

    private void changeIntegerDataType(String defaultValue) {
        changeNumberDataType(new IntegerConverter(), defaultValue);
    }

    private void changeFloatDataType(String defaultValue) {
        changeNumberDataType(new FloatConverter(), defaultValue);
    }

    private <NumberType extends Number> void changeNumberDataType(NumberConverter<NumberType> converter, String defaultValue) {
        String[] valueSet = this.valuesContext.getPropertySet().getProperty("valueSet").getValue();
        Number[] numbers = null;
        if (valueSet != null && valueSet.length > 0) {
            numbers = new Number[valueSet.length];
            for (int i=0; i<valueSet.length; i++) {
                try {
                    numbers[i] = converter.parse(valueSet[i]);
                } catch (ConversionException ex) {
                    // ignore exception
                }
            }
        }
        Number defaultNumber = null;
        if (!StringUtils.isNullOrEmpty(defaultValue)) {
            try {
                defaultNumber = converter.parse(defaultValue);
            } catch (ConversionException e) {
                logger.warning(e.getMessage());
            }
        }
        try {
            getProperty().setValue(defaultNumber);
        } catch (ValidationException e) {
            logger.warning(e.getMessage());
        }
        createDefaultValueComponent(numbers, defaultNumber);
    }

    private JComponent addTextPropertyEditor(JPanel parent, String label, String propertyName, int line, String requiredMessage) {
        PropertyDescriptor propertyDescriptor = this.container.getDescriptor(propertyName);
        JTextField editorComponent = new JTextField();
        ComponentAdapter adapter = null;
        if (StringUtils.isNullOrEmpty(requiredMessage)) {
            adapter = new TextComponentAdapter(editorComponent);
        } else {
            adapter = new RequiredTextComponentAdapter(editorComponent, requiredMessage);
        }
        this.valuesContext.bind(propertyDescriptor.getName(), adapter);

        parent.add(new JLabel(label), getConstraints(line, 0, 1));
        parent.add(editorComponent, getConstraints(line, 1, 1));

        return editorComponent;
    }

    private JComponent addTextPropertyEditor(JPanel parent, ComponentAdapter adapter, String label, String propertyName, int line) {
        JComponent editorComponent = adapter.getComponents()[0];
        PropertyDescriptor propertyDescriptor = this.container.getDescriptor(propertyName);
        this.valuesContext.bind(propertyDescriptor.getName(), adapter);

        parent.add(new JLabel(label), getConstraints(line, 0, 1));
        parent.add(editorComponent, getConstraints(line, 1, 1));

        return editorComponent;
    }

    private JComponent addBoolPropertyEditor(JPanel parent, String label, String propertyName, int line) {
        PropertyDescriptor propertyDescriptor = this.container.getDescriptor(propertyName);
        CheckBoxEditor boolEditor = new CheckBoxEditor();
        JCheckBox checkBoxComponent = (JCheckBox)boolEditor.createEditorComponent(propertyDescriptor, valuesContext);
        checkBoxComponent.setBorder(new EmptyBorder(1, 0, 1, 0));

        parent.add(new JLabel(label), getConstraints(line, 0, 1));
        parent.add(checkBoxComponent, getConstraints(line, 1, 1));

        return checkBoxComponent;
    }

    private static GridBagConstraints getConstraints(int row, int col, int noCells) {
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

    public static String processDefaultValue(Object defaultValue) {
        String defaultValueAsString = null;
        if (defaultValue != null) {
            if (defaultValue.getClass().isArray()) {
                Object[] array = (Object[])defaultValue;
                defaultValueAsString = "";
                for (int i=0; i<array.length; i++) {
                    if (i > 0) {
                        defaultValueAsString += ",";
                    }
                    defaultValueAsString += array[i].toString();
                }
            } else {
                defaultValueAsString = defaultValue.toString();
            }
        }
        return defaultValueAsString;
    }

    private static <NumberType extends Number> boolean canConvertArrayToNumber(NumberConverter<NumberType> converter, String[] valueSet) {
        if (valueSet != null && valueSet.length > 0) {
            for (int i=0; i<valueSet.length; i++) {
                try {
                    NumberType number = converter.parse(valueSet[i]);
                    if (number == null) {
                        return false;
                    }
                } catch (ConversionException e) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
