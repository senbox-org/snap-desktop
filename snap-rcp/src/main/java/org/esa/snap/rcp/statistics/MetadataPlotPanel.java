/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.rcp.statistics;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.ui.GridBagUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.openide.windows.TopComponent;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.GridBagConstraints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * The metadata plot pane within the statistics window.
 */
class MetadataPlotPanel extends ChartPagePanel {
    private static final String PROPERTY_NAME_MARK_SEGMENTS = "markSegments";
    private static final String PROPERTY_NAME_LOG_SCALED = "logScaled";
    private static final String DEFAULT_SAMPLE_DATASET_NAME = "Sample";

    private static final String NO_DATA_MESSAGE = "No metadata plot computed yet.\n" +
            "To create a plot, select metadata elements in both combo boxes.\n" +
            "The plot will be computed when you click the 'Refresh View' button.\n" +
            HELP_TIP_MESSAGE + "\n" +
            ZOOM_TIP_MESSAGE;
    private static final String CHART_TITLE = "Metadata Plot";

    private JFreeChart chart;
    private XYIntervalSeriesCollection dataset;
    private AxisRangeControl xAxisRangeControl;
    private AxisRangeControl yAxisRangeControl;
    private MetadataPlotSettings plotSettings;
    private AtomicBoolean axisAdjusting;


    MetadataPlotPanel(TopComponent parentComponent, String helpId) {
        super(parentComponent, helpId, CHART_TITLE, true);
        axisAdjusting = new AtomicBoolean(false);
    }

    @Override
    protected void initComponents() {
        if (hasAlternativeView()) {
            getAlternativeView().initComponents();
        }
        dataset = new XYIntervalSeriesCollection();
        chart = ChartFactory.createXYLineChart(
                CHART_TITLE,
                "x-values",
                DEFAULT_SAMPLE_DATASET_NAME,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        final XYPlot plot = chart.getXYPlot();

        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        final AxisChangeListener axisListener = event -> adjustAxisControlComponents();

        final ValueAxis domainAxis = plot.getDomainAxis();
        final ValueAxis rangeAxis = plot.getRangeAxis();
        // allow transfer from bounds into min/max fields, if auto min/maxis enabled
        domainAxis.setAutoRange(true);
        rangeAxis.setAutoRange(true);

        domainAxis.addChangeListener(axisListener);
        rangeAxis.addChangeListener(axisListener);

        ChartPanel profilePlotDisplay = new ChartPanel(chart);

        profilePlotDisplay.setInitialDelay(200);
        profilePlotDisplay.setDismissDelay(1500);
        profilePlotDisplay.setReshowDelay(200);
        profilePlotDisplay.setZoomTriggerDistance(5);
        profilePlotDisplay.getPopupMenu().addSeparator();
        profilePlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());


        xAxisRangeControl = new AxisRangeControl("X-Axis");
        yAxisRangeControl = new AxisRangeControl("Y-Axis");

        final PropertyChangeListener changeListener = evt -> {
            if (evt.getPropertyName().equals(PROPERTY_NAME_MARK_SEGMENTS)) {
                updateDataSet();
            }
            if (evt.getPropertyName().equals(PROPERTY_NAME_LOG_SCALED)) {
                updateScalingOfYAxis();
            }
            updateUIState();
        };
        xAxisRangeControl.getBindingContext().addPropertyChangeListener(changeListener);
        xAxisRangeControl.getBindingContext().getPropertySet().addProperty(Property.create(PROPERTY_NAME_MARK_SEGMENTS, false));
        xAxisRangeControl.getBindingContext().getPropertySet().getDescriptor(PROPERTY_NAME_MARK_SEGMENTS).setDescription("Toggle whether to mark segments");

        yAxisRangeControl.getBindingContext().addPropertyChangeListener(changeListener);
        yAxisRangeControl.getBindingContext().getPropertySet().addProperty(Property.create(PROPERTY_NAME_LOG_SCALED, false));
        yAxisRangeControl.getBindingContext().getPropertySet().getDescriptor(PROPERTY_NAME_LOG_SCALED).setDescription("Toggle whether to use a logarithmic axis");

        plotSettings = new MetadataPlotSettings();
        final BindingContext bindingContext = new BindingContext(PropertyContainer.createObjectBacked(plotSettings));

        JPanel settingsPanel = createSettingsPanel(bindingContext);
        createUI(profilePlotDisplay, settingsPanel, (RoiMaskSelector) null);

        updateComponents();

    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
        super.nodeDataChanged(event);
    }

    @Override
    protected void updateComponents() {
        super.updateComponents();
    }


    @Override
    protected void updateChartData() {

    }

    @Override
    protected String getDataAsText() {
        return "";
    }


    private void updatePlotSettings() {

    }

    private void updateUIState() {

    }

    private void updateDataSet() {

    }

