package org.esa.snap.product.library.ui.v2;

import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.ParameterName;
import ro.cs.tao.datasource.remote.scihub.parameters.SciHubParameterProvider;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class SciHubProductsDataSource extends AbstractProductsDataSource {

    private JComboBox<String> supportedMissionsComboBox;

    public SciHubProductsDataSource(int textFieldPreferredHeight, Insets defaultListItemMargins) {
        createSupportedMissionsComboBox(textFieldPreferredHeight, defaultListItemMargins);
    }

    @Override
    public String getName() {
        return "ESA SciHub";
    }

    @Override
    public String[] getSupportedSensors() {
        SciHubParameterProvider sciHubParameterProvider = new SciHubParameterProvider();
        return sciHubParameterProvider.getSupportedSensors();
    }

    @Override
    public JPanel buildParametersPanel() {
        SciHubParameterProvider sciHubParameterProvider = new SciHubParameterProvider();
        this.supportedMissionsComboBox.removeAllItems();
        String[] sensors = sciHubParameterProvider.getSupportedSensors();
        for (int i=0; i<sensors.length; i++) {
            this.supportedMissionsComboBox.addItem(sensors[i]);
        }

        int gapBetweenRows = 5;
        int gapBetweenColumns = 5;

        JPanel outputParametersPanel = new JPanel(new GridLayout(1, 2, gapBetweenColumns, gapBetweenRows));

        JPanel leftParametersPanel = new JPanel(new GridBagLayout());
        JPanel rightParametersPanel = new JPanel(new GridBagLayout());
        outputParametersPanel.add(leftParametersPanel);
        outputParametersPanel.add(rightParametersPanel);

        int index = 1;
        int leftParametersRowIndex = 0;
        int rightParametersRowIndex = 0;

        GridBagConstraints c = SwingUtils.buildConstraints(0, leftParametersRowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        leftParametersPanel.add(new JLabel("Mission"), c);

        c = SwingUtils.buildConstraints(1, leftParametersRowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        leftParametersPanel.add(this.supportedMissionsComboBox, c);

        leftParametersRowIndex++;

        Map<String, Map<ParameterName, DataSourceParameter>> supportedParameters = sciHubParameterProvider.getSupportedParameters();
        String selectedSensor = (String)this.supportedMissionsComboBox.getSelectedItem();
        Map<ParameterName, DataSourceParameter> sensorParameters = supportedParameters.get(selectedSensor);
        Iterator<Map.Entry<ParameterName, DataSourceParameter>> it = sensorParameters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ParameterName, DataSourceParameter> entry = it.next();
            DataSourceParameter param = entry.getValue();
            if (param.getType() == String.class) {
                index++;
                int rowIndex;
                JPanel parametersPanel;
                if (index % 2 == 0) {
                    parametersPanel = rightParametersPanel;
                    rowIndex = rightParametersRowIndex;
                    rightParametersRowIndex++;

                } else {
                    parametersPanel = leftParametersPanel;
                    rowIndex = leftParametersRowIndex;
                    leftParametersRowIndex++;
                }

                int verticalGapBetweenRows = (rowIndex > 0) ? gapBetweenRows : 0;
                JTextField parameterTextField = new JTextField();
                c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, 0);
                parametersPanel.add(new JLabel(entry.getValue().getLabel()), c);
                c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, gapBetweenColumns);
                parametersPanel.add(parameterTextField, c);
            }
        }

        return outputParametersPanel;
    }

    private void createSupportedMissionsComboBox(int textFieldPreferredHeight, Insets defaultListItemMargins) {
        this.supportedMissionsComboBox = new JComboBox<String>();

        Dimension comboBoxSize = this.supportedMissionsComboBox.getPreferredSize();
        comboBoxSize.height = textFieldPreferredHeight;
        this.supportedMissionsComboBox.setPreferredSize(comboBoxSize);
        this.supportedMissionsComboBox.setMinimumSize(comboBoxSize);
        LabelListCellRenderer<String> renderer = new LabelListCellRenderer<String>(defaultListItemMargins) {
            @Override
            protected String getItemDisplayText(String value) {
                return (value == null) ? "" : value;
            }
        };
        this.supportedMissionsComboBox.setMaximumRowCount(5);
        this.supportedMissionsComboBox.setRenderer(renderer);
        this.supportedMissionsComboBox.setBackground(new Color(0, 0, 0, 0)); // set the transparent color
        this.supportedMissionsComboBox.setOpaque(true);
        this.supportedMissionsComboBox.setSelectedItem(null);
        this.supportedMissionsComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                }
            }
        });

    }
}
