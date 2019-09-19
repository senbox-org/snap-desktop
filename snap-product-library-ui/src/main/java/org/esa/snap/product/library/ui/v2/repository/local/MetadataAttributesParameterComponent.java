package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.AbstractParameterComponent;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.remote.execution.machines.RemoteMachineProperties;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jcoravu on 18/9/2019.
 */
public class MetadataAttributesParameterComponent extends AbstractParameterComponent<List<Attribute>> {

    private JComboBox<String> attributesComboBox;
    private JTextField attributeValueTextField;
    private JButton addAttributeButton;
    private JButton removeAttributeButton;
    private JList<Attribute> addedAttributesList;
    private final JPanel component;

    public MetadataAttributesParameterComponent(JComboBox<String> attributesComboBox, String parameterName, String parameterLabelText, boolean required, ComponentDimension componentDimension) {
        super(parameterName, parameterLabelText, required);

        this.attributesComboBox = attributesComboBox;

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

        this.addedAttributesList = new JList<Attribute>(new DefaultListModel<Attribute>());
        this.addedAttributesList.setVisibleRowCount(5);
        this.addedAttributesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.addedAttributesList.setCellRenderer(new LabelListCellRenderer<Attribute>(componentDimension.getListItemMargins()) {
            @Override
            protected String getItemDisplayText(Attribute attribute) {
                if (attribute == null) {
                    return " ";
                }
                return attribute.getName() + " = " + attribute.getValue();
            }
        });

        int gapBetweenColumns = componentDimension.getGapBetweenColumns();
        int gapBetweenRows = componentDimension.getGapBetweenRows();

        this.component = new JPanel(new GridBagLayout());
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, 0);
        this.component.add(this.attributesComboBox, c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        this.component.add(this.attributeValueTextField, c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.NORTH, 1, 1, 0, gapBetweenColumns);
        this.component.add(this.addAttributeButton, c);

        JScrollPane scrollPane = new JScrollPane(this.addedAttributesList);
        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.BOTH, GridBagConstraints.WEST, 2, 1, gapBetweenRows, 0);
        this.component.add(scrollPane, c);
        c = SwingUtils.buildConstraints(2, 1, GridBagConstraints.NONE, GridBagConstraints.NORTH, 1, 1, gapBetweenRows, gapBetweenColumns);
        this.component.add(this.removeAttributeButton, c);
    }

    @Override
    public JComponent getComponent() {
        return this.component;
    }

    @Override
    public List<Attribute> getParameterValue() {
        DefaultListModel<Attribute> model = (DefaultListModel<Attribute>)this.addedAttributesList.getModel();
        if (model.getSize() > 0) {
            List<Attribute> result = new ArrayList<>(model.getSize());
            for (int i = 0; i < model.getSize(); i++) {
                result.add(model.getElementAt(i));
            }
            return result;
        }
        return null;
    }

    private void remoteAttributeButtonClicked() {
        ListSelectionModel selectionModel = this.addedAttributesList.getSelectionModel();
        DefaultListModel<Attribute> model = (DefaultListModel<Attribute>)this.addedAttributesList.getModel();
        for (int i=model.getSize()-1; i>=0; i--) {
            if (selectionModel.isSelectedIndex(i)) {
                model.remove(i);
            }
        }
    }

    private void addAttributeButtonClicked() {
        String selectedAttributeName = (String)this.attributesComboBox.getSelectedItem();
        String attributeValue = this.attributeValueTextField.getText().trim();
        if (selectedAttributeName != null && attributeValue.length() > 0) {
            Attribute attribute = new Attribute(selectedAttributeName, attributeValue);
            DefaultListModel<Attribute> model = (DefaultListModel<Attribute>)this.addedAttributesList.getModel();
            model.addElement(attribute);

            this.attributesComboBox.setSelectedItem(null);
            this.attributeValueTextField.setText("");
        }
    }
}
