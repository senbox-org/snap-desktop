package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;

import javax.swing.JComboBox;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class StringComboBoxParameterComponent extends AbstractParameterComponent<String> {

    private final JComboBox<String> component;

    public StringComboBoxParameterComponent(String parameterName, String defaultValue, String parameterLabelText,
                                            boolean required, String[] values, ComponentDimension componentDimension) {

        super(parameterName, parameterLabelText, required);

        this.component = RemoteProductsRepositoryPanel.buildComboBox(values, defaultValue, componentDimension);
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
