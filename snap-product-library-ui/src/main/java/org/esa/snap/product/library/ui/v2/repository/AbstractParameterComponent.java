package org.esa.snap.product.library.ui.v2.repository;

import javax.swing.JComponent;
import javax.swing.JLabel;

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

    public String getParameterName() {
        return parameterName;
    }

    public JLabel getLabel() {
        return label;
    }

    public boolean isRequired() {
        return required;
    }
}
