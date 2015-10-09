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
import com.bc.ceres.binding.converters.StringConverter;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.apache.commons.lang.StringUtils;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.descriptor.ParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.PropertyAttributeException;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolParameterDescriptor;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Ramona Manda
 */
public class PropertyMemberUIWrapperFactory {

    public static PropertyMemberUIWrapper buildEmptyPropertyWrapper(){
        return new PropertyMemberUIWrapper(null, null, null, null, -1, null) {
            public String getErrorValueMessage(Object value) {
                return null;
            }

            @Override
            protected void setMemberValue(Object value) throws PropertyAttributeException {     }

            @Override
            public String getMemberValue() {return null;}

            @Override
            protected JComponent buildUIComponent() {
                return new JLabel();
            }

            @Override
            protected String getValueFromUIComponent() throws PropertyAttributeException {
                return null;
            }
        };
    }

    public static PropertyMemberUIWrapper buildPropertyWrapper(String attributeName, ToolParameterDescriptor property, ToolAdapterOperatorDescriptor opDescriptor, BindingContext context, PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        switch (attributeName) {
            case "name":
                return buildNamePropertyWrapper(attributeName, property, opDescriptor, context, 100, callback);
            case "dataType":
                return buildTypePropertyWrapper(attributeName, property, opDescriptor, context, 150, callback);
            case "defaultValue":
                return buildValuePropertyEditorWrapper(attributeName, property, opDescriptor, context, 250, callback);
            default:
                break;
        }
        /*if (attributeName.equals("name")) {
            return buildNamePropertyWrapper(attributeName, property, opDescriptor, context, 100, callback);
        }
        if (attributeName.equals("dataType")) {
            return buildTypePropertyWrapper(attributeName, property, opDescriptor, context, 150, callback);
        }
        if (attributeName.equals("defaultValue")) {
            return buildValuePropertyEditorWrapper(attributeName, property, opDescriptor, context, 250, callback);
        }*/
        Method getter = null;
        try {
            getter = property.getClass().getSuperclass().getDeclaredMethod("is" + attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1));
        } catch (NoSuchMethodException ignored) {
        }
        Object value = null;
        try {
            value = property.getAttribute(attributeName);
        } catch (Exception ignored) {
        }
        //TODO class/superclass!
        if (getter != null || (value != null && value.getClass().getSuperclass().equals(Boolean.class))) {
            return buildBooleanPropertyWrapper(attributeName, property, opDescriptor, context, 30, callback);
        }
        try {
            getter = property.getClass().getSuperclass().getDeclaredMethod("get" + attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1));
        } catch (NoSuchMethodException ignored) {
        }
        if (getter != null && getter.getReturnType().equals(String.class)) {
            return buildStringPropertyWrapper(attributeName, property, opDescriptor, context, 100, callback);
        }
        if (attributeName.equals("valueRange") || attributeName.equals("pattern") || attributeName.equals("valueSet")) {
            return buildStringPropertyWrapper(attributeName, property, opDescriptor, context, 150, callback);
        }
        return buildEmptyWrapper(attributeName, property, opDescriptor, context, 100, callback);
    }

    private static Object parseAttributeValue(String attributeName, String value, ToolParameterDescriptor property) throws PropertyAttributeException {
        if (value == null || value.length() == 0) {
            return null;
        }
        Method getter = null;
        try {
            getter = property.getClass().getSuperclass().getDeclaredMethod("get" + attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1));
            if (getter != null && getter.getReturnType().equals(String.class)) {
                return value;
            }
            if (getter != null && getter.getReturnType().equals(String[].class)) {
                Object[] items = ValueSet.parseValueSet(value, String.class).getItems();
                String[] result = new String[items.length];
                for (int i = 0; i < items.length; i++) {
                    result[i] = items[i].toString();
                }
                return result;
            }
            if (getter != null && getter.getReturnType().equals(ValueRange.class)) {
                return ValueRange.parseValueRange(value);
            }
            if (getter != null && getter.getReturnType().equals(Pattern.class)) {
                return Pattern.compile(value);
            }
        } catch (Exception ex) {
            throw new PropertyAttributeException("Error on parsing the value " + value + " in order to set the value for attribute " + attributeName);
        }
        return value;
    }

    private static PropertyMemberUIWrapper buildStringPropertyWrapper(String attributeName, ToolParameterDescriptor property, ToolAdapterOperatorDescriptor opDescriptor, BindingContext context, int width, PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        return new PropertyMemberUIWrapper(attributeName, property, opDescriptor, context, width, callback) {
            public String getErrorValueMessage(Object value) {
                return null;
            }

            @Override
            protected void setMemberValue(Object value) throws PropertyAttributeException {
                property.setAttribute(attributeName, value);
            }

            @Override
            public String getMemberValue() throws PropertyAttributeException {
                Object obj = property.getAttribute(attributeName);
                if (obj == null) {
                    return "";
                }
                if (obj instanceof String[]) {
                    return StringUtils.join(((String[]) obj), ArrayConverter.SEPARATOR);
                }
                return obj.toString();
            }

            @Override
            protected JComponent buildUIComponent() throws Exception {
                JTextField field = new JTextField(getMemberValue());
                return field;
            }

            @Override
            protected Object getValueFromUIComponent() throws PropertyAttributeException {
                return PropertyMemberUIWrapperFactory.parseAttributeValue(attributeName, ((JTextField) UIComponent).getText(), property);
            }
        };
    }

    private static PropertyMemberUIWrapper buildBooleanPropertyWrapper(String attributeName, ToolParameterDescriptor property, ToolAdapterOperatorDescriptor opDescriptor, BindingContext context, int width, PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        return new PropertyMemberUIWrapper(attributeName, property, opDescriptor, context, width, callback) {

            @Override
            protected void setMemberValue(Object value) throws PropertyAttributeException {
                property.setAttribute(attributeName, value);
            }

            @Override
            public Boolean getMemberValue() throws PropertyAttributeException {
                Object obj = property.getAttribute(attributeName);
                if (obj == null) {
                    return false;
                }
                return (boolean) obj;
            }

            @Override
            protected JComponent buildUIComponent() throws PropertyAttributeException {
                JCheckBox button = new JCheckBox();
                button.setSelected(getMemberValue());
                return button;
            }

            @Override
            protected Boolean getValueFromUIComponent() throws PropertyAttributeException {
                return ((JCheckBox) UIComponent).isSelected();
            }
        };
    }

    private static PropertyMemberUIWrapper buildNamePropertyWrapper(String attributeName, ToolParameterDescriptor property, ToolAdapterOperatorDescriptor opDescriptor, BindingContext context, int width, PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        return new PropertyMemberUIWrapper(attributeName, property, opDescriptor, context, width, callback) {
            public String getErrorValueMessage(Object value) {
                if (value == null || !(value instanceof String) || ((String) value).length() == 0) {
                    return "Name of the property cannot be empty and must be a string!";
                }
                if (property.getName().equals(value)) {
                    return null;
                }
                //check if there is any other property with the same name, it should not!
                for (ParameterDescriptor prop : opDescriptor.getParameterDescriptors()) {
                    if (prop != property && prop.getName().equals((value))) {
                        return "The operator must not have more then one parameter with the same name!";
                    }
                }
                return null;
            }

            @Override
            protected void setMemberValue(Object value) throws PropertyAttributeException {
                property.setName(value.toString());
            }

            @Override
            public String getMemberValue() {
                return property.getName();
            }

            @Override
            protected JComponent buildUIComponent() {
                JTextField field = new JTextField(getMemberValue());
                return field;
            }

            @Override
            protected String getValueFromUIComponent() throws PropertyAttributeException {
                return ((JTextField) UIComponent).getText();
            }
        };
    }

    private static PropertyMemberUIWrapper buildTypePropertyWrapper(String attributeName, ToolParameterDescriptor property, ToolAdapterOperatorDescriptor opDescriptor, BindingContext context, int width, PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        return new PropertyMemberUIWrapper(attributeName, property, opDescriptor, context, width, callback) {
            public String getErrorValueMessage(Object value) {
                if (value == null) {
                    return "Type of the property cannot be empty!";
                }
                return null;
            }

            @Override
            protected void setMemberValue(Object value) throws PropertyAttributeException {
                property.setDataType((Class<?>) value);
            }

            @Override
            public Class<?> getMemberValue() {
                return property.getDataType();
            }

            @Override
            protected JComponent buildUIComponent() {
                JTextField field = new JTextField(getMemberValue().getCanonicalName());
                return field;
            }

            @Override
            public boolean propertyUIComponentsNeedsRevalidation() {
                return true;
            }

            @Override
            protected Class<?> getValueFromUIComponent() throws PropertyAttributeException {
                try {
                    return Class.forName(((JTextField) UIComponent).getText());
                } catch (ClassNotFoundException ex) {
                    throw new PropertyAttributeException("Type of the property not found in the libraries");
                }
            }
        };
    }

    private static PropertyMemberUIWrapper buildValuePropertyEditorWrapper(String attributeName, ToolParameterDescriptor property, ToolAdapterOperatorDescriptor opDescriptor, BindingContext context, int width, PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        return new PropertyMemberUIWrapper(attributeName, property, opDescriptor, context, width, callback) {
            public String getErrorValueMessage(Object value) {
                return null;
            }

            @Override
            public void setMemberValue(Object value) throws PropertyAttributeException {
            }

            public JComponent reloadUIComponent(Class<?> newParamType) throws Exception{
                property.setDataType(newParamType);
                Property oldProp = context.getPropertySet().getProperty(property.getName());
                if(oldProp != null) {
                    context.getPropertySet().removeProperty(oldProp);
                }

                PropertyDescriptor descriptor;
                try {
                    descriptor = ParameterDescriptorFactory.convert(property, new ParameterDescriptorFactory().getSourceProductMap());
                } catch (Exception ex) {
                    property.setDefaultValue(null);
                    descriptor = ParameterDescriptorFactory.convert(property, new ParameterDescriptorFactory().getSourceProductMap());
                }
                descriptor.setDefaultConverter();
                try {
                    descriptor.setDefaultValue(property.getDefaultValue());
                } catch (Exception ex) {
                    Logger.getLogger(PropertyMemberUIWrapper.class.getName()).warning(ex.getMessage());
                }
                try {
                    descriptor.setValueSet(ValueSet.parseValueSet(property.getValueSet(), new StringConverter()));
                } catch (Exception ex) {
                    Logger.getLogger(PropertyMemberUIWrapper.class.getName()).warning(ex.getMessage());
                }
                DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
                propertySetDescriptor.addPropertyDescriptor(descriptor);
                PropertyContainer container = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
                context.getPropertySet().addProperties(container.getProperties());
                return super.reloadUIComponent(newParamType);
            }

            @Override
            public Object getMemberValue() {
                return property.getDefaultValue();
            }

            @Override
            protected JComponent buildUIComponent() {
                PropertyDescriptor propertydescriptor = context.getPropertySet().getDescriptor(property.getName());
                PropertyEditor propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(
                        propertydescriptor);
                JComponent editorComponent = propertyEditor.createEditorComponent(propertydescriptor,
                        context);
                return editorComponent;
            }

            @Override
            protected Object getValueFromUIComponent() throws PropertyAttributeException {
                return null;
            }

            @Override
            public void focusLost(FocusEvent e){}
        };
    }

    public static PropertyMemberUIWrapper buildEmptyWrapper(String attributeName, ToolParameterDescriptor property, ToolAdapterOperatorDescriptor opDescriptor, BindingContext context, int width, PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        return new PropertyMemberUIWrapper(attributeName, property, opDescriptor, context, width, callback) {
            @Override
            public String getErrorValueMessage(Object value) {
                return null;
            }

            @Override
            protected void setMemberValue(Object value) throws PropertyAttributeException {
            }

            @Override
            public Object getMemberValue() {
                return null;
            }

            @Override
            protected JComponent buildUIComponent() {
                return new JLabel("");
            }

            @Override
            protected Object getValueFromUIComponent() {
                return null;
            }
        };
    }

}
