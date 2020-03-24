package org.esa.snap.product.library.ui.v2.repository.local;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.input.AbstractParameterComponent;
import org.esa.snap.product.library.v2.database.AttributeFilter;
import org.esa.snap.product.library.v2.database.AttributeValueFilter;
import org.esa.snap.ui.loading.CustomComboBox;
import org.esa.snap.ui.loading.ItemRenderer;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 18/9/2019.
 */
public class AttributesParameterComponent extends AbstractParameterComponent<List<AttributeFilter>> {

    private static final AttributeValueFilter EQUAL_VALUE_FILTER = new AttributeValueFilter() {
        @Override
        public boolean matches(String attributeValue, String valueToCheck) {
            return attributeValue.equalsIgnoreCase(valueToCheck);
        }
    };
    private static final AttributeValueFilter CONTAINS_VALUE_FILTER = new AttributeValueFilter() {
        @Override
        public boolean matches(String attributeValue, String valueToCheck) {
            return StringUtils.containsIgnoreCase(attributeValue, valueToCheck);
        }
    };

    private final JComboBox<String> attributeNamesComboBox;
    private final JComboBox<String> attributeValuesEditableComboBox;
    private final JComboBox<AttributeValueFilter> filtersComboBox;
    private final JButton addAttributeButton;
    private final JButton removeAttributeButton;
    private final JList<AttributeFilter> attributesList;
    private final JPanel component;
    private final Map<AttributeValueFilter, String> atributeFiltersMap;

