package org.esa.snap.product.library.ui.v2.repository;

import javax.swing.*;

/**
 * Created by jcoravu on 7/8/2019.
 */
public abstract class AbstractParameterComponent<ValueType> {

    private final String parameterName;
    private final JLabel label;
    private final boolean required;

    protected AbstractParameterComponent(String parameterName, String parameterLabelText, boolean required) {
        this.parameterName = parameterName;
        this.required = required;
        this.label = new JLabel(parameterLabelText);
    }

    public abstract JComponent getComponent();

    public abstract ValueType getParameterValue();

    public abstract void clearParameterValue();

    public String getParameterName() {
        return parameterName;
    }

    public JLabel getLabel() {
        return label;
    }

    public String getRequiredErrorDialogMessage() {
        if (this.required) {
            return "The '" + this.label.getText()+"' parameter value is required.";
        }
        return null;
    }
}
