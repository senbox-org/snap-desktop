package org.esa.snap.product.library.ui.v2.repository.input;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.ui.loading.SwingUtils;
import org.esa.snap.ui.loading.VerticalScrollablePanel;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The panel contains the search parameters of a repository.
 *
 * Created by jcoravu on 18/9/2019.
 */
public class ParametersPanel extends VerticalScrollablePanel {

    public ParametersPanel() {
        super(new GridBagLayout());
    }

    public List<AbstractParameterComponent<?>> addParameterComponents(List<RepositoryQueryParameter> parameters, int startRowIndex, int startGapBetweenRows,
                                                                      ComponentDimension componentDimension, Class<?>[] classesToIgnore) {

        int gapBetweenColumns = componentDimension.getGapBetweenColumns();
        int gapBetweenRows = componentDimension.getGapBetweenRows();
        int textFieldPreferredHeight = componentDimension.getTextFieldPreferredHeight();
        Color textFieldBackgroundColor = componentDimension.getTextFieldBackgroundColor();

        List<AbstractParameterComponent<?>> parameterComponents = new ArrayList<AbstractParameterComponent<?>>();

        for (int i=0; i<parameters.size(); i++) {
            RepositoryQueryParameter param = parameters.get(i);
            AbstractParameterComponent parameterComponent = null;
            if (param.getType() == String.class) {
                // the parameter type is string
                String defaultValue = (param.getDefaultValue() == null) ? null : (String)param.getDefaultValue();
                if (param.getValueSet() == null) {
                    if(param.getName().toLowerCase().contentEquals("password")){
                        parameterComponent = new SecretStringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight, textFieldBackgroundColor);
                    } else {
                        parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight, textFieldBackgroundColor);
                    }
                } else {
                    Object[] defaultValues = param.getValueSet();
                    String[] values = new String[defaultValues.length + 1];
                    for (int k=0; k<defaultValues.length; k++) {
                        values[k+1] = defaultValues[k].toString();
                    }
                    parameterComponent = new StringComboBoxParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), values, componentDimension);
                }
            } else if (param.getType() == Double.class || param.getType() == double.class || param.getType() == Float.class || param.getType() == float.class || param.getType() == Integer.class || param.getType() == int.class || param.getType() == Short.class || param.getType() == short.class ) {
                // the parameter type is number
                Number defaultValue = (param.getDefaultValue() == null) ? null : (Number)param.getDefaultValue();
                parameterComponent = new NumberParameterComponent(param.getName(), param.getType(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight, textFieldBackgroundColor);
            } else if (param.getType() == LocalDateTime.class || param.getType() == LocalDateTime[].class) {
                final DateParameterComponent dateParameterComponent = new DateParameterComponent(param.getName(), param.getLabel(), param.isRequired(), textFieldPreferredHeight, textFieldBackgroundColor);
                if (param.getName().toLowerCase().startsWith("start")) {
                    dateParameterComponent.setParameterValue(LocalDateTime.now().minusDays(1));
                } else if (param.getName().toLowerCase().startsWith("end")) {
                    dateParameterComponent.setParameterValue(LocalDateTime.now());
                }
                parameterComponent = dateParameterComponent;
            } else if (param.getType() == String[].class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : param.getDefaultValue().toString();
                if(param.getName().toLowerCase().contentEquals("password")){
                    parameterComponent = new SecretStringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight, textFieldBackgroundColor);
                }else {
                    parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight, textFieldBackgroundColor);
                }
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
