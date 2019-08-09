package org.esa.snap.product.library.ui.v2;

import javax.swing.JComponent;

/**
 * Created by jcoravu on 7/8/2019.
 */
public abstract class AbstractParameterComponent<ValueType> {

    private final String parameterName;

    protected AbstractParameterComponent(String parameterName) {
        this.parameterName = parameterName;
    }

    public abstract JComponent getComponent();

    public abstract ValueType getParameterValue();

    public String getParameterName() {
        return parameterName;
    }
}
