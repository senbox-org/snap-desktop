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
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.jidesoft.swing.SimpleScrollPane;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.AbstractButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.StxFactory;
import org.esa.snap.core.image.FillConstantOpImage;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.Debug;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.colormanip.ScatterPlot3DColorManipulationPanel;
import org.esa.snap.rcp.colormanip.ScatterPlot3DFormModel;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.windows.TopComponent;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.List;

/**
 * The 3D scatter plot pane within the statistics window.
 *
 * @author Tonio Fincke
 */
class ScatterPlot3DPlotPanel extends PagePanel {

    private AbstractButton hideAndShowButton;
    private JPanel backgroundPanel;
    private RoiMaskSelector roiMaskSelector;
    private AbstractButton refreshButton;

    private AxisRangeControl xAxisRangeControl;
    private AxisRangeControl yAxisRangeControl;
    private AxisRangeControl zAxisRangeControl;

    private final static String PROPERTY_NAME_AUTO_MIN_MAX = "autoMinMax";
    private final static String PROPERTY_NAME_MIN = "min";
    private final static String PROPERTY_NAME_MAX = "max";
    private final static String PROPERTY_NAME_USE_ROI_MASK = "useRoiMask";
    private final static String PROPERTY_NAME_ROI_MASK = "roiMask";
    private final static String PROPERTY_NAME_DISPLAY_LEVEL = "displayLevel";
    private final static String PROPERTY_NAME_X_PRODUCT = "xProduct";
    private final static String PROPERTY_NAME_Y_PRODUCT = "yProduct";
    private final static String PROPERTY_NAME_Z_PRODUCT = "zProduct";
    private final static String PROPERTY_NAME_COLOR_PRODUCT = "colorProduct";
    private final static String PROPERTY_NAME_X_BAND = "xBand";
    private final static String PROPERTY_NAME_Y_BAND = "yBand";
    private final static String PROPERTY_NAME_Z_BAND = "zBand";
    private final static String PROPERTY_NAME_COLOR_BAND = "colorBand";

    private BindingContext bindingContext;
    private DataSourceConfig dataSourceConfig;
    private Property xProductProperty;
    private Property yProductProperty;
    private Property zProductProperty;
    private Property colorProductProperty;
    private Property xBandProperty;
    private Property yBandProperty;
    private Property zBandProperty;
    private Property colorBandProperty;
    private Property displayLevelProperty;
    private JComboBox<ListCellRenderer> xProductList;
    private JComboBox<ListCellRenderer> yProductList;
    private JComboBox<ListCellRenderer> zProductList;
    private JComboBox<ListCellRenderer> colorProductList;
    private JComboBox<ListCellRenderer> xBandList;
    private JComboBox<ListCellRenderer> yBandList;
    private JComboBox<ListCellRenderer> zBandList;
    private JComboBox<ListCellRenderer> colorBandList;
    //todo instead using referenceSize, use referenceSceneRasterTransform
    private Dimension referenceSize;

    private static final String CHART_TITLE = "3D Scatter Plot";
    private static final String NO_DATA_MESSAGE = "No 3D scatter plot computed yet.";
    private static final int NUM_DECIMALS = 2;
    private ScatterPlot3dJzyPanel scatterPlot3dJzyPanel;
    private ScatterPlot3DColorManipulationPanel colorManipulationPanel;
    private ScatterPlot3DFormModel formModel;
    private JCheckBox projectToXCheckBox;
    private JCheckBox projectToYCheckBox;
    private JCheckBox projectToZCheckBox;
    private float minX;
    private float maxX;
    private float minY;
    private float maxY;
    private float minZ;
    private float maxZ;
    private JSpinner levelSpinner;

    ScatterPlot3DPlotPanel(TopComponent parentDialog, String helpId) {
        super(parentDialog, helpId, CHART_TITLE);
    }

    @Override
    protected void initComponents() {
        dataSourceConfig = new DataSourceConfig();
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(dataSourceConfig));

