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
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.StxFactory;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.GridBagUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.RectangleInsets;
import org.openide.windows.TopComponent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Shape;
import java.awt.event.ItemEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;


/**
 * The density plot pane within the statistics window.
 */
class DensityPlotPanel extends ChartPagePanel {

    private static final String NO_DATA_MESSAGE = "No scatter plot computed yet.\n" +
            "To create a scatter plot, select bands in both combo boxes.\n" +
            "The plot will be computed when you click the 'Refresh View' button.\n" +
            HELP_TIP_MESSAGE + "\n" +
            ZOOM_TIP_MESSAGE;
    public static final String CHART_TITLE = "Scatter Plot";

    private final static String PROPERTY_NAME_AUTO_MIN_MAX = "autoMinMax";
    private final static String PROPERTY_NAME_MIN = "min";
    private final static String PROPERTY_NAME_MAX = "max";
    private final static String PROPERTY_NAME_USE_ROI_MASK = "useRoiMask";
    private final static String PROPERTY_NAME_ROI_MASK = "roiMask";
    private final static String PROPERTY_NAME_X_PRODUCT = "xProduct";
    private final static String PROPERTY_NAME_Y_PRODUCT = "yProduct";
    private final static String PROPERTY_NAME_X_BAND = "xBand";
    private final static String PROPERTY_NAME_Y_BAND = "yBand";

    private static final int X_VAR = 0;
    private static final int Y_VAR = 1;

    private static final int NUM_DECIMALS = 2;

    private BindingContext bindingContext;
    private DataSourceConfig dataSourceConfig;
    private Property xProductProperty;
    private Property yProductProperty;
    private Property xBandProperty;
    private Property yBandProperty;
    private JComboBox<ListCellRenderer> xProductList;
    private JComboBox<ListCellRenderer> yProductList;
    private JComboBox<ListCellRenderer> xBandList;
    private JComboBox<ListCellRenderer> yBandList;
    //todo instead using referenceSize, use referenceSceneRasterTransform
    private Dimension referenceSize;

    private static AxisRangeControl[] axisRangeControls = new AxisRangeControl[2];
    private IndexColorModel toggledColorModel;
    private IndexColorModel untoggledColorModel;

    private ChartPanel densityPlotDisplay;
    private XYImagePlot plot;
    private static final Color backgroundColor = new Color(255, 255, 255, 0);
    private boolean plotColorsInverted;
    private JCheckBox toggleColorCheckBox;

    DensityPlotPanel(TopComponent parentComponent, String helpId) {
        super(parentComponent, helpId, CHART_TITLE, true);
    }

    @Override
    protected void initComponents() {
        initParameters();
        createUI();
        initActionEnablers();
    }

    private void initActionEnablers() {
        RefreshActionEnabler roiMaskActionEnabler = new RefreshActionEnabler(refreshButton, PROPERTY_NAME_USE_ROI_MASK,
                                                                             PROPERTY_NAME_ROI_MASK);
        roiMaskActionEnabler.addProductBandEnablement(PROPERTY_NAME_X_PRODUCT, PROPERTY_NAME_X_BAND);
        roiMaskActionEnabler.addProductBandEnablement(PROPERTY_NAME_Y_PRODUCT, PROPERTY_NAME_Y_BAND);
        bindingContext.addPropertyChangeListener(roiMaskActionEnabler);
        RefreshActionEnabler rangeControlActionEnabler = new RefreshActionEnabler(refreshButton, PROPERTY_NAME_MIN, PROPERTY_NAME_AUTO_MIN_MAX,
                                                                                  PROPERTY_NAME_MAX);
        axisRangeControls[X_VAR].getBindingContext().addPropertyChangeListener(rangeControlActionEnabler);
        axisRangeControls[Y_VAR].getBindingContext().addPropertyChangeListener(rangeControlActionEnabler);
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
        super.nodeDataChanged(event);
        if (!dataSourceConfig.useRoiMask) {
            return;
        }
        final Mask roiMask = dataSourceConfig.roiMask;
        if (roiMask == null) {
            return;
        }
        final ProductNode sourceNode = event.getSourceNode();
        if (!(sourceNode instanceof Mask)) {
            return;
        }
        final String maskName = sourceNode.getName();
        if (roiMask.getName().equals(maskName)) {
            updateComponents();
        }
    }

