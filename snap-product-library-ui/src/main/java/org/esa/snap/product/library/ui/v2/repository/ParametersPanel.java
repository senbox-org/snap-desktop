package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.Box;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jcoravu on 18/9/2019.
 */
public class ParametersPanel extends JPanel {

    public ParametersPanel() {
        super(new GridBagLayout());
    }

    public List<AbstractParameterComponent<?>> addParameterComponents(List<QueryFilter> parameters, int startRowIndex, int startGapBetweenRows, ComponentDimension componentDimension, Class<?>[] classesToIgnore) {
        int gapBetweenColumns = componentDimension.getGapBetweenColumns();
        int gapBetweenRows = componentDimension.getGapBetweenRows();
        int textFieldPreferredHeight = componentDimension.getTextFieldPreferredHeight();

        List<AbstractParameterComponent<?>> parameterComponents = new ArrayList<AbstractParameterComponent<?>>();

        for (int i=0; i<parameters.size(); i++) {
            QueryFilter param = parameters.get(i);
            AbstractParameterComponent parameterComponent = null;
            if (param.getType() == String.class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : (String)param.getDefaultValue();
                if (param.getValueSet() == null) {
                    parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight);
                } else {
                    Object[] defaultValues = param.getValueSet();
                    String[] values = new String[defaultValues.length + 1];
                    for (int k=0; k<defaultValues.length; k++) {
                        values[k+1] = defaultValues[k].toString();
                    }
                    parameterComponent = new StringComboBoxParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), values, componentDimension);
                }
            } else if (param.getType() == Double.class || param.getType() == Integer.class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : param.getDefaultValue().toString();
                parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight);
            } else if (param.getType() == Date.class) {
                parameterComponent = new DateParameterComponent(param.getName(), param.getLabel(), param.isRequired(), textFieldPreferredHeight);
            } else if (param.getType() == String[].class) {
                //TODO Jean implement a specific parameter
                String defaultValue = (param.getDefaultValue() == null) ? null : param.getDefaultValue().toString();
                parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight);
            } else if (param.getType() == Object[].class) {
                parameterComponent = new ComboBoxParameterComponent(param, componentDimension);
            } else {
                boolean found = false;
                for (int k=0; k<classesToIgnore.length; k++) {
                    if (classesToIgnore[k] == param.getType()) {
                        found = true;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("Unknown parameter: name: '" + param.getName() + "', type: '" + param.getType() + "', label: '" + param.getLabel() + "'.");
                }
            }
            if (parameterComponent != null) {
                int rowIndex = startRowIndex + parameterComponents.size();
                parameterComponents.add(parameterComponent);

                int topMargin = (rowIndex == startRowIndex) ? startGapBetweenRows : gapBetweenRows;
                GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, topMargin, 0);
                add(parameterComponent.getLabel(), c);
                c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, topMargin, gapBetweenColumns);
                add(parameterComponent.getComponent(), c);
                rowIndex++;
            }
        }
        return parameterComponents;
    }
}