        levelSpinner = new JSpinner();
        displayLevelProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_DISPLAY_LEVEL);
        try {
            displayLevelProperty.setValue(new Integer(0));
        } catch (ValidationException e) {
            // do nothing
        }
        displayLevelProperty.getDescriptor().setValueRange(new ValueRange(0, 1));
        bindingContext.bind(PROPERTY_NAME_DISPLAY_LEVEL, levelSpinner);

        xBandList = new JComboBox<>();
        xBandList.setRenderer(new BandListCellRenderer());
        bindingContext.bind(PROPERTY_NAME_X_BAND, xBandList);
        xBandList.addActionListener(e -> {
            final Object value = xBandList.getSelectedItem();
            if (value != null) {
                final Dimension rasterSize = ((RasterDataNode) value).getRasterSize();
                if (!rasterSize.equals(referenceSize)) {
                    referenceSize = rasterSize;
                    updateBandList(getProduct(), yBandProperty, true);
                    updateBandList(getProduct(), zBandProperty, true);
                    updateBandList(getProduct(), colorBandProperty, true);
                }
            }
        });
        xBandList.addActionListener(new LevelActionListener());
        xBandProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_X_BAND);

        yBandList = new JComboBox<>();
        yBandList.setRenderer(new BandListCellRenderer());
        yBandList.addActionListener(new LevelActionListener());
        bindingContext.bind(PROPERTY_NAME_Y_BAND, yBandList);
        yBandProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_Y_BAND);

        zBandList = new JComboBox<>();
        zBandList.setRenderer(new BandListCellRenderer());
        zBandList.addActionListener(new LevelActionListener());
        bindingContext.bind(PROPERTY_NAME_Z_BAND, zBandList);
        zBandProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_Z_BAND);

        formModel = new ScatterPlot3DFormModel();
        colorManipulationPanel = new ScatterPlot3DColorManipulationPanel(this, formModel);
        colorBandList = new JComboBox<>();
        colorBandList.setRenderer(new BandListCellRenderer());
        colorBandList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Object value = colorBandList.getSelectedItem();
                colorManipulationPanel.setRasterDataNode((RasterDataNode) value);
            }
        });
        bindingContext.bind(PROPERTY_NAME_COLOR_BAND, colorBandList);
        colorBandProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_COLOR_BAND);

        xProductList = new JComboBox<>();
        xProductList.addItemListener(new ProductListListener(xBandProperty, false));
        xProductList.setRenderer(new ProductListCellRenderer());
        bindingContext.bind(PROPERTY_NAME_X_PRODUCT, xProductList);
        xProductProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_X_PRODUCT);

        yProductList = new JComboBox<>();
        yProductList.addItemListener(new ProductListListener(yBandProperty, false));
        yProductList.setRenderer(new ProductListCellRenderer());
        bindingContext.bind(PROPERTY_NAME_Y_PRODUCT, yProductList);
        yProductProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_Y_PRODUCT);

        zProductList = new JComboBox<>();
        zProductList.addItemListener(new ProductListListener(zBandProperty, false));
        zProductList.setRenderer(new ProductListCellRenderer());
        bindingContext.bind(PROPERTY_NAME_Z_PRODUCT, zProductList);
        zProductProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_Z_PRODUCT);

        colorProductList = new JComboBox<>();
        colorProductList.addItemListener(new ProductListListener(colorBandProperty, false));
        colorProductList.setRenderer(new ProductListCellRenderer());
        bindingContext.bind(PROPERTY_NAME_COLOR_PRODUCT, colorProductList);
        colorProductProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_COLOR_PRODUCT);

        projectToXCheckBox = new JCheckBox("Project Data to X-Plane");
        projectToXCheckBox.addActionListener(e -> {
            scatterPlot3dJzyPanel.projectToX(projectToXCheckBox.isSelected());
        });
        projectToYCheckBox = new JCheckBox("Project Data to Y-Plane");
        projectToYCheckBox.addActionListener(e -> {
            scatterPlot3dJzyPanel.projectToY(projectToYCheckBox.isSelected());
        });
        projectToZCheckBox = new JCheckBox("Project Data to Z-Plane");
        projectToZCheckBox.addActionListener(e -> {
            scatterPlot3dJzyPanel.projectToZ(projectToZCheckBox.isSelected());
        });

        xAxisRangeControl = new AxisRangeControl("X-Axis");
        yAxisRangeControl = new AxisRangeControl("Y-Axis");
        zAxisRangeControl = new AxisRangeControl("Z-Axis");
        scatterPlot3dJzyPanel = new ScatterPlot3dJzyPanel();
        scatterPlot3dJzyPanel.init();
        createUI(scatterPlot3dJzyPanel, createOptionsPanel(), new RoiMaskSelector(bindingContext));
        initActionEnablers();
        updateUIState();
    }

    private void initActionEnablers() {
        RefreshActionEnabler refreshActionEnabler = new RefreshActionEnabler(refreshButton, PROPERTY_NAME_USE_ROI_MASK,
                PROPERTY_NAME_ROI_MASK, PROPERTY_NAME_DISPLAY_LEVEL);
        refreshActionEnabler.addProductBandEnablement(PROPERTY_NAME_X_PRODUCT, PROPERTY_NAME_X_BAND);
        refreshActionEnabler.addProductBandEnablement(PROPERTY_NAME_Y_PRODUCT, PROPERTY_NAME_Y_BAND);
        refreshActionEnabler.addProductBandEnablement(PROPERTY_NAME_Z_PRODUCT, PROPERTY_NAME_Z_BAND);
        refreshActionEnabler.addProductBandEnablement(PROPERTY_NAME_COLOR_PRODUCT, PROPERTY_NAME_COLOR_BAND, true);
        bindingContext.addPropertyChangeListener(refreshActionEnabler);
        RefreshActionEnabler rangeControlActionEnabler = new RefreshActionEnabler(refreshButton, PROPERTY_NAME_MIN,
                PROPERTY_NAME_AUTO_MIN_MAX, PROPERTY_NAME_MAX);
        xAxisRangeControl.getBindingContext().addPropertyChangeListener(rangeControlActionEnabler);
        yAxisRangeControl.getBindingContext().addPropertyChangeListener(rangeControlActionEnabler);
        zAxisRangeControl.getBindingContext().addPropertyChangeListener(rangeControlActionEnabler);
        formModel.addPropertyChangeListener(evt -> refreshButton.setEnabled(true));
    }

    private void setMaxAllowedLevel() {
        int maxAllowedLevel = 10000;
        RasterDataNode[] nodes = new RasterDataNode[]{dataSourceConfig.xBand, dataSourceConfig.yBand,
                dataSourceConfig.zBand, dataSourceConfig.colorBand};
        for (RasterDataNode node : nodes) {
            if (node != null) {
                maxAllowedLevel = Math.min(node.getMultiLevelModel().getLevelCount() - 1, maxAllowedLevel);
            }
        }
        if (maxAllowedLevel == 10000) {
            maxAllowedLevel = 0;
        }

        displayLevelProperty.getDescriptor().setValueRange(new ValueRange(0, maxAllowedLevel));
        bindingContext.unbind(bindingContext.getBinding(PROPERTY_NAME_DISPLAY_LEVEL));
        bindingContext.bind(PROPERTY_NAME_DISPLAY_LEVEL, levelSpinner);
    }

    private float[] getData(RasterDataNode node, int level) {
        Mask mask = dataSourceConfig.useRoiMask ? dataSourceConfig.roiMask : null;
        RenderedImage image;
        if (mask != null) {
            image = createMaskedGeophysicalImage(node, mask, level);
        } else {
            image = node.getGeophysicalImage().getImage(level);
        }
        final int size = image.getWidth() * image.getHeight();
        float[] data = new float[size];
        image.getData().getPixels(0, 0, image.getWidth(), image.getHeight(), data);
        return data;
    }

    private List[] getDataLists(float[] xData, float xNoDataValue, float[] yData, float yNoDataValue,
                                float[] zData, float zNoDataValue, int[] colorData, int numColorBands) {
        List[] dataLists = new List[4];
        dataLists[0] = new ArrayList<Float>();
        dataLists[1] = new ArrayList<Float>();
        dataLists[2] = new ArrayList<Float>();
        dataLists[3] = new ArrayList<Integer>();
        for (int i = 0; i < xData.length; i++) {
            if (isValid(xData[i], xNoDataValue) && isValid(yData[i], yNoDataValue) && isValid(zData[i], zNoDataValue)) {
                dataLists[0].add(xData[i]);
                dataLists[1].add(yData[i]);
                dataLists[2].add(zData[i]);
                dataLists[3].add(colorData[i * numColorBands]);
                dataLists[3].add(colorData[i * numColorBands + 1]);
                dataLists[3].add(colorData[i * numColorBands + 2]);
            }
        }
        return dataLists;
    }

    private boolean isValid(float value, float noDataValue) {
        if (Float.isNaN(value)) {
            return false;
        }
        if (Float.isNaN(noDataValue)) {
            return true;
        }
        return Math.abs(value - noDataValue) > 1e-8;
    }

    private void updateChartData() {
        int level = dataSourceConfig.displayLevel;
        float[] xData = getData(dataSourceConfig.xBand, level);
        float[] yData = getData(dataSourceConfig.yBand, level);
        float[] zData = getData(dataSourceConfig.zBand, level);

        final RasterDataNode xNode = dataSourceConfig.xBand;
        final RasterDataNode yNode = dataSourceConfig.yBand;
        final RasterDataNode zNode = dataSourceConfig.zBand;
        final RasterDataNode colorNode = dataSourceConfig.colorBand;

        final ImageInfo imageInfo = formModel.getModifiedImageInfo();
        final RenderedImage colorImage =
                ImageManager.getInstance().createColoredBandImage(new RasterDataNode[]{colorNode}, imageInfo, level);
        int numColorBands = colorImage.getSampleModel().getNumBands();
        final int colorSize = colorImage.getWidth() * colorImage.getHeight() * numColorBands;
        int[] colorData = new int[colorSize];
        colorImage.getData().getPixels(0, 0, colorImage.getWidth(), colorImage.getHeight(), colorData);

        List[] dataLists = getDataLists(xData, (float) xNode.getGeophysicalNoDataValue(),
                yData, (float) yNode.getGeophysicalNoDataValue(), zData, (float) zNode.getGeophysicalNoDataValue(),
                colorData, numColorBands);

        Mask mask = dataSourceConfig.useRoiMask ? dataSourceConfig.roiMask : null;
        setMinAndMaxValuesFromArray(dataLists[0], dataLists[1], dataLists[2]);
        if (mask != null) {
            xAxisRangeControl.adjustComponents(minX, maxX, NUM_DECIMALS);
            yAxisRangeControl.adjustComponents(minY, maxY, NUM_DECIMALS);
            zAxisRangeControl.adjustComponents(minZ, maxZ, NUM_DECIMALS);
        } else {
            try {
                setRange(xAxisRangeControl, xNode, null, ProgressMonitor.NULL);
                setRange(yAxisRangeControl, yNode, null, ProgressMonitor.NULL);
                setRange(zAxisRangeControl, zNode, null, ProgressMonitor.NULL);
            } catch (IOException e) {
                xAxisRangeControl.adjustComponents(minX, maxX, NUM_DECIMALS);
                yAxisRangeControl.adjustComponents(minY, maxY, NUM_DECIMALS);
                zAxisRangeControl.adjustComponents(minZ, maxZ, NUM_DECIMALS);
            }
        }
        scatterPlot3dJzyPanel.setChartTitle("3D Scatter Plot");
        scatterPlot3dJzyPanel.setLabelNames(xNode.getName(), yNode.getName(), zNode.getName());
        scatterPlot3dJzyPanel.setChartBounds(
                xAxisRangeControl.getMin().floatValue(), xAxisRangeControl.getMax().floatValue(),
                yAxisRangeControl.getMin().floatValue(), yAxisRangeControl.getMax().floatValue(),
                zAxisRangeControl.getMin().floatValue(), zAxisRangeControl.getMax().floatValue());
        scatterPlot3dJzyPanel.setChartData(dataLists[0], dataLists[1], dataLists[2]);
        scatterPlot3dJzyPanel.setColors(dataLists[3]);
        renderChart();
    }

    private void setMinAndMaxValuesFromArray(List<Float> xData, List<Float> yData, List<Float> zData) {
        minX = Collections.min(xData);
        maxX = Collections.max(xData);
        minY = Collections.min(yData);
        maxY = Collections.max(yData);
        minZ = Collections.min(zData);
        maxZ = Collections.max(zData);
    }

    private JPanel createOptionsPanel() {
        final JPanel optionsPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=0,weightx=1,gridx=0");
//        CollapsiblePane xAxisCollapsiblePane = new CollapsiblePane("X-Axis", xAxisRangeControl.getPanel(), false, false);
//        GridBagUtils.addToPanel(optionsPanel, xAxisCollapsiblePane, gbc, "gridy=0, insets.top=2");
        GridBagUtils.addToPanel(optionsPanel, xAxisRangeControl.getPanel(), gbc, "gridy=0, insets.top=2");
        GridBagUtils.addToPanel(optionsPanel, xProductList, gbc, "gridy=1,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, xBandList, gbc, "gridy=2,insets.left=4,insets.right=2");
//        CollapsiblePane yAxisCollapsiblePane = new CollapsiblePane("Y-Axis", yAxisRangeControl.getPanel(), false, false);
//        GridBagUtils.addToPanel(optionsPanel, yAxisCollapsiblePane, gbc, "gridy=3,insets.left=0,insets.right=0");
        GridBagUtils.addToPanel(optionsPanel, yAxisRangeControl.getPanel(), gbc, "gridy=3,insets.left=0,insets.right=0");
        GridBagUtils.addToPanel(optionsPanel, yProductList, gbc, "gridy=4,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, yBandList, gbc, "gridy=5,insets.left=4,insets.right=2");
//        CollapsiblePane zAxisCollapsiblePane = new CollapsiblePane("Z-Axis", zAxisRangeControl.getPanel(), false, false);
//        GridBagUtils.addToPanel(optionsPanel, zAxisCollapsiblePane, gbc, "gridy=6,insets.left=0,insets.right=0");
        GridBagUtils.addToPanel(optionsPanel, zAxisRangeControl.getPanel(), gbc, "gridy=6,insets.left=0,insets.right=0");
        GridBagUtils.addToPanel(optionsPanel, zProductList, gbc, "gridy=7,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, zBandList, gbc, "gridy=8,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, new TitledSeparator("Colour Axis"), gbc, "gridy=9,insets.left=4,insets.right=0");
        GridBagUtils.addToPanel(optionsPanel, colorProductList, gbc, "gridy=10,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, colorBandList, gbc, "gridy=11,insets.left=4,insets.right=2");
        final JPanel colorPanel = colorManipulationPanel.getContentPanel();
        GridBagUtils.addToPanel(optionsPanel, colorPanel, gbc, "gridy=12,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, new JSeparator(), gbc, "gridy=13,insets.left=4,insets.right=0");
        TableLayout levelPanelLayout = new TableLayout(2);
        levelPanelLayout.setColumnWeightX(0, 1.0);
        levelPanelLayout.setColumnFill(0, TableLayout.Fill.HORIZONTAL);
        levelPanelLayout.setColumnFill(1, TableLayout.Fill.NONE);
        levelPanelLayout.setTablePadding(0, 2);
        levelPanelLayout.setColumnWeightX(1, 0.0);
        JPanel levelPanel = new JPanel(levelPanelLayout);
        levelPanel.add(new JLabel("Level of Detail: "));
        levelPanel.add(levelSpinner);
        GridBagUtils.addToPanel(optionsPanel, levelPanel, gbc, "gridy=14,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, projectToXCheckBox, gbc, "gridy=15,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, projectToYCheckBox, gbc, "gridy=16,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, projectToZCheckBox, gbc, "gridy=17,insets.left=4,insets.right=2");
        return optionsPanel;
    }

    private static void setRange(AxisRangeControl axisRangeControl, RasterDataNode raster, Mask mask, ProgressMonitor pm) throws IOException {
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

    @Override
    protected String getDataAsText() {
        return null;
    }

    @Override
    protected void updateComponents() {
        final ValueSet productValueSet = new ValueSet(createAvailableProductList());
        xProductProperty.getDescriptor().setValueSet(productValueSet);
        yProductProperty.getDescriptor().setValueSet(productValueSet);
        zProductProperty.getDescriptor().setValueSet(productValueSet);
        colorProductProperty.getDescriptor().setValueSet(productValueSet);

        if (productValueSet.getItems().length > 0) {
            Product currentProduct = getProduct();
            try {
                xProductProperty.setValue(currentProduct);
                yProductProperty.setValue(currentProduct);
                zProductProperty.setValue(currentProduct);
                colorProductProperty.setValue(currentProduct);
            } catch (ValidationException ignored) {
                Debug.trace(ignored);
            }
        }
        updateBandList(getProduct(), xBandProperty, false);
        updateBandList(getProduct(), yBandProperty, true);
        updateBandList(getProduct(), zBandProperty, true);
        updateBandList(getProduct(), colorBandProperty, true);
        roiMaskSelector.updateMaskSource(getProduct(), getRaster());
        updateUIState();
    }

    private static Product[] createAvailableProductList() {
        return SnapApp.getDefault().getProductManager().getProducts();
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

    private void updateUIState() {
        refreshButton.setEnabled(xBandProperty.getValue() != null && yBandProperty.getValue() != null &&
                zBandProperty.getValue() != null);
        xAxisRangeControl.setComponentsEnabled(getRaster() != null);
        yAxisRangeControl.setComponentsEnabled(getRaster() != null);
        zAxisRangeControl.setComponentsEnabled(getRaster() != null);
    }

    private JPanel createTopPanel() {
        refreshButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ViewRefresh22.png"),
                false);
        refreshButton.setToolTipText("Refresh View");
        refreshButton.setName("refreshButton");
        refreshButton.addActionListener(e -> {
            updateChartData();
            refreshButton.setEnabled(false);
        });

        AbstractButton switchToTableButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Table24.png"),
                false);
        switchToTableButton.setToolTipText("Switch to Table View");
        switchToTableButton.setName("switchToTableButton");
        switchToTableButton.setEnabled(hasAlternativeView());
        switchToTableButton.addActionListener(e -> showAlternativeView());

        final TableLayout tableLayout = new TableLayout(6);
        tableLayout.setColumnFill(2, TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(2, 1.0);
        tableLayout.setRowPadding(0, new Insets(0, 4, 0, 0));
        JPanel buttonPanel = new JPanel(tableLayout);
        buttonPanel.add(refreshButton);
        tableLayout.setRowPadding(0, new Insets(0, 0, 0, 0));
        buttonPanel.add(switchToTableButton);
        buttonPanel.add(new JPanel());

        return buttonPanel;
    }

    private JPanel createChartBottomPanel(final ScatterPlot3dJzyPanel chartPanel) {

        final AbstractButton zoomAllButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/view-fullscreen.png"),
                false);
        zoomAllButton.setToolTipText("Zoom all.");
        zoomAllButton.setName("zoomAllButton.");
        zoomAllButton.addActionListener(e -> {
            chartPanel.setChartBounds(minX, maxX, minY, maxY, minZ, maxZ);
            chartPanel.repaint();
        });

        final AbstractButton propertiesButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Edit24.gif"),
                false);
        propertiesButton.setToolTipText("Edit properties.");
        propertiesButton.setName("propertiesButton.");
        propertiesButton.setEnabled(false);

        final AbstractButton saveButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Export24.gif"),
                false);
        saveButton.setToolTipText("Save chart as image.");
        saveButton.setName("saveButton.");
        saveButton.addActionListener(e -> {
            try {
                chartPanel.doSaveAs();
            } catch (IOException e1) {
                AbstractDialog.showErrorDialog(chartPanel, "Could not save chart:\n" + e1.getMessage(), "Error");
            }
        });

        final AbstractButton printButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Print24.gif"),
                false);
        printButton.setToolTipText("Print chart.");
        printButton.setName("printButton.");
        printButton.setEnabled(false);

        final TableLayout tableLayout = new TableLayout(6);
        tableLayout.setColumnFill(4, TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(4, 1.0);
        JPanel buttonPanel = new JPanel(tableLayout);
        tableLayout.setRowPadding(0, new Insets(0, 4, 0, 0));
        buttonPanel.add(zoomAllButton);
        tableLayout.setRowPadding(0, new Insets(0, 0, 0, 0));
        buttonPanel.add(propertiesButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(printButton);
        buttonPanel.add(new JPanel());
        buttonPanel.add(getHelpButton());

        return buttonPanel;
    }

    /**
     * Responsible for creating the UI layout.
     *
     * @param chartPanel      the panel of the chart
     * @param optionsPanel    the options panel for changing settings
     * @param roiMaskSelector optional ROI mask selector, can be {@code null} if not wanted.
     */
    protected void createUI(ScatterPlot3dJzyPanel chartPanel, JPanel optionsPanel, RoiMaskSelector roiMaskSelector) {
        this.roiMaskSelector = roiMaskSelector;
        final JPanel extendedOptionsPanel = GridBagUtils.createPanel();
        GridBagConstraints extendedOptionsPanelConstraints = GridBagUtils.createConstraints("insets.left=4,insets.right=2,anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1");
        GridBagUtils.addToPanel(extendedOptionsPanel, new JSeparator(), extendedOptionsPanelConstraints, "gridy=0");
        GridBagUtils.addToPanel(extendedOptionsPanel, roiMaskSelector.createPanel(), extendedOptionsPanelConstraints, "gridy=1,insets.left=-4");
        GridBagUtils.addToPanel(extendedOptionsPanel, new JPanel(), extendedOptionsPanelConstraints, "gridy=1,insets.left=-4");
        GridBagUtils.addToPanel(extendedOptionsPanel, optionsPanel, extendedOptionsPanelConstraints, "insets.left=0,insets.right=0,gridy=2,fill=VERTICAL,fill=HORIZONTAL,weighty=1");
        GridBagUtils.addToPanel(extendedOptionsPanel, new JSeparator(), extendedOptionsPanelConstraints, "insets.left=4,insets.right=2,gridy=5,anchor=SOUTHWEST");

        final SimpleScrollPane optionsScrollPane = new SimpleScrollPane(extendedOptionsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        optionsScrollPane.setBorder(null);
        optionsScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        final JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(createTopPanel(), BorderLayout.NORTH);
        rightPanel.add(optionsScrollPane, BorderLayout.CENTER);
        rightPanel.add(createChartBottomPanel(chartPanel), BorderLayout.SOUTH);

        final ImageIcon collapseIcon = UIUtils.loadImageIcon("icons/PanelRight12.png");
        final ImageIcon collapseRolloverIcon = ToolButtonFactory.createRolloverIcon(collapseIcon);
        final ImageIcon expandIcon = UIUtils.loadImageIcon("icons/PanelLeft12.png");
        final ImageIcon expandRolloverIcon = ToolButtonFactory.createRolloverIcon(expandIcon);

        hideAndShowButton = ToolButtonFactory.createButton(collapseIcon, false);
        hideAndShowButton.setToolTipText("Collapse Options Panel");
        hideAndShowButton.setName("switchToChartButton");
        hideAndShowButton.addActionListener(new ActionListener() {

            private boolean rightPanelShown;

            @Override
            public void actionPerformed(ActionEvent e) {
                rightPanel.setVisible(rightPanelShown);
                if (rightPanelShown) {
                    hideAndShowButton.setIcon(collapseIcon);
                    hideAndShowButton.setRolloverIcon(collapseRolloverIcon);
                    hideAndShowButton.setToolTipText("Collapse Options Panel");
                } else {
                    hideAndShowButton.setIcon(expandIcon);
                    hideAndShowButton.setRolloverIcon(expandRolloverIcon);
                    hideAndShowButton.setToolTipText("Expand Options Panel");
                }
                rightPanelShown = !rightPanelShown;
            }
        });

        backgroundPanel = new JPanel(new BorderLayout());
        backgroundPanel.add(chartPanel, BorderLayout.CENTER);
        backgroundPanel.add(rightPanel, BorderLayout.EAST);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.add(backgroundPanel, new Integer(0));
        layeredPane.add(hideAndShowButton, new Integer(1));
        add(layeredPane);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        backgroundPanel.setBounds(0, 0, getWidth() - 8, getHeight() - 8);
        hideAndShowButton.setBounds(getWidth() - hideAndShowButton.getWidth() - 12, 2, 24, 24);
    }

    private static String formatProductName(final Product product) {
        String name = product.getName().substring(0, Math.min(10, product.getName().length()));
        if (product.getName().length() > 10) {
            name += "...";
        }
        return product.getProductRefString() + name;
    }

    void renderChart() {
        if (scatterPlot3dJzyPanel != null) {
            scatterPlot3dJzyPanel.renderChart();
        }
    }

    private static MultiLevelImage createMaskedGeophysicalImage(final RasterDataNode node, final Mask mask, int level) {
        MultiLevelImage varImage = node.getGeophysicalImage();
        MultiLevelImage maskImage = mask.getSourceImage();
        final MultiLevelModel multiLevelModel = node.getMultiLevelModel();
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(multiLevelModel) {

            @Override
            public RenderedImage createImage(int sourceLevel) {
                return new FillConstantOpImage(varImage.getImage(level), maskImage.getImage(level),
                        node.getGeophysicalNoDataValue());
            }
        });
    }

    private static class DataSourceConfig {

        private boolean useRoiMask;
        private Mask roiMask;
        private Product xProduct;
        private Product yProduct;
        private Product zProduct;
        private Product colorProduct;
        private RasterDataNode xBand;
        private RasterDataNode yBand;
        private RasterDataNode zBand;
        private RasterDataNode colorBand;
        private Product xProductProperty;
        private Product yProductProperty;
        private Product zProductProperty;
        private Product colorProductProperty;
        private Integer displayLevel;
        private Property xBandProperty;
        private Property yBandProperty;
        private Property zBandProperty;
        private Property colorBandProperty;
    }

    private class LevelActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            setMaxAllowedLevel();
        }
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

    private class ProductListListener implements ItemListener {

        private final Property bandProperty;
        private final boolean considerReferenceSize;

        ProductListListener(Property bandProperty, boolean considerReferenceSize) {
            this.bandProperty = bandProperty;
            this.considerReferenceSize = considerReferenceSize;
        }

        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                final Product selectedProduct = (Product) event.getItem();
                updateBandList(selectedProduct, bandProperty, considerReferenceSize);
            }
        }

    }

}

