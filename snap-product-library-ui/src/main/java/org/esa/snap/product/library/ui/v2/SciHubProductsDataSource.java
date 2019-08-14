package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.product.library.v2.parameters.QueryFilter;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class SciHubProductsDataSource extends AbstractProductsDataSource {

    private JComboBox<String> missionsComboBox;
    private List<AbstractParameterComponent> parameterComponents;
    private JComboBox<String> selectionRectangleComboBox;
    private JLabel areaOfInterestLabel;
    private JPanel leftParametersPanel;

    public SciHubProductsDataSource(int textFieldPreferredHeight, Insets defaultListItemMargins) {
        int gapBetweenRows = 5;
        int gapBetweenColumns = 5;

        setLayout(new BorderLayout(gapBetweenColumns, gapBetweenRows));

        createSupportedMissionsComboBox(textFieldPreferredHeight, defaultListItemMargins);

        addParameters();
    }

    @Override
    public String getName() {
        return "ESA SciHub";
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        this.missionsComboBox.setEnabled(enabled);
        for (int i=0; i<this.parameterComponents.size(); i++) {
            JComponent component = this.parameterComponents.get(i).getComponent();
            component.setEnabled(enabled);
        }
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

        this.missionsComboBox.removeAllItems();
        String[] sensors = SciHubDownloader.getSupportedSensors();
        for (int i = 0; i < sensors.length; i++) {
            this.missionsComboBox.addItem(sensors[i]);
        }

        this.missionsComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    newSelectedMission((String)e.getItem());
                }
            }
        });
    }

    @Override
    public String getSelectedMission() {
        return (String) this.missionsComboBox.getSelectedItem();
    }

    @Override
    public Map<String, Object> getParameterValues() {
        Map<String, Object> result = new HashMap<>();
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            Object value = parameterComponent.getParameterValue();
            if (value != null) {
                result.put(parameterComponent.getParameterName(), value);
            }
        }
        return result;
    }

    protected void newSelectedMission(String selectedMission) {
        removeAll();
        addParameters();
        revalidate();
        repaint();
    }

    private void addParameters() {
        this.parameterComponents = new ArrayList<>();

        String selectedMission = (String) this.missionsComboBox.getSelectedItem();

        int gapBetweenRows = 5;
        int gapBetweenColumns = 5;

        JPanel parametersPanel = new JPanel(new GridLayout(1, 2, gapBetweenColumns, gapBetweenRows));

        this.leftParametersPanel = new JPanel(new GridBagLayout());
        JPanel rightParametersPanel = new JPanel(new GridBagLayout());
        parametersPanel.add(this.leftParametersPanel);
        parametersPanel.add(rightParametersPanel);

        add(parametersPanel, BorderLayout.NORTH);

        int leftParametersRowIndex = 0;
        int rightParametersRowIndex = 0;

        GridBagConstraints c = SwingUtils.buildConstraints(0, leftParametersRowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        this.leftParametersPanel.add(new JLabel("Mission"), c);

        c = SwingUtils.buildConstraints(1, leftParametersRowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        this.leftParametersPanel.add(this.missionsComboBox, c);

        leftParametersRowIndex++;

        QueryFilter rectangleParameter = null;
        List<QueryFilter> sensorParameters = SciHubDownloader.getSensorParameters(selectedMission);
        for (int i=0; i<sensorParameters.size(); i++) {
            QueryFilter param = sensorParameters.get(i);
            AbstractParameterComponent parameterComponent = null;
            if (param.getType() == String.class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : param.getDefaultValue().toString();
                parameterComponent = new StringParameterComponent(param.getName(), defaultValue);
            } else if (param.getType() == Double.class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : param.getDefaultValue().toString();
                parameterComponent = new StringParameterComponent(param.getName(), defaultValue);
            } else if (param.getType() == Date.class) {
                parameterComponent = new DateParameterComponent(param.getName());
            } else if (param.getType() == Rectangle.Double.class) {
                rectangleParameter = param;
            }
            if (parameterComponent != null) {
                this.parameterComponents.add(parameterComponent);
                int rowIndex;
                JPanel panel;
                if (leftParametersRowIndex > rightParametersRowIndex) {
                    panel = rightParametersPanel;
                    rowIndex = rightParametersRowIndex++;
                } else {
                    panel = this.leftParametersPanel;
                    rowIndex = leftParametersRowIndex++;
                }
                int verticalGapBetweenRows = (rowIndex > 0) ? gapBetweenRows : 0;
                c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, 0);
                panel.add(new JLabel(param.getLabel()), c);
                c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, gapBetweenColumns);
                panel.add(parameterComponent.getComponent(), c);
            }
        }
        if (rectangleParameter != null) {
            createSelectionRectangleComboBox();

            int rowIndex;
            JPanel panel;
            if (leftParametersRowIndex > rightParametersRowIndex) {
                panel = rightParametersPanel;
                rowIndex = rightParametersRowIndex++;
            } else {
                panel = this.leftParametersPanel;
                rowIndex = leftParametersRowIndex++;
            }
            int verticalGapBetweenRows = (rowIndex > 0) ? gapBetweenRows : 0;
            c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, 0);
            panel.add(new JLabel("Selection area"), c);
            c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, gapBetweenColumns);
            panel.add(this.selectionRectangleComboBox, c);

            if (leftParametersRowIndex > rightParametersRowIndex) {
                c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, verticalGapBetweenRows, 0);
                rightParametersPanel.add(new JLabel(), c); // add an empty label
            }

            SelectionAreaParameterComponent selectionAreaParameterComponent = new SelectionAreaParameterComponent(rectangleParameter.getName());
            this.parameterComponents.add(selectionAreaParameterComponent);
            JPanel worldPanel = selectionAreaParameterComponent.getComponent();
            worldPanel.setBackground(Color.WHITE);
            worldPanel.setOpaque(true);
            worldPanel.setBorder(new EtchedBorder());

            this.areaOfInterestLabel = new JLabel(rectangleParameter.getLabel());

            JPanel areaOfInterestPanel = new JPanel(new GridBagLayout());
            c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, 0, 0);
            areaOfInterestPanel.add(this.areaOfInterestLabel, c);
            c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
            areaOfInterestPanel.add(worldPanel, c);

            add(areaOfInterestPanel, BorderLayout.CENTER);

            // compute the maximum label width for the left column
            int maximumLabelWidth = computeLeftPanelMaximumLabelWidth();
            setLabelSize(this.areaOfInterestLabel, maximumLabelWidth);
            for (int i=0; i<this.leftParametersPanel.getComponentCount(); i++) {
                Component component = this.leftParametersPanel.getComponent(i);
                if (component instanceof JLabel) {
                    setLabelSize((JLabel)component, maximumLabelWidth);
                }
            }
        }
    }

    @Override
    public int computeLeftPanelMaximumLabelWidth() {
        int maximumLabelWidth = this.areaOfInterestLabel.getPreferredSize().width;
        for (int i=0; i<this.leftParametersPanel.getComponentCount(); i++) {
            Component component = this.leftParametersPanel.getComponent(i);
            if (component instanceof JLabel) {
                int labelWidth = component.getPreferredSize().width;
                if (maximumLabelWidth < labelWidth) {
                    maximumLabelWidth = labelWidth;
                }
            }
        }
        return maximumLabelWidth;
    }

    private void createSelectionRectangleComboBox() {
        this.selectionRectangleComboBox = new JComboBox<String>();
        this.selectionRectangleComboBox.addItem("Inside");
        this.selectionRectangleComboBox.addItem("Intersect");
        this.selectionRectangleComboBox.setBackground(new Color(0, 0, 0, 0)); // set the transparent color
        this.selectionRectangleComboBox.setOpaque(true);
        this.selectionRectangleComboBox.setSelectedItem(null);
    }

    public static void setLabelSize(JLabel label, int maximumLabelWidth) {
        Dimension labelSize = label.getPreferredSize();
        labelSize.width = maximumLabelWidth;
        label.setPreferredSize(labelSize);
        label.setMinimumSize(labelSize);
    }
}

