/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.timeseries.ui.matrix;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.timeseries.core.TimeSeriesMapper;
import org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.snap.timeseries.core.timeseries.datamodel.TimeCoding;
import org.esa.snap.timeseries.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Main class for the matrix tool.
 *
 * @author Marco Peters
 * @author Thomas Storm
 */
@TopComponent.Description(
        preferredID = "TimeSeriesMatrixTopComponent",
        iconBase = "org/esa/snap/timeseries/ui/icons/timeseries-matrix.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 4
)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_TimeSeriesMatrixTopComponentName",
        preferredID = "TimeSeriesMatrixTopComponent"
)
@ActionID(category = "Window", id = "org.esa.snap.timeseries.ui.matrix.TimeSeriesMatrixTopComponent")
//@ActionReferences({
//        @ActionReference(path = "Menu/Raster/Time Series", position = 1240, separatorAfter = 1250),
//        @ActionReference(path = "Toolbars/Time Series", position = 40)
//})
@NbBundle.Messages({ "CTL_TimeSeriesMatrixTopComponentName=Time Series Matrix" })
public class TimeSeriesMatrixTopComponent extends TopComponent {

    private static final String HELP_ID = "timeSeriesMatrix";

    private static final int MATRIX_MINIMUM = 3;
    private static final int MATRIX_DEFAULT_VALUE = MATRIX_MINIMUM;
    private static final int MATRIX_MAXIMUM = 15;
    private static final int MATRIX_STEP_SIZE = 2;

    private JSpinner matrixSizeSpinner;
    private JLabel dateLabel;
    private ProductSceneView currentView;
    private AbstractTimeSeries timeSeries;
    private final SceneViewHandler sceneViewListener;
    private final TimeSeriesPPL pixelPosListener;
    private final MatrixMouseWheelListener mouseWheelListener;
    private final TimeSeriesListener timeSeriesMatrixTSL;

    private static final String DATE_PREFIX = "Date: ";
    private MatrixTableModel matrixModel;
    private final DateFormat dateFormat;
    private MatrixCellRenderer matrixCellRenderer;

    public TimeSeriesMatrixTopComponent() {
        pixelPosListener = new TimeSeriesPPL();
        sceneViewListener = new SceneViewHandler();
        mouseWheelListener = new MatrixMouseWheelListener();
        timeSeriesMatrixTSL = new TimeSeriesMatrixTSL();
        dateFormat = ProductData.UTC.createDateFormat("dd-MMM-yyyy HH:mm:ss");
        initUI();
    }

    private void initUI() {
        SnapApp.getDefault().getSelectionSupport(ProductSceneView.class).addHandler(sceneViewListener);

        dateLabel = new JLabel(String.format(DATE_PREFIX + " %s", getStartDateString()));
        matrixSizeSpinner = new JSpinner(new SpinnerNumberModel(MATRIX_DEFAULT_VALUE,
                MATRIX_MINIMUM, MATRIX_MAXIMUM,
                MATRIX_STEP_SIZE));
        final JComponent editor = matrixSizeSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setEditable(false);
        }
        matrixSizeSpinner.addChangeListener(e -> matrixModel.setMatrixSize((Integer) matrixSizeSpinner.getModel().getValue()));

        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setRowWeightY(1, 1.0);
        tableLayout.setCellColspan(0, 0, 2);

        setLayout(tableLayout);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel buttonPanel = createButtonPanel();
        JPanel tablePanel = createTablePanel();
        add(dateLabel);
        add(tablePanel);
        add(buttonPanel);

        setCurrentView(SnapApp.getDefault().getSelectedProductSceneView());
        setDisplayName(Bundle.CTL_TimeSeriesMatrixTopComponentName());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    @Override
    protected void componentShowing() {
        addMouseWheelListener();
    }

    @Override
    public void componentOpened() {
        addMouseWheelListener();
    }

    @Override
    public void componentClosed() {
        removeMouseWheelListener();
    }

    @Override
    public void componentHidden() {
        removeMouseWheelListener();
    }

