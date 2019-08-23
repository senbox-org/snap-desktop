package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.table.CustomSplitPane;
import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.product.library.v2.parameters.QueryFilter;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

    private final int gapBetweenRows;
    private final int gapBetweenColumns;
    private JComboBox<String> missionsComboBox;
    private List<AbstractParameterComponent> parameterComponents;
    private JLabel missionsLabel;
    private int textFieldPreferredHeight;
    private final IMissionParameterListener missionParameterListener;

    public SciHubProductsDataSource(int textFieldPreferredHeight, Insets defaultListItemMargins, int gapBetweenRows, int gapBetweenColumns, IMissionParameterListener missionParameterListener) {
        this.gapBetweenRows = gapBetweenRows;
        this.gapBetweenColumns = gapBetweenColumns;
        this.textFieldPreferredHeight = textFieldPreferredHeight;
        this.missionParameterListener = missionParameterListener;

        createMissionsComboBox(textFieldPreferredHeight, defaultListItemMargins);

        setLayout(new BorderLayout(this.gapBetweenColumns, this.gapBetweenRows));
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

    @Override
    public void refreshMissionParameters() {
        removeAll();
        addParameters();
        revalidate();
        repaint();
    }

    private void createMissionsComboBox(int textFieldPreferredHeight, Insets defaultListItemMargins) {
        this.missionsLabel = new JLabel("Mission");

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
        String[] supportedMissions = SciHubDownloader.getSupportedMissions();
        for (int i = 0; i < supportedMissions.length; i++) {
            this.missionsComboBox.addItem(supportedMissions[i]);
        }

        this.missionsComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    missionParameterListener.newSelectedMission(getSelectedMission(), SciHubProductsDataSource.this);
                }
            }
        });
    }

    private void addParameters() {
        JComponent panel = new JPanel(new GridBagLayout());

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        panel.add(this.missionsLabel, c);

        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        panel.add(this.missionsComboBox, c);

        this.parameterComponents = new ArrayList<>();

        String selectedMission = (String) this.missionsComboBox.getSelectedItem();
        int rowIndex = 1;
        QueryFilter rectangleParameter = null;
        List<QueryFilter> sensorParameters = SciHubDownloader.getMissionParameters(selectedMission);
        for (int i=0; i<sensorParameters.size(); i++) {
            QueryFilter param = sensorParameters.get(i);
            AbstractParameterComponent parameterComponent = null;
            if (param.getType() == String.class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : param.getDefaultValue().toString();
                parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), this.textFieldPreferredHeight);
            } else if (param.getType() == Double.class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : param.getDefaultValue().toString();
                parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), this.textFieldPreferredHeight);
            } else if (param.getType() == Date.class) {
                parameterComponent = new DateParameterComponent(param.getName(), param.getLabel(), this.textFieldPreferredHeight);
            } else if (param.getType() == Rectangle.Double.class) {
                rectangleParameter = param;
            }
            if (parameterComponent != null) {
                this.parameterComponents.add(parameterComponent);
                c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
                panel.add(parameterComponent.getLabel(), c);
                c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
                panel.add(parameterComponent.getComponent(), c);
                rowIndex++;
            }
        }

        c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, 0, 0);
        panel.add(Box.createVerticalGlue(), c); // add an empty label

        if (rectangleParameter != null) {
            SelectionAreaParameterComponent selectionAreaParameterComponent = new SelectionAreaParameterComponent(rectangleParameter.getName(), rectangleParameter.getLabel());
            this.parameterComponents.add(selectionAreaParameterComponent);
            JPanel worldPanel = selectionAreaParameterComponent.getComponent();
            worldPanel.setBackground(Color.WHITE);
            worldPanel.setOpaque(true);
            worldPanel.setBorder(new EtchedBorder());

            JPanel areaOfInterestPanel = new JPanel(new GridBagLayout());
            c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, 0, 0);
            areaOfInterestPanel.add(selectionAreaParameterComponent.getLabel(), c);
            c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
            areaOfInterestPanel.add(worldPanel, c);

            CustomSplitPane verticalSplitPane = new CustomSplitPane(JSplitPane.VERTICAL_SPLIT, 1, 2);
            verticalSplitPane.setTopComponent(panel);
            verticalSplitPane.setBottomComponent(areaOfInterestPanel);

            panel = verticalSplitPane;
        }

        // set the same label with
        int maximumLabelWidth = computeLeftPanelMaximumLabelWidth();
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            SciHubProductsDataSource.setLabelSize(parameterComponent.getLabel(), maximumLabelWidth);
        }

        add(panel, BorderLayout.CENTER);
    }

    @Override
    public int computeLeftPanelMaximumLabelWidth() {
        int maximumLabelWidth = this.missionsLabel.getPreferredSize().width;
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            int labelWidth = parameterComponent.getLabel().getPreferredSize().width;
            if (maximumLabelWidth < labelWidth) {
                maximumLabelWidth = labelWidth;
            }
        }
        return maximumLabelWidth;
    }

    public static void setLabelSize(JLabel label, int maximumLabelWidth) {
        Dimension labelSize = label.getPreferredSize();
        labelSize.width = maximumLabelWidth;
        label.setPreferredSize(labelSize);
        label.setMinimumSize(labelSize);
    }
}

