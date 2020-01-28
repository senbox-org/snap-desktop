package org.esa.snap.product.library.ui.v2.repository.local;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.AbstractParameterComponent;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.v2.database.AttributeFilter;
import org.esa.snap.product.library.v2.database.AttributeValueFilter;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
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

    private final JComboBox<String> attributeNamesComboBox;
    private final JComboBox<AttributeValueFilter> filtersComboBox;
    private final JTextField attributeValueTextField;
    private final JButton addAttributeButton;
    private final JButton removeAttributeButton;
    private final JList<AttributeFilter> attributesList;
    private final JPanel component;
    private final Map<AttributeValueFilter, String> atributeFiltersMap;

    public AttributesParameterComponent(JComboBox<String> attributesComboBox, String parameterName, String parameterLabelText, boolean required, ComponentDimension componentDimension) {
        super(parameterName, parameterLabelText, required);

        this.attributeNamesComboBox = attributesComboBox;

        AttributeValueFilter equalValueFilter = new AttributeValueFilter() {
            @Override
            public boolean matches(String attributeValue, String valueToCheck) {
                return attributeValue.equalsIgnoreCase(valueToCheck);
            }
        };
        AttributeValueFilter containsValueFilter = new AttributeValueFilter() {
            @Override
            public boolean matches(String attributeValue, String valueToCheck) {
                return StringUtils.containsIgnoreCase(attributeValue, valueToCheck);
            }
        };
        this.atributeFiltersMap = new LinkedHashMap<>();
        this.atributeFiltersMap.put(equalValueFilter, "Equal");
        this.atributeFiltersMap.put(containsValueFilter, "Contains");

        this.filtersComboBox = RemoteProductsRepositoryPanel.buildComboBox(componentDimension);
        Dimension filtersComboBoxSize = this.filtersComboBox.getPreferredSize();
        LabelListCellRenderer<AttributeValueFilter> renderer = new LabelListCellRenderer<AttributeValueFilter>(filtersComboBoxSize.height) {
            @Override
            protected String getItemDisplayText(AttributeValueFilter value) {
                return (value == null) ? " " : getFilterDisplayName(value);
            }
        };
        this.filtersComboBox.setRenderer(renderer);
        JLabel label = new JLabel();
        int maximumWidth = 0;
        this.filtersComboBox.addItem(null);
        for (Map.Entry<AttributeValueFilter, String> entry : this.atributeFiltersMap.entrySet()) {
            this.filtersComboBox.addItem(entry.getKey());
            label.setText(entry.getValue());
            int preferredWidth = label.getPreferredSize().width;
            if (preferredWidth > maximumWidth) {
                maximumWidth = preferredWidth;
            }
        }
        filtersComboBoxSize.width += maximumWidth;
        this.filtersComboBox.setPreferredSize(filtersComboBoxSize);
        this.filtersComboBox.setSelectedItem(null);

        this.attributeValueTextField = new JTextField();
        Dimension preferredSize = this.attributeValueTextField.getPreferredSize();
        preferredSize.height = componentDimension.getTextFieldPreferredHeight();
        this.attributeValueTextField.setPreferredSize(preferredSize);
        this.attributeValueTextField.setMinimumSize(preferredSize);

        Dimension buttonSize = new Dimension(componentDimension.getTextFieldPreferredHeight(), componentDimension.getTextFieldPreferredHeight());
        this.addAttributeButton = RepositorySelectionPanel.buildButton("/org/esa/snap/resources/images/icons/Add16.png", null, buttonSize, 1);
        this.addAttributeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                addAttributeButtonClicked();
            }
        });
        this.removeAttributeButton = RepositorySelectionPanel.buildButton("/org/esa/snap/resources/images/icons/Remove16.png", null, buttonSize, 1);
        this.removeAttributeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                remoteAttributeButtonClicked();
            }
        });

        this.attributesList = new JList<AttributeFilter>(new DefaultListModel<AttributeFilter>()) {
            @Override
            public Color getBackground() {
                return isEnabled() ? super.getBackground() : UIManager.getColor("TextField.inactiveBackground");
            }
        };
        this.attributesList.setVisibleRowCount(5);
        this.attributesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        int cellItemHeight = this.attributeNamesComboBox.getPreferredSize().height;
        this.attributesList.setCellRenderer(new LabelListCellRenderer<AttributeFilter>(cellItemHeight) {
            @Override
            protected String getItemDisplayText(AttributeFilter attribute) {
                if (attribute == null) {
                    return " ";
                }
                return attribute.getName() + " " +getFilterDisplayName(attribute.getValueFilter()) + " " + attribute.getValue();
            }
        });

        int gapBetweenColumns = componentDimension.getGapBetweenColumns();
        int gapBetweenRows = componentDimension.getGapBetweenRows();

        this.component = new JPanel(new GridBagLayout());
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, 0);
        this.component.add(this.attributeNamesComboBox, c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        this.component.add(this.filtersComboBox, c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        this.component.add(this.attributeValueTextField, c);
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
    public String getRequiredErrorDialogMessage() {
        if (isInvalidFilter()) {
            StringBuilder message = new StringBuilder();
            message.append("The '" + getLabel().getText()+"' parameter has missing values.")
                   .append("\n\n")
                   .append("Specify the missing values or remove the existing values.");
            return message.toString();
        }
        return null;
    }

    @Override
    public List<AttributeFilter> getParameterValue() {
        DefaultListModel<AttributeFilter> model = (DefaultListModel<AttributeFilter>)this.attributesList.getModel();
        List<AttributeFilter> result = new ArrayList<>(model.getSize());
        AttributeFilter attributeFilter = buildAttributeFilter();
        if (attributeFilter == null) {
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
            DefaultListModel<AttributeFilter> model = (DefaultListModel<AttributeFilter>)this.attributesList.getModel();
            model.addElement(attributeFilter);
            resetFilters();
        }
    }

    private void resetFilters() {
        this.attributeNamesComboBox.setSelectedItem(null);
        this.filtersComboBox.setSelectedItem(null);
        this.attributeValueTextField.setText("");
    }

    private String getSelectedAttributeName() {
        return (String)this.attributeNamesComboBox.getSelectedItem();
    }

    private AttributeValueFilter getSelectedAttributeValueFilter() {
        return (AttributeValueFilter)this.filtersComboBox.getSelectedItem();
    }

    private String getEnteredAttributeValue() {
        return this.attributeValueTextField.getText().trim();
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
