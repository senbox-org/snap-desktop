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

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.util.RoiMaskSelector;
import org.esa.snap.ui.GridBagUtils;
import org.geotools.feature.FeatureCollection;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.function.Function2D;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.openide.windows.TopComponent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * The scatter plot pane within the statistics window.
 *
 * @author Olaf Danne
 * @author Sabine Embacher
 */
class ScatterPlotPanel extends ChartPagePanel {

    public static final String CHART_TITLE = "Correlative Plot";
    private static final String NO_DATA_MESSAGE = "No correlative plot computed yet.\n" +
            "To create a correlative plot\n" +
            "   -Select a band" + "\n" +
            "   -Select vector data (e.g., a SeaDAS 6.x track)" + "\n" +
            "   -Select the data as point data source" + "\n" +
            "   -Select a data field" + "\n" +
            HELP_TIP_MESSAGE + "\n" +
            ZOOM_TIP_MESSAGE;

    private final String PROPERTY_NAME_X_AXIS_LOG_SCALED = "xAxisLogScaled";
    private final String PROPERTY_NAME_Y_AXIS_LOG_SCALED = "yAxisLogScaled";
    private final String PROPERTY_NAME_DATA_FIELD = "dataField";
    private final String PROPERTY_NAME_POINT_DATA_SOURCE = "pointDataSource";
    private final String PROPERTY_NAME_BOX_SIZE = "boxSize";
    private final String PROPERTY_NAME_SHOW_ACCEPTABLE_DEVIATION = "showAcceptableDeviation";
    private final String PROPERTY_NAME_ACCEPTABLE_DEVIATION = "acceptableDeviationInterval";
    private final String PROPERTY_NAME_SHOW_REGRESSION_LINE = "showRegressionLine";

    private final ScatterPlotModel scatterPlotModel;
    private final BindingContext bindingContext;
    private final AxisRangeControl xAxisRangeControl;
    private final AxisRangeControl yAxisRangeControl;
    private final XYIntervalSeriesCollection scatterpointsDataset;
    private final XYIntervalSeriesCollection acceptableDeviationDataset;
    private final XYIntervalSeriesCollection regressionDataset;

    private final JFreeChart chart;
    private final ProductManager.Listener productRemovedListener;
    private final Map<Product, UserSettings> userSettingsMap;
    private ChartPanel scatterPlotDisplay;
    private ComputedData[] computedDatas;
    private CorrelativeFieldSelector correlativeFieldSelector;
    private Range xAutoRangeAxisRange;
    private Range yAutoRangeAxisRange;
    private AxisChangeListener domainAxisChangeListener;
    private boolean computingData;
    private XYTitleAnnotation r2Annotation;

