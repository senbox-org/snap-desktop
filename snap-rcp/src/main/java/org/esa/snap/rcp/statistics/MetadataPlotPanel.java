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

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.SliderAdapter;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.ui.io.TableModelCsvEncoder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.ui.RectangleInsets;
import org.openide.windows.TopComponent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Hashtable;

import static org.esa.snap.rcp.statistics.MetadataPlotSettings.*;


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
    private static final String DEFAULT_X_AXIS_LABEL = "x-values";

    private DefaultXYDataset dataset;
    private MetadataPlotSettings plotSettings;
    private boolean isInitialized;
    private JSlider recordSlider;
    private SpinnerNumberModel numRecSpinnerModel;
    private XYPlot xyPlot;
    private DefaultXYItemRenderer xyItemRenderer;
    private JTextField recordValueField;


    MetadataPlotPanel(TopComponent parentComponent, String helpId) {
        super(parentComponent, helpId, CHART_TITLE, false);
    }

    @Override
    protected void initComponents() {
        if (hasAlternativeView()) {
            getAlternativeView().initComponents();
        }
        dataset = new DefaultXYDataset();
        JFreeChart chart = ChartFactory.createXYLineChart(
                CHART_TITLE,
                DEFAULT_X_AXIS_LABEL,
                DEFAULT_SAMPLE_DATASET_NAME,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        xyPlot = chart.getXYPlot();

        xyPlot.setNoDataMessage(NO_DATA_MESSAGE);
        xyPlot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        final ValueAxis domainAxis = xyPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        domainAxis.setAutoRangeMinimumSize(2);

        xyItemRenderer = new DefaultXYItemRenderer();
        xyItemRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        xyItemRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator("{2,number,#.#}",
                                                                                  NumberFormat.getNumberInstance(),
                                                                                  NumberFormat.getNumberInstance()));
        xyPlot.setRenderer(0, xyItemRenderer);
        xyPlot.setRenderer(1, xyItemRenderer);


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

        bindingContext.setComponentsEnabled(PROP_NAME_RECORD_START_INDEX, false);
        bindingContext.setComponentsEnabled(PROP_NAME_RECORDS_PER_PLOT, false);

        isInitialized = true;

        updateComponents();
        updateChartData();

        bindingContext.addPropertyChangeListener(PROP_NAME_METADATA_ELEMENT, evt -> updateUiState());
        bindingContext.addPropertyChangeListener(evt -> updateChartData());

    }

    @Override
    protected void showAlternativeView() {
        final TableModel model;
        if (xyPlot != null && xyPlot.getSeriesCount() > 0) {
            model = new MetadataPlotTableModel(xyPlot);
        } else {
            model = new DefaultTableModel();
        }
        final TableViewPagePanel alternativPanel = (TableViewPagePanel) getAlternativeView();
        alternativPanel.setModel(model);
        super.showAlternativeView();
    }

    private NumberAxis configureRangeIndex(int index) {
        NumberAxis axis = new NumberAxis();
        axis.setAutoRange(true);
        axis.setLabelFont(xyPlot.getDomainAxis().getLabelFont());
        axis.setLabel(String.format("Y%d Samples", index + 1));
        xyPlot.setRangeAxis(index, axis);
        xyPlot.setRangeAxisLocation(index, index == 0 ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.BOTTOM_OR_RIGHT);
        return axis;
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
        resetChart();

        MetadataElement metadataElement = plotSettings.getMetadataElement();
        String nameX = plotSettings.getNameX();
        String nameY1 = plotSettings.getNameY1();
        if (metadataElement == null || nameX == null || StringUtils.isNullOrEmpty(nameY1) || MetadataPlotSettings.FIELD_NAME_NONE.equals(nameY1)) {
            return;
        }

        int numRecords = plotSettings.getNumRecords();
        int recordsPerPlot = plotSettings.getRecordsPerPlot();
        int startIndex = plotSettings.getRecordStartIndex();
        String nameY2 = plotSettings.getFieldY2();
        double[] recordIndices = getRecordIndices(startIndex, recordsPerPlot, numRecords);
        String[] recordElementNames = new String[recordIndices.length];
        Arrays.setAll(recordElementNames, i -> String.format("%s.%.0f", metadataElement.getName(), recordIndices[i]));

        xyPlot.getDomainAxis().setLabel(nameX);

        switch (nameX) {
            case MetadataPlotSettings.FIELD_NAME_RECORD_INDEX:
                configureChartForRecordIndex(metadataElement, nameX, nameY1, nameY2, recordElementNames,
                                             String.format("%s.1", metadataElement.getName()), recordIndices);
                break;
            case MetadataPlotSettings.FIELD_NAME_ARRAY_FIELD_INDEX:
                configureChartForArrayIndex(metadataElement, nameX, nameY1, nameY2, recordElementNames,
                                            String.format("%s.1", metadataElement.getName()));
                break;
            default:
                configureChartForDefault(metadataElement, nameX, nameY1, nameY2);
                break;
        }

    }

    private void configureChartForDefault(MetadataElement metadataElement, String nameX, String nameY1, String nameY2) {
        if (!isValidYField(metadataElement, metadataElement.getName(), nameY1)) {
            return;
        }
        xyPlot.getDomainAxis().setLabel(nameX);
        double[] xData = new double[1];
        Arrays.setAll(xData, i -> metadataElement.getAttribute(nameX).getData().getElemDouble());

        NumberAxis y1Axis = configureRangeIndex(0);
        String unitY1 = metadataElement.getAttribute(nameY1).getUnit();
        y1Axis.setLabel(getYAxisLabel(nameY1, unitY1));

        double[] y1AxisData = new double[1];
        Arrays.setAll(y1AxisData, i -> metadataElement.getAttribute(nameY1).getData().getElemDouble());
        dataset.addSeries(nameY1, new double[][]{xData, y1AxisData});

        if (!isValidYField(metadataElement, metadataElement.getName(), nameY2)) {
            return;
        }
        NumberAxis y2Axis = configureRangeIndex(1);
        String unitY2 = metadataElement.getAttribute(nameY2).getUnit();
        y2Axis.setLabel(getYAxisLabel(nameY2, unitY2));

        double[] y2AxisData = new double[1];
        Arrays.setAll(y2AxisData, i -> metadataElement.getAttribute(nameY2).getData().getElemDouble());
        dataset.addSeries(nameY2, new double[][]{xData, y2AxisData});


    }

    private void configureChartForArrayIndex(MetadataElement metadataElement, String nameX, String nameY1, String nameY2, String[] recordElementNames, String refRecordName) {
        if (!isValidArrayYField(metadataElement, refRecordName, nameY1)) {
            return;
        }
        xyItemRenderer.setBaseSeriesVisibleInLegend(false);
        xyPlot.getDomainAxis().setLabel(nameX);

        MetadataElement refElem = metadataElement.getElement(refRecordName);
        int arrayLength = (int) refElem.getAttribute(nameY1).getNumDataElems();
        double[] arrayIndices = new double[arrayLength];
        Arrays.setAll(arrayIndices, i -> i);

        NumberAxis y1Axis = configureRangeIndex(0);
        String unitY1 = metadataElement.getElement(refRecordName).getAttribute(nameY1).getUnit();
        y1Axis.setLabel(getYAxisLabel(nameY1, unitY1));

        for (String recordElementName : recordElementNames) {
            double[] y1AxisData = new double[arrayIndices.length];
            Arrays.setAll(y1AxisData, i -> metadataElement.getElement(recordElementName).getAttribute(nameY1).getData().getElemDoubleAt(i));
            String seriesKey = String.format("%s/%s", recordElementName, nameY1);
            dataset.addSeries(seriesKey, new double[][]{arrayIndices, y1AxisData});
        }

        if (!isValidArrayYField(metadataElement, refRecordName, nameY2)) {
            return;
        }
        NumberAxis y2Axis = configureRangeIndex(1);
        String unitY2 = metadataElement.getElement(refRecordName).getAttribute(nameY2).getUnit();
        y2Axis.setLabel(getYAxisLabel(nameY2, unitY2));

        for (String recordElementName : recordElementNames) {
            double[] y2AxisData = new double[arrayIndices.length];
            Arrays.setAll(y2AxisData, i -> metadataElement.getElement(recordElementName).getAttribute(nameY2).getData().getElemDoubleAt(i));
            String seriesKey = String.format("%s/%s", recordElementName, nameY2);
            dataset.addSeries(seriesKey, new double[][]{arrayIndices, y2AxisData});
        }
    }

    private void configureChartForRecordIndex(MetadataElement metadataElement, String nameX, String nameY1, String nameY2, String[] recordElementNames, String refRecordName, double[] recordIndices) {
        if (!isValidYField(metadataElement, refRecordName, nameY1)) {
            return;
        }

        xyPlot.getDomainAxis().setLabel(nameX);

        NumberAxis y1Axis = configureRangeIndex(0);
        String unitY1 = metadataElement.getElement(refRecordName).getAttribute(nameY1).getUnit();
        y1Axis.setLabel(getYAxisLabel(nameY1, unitY1));
        double[] y1AxisData = new double[recordIndices.length];
        Arrays.setAll(y1AxisData, i -> metadataElement.getElement(recordElementNames[i]).getAttribute(nameY1).getData().getElemDouble());
        dataset.addSeries(nameY1, new double[][]{recordIndices, y1AxisData});

        if (!isValidYField(metadataElement, recordElementNames[0], nameY2)) {
            return;
        }
        NumberAxis y2Axis = configureRangeIndex(1);
        String unitY2 = metadataElement.getElement(refRecordName).getAttribute(nameY2).getUnit();
        y2Axis.setLabel(getYAxisLabel(nameY2, unitY2));
        double[] y2AxisData = new double[recordIndices.length];
        Arrays.setAll(y2AxisData, i -> metadataElement.getElement(recordElementNames[i]).getAttribute(nameY2).getData().getElemDouble());
        dataset.addSeries(nameY2, new double[][]{recordIndices, y2AxisData});
    }

    private String getYAxisLabel(String name, String unit) {
        return name + (StringUtils.isNullOrEmpty(unit) || unit.equals("-") ? "" : (" in " + unit));
    }

    private void resetChart() {
        removeAllDatasetSeries();
        xyPlot.clearRangeAxes();
        xyPlot.getDomainAxis().setLabel(DEFAULT_X_AXIS_LABEL);
        xyItemRenderer.setBaseSeriesVisibleInLegend(true);
    }

    private boolean isValidYField(MetadataElement metadataElement, String elementName, String nameY) {
        MetadataElement element = metadataElement.getName().equals(elementName) ? metadataElement : metadataElement.getElement(elementName);
        return StringUtils.isNotNullAndNotEmpty(nameY) && !MetadataPlotSettings.FIELD_NAME_NONE.equals(nameY) &&
                element != null && element.getAttribute(nameY) != null;
    }

    private boolean isValidArrayYField(MetadataElement metadataElement, String elementName, String nameY) {
        MetadataElement element = metadataElement.getElement(elementName);
        if (element == null) {
            return false;
        }
        MetadataAttribute attribute = element.getAttribute(nameY);
        return attribute != null && StringUtils.isNotNullAndNotEmpty(nameY) && !MetadataPlotSettings.FIELD_NAME_NONE.equals(nameY) && attribute.getNumDataElems() > 1;
    }

    private void removeAllDatasetSeries() {
        int seriesCount = dataset.getSeriesCount();
        for (int i = 0; i < seriesCount; i++) {
            Comparable key = dataset.getSeriesKey(0);
            dataset.removeSeries(key);
        }
    }

    static double[] getRecordIndices(int startIndex, int recordsPerPlot, int numRecords) {
        int clippedStartIndex = Math.max(1, Math.min(startIndex, numRecords));
        int clippedEndIndex = Math.min(numRecords, Math.min((startIndex - 1) + recordsPerPlot, numRecords));
        double[] indexArray = new double[clippedEndIndex - clippedStartIndex + 1];
        Arrays.setAll(indexArray, index -> index + clippedStartIndex);
        return indexArray;
    }

    @Override
    protected String getDataAsText() {
        StringWriter sw = new StringWriter();
        try {
            encodeCsv(sw);
            sw.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return sw.toString();
    }

    private void encodeCsv(Writer writer) throws IOException {
        new TableModelCsvEncoder(new MetadataPlotTableModel(xyPlot)).encodeCsv(writer);
    }

    private JPanel createSettingsPanel(BindingContext bindingContext) {
        final JLabel datasetLabel = new JLabel("Dataset: ");
        final JComboBox<MetadataElement> datasetBox = new JComboBox<>();
        datasetBox.setRenderer(new ProductNodeListCellRenderer());
        JLabel recordLabel = new JLabel("Record: ");
        recordValueField = new JTextField(7);
        recordSlider = new JSlider(SwingConstants.HORIZONTAL, 1, 1, 1);
        recordSlider.setPaintTrack(true);
        recordSlider.setPaintTicks(true);
        recordSlider.setPaintLabels(true);
        configureSilderLabels(recordSlider);

        JLabel numRecordsLabel = new JLabel("Records / Plot: ");
        numRecSpinnerModel = new SpinnerNumberModel(1, 1, 1, 1);
        JSpinner numRecordsSpinner = new JSpinner(numRecSpinnerModel);
        numRecordsSpinner.setEditor(new JSpinner.NumberEditor(numRecordsSpinner, "#"));

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
        bindingContext.bind(PROP_NAME_RECORD_START_INDEX, recordValueField);
        bindingContext.bind(PROP_NAME_RECORD_START_INDEX, new SliderAdapter(recordSlider));
        bindingContext.bind(PROP_NAME_RECORDS_PER_PLOT, numRecordsSpinner);
        bindingContext.bind(PROP_NAME_FIELD_X, xFieldBox);
        bindingContext.bind(PROP_NAME_FIELD_Y1, y1FieldBox);
        bindingContext.bind(PROP_NAME_FIELD_Y2, y2FieldBox);

        TableLayout layout = new TableLayout(3);
        JPanel plotSettingsPanel = new JPanel(layout);

        layout.setTableWeightX(0.0);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(4, 4);

        layout.setCellWeightX(0, 1, 1.0);
        layout.setCellColspan(0, 1, 2);
        plotSettingsPanel.add(datasetLabel);
        plotSettingsPanel.add(datasetBox);

        layout.setCellWeightX(1, 1, 0.2);
        layout.setCellWeightX(1, 2, 0.8);
        plotSettingsPanel.add(recordLabel);
        plotSettingsPanel.add(recordValueField);
        plotSettingsPanel.add(recordSlider);

        layout.setCellWeightX(2, 1, 1.0);
        layout.setCellColspan(2, 1, 2);
        plotSettingsPanel.add(numRecordsLabel);
        plotSettingsPanel.add(numRecordsSpinner);

        layout.setCellWeightX(3, 1, 1.0);
        layout.setCellColspan(3, 1, 2);
        plotSettingsPanel.add(xFieldLabel);
        plotSettingsPanel.add(xFieldBox);

        layout.setCellWeightX(4, 1, 1.0);
        layout.setCellColspan(4, 1, 2);
        plotSettingsPanel.add(y1FieldLabel);
        plotSettingsPanel.add(y1FieldBox);

        layout.setCellWeightX(5, 1, 1.0);
        layout.setCellColspan(5, 1, 2);
        plotSettingsPanel.add(y2FieldLabel);
        plotSettingsPanel.add(y2FieldBox);

        updateSettings();
        updateUiState();

        return plotSettingsPanel;
    }

    private void configureSilderLabels(JSlider recordSlider) {
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        JLabel minLabel = new JLabel(String.valueOf(recordSlider.getMinimum()));
        labelTable.put(recordSlider.getMinimum(), minLabel);
        JLabel maxLabel = new JLabel(String.valueOf(recordSlider.getMaximum()));
        labelTable.put(recordSlider.getMaximum(), maxLabel);
        recordSlider.setLabelTable(labelTable);
    }

    private void updateUiState() {
        if (!isInitialized) {
            return;
        }

        int numRecords = plotSettings.getNumRecords();
        recordSlider.setMaximum(numRecords);
        configureSilderLabels(recordSlider);

        numRecSpinnerModel.setMaximum(numRecords);

        plotSettings.getContext().setComponentsEnabled(PROP_NAME_RECORD_START_INDEX, numRecords > 1);
        plotSettings.getContext().setComponentsEnabled(PROP_NAME_RECORDS_PER_PLOT, numRecords > 1);
        recordValueField.setEditable(numRecords > 1);

    }

    private void updateSettings() {
        Product product = getProduct();
        if (product == null) {
            plotSettings.setMetadataElements(null);
            return;
        }

        removeAllDatasetSeries();

        MetadataElement metadataRoot = product.getMetadataRoot();
        MetadataElement[] elements = metadataRoot.getElements();
        recordSlider.setValue(1);

        plotSettings.setMetadataElements(elements);

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

}

