package org.esa.snap.product.library.ui.v2.repository.input;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class StringComboBoxParameterComponent extends AbstractParameterComponent<String> {

    private final JComboBox<String> component;

    public StringComboBoxParameterComponent(String parameterName, String defaultValue, String parameterLabelText,
                                            boolean required, String[] values, ComponentDimension componentDimension) {

        super(parameterName, parameterLabelText, required);

        this.component = SwingUtils.buildComboBox(values, defaultValue, componentDimension.getTextFieldPreferredHeight(), false);
    }

    @Override
    public JComponent getComponent() {
        return this.component;
    }

    @Override
    public void setParameterValue(Object value) {
        if (value == null) {
            clearParameterValue();
        } else if (value instanceof String) {
            this.component.setSelectedItem(value);
        } else {
            throw new ClassCastException("The parameter value type '" + value + "' must be '" + String.class+"'.");
        }
    }

    @Override
    public String getParameterValue() {
        return (String)this.component.getModel().getSelectedItem();
    }

    @Override
    public void clearParameterValue() {
        this.component.setSelectedItem(null);
    }
}
