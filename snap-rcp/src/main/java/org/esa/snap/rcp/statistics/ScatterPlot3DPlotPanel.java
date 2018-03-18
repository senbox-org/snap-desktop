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
import com.vividsolutions.jts.geom.Point;
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
import org.jfree.data.Range;
import org.jfree.data.function.Function2D;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
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
class ScatterPlot3DPlotPanel extends PagePanel {

    public static final String CHART_TITLE = "3D Scatter Plot";
    private static final String NO_DATA_MESSAGE = "No 3D scatter plot computed yet.";
//            "To create a 3D scatter plot\n" +
//            "   -Select a band" + "\n" +
//            "   -Select vector data (e.g., a SeaDAS 6.x track)" + "\n" +
//            "   -Select the data as point data source" + "\n" +
//            "   -Select a data field" + "\n" +
//            HELP_TIP_MESSAGE + "\n" +
//            ZOOM_TIP_MESSAGE;

    private final String PROPERTY_NAME_X_AXIS_LOG_SCALED = "xAxisLogScaled";
    private final String PROPERTY_NAME_Y_AXIS_LOG_SCALED = "yAxisLogScaled";
    private final String PROPERTY_NAME_DATA_FIELD = "dataField";
    private final String PROPERTY_NAME_POINT_DATA_SOURCE = "pointDataSource";
    private final String PROPERTY_NAME_BOX_SIZE = "boxSize";
    private final String PROPERTY_NAME_SHOW_ACCEPTABLE_DEVIATION = "showAcceptableDeviation";
    private final String PROPERTY_NAME_ACCEPTABLE_DEVIATION = "acceptableDeviationInterval";
    private final String PROPERTY_NAME_SHOW_REGRESSION_LINE = "showRegressionLine";

//    private final ScatterPlotModel scatterPlotModel;
//    private final BindingContext bindingContext;
    private final AxisRangeControl xAxisRangeControl;
    private final AxisRangeControl yAxisRangeControl;
    private final AxisRangeControl zAxisRangeControl;
//    private ComputedData[] computedDatas;

    private final ProductManager.Listener productRemovedListener;
//    private final Map<Product, UserSettings> userSettingsMap;

    ScatterPlot3DPlotPanel(TopComponent parentDialog, String helpId) {
        super(parentDialog, helpId, CHART_TITLE);
//        userSettingsMap = new HashMap<>();
        productRemovedListener = new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
//                final UserSettings userSettings = userSettingsMap.remove(event.getProduct());
//                if (userSettings != null) {
//                    userSettings.dispose();
//                }
            }
        };

        xAxisRangeControl = new AxisRangeControl("X-Axis");
        yAxisRangeControl = new AxisRangeControl("Y-Axis");
        zAxisRangeControl = new AxisRangeControl("Z-Axis");
//        scatterPlotModel = new ScatterPlotModel();
//        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(scatterPlotModel));
//        final PropertyChangeListener userSettingsUpdateListener = evt -> {
//            if (getRaster() != null) {
//                final VectorDataNode pointDataSourceValue = scatterPlotModel.pointDataSource;
//                final AttributeDescriptor dataFieldValue = scatterPlotModel.dataField;
//                final UserSettings userSettings = getUserSettings(getRaster().getProduct());
//                userSettings.set(getRaster().getName(), pointDataSourceValue, dataFieldValue);
//            }
//        };

//        bindingContext.addPropertyChangeListener(PROPERTY_NAME_DATA_FIELD, userSettingsUpdateListener);
//        bindingContext.addPropertyChangeListener(PROPERTY_NAME_POINT_DATA_SOURCE, userSettingsUpdateListener);
    }

    @Override
    protected void initComponents() {

    }

    @Override
    protected void updateComponents() {

    }

    @Override
    protected String getDataAsText() {
        return null;
    }
}

