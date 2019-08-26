package org.esa.snap.product.library.ui.v2.data.source;

import org.esa.snap.product.library.ui.v2.ComponentDimension;

import javax.swing.JComboBox;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class StringComboBoxParameterComponent extends AbstractParameterComponent<String> {

    private final JComboBox<String> component;

    public StringComboBoxParameterComponent(String parameterName, String defaultValue, String parameterLabelText,
                                            String[] values, ComponentDimension componentDimension) {

        super(parameterName, parameterLabelText);

        this.component = RemoteProductsDataSourcePanel.buildComboBox(values, defaultValue, componentDimension);
    }

    @Override
    public JComboBox<String> getComponent() {
        return this.component;
    }

    @Override
    public String getParameterValue() {
        return (String)this.component.getModel().getSelectedItem();
    }
}
