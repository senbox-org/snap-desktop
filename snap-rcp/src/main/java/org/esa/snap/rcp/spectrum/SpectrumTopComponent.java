/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.spectrum;

import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.help.HelpAction;
import org.esa.snap.rcp.placemark.ProductChooser;
import org.esa.snap.rcp.statistics.XYPlotMarker;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.*;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.spectrum.*;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.jfree.chart.*;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jspecify.annotations.NonNull;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.List;
import java.util.*;

@TopComponent.Description(preferredID = "SpectrumTopComponent", iconBase = "org/esa/snap/rcp/icons/Spectrum.gif")
@TopComponent.Registration(mode = "Spectrum", openAtStartup = false, position = 80)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.SpectrumTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Optical", position = 0),
        @ActionReference(path = "Menu/View/Tool Windows/Optical"),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(displayName = "#CTL_SpectrumTopComponent_Name", preferredID = "SpectrumTopComponent")
@NbBundle.Messages({"CTL_SpectrumTopComponent_Name=Spectrum View", "CTL_SpectrumTopComponent_HelpId=showSpectrumWnd"})
/*
 * A window which displays spectra at selected pixel positions.
 */
public class SpectrumTopComponent extends ToolTopComponent {

    public static final String ID = SpectrumTopComponent.class.getName();

    private static final String SUPPRESS_MESSAGE_KEY = "plugin.spectrum.tip";
    private static final float EPSILON_FOR_PRODUCT_COMPATIBILITY_TEST = 1e-8f;

    private boolean wasOpenedBefore = false;

    private final Map<Product, DisplayableSpectrum[]> productToSpectraMap;
    private final Map<Product, List<SpectrumBand>> productToSpectralBandsMap;

    private final ProductNodeListenerAdapter productNodeHandler;
    private final PinSelectionChangeListener pinSelectionChangeListener;
    private final PixelPositionListener pixelPositionListener;
    private final BandGroupsManager bandGroupsManager;

    private AbstractButton filterButton;
    private AbstractButton showSpectrumForCursorButton;
    private AbstractButton showSpectraForSelectedPinsButton;
    private AbstractButton showSpectraForAllPinsButton;
    private AbstractButton showSecondaryProductSpectrumButton;
    private AbstractButton showGridButton;

    private boolean tipShown;
    private ProductSceneView currentView;
    private Product currentProduct;
    private final List<Product> secondaryProducts = new ArrayList<>();
    private ChartPanel chartPanel;
    private ChartHandler chartHandler;

    private boolean domainAxisAdjustmentIsFrozen;
    private boolean rangeAxisAdjustmentIsFrozen;
    private boolean isCodeInducedAxisChange;
    private boolean automaticAdjustment;
    private ProductManager productManager;
    private boolean pixelPosNotAvailable;

    public SpectrumTopComponent() {
        productNodeHandler = new ProductNodeHandler(this);
        pinSelectionChangeListener = new PinSelectionChangeListener(this);
        productToSpectraMap = new HashMap<>();
        productToSpectralBandsMap = new HashMap<>();
        pixelPositionListener = new CursorSpectrumPixelPositionListener(this);
        bandGroupsManager = getBandGroupsManager();

        initUI();
    }

    private BandGroupsManager getBandGroupsManager() {
        final BandGroupsManager bandGroupsManager;
        try {
            bandGroupsManager = BandGroupsManager.getInstance();
        } catch (IOException e) {
            Dialogs.showError(e.getMessage());
            throw new RuntimeException(e);
        }
        return bandGroupsManager;
    }

