package org.esa.snap.product.library.ui.v2.repository.input;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class StringComboBoxParameterComponent extends AbstractParameterComponent<String> {

    private final JComboBox<String> readOnlyComboBox;

    public StringComboBoxParameterComponent(String parameterName, String defaultValue, String parameterLabelText,
                                            boolean required, String[] values, ComponentDimension componentDimension) {

        super(parameterName, parameterLabelText, required);

        this.readOnlyComboBox = SwingUtils.buildComboBox(values, defaultValue, componentDimension.getTextFieldPreferredHeight(), false);
        this.readOnlyComboBox.setBackground(componentDimension.getTextFieldBackgroundColor());
    }

    @Override
    public JComponent getComponent() {
        return this.readOnlyComboBox;
    }

    @Override
    public void setParameterValue(Object value) {
        if (value == null) {
            clearParameterValue();
        } else if (value instanceof String) {
            this.readOnlyComboBox.setSelectedItem(value);
        } else {
            throw new ClassCastException("The parameter value type '" + value + "' must be '" + String.class+"'.");
        }
    }

    @Override
    public String getParameterValue() {
        return getSelectedItem();
    }

    @Override
    public void clearParameterValue() {
        this.readOnlyComboBox.setSelectedItem(null);
    }

    @Override
    public Boolean hasValidValue() {
        String value = getSelectedItem();
        if (value == null) {
            return null; // the value is not specified
        }
        return true; // the value is specified and it is valid
    }

    private String getSelectedItem() {
        return (String)this.readOnlyComboBox.getModel().getSelectedItem();
    }
}
