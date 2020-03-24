package org.esa.snap.product.library.ui.v2.repository.input;

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

    public abstract void setParameterValue(Object value);

    public abstract Boolean hasValidValue();

    public String getParameterName() {
        return parameterName;
    }

    public JLabel getLabel() {
        return label;
    }

    public String getRequiredValueErrorDialogMessage() {
        if (this.required) {
            return "The '" + this.label.getText()+"' parameter value is required.";
        }
        return null;
    }

    public String getInvalidValueErrorDialogMessage() {
        return null;
    }
}
