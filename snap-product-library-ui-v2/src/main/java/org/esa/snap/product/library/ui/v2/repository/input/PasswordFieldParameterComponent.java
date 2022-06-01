package org.esa.snap.product.library.ui.v2.repository.input;

import javax.swing.*;
import java.awt.*;

/**
 * The text field parameter component allows the user to enter a value for the search parameter.
 *
 * Created by jcoravu on 7/8/2019.
 */
public abstract class PasswordFieldParameterComponent<ValueType> extends AbstractParameterComponent<ValueType> {

    protected final CustomPasswordField passwordField;

    protected PasswordFieldParameterComponent(String parameterName, String parameterLabelText, boolean required, int textFieldPreferredHeight, Color backgroundColor) {
        super(parameterName, parameterLabelText, required);

        this.passwordField = new CustomPasswordField(textFieldPreferredHeight, backgroundColor);
    }

    @Override
    public JComponent getComponent() {
        return this.passwordField;
    }

    @Override
    public void clearParameterValue() {
        this.passwordField.setText("");
    }

    protected final String getPassword() {
        return new String(this.passwordField.getPassword()).trim();
    }
}
