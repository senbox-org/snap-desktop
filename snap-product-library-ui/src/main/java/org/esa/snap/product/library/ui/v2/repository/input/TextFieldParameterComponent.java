package org.esa.snap.product.library.ui.v2.repository.input;

import javax.swing.*;
import java.awt.*;

/**
 * Created by jcoravu on 7/8/2019.
 */
public abstract class TextFieldParameterComponent<ValueType> extends AbstractParameterComponent<ValueType> {

    protected final JTextField textField;

    protected TextFieldParameterComponent(String parameterName, String parameterLabelText, boolean required, int textFieldPreferredHeight, Color backgroundColor) {
        super(parameterName, parameterLabelText, required);

        this.textField = new JTextField();
        this.textField.setBackground(backgroundColor);

        Dimension preferredSize = this.textField.getPreferredSize();
        preferredSize.height = textFieldPreferredHeight;
        this.textField.setPreferredSize(preferredSize);
        this.textField.setMinimumSize(preferredSize);
    }

    @Override
    public JComponent getComponent() {
        return this.textField;
    }

    @Override
    public void clearParameterValue() {
        this.textField.setText("");
    }

    protected final String getText() {
        return this.textField.getText().trim();
    }
}