    public AttributesParameterComponent(JComboBox<String> attributesComboBox, JComboBox<String> attributeValuesEditableComboBox, String parameterName,
                                        String parameterLabelText, boolean required, ComponentDimension componentDimension) {

        super(parameterName, parameterLabelText, required);

        this.attributeNamesComboBox = attributesComboBox;

        this.attributeValuesEditableComboBox = attributeValuesEditableComboBox;

        this.atributeFiltersMap = new LinkedHashMap<>();
        this.atributeFiltersMap.put(EQUAL_VALUE_FILTER, "Equal");
        this.atributeFiltersMap.put(CONTAINS_VALUE_FILTER, "Contains");

        ItemRenderer<AttributeValueFilter> filtersItemRenderer = new ItemRenderer<AttributeValueFilter>() {
            @Override
            public String getItemDisplayText(AttributeValueFilter item) {
                return (item == null) ? " " : getFilterDisplayName(item);
            }
        };
        this.filtersComboBox = new CustomComboBox(filtersItemRenderer, componentDimension.getTextFieldPreferredHeight(), false, componentDimension.getTextFieldBackgroundColor());

        JLabel label = new JLabel();
        int maximumWidth = 0;
        this.filtersComboBox.addItem(null); // add no filter on the first position
        for (Map.Entry<AttributeValueFilter, String> entry : this.atributeFiltersMap.entrySet()) {
            this.filtersComboBox.addItem(entry.getKey());
            label.setText(entry.getValue());
            int preferredWidth = label.getPreferredSize().width;
            if (preferredWidth > maximumWidth) {
                maximumWidth = preferredWidth;
            }
        }
        Dimension filtersComboBoxSize = this.filtersComboBox.getPreferredSize();
        filtersComboBoxSize.width += maximumWidth;
        this.filtersComboBox.setPreferredSize(filtersComboBoxSize);
        this.filtersComboBox.setSelectedItem(null);

        Dimension buttonSize = new Dimension(componentDimension.getTextFieldPreferredHeight(), componentDimension.getTextFieldPreferredHeight());
        this.addAttributeButton = SwingUtils.buildButton("/org/esa/snap/resources/images/icons/Add16.png", null, buttonSize, 1);
        this.addAttributeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                addAttributeButtonClicked();
            }
        });
        this.removeAttributeButton = SwingUtils.buildButton("/org/esa/snap/resources/images/icons/Remove16.png", null, buttonSize, 1);
        this.removeAttributeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                remoteAttributeButtonClicked();
            }
        });

        this.attributesList = new JList<AttributeFilter>(new DefaultListModel<AttributeFilter>());
        this.attributesList.setBackground(componentDimension.getTextFieldBackgroundColor());
        this.attributesList.setVisibleRowCount(5);
        this.attributesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        int cellItemHeight = this.attributeNamesComboBox.getPreferredSize().height;
        this.attributesList.setCellRenderer(new LabelListCellRenderer<AttributeFilter>(cellItemHeight) {
            @Override
            protected String getItemDisplayText(AttributeFilter attribute) {
                if (attribute == null) {
                    return " ";
                }
                return attribute.getName() + " " + getFilterDisplayName(attribute.getValueFilter()) + " " + attribute.getValue();
            }
        });

        int gapBetweenColumns = componentDimension.getGapBetweenColumns();
        int gapBetweenRows = componentDimension.getGapBetweenRows();

        this.component = new JPanel(new GridBagLayout()) {
            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);

                for (int i=0; i<getComponentCount(); i++) {
                    getComponent(i).setEnabled(enabled);
                }
            }
        };
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, 0);
        this.component.add(this.attributeNamesComboBox, c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        this.component.add(this.filtersComboBox, c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        this.component.add(this.attributeValuesEditableComboBox, c);
        c = SwingUtils.buildConstraints(3, 0, GridBagConstraints.NONE, GridBagConstraints.NORTH, 1, 1, 0, gapBetweenColumns);
        this.component.add(this.addAttributeButton, c);

        JScrollPane scrollPane = new JScrollPane(this.attributesList);
        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.BOTH, GridBagConstraints.WEST, 3, 1, gapBetweenRows, 0);
        this.component.add(scrollPane, c);
        c = SwingUtils.buildConstraints(3, 1, GridBagConstraints.NONE, GridBagConstraints.NORTH, 1, 1, gapBetweenRows, gapBetweenColumns);
        this.component.add(this.removeAttributeButton, c);
    }

    private String getFilterDisplayName(AttributeValueFilter filter) {
        return this.atributeFiltersMap.get(filter);
    }

    @Override
    public JComponent getComponent() {
        return this.component;
    }

    @Override
    public void clearParameterValue() {
        resetFilters();
        DefaultListModel<AttributeFilter> model = (DefaultListModel<AttributeFilter>)this.attributesList.getModel();
        model.clear();
    }

    @Override
    public List<AttributeFilter> getParameterValue() {
        DefaultListModel<AttributeFilter> model = (DefaultListModel<AttributeFilter>)this.attributesList.getModel();
        List<AttributeFilter> result = new ArrayList<>(model.getSize());
        AttributeFilter attributeFilter = buildAttributeFilter();
        if (attributeFilter == null) {
            // the attribute filter is not specified
            if (isInvalidFilter()) {
                return null;
            }
        } else {
            result.add(attributeFilter);
        }
        for (int i = 0; i < model.getSize(); i++) {
            result.add(model.getElementAt(i));
        }
        return (result.size() > 0) ? result : null;
    }

    @Override
    public void setParameterValue(Object value) {
        if (value == null) {
            clearParameterValue();
        } else if (value instanceof List) {
            List<AttributeFilter> result = (List<AttributeFilter>)value;
            clearParameterValue();
            if (result.size() > 0) {
                AttributeFilter firstAttribute = result.get(0);
                this.attributeNamesComboBox.setSelectedItem(firstAttribute.getName());
                this.filtersComboBox.setSelectedItem(firstAttribute.getValueFilter());
                this.attributeValuesEditableComboBox.setSelectedItem(firstAttribute.getValue());
                DefaultListModel<AttributeFilter> model = (DefaultListModel<AttributeFilter>)this.attributesList.getModel();
                for (int i=1; i<result.size(); i++) {
                    model.addElement(result.get(i));
                }
            }
        } else {
            throw new ClassCastException("The parameter value type '" + value + "' must be '" + List.class+"'.");
        }
    }

    @Override
    public Boolean hasValidValue() {
        String selectedAttributeName = getSelectedAttributeName();
        AttributeValueFilter selectedAttributeValueFilter = getSelectedAttributeValueFilter();
        String attributeValue = getEnteredAttributeValue();
        if (selectedAttributeName == null && selectedAttributeValueFilter == null && attributeValue.length() == 0) {
            // no filter specified
            DefaultListModel<AttributeFilter> model = (DefaultListModel<AttributeFilter>)this.attributesList.getModel();
            if (model.getSize() == 0) {
                // no attributes added into the list
                return null;
            }
            // the list contains at least one attribute
            return true;
        } else if (selectedAttributeName != null && selectedAttributeValueFilter != null && attributeValue.length() > 0) {
            // the filter is specified
            return true;
        }
        return false; // the value is invalid
    }

    @Override
    public String getInvalidValueErrorDialogMessage() {
        StringBuilder message = new StringBuilder();
        message.append("The '" + getLabel().getText()+"' parameter has missing values.")
                .append("\n\n")
                .append("Specify the missing values or remove the existing values.");
        return message.toString();
    }

    private void remoteAttributeButtonClicked() {
        ListSelectionModel selectionModel = this.attributesList.getSelectionModel();
        DefaultListModel<AttributeFilter> model = (DefaultListModel<AttributeFilter>)this.attributesList.getModel();
        for (int i=model.getSize()-1; i>=0; i--) {
            if (selectionModel.isSelectedIndex(i)) {
                model.remove(i);
            }
        }
    }

    private void addAttributeButtonClicked() {
        AttributeFilter attributeFilter = buildAttributeFilter();
        if (attributeFilter != null) {
            DefaultListModel<AttributeFilter> attributesListModel = (DefaultListModel<AttributeFilter>)this.attributesList.getModel();
            attributesListModel.addElement(attributeFilter);

            boolean foundAttributeValue = false;
            ComboBoxModel<String> attributeValuesModel = this.attributeValuesEditableComboBox.getModel();
            for (int i=0; i<attributeValuesModel.getSize() && !foundAttributeValue; i++) {
                if (attributeFilter.getValue().equals(attributeValuesModel.getElementAt(i))) {
                    foundAttributeValue = true;
                }
            }
            if (!foundAttributeValue) {
                this.attributeValuesEditableComboBox.addItem(attributeFilter.getValue());
            }

            resetFilters();
        }
    }

    private void resetFilters() {
        this.attributeNamesComboBox.setSelectedItem(null);
        this.filtersComboBox.setSelectedItem(null);
        this.attributeValuesEditableComboBox.setSelectedItem(null);
    }

    private String getSelectedAttributeName() {
        return (String)this.attributeNamesComboBox.getSelectedItem();
    }

    private AttributeValueFilter getSelectedAttributeValueFilter() {
        return (AttributeValueFilter)this.filtersComboBox.getSelectedItem();
    }

    private String getEnteredAttributeValue() {
        JTextComponent textComponent = (JTextComponent)this.attributeValuesEditableComboBox.getEditor().getEditorComponent();
        return textComponent.getText().trim();
    }

    private AttributeFilter buildAttributeFilter() {
        String selectedAttributeName = getSelectedAttributeName();
        AttributeValueFilter selectedAttributeValueFilter = getSelectedAttributeValueFilter();
        String attributeValue = getEnteredAttributeValue();
        if (selectedAttributeName != null && selectedAttributeValueFilter != null && attributeValue.length() > 0) {
            return new AttributeFilter(selectedAttributeName, attributeValue, selectedAttributeValueFilter);
        }
        return null;
    }

    private boolean isInvalidFilter() {
        return (getSelectedAttributeName() != null || getSelectedAttributeValueFilter() != null || getEnteredAttributeValue().length() > 0);
    }
}
