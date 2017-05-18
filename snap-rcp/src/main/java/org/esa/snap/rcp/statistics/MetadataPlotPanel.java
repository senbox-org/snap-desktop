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
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.SliderAdapter;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.ui.GridBagUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.openide.windows.TopComponent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.esa.snap.rcp.statistics.MetadataPlotPanel.MetadataPlotSettings.*;


/**
 * The metadata plot pane within the statistics window.
 */
class MetadataPlotPanel extends ChartPagePanel {
    private static final String DEFAULT_SAMPLE_DATASET_NAME = "Sample";

    private static final String NO_DATA_MESSAGE = "No metadata plot computed yet.\n" +
            "To create a plot, select metadata elements in both combo boxes.\n" +
            "The plot will be computed when you click the 'Refresh View' button.\n" +
            HELP_TIP_MESSAGE + "\n" +
            ZOOM_TIP_MESSAGE;
    private static final String CHART_TITLE = "Metadata Plot";

    private JFreeChart chart;
    private XYIntervalSeriesCollection dataset;
    private MetadataPlotSettings plotSettings;
    private boolean isInitialized;


    MetadataPlotPanel(TopComponent parentComponent, String helpId) {
        super(parentComponent, helpId, CHART_TITLE, true);
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

        final ValueAxis domainAxis = plot.getDomainAxis();
        final ValueAxis rangeAxis = plot.getRangeAxis();
        // allow transfer from bounds into min/max fields, if auto min/maxis enabled
        domainAxis.setAutoRange(true);
        rangeAxis.setAutoRange(true);

        ChartPanel profilePlotDisplay = new ChartPanel(chart);

        profilePlotDisplay.setInitialDelay(200);
        profilePlotDisplay.setDismissDelay(1500);
        profilePlotDisplay.setReshowDelay(200);
        profilePlotDisplay.setZoomTriggerDistance(5);
        profilePlotDisplay.getPopupMenu().addSeparator();
        profilePlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());


        plotSettings = new MetadataPlotSettings();
        final BindingContext bindingContext = plotSettings.getContext();

        JPanel settingsPanel = createSettingsPanel(bindingContext);
        createUI(profilePlotDisplay, settingsPanel, (RoiMaskSelector) null);

        isInitialized = true;

