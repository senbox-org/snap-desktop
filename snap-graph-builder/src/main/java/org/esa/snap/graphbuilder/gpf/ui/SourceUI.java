/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.gpf.ui;

import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.util.GeoUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.loading.SwingUtils;
import org.esa.snap.ui.product.ProductSubsetByPolygonUiComponents;
import org.locationtech.jts.geom.Geometry;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Reader OperatorUI
 */
public class SourceUI extends BaseOperatorUI {


    private static final int MIN_SCENE_VALUE = 0;
    private static final String FILE_PARAMETER = "file";
    private static final String FORMAT_PARAMETER = "formatName";
    private static final String USE_ADVANCED_OPTIONS_PARAMETER = "useAdvancedOptions";
    private static final String BAND_LIST_PARAMETER = "bandNames";
    private static final String MASK_LIST_PARAMETER = "maskNames";
    private static final String PIXEL_REGION_PARAMETER = "pixelRegion";
    private static final String GEOMETRY_REGION_PARAMETER = "geometryRegion";
    private static final String POLYGON_REGION_PARAMETER = "polygonRegion";
    private static final String COPY_METADATA_PARAMETER = "copyMetadata";
    private static final String ANY_FORMAT = "Any Format";
    private final JList bandList = new JList();
    private final JList maskList = new JList();
    SourceProductSelector sourceProductSelector = null;
    private JComboBox<String> formatNameComboBox = new JComboBox<>();
    private JButton advancedOptionsBtn = new JButton("Advanced options");
    private JPanel advancedOptionsPanel = new JPanel(new GridBagLayout());
    private AtomicBoolean updatingUI = new AtomicBoolean(false);
    private JCheckBox copyMetadata = new JCheckBox("Copy Metadata", true);
    private JRadioButton pixelCoordRadio = new JRadioButton("Pixel Coordinates");
    private JRadioButton geoCoordRadio = new JRadioButton("Geographic Coordinates");
    private final JRadioButton vectorFileRadio = new JRadioButton("Polygon");
    private JPanel pixelPanel = new JPanel(new GridBagLayout());
    private JPanel geoPanel = new JPanel(new GridBagLayout());
    private final ProductSubsetByPolygonUiComponents productSubsetByPolygonUiComponents = new ProductSubsetByPolygonUiComponents(this.geoPanel);
    private final JPanel vectorFilePanel = productSubsetByPolygonUiComponents.getImportVectorFilePanel();
    private JSpinner pixelCoordXSpinner;
    private JSpinner pixelCoordYSpinner;
    private JSpinner pixelCoordWidthSpinner;
    private JSpinner pixelCoordHeightSpinner;
    private JSpinner geoCoordWestLongSpinner;
    private JSpinner geoCoordEastLongSpinner;
    private JSpinner geoCoordNorthLatSpinner;
    private JSpinner geoCoordSouthLatSpinner;

    private static List<String> getFormatsForFile(final File file) {
        final Iterator<ProductReaderPlugIn> allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        final List<String> formatNameList = new ArrayList<>();

        while (allReaderPlugIns.hasNext()) {
            ProductReaderPlugIn reader = allReaderPlugIns.next();
            String[] formatNames = reader.getFormatNames();
            for (String formatName : formatNames) {
                if (file == null || reader.getDecodeQualification(file) != DecodeQualification.UNABLE &&
                        !formatNameList.contains(formatName)) {
                    formatNameList.add(formatName);
                }
            }
        }
        formatNameList.sort(String::compareTo);
        formatNameList.add(0, ANY_FORMAT);

        return formatNameList;
    }

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        paramMap = parameterMap;
        sourceProductSelector = new SourceProductSelector(appContext);
        sourceProductSelector.initProducts();
        sourceProductSelector.addSelectionChangeListener(new SourceSelectionChangeListener());

