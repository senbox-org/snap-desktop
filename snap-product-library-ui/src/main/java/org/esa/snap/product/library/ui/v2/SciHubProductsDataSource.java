package org.esa.snap.product.library.ui.v2;

import org.esa.snap.graphbuilder.gpf.ui.worldmap.WorldMapUI;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;
import org.jdesktop.swingx.JXDatePicker;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.ParameterName;
import ro.cs.tao.datasource.remote.scihub.parameters.SciHubParameterProvider;
import ro.cs.tao.eodata.Polygon2D;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class SciHubProductsDataSource extends AbstractProductsDataSource {

    private JComboBox<String> missionsComboBox;
    private WorldMapUI worldMapUI;

    public SciHubProductsDataSource(int textFieldPreferredHeight, Insets defaultListItemMargins) {
        int gapBetweenRows = 5;
        int gapBetweenColumns = 5;

        setLayout(new BorderLayout(gapBetweenColumns, gapBetweenRows));

        createSupportedMissionsComboBox(textFieldPreferredHeight, defaultListItemMargins);

        this.worldMapUI = new WorldMapUI();

        addParameters();
    }

    @Override
    public String getName() {
        return "ESA SciHub";
    }

    private void createSupportedMissionsComboBox(int textFieldPreferredHeight, Insets defaultListItemMargins) {
        this.missionsComboBox = new JComboBox<String>();

        Dimension comboBoxSize = this.missionsComboBox.getPreferredSize();
        comboBoxSize.height = textFieldPreferredHeight;
        this.missionsComboBox.setPreferredSize(comboBoxSize);
        this.missionsComboBox.setMinimumSize(comboBoxSize);
        LabelListCellRenderer<String> renderer = new LabelListCellRenderer<String>(defaultListItemMargins) {
            @Override
            protected String getItemDisplayText(String value) {
                return (value == null) ? "" : value;
            }
        };
        this.missionsComboBox.setMaximumRowCount(5);
        this.missionsComboBox.setRenderer(renderer);
        this.missionsComboBox.setBackground(new Color(0, 0, 0, 0)); // set the transparent color
        this.missionsComboBox.setOpaque(true);
        this.missionsComboBox.setSelectedItem(null);

        SciHubParameterProvider sciHubParameterProvider = new SciHubParameterProvider();
        this.missionsComboBox.removeAllItems();
        String[] sensors = sciHubParameterProvider.getSupportedSensors();
        for (int i = 0; i < sensors.length; i++) {
            this.missionsComboBox.addItem(sensors[i]);
        }

        this.missionsComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    newMissionSelected((String)e.getItem());
                }
            }
        });
    }

    private void newMissionSelected(String selectedMission) {
        removeAll();
        addParameters();
        revalidate();
        repaint();
    }

    private void addParameters() {
        String selectedMission = (String) this.missionsComboBox.getSelectedItem();

        int gapBetweenRows = 5;
        int gapBetweenColumns = 5;

        JPanel parametersPanel = new JPanel(new GridLayout(1, 2, gapBetweenColumns, gapBetweenRows));

        JPanel leftParametersPanel = new JPanel(new GridBagLayout());
        JPanel rightParametersPanel = new JPanel(new GridBagLayout());
        parametersPanel.add(leftParametersPanel);
        parametersPanel.add(rightParametersPanel);

        add(parametersPanel, BorderLayout.NORTH);

        int index = 1;
        int leftParametersRowIndex = 0;
        int rightParametersRowIndex = 0;

        GridBagConstraints c = SwingUtils.buildConstraints(0, leftParametersRowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        leftParametersPanel.add(new JLabel("Mission"), c);

        c = SwingUtils.buildConstraints(1, leftParametersRowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        leftParametersPanel.add(this.missionsComboBox, c);

        leftParametersRowIndex++;

        SciHubParameterProvider sciHubParameterProvider = new SciHubParameterProvider();
        Map<String, Map<ParameterName, DataSourceParameter>> supportedParameters = sciHubParameterProvider.getSupportedParameters();
        Map<ParameterName, DataSourceParameter> sensorParameters = supportedParameters.get(selectedMission);
        Iterator<Map.Entry<ParameterName, DataSourceParameter>> it = sensorParameters.entrySet().iterator();
        DataSourceParameter polygonParameter = null;
        while (it.hasNext()) {
            Map.Entry<ParameterName, DataSourceParameter> entry = it.next();
            DataSourceParameter param = entry.getValue();
            JComponent component = null;
            if (param.getType() == String.class || param.getType() == Double.class) {
                index++;
                component = new JTextField();
            } else if (param.getType() == Date.class) {
                index++;
                component = new JXDatePicker();
            } else if (param.getType() == Polygon2D.class) {
                polygonParameter = param;
            }
            if (component != null) {
                int rowIndex;
                JPanel panel;
                if (index % 2 == 0) {
                    panel = rightParametersPanel;
                    rowIndex = rightParametersRowIndex;
                    rightParametersRowIndex++;

                } else {
                    panel = leftParametersPanel;
                    rowIndex = leftParametersRowIndex;
                    leftParametersRowIndex++;
                }
                int verticalGapBetweenRows = (rowIndex > 0) ? gapBetweenRows : 0;
                c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, 0);
                panel.add(new JLabel(entry.getValue().getLabel()), c);
                c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, gapBetweenColumns);
                panel.add(component, c);
            }
        }
        if (polygonParameter != null) {
            index++;

            JComboBox<String> selectionRectangleComboBox = new JComboBox<String>();
            selectionRectangleComboBox.addItem("Inside");
            selectionRectangleComboBox.addItem("Intersect");
            selectionRectangleComboBox.setBackground(new Color(0, 0, 0, 0)); // set the transparent color
            selectionRectangleComboBox.setOpaque(true);
            selectionRectangleComboBox.setSelectedItem(null);

            int rowIndex;
            JPanel panel;
            if (index % 2 == 0) {
                panel = rightParametersPanel;
                rowIndex = rightParametersRowIndex;
                rightParametersRowIndex++;

            } else {
                panel = leftParametersPanel;
                rowIndex = leftParametersRowIndex;
                leftParametersRowIndex++;
            }
            int verticalGapBetweenRows = (rowIndex > 0) ? gapBetweenRows : 0;
            c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, 0);
            panel.add(new JLabel("Selection rectangle"), c);
            c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, gapBetweenColumns);
            panel.add(selectionRectangleComboBox, c);

            JPanel areaOfInterestPanel = new JPanel(new GridBagLayout());
            c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, 0, 0);
            areaOfInterestPanel.add(new JLabel(polygonParameter.getLabel()), c);
            c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
            JPanel worldPanel = this.worldMapUI.getWorlMapPane();
            worldPanel.setBorder(new EtchedBorder());
            areaOfInterestPanel.add(worldPanel, c);

            add(areaOfInterestPanel, BorderLayout.CENTER);
        }
    }
}

