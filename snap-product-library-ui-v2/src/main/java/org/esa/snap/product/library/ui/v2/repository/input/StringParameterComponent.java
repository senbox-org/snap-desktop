package org.esa.snap.product.library.ui.v2.repository.input;

import javax.swing.*;
import java.awt.*;

/**
 * The text field parameter component allows the user to enter a value for the search parameter.
 *
 * Created by jcoravu on 7/8/2019.
 */
public class StringParameterComponent extends TextFieldParameterComponent<String> {

    public StringParameterComponent(String parameterName, String defaultValue, String parameterLabelText, boolean required, int textFieldPreferredHeight, Color backgroundColor) {
        super(parameterName, parameterLabelText, required, textFieldPreferredHeight, backgroundColor);

        if (defaultValue != null) {
            this.textField.setText(defaultValue);
        }
    }

    @Override
    public String getParameterValue() {
        String value = getText();
        return (value.length() > 0 ? value : null);
    }

    @Override
    public void setParameterValue(Object value) {
        if (value == null) {
            clearParameterValue();
        } else if (value instanceof String) {
            this.textField.setText((String) value);
        } else {
            throw new ClassCastException("The parameter value type '" + value + "' must be '" + String.class + "'.");
        }
    }

    @Override
    public Boolean hasValidValue() {
        String value = getText();
        if (value.length() > 0) {
            return true; // the value is specified and it is valid
        }
        return null; // the value is not specified
    }
}