    private JPanel createTablePanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
        matrixModel = new MatrixTableModel();
        JTable matrixTable = new JTable(matrixModel);
        matrixCellRenderer = new MatrixCellRenderer(matrixModel);
        matrixTable.setDefaultRenderer(Double.class, matrixCellRenderer);
        matrixTable.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        mainPanel.add(BorderLayout.CENTER, matrixTable);
        return mainPanel;
    }

    private String getStartDateString() {
        String startDateString = "";
        if (currentView != null && timeSeries != null) {
            final TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(currentView.getRaster());
            Date startDate = timeCoding.getStartTime().getAsDate();
            startDateString = dateFormat.format(startDate);
        }
        return startDateString;
    }

    private JPanel createButtonPanel() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setRowPadding(0, new Insets(0, 4, 4, 4));
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        JPanel buttonPanel = new JPanel(tableLayout);

        AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help22.png"), false);
        helpButton.addActionListener(e -> new HelpCtx(HELP_ID).display());
        helpButton.setToolTipText("Help");
        buttonPanel.add(matrixSizeSpinner);
        buttonPanel.add(tableLayout.createVerticalSpacer());
        buttonPanel.add(helpButton);

        return buttonPanel;
    }

    /*
     * Checks if the view displays a timeseries product.
     * If so it is set as the current view.
     */
    private void setCurrentView(ProductSceneView newView) {
        if (currentView == newView) {
            return;
        }
        if (currentView != null) {
            currentView.removePixelPositionListener(pixelPosListener);
            removeMouseWheelListener();
            if (timeSeries != null) {
                timeSeries.removeTimeSeriesListener(timeSeriesMatrixTSL);
            }
        }
        currentView = newView;
        if (isTimeSeriesView(currentView)) {
            currentView.addPixelPositionListener(pixelPosListener);
            timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
            timeSeries.addTimeSeriesListener(timeSeriesMatrixTSL);
            addMouseWheelListener();
            final RasterDataNode raster = currentView.getRaster();
            if (raster instanceof Band) {
                matrixModel.setBand((Band) raster);
                matrixModel.setMatrixSize((Integer) matrixSizeSpinner.getValue());
                matrixCellRenderer.setInvalidColor(currentView.getLayerCanvas().getBackground());
                updateDateLabel((Band) currentView.getRaster());
            }
        } else {
            timeSeries = null;
            matrixModel.setMatrixSize(0);
        }
    }

    private void updateDateLabel(Band band) {
        String dateString = "";
        if (band != null) {
            final TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(band);
            final Date startTime = timeCoding.getStartTime().getAsDate();
            dateString = dateFormat.format(startTime);
        }
        dateLabel.setText(String.format(DATE_PREFIX + " %s", dateString));
    }

    // Depending on the direction value this method returns the next
    // band in the list of available bands in the time series.
    // Negative value of direction means previous band.
    // If there is no next band the current band is returned.

    private Band getNextBand(Band currentBand, int direction) {
        final String varName = AbstractTimeSeries.rasterToVariableName(currentBand.getName());
        final List<Band> bandList = timeSeries.getBandsForVariable(varName);
        final int currentIndex = bandList.indexOf(currentBand);

        if (direction < 0) {
            if (currentIndex > 0) {
                return bandList.get(currentIndex - 1);
            }
        } else {
            if (currentIndex + 1 < bandList.size()) {
                return bandList.get(currentIndex + 1);
            }
        }
        return currentBand;
    }

    private boolean isTimeSeriesView(ProductSceneView view) {
        if (view != null) {
            final RasterDataNode viewRaster = view.getRaster();
            final String viewProductType = viewRaster.getProduct().getProductType();
            return !view.isRGB() &&
                    viewProductType.equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE) &&
                    TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct()) != null;
        }
        return false;
    }


    private void addMouseWheelListener() {
        if (currentView != null) {
            final LayerCanvas layerCanvas = currentView.getLayerCanvas();
            final List<MouseWheelListener> listeners = Arrays.asList(layerCanvas.getMouseWheelListeners());
            if (!listeners.contains(mouseWheelListener)) {
                layerCanvas.addMouseWheelListener(mouseWheelListener);
            }
        }
    }

    private void removeMouseWheelListener() {
        if (currentView != null) {
            currentView.getLayerCanvas().removeMouseWheelListener(mouseWheelListener);
        }
    }

    private class SceneViewHandler implements SelectionSupport.Handler<ProductSceneView> {

        @Override
        public void selectionChange(ProductSceneView oldValue, ProductSceneView newValue) {
            if (currentView == oldValue) {
                setCurrentView(null);
            }
            setCurrentView(newValue);
        }
    }

    private class TimeSeriesPPL implements PixelPositionListener {

        @Override
        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY,
                                    int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (isVisible() && currentView != null) {
                AffineTransform i2mTransform = imageLayer.getImageToModelTransform(currentLevel);
                Point2D modelP = i2mTransform.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), null);
                AffineTransform m2iTransform = imageLayer.getModelToImageTransform();
                Point2D levelZeroP = m2iTransform.transform(modelP, null);
                matrixModel.setCenterPixel(MathUtils.floorInt(levelZeroP.getX()),
                        MathUtils.floorInt(levelZeroP.getY()));
            }
        }

        @Override
        public void pixelPosNotAvailable() {
            matrixModel.clearMatrix();
        }
    }

    private class MatrixMouseWheelListener implements MouseWheelListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isAltDown()) {
                Band nextBand = getNextBand(matrixModel.getBand(), e.getWheelRotation());
                if (nextBand != null) {
                    matrixModel.setBand(nextBand);
                    updateDateLabel(nextBand);
                }
            }
        }
    }

    private class TimeSeriesMatrixTSL extends TimeSeriesListener {

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            final ProductNode node = event.getSourceNode();
            if (node == matrixModel.getBand()) {
                final Band band = matrixModel.getBand();
                Band nextBand = getNextBand(band, 1);
                if (nextBand == band) {
                    nextBand = getNextBand(band, -1);
                }
                if (nextBand == band) {
                    nextBand = null;
                }
                updateDateLabel(nextBand);
            }
        }
    }

}