    //package local for testing
    static DisplayableSpectrum[] createSpectraFromUngroupedBands(SpectrumBand[] ungroupedBands, int symbolIndex, int strokeIndex) {
        List<String> knownUnits = new ArrayList<>();
        List<DisplayableSpectrum> displayableSpectrumList = new ArrayList<>();
        DisplayableSpectrum defaultSpectrum = new DisplayableSpectrum("tbd", -1);
        for (SpectrumBand ungroupedBand : ungroupedBands) {
            final String unit = ungroupedBand.getOriginalBand().getUnit();
            if (StringUtils.isNullOrEmpty(unit)) {
                defaultSpectrum.addBand(ungroupedBand);
            } else if (knownUnits.contains(unit)) {
                displayableSpectrumList.get(knownUnits.indexOf(unit)).addBand(ungroupedBand);
            } else {
                knownUnits.add(unit);
                final DisplayableSpectrum spectrum = new DisplayableSpectrum("Bands measured in " + unit, symbolIndex);
                symbolIndex++;
                spectrum.setLineStyle(SpectrumStrokeProvider.getStroke(strokeIndex));
                strokeIndex++;
                spectrum.addBand(ungroupedBand);
                displayableSpectrumList.add(spectrum);
            }
        }
        if (strokeIndex == 0) {
            defaultSpectrum.setName(DisplayableSpectrum.DEFAULT_SPECTRUM_NAME);
        } else {
            defaultSpectrum.setName(DisplayableSpectrum.REMAINING_BANDS_NAME);
        }
        defaultSpectrum.setSymbolIndex(symbolIndex);
        defaultSpectrum.setLineStyle(SpectrumStrokeProvider.getStroke(strokeIndex));
        displayableSpectrumList.add(defaultSpectrum);
        return displayableSpectrumList.toArray(new DisplayableSpectrum[0]);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_SpectrumTopComponent_HelpId());
    }

    private void setCurrentView(ProductSceneView view) {
        ProductSceneView oldView = currentView;
        currentView = view;
        if (oldView != currentView) {
            if (oldView != null) {
                oldView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionChangeListener);
            }
            if (currentView != null) {
                currentView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionChangeListener);
                setCurrentProduct(currentView.getProduct());
                if (!productToSpectraMap.containsKey(currentProduct)) {
                    setUpSpectra();
                }
                recreateChart();

            }
            updateUIState();
        }
    }

    Product getCurrentProduct() {
        return currentProduct;
    }

    ProductSceneView getCurrentView() {
        return currentView;
    }

    List<Product> getSecondaryProducts() {
        return secondaryProducts;
    }

    Map<Product, DisplayableSpectrum[]> getProductToSpectraMap() {
        return productToSpectraMap;
    }

    ChartHandler getChartHandler() {
        return chartHandler;
    }

    boolean isAutomaticAdjustment() {
        return automaticAdjustment;
    }

    void setAutomaticAdjustment(boolean automaticAdjustment) {
        this.automaticAdjustment = automaticAdjustment;
    }

    void setCodeInducedAxisChange(boolean codeInducedAxisChange) {
        isCodeInducedAxisChange = codeInducedAxisChange;
    }

    boolean isDomainAxisAdjustmentIsFrozen() {
        return domainAxisAdjustmentIsFrozen;
    }

    void setDomainAxisAdjustmentIsFrozen(boolean domainAxisAdjustmentIsFrozen) {
        this.domainAxisAdjustmentIsFrozen = domainAxisAdjustmentIsFrozen;
    }

    boolean isRangeAxisAdjustmentIsFrozen() {
        return rangeAxisAdjustmentIsFrozen;
    }

    void setRangeAxisAdjustmentIsFrozen(boolean rangeAxisAdjustmentIsFrozen) {
        this.rangeAxisAdjustmentIsFrozen = rangeAxisAdjustmentIsFrozen;
    }

    private void setCurrentProduct(Product product) {
        if (currentProduct != product) {
            Product oldProduct = currentProduct;
            currentProduct = product;
            if (oldProduct != null) {
                oldProduct.removeProductNodeListener(productNodeHandler);
            }
            if (currentProduct != null) {
                currentProduct.addProductNodeListener(productNodeHandler);
            }
            if (currentProduct == null) {
                chartHandler.setEmptyPlot();
            }
            updateUIState();
        }
    }

    void updateUIState() {
        boolean hasView = currentView != null;
        boolean hasProduct = currentProduct != null;
        boolean hasSelectedPins = hasView && currentView.getSelectedPins().length > 0;
        boolean hasPins = hasProduct && currentProduct.getPinGroup().getNodeCount() > 0;
        filterButton.setEnabled(hasProduct);
        showSpectrumForCursorButton.setEnabled(hasView);
        showSpectraForSelectedPinsButton.setEnabled(hasSelectedPins);
        showSpectraForAllPinsButton.setEnabled(hasPins);
        showGridButton.setEnabled(hasView);
        chartPanel.setEnabled(hasProduct);    // todo - hasSpectraGraphs
        showGridButton.setSelected(hasView);
        chartHandler.setGridVisible(showGridButton.isSelected());
        updateSecondaryProductButtonState();
    }

    private void updateSecondaryProductButtonState() {
        if (currentProduct == null) {
            secondaryProducts.clear();
            showSecondaryProductSpectrumButton.setEnabled(false);
            return;
        }
        Product[] products = productManager.getProducts();
        for (Product product : products) {
            if (currentProduct == product) {
                continue;
            }
            if (currentProduct.isCompatibleProduct(product, EPSILON_FOR_PRODUCT_COMPATIBILITY_TEST)) {
                showSecondaryProductSpectrumButton.setEnabled(true);
                return;
            }
        }
        showSecondaryProductSpectrumButton.setEnabled(false);
    }

    void setPrepareForUpdateMessage() {
        chartHandler.setCollectingSpectralInformationMessage();
    }

    void clearPrepareForUpdateMessage() {
        chartHandler.setPlotMessage("");
    }

    void updateData(int pixelX, int pixelY, int level, boolean pixelPosInRasterBounds) {
        chartHandler.setPosition(pixelX, pixelY, level, pixelPosInRasterBounds);
        chartHandler.updateData();
    }

    void updateChart(boolean adjustAxes) {
        chartHandler.setAutomaticRangeAdjustments(adjustAxes);
        updateChart();
    }

    void updateChart() {
        maybeShowTip();
        chartHandler.updateChart();
        chartPanel.repaint();
    }

    private void maybeShowTip() {
        if (!tipShown) {
            final String message = "<html>Tip: If you press the SHIFT key while moving the mouse cursor over<br/>" +
                    "an image, " + SnapApp.getDefault().getInstanceName() + " adjusts the diagram axes " +
                    "to the local values at the<br/>" +
                    "current pixel position, if you release the SHIFT key again, then the<br/>" +
                    "min/max are accumulated again.</html>";
            Dialogs.showInformation("Spectrum Tip", message, SUPPRESS_MESSAGE_KEY);
            tipShown = true;
        }
    }

    private SpectrumBand[] getAvailableSpectralBands(Product product) {
        System.out.println("getAvailableSpectralBands for productNr: " + product.getRefNo());
        if (product == null) {
            return new SpectrumBand[0];
        }

        if (!productToSpectralBandsMap.containsKey(product)) {
            productToSpectralBandsMap.put(product, new ArrayList<>());
        }

        List<SpectrumBand> spectrumBands = productToSpectralBandsMap.get(product);
        Band[] bands = product.getBands();
        for (Band band : bands) {
            if (isSpectralBand(band) && !band.isFlagBand()) {
                boolean isAlreadyIncluded = false;
                for (SpectrumBand spectrumBand : spectrumBands) {
                    if (spectrumBand.getOriginalBand() == band) {
                        isAlreadyIncluded = true;
                        break;
                    }
                }
                if (!isAlreadyIncluded) {
                    spectrumBands.add(new SpectrumBand(band, true));
                }
            }
        }
        return spectrumBands.toArray(new SpectrumBand[0]);
    }

    static boolean isSpectralBand(@NonNull Band band) {
        return band.getSpectralWavelength() > 0.0;
    }

    private void initUI() {
        final JFreeChart chart = ChartFactory.createXYLineChart(Bundle.CTL_SpectrumTopComponent_Name(),
                "Wavelength (nm)", "", null, PlotOrientation.VERTICAL,
                true, true, false);
        chart.getXYPlot().getRangeAxis().addChangeListener(axisChangeEvent -> {
            if (!isCodeInducedAxisChange) {
                setRangeAxisAdjustmentIsFrozen(!((ValueAxis) axisChangeEvent.getAxis()).isAutoRange());
            }
        });
        chart.getXYPlot().getDomainAxis().addChangeListener(axisChangeEvent -> {
            if (!isCodeInducedAxisChange) {
                setDomainAxisAdjustmentIsFrozen(!((ValueAxis) axisChangeEvent.getAxis()).isAutoRange());
            }
        });
        chart.getXYPlot().getRangeAxis().setAutoRange(false);
        setRangeAxisAdjustmentIsFrozen(false);
        chart.getXYPlot().getDomainAxis().setAutoRange(false);
        setDomainAxisAdjustmentIsFrozen(false);
        chartPanel = new ChartPanel(chart);
        chartHandler = new ChartHandler(this, chart);
        final XYPlotMarker plotMarker = new XYPlotMarker(chartPanel, new XYPlotMarker.Listener() {
            @Override
            public void pointSelected(XYDataset xyDataset, int seriesIndex, Point2D dataPoint) {
                //do nothing
            }

            @Override
            public void pointDeselected() {
                //do nothing
            }
        });

        filterButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Filter24.gif"), false);
        filterButton.setName("filterButton");
        filterButton.setEnabled(false);
        filterButton.addActionListener(e -> {
            selectSpectralBands();
            recreateChart();
        });

        showSpectrumForCursorButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/CursorSpectrum24.gif"), true);
        showSpectrumForCursorButton.addActionListener(e -> recreateChart());
        showSpectrumForCursorButton.setName("showSpectrumForCursorButton");
        showSpectrumForCursorButton.setSelected(true);
        showSpectrumForCursorButton.setToolTipText("Show spectrum at cursor position.");

        showSpectraForSelectedPinsButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/SelectedPinSpectra24.gif"), true);
        showSpectraForSelectedPinsButton.addActionListener(e -> {
            if (isShowingSpectraForAllPins()) {
                showSpectraForAllPinsButton.setSelected(false);
            } else if (!isShowingSpectraForSelectedPins()) {
                plotMarker.setInvisible();
            }
            recreateChart();
        });
        showSpectraForSelectedPinsButton.setName("showSpectraForSelectedPinsButton");
        showSpectraForSelectedPinsButton.setToolTipText("Show spectra for selected pins.");

        showSpectraForAllPinsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/PinSpectra24.gif"),
                true);
        showSpectraForAllPinsButton.addActionListener(e -> {
            if (isShowingSpectraForSelectedPins()) {
                showSpectraForSelectedPinsButton.setSelected(false);
            } else if (!isShowingSpectraForAllPins()) {
                plotMarker.setInvisible();
            }
            recreateChart();
        });
        showSpectraForAllPinsButton.setName("showSpectraForAllPinsButton");
        showSpectraForAllPinsButton.setToolTipText("Show spectra for all pins.");

        showGridButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/SpectrumGrid24.gif"), true);
        showGridButton.addActionListener(e -> chartHandler.setGridVisible(showGridButton.isSelected()));
        showGridButton.setName("showGridButton");
        showGridButton.setToolTipText("Show diagram grid.");

        showSecondaryProductSpectrumButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/SelectSecondaryProductSpectra24.gif"), true);
        showSecondaryProductSpectrumButton.addActionListener(e -> selectSecondaryProduct());
        showSecondaryProductSpectrumButton.setName("showSecondaryProductSpectrumButton");
        showSecondaryProductSpectrumButton.setSelected(false);
        showSecondaryProductSpectrumButton.setEnabled(false);
        showSecondaryProductSpectrumButton.setToolTipText("Show spectrum at cursor position of secondary product.");

        AbstractButton exportSpectraButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Export24.gif"),
                false);
        exportSpectraButton.addActionListener(new SpectraExportAction(this));
        exportSpectraButton.setToolTipText("Export spectra to text file.");
        exportSpectraButton.setName("exportSpectraButton");

        AbstractButton helpButton = ToolButtonFactory.createButton(new HelpAction(this), false);
        helpButton.setName("helpButton");
        helpButton.setToolTipText("Help.");


        final JPanel buttonPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 2;
        gbc.gridy = 0;
        buttonPane.add(filterButton, gbc);
        gbc.gridy++;
        buttonPane.add(showSpectrumForCursorButton, gbc);
        gbc.gridy++;
        buttonPane.add(showSpectraForSelectedPinsButton, gbc);
        gbc.gridy++;
        buttonPane.add(showSpectraForAllPinsButton, gbc);
        gbc.gridy++;
        buttonPane.add(showGridButton, gbc);
        gbc.gridy++;
        buttonPane.add(showSecondaryProductSpectrumButton, gbc);
        gbc.gridy++;
        buttonPane.add(exportSpectraButton, gbc);

        gbc.gridy++;
        gbc.insets.bottom = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.EAST;
        buttonPane.add(helpButton, gbc);

        chartPanel.setPreferredSize(new Dimension(300, 200));
        chartPanel.setBackground(Color.white);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        chartPanel.addChartMouseListener(plotMarker);

        JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPane.add(BorderLayout.CENTER, chartPanel);
        mainPane.add(BorderLayout.EAST, buttonPane);
        mainPane.setPreferredSize(new Dimension(320, 200));

        productManager = SnapApp.getDefault().getProductManager();
        productManager.addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
                updateUIState();
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                final Product removedProduct = event.getProduct();
                if (currentProduct == removedProduct) {
                    chartPanel.getChart().getXYPlot().setDataset(null);
                    setCurrentView(null);
                    setCurrentProduct(null);
                }
                productToSpectraMap.remove(removedProduct);
                productToSpectralBandsMap.remove(removedProduct);
                removedProduct.removeProductNodeListener(productNodeHandler);
                secondaryProducts.remove(removedProduct);
                PlacemarkGroup pinGroup = removedProduct.getPinGroup();
                for (int i = 0; i < pinGroup.getNodeCount(); i++) {
                    chartHandler.removePinInformation(pinGroup.get(i));
                }
                updateUIState();
            }
        });


        final ProductSceneView view = getSelectedProductSceneView();
        if (view != null) {
            productSceneViewSelected(view);
        }
        setDisplayName(Bundle.CTL_SpectrumTopComponent_Name());
        setLayout(new BorderLayout());
        add(mainPane, BorderLayout.CENTER);
        updateUIState();
    }

    private void selectSecondaryProduct() {
        if (currentProduct == null) {
            secondaryProducts.clear();
            return;
        }
        if (!showSecondaryProductSpectrumButton.isSelected()) {
            return;
        }
        final ArrayList<Product> compatibleProducts = new ArrayList<>();
        for (Product product : productManager.getProducts()) {
            if (currentProduct == product) {
                continue;
            }
            if (currentProduct.isCompatibleProduct(product, EPSILON_FOR_PRODUCT_COMPATIBILITY_TEST)) {
                compatibleProducts.add(product);
            }
        }
        int numCompProducts = compatibleProducts.size();
        Product[] selectedProducts = new Product[0];
        if (numCompProducts == 0) {
            // nothing to do
        } else if (numCompProducts == 1) {
            selectedProducts = compatibleProducts.toArray(new Product[0]);
        } else {
            final ProductChooser chooser = new ProductChooser(SnapApp.getDefault().getMainFrame(),
                    "Choose Secondary Product",
                    this.getHelpCtx().getHelpID(),
                    compatibleProducts.toArray(new Product[0]),
                    secondaryProducts.toArray(new Product[0]));
            int showValue = chooser.show();
            if (ModalDialog.ID_OK == showValue) {
                selectedProducts = chooser.getSelectedProducts();
            }
        }

        showSecondaryProductSpectrumButton.setSelected(selectedProducts.length > 0);
        secondaryProducts.clear();
        secondaryProducts.addAll(Arrays.asList(selectedProducts));
        for (Product selectedProduct : selectedProducts) {
            selectedProduct.addProductNodeListener(productNodeHandler);
        }
        setUpSpectra();
        recreateChart();
    }

    private void selectSpectralBands() {
//        selectSpectralBands(currentProduct);
        final List<Product> spectraToBeDisplayed = new ArrayList<>();
        spectraToBeDisplayed.add(currentProduct);
        spectraToBeDisplayed.addAll(getSecondaryProducts());
        final List<DisplayableSpectrum> allSpectraList = new ArrayList<>();
        for (Product product : spectraToBeDisplayed) {
            allSpectraList.addAll(Arrays.asList(productToSpectraMap.get(product)));
        }
        final DisplayableSpectrum[] allSpectra = allSpectraList.toArray(new DisplayableSpectrum[0]);
        boolean alsoChooseColor = true;
        SpectrumChooser spectrumChooser = new SpectrumChooser(SwingUtilities.getWindowAncestor(this), allSpectra, alsoChooseColor);
        if (spectrumChooser.show() == AbstractDialog.ID_OK) {
            final DisplayableSpectrum[] spectra = spectrumChooser.getSpectra();
            final Map<Product, List<DisplayableSpectrum>> productSpectra = new HashMap<>();
            for (DisplayableSpectrum displayableSpectrum : spectra) {
                Product product = displayableSpectrum.getSpectralBands()[0].getProduct();
                if (!productSpectra.containsKey(product)) {
                    productSpectra.put(product, new ArrayList<>());
                }
                productSpectra.get(product).add(displayableSpectrum);
            }
            for (Product product : productSpectra.keySet()) {
                productToSpectraMap.put(product, productSpectra.get(product).toArray(new DisplayableSpectrum[0]));
            }
        }
    }

    boolean isShowingCursorSpectrum() {
        return showSpectrumForCursorButton.isSelected();
    }

    boolean showSecondaryProductSpectra() {
        return showSecondaryProductSpectrumButton.isSelected();
    }

    boolean isShowingPinSpectra() {
        return isShowingSpectraForSelectedPins() || isShowingSpectraForAllPins();
    }

    private boolean isShowingSpectraForAllPins() {
        return showSpectraForAllPinsButton.isSelected();
    }

    void recreateChart() {
        chartHandler.updateData();
        chartHandler.updateChart();
        chartPanel.repaint();
        updateUIState();
    }

    Placemark[] getDisplayedPins() {
        if (isShowingSpectraForSelectedPins() && currentView != null) {
            return currentView.getSelectedPins();
        } else if (isShowingSpectraForAllPins() && currentProduct != null) {
            ProductNodeGroup<Placemark> pinGroup = currentProduct.getPinGroup();
            return pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
        } else {
            return new Placemark[0];
        }
    }

    void setUpSpectra() {
        if (currentView == null) {
            return;
        }

        setUpSpectra(currentProduct);
        for (Product secondaryProduct : secondaryProducts) {
            setUpSpectra(secondaryProduct);
        }
    }

    private void setUpSpectra(Product product) {
        final SpectrumBand[] availableSpectralBands = getAvailableSpectralBands(product);
        if (availableSpectralBands.length == 0) {
            productToSpectraMap.put(product, new DisplayableSpectrum[0]);
            return;
        }
        final String namePrefix;
        if ( showSecondaryProductSpectra()) {
            namePrefix = "[" + product.getRefNo() + "]";
        } else {
            namePrefix = "";
        }
        int displayIndex = 0;
        final List<DisplayableSpectrum> spectra = new ArrayList<>();
        final BandGroup[] userBandGroups = bandGroupsManager.getGroupsMatchingProduct(product);
        if (userBandGroups.length > 0) {
            final DisplayableSpectrum[] userGroupingSpectra = new DisplayableSpectrum[userBandGroups.length];
            for (int i = 0; i < userBandGroups.length; i++) {
                final int symbolIndex = SpectrumShapeProvider.getValidIndex(displayIndex, false);
                ++displayIndex;
                final BandGroup userBandGroup = userBandGroups[i];
                final String spectrumName = namePrefix + userBandGroup.getName();
                final DisplayableSpectrum spectrum = new DisplayableSpectrum(spectrumName, symbolIndex);
                spectrum.setSelected(false);
                spectrum.setLineStyle(SpectrumStrokeProvider.getStroke(i));

                String[] bandNames = userBandGroup.getMatchingBandNames(product);
                for (final String bandName : bandNames) {
                    for (SpectrumBand availableSpectralBand : availableSpectralBands) {
                        if (availableSpectralBand.getName().equals(bandName)) {
                            spectrum.addBand(availableSpectralBand);
                        }
                    }
                }

                userGroupingSpectra[i] = spectrum;
            }

            spectra.addAll(Arrays.asList(userGroupingSpectra));
        }

        final RasterDataNode raster = currentView.getRaster();
        final BandGroup autoGrouping = product.getAutoGrouping();
        if (autoGrouping != null) {
            final int selectedSpectrumIndex = autoGrouping.indexOf(raster.getName());
            DisplayableSpectrum[] autoGroupingSpectra = new DisplayableSpectrum[autoGrouping.size()];
            final Iterator<String[]> iterator = autoGrouping.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                final String[] autoGroupingNameAsArray = iterator.next();
                StringBuilder spectrumNameBuilder = new StringBuilder(autoGroupingNameAsArray[0]);
                if (autoGroupingNameAsArray.length > 1) {
                    for (int j = 1; j < autoGroupingNameAsArray.length; j++) {
                        String autoGroupingNamePart = autoGroupingNameAsArray[j];
                        spectrumNameBuilder.append("_").append(autoGroupingNamePart);
                    }
                }
                final String spectrumName = spectrumNameBuilder.toString();
                int symbolIndex = SpectrumShapeProvider.getValidIndex(displayIndex, false);
                ++displayIndex;
                DisplayableSpectrum spectrum = new DisplayableSpectrum(namePrefix + spectrumName, symbolIndex);
                spectrum.setSelected(i == selectedSpectrumIndex);
                spectrum.setLineStyle(SpectrumStrokeProvider.getStroke(i));
                autoGroupingSpectra[i] = spectrum;
                i++;
            }
            List<SpectrumBand> ungroupedBandsList = new ArrayList<>();
            for (SpectrumBand availableSpectralBand : availableSpectralBands) {
                final String bandName = availableSpectralBand.getName();
                final int spectrumIndex = autoGrouping.indexOf(bandName);
                if (spectrumIndex != -1) {
                    autoGroupingSpectra[spectrumIndex].addBand(availableSpectralBand);
                } else {
                    ungroupedBandsList.add(availableSpectralBand);
                }
            }
            spectra.addAll(Arrays.asList(autoGroupingSpectra));
            if (!ungroupedBandsList.isEmpty()) {
                int validIndex = SpectrumShapeProvider.getValidIndex(displayIndex, false);
                ++displayIndex;
                final DisplayableSpectrum[] spectraFromUngroupedBands =
                        createSpectraFromUngroupedBands(ungroupedBandsList.toArray(new SpectrumBand[0]),
                                validIndex, i);
                spectra.addAll(Arrays.asList(spectraFromUngroupedBands));
            }
        } else {
            DisplayableSpectrum[] spectraFromUngroupedBands = createSpectraFromUngroupedBands(availableSpectralBands, 1, 0);
            spectra.addAll(Arrays.asList(spectraFromUngroupedBands));
        }
        productToSpectraMap.put(product, spectra.toArray(new DisplayableSpectrum[0]));
    }

    DisplayableSpectrum[] getAllSpectra() {
        if (currentView == null || !productToSpectraMap.containsKey(currentProduct)) {
            return new DisplayableSpectrum[0];
        }
        return productToSpectraMap.get(currentProduct);
    }

    private boolean isShowingSpectraForSelectedPins() {
        return showSpectraForSelectedPinsButton.isSelected();
    }

    List<DisplayableSpectrum> getSelectedSpectra() {
        List<DisplayableSpectrum> selectedSpectra = new ArrayList<>();
        if (currentView != null) {
            if (currentProduct != null && productToSpectraMap.containsKey(currentProduct)) {
                DisplayableSpectrum[] allSpectra = productToSpectraMap.get(currentProduct);
                for (DisplayableSpectrum displayableSpectrum : allSpectra) {
                    if (displayableSpectrum.isSelected()) {
                        selectedSpectra.add(displayableSpectrum);
                    }
                }
            }
            if (showSecondaryProductSpectra()) {
                for (Product product : secondaryProducts) {
                    if (productToSpectraMap.containsKey(product)) {
                        DisplayableSpectrum[] secondarySpectra = productToSpectraMap.get(product);
                        for (DisplayableSpectrum displayableSpectrum : secondarySpectra) {
                            if (displayableSpectrum.isSelected()) {
                                selectedSpectra.add(displayableSpectrum);
                            }
                        }
                    }
                }
            }
        }
        return selectedSpectra;
    }

    void updateSpectraUnits() {
        for (DisplayableSpectrum spectrum : getAllSpectra()) {
            spectrum.updateUnit();
        }
    }

    void removeCursorSpectraFromDataset() {
        chartHandler.removeCursorSpectraFromDataset();
    }

    @Override
    protected void productSceneViewSelected(ProductSceneView view) {
        view.addPixelPositionListener(pixelPositionListener);
        setCurrentView(view);
    }

    @Override
    protected void productSceneViewDeselected(ProductSceneView view) {
        view.removePixelPositionListener(pixelPositionListener);
        setCurrentView(null);
    }

    @Override
    protected void componentOpened() {
        if (this.wasOpenedBefore) {
            setUpSpectra();
        } else {
            this.wasOpenedBefore = true;
        }
        final ProductSceneView selectedProductSceneView = getSelectedProductSceneView();
        if (selectedProductSceneView != null) {
            selectedProductSceneView.addPixelPositionListener(pixelPositionListener);
            setCurrentView(selectedProductSceneView);
        }
    }

    @Override
    protected void componentClosed() {
        if (currentView != null) {
            currentView.removePixelPositionListener(pixelPositionListener);
        }
    }

    boolean showsValidCursorSpectra() {
        return chartHandler.showsValidCursorSpectra();
    }

    Map<Placemark, Map<Band, Double>> getPinToEnergies() {
        return chartHandler.getPinToEnergies();
    }

    public void setPixelPosNotAvailable(boolean pixelPosNotAvailable) {
        this.pixelPosNotAvailable = pixelPosNotAvailable;
    }

    public boolean isPixelPosNotAvailable() {
        return pixelPosNotAvailable;
    }
}