        final JComponent panel = createPanel();
        initParameters();
        final Product selectedProduct = sourceProductSelector.getSelectedProduct();
        if (selectedProduct != null) {
            updateFormatNamesCombo(selectedProduct.getFileLocation());
            resetProductSubsetByPolygon(selectedProduct);
        }
        return new JScrollPane(panel);
    }

    private void updateFormatNamesCombo(final File file) {
        if (file == null) {
            return;
        }
        final List<String> formatNameList = getFormatsForFile(file);

        formatNameComboBox.removeAllItems();
        for (String format : formatNameList) {
            formatNameComboBox.addItem(format);
        }
    }

    @Override
    public void initParameters() {
        assert (paramMap != null);
        final Object fileValue = paramMap.get(FILE_PARAMETER);
        if (fileValue != null) {
            try {
                final File file = (File) fileValue;
                Product srcProduct = null;
                // check if product is already opened
                final Product[] openedProducts = SnapApp.getDefault().getProductManager().getProducts();
                for (Product openedProduct : openedProducts) {
                    if (file.equals(openedProduct.getFileLocation())) {
                        srcProduct = openedProduct;
                        break;
                    }
                }
                if (srcProduct == null) {
                    srcProduct = CommonReaders.readProduct(file);
                }
                if (sourceProductSelector.getSelectedProduct() == null || sourceProductSelector.getSelectedProduct().getFileLocation() != fileValue) {
                    sourceProductSelector.setSelectedProduct(srcProduct);
                }
            } catch (IOException e) {
                // do nothing
            }
        }
        final Object formatValue = paramMap.get(FORMAT_PARAMETER);
        if (formatValue != null) {
            formatNameComboBox.setSelectedItem(formatValue);
        } else {
            formatNameComboBox.setSelectedItem(ANY_FORMAT);
        }
    }

    @Override
    public UIValidation validateParameters() {
        if (sourceProductSelector != null && sourceProductSelector.getSelectedProduct() == null) {
            return new UIValidation(UIValidation.State.ERROR, "Source product not selected");
        }
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        if (sourceProductSelector != null) {
            final Product prod = sourceProductSelector.getSelectedProduct();
            if (prod != null && prod.getFileLocation() != null) {
                File currentProductFileLocation = (File) paramMap.get(FILE_PARAMETER);
                paramMap.put(FILE_PARAMETER, prod.getFileLocation());
                if (currentProductFileLocation == null || currentProductFileLocation != prod.getFileLocation()) {
                    // sourceProducts from BaseOperatorUI should be populated in order to be able to later obtain getBandNames(), getGeometries(),
                    // therefore calling setSourceProduct(prod); would not be enough, setSourceProducts() is needed
                    setSourceProducts(new Product[]{prod});
                    OperatorUIUtils.initParamList(bandList, getBandNames());
                    OperatorUIUtils.initParamList(maskList, getGeometries());
                    pixelPanelChanged();
                    geoCodingChange();
                }
            }
        }
        String selectedFormat = (String) formatNameComboBox.getSelectedItem();
        if (selectedFormat != null && selectedFormat.equals(ANY_FORMAT)) {
            selectedFormat = null;
        }
        paramMap.put(FORMAT_PARAMETER, selectedFormat);
        paramMap.put(USE_ADVANCED_OPTIONS_PARAMETER, advancedOptionsPanel.isVisible());
        OperatorUIUtils.updateParamList(bandList, paramMap, BAND_LIST_PARAMETER);
        OperatorUIUtils.updateParamList(maskList, paramMap, MASK_LIST_PARAMETER);
        paramMap.remove(PIXEL_REGION_PARAMETER);
        paramMap.remove(GEOMETRY_REGION_PARAMETER);
        paramMap.remove(POLYGON_REGION_PARAMETER);
        final Rectangle pixelRegion = new Rectangle(((Number) pixelCoordXSpinner.getValue()).intValue(), ((Number) pixelCoordYSpinner.getValue()).intValue(), ((Number) pixelCoordWidthSpinner.getValue()).intValue(), ((Number) pixelCoordHeightSpinner.getValue()).intValue());
        if (pixelCoordRadio.isSelected()) {
            paramMap.put(PIXEL_REGION_PARAMETER, pixelRegion);
        }
        if (geoCoordRadio.isSelected()) {
            paramMap.put(GEOMETRY_REGION_PARAMETER, getGeometry());
        }
        if (vectorFileRadio.isSelected()) {
            paramMap.put(POLYGON_REGION_PARAMETER, getPolygon());
        }
        paramMap.put(COPY_METADATA_PARAMETER, copyMetadata.isSelected());
    }

    public void updateAdvancedOptionsUIAtProductChange() {
        if (sourceProductSelector != null) {
            advancedOptionsBtn.setEnabled(sourceProductSelector.getSelectedProduct() != null);

            final Product prod = sourceProductSelector.getSelectedProduct();
            if (prod != null && prod.getFileLocation() != null) {
                File currentProductFileLocation = (File) paramMap.get(FILE_PARAMETER);
                if (currentProductFileLocation == null || currentProductFileLocation != prod.getFileLocation()) {
                    // for same types of products (with identical band names) the selected bands/masks are kept, therefore clear the selection when input product changes
                    bandList.clearSelection();
                    maskList.clearSelection();

                    // reset default visible coords panel
                    pixelCoordRadio.setSelected(true);
                    pixelPanel.setVisible(true);
                    geoPanel.setVisible(false);
                    vectorFilePanel.setVisible(false);

                    // also reset pixel coords
                    pixelCoordXSpinner.setValue(0);
                    pixelCoordYSpinner.setValue(0);
                    pixelCoordWidthSpinner.setValue(Integer.MAX_VALUE);
                    pixelCoordHeightSpinner.setValue(Integer.MAX_VALUE);

                    // trigger the calculation of product bounds
                    pixelPanelChanged();
                    // sync geo coords
                    syncLatLonWithXYParams();
                    // reset subset polygon
                    resetProductSubsetByPolygon(prod);
                }
            }
        }
    }

    public void setSourceProduct(final Product product) {
        if (sourceProductSelector != null) {
            sourceProductSelector.setSelectedProduct(product);
            if (product != null && product.getFileLocation() != null) {
                paramMap.put(FILE_PARAMETER, product.getFileLocation());
            }
        }
    }

    private JComponent createPanel() {
        int gapBetweenRows = 10;
        int gapBetweenColumns = 10;

        initPixelCoordUIComponents();
        initGeoCoordUIComponents();

        createPixelPanel(gapBetweenColumns, gapBetweenRows);
        createGeoCodingPanel(gapBetweenColumns, gapBetweenRows);

        pixelCoordRadio.setSelected(true);
        pixelCoordRadio.setActionCommand("pixelCoordRadio");
        geoCoordRadio.setActionCommand("geoCoordRadio");
        vectorFileRadio.setActionCommand("vectorFileRadio");
        ButtonGroup group = new ButtonGroup();
        group.add(pixelCoordRadio);
        group.add(geoCoordRadio);
        group.add(vectorFileRadio);
        advancedOptionsBtn.addActionListener(e -> {
            advancedOptionsPanel.setVisible(!advancedOptionsPanel.isVisible());
            advancedOptionsBtn.setText(advancedOptionsPanel.isVisible() ? "Without Advanced options" : "Advanced options");
        });
        advancedOptionsBtn.setEnabled(sourceProductSelector.getSelectedProduct() != null);
        pixelCoordRadio.addActionListener(e -> {
            pixelPanel.setVisible(true);
            geoPanel.setVisible(false);
            vectorFilePanel.setVisible(false);
        });
        geoCoordRadio.addActionListener(e -> {
            pixelPanel.setVisible(false);
            geoPanel.setVisible(true);
            vectorFilePanel.setVisible(false);
        });
        vectorFileRadio.addActionListener(e -> {
            pixelPanel.setVisible(false);
            geoPanel.setVisible(false);
            vectorFilePanel.setVisible(true);
        });

        JPanel contentPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, 0, 0);
        JPanel productSelectionPanel = sourceProductSelector.createDefaultPanel();
        productSelectionPanel.setMinimumSize(new Dimension(600, 70));
        productSelectionPanel.setPreferredSize(new Dimension(600, 70));
        contentPanel.add(productSelectionPanel, gbc);
        gbc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        contentPanel.add(new JLabel("Data Format:"), gbc);
        gbc = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        formatNameComboBox.setToolTipText("Select 'Any Format' to let SNAP decide");
        contentPanel.add(formatNameComboBox, gbc);
        gbc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(advancedOptionsBtn, gbc);
        gbc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.BOTH, GridBagConstraints.WEST, 2, 1, 0, 0);
        advancedOptionsPanel.setVisible(false);
        contentPanel.add(advancedOptionsPanel, gbc);
        gbc = SwingUtils.buildConstraints(0, 4, GridBagConstraints.BOTH, GridBagConstraints.WEST, 2, 1, 0, 0);
        contentPanel.add(new JPanel(), gbc);

        gbc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        advancedOptionsPanel.add(new JLabel("Source Bands:"), gbc);
        gbc = SwingUtils.buildConstraints(1, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        advancedOptionsPanel.add(new JScrollPane(bandList), gbc);

        gbc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        advancedOptionsPanel.add(copyMetadata, gbc);
        gbc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        advancedOptionsPanel.add(new JLabel("Source Masks:"), gbc);
        gbc = SwingUtils.buildConstraints(1, 2, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        advancedOptionsPanel.add(new JScrollPane(maskList), gbc);

        JPanel regionTypePanel = new JPanel(new GridLayout(1, 3));
        regionTypePanel.add(pixelCoordRadio);
        regionTypePanel.add(geoCoordRadio);
        regionTypePanel.add(vectorFileRadio);

        gbc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 2, 1, gapBetweenRows, 0);
        advancedOptionsPanel.add(regionTypePanel, gbc);

        gbc = SwingUtils.buildConstraints(0, 4, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 2, 1, gapBetweenRows, 0);
        advancedOptionsPanel.add(pixelPanel, gbc);

        gbc = SwingUtils.buildConstraints(0, 5, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 2, 1, gapBetweenRows, 0);
        advancedOptionsPanel.add(geoPanel, gbc);
        geoPanel.setVisible(false);

        gbc = SwingUtils.buildConstraints(0, 6, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 2, 1, gapBetweenRows, 0);
        advancedOptionsPanel.add(vectorFilePanel, gbc);
        vectorFilePanel.setVisible(false);

        return contentPanel;
    }

    private void initPixelCoordUIComponents() {
        pixelCoordXSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 25));
        pixelCoordXSpinner.getModel().setValue(MIN_SCENE_VALUE);
        pixelCoordXSpinner.setToolTipText("Start X co-ordinate given in pixels");
        pixelCoordXSpinner.addChangeListener(this::updateUIStatePixelCoordsChanged);

        pixelCoordYSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 25));
        pixelCoordYSpinner.getModel().setValue(MIN_SCENE_VALUE);
        pixelCoordYSpinner.setToolTipText("Start Y co-ordinate given in pixels");
        pixelCoordYSpinner.addChangeListener(this::updateUIStatePixelCoordsChanged);

        pixelCoordWidthSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 25));
        pixelCoordWidthSpinner.getModel().setValue(Integer.MAX_VALUE);
        pixelCoordWidthSpinner.setToolTipText("Product width");
        pixelCoordWidthSpinner.addChangeListener(this::updateUIStatePixelCoordsChanged);

        pixelCoordHeightSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 25));
        pixelCoordHeightSpinner.getModel().setValue(Integer.MAX_VALUE);
        pixelCoordHeightSpinner.setToolTipText("Product height");
        pixelCoordHeightSpinner.addChangeListener(this::updateUIStatePixelCoordsChanged);
    }

    private void initGeoCoordUIComponents() {
        geoCoordNorthLatSpinner = new JSpinner(new SpinnerNumberModel(0.0, -90.0, 90.0, 1.0));
        geoCoordNorthLatSpinner.getModel().setValue(90.0);
        geoCoordNorthLatSpinner.setToolTipText("North bound latitude (°)");
        geoCoordNorthLatSpinner.addChangeListener(this::updateUIStateGeoCoordsChanged);

        geoCoordWestLongSpinner = new JSpinner(new SpinnerNumberModel(0.0, -180.0, 180.0, 1.0));
        geoCoordWestLongSpinner.getModel().setValue(-180.0);
        geoCoordWestLongSpinner.setToolTipText("West bound longitude (°)");
        geoCoordWestLongSpinner.addChangeListener(this::updateUIStateGeoCoordsChanged);

        geoCoordSouthLatSpinner = new JSpinner(new SpinnerNumberModel(0.0, -90.0, 90.0, 1.0));
        geoCoordSouthLatSpinner.getModel().setValue(-90.0);
        geoCoordSouthLatSpinner.setToolTipText("South bound latitude (°)");
        geoCoordSouthLatSpinner.addChangeListener(this::updateUIStateGeoCoordsChanged);

        geoCoordEastLongSpinner = new JSpinner(new SpinnerNumberModel(0.0, -180.0, 180.0, 1.0));
        geoCoordEastLongSpinner.getModel().setValue(180.0);
        geoCoordEastLongSpinner.setToolTipText("East bound longitude (°)");
        geoCoordEastLongSpinner.addChangeListener(this::updateUIStateGeoCoordsChanged);
    }

    private void createPixelPanel(int gapBetweenColumns, int gapBetweenRows) {
        GridBagConstraints pixgbc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, 0, 0);
        pixelPanel.add(new JLabel("SceneX:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, gapBetweenColumns);
        pixelPanel.add(pixelCoordXSpinner, pixgbc);

        pixgbc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        pixelPanel.add(new JLabel("SceneY:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        pixelPanel.add(pixelCoordYSpinner, pixgbc);

        pixgbc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        pixelPanel.add(new JLabel("Scene width:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        pixelPanel.add(pixelCoordWidthSpinner, pixgbc);

        pixgbc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        pixelPanel.add(new JLabel("Scene height:"), pixgbc);
        pixgbc = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        pixelPanel.add(pixelCoordHeightSpinner, pixgbc);
    }

    private void createGeoCodingPanel(int gapBetweenColumns, int gapBetweenRows) {
        GridBagConstraints geobc = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, 0, 0);
        geoPanel.add(new JLabel("North latitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, gapBetweenColumns);
        geoPanel.add(geoCoordNorthLatSpinner, geobc);

        geobc = SwingUtils.buildConstraints(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        geoPanel.add(new JLabel("West longitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        geoPanel.add(geoCoordWestLongSpinner, geobc);

        geobc = SwingUtils.buildConstraints(0, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        geoPanel.add(new JLabel("South latitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        geoPanel.add(geoCoordSouthLatSpinner, geobc);

        geobc = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, 0);
        geoPanel.add(new JLabel("East longitude bound:"), geobc);
        geobc = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, gapBetweenRows, gapBetweenColumns);
        geoPanel.add(geoCoordEastLongSpinner, geobc);
    }

    private void updateUIStatePixelCoordsChanged(ChangeEvent event) {
        if (updatingUI.compareAndSet(false, true)) {
            try {
                if (event != null && pixelCoordRadio.isEnabled()) {
                    pixelPanelChanged();
                    syncLatLonWithXYParams();
                }
            } finally {
                updatingUI.set(false);
            }
        }
    }

    private void updateUIStateGeoCoordsChanged(ChangeEvent event) {
        if (updatingUI.compareAndSet(false, true)) {
            try {
                if (event != null && geoCoordRadio.isEnabled()) {
                    geoCodingChange();
                }
            } finally {
                updatingUI.set(false);
            }
        }
    }

    private void pixelPanelChanged() {
        int productWidth = 0;
        int productHeight = 0;
        if (sourceProductSelector != null) {
            final Product prod = sourceProductSelector.getSelectedProduct();
            productWidth = prod.getSceneRasterWidth();
            productHeight = prod.getSceneRasterHeight();
        }

        int x1 = ((Number) pixelCoordXSpinner.getValue()).intValue();
        int y1 = ((Number) pixelCoordYSpinner.getValue()).intValue();
        int w = ((Number) pixelCoordWidthSpinner.getValue()).intValue();
        int h = ((Number) pixelCoordHeightSpinner.getValue()).intValue();

        if (x1 < 0) {
            x1 = 0;
        }
        if (x1 > productWidth - 2) {
            x1 = productWidth - 2;
        }
        if (y1 < 0) {
            y1 = 0;
        }
        if (y1 > productHeight - 2) {
            y1 = productHeight - 2;
        }

        if (w > productWidth) {
            w = productWidth;
        }
        if (x1 + w > productWidth) {
            if ((w - x1) >= 2) {
                w = w - x1;
            } else {
                w = productWidth - x1;
            }
        }

        if (h > productHeight) {
            h = productHeight;
        }
        if (y1 + h > productHeight) {
            if (h - y1 >= 2) {
                h = h - y1;
            } else {
                h = productHeight - y1;
            }

        }

        //reset fields values when the user writes wrong values
        pixelCoordXSpinner.setValue(0);
        pixelCoordYSpinner.setValue(0);
        pixelCoordWidthSpinner.setValue(w);
        pixelCoordHeightSpinner.setValue(h);

        pixelCoordXSpinner.setValue(x1);
        pixelCoordYSpinner.setValue(y1);
        pixelCoordWidthSpinner.setValue(w);
        pixelCoordHeightSpinner.setValue(h);
    }

    private void geoCodingChange() {
        final GeoPos geoPos1 = new GeoPos((Double) geoCoordNorthLatSpinner.getValue(),
                (Double) geoCoordWestLongSpinner.getValue());
        final GeoPos geoPos2 = new GeoPos((Double) geoCoordSouthLatSpinner.getValue(),
                (Double) geoCoordEastLongSpinner.getValue());

        updateXYParams(geoPos1, geoPos2);
    }

    private void syncLatLonWithXYParams() {
        GeoCoding geoCoding = null;
        if (sourceProductSelector != null) {
            final Product prod = sourceProductSelector.getSelectedProduct();
            geoCoding = prod.getSceneGeoCoding();
        }
        if (geoCoding != null) {
            final PixelPos pixelPos1 = new PixelPos((Integer) pixelCoordXSpinner.getValue(), (Integer) pixelCoordYSpinner.getValue());
            int paramX2 = (Integer) pixelCoordWidthSpinner.getValue() + (Integer) pixelCoordXSpinner.getValue() - 1;
            int paramY2 = (Integer) pixelCoordHeightSpinner.getValue() + (Integer) pixelCoordYSpinner.getValue() - 1;
            final PixelPos pixelPos2 = new PixelPos(paramX2, paramY2);

            final GeoPos geoPos1 = geoCoding.getGeoPos(pixelPos1, null);
            final GeoPos geoPos2 = geoCoding.getGeoPos(pixelPos2, null);
            if (geoPos1.isValid()) {
                double lat = geoPos1.getLat();
                lat = MathUtils.crop(lat, -90.0, 90.0);
                geoCoordNorthLatSpinner.setValue(lat);
                double lon = geoPos1.getLon();
                lon = MathUtils.crop(lon, -180.0, 180.0);
                geoCoordWestLongSpinner.setValue(lon);
            }
            if (geoPos2.isValid()) {
                double lat = geoPos2.getLat();
                lat = MathUtils.crop(lat, -90.0, 90.0);
                geoCoordSouthLatSpinner.setValue(lat);
                double lon = geoPos2.getLon();
                lon = MathUtils.crop(lon, -180.0, 180.0);
                geoCoordEastLongSpinner.setValue(lon);
            }
        }
    }

    private void updateXYParams(GeoPos geoPos1, GeoPos geoPos2) {
        GeoCoding geoCoding = null;
        int productWidth = 0;
        int productHeight = 0;
        if (sourceProductSelector != null) {
            final Product prod = sourceProductSelector.getSelectedProduct();
            geoCoding = prod.getSceneGeoCoding();
            productWidth = prod.getSceneRasterWidth();
            productHeight = prod.getSceneRasterHeight();
        }
        if (geoCoding != null) {
            final PixelPos pixelPos1 = geoCoding.getPixelPos(geoPos1, null);
            if (!pixelPos1.isValid()) {
                pixelPos1.setLocation(0, 0);
            }
            final PixelPos pixelPos2 = geoCoding.getPixelPos(geoPos2, null);
            if (!pixelPos2.isValid()) {
                pixelPos2.setLocation(productWidth, productHeight);
            }

            final Rectangle.Float region = new Rectangle.Float();
            region.setFrameFromDiagonal(pixelPos1.x, pixelPos1.y, pixelPos2.x, pixelPos2.y);
            final Rectangle.Float productBounds;

            productBounds = new Rectangle.Float(0, 0, productWidth, productHeight);

            Rectangle2D finalRegion = productBounds.createIntersection(region);

            if (isValueInNumericSpinnerRange(pixelCoordXSpinner, (int) finalRegion.getMinX())) {
                pixelCoordXSpinner.setValue((int) finalRegion.getMinX());
            }
            if (isValueInNumericSpinnerRange(pixelCoordYSpinner, (int) finalRegion.getMinY())) {
                pixelCoordYSpinner.setValue((int) finalRegion.getMinY());
            }
            int width = (int) (finalRegion.getMaxX() - finalRegion.getMinX()) + 1;
            int height = (int) (finalRegion.getMaxY() - finalRegion.getMinY()) + 1;
            if (isValueInNumericSpinnerRange(pixelCoordWidthSpinner, width)) {
                pixelCoordWidthSpinner.setValue(width);
            }
            if (isValueInNumericSpinnerRange(pixelCoordHeightSpinner, height)) {
                pixelCoordHeightSpinner.setValue(height);
            }
        }
    }

    private boolean isValueInNumericSpinnerRange(JSpinner spinner, Integer value) {
        final Integer min = (Integer) ((SpinnerNumberModel) spinner.getModel()).getMinimum();
        final Integer max = (Integer) ((SpinnerNumberModel) spinner.getModel()).getMaximum();
        return value >= min && value <= max;
    }

    private Geometry getGeometry() {
        GeoCoding geoCoding = null;
        int productWidth = 0;
        int productHeight = 0;
        if (sourceProductSelector != null) {
            final Product prod = sourceProductSelector.getSelectedProduct();
            geoCoding = prod.getSceneGeoCoding();
            productWidth = prod.getSceneRasterWidth();
            productHeight = prod.getSceneRasterHeight();
        }
        if (geoCoding != null) {
            final GeoPos geoPos1 = new GeoPos((Double) geoCoordNorthLatSpinner.getValue(), (Double) geoCoordWestLongSpinner.getValue());
            final GeoPos geoPos2 = new GeoPos((Double) geoCoordSouthLatSpinner.getValue(), (Double) geoCoordEastLongSpinner.getValue());
            final PixelPos pixelPos1 = geoCoding.getPixelPos(geoPos1, null);
            final PixelPos pixelPos2 = geoCoding.getPixelPos(geoPos2, null);

            final Rectangle.Float region = new Rectangle.Float();
            region.setFrameFromDiagonal(pixelPos1.x, pixelPos1.y, pixelPos2.x, pixelPos2.y);
            final Rectangle.Float productBounds = new Rectangle.Float(0, 0, productWidth, productHeight);
            Rectangle2D finalRegion = productBounds.createIntersection(region);
            Rectangle bounds = new Rectangle((int) finalRegion.getMinX(), (int) finalRegion.getMinY(), (int) (finalRegion.getMaxX() - finalRegion.getMinX()) + 1, (int) (finalRegion.getMaxY() - finalRegion.getMinY()) + 1);
            return GeoUtils.computeGeometryUsingPixelRegion(geoCoding, bounds);
        }
        return null;
    }

    private org.locationtech.jts.geom.Polygon getPolygon() {
        if (productSubsetByPolygonUiComponents.getProductSubsetByPolygon().isLoaded()) {
            return productSubsetByPolygonUiComponents.getProductSubsetByPolygon().getSubsetGeoPolygon();
        }
        return null;
    }

    private void resetProductSubsetByPolygon(Product product){
        final MetadataInspector.Metadata productMetadata = new MetadataInspector.Metadata(product.getSceneRasterWidth(), product.getSceneRasterHeight());
        productMetadata.setGeoCoding(product.getSceneGeoCoding());
        productSubsetByPolygonUiComponents.setTargetProductMetadata(productMetadata);
    }

    private class SourceSelectionChangeListener implements SelectionChangeListener {

        public void selectionChanged(SelectionChangeEvent event) {
            final Object selected = event.getSelection().getSelectedValue();
            if (selected instanceof Product) {
                Product product = (Product) selected;
                if (product.getFileLocation() != null) {
                    updateFormatNamesCombo(product.getFileLocation());
                }
            }
            updateAdvancedOptionsUIAtProductChange();
            updateParameters();
        }

        public void selectionContextChanged(SelectionChangeEvent event) {
            //nothing to do
        }
    }

}
