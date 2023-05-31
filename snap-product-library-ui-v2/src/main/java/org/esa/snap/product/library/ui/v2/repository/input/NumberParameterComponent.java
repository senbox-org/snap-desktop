package org.esa.snap.product.library.ui.v2.repository.input;

import java.awt.*;

/**
 * The text field parameter component allows the user to enter a number for the search parameter.
 *
 * Created by jcoravu on 17/2/2020.
 */
public class NumberParameterComponent extends TextFieldParameterComponent<Number> {

    private final Class<?> parameterType;

    public NumberParameterComponent(String parameterName, Class<?> parameterType, Number defaultValue, String parameterLabelText, boolean required, int textFieldPreferredHeight, Color backgroundColor) {
        super(parameterName, parameterLabelText, required, textFieldPreferredHeight, backgroundColor);

        this.parameterType = parameterType;
        if (defaultValue != null) {
            this.textField.setText(defaultValue.toString());
        }
    }

    @Override
    public Boolean hasValidValue() {
        String value = getText();
        if (value.length() > 0) {
            // the value is specified
            try {
                convertValue(value);
                // the value is valid
                return true;
            } catch (NumberFormatException exception) {
                return false; // the value is invalid
            }
        }
        // the value is not specified
        return null;
    }

    @Override
    public String getInvalidValueErrorDialogMessage() {
        return "The '" + getLabel().getText()+"' parameter value '"+getText()+"' is invalid.";
    }

    @Override
    public Number getParameterValue() {
        String value = getText();
        if (value.length() > 0) {
            return convertValue(value);
        }
        return null;
    }

    @Override
    public void setParameterValue(Object value) {
        if (value == null) {
            clearParameterValue();
        } else if (value instanceof Number) {
            this.textField.setText(value.toString());
        } else {
            throw new ClassCastException("The parameter value type '" + value + "' must be '" + Number.class + "'.");
        }
    }

    private Number convertValue(String value) {
        if (this.parameterType == Double.class || this.parameterType == double.class) {
            return Double.parseDouble(value);
        }
        if (this.parameterType == Float.class || this.parameterType == float.class) {
            return Float.parseFloat(value);
        }
        if (this.parameterType == Short.class || this.parameterType == short.class) {
            return Short.parseShort(value);
        }
        if (this.parameterType == Integer.class || this.parameterType == int.class) {
            return Integer.parseInt(value);
        }
        throw new IllegalStateException("Unknown parameter type '" + this.parameterType + "'.");
    }
}
