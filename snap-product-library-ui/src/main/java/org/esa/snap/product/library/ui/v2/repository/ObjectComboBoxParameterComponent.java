package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.remote.products.repository.ItemRenderer;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.ui.loading.LabelListCellRenderer;

import javax.swing.JComboBox;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class ObjectComboBoxParameterComponent extends AbstractParameterComponent<Object> {

    private final JComboBox<Object> component;
    private final ItemRenderer<Object> itemRenderer;

    public ObjectComboBoxParameterComponent(QueryFilter parameter, ComponentDimension componentDimension) {
        super(parameter.getName(), parameter.getLabel(), parameter.isRequired());

        if (parameter.getValueRenderer() == null) {
            throw new NullPointerException("The item renderer is null.");
        }
        if (parameter.getValueSet() == null) {
            throw new NullPointerException("The value set is null.");
        }
        if (parameter.getValueSet().length == 0) {
            throw new IllegalArgumentException("The value set is empty.");
        }

        this.itemRenderer = parameter.getValueRenderer();
        this.component = RemoteProductsRepositoryPanel.buildComboBox(componentDimension);
        int cellItemHeight = this.component.getPreferredSize().height;
        LabelListCellRenderer<Object> renderer = new LabelListCellRenderer<Object>(cellItemHeight) {
            @Override
            protected String getItemDisplayText(Object value) {
                return (value == null) ? " " : itemRenderer.getDisplayName(value);
            }
        };
        this.component.setRenderer(renderer);

        Object[] values = parameter.getValueSet();
        if (!isRequired()) {
            this.component.addItem(null);
        }
        for (int i = 0; i < values.length; i++) {
            this.component.addItem(values[i]);
        }
        if (parameter.getDefaultValue() != null) {
            for (int i = 0; i < values.length; i++) {
                if (parameter.getDefaultValue().equals(values[i])) {
                    this.component.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    @Override
    public JComboBox<Object> getComponent() {
        return this.component;
    }

    @Override
    public Object getParameterValue() {
        return this.component.getModel().getSelectedItem();
    }

    @Override
    public void clearParameterValue() {
        this.component.setSelectedItem(null);
    }
}