    ScatterPlotPanel(TopComponent parentDialog, String helpId) {
        super(parentDialog, helpId, CHART_TITLE, false);
        userSettingsMap = new HashMap<>();
        productRemovedListener = new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                final UserSettings userSettings = userSettingsMap.remove(event.getProduct());
                if (userSettings != null) {
                    userSettings.dispose();
                }
            }
        };

        xAxisRangeControl = new AxisRangeControl("X-Axis");
        yAxisRangeControl = new AxisRangeControl("Y-Axis");
        scatterPlotModel = new ScatterPlotModel();
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(scatterPlotModel));
        scatterpointsDataset = new XYIntervalSeriesCollection();
        acceptableDeviationDataset = new XYIntervalSeriesCollection();
        regressionDataset = new XYIntervalSeriesCollection();
        r2Annotation = new XYTitleAnnotation(0, 0, new TextTitle(""));
        chart = ChartFactory.createScatterPlot(CHART_TITLE, "", "", scatterpointsDataset, PlotOrientation.VERTICAL,
                true, true, false);
        chart.getXYPlot().setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        createDomainAxisChangeListener();
        final PropertyChangeListener userSettingsUpdateListener = evt -> {
            if (getRaster() != null) {
                final VectorDataNode pointDataSourceValue = scatterPlotModel.pointDataSource;
                final AttributeDescriptor dataFieldValue = scatterPlotModel.dataField;
                final UserSettings userSettings = getUserSettings(getRaster().getProduct());
                userSettings.set(getRaster().getName(), pointDataSourceValue, dataFieldValue);
            }
        };

        bindingContext.addPropertyChangeListener(PROPERTY_NAME_DATA_FIELD, userSettingsUpdateListener);
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_POINT_DATA_SOURCE, userSettingsUpdateListener);
    }

    @Override
    protected void handleLayerContentChanged() {
        computeChartDataIfPossible();
    }

    @Override
    protected String getDataAsText() {
        if (scatterpointsDataset.getItemCount(0) > 0) {
            final ScatterPlotTableModel scatterPlotTableModel;
            scatterPlotTableModel = new ScatterPlotTableModel(getRasterName(),
                    getCorrelativeDataName(),
                    computedDatas);
            return scatterPlotTableModel.toCVS();
        }
        return "";
    }

    @Override
    protected void initComponents() {
        getAlternativeView().initComponents();
        initParameters();
        createUI();
        SnapApp.getDefault().getProductManager().addListener(productRemovedListener);
    }

    @Override
    protected void updateComponents() {
        super.updateComponents();
        if (!isVisible()) {
            return;
        }
        final AttributeDescriptor dataField = scatterPlotModel.dataField;
        xAxisRangeControl.setTitleSuffix(dataField != null ? dataField.getLocalName() : null);

        final RasterDataNode raster = getRaster();
        yAxisRangeControl.setTitleSuffix(raster != null ? raster.getName() : null);


        if (raster != null) {
            final Product product = getProduct();
            final String rasterName = raster.getName();

            final UserSettings userSettings = getUserSettings(product);
            final VectorDataNode userSelectedPointDataSource = userSettings.getPointDataSource(rasterName);
            final AttributeDescriptor userSelectedDataField = userSettings.getDataField(rasterName);

            correlativeFieldSelector.updatePointDataSource(product);
            correlativeFieldSelector.updateDataField();

            if (userSelectedPointDataSource != null) {
                correlativeFieldSelector.tryToSelectPointDataSource(userSelectedPointDataSource);
            }
            if (userSelectedDataField != null) {
                correlativeFieldSelector.tryToSelectDataField(userSelectedDataField);
            }
        }

        if (isRasterChanged()) {
            getPlot().getRangeAxis().setLabel(StatisticChartStyling.getAxisLabel(raster, "X", false));
            computeChartDataIfPossible();
        }
    }

    private String getCorrelativeDataName() {
        return scatterPlotModel.dataField.getLocalName();
    }

    @Override
    protected void updateChartData() {
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
        if (event.getSourceNode() instanceof Placemark) {
            updateComponents();
        }
    }

    @Override
    public void nodeRemoved(ProductNodeEvent event) {
        if (event.getSourceNode() instanceof VectorDataNode) {
            updateComponents();
            computeChartDataIfPossible();
        }
    }

    @Override
    protected void showAlternativeView() {
        final TableModel model;
        if (computedDatas != null && computedDatas.length > 0) {
            model = new ScatterPlotTableModel(getRasterName(),
                    getCorrelativeDataName(),
                    computedDatas);
        } else {
            model = new DefaultTableModel();
        }
        final TableViewPagePanel alternativPanel = (TableViewPagePanel) getAlternativeView();
        alternativPanel.setModel(model);
        super.showAlternativeView();
    }

    private String getRasterName() {
        return getRaster() != null ? getRaster().getName() : "";
    }

    private void initParameters() {

        final PropertyChangeListener recomputeListener = evt -> computeChartDataIfPossible();

        bindingContext.addPropertyChangeListener(RoiMaskSelector.PROPERTY_NAME_USE_ROI_MASK, recomputeListener);
        bindingContext.addPropertyChangeListener(RoiMaskSelector.PROPERTY_NAME_ROI_MASK, recomputeListener);
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_BOX_SIZE, recomputeListener);
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_DATA_FIELD, recomputeListener);

        final PropertyChangeListener computeLineDataListener = evt -> computeRegressionAndAcceptableDeviationData();
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_SHOW_ACCEPTABLE_DEVIATION, computeLineDataListener);
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_ACCEPTABLE_DEVIATION, computeLineDataListener);
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_SHOW_REGRESSION_LINE, computeLineDataListener);


        final PropertyChangeListener rangeLabelUpdateListener = evt -> {
            final VectorDataNode pointDataSource = scatterPlotModel.pointDataSource;
            final AttributeDescriptor dataField = scatterPlotModel.dataField;
            if (dataField != null && pointDataSource != null) {
                final String dataFieldName = dataField.getLocalName();
                getPlot().getDomainAxis().setLabel(dataFieldName);
                xAxisRangeControl.setTitleSuffix(dataFieldName);
            } else {
                getPlot().getDomainAxis().setLabel("");
                xAxisRangeControl.setTitleSuffix("");
            }
        };

        bindingContext.addPropertyChangeListener(PROPERTY_NAME_DATA_FIELD, rangeLabelUpdateListener);
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_POINT_DATA_SOURCE, rangeLabelUpdateListener);

        bindingContext.addPropertyChangeListener(PROPERTY_NAME_X_AXIS_LOG_SCALED, evt -> updateScalingOfXAxis());
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_Y_AXIS_LOG_SCALED, evt -> updateScalingOfYAxis());

        xAxisRangeControl.getBindingContext().addPropertyChangeListener(
                evt -> handleAxisRangeControlChanges(evt, xAxisRangeControl, getPlot().getDomainAxis(), xAutoRangeAxisRange));
        yAxisRangeControl.getBindingContext().addPropertyChangeListener(
                evt -> handleAxisRangeControlChanges(evt, yAxisRangeControl, getPlot().getRangeAxis(), yAutoRangeAxisRange));


    }

    private void handleAxisRangeControlChanges(PropertyChangeEvent evt, AxisRangeControl axisRangeControl,
                                               ValueAxis valueAxis, Range computedAutoRange) {
        final String propertyName = evt.getPropertyName();
        switch (propertyName) {
            case AxisRangeControl.PROPERTY_NAME_AUTO_MIN_MAX:
                if (axisRangeControl.isAutoMinMax()) {
                    final double min = computedAutoRange.getLowerBound();
                    final double max = computedAutoRange.getUpperBound();
                    axisRangeControl.adjustComponents(min, max, 3);
                }
                break;
            case AxisRangeControl.PROPERTY_NAME_MIN:
                valueAxis.setLowerBound(axisRangeControl.getMin());
                break;
            case AxisRangeControl.PROPERTY_NAME_MAX:
                valueAxis.setUpperBound(axisRangeControl.getMax());
                break;
        }
    }

    private void createUI() {

        final XYPlot plot = getPlot();
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setNoDataMessage(NO_DATA_MESSAGE);
        int confidenceDSIndex = 0;
        int regressionDSIndex = 1;
        int scatterpointsDSIndex = 2;
        plot.setDataset(confidenceDSIndex, acceptableDeviationDataset);
        plot.setDataset(regressionDSIndex, regressionDataset);
        plot.setDataset(scatterpointsDSIndex, scatterpointsDataset);

        plot.addAnnotation(r2Annotation);

        final DeviationRenderer identityRenderer = new DeviationRenderer(true, false);
        identityRenderer.setSeriesPaint(0, StatisticChartStyling.SAMPLE_DATA_PAINT);
        identityRenderer.setSeriesFillPaint(0, StatisticChartStyling.SAMPLE_DATA_FILL_PAINT);
        plot.setRenderer(confidenceDSIndex, identityRenderer);

        final DeviationRenderer regressionRenderer = new DeviationRenderer(true, false);
        regressionRenderer.setSeriesPaint(0, StatisticChartStyling.REGRESSION_DATA_PAINT);
        regressionRenderer.setSeriesFillPaint(0, StatisticChartStyling.REGRESSION_DATA_FILL_PAINT);
        plot.setRenderer(regressionDSIndex, regressionRenderer);

        final XYErrorRenderer scatterPointsRenderer = new XYErrorRenderer();
        scatterPointsRenderer.setDrawXError(true);
        scatterPointsRenderer.setErrorStroke(new BasicStroke(1));
        scatterPointsRenderer.setErrorPaint(StatisticChartStyling.CORRELATIVE_POINT_OUTLINE_PAINT);
        scatterPointsRenderer.setSeriesShape(0, StatisticChartStyling.CORRELATIVE_POINT_SHAPE);
        scatterPointsRenderer.setSeriesOutlinePaint(0, StatisticChartStyling.CORRELATIVE_POINT_OUTLINE_PAINT);
        scatterPointsRenderer.setSeriesFillPaint(0, StatisticChartStyling.CORRELATIVE_POINT_FILL_PAINT);
        scatterPointsRenderer.setSeriesLinesVisible(0, false);
        scatterPointsRenderer.setSeriesShapesVisible(0, true);
        scatterPointsRenderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
        scatterPointsRenderer.setSeriesToolTipGenerator(0, (dataset, series, item) -> {
            final XYIntervalSeriesCollection collection = (XYIntervalSeriesCollection) dataset;
            final Comparable key = collection.getSeriesKey(series);
            final double xValue = collection.getXValue(series, item);
            final double endYValue = collection.getEndYValue(series, item);
            final double yValue = collection.getYValue(series, item);
            return String.format("%s: mean = %6.2f, sigma = %6.2f | %s: value = %6.2f",
                    getRasterName(), yValue, endYValue - yValue,
                    key, xValue);
        });
        plot.setRenderer(scatterpointsDSIndex, scatterPointsRenderer);

        final boolean autoRangeIncludesZero = false;
        final boolean xLog = scatterPlotModel.xAxisLogScaled;
        final boolean yLog = scatterPlotModel.yAxisLogScaled;
        plot.setDomainAxis(
                StatisticChartStyling.updateScalingOfAxis(xLog, plot.getDomainAxis(), autoRangeIncludesZero));
        plot.setRangeAxis(StatisticChartStyling.updateScalingOfAxis(yLog, plot.getRangeAxis(), autoRangeIncludesZero));

        createUI(createChartPanel(chart), createInputParameterPanel(), bindingContext);

        plot.getDomainAxis().addChangeListener(domainAxisChangeListener);
        scatterPlotDisplay.setMouseWheelEnabled(true);
        scatterPlotDisplay.setMouseZoomable(true);
    }

    private void createDomainAxisChangeListener() {
        domainAxisChangeListener = event -> {
            if (!computingData) {
                computeRegressionAndAcceptableDeviationData();
            }
        };
    }

    private ChartPanel createChartPanel(final JFreeChart chart) {
        scatterPlotDisplay = new ChartPanel(chart) {
            @Override
            public void restoreAutoBounds() {
                // here we tweak the notify flag on the plot so that only
                // one notification happens even though we update multiple
                // axes...
                final XYPlot plot = chart.getXYPlot();
                boolean savedNotify = plot.isNotify();
                plot.setNotify(false);
                xAxisRangeControl.adjustAxis(plot.getDomainAxis(), 3);
                yAxisRangeControl.adjustAxis(plot.getRangeAxis(), 3);
                plot.setNotify(savedNotify);
            }
        };

        MaskSelectionToolSupport maskSelectionToolSupport = new MaskSelectionToolSupport(this,
                scatterPlotDisplay,
                "correlative_plot_area",
                "Mask generated from selected correlative plot area",
                Color.RED,
                PlotAreaSelectionTool.AreaType.Y_RANGE) {
            @Override
            protected String createMaskExpression(PlotAreaSelectionTool.AreaType areaType, Shape shape) {
                Rectangle2D bounds = shape.getBounds2D();
                return createMaskExpression(bounds.getMinY(), bounds.getMaxY());
            }

            protected String createMaskExpression(double x1, double x2) {
                String bandName = BandArithmetic.createExternalName(getRaster().getName());
                return String.format("%s >= %s && %s <= %s", bandName, x1, bandName, x2);
            }
        };
        scatterPlotDisplay.getPopupMenu().addSeparator();
        scatterPlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createMaskSelectionModeMenuItem());
        scatterPlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createDeleteMaskMenuItem());
        scatterPlotDisplay.getPopupMenu().addSeparator();
        scatterPlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());
        return scatterPlotDisplay;
    }

    private JPanel createInputParameterPanel() {
        final PropertyDescriptor boxSizeDescriptor = bindingContext.getPropertySet().getDescriptor(
                PROPERTY_NAME_BOX_SIZE);
        boxSizeDescriptor.setValueRange(new ValueRange(1, 101));
        boxSizeDescriptor.setAttribute("stepSize", 2);
        boxSizeDescriptor.setValidator((property, value) -> {
            if (((Number) value).intValue() % 2 == 0) {
                throw new ValidationException("Only odd values allowed as box size.");
            }
        });
        final JSpinner boxSizeSpinner = new JSpinner();
        bindingContext.bind(PROPERTY_NAME_BOX_SIZE, boxSizeSpinner);

        final JPanel boxSizePanel = new JPanel(new BorderLayout(5, 3));
        boxSizePanel.add(new JLabel("Box size:"), BorderLayout.WEST);
        boxSizePanel.add(boxSizeSpinner);

        correlativeFieldSelector = new CorrelativeFieldSelector(bindingContext);

        final JPanel pointDataSourcePanel = new JPanel(new BorderLayout(5, 3));
        pointDataSourcePanel.add(correlativeFieldSelector.pointDataSourceLabel, BorderLayout.NORTH);
        pointDataSourcePanel.add(correlativeFieldSelector.pointDataSourceList);

        final JPanel pointDataFieldPanel = new JPanel(new BorderLayout(5, 3));
        pointDataFieldPanel.add(correlativeFieldSelector.dataFieldLabel, BorderLayout.NORTH);
        pointDataFieldPanel.add(correlativeFieldSelector.dataFieldList);

        final JCheckBox xLogCheck = new JCheckBox("Log10 scaled");
        bindingContext.bind(PROPERTY_NAME_X_AXIS_LOG_SCALED, xLogCheck);
        final JPanel xAxisOptionPanel = new JPanel(new BorderLayout());
        xAxisOptionPanel.add(xAxisRangeControl.getPanel());
        xAxisOptionPanel.add(xLogCheck, BorderLayout.SOUTH);

        final JCheckBox yLogCheck = new JCheckBox("Log10 scaled");
        bindingContext.bind(PROPERTY_NAME_Y_AXIS_LOG_SCALED, yLogCheck);
        final JPanel yAxisOptionPanel = new JPanel(new BorderLayout());
        yAxisOptionPanel.add(yAxisRangeControl.getPanel());
        yAxisOptionPanel.add(yLogCheck, BorderLayout.SOUTH);

        final JCheckBox acceptableCheck = new JCheckBox("Show tolerance range");
        JLabel fieldPrefix = new JLabel("+/-");
        final JTextField acceptableField = new JTextField();
        acceptableField.setPreferredSize(new Dimension(40, acceptableField.getPreferredSize().height));
        acceptableField.setHorizontalAlignment(JTextField.RIGHT);
        final JLabel percentLabel = new JLabel(" %");
        bindingContext.bind(PROPERTY_NAME_SHOW_ACCEPTABLE_DEVIATION, acceptableCheck);
        bindingContext.bind(PROPERTY_NAME_ACCEPTABLE_DEVIATION, acceptableField);
        bindingContext.getBinding(PROPERTY_NAME_ACCEPTABLE_DEVIATION).addComponent(percentLabel);
        bindingContext.getBinding(PROPERTY_NAME_ACCEPTABLE_DEVIATION).addComponent(fieldPrefix);
        bindingContext.bindEnabledState(PROPERTY_NAME_ACCEPTABLE_DEVIATION, true,
                PROPERTY_NAME_SHOW_ACCEPTABLE_DEVIATION, true);

        final JPanel confidencePanel = GridBagUtils.createPanel();
        GridBagConstraints confidencePanelConstraints = GridBagUtils.createConstraints(
                "anchor=NORTHWEST,fill=HORIZONTAL,insets.top=5,weighty=0,weightx=1");
        GridBagUtils.addToPanel(confidencePanel, acceptableCheck, confidencePanelConstraints, "gridy=0,gridwidth=3");
        GridBagUtils.addToPanel(confidencePanel, fieldPrefix, confidencePanelConstraints,
                "weightx=0,insets.left=22,gridy=1,gridx=0,insets.top=4,gridwidth=1");
        GridBagUtils.addToPanel(confidencePanel, acceptableField, confidencePanelConstraints,
                "weightx=1,gridx=1,insets.left=2,insets.top=2");
        GridBagUtils.addToPanel(confidencePanel, percentLabel, confidencePanelConstraints,
                "weightx=0,gridx=2,insets.left=0,insets.top=4");

        final JCheckBox regressionCheck = new JCheckBox("Show regression line");
        bindingContext.bind(PROPERTY_NAME_SHOW_REGRESSION_LINE, regressionCheck);

        // UI arrangement

        JPanel middlePanel = GridBagUtils.createPanel();
        GridBagConstraints middlePanelConstraints = GridBagUtils.createConstraints(
                "anchor=NORTHWEST,fill=HORIZONTAL,insets.top=6,weighty=0,weightx=1");
        GridBagUtils.addToPanel(middlePanel, boxSizePanel, middlePanelConstraints, "gridy=0,insets.left=6");
        GridBagUtils.addToPanel(middlePanel, pointDataSourcePanel, middlePanelConstraints, "gridy=1");
        GridBagUtils.addToPanel(middlePanel, pointDataFieldPanel, middlePanelConstraints, "gridy=2");
        GridBagUtils.addToPanel(middlePanel, xAxisOptionPanel, middlePanelConstraints, "gridy=3,insets.left=0");
        GridBagUtils.addToPanel(middlePanel, yAxisOptionPanel, middlePanelConstraints, "gridy=4");
        GridBagUtils.addToPanel(middlePanel, new JSeparator(), middlePanelConstraints, "gridy=5,insets.left=4");
        GridBagUtils.addToPanel(middlePanel, confidencePanel, middlePanelConstraints,
                "gridy=6,fill=HORIZONTAL,insets.left=-4");
        GridBagUtils.addToPanel(middlePanel, regressionCheck, middlePanelConstraints,
                "gridy=7,insets.left=-4,insets.top=8");

        return middlePanel;
    }

    private void updateScalingOfXAxis() {
        final boolean logScaled = scatterPlotModel.xAxisLogScaled;
        final ValueAxis oldAxis = getPlot().getDomainAxis();
        ValueAxis newAxis = StatisticChartStyling.updateScalingOfAxis(logScaled, oldAxis, false);
        oldAxis.removeChangeListener(domainAxisChangeListener);
        newAxis.addChangeListener(domainAxisChangeListener);
        getPlot().setDomainAxis(newAxis);
        finishScalingUpdate(xAxisRangeControl, newAxis, oldAxis);
    }

    private void updateScalingOfYAxis() {
        final boolean logScaled = scatterPlotModel.yAxisLogScaled;
        final ValueAxis oldAxis = getPlot().getRangeAxis();
        ValueAxis newAxis = StatisticChartStyling.updateScalingOfAxis(logScaled, oldAxis, false);
        getPlot().setRangeAxis(newAxis);
        finishScalingUpdate(yAxisRangeControl, newAxis, oldAxis);
    }

    private void finishScalingUpdate(AxisRangeControl axisRangeControl, ValueAxis newAxis, ValueAxis oldAxis) {
        if (axisRangeControl.isAutoMinMax()) {
            newAxis.setAutoRange(false);
            acceptableDeviationDataset.removeAllSeries();
            regressionDataset.removeAllSeries();
            getPlot().removeAnnotation(r2Annotation);
            newAxis.setAutoRange(true);
            axisRangeControl.adjustComponents(newAxis, 3);
            newAxis.setAutoRange(false);
            computeRegressionAndAcceptableDeviationData();
        } else {
            newAxis.setAutoRange(false);
            newAxis.setRange(oldAxis.getRange());
        }
    }

    private XYPlot getPlot() {
        return chart.getXYPlot();
    }

    private void computeChartDataIfPossible() {
        // need to do this later: all GUI events must be processed first in order to get the correct state
        SwingUtilities.invokeLater(() -> {
            if (scatterPlotModel.pointDataSource != null
                    && scatterPlotModel.dataField != null
                    && scatterPlotModel.pointDataSource.getFeatureCollection() != null
                    && scatterPlotModel.pointDataSource.getFeatureCollection().features() != null
                    && scatterPlotModel.pointDataSource.getFeatureCollection().features().hasNext()
                    && scatterPlotModel.pointDataSource.getFeatureCollection().features().next() != null
                    && scatterPlotModel.pointDataSource.getFeatureCollection().features().next().getAttribute(
                    scatterPlotModel.dataField.getLocalName()) != null
                    && getRaster() != null) {
                compute(scatterPlotModel.useRoiMask ? scatterPlotModel.roiMask : null);
            } else {
                scatterpointsDataset.removeAllSeries();
                acceptableDeviationDataset.removeAllSeries();
                regressionDataset.removeAllSeries();
                getPlot().removeAnnotation(r2Annotation);
                computedDatas = null;
            }
        });
    }

    private void compute(final Mask selectedMask) {

        final RasterDataNode raster = getRaster();

        final AttributeDescriptor dataField = scatterPlotModel.dataField;
        if (raster == null || dataField == null) {
            return;
        }

        SwingWorker<ComputedData[], Object> swingWorker = new SwingWorker<ComputedData[], Object>() {

            @Override
            protected ComputedData[] doInBackground() throws Exception {
                SystemUtils.LOG.finest("start computing scatter plot data");

                final List<ComputedData> computedDataList = new ArrayList<>();

                final FeatureCollection<SimpleFeatureType, SimpleFeature> collection = scatterPlotModel.pointDataSource.getFeatureCollection();
                final SimpleFeature[] features = collection.toArray(new SimpleFeature[collection.size()]);

                final int boxSize = scatterPlotModel.boxSize;

                final Rectangle sceneRect = new Rectangle(raster.getRasterWidth(), raster.getRasterHeight());

                final GeoCoding geoCoding = raster.getGeoCoding();
                final AffineTransform imageToModelTransform;
                imageToModelTransform = Product.findImageToModelTransform(geoCoding);
                for (SimpleFeature feature : features) {
                    final Point point = (Point) feature.getDefaultGeometryProperty().getValue();
                    Point2D modelPos = new Point2D.Float((float) point.getX(), (float) point.getY());
                    final Point2D imagePos = imageToModelTransform.inverseTransform(modelPos, null);

                    if (!sceneRect.contains(imagePos)) {
                        continue;
                    }
                    final float imagePosX = (float) imagePos.getX();
                    final float imagePosY = (float) imagePos.getY();
                    final Rectangle imageRect = sceneRect.intersection(new Rectangle(((int) imagePosX) - boxSize / 2,
                            ((int) imagePosY) - boxSize / 2,
                            boxSize, boxSize));
                    if (imageRect.isEmpty()) {
                        continue;
                    }
                    final double[] rasterValues = new double[imageRect.width * imageRect.height];
                    raster.readPixels(imageRect.x, imageRect.y, imageRect.width, imageRect.height, rasterValues);

                    final int[] maskBuffer = new int[imageRect.width * imageRect.height];
                    Arrays.fill(maskBuffer, 1);
                    if (selectedMask != null) {
                        selectedMask.readPixels(imageRect.x, imageRect.y, imageRect.width, imageRect.height,
                                maskBuffer);
                    }

                    final int centerIndex = imageRect.width * (imageRect.height / 2) + (imageRect.width / 2);
                    if (maskBuffer[centerIndex] == 0) {
                        continue;
                    }

                    double sum = 0;
                    double sumSqr = 0;
                    int n = 0;
                    boolean valid = false;

                    for (int y = 0; y < imageRect.height; y++) {
                        for (int x = 0; x < imageRect.width; x++) {
                            final int index = y * imageRect.height + x;
                            if (raster.isPixelValid(x + imageRect.x, y + imageRect.y) && maskBuffer[index] != 0) {
                                final double rasterValue = rasterValues[index];
                                sum += rasterValue;
                                sumSqr += rasterValue * rasterValue;
                                n++;
                                valid = true;
                            }
                        }
                    }

                    if (!valid) {
                        continue;
                    }

                    double rasterMean = sum / n;
                    double rasterSigma = n > 1 ? Math.sqrt((sumSqr - (sum * sum) / n) / (n - 1)) : 0.0;

                    String localName = dataField.getLocalName();
                    Number attribute = (Number) feature.getAttribute(localName);

                    final Collection<org.opengis.feature.Property> featureProperties = feature.getProperties();

                    final float correlativeData = attribute.floatValue();
                    final GeoPos geoPos = new GeoPos();
                    if (geoCoding.canGetGeoPos()) {
                        final PixelPos pixelPos = new PixelPos(imagePosX, imagePosY);
                        geoCoding.getGeoPos(pixelPos, geoPos);
                    } else {
                        geoPos.setInvalid();
                    }
                    computedDataList.add(
                            new ComputedData(imagePosX, imagePosY, (float) geoPos.getLat(), (float) geoPos.getLon(), (float) rasterMean,
                                    (float) rasterSigma, correlativeData, featureProperties));
                }

                return computedDataList.toArray(new ComputedData[computedDataList.size()]);
            }

            @Override
            public void done() {
                try {
                    final ValueAxis xAxis = getPlot().getDomainAxis();
                    final ValueAxis yAxis = getPlot().getRangeAxis();

                    xAxis.setAutoRange(false);
                    yAxis.setAutoRange(false);

                    scatterpointsDataset.removeAllSeries();
                    acceptableDeviationDataset.removeAllSeries();
                    regressionDataset.removeAllSeries();
                    getPlot().removeAnnotation(r2Annotation);
                    computedDatas = null;

                    final ComputedData[] data = get();
                    if (data.length == 0) {
                        return;
                    }

                    computedDatas = data;

                    final XYIntervalSeries scatterValues = new XYIntervalSeries(getCorrelativeDataName());
                    for (ComputedData computedData : computedDatas) {
                        final float rasterMean = computedData.rasterMean;
                        final float rasterSigma = computedData.rasterSigma;
                        final float correlativeData = computedData.correlativeData;
                        scatterValues.add(correlativeData, correlativeData, correlativeData,
                                rasterMean, rasterMean - rasterSigma, rasterMean + rasterSigma);
                    }

                    computingData = true;
                    scatterpointsDataset.addSeries(scatterValues);

                    xAxis.setAutoRange(true);
                    yAxis.setAutoRange(true);

                    xAxis.setAutoRange(false);
                    yAxis.setAutoRange(false);

                    xAutoRangeAxisRange = new Range(xAxis.getLowerBound(), xAxis.getUpperBound());
                    yAutoRangeAxisRange = new Range(yAxis.getLowerBound(), yAxis.getUpperBound());

                    if (xAxisRangeControl.isAutoMinMax()) {
                        xAxisRangeControl.adjustComponents(xAxis, 3);
                    } else {
                        xAxisRangeControl.adjustAxis(xAxis, 3);
                    }
                    if (yAxisRangeControl.isAutoMinMax()) {
                        yAxisRangeControl.adjustComponents(yAxis, 3);
                    } else {
                        yAxisRangeControl.adjustAxis(yAxis, 3);
                    }

                    computeRegressionAndAcceptableDeviationData();
                    computingData = false;
                } catch (InterruptedException | CancellationException e) {
                    SystemUtils.LOG.log(Level.WARNING, "Failed to compute correlative plot.", e);
                    Dialogs.showMessage(CHART_TITLE,
                            "Failed to compute correlative plot.\n" +
                                    "Calculation canceled.",
                            JOptionPane.ERROR_MESSAGE, null);
                } catch (ExecutionException e) {
                    SystemUtils.LOG.log(Level.WARNING, "Failed to compute correlative plot.", e);
                    Dialogs.showMessage(CHART_TITLE,
                            "Failed to compute correlative plot.\n" +
                                    "An error occurred:\n" + e.getCause().getMessage(),
                            JOptionPane.ERROR_MESSAGE, null);
                }
            }
        };
        swingWorker.execute();
    }

    private void computeRegressionAndAcceptableDeviationData() {
        acceptableDeviationDataset.removeAllSeries();
        regressionDataset.removeAllSeries();
        getPlot().removeAnnotation(r2Annotation);
        if (computedDatas != null) {
            final ValueAxis domainAxis = getPlot().getDomainAxis();
            final double min = domainAxis.getLowerBound();
            final double max = domainAxis.getUpperBound();
            acceptableDeviationDataset.addSeries(computeAcceptableDeviationData(min, max));
            if (scatterPlotModel.showRegressionLine) {
                final XYIntervalSeries series = computeRegressionData(min, max);
                if (series != null) {
                    regressionDataset.addSeries(series);
                    computeCoefficientOfDetermination();
                }
            }
        }
    }

    private XYIntervalSeries computeRegressionData(double xStart, double xEnd) {
        if (scatterpointsDataset.getItemCount(0) > 1) {
            final double[] coefficients = Regression.getOLSRegression(scatterpointsDataset, 0);
            final Function2D curve = new LineFunction2D(coefficients[0], coefficients[1]);
            final XYSeries regressionData = DatasetUtils.sampleFunction2DToSeries(curve, xStart, xEnd, 100, "regression line");
            final XYIntervalSeries xyIntervalRegression = new XYIntervalSeries(regressionData.getKey());
            for (int i = 0; i < regressionData.getItemCount(); i++) {
                XYDataItem item = regressionData.getDataItem(i);
                final double x = item.getXValue();
                final double y = item.getYValue();
                xyIntervalRegression.add(x, x, x, y, y, y);
            }
            return xyIntervalRegression;
        } else {
            Dialogs.showInformation("Unable to compute regression line.\n" +
                    "At least 2 values are needed to compute regression coefficients.");
            return null;
        }
    }

    private void computeCoefficientOfDetermination() {
        int numberOfItems = scatterpointsDataset.getSeries(0).getItemCount();
        double arithmeticMeanOfX = 0;  //arithmetic mean of X
        double arithmeticMeanOfY = 0;  //arithmetic mean of Y
        double varX = 0;    //variance of X
        double varY = 0;    //variance of Y
        double coVarXY = 0;  //covariance of X and Y;
        //compute arithmetic means
        for (int i = 0; i < numberOfItems; i++) {
            arithmeticMeanOfX += scatterpointsDataset.getXValue(0, i);
            arithmeticMeanOfY += scatterpointsDataset.getYValue(0, i);
        }
        arithmeticMeanOfX /= numberOfItems;
        arithmeticMeanOfY /= numberOfItems;
        //compute variances and covariance
        for (int i = 0; i < numberOfItems; i++) {
            varX += Math.pow(scatterpointsDataset.getXValue(0, i) - arithmeticMeanOfX, 2);
            varY += Math.pow(scatterpointsDataset.getYValue(0, i) - arithmeticMeanOfY, 2);
            coVarXY += (scatterpointsDataset.getXValue(0, i) - arithmeticMeanOfX) * (scatterpointsDataset.getYValue(0,
                    i) -
                    arithmeticMeanOfY);
        }
        //computation of coefficient of determination
        double r2 = Math.pow(coVarXY, 2) / (varX * varY);
        r2 = MathUtils.round(r2, Math.pow(10.0, 5));

        final double[] coefficients = Regression.getOLSRegression(scatterpointsDataset, 0);
        final double intercept = coefficients[0];
        final double slope = coefficients[1];
        final String linearEquation;
        if (intercept >= 0) {
            linearEquation = "y = " + (float) slope + "x + " + (float) intercept;
        } else {
            linearEquation = "y = " + (float) slope + "x - " + Math.abs((float) intercept);
        }

        TextTitle tt = new TextTitle(linearEquation + "\nR² = " + r2);
        tt.setTextAlignment(HorizontalAlignment.RIGHT);
        tt.setFont(chart.getLegend().getItemFont());
        tt.setBackgroundPaint(new Color(200, 200, 255, 100));
        tt.setFrame(new BlockBorder(Color.white));
        tt.setPosition(RectangleEdge.BOTTOM);

        r2Annotation = new XYTitleAnnotation(0.98, 0.02, tt,
                RectangleAnchor.BOTTOM_RIGHT);
        r2Annotation.setMaxWidth(0.48);
        getPlot().addAnnotation(r2Annotation);
    }

    private XYIntervalSeries computeAcceptableDeviationData(double lowerBound, double upperBound) {
        final XYSeries identity = DatasetUtils.sampleFunction2DToSeries(x -> x, lowerBound, upperBound, 100, "1:1 line");
        final XYIntervalSeries xyIntervalSeries = new XYIntervalSeries(identity.getKey());
        for (int i = 0; i < identity.getItemCount(); i++) {
            XYDataItem item = identity.getDataItem(i);
            final double x = item.getXValue();
            final double y = item.getYValue();
            if (scatterPlotModel.showAcceptableDeviation) {
                final double acceptableDeviation = scatterPlotModel.acceptableDeviationInterval;
                final double xOff = acceptableDeviation * x / 100;
                final double yOff = acceptableDeviation * y / 100;
                xyIntervalSeries.add(x, x - xOff, x + xOff, y, y - yOff, y + yOff);
            } else {
                xyIntervalSeries.add(x, x, x, y, y, y);
            }
        }
        return xyIntervalSeries;
    }

    private UserSettings getUserSettings(Product product) {
        if (product == null) {
            return null;
        }
        if (userSettingsMap.get(product) == null) {
            userSettingsMap.put(product, new UserSettings());
        }
        return userSettingsMap.get(product);
    }

    // The fields of this class are used by the binding framework
    @SuppressWarnings({"UnusedDeclaration", "FieldMayBeFinal"})
    static class ScatterPlotModel {

        public boolean showRegressionLine;
        private int boxSize = 1;
        private boolean useRoiMask;
        private Mask roiMask;
        private VectorDataNode pointDataSource;
        private AttributeDescriptor dataField;
        private boolean xAxisLogScaled;
        private boolean yAxisLogScaled;
        private boolean showAcceptableDeviation;
        private double acceptableDeviationInterval = 15;
    }

    static class ComputedData {

        final float x;
        final float y;
        final float lat;
        final float lon;
        final float rasterMean;
        final float rasterSigma;
        final float correlativeData;
        final Collection<org.opengis.feature.Property> featureProperties;

        ComputedData(float x, float y, float lat, float lon, float rasterMean, float rasterSigma, float correlativeData,
                     Collection<org.opengis.feature.Property> featureProperties) {
            this.x = x;
            this.y = y;
            this.lat = lat;
            this.lon = lon;
            this.rasterMean = rasterMean;
            this.rasterSigma = rasterSigma;
            this.correlativeData = correlativeData;
            this.featureProperties = featureProperties;
        }
    }

    private static class UserSettings {

        Map<String, VectorDataNode> pointDataSource = new HashMap<>();
        Map<String, AttributeDescriptor> dataField = new HashMap<>();

        public void set(String rasterName, VectorDataNode pointDataSourceValue, AttributeDescriptor dataFieldValue) {
            if (pointDataSourceValue != null && dataFieldValue != null) {
                pointDataSource.put(rasterName, pointDataSourceValue);
                dataField.put(rasterName, dataFieldValue);
            }
        }

        public VectorDataNode getPointDataSource(String rasterName) {
            return pointDataSource.get(rasterName);
        }

        public AttributeDescriptor getDataField(String rasterName) {
            return dataField.get(rasterName);
        }

        public void dispose() {
            pointDataSource.clear();
            pointDataSource = null;
            dataField.clear();
            dataField = null;
        }
    }
}