        updateComponents();
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
        // probably nothing needs to be done here
        super.nodeDataChanged(event);
    }

    @Override
    protected void updateComponents() {
        super.updateComponents();
        updateSettings();
        updateUiState();
    }


    @Override
    protected void updateChartData() {
        // probably nothing needs to be done here
    }

    @Override
    protected String getDataAsText() {
        return "";
    }


    private void updateUiState() {
        if (!isInitialized) {
            return;
        }

        dataset.removeAllSeries();

    }

    private void updateSettings() {
        Product product = getProduct();
        if (product == null) {
            return;
        }

        MetadataElement metadataRoot = product.getMetadataRoot();
        MetadataElement[] elements = metadataRoot.getElements();

        plotSettings.setMetadataElements(elements);

    }

    private JPanel createSettingsPanel(BindingContext bindingContext) {
        final JLabel datasetLabel = new JLabel("Dataset: ");
        final JComboBox<MetadataElement> datasetBox = new JComboBox<>();
        datasetBox.setRenderer(new ProductNodeListCellRenderer());
        JLabel recordLabel = new JLabel("Record: ");
        JSlider recordSlider = new JSlider(SwingConstants.HORIZONTAL, 1, 100, 1);
        recordSlider.setPaintTrack(true);
        recordSlider.setPaintTicks(true);
        recordSlider.setPaintLabels(true);
        Hashtable standardLabels = recordSlider.createStandardLabels(recordSlider.getMaximum() - recordSlider.getMinimum(), recordSlider.getMinimum());
        recordSlider.setLabelTable(standardLabels);
        JLabel numRecordsLabel = new JLabel("Records: ");
        JSpinner numRecordsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        final JLabel xFieldLabel = new JLabel("X Field: ");
        final JComboBox<MetadataAttribute> xFieldBox = new JComboBox<>();
        xFieldBox.setRenderer(new ProductNodeListCellRenderer());
        final JLabel y1FieldLabel = new JLabel("Y Field: ");
        final JComboBox<MetadataAttribute> y1FieldBox = new JComboBox<>();
        y1FieldBox.setRenderer(new ProductNodeListCellRenderer());
        final JLabel y2FieldLabel = new JLabel("Y2 Field: ");
        final JComboBox<MetadataAttribute> y2FieldBox = new JComboBox<>();
        y2FieldBox.setRenderer(new ProductNodeListCellRenderer());

        bindingContext.bind(PROP_NAME_METADATA_ELEMENT, datasetBox);
        bindingContext.bind(PROP_NAME_RECORD_START_INDEX, new SliderAdapter(recordSlider));
        bindingContext.bind(PROP_NAME_NUM_DISPLAY_RECORDS, numRecordsSpinner);
        bindingContext.bind(PROP_NAME_FIELD_X, xFieldBox);
        bindingContext.bind(PROP_NAME_FIELD_Y1, y1FieldBox);
        bindingContext.bind(PROP_NAME_FIELD_Y2, y2FieldBox);

        JPanel plotSettingsPanel = GridBagUtils.createPanel();
        GridBagConstraints plotSettingsPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=4");
        GridBagUtils.addToPanel(plotSettingsPanel, datasetLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=0,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, datasetBox, plotSettingsPanelConstraints, "gridwidth=1,gridy=0,gridx=1,weightx=1,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, recordLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=1,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, recordSlider, plotSettingsPanelConstraints, "gridwidth=1,gridy=1,gridx=1,weightx=1,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, numRecordsLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=2,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, numRecordsSpinner, plotSettingsPanelConstraints, "gridwidth=1,gridy=2,gridx=1,weightx=1,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, xFieldLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=3,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, xFieldBox, plotSettingsPanelConstraints, "gridwidth=1,gridy=3,gridx=1,weightx=1,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, y1FieldLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=4,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, y1FieldBox, plotSettingsPanelConstraints, "gridwidth=1,gridy=4,gridx=1,weightx=1,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, y2FieldLabel, plotSettingsPanelConstraints, "gridwidth=1,gridy=5,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(plotSettingsPanel, y2FieldBox, plotSettingsPanelConstraints, "gridwidth=1,gridy=5,gridx=1,weightx=1,insets.left=4");

        updateSettings();

        bindingContext.addPropertyChangeListener(evt -> {
            updateUiState();
        });
        return plotSettingsPanel;
    }

    static class MetadataPlotSettings {

        static final String PROP_NAME_METADATA_ELEMENT = "metadataElement";
        static final String PROP_NAME_RECORD_START_INDEX = "recordStartIndex";
        static final String PROP_NAME_NUM_DISPLAY_RECORDS = "numDisplayRecords";
        static final String PROP_NAME_FIELD_X = "fieldX";
        static final String PROP_NAME_FIELD_Y1 = "fieldY1";
        static final String PROP_NAME_FIELD_Y2 = "fieldY2";

        MetadataElement metadataElement;
        MetadataField fieldX;
        MetadataField fieldY1;
        MetadataField fieldY2;
        @Parameter(interval = "[0,100]")
        double recordStartIndex;
        @Parameter(defaultValue = "0")
        int numDisplayRecords;

        private BindingContext context;

        public MetadataPlotSettings() {
            context = new BindingContext(PropertyContainer.createObjectBacked(this, new ParameterDescriptorFactory()));
            Property propertyRecordStart = context.getPropertySet().getProperty(PROP_NAME_RECORD_START_INDEX);
            propertyRecordStart.getDescriptor().setAttribute("stepSize", 1);
            Property propertyMetaElement = context.getPropertySet().getProperty(PROP_NAME_METADATA_ELEMENT);
            propertyMetaElement.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    try {
                        // todo - swingworker?
                        int numAvailableRecords = getNumRecords(metadataElement);

                        PropertySet propertySet = context.getPropertySet();
                        Property recordStartProperty = propertySet.getProperty(PROP_NAME_RECORD_START_INDEX);
                        recordStartProperty.getDescriptor().setValueRange(new ValueRange(1, numAvailableRecords));
                        recordStartProperty.setValue(1);
                        Property numDispRecordProperty = propertySet.getProperty(PROP_NAME_NUM_DISPLAY_RECORDS);
                        numDispRecordProperty.getDescriptor().setValueRange(new ValueRange(1, numAvailableRecords));
                        numDispRecordProperty.setValue(1);


                        MetadataField[] usableFields = retrieveUsableFields(metadataElement);
                        PropertyDescriptor propertyFieldX = propertySet.getProperty(PROP_NAME_FIELD_X).getDescriptor();
                        propertyFieldX.setValueSet(new ValueSet(usableFields));
                        PropertyDescriptor propertyFieldY1 = propertySet.getProperty(PROP_NAME_FIELD_Y1).getDescriptor();
                        propertyFieldY1.setValueSet(new ValueSet(usableFields));
                        PropertyDescriptor propertyFieldY2 = propertySet.getProperty(PROP_NAME_FIELD_Y2).getDescriptor();
                        propertyFieldY2.setValueSet(new ValueSet(usableFields));


                    } catch (ValidationException e) {
                        e.printStackTrace();
                    }

                }
            });

        }

        public BindingContext getContext() {
            return context;
        }

        void setMetadataElements(MetadataElement[] elements) {
            Property property = context.getPropertySet().getProperty(PROP_NAME_METADATA_ELEMENT);
            property.getDescriptor().setValueSet(new ValueSet(filterElements(elements)));
            try {
                property.setValue(elements[0]);
            } catch (ValidationException e) {
                e.printStackTrace();
            }
        }

        private MetadataField[] retrieveUsableFields(MetadataElement element) {
            List<MetadataField> list = new ArrayList<>();
            if(getNumRecords(element) > 1) {
                
            }

            return list.toArray(new MetadataField[0]);
        }

        private MetadataElement[] filterElements(MetadataElement[] elements) {
            return elements;
        }

        private int getNumRecords(MetadataElement metadataElement) {
            int numSubElements = metadataElement.getNumElements();
            if (numSubElements > 0) {
                MetadataElement[] subElements = metadataElement.getElements();
                int count = 0;
                for (MetadataElement subElement : subElements) {
                    if (subElement.getName().matches(metadataElement.getName() + "\\.\\d+")) {
                        count++;
                    }

                }
                if (count == numSubElements) {
                    return count;
                }
            }
            return 1;
        }

    }

    private static class ProductNodeListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel rendererComponent = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ProductNode) {
                ProductNode element = (ProductNode) value;
                rendererComponent.setText(element.getName());
            }
            return rendererComponent;
        }
    }

    private interface MetadataField {


        static MetadataField create(ProductNode node) {
            if (!(node instanceof MetadataElement || node instanceof MetadataAttribute)) {
                throw new IllegalArgumentException("Parameters 'node' must be either instance " +
                                                           "of MetadataElement or MetadataAttribute.");
            }
            return null;
        }

        String getName();

        Object getData();

    }
}