    @Override
    protected void updateComponents() {
        super.updateComponents();
        if (isRasterChanged() || isProductChanged()) {
            plot.setImage(null);
            plot.setDataset(null);
            if (isProductChanged()) {
                plot.getDomainAxis().setLabel("X");
                plot.getRangeAxis().setLabel("Y");
            }
            final ValueSet productValueSet = new ValueSet(createAvailableProductList());
            xProductProperty.getDescriptor().setValueSet(productValueSet);
            yProductProperty.getDescriptor().setValueSet(productValueSet);

            if (productValueSet.getItems().length > 0) {
                Product currentProduct = getProduct();
                try {
                    xProductProperty.setValue(currentProduct);
                    yProductProperty.setValue(currentProduct);
                } catch (ValidationException ignored) {
                    Debug.trace(ignored);
                }
            }

            updateBandList(getProduct(), xBandProperty, false);
            updateBandList(getProduct(), yBandProperty, true);

            toggleColorCheckBox.setEnabled(false);
        }
        refreshButton.setEnabled(xBandProperty.getValue() != null && yBandProperty.getValue() != null);
    }

    private void updateBandList(final Product product, final Property bandProperty, boolean considerReferenceSize) {
        if (product == null) {
            return;
        }

        final ValueSet bandValueSet = new ValueSet(createAvailableBandList(product, considerReferenceSize));
        bandProperty.getDescriptor().setValueSet(bandValueSet);
        if (bandValueSet.getItems().length > 0) {
            RasterDataNode currentRaster = getRaster();
            if (bandValueSet.contains(getRaster())) {
                currentRaster = getRaster();
            }
            try {
                bandProperty.setValue(currentRaster);
            } catch (ValidationException ignored) {
                Debug.trace(ignored);
            }
        }
    }

    private void initParameters() {
        axisRangeControls[X_VAR] = new AxisRangeControl("X-Axis");
        axisRangeControls[Y_VAR] = new AxisRangeControl("Y-Axis");
        initColorModels();
        plotColorsInverted = false;
        dataSourceConfig = new DataSourceConfig();
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(dataSourceConfig));