    private JPanel createSettingsPanel(BindingContext bindingContext) {
        final JLabel datasetLabel = new JLabel("Dataset: ");
        final JComboBox datasetBox = new JComboBox();
        JLabel recordLabel = new JLabel("Record: ");
        JSlider recordSlider = new JSlider(SwingConstants.HORIZONTAL,1,100,1);
        JLabel numRecordsLabel = new JLabel("Records: ");
        JSpinner numRecordsSpinner = new JSpinner(new SpinnerNumberModel(1,1,100,1));
        final JLabel xfieldLabel = new JLabel("X Field: ");
        final JComboBox xFieldBox = new JComboBox();
        final JLabel y1fieldLabel = new JLabel("Y Field: ");
        final JComboBox y1FieldBox = new JComboBox();
        final JLabel y2fieldLabel = new JLabel("Y2 Field: ");
        final JComboBox y2FieldBox = new JComboBox();

        JPanel plotSettingsPanel = GridBagUtils.createPanel();
        GridBagConstraints plotSettingsPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2");
        GridBagUtils.addToPanel(plotSettingsPanel, datasetLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=0,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, datasetBox, plotSettingsPanelConstraints, "gridwidth=1,gridy=0,gridx=1,weightx=1,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, recordLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=1,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, recordSlider, plotSettingsPanelConstraints, "gridwidth=1,gridy=1,gridx=1,weightx=1,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, numRecordsLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=2,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, numRecordsSpinner, plotSettingsPanelConstraints, "gridwidth=1,gridy=2,gridx=1,weightx=1,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, xfieldLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=3,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, xFieldBox, plotSettingsPanelConstraints, "gridwidth=1,gridy=3,gridx=1,weightx=1,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, y1fieldLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=3,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, y1FieldBox, plotSettingsPanelConstraints, "gridwidth=1,gridy=3,gridx=1,weightx=1,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, y2fieldLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=3,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, y2FieldBox, plotSettingsPanelConstraints, "gridwidth=1,gridy=3,gridx=1,weightx=1,insets.left=4");
        
        xAxisRangeControl.getBindingContext().bind(PROPERTY_NAME_MARK_SEGMENTS, new JCheckBox("Mark segments"));
        yAxisRangeControl.getBindingContext().bind(PROPERTY_NAME_LOG_SCALED, new JCheckBox("Log10 scaled"));

        JPanel displayOptionsPanel = GridBagUtils.createPanel();
        GridBagConstraints displayOptionsConstraints = GridBagUtils.createConstraints("anchor=SOUTH,fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(displayOptionsPanel, xAxisRangeControl.getPanel(), displayOptionsConstraints, "gridy=0");
        GridBagUtils.addToPanel(displayOptionsPanel, xAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_MARK_SEGMENTS).getComponents()[0], displayOptionsConstraints, "gridy=1");
        GridBagUtils.addToPanel(displayOptionsPanel, yAxisRangeControl.getPanel(), displayOptionsConstraints, "gridy=2");
        GridBagUtils.addToPanel(displayOptionsPanel, yAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_LOG_SCALED).getComponents()[0], displayOptionsConstraints, "gridy=3");

        JPanel settingsPanel = GridBagUtils.createPanel();
        GridBagConstraints settingsPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1");
        GridBagUtils.addToPanel(settingsPanel, plotSettingsPanel, settingsPanelConstraints, "gridy=0");
        GridBagUtils.addToPanel(settingsPanel, new JPanel(), settingsPanelConstraints, "gridy=1,fill=VERTICAL,weighty=1");
        GridBagUtils.addToPanel(settingsPanel, displayOptionsPanel, settingsPanelConstraints, "gridy=2,fill=HORIZONTAL,weighty=0");

        final PropertyDescriptor datasetDescriptor = bindingContext.getPropertySet().getProperty("dataset").getDescriptor();
        datasetDescriptor.setValueSet(new ValueSet(new Object[0])); // todo


        bindingContext.addPropertyChangeListener(evt -> {
            updatePlotSettings();
            updateDataSet();
            updateUIState();
        });
        return settingsPanel;
    }

    private void adjustAxisControlComponents() {
        if (!axisAdjusting.getAndSet(true)) {
            try {
                if (xAxisRangeControl.isAutoMinMax()) {
                    xAxisRangeControl.adjustComponents(chart.getXYPlot().getDomainAxis(), 0);
                }
                if (yAxisRangeControl.isAutoMinMax()) {
                    yAxisRangeControl.adjustComponents(chart.getXYPlot().getRangeAxis(), 2);
                }
            } finally {
                axisAdjusting.set(false);
            }
        }
    }

    private void updateScalingOfYAxis() {
        final boolean logScaled = (Boolean) yAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_LOG_SCALED).getPropertyValue();
        final XYPlot plot = chart.getXYPlot();
        plot.setRangeAxis(StatisticChartStyling.updateScalingOfAxis(logScaled, plot.getRangeAxis(), true));
    }


    private class MetadataPlotSettings {

        MetadataElement dataset;
        MetadataAttribute fieldX;
        MetadataAttribute fieldY;
        MetadataAttribute fieldY2;
        int currentRecordIndex;
        int numRecords;

    }

}

