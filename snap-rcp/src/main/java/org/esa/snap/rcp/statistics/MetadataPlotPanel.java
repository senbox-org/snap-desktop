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
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.util.RoiMaskSelector;
import org.esa.snap.ui.io.TableModelCsvEncoder;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.DefaultDrawingSupplier;
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
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Locale;
import java.util.TimeZone;

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
    private final static Paint[] DEFAULT_PAINT_ARRAY = ChartColor.createDefaultPaintArray();
    private final static Shape[] DEFAULT_SHAPE_ARRAY = DefaultDrawingSupplier.createStandardSeriesShapes();
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static final String TOOL_TIP_FORMAT = "{0}: {1}, {2}";
    public static final String ITEM_LABEL_FORMAT = "{2}";

    private MetadataPlotSettings plotSettings;
    private boolean isInitialized;
    private JSlider recordSlider;
    private SpinnerNumberModel numRecSpinnerModel;
    private XYPlot xyPlot;
    private JTextField recordValueField;


    MetadataPlotPanel(TopComponent parentComponent, String helpId) {
        super(parentComponent, helpId, CHART_TITLE, false);
    }

    @Override
    protected void initComponents() {
        if (hasAlternativeView()) {
            getAlternativeView().initComponents();
        }
        JFreeChart chart = ChartFactory.createXYLineChart(
                CHART_TITLE,
                DEFAULT_X_AXIS_LABEL,
                DEFAULT_SAMPLE_DATASET_NAME,
                new DefaultXYDataset(),
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        xyPlot = chart.getXYPlot();

        xyPlot.setNoDataMessage(NO_DATA_MESSAGE);
        xyPlot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));


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

    private DefaultXYItemRenderer creatItemRenderer(int index, int yDataType) {
        DefaultXYItemRenderer itemRenderer = new DefaultXYItemRenderer();
        itemRenderer.setSeriesPaint(0, DEFAULT_PAINT_ARRAY[index % DEFAULT_PAINT_ARRAY.length]);
        itemRenderer.setSeriesShape(0, DEFAULT_SHAPE_ARRAY[index % DEFAULT_SHAPE_ARRAY.length]);
        itemRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        final DecimalFormat numberFormat = new DecimalFormat("0.00#");
        numberFormat.setGroupingUsed(false);
        StandardXYToolTipGenerator toolTipGenerator;
        if (ProductData.TYPE_UTC == yDataType) {
            toolTipGenerator = new StandardXYToolTipGenerator(TOOL_TIP_FORMAT, numberFormat, SIMPLE_DATE_FORMAT);
        } else {
            toolTipGenerator = new StandardXYToolTipGenerator(TOOL_TIP_FORMAT, numberFormat, numberFormat);
        }
        itemRenderer.setSeriesToolTipGenerator(0, toolTipGenerator);

        StandardXYItemLabelGenerator itemLabelGenerator;
        if (ProductData.TYPE_UTC == yDataType) {
            itemLabelGenerator = new StandardXYItemLabelGenerator(ITEM_LABEL_FORMAT, numberFormat, SIMPLE_DATE_FORMAT);
        } else {
            itemLabelGenerator = new StandardXYItemLabelGenerator(ITEM_LABEL_FORMAT, numberFormat, numberFormat);
        }
        itemRenderer.setSeriesItemLabelGenerator(0, itemLabelGenerator);
        return itemRenderer;
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
                if (recordElementNames.length == 0) {
                    break;
                }
                if (metadataElement.containsElement(recordElementNames[0])) {
                    configureChartForDefault(metadataElement.getElement(recordElementNames[0]), nameX, nameY1, nameY2);
                } else {
                    configureChartForDefault(metadataElement, nameX, nameY1, nameY2);
                }
                break;
        }


    }

    private void configureChartForDefault(MetadataElement metadataElement, String nameX, String nameY1, String nameY2) {
        if (!isValidYField(metadataElement, metadataElement.getName(), nameY1)) {
            return;
        }

        MetadataAttribute xAttribute = metadataElement.getAttribute(nameX);
        int xDataType = getAttributeType(xAttribute);
        configureDomainAxis(0, nameX, xDataType);
        double[] xData = new double[1];
        Arrays.setAll(xData, i -> getDataAsDouble(xAttribute.getData()));

        MetadataAttribute y1Attribute = metadataElement.getAttribute(nameY1);
        int y1DataType = getAttributeType(y1Attribute);
        ValueAxis y1Axis = configureRangeIndex(0, y1DataType);
        String unitY1 = y1Attribute.getUnit();
        y1Axis.setLabel(getYAxisLabel(nameY1, unitY1));
        double[] y1AxisData = new double[1];
        Arrays.setAll(y1AxisData, i -> getDataAsDouble(y1Attribute.getData()));
        DefaultXYDataset dataset1 = new DefaultXYDataset();
        dataset1.addSeries(nameY1, new double[][]{xData, y1AxisData});
        xyPlot.setDataset(0, dataset1);
        xyPlot.mapDatasetToRangeAxis(0, 0);
        xyPlot.setRenderer(0, creatItemRenderer(0, y1DataType));

        if (!isValidYField(metadataElement, metadataElement.getName(), nameY2)) {
            return;
        }

        MetadataAttribute y2Attribute = metadataElement.getAttribute(nameY2);
        int y2DataType = getAttributeType(y2Attribute);
        ValueAxis y2Axis = configureRangeIndex(1, y2DataType);
        String unitY2 = y2Attribute.getUnit();
        y2Axis.setLabel(getYAxisLabel(nameY2, unitY2));
        double[] y2AxisData = new double[1];
        Arrays.setAll(y2AxisData, i -> getDataAsDouble(y2Attribute.getData()));
        DefaultXYDataset dataset2 = new DefaultXYDataset();
        dataset2.addSeries(nameY2, new double[][]{xData, y2AxisData});
        xyPlot.setDataset(1, dataset2);
        xyPlot.mapDatasetToRangeAxis(1, 1);
        xyPlot.setRenderer(1, creatItemRenderer(1, y1DataType));
    }

    private void configureChartForArrayIndex(MetadataElement metadataElement, String nameX, String nameY1, String nameY2, String[] recordElementNames,
                                             String refRecordName) {
        if (!isValidArrayYField(metadataElement, refRecordName, nameY1)) {
            return;
        }
        configureDomainAxis(0, nameX, ProductData.TYPE_INT32);

        MetadataElement refElem = metadataElement.getElement(refRecordName);
        int y1ArrayLength = (int) refElem.getAttribute(nameY1).getNumDataElems();
        double[] y1ArrayIndices = new double[y1ArrayLength];
        Arrays.setAll(y1ArrayIndices, i -> i);

        MetadataAttribute y1Attribute = metadataElement.getElement(refRecordName).getAttribute(nameY1);
        int y1DataType = getAttributeType(y1Attribute);
        ValueAxis y1Axis = configureRangeIndex(0, y1DataType);
        String unitY1 = y1Attribute.getUnit();
        y1Axis.setLabel(getYAxisLabel(nameY1, unitY1));
        int dataSetCnt = 0;
        for (int i = 0; i < recordElementNames.length; i++, dataSetCnt++) {
            String recordElementName = recordElementNames[i];
            addArrayDataToSeries(0, 0, dataSetCnt, nameY1, metadataElement, y1ArrayIndices, recordElementName);
        }

        if (!isValidArrayYField(metadataElement, refRecordName, nameY2)) {
            return;
        }

        MetadataAttribute y2Attribute = metadataElement.getElement(refRecordName).getAttribute(nameY2);
        int y2DataType = getAttributeType(y2Attribute);
        ValueAxis y2Axis = configureRangeIndex(1, y2DataType);
        String unitY2 = y2Attribute.getUnit();
        y2Axis.setLabel(getYAxisLabel(nameY2, unitY2));

        int y2ArrayLength = (int) refElem.getAttribute(nameY2).getNumDataElems();
        double[] y2ArrayIndices = new double[y2ArrayLength];
        Arrays.setAll(y2ArrayIndices, i -> i);

        if (y2ArrayLength != y1ArrayLength) {
            configureDomainAxis(0, nameY1 + " - " + nameX, ProductData.TYPE_INT32);
            configureDomainAxis(1, nameY2 + " - " + nameX, ProductData.TYPE_INT32);
        }

        for (int i = 0; i < recordElementNames.length; i++, dataSetCnt++) {
            String recordElementName = recordElementNames[i];
            addArrayDataToSeries(y2ArrayLength != y1ArrayLength ? 1 : 0, 1, dataSetCnt, nameY2, metadataElement, y2ArrayIndices, recordElementName);
        }
    }

    private void addArrayDataToSeries(int domainAxisIndex, int rangeAxisIndex, int datasetIndex, String yName, MetadataElement metadataElement,
                                      double[] arrayIndices, String recordElementName) {
        double[] yAxisData = new double[arrayIndices.length];
        ProductData attributeData = metadataElement.getElement(recordElementName).getAttribute(yName).getData();
        Arrays.setAll(yAxisData, attributeData::getElemDoubleAt);
        String seriesKey = String.format("%s/%s", recordElementName, yName);
        DefaultXYDataset dataset = new DefaultXYDataset();
        dataset.addSeries(seriesKey, new double[][]{arrayIndices, yAxisData});
        xyPlot.setDataset(datasetIndex, dataset);
        xyPlot.mapDatasetToRangeAxis(datasetIndex, rangeAxisIndex);
        xyPlot.mapDatasetToDomainAxis(datasetIndex, domainAxisIndex);
        xyPlot.setRenderer(datasetIndex, creatItemRenderer(datasetIndex, ProductData.TYPE_INT32));
    }

    private void configureChartForRecordIndex(MetadataElement metadataElement, String nameX, String nameY1, String nameY2,
                                              String[] recordElementNames, String refRecordName, double[] recordIndices) {
        if (!isValidYField(metadataElement, refRecordName, nameY1)) {
            return;
        }

        configureRangeAxis(0, metadataElement, nameY1, recordElementNames, refRecordName, recordIndices);

        configureDomainAxis(0, nameX, ProductData.TYPE_INT32);

        if (!isValidYField(metadataElement, recordElementNames[0], nameY2)) {
            return;
        }
        configureRangeAxis(1, metadataElement, nameY2, recordElementNames, refRecordName, recordIndices);
    }

    private void configureRangeAxis(int index, MetadataElement metadataElement, String yAttributeName, String[] recordElementNames,
                                    String refRecordName,
                                    double[] recordIndices) {
        double[] yAxisData = new double[recordIndices.length];
        Arrays.setAll(yAxisData, i -> getDataAsDouble(metadataElement.getElement(recordElementNames[i]).getAttribute(yAttributeName).getData()));
        DefaultXYDataset dataset2 = new DefaultXYDataset();
        dataset2.addSeries(yAttributeName, new double[][]{recordIndices, yAxisData});
        xyPlot.setDataset(index, dataset2);
        xyPlot.mapDatasetToRangeAxis(index, index);

        int yDataType = getAttributeType(metadataElement.getElement(refRecordName).getAttribute(yAttributeName));
        ValueAxis yAxis = configureRangeIndex(index, yDataType);
        String yUnit = metadataElement.getElement(refRecordName).getAttribute(yAttributeName).getUnit();
        yAxis.setLabel(getYAxisLabel(yAttributeName, yUnit));
        xyPlot.setRenderer(index, creatItemRenderer(index, yDataType));
    }

    private ValueAxis configureRangeIndex(int index, int dataType) {
        ValueAxis axis = createAxis(dataType);
        axis.setAutoRange(true);
        Font axisFont = axis.getLabelFont().deriveFont(Font.BOLD);
        axis.setLabelFont(axisFont);
        axis.setLabel(String.format("Y%d Samples", index + 1));
        xyPlot.setRangeAxis(index, axis);
        xyPlot.setRangeAxisLocation(index, index == 0 ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.BOTTOM_OR_RIGHT);
        return axis;
    }

    private ValueAxis createAxis(int dataType) {
        ValueAxis axis;
        if (ProductData.TYPE_UTC == dataType) {
            axis = new DateAxis("Date", TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
            ((DateAxis) axis).setDateFormatOverride(SIMPLE_DATE_FORMAT);
        } else {
            axis = new NumberAxis();
            ((NumberAxis) axis).setAutoRangeIncludesZero(false);
        }
        return axis;
    }

    private void configureDomainAxis(int index, String nameX, int dataType) {
        ValueAxis axis = createAxis(dataType);
        axis.setAutoRange(true);
        axis.setAutoRangeMinimumSize(2);
        axis.setLabel(nameX);
        Font axisFont = axis.getLabelFont().deriveFont(Font.BOLD);
        axis.setLabelFont(axisFont);
        xyPlot.setDomainAxis(index, axis);
    }

    private int getAttributeType(MetadataAttribute attribute) {
        return ProductData.getType(attribute.getData().getTypeString());
    }

    private double getDataAsDouble(ProductData data) {
        if (data instanceof ProductData.UTC) {
            return ((ProductData.UTC) data).getAsDate().getTime();
        } else {
            return data.getElemDouble();
        }
    }

    private String getYAxisLabel(String name, String unit) {
        return name + (StringUtils.isNullOrEmpty(unit) || unit.equals("-") ? "" : (" in " + unit));
    }

    private void resetChart() {
        removeAllDatasetSeries();
        xyPlot.clearRangeAxes();
        xyPlot.clearDomainAxes();
        configureDomainAxis(0, DEFAULT_X_AXIS_LABEL, ProductData.TYPE_FLOAT64);
        xyPlot.getRenderer().setBaseSeriesVisibleInLegend(true);
    }

    private boolean isValidYField(MetadataElement metadataElement, String elementName, String nameY) {
        MetadataElement element = metadataElement.getName().equals(elementName) ? metadataElement : metadataElement.getElement(elementName);
        return StringUtils.isNotNullAndNotEmpty(nameY) && !MetadataPlotSettings.FIELD_NAME_NONE.equals(nameY) &&
               element != null && element.getAttribute(nameY) != null;
    }

    private boolean isValidArrayYField(MetadataElement metadataElement, String elementName, String nameY) {
        MetadataElement element = metadataElement.getElement(elementName);
        if (element == null || StringUtils.isNullOrEmpty(nameY)) {
            return false;
        }
        MetadataAttribute attribute = element.getAttribute(nameY);
        return attribute != null && !MetadataPlotSettings.FIELD_NAME_NONE.equals(
                nameY) && attribute.getNumDataElems() > 1;
    }

    private void removeAllDatasetSeries() {
        int datasetCount = xyPlot.getDatasetCount();
        for (int i = 0; i < datasetCount; i++) {
            xyPlot.setDataset(i, null);
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