        xProductList = new JComboBox<>();
        xProductList.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                final Product selectedXProduct = (Product) event.getItem();
                updateBandList(selectedXProduct, xBandProperty, false);
            }
        });
        xProductList.setRenderer(new ProductListCellRenderer());
        bindingContext.bind(PROPERTY_NAME_X_PRODUCT, xProductList);
        xProductProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_X_PRODUCT);

        yProductList = new JComboBox<>();
        yProductList.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                final Product selectedYProduct = (Product) event.getItem();
                updateBandList(selectedYProduct, yBandProperty, true);
            }
        });
        yProductList.setRenderer(new ProductListCellRenderer());
        bindingContext.bind(PROPERTY_NAME_Y_PRODUCT, yProductList);
        yProductProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_Y_PRODUCT);

        xBandList = new JComboBox<>();
        xBandList.setRenderer(new BandListCellRenderer());
        bindingContext.bind(PROPERTY_NAME_X_BAND, xBandList);
        xBandList.addActionListener(e -> {
            final Object value = xBandList.getSelectedItem();
            if (value != null) {
                final Dimension rasterSize = ((RasterDataNode) value).getRasterSize();
                if (rasterSize != referenceSize) {
                    referenceSize = rasterSize;
                    updateBandList(getProduct(), yBandProperty, true);
                }
            }
        });
        xBandProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_X_BAND);

        yBandList = new JComboBox<>();
        yBandList.setRenderer(new BandListCellRenderer());
        bindingContext.bind(PROPERTY_NAME_Y_BAND, yBandList);
        yBandProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_Y_BAND);
    }

    private static String formatProductName(final Product product) {
        String name = product.getName().substring(0, Math.min(10, product.getName().length()));
        if (product.getName().length() > 10) {
            name += "...";
        }
        return product.getProductRefString() + name;
    }

    private void initColorModels() {
        for (int j = 0; j <= 1; j++) {
            final int palSize = 256;
            final byte[] r = new byte[palSize];
            final byte[] g = new byte[palSize];
            final byte[] b = new byte[palSize];
            final byte[] a = new byte[palSize];
            r[0] = (byte) backgroundColor.getRed();
            g[0] = (byte) backgroundColor.getGreen();
            b[0] = (byte) backgroundColor.getBlue();
            a[0] = (byte) backgroundColor.getAlpha();
            for (int i = 1; i < 128; i++) {
                if (j == 0) {
                    r[i] = (byte) (2 * i);
                    g[i] = (byte) 0;
                } else {
                    r[i] = (byte) 255;
                    g[i] = (byte) (255 - (2 * (i - 128)));
                }
                b[i] = (byte) 0;
                a[i] = (byte) 255;
            }
            for (int i = 128; i < 256; i++) {
                if (j == 0) {
                    r[i] = (byte) 255;
                    g[i] = (byte) (2 * (i - 128));
                } else {
                    r[i] = (byte) (255 - (2 * i));
                    g[i] = (byte) 0;
                }
                b[i] = (byte) 0;
                a[i] = (byte) 255;
            }
            if (j == 0) {
                toggledColorModel = new IndexColorModel(8, palSize, r, g, b, a);
            } else {
                untoggledColorModel = new IndexColorModel(8, palSize, r, g, b, a);
            }

        }
    }

    private void createUI() {
        plot = new XYImagePlot();
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        domainAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setAutoRangeIncludesZero(false);
        domainAxis.setUpperMargin(0);
        domainAxis.setLowerMargin(0);
        rangeAxis.setUpperMargin(0);
        rangeAxis.setLowerMargin(0);
        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.getRenderer().setDefaultToolTipGenerator(new XYPlotToolTipGenerator());
        JFreeChart chart = new JFreeChart(CHART_TITLE, plot);
        ChartFactory.getChartTheme().apply(chart);

        chart.removeLegend();
        createUI(createChartPanel(chart), createOptionsPanel(), bindingContext);
        updateUIState();
    }

    private void toggleColor() {
        BufferedImage image = plot.getImage();
        if (image != null) {
            if (!plotColorsInverted) {
                image = new BufferedImage(untoggledColorModel, image.getRaster(), image.isAlphaPremultiplied(), null);
            } else {
                image = new BufferedImage(toggledColorModel, image.getRaster(), image.isAlphaPremultiplied(), null);
            }
            plot.setImage(image);
            densityPlotDisplay.getChart().setNotify(true);
            plotColorsInverted = !plotColorsInverted;
        }
    }

    private JPanel createOptionsPanel() {
        toggleColorCheckBox = new JCheckBox("Invert plot colors");
        toggleColorCheckBox.addActionListener(e -> toggleColor());
        toggleColorCheckBox.setEnabled(false);
        final JPanel optionsPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=0,weightx=1,gridx=0");
        GridBagUtils.addToPanel(optionsPanel, axisRangeControls[X_VAR].getPanel(), gbc, "gridy=0, insets.top=2");
        GridBagUtils.addToPanel(optionsPanel, xProductList, gbc, "gridy=1,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, xBandList, gbc, "gridy=2,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, axisRangeControls[Y_VAR].getPanel(), gbc, "gridy=3,insets.left=0,insets.right=0");
        GridBagUtils.addToPanel(optionsPanel, yProductList, gbc, "gridy=4,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, yBandList, gbc, "gridy=5,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, new JPanel(), gbc, "gridy=6");
        GridBagUtils.addToPanel(optionsPanel, new JSeparator(), gbc, "gridy=7,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, toggleColorCheckBox, gbc, "gridy=8,insets.left=0,insets.right=0");
        return optionsPanel;
    }

    private ChartPanel createChartPanel(JFreeChart chart) {
        densityPlotDisplay = new ChartPanel(chart);

        MaskSelectionToolSupport maskSelectionToolSupport = new MaskSelectionToolSupport(this,
                                                                                         densityPlotDisplay,
                                                                                         "scatter_plot_area",
                                                                                         "Mask generated from selected scatter plot area",
                                                                                         Color.RED,
                                                                                         PlotAreaSelectionTool.AreaType.ELLIPSE) {
            @Override
            protected String createMaskExpression(PlotAreaSelectionTool.AreaType areaType, Shape shape) {
                Rectangle2D bounds = shape.getBounds2D();
                return createMaskExpression(bounds.getCenterX(), bounds.getCenterY(), 0.5 * bounds.getWidth(), 0.5 * bounds.getHeight());
            }

            protected String createMaskExpression(double x0, double y0, double dx, double dy) {
                return String.format("sqrt(sq((%s - %s)/%s) + sq((%s - %s)/%s)) < 1.0",
                                     BandArithmetic.createExternalName(dataSourceConfig.xBand.getName()),
                                     x0,
                                     dx,
                                     BandArithmetic.createExternalName(dataSourceConfig.yBand.getName()),
                                     y0,
                                     dy);
            }
        };

        densityPlotDisplay.getPopupMenu().addSeparator();
        densityPlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createMaskSelectionModeMenuItem());
        densityPlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createDeleteMaskMenuItem());
        densityPlotDisplay.getPopupMenu().addSeparator();
        densityPlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());
        return densityPlotDisplay;
    }

    private RasterDataNode getRaster(int varIndex) {
        final Product product = getProduct();
        if (product == null) {
            return null;
        }
        final RasterDataNode raster;
        if (varIndex == X_VAR) {
            raster = dataSourceConfig.xBand;
        } else {
            raster = dataSourceConfig.yBand;
        }
        Debug.assertTrue(raster != null);
        return raster;
    }

    private void updateUIState() {
        super.updateComponents();
    }

    private static void checkBandsForRange() throws IllegalArgumentException {
        if (axisRangeControls[X_VAR].getMin().equals(axisRangeControls[X_VAR].getMax()) &&
                axisRangeControls[Y_VAR].getMin().equals(axisRangeControls[Y_VAR].getMax())) {
            throw new IllegalArgumentException("Value range of at least one band must be larger than one");
        }
    }

    @Override
    protected void updateChartData() {

        final RasterDataNode rasterX = getRaster(X_VAR);
        final RasterDataNode rasterY = getRaster(Y_VAR);

        if (rasterX == null || rasterY == null) {
            return;
        }

        ProgressMonitorSwingWorker<BufferedImage, Object> swingWorker = new ProgressMonitorSwingWorker<BufferedImage, Object>(
                this, "Computing scatter plot") {

            @Override
            protected BufferedImage doInBackground(ProgressMonitor pm) throws Exception {
                pm.beginTask("Computing scatter plot...", 100);
                try {
                    checkBandsForRange();
                    setRange(X_VAR, rasterX, dataSourceConfig.useRoiMask ? dataSourceConfig.roiMask : null, SubProgressMonitor.create(pm, 15));
                    setRange(Y_VAR, rasterY, dataSourceConfig.useRoiMask ? dataSourceConfig.roiMask : null, SubProgressMonitor.create(pm, 15));
                    final BufferedImage densityPlotImage = ProductUtils.createDensityPlotImage(rasterX,
                                                                                               axisRangeControls[X_VAR].getMin().floatValue(),
                                                                                               axisRangeControls[X_VAR].getMax().floatValue(),
                                                                                               rasterY,
                                                                                               axisRangeControls[Y_VAR].getMin().floatValue(),
                                                                                               axisRangeControls[Y_VAR].getMax().floatValue(),
                                                                                               dataSourceConfig.useRoiMask ? dataSourceConfig.roiMask : null,
                                                                                               512,
                                                                                               512,
                                                                                               backgroundColor,
                                                                                               null,
                                                                                               SubProgressMonitor.create(pm, 70));
                    toggleColorCheckBox.setSelected(false);
                    plotColorsInverted = false;
                    return densityPlotImage;
                } finally {
                    pm.done();
                }
            }

            @Override
            public void done() {
                try {
                    checkBandsForRange();
                    final BufferedImage densityPlotImage = get();
                    double minX = axisRangeControls[X_VAR].getMin();
                    double maxX = axisRangeControls[X_VAR].getMax();
                    double minY = axisRangeControls[Y_VAR].getMin();
                    double maxY = axisRangeControls[Y_VAR].getMax();
                    if (minX > maxX || minY > maxY) {
                        Dialogs.showMessage(/*I18N*/ CHART_TITLE, /*I18N*/
                                                "Failed to compute scatter plot.\n" +
                                                        "No Pixels considered..",
                                            JOptionPane.ERROR_MESSAGE,
                                            null
                        );
                        plot.setDataset(null);
                        return;

                    }

                    if (MathUtils.equalValues(minX, maxX, 1.0e-4)) {
                        minX = Math.floor(minX);
                        maxX = Math.ceil(maxX);
                    }
                    if (MathUtils.equalValues(minY, maxY, 1.0e-4)) {
                        minY = Math.floor(minY);
                        maxY = Math.ceil(maxY);
                    }
                    plot.setImage(densityPlotImage);
                    plot.setImageDataBounds(new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY));
                    axisRangeControls[X_VAR].adjustComponents(minX, maxX, NUM_DECIMALS);
                    axisRangeControls[Y_VAR].adjustComponents(minY, maxY, NUM_DECIMALS);
                    plot.getDomainAxis().setLabel(StatisticChartStyling.getAxisLabel(getRaster(X_VAR), "X", false));
                    plot.getRangeAxis().setLabel(StatisticChartStyling.getAxisLabel(getRaster(Y_VAR), "Y", false));
                    toggleColorCheckBox.setEnabled(true);
                } catch (InterruptedException | CancellationException | ExecutionException | IllegalArgumentException e) {
                    String msg = "Failed to compute scatter plot";
                    String reason;
                    int messageType = JOptionPane.ERROR_MESSAGE;
                    Level logLevel = Level.SEVERE;
                    if (e instanceof CancellationException) {
                        messageType = JOptionPane.INFORMATION_MESSAGE;
                        logLevel = Level.INFO;
                        reason = "Calculation canceled by user";
                    } else if (e instanceof InterruptedException) {
                        reason = "Calculation unexpectedly interrupted";
                    } else {
                        reason = "An unexpected error occurred: " + e.getCause().getMessage();
                    }

                    SystemUtils.LOG.log(logLevel, String.format("%s: %s", msg, reason), e);
                    Dialogs.showMessage(CHART_TITLE, String.format("%s:\n%s", msg, reason), messageType, null);
                }
            }
        };
        swingWorker.execute();
    }

    private static void setRange(int varIndex, RasterDataNode raster, Mask mask, ProgressMonitor pm) throws IOException {
        final AxisRangeControl axisRangeControl = axisRangeControls[varIndex];
        if (axisRangeControl.isAutoMinMax()) {
            Stx stx;
            if (mask == null) {
                stx = raster.getStx(false, pm);
            } else {
                stx = new StxFactory().withRoiMask(mask).create(raster, pm);
            }
            axisRangeControl.adjustComponents(stx.getMinimum(), stx.getMaximum(), NUM_DECIMALS);
        }
    }

    private static Product[] createAvailableProductList() {
        return SnapApp.getDefault().getProductManager().getProducts();
    }

    private RasterDataNode[] createAvailableBandList(final Product product, boolean considerReferenceSize) {
        final List<RasterDataNode> availableBandList = new ArrayList<>(17);
        if (product != null) {
            for (int i = 0; i < product.getNumBands(); i++) {
                final Band band = product.getBandAt(i);
                if (!considerReferenceSize || band.getRasterSize().equals(referenceSize)) {
                    availableBandList.add(band);
                }
            }
            if (!considerReferenceSize || product.getSceneRasterSize().equals(referenceSize)) {
                for (int i = 0; i < product.getNumTiePointGrids(); i++) {
                    availableBandList.add(product.getTiePointGridAt(i));
                }
            }
        }
        // if raster is only bound to the product and does not belong to it
        final RasterDataNode raster = getRaster();
        if (raster != null && raster.getProduct() == product &&
                (!considerReferenceSize || raster.getRasterSize().equals(raster.getProduct().getSceneRasterSize()))) {
            if (!availableBandList.contains(raster)) {
                availableBandList.add(raster);
            }
        }
        return availableBandList.toArray(new RasterDataNode[availableBandList.size()]);
    }

    @Override
    protected boolean checkDataToClipboardCopy() {
        final int warnLimit = 2000;
        final int excelLimit = 65536;
        final int numNonEmptyBins = getNumNonEmptyBins();
        if (numNonEmptyBins > warnLimit) {
            String excelNote = "";
            if (numNonEmptyBins > excelLimit - 100) {
                excelNote = "Note that e.g., Microsoft® Excel 2002 only supports a total of "
                        + excelLimit + " rows in a sheet.\n";   /*I18N*/
            }
            final String message = MessageFormat.format(
                    "This scatter plot contains {0} non-empty bins.\n" +
                            "For each bin, a text data row containing an x, y and z value will be created.\n" +
                            "{1}\nPress ''Yes'' if you really want to copy this amount of data to the system clipboard.\n",
                    numNonEmptyBins, excelNote);
            final int status = JOptionPane.showConfirmDialog(this,
                                                             message, /*I18N*/
                                                             "Copy Data to Clipboard", /*I18N*/
                                                             JOptionPane.YES_NO_OPTION,
                                                             JOptionPane.WARNING_MESSAGE);
            if (status != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        return true;
    }

    private static byte[] getValidData(BufferedImage image) {
        if (image != null &&
                image.getColorModel() instanceof IndexColorModel &&
                image.getData().getDataBuffer() instanceof DataBufferByte) {
            return ((DataBufferByte) image.getData().getDataBuffer()).getData();
        }
        return null;
    }

    protected int getNumNonEmptyBins() {
        final byte[] data = getValidData(plot.getImage());
        int n = 0;
        if (data != null) {
            int b;
            for (byte aData : data) {
                b = aData & 0xff;
                if (b != 0) {
                    n++;
                }
            }
        }
        return n;
    }

    @Override
    protected String getDataAsText() {
        final BufferedImage image = plot.getImage();
        final Rectangle2D bounds = plot.getImageDataBounds();

        final byte[] data = getValidData(image);
        if (data == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder(64000);
        final int w = image.getWidth();
        final int h = image.getHeight();

        final RasterDataNode rasterX = getRaster(X_VAR);
        assert rasterX != null;
        final String nameX = rasterX.getName();
        final double sampleMinX = bounds.getMinX();
        final double sampleMaxX = bounds.getMaxX();

        final RasterDataNode rasterY = getRaster(Y_VAR);
        assert rasterY != null;
        final String nameY = rasterY.getName();
        final double sampleMinY = bounds.getMinY();
        final double sampleMaxY = bounds.getMaxY();

        sb.append("Product name:\t").append(rasterX.getProduct().getName()).append("\n");
        sb.append("Dataset X name:\t").append(nameX).append("\n");
        sb.append("Dataset Y name:\t").append(nameY).append("\n");
        sb.append('\n');
        sb.append(nameX).append(" minimum:\t").append(sampleMinX).append("\t").append(rasterX.getUnit()).append(
                "\n");
        sb.append(nameX).append(" maximum:\t").append(sampleMaxX).append("\t").append(rasterX.getUnit()).append(
                "\n");
        sb.append(nameX).append(" bin size:\t").append((sampleMaxX - sampleMinX) / w).append("\t").append(
                rasterX.getUnit()).append("\n");
        sb.append(nameX).append(" #bins:\t").append(w).append("\n");
        sb.append('\n');
        sb.append(nameY).append(" minimum:\t").append(sampleMinY).append("\t").append(rasterY.getUnit()).append(
                "\n");
        sb.append(nameY).append(" maximum:\t").append(sampleMaxY).append("\t").append(rasterY.getUnit()).append(
                "\n");
        sb.append(nameY).append(" bin size:\t").append((sampleMaxY - sampleMinY) / h).append("\t").append(
                rasterY.getUnit()).append("\n");
        sb.append(nameY).append(" #bins:\t").append(h).append("\n");
        sb.append('\n');

        sb.append(nameX);
        sb.append('\t');
        sb.append(nameY);
        sb.append('\t');
        sb.append("Bin counts\t(cropped at 255)");
        sb.append('\n');

        int x, y, z;
        double v1, v2;
        for (int i = 0; i < data.length; i++) {
            z = data[i] & 0xff;
            if (z != 0) {

                x = i % w;
                y = h - i / w - 1;

                v1 = sampleMinX + ((x + 0.5) * (sampleMaxX - sampleMinX)) / w;
                v2 = sampleMinY + ((y + 0.5) * (sampleMaxY - sampleMinY)) / h;

                sb.append(v1);
                sb.append('\t');
                sb.append(v2);
                sb.append('\t');
                sb.append(z);
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    @SuppressWarnings("unused")
    private static class DataSourceConfig {

        public boolean useRoiMask;
        public Mask roiMask;
        private Product xProduct;
        private Product yProduct;
        private RasterDataNode xBand;
        private RasterDataNode yBand;
        private Property xProductProperty;
        private Property yProductProperty;
        private Property xBandProperty;
        private Property yBandProperty;
    }

    private static class BandListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
                this.setText(((RasterDataNode) value).getName());
            }
            return this;
        }
    }

    private static class ProductListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
                this.setText(formatProductName((Product) value));
            }
            return this;
        }
    }

}

