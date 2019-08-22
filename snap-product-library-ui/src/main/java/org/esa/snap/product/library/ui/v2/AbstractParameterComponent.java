package org.esa.snap.product.library.ui.v2;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Created by jcoravu on 7/8/2019.
 */
public abstract class AbstractParameterComponent<ValueType> {

    private final String parameterName;
    private final JLabel label;

    protected AbstractParameterComponent(String parameterName, String parameterLabelText) {
        this.parameterName = parameterName;
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
}
