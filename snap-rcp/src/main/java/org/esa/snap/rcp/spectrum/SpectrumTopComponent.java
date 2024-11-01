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

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelModel;
import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.help.HelpAction;
import org.esa.snap.rcp.placemark.PlacemarkUtils;
import org.esa.snap.rcp.statistics.XYPlotMarker;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.spectrum.*;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.jfree.chart.*;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.LineBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.locationtech.jts.geom.Point;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.media.jai.PlanarImage;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

    private final Map<RasterDataNode, DisplayableSpectrum[]> rasterToSpectraMap;
    private final Map<RasterDataNode, List<SpectrumBand>> rasterToSpectralBandsMap;

    private final ProductNodeListenerAdapter productNodeHandler;
    private final PinSelectionChangeListener pinSelectionChangeListener;
    private final PixelPositionListener pixelPositionListener;
    private final BandGroupsManager bandGroupsManager;

    private AbstractButton filterButton;
    private AbstractButton showSpectrumForCursorButton;
    private AbstractButton showSpectraForSelectedPinsButton;
    private AbstractButton showSpectraForAllPinsButton;
    private AbstractButton showGridButton;

    private boolean tipShown;
    private ProductSceneView currentView;
    private Product currentProduct;
    private ChartPanel chartPanel;
    private ChartHandler chartHandler;

    private boolean domainAxisAdjustmentIsFrozen;
    private boolean rangeAxisAdjustmentIsFrozen;
    private boolean isCodeInducedAxisChange;
    private boolean isUserInducedAutomaticAdjustmentChosen;

    public SpectrumTopComponent() {
        productNodeHandler = new ProductNodeHandler();
        pinSelectionChangeListener = new PinSelectionChangeListener();
        rasterToSpectraMap = new HashMap<>();
        rasterToSpectralBandsMap = new HashMap<>();
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
                if (!rasterToSpectraMap.containsKey(currentView.getRaster())) {
                    setUpSpectra();
                }
                recreateChart();

            }
            updateUIState();
        }
    }

    private Product getCurrentProduct() {
        return currentProduct;
    }

    private void setCurrentProduct(Product product) {
        Product oldProduct = currentProduct;
        currentProduct = product;
        if (currentProduct != oldProduct) {
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

    private void updateUIState() {
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
    }

    void setPrepareForUpdateMessage() {
        chartHandler.setCollectingSpectralInformationMessage();
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

    private SpectrumBand[] getAvailableSpectralBands(RasterDataNode currentRaster) {
        if (!rasterToSpectralBandsMap.containsKey(currentRaster)) {
            rasterToSpectralBandsMap.put(currentRaster, new ArrayList<>());
        }
        List<SpectrumBand> spectrumBands = rasterToSpectralBandsMap.get(currentRaster);
        Band[] bands = currentProduct.getBands();
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
        return spectrumBands.toArray(new SpectrumBand[spectrumBands.size()]);
    }

    private boolean isSpectralBand(Band band) {
        return band.getSpectralWavelength() > 0.0;
    }

    private void initUI() {
        final JFreeChart chart = ChartFactory.createXYLineChart(Bundle.CTL_SpectrumTopComponent_Name(),
                "Wavelength (nm)", "", null, PlotOrientation.VERTICAL,
                true, true, false);
        chart.getXYPlot().getRangeAxis().addChangeListener(axisChangeEvent -> {
            if (!isCodeInducedAxisChange) {
                rangeAxisAdjustmentIsFrozen = !((ValueAxis) axisChangeEvent.getAxis()).isAutoRange();
            }
        });
        chart.getXYPlot().getDomainAxis().addChangeListener(axisChangeEvent -> {
            if (!isCodeInducedAxisChange) {
                domainAxisAdjustmentIsFrozen = !((ValueAxis) axisChangeEvent.getAxis()).isAutoRange();
            }
        });
        chart.getXYPlot().getRangeAxis().setAutoRange(false);
        rangeAxisAdjustmentIsFrozen = false;
        chart.getXYPlot().getDomainAxis().setAutoRange(false);
        domainAxisAdjustmentIsFrozen = false;
        chartPanel = new ChartPanel(chart);
        chartHandler = new ChartHandler(chart);
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

        SnapApp.getDefault().getProductManager().addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
                // ignored
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                final Product product = event.getProduct();
                if (getCurrentProduct() == product) {
                    chartPanel.getChart().getXYPlot().setDataset(null);
                    setCurrentView(null);
                    setCurrentProduct(null);
                }
                if (currentView != null) {
                    final RasterDataNode currentRaster = currentView.getRaster();
                    rasterToSpectraMap.remove(currentRaster);
                    rasterToSpectralBandsMap.remove(currentRaster);
                }
                PlacemarkGroup pinGroup = product.getPinGroup();
                for (int i = 0; i < pinGroup.getNodeCount(); i++) {
                    chartHandler.removePinInformation(pinGroup.get(i));
                }
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

    private void selectSpectralBands() {
        final RasterDataNode currentRaster = currentView.getRaster();
        final DisplayableSpectrum[] allSpectra = rasterToSpectraMap.get(currentRaster);
        final SpectrumChooser spectrumChooser = new SpectrumChooser(SwingUtilities.getWindowAncestor(this), allSpectra);
        if (spectrumChooser.show() == AbstractDialog.ID_OK) {
            final DisplayableSpectrum[] spectra = spectrumChooser.getSpectra();
            rasterToSpectraMap.put(currentRaster, spectra);
        }
    }

    boolean isShowingCursorSpectrum() {
        return showSpectrumForCursorButton.isSelected();
    }

    private boolean isShowingPinSpectra() {
        return isShowingSpectraForSelectedPins() || isShowingSpectraForAllPins();
    }

    private boolean isShowingSpectraForAllPins() {
        return showSpectraForAllPinsButton.isSelected();
    }

    private void recreateChart() {
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

    private void setUpSpectra() {
        if (currentView == null) {
            return;
        }

        final RasterDataNode raster = currentView.getRaster();
        final SpectrumBand[] availableSpectralBands = getAvailableSpectralBands(raster);
        if (availableSpectralBands.length == 0) {
            rasterToSpectraMap.put(raster, new DisplayableSpectrum[0]);
            return;
        }

        int displayIndex = 0;
        final List<DisplayableSpectrum> spectra = new ArrayList<>();
        final BandGroup[] userBandGroups = bandGroupsManager.getGroupsMatchingProduct(currentProduct);
        if (userBandGroups.length > 0) {
            final DisplayableSpectrum[] userGroupingSpectra = new DisplayableSpectrum[userBandGroups.length];
            for (int i = 0; i < userBandGroups.length; i++) {
                final int symbolIndex = SpectrumShapeProvider.getValidIndex(displayIndex, false);
                ++displayIndex;
                final BandGroup userBandGroup = userBandGroups[i];
                final DisplayableSpectrum spectrum = new DisplayableSpectrum(userBandGroup.getName(), symbolIndex);
                spectrum.setSelected(false);
                spectrum.setLineStyle(SpectrumStrokeProvider.getStroke(i));

                String[] bandNames = userBandGroup.getMatchingBandNames(currentProduct);
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

        final BandGroup autoGrouping = currentProduct.getAutoGrouping();
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
                DisplayableSpectrum spectrum = new DisplayableSpectrum(spectrumName, symbolIndex);
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
            if (ungroupedBandsList.isEmpty()) {
                spectra.addAll(Arrays.asList(autoGroupingSpectra));
            } else {
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
        rasterToSpectraMap.put(raster, spectra.toArray(new DisplayableSpectrum[0]));
    }

    private DisplayableSpectrum[] getAllSpectra() {
        if (currentView == null || !rasterToSpectraMap.containsKey(currentView.getRaster())) {
            return new DisplayableSpectrum[0];
        }
        return rasterToSpectraMap.get(currentView.getRaster());
    }

    private boolean isShowingSpectraForSelectedPins() {
        return showSpectraForSelectedPinsButton.isSelected();
    }

    List<DisplayableSpectrum> getSelectedSpectra() {
        List<DisplayableSpectrum> selectedSpectra = new ArrayList<>();
        if (currentView != null) {
            final RasterDataNode currentRaster = currentView.getRaster();
            if (currentProduct != null && rasterToSpectraMap.containsKey(currentRaster)) {
                DisplayableSpectrum[] allSpectra = rasterToSpectraMap.get(currentRaster);
                for (DisplayableSpectrum displayableSpectrum : allSpectra) {
                    if (displayableSpectrum.isSelected()) {
                        selectedSpectra.add(displayableSpectrum);
                    }
                }
            }
        }
        return selectedSpectra;
    }

    private void updateSpectraUnits() {
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

    public boolean showsValidCursorSpectra() {
        return chartHandler.showsValidCursorSpectra();
    }

    Map<Placemark, Map<Band, Double>> getPinToEnergies() {
        return chartHandler.getPinToEnergies();
    }

    private class ChartHandler {

        private static final String MESSAGE_NO_SPECTRAL_BANDS = "No spectral bands available";   /*I18N*/
        private static final String MESSAGE_NO_PRODUCT_SELECTED = "No product selected";
        private static final String MESSAGE_NO_SPECTRA_SELECTED = "No spectra selected";
        private static final String MESSAGE_COLLECTING_SPECTRAL_INFORMATION = "Collecting spectral information...";

        private final JFreeChart chart;
        private final ChartUpdater chartUpdater;

        private ChartHandler(JFreeChart chart) {
            chartUpdater = new ChartUpdater();
            this.chart = chart;
            setLegend(chart);
            setAutomaticRangeAdjustments(false);
            final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
            renderer.setDefaultLinesVisible(true);
            renderer.setDefaultShapesFilled(false);
            setPlotMessage(MESSAGE_NO_PRODUCT_SELECTED);
        }

        private void setAutomaticRangeAdjustments(boolean userInducesAutomaticAdjustment) {
            final XYPlot plot = chart.getXYPlot();
            boolean adjustmentHasChanged = false;
            if (userInducesAutomaticAdjustment) {
                if (!isUserInducedAutomaticAdjustmentChosen) {
                    isUserInducedAutomaticAdjustmentChosen = true;
                    if (!isAutomaticDomainAdjustmentSet()) {
                        plot.getDomainAxis().setAutoRange(true);
                        domainAxisAdjustmentIsFrozen = false;
                        adjustmentHasChanged = true;
                    }
                    if (!isAutomaticRangeAdjustmentSet()) {
                        plot.getRangeAxis().setAutoRange(true);
                        rangeAxisAdjustmentIsFrozen = false;
                        adjustmentHasChanged = true;
                    }
                }
            } else {
                if (isUserInducedAutomaticAdjustmentChosen) {
                    isUserInducedAutomaticAdjustmentChosen = false;
                    if (isAutomaticDomainAdjustmentSet()) {
                        plot.getDomainAxis().setAutoRange(false);
                        domainAxisAdjustmentIsFrozen = false;
                        adjustmentHasChanged = true;
                    }
                    if (isAutomaticRangeAdjustmentSet()) {
                        plot.getRangeAxis().setAutoRange(false);
                        rangeAxisAdjustmentIsFrozen = false;
                        adjustmentHasChanged = true;
                    }
                }
            }
            if (adjustmentHasChanged) {
                chartUpdater.invalidatePlotBounds();
            }
        }

        private boolean isAutomaticDomainAdjustmentSet() {
            return chart.getXYPlot().getDomainAxis().isAutoRange();
        }

        private boolean isAutomaticRangeAdjustmentSet() {
            return chart.getXYPlot().getRangeAxis().isAutoRange();
        }

        private void setLegend(JFreeChart chart) {
            chart.removeLegend();
            final LegendTitle legend = new LegendTitle(new SpectrumLegendItemSource());
            legend.setPosition(RectangleEdge.BOTTOM);
            LineBorder border = new LineBorder(Color.BLACK, new BasicStroke(), new RectangleInsets(2, 2, 2, 2));
            legend.setFrame(border);
            chart.addLegend(legend);
        }

        private void setPosition(int pixelX, int pixelY, int level, boolean pixelPosInRasterBounds) {
            chartUpdater.setPosition(pixelX, pixelY, level, pixelPosInRasterBounds);
        }

        private void updateChart() {
            if (chartUpdater.isDatasetEmpty()) {
                setEmptyPlot();
                return;
            }
            List<DisplayableSpectrum> spectra = getSelectedSpectra();
            chartUpdater.updateChart(chart, spectra);
            chart.getXYPlot().clearAnnotations();
        }

        private void updateData() {
            List<DisplayableSpectrum> spectra = getSelectedSpectra();
            chartUpdater.updateData(chart, spectra);
        }

        private void setEmptyPlot() {
            chart.getXYPlot().setDataset(null);
            if (getCurrentProduct() == null) {
                setPlotMessage(MESSAGE_NO_PRODUCT_SELECTED);
            } else if (!chartUpdater.showsValidCursorSpectra()) {
            } else if (getAllSpectra().length == 0) {
                setPlotMessage(MESSAGE_NO_SPECTRA_SELECTED);
            } else {
                setPlotMessage(MESSAGE_NO_SPECTRAL_BANDS);
            }
        }

        private void setGridVisible(boolean visible) {
            chart.getXYPlot().setDomainGridlinesVisible(visible);
            chart.getXYPlot().setRangeGridlinesVisible(visible);
        }

        private void removePinInformation(Placemark pin) {
            chartUpdater.removePinInformation(pin);
        }

        private void removeBandInformation(Band band) {
            chartUpdater.removeBandinformation(band);
        }

        private void setPlotMessage(String messageText) {
            chart.getXYPlot().clearAnnotations();
            TextTitle tt = new TextTitle(messageText);
            tt.setTextAlignment(HorizontalAlignment.RIGHT);
            tt.setFont(chart.getLegend().getItemFont());
            tt.setBackgroundPaint(new Color(200, 200, 255, 50));
            tt.setFrame(new BlockBorder(Color.white));
            tt.setPosition(RectangleEdge.BOTTOM);
            XYTitleAnnotation message = new XYTitleAnnotation(0.5, 0.5, tt, RectangleAnchor.CENTER);
            chart.getXYPlot().addAnnotation(message);
        }

        public boolean showsValidCursorSpectra() {
            return chartUpdater.showsValidCursorSpectra();
        }

        public void removeCursorSpectraFromDataset() {
            chartUpdater.removeCursorSpectraFromDataset();
        }

        public void setCollectingSpectralInformationMessage() {
            setPlotMessage(MESSAGE_COLLECTING_SPECTRAL_INFORMATION);
        }

        public Map<Placemark, Map<Band, Double>> getPinToEnergies() {
            return chartUpdater.getPinToEnergies();
        }
    }

    private class ChartUpdater {

        private final static int domain_axis_index = 0;
        private final static int range_axis_index = 1;
        private final static double relativePlotInset = 0.05;

        private final Map<Placemark, Map<Band, Double>> pinToEnergies;
        private boolean showsValidCursorSpectra;
        private boolean pixelPosInRasterBounds;
        private int rasterPixelX;
        private int rasterPixelY;
        private int rasterLevel;
        private final Range[] plotBounds;
        private XYSeriesCollection dataset;
        private Point2D modelP;

        private ChartUpdater() {
            pinToEnergies = new HashMap<>();
            plotBounds = new Range[2];
            invalidatePlotBounds();
        }

        void invalidatePlotBounds() {
            plotBounds[domain_axis_index] = null;
            plotBounds[range_axis_index] = null;
        }

        private void setPosition(int pixelX, int pixelY, int level, boolean pixelPosInRasterBounds) {
            rasterPixelX = pixelX;
            rasterPixelY = pixelY;
            rasterLevel = level;
            this.pixelPosInRasterBounds = pixelPosInRasterBounds;
            final AffineTransform i2m = currentView.getBaseImageLayer().getImageToModelTransform(level);
            modelP = i2m.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), new Point2D.Double());
        }

        private void updateData(JFreeChart chart, List<DisplayableSpectrum> spectra) {
            dataset = new XYSeriesCollection();
            if (rasterLevel >= 0) {
                fillDatasetWithPinSeries(spectra, dataset, chart);
                fillDatasetWithCursorSeries(spectra, dataset, chart);
            }
        }

        private void updateChart(JFreeChart chart, List<DisplayableSpectrum> spectra) {
            final XYPlot plot = chart.getXYPlot();
            if (!chartHandler.isAutomaticDomainAdjustmentSet() && !domainAxisAdjustmentIsFrozen) {
                isCodeInducedAxisChange = true;
                updatePlotBounds(dataset.getDomainBounds(true), plot.getDomainAxis(), domain_axis_index);
                isCodeInducedAxisChange = false;
            }
            if (!chartHandler.isAutomaticRangeAdjustmentSet() && !rangeAxisAdjustmentIsFrozen) {
                isCodeInducedAxisChange = true;
                updatePlotBounds(dataset.getRangeBounds(true), plot.getRangeAxis(), range_axis_index);
                isCodeInducedAxisChange = false;
            }
            plot.setDataset(dataset);
            setPlotUnit(spectra, plot);
        }

        private void setPlotUnit(List<DisplayableSpectrum> spectra, XYPlot plot) {
            String unitToBeDisplayed = "";
            if (spectra.size() > 0) {
                unitToBeDisplayed = spectra.get(0).getUnit();
                int i = 1;
                while (i < spectra.size() && !unitToBeDisplayed.equals(DisplayableSpectrum.MIXED_UNITS)) {
                    DisplayableSpectrum displayableSpectrum = spectra.get(i);
                    i++;
                    if (displayableSpectrum.hasSelectedBands() && !unitToBeDisplayed.equals(displayableSpectrum.getUnit())) {
                        unitToBeDisplayed = DisplayableSpectrum.MIXED_UNITS;
                    }
                }
            }
            isCodeInducedAxisChange = true;
            plot.getRangeAxis().setLabel(unitToBeDisplayed);
            isCodeInducedAxisChange = false;
        }

        private void updatePlotBounds(Range newBounds, ValueAxis axis, int index) {
            if (newBounds != null) {
                final Range axisBounds = axis.getRange();
                final Range oldBounds = plotBounds[index];
                plotBounds[index] = getNewRange(newBounds, plotBounds[index], axisBounds);
                if (oldBounds != plotBounds[index]) {
                    axis.setRange(getNewPlotBounds(plotBounds[index]));
                }
            }
        }

        private Range getNewRange(Range newBounds, Range currentBounds, Range plotBounds) {
            if (currentBounds == null) {
                currentBounds = newBounds;
            } else {
                if (plotBounds.getLowerBound() > 0 && newBounds.getLowerBound() < currentBounds.getLowerBound() ||
                        newBounds.getUpperBound() > currentBounds.getUpperBound()) {
                    currentBounds = new Range(Math.min(currentBounds.getLowerBound(), newBounds.getLowerBound()),
                            Math.max(currentBounds.getUpperBound(), newBounds.getUpperBound()));
                }
            }
            return currentBounds;
        }

        private Range getNewPlotBounds(Range bounds) {
            double range = bounds.getLength();
            double delta = range * relativePlotInset;
            return new Range(Math.max(0, bounds.getLowerBound() - delta),
                    bounds.getUpperBound() + delta);
        }

        private void fillDatasetWithCursorSeries(List<DisplayableSpectrum> spectra, XYSeriesCollection dataset, JFreeChart chart) {
            showsValidCursorSpectra = false;
            if (modelP == null) {
                return;
            }
            if (isShowingCursorSpectrum() && currentView != null) {
                for (DisplayableSpectrum spectrum : spectra) {
                    XYSeries series = new XYSeries(spectrum.getName());
                    final Band[] spectralBands = spectrum.getSelectedBands();
                    if (!currentProduct.isMultiSize()) {
                        for (Band spectralBand : spectralBands) {
                            final float wavelength = spectralBand.getSpectralWavelength();
                            if (pixelPosInRasterBounds && isPixelValid(spectralBand, rasterPixelX, rasterPixelY, rasterLevel)) {
                                addToSeries(spectralBand, rasterPixelX, rasterPixelY, rasterLevel, series, wavelength);
                                showsValidCursorSpectra = true;
                            }
                        }
                    } else {
                        for (Band spectralBand : spectralBands) {
                            final float wavelength = spectralBand.getSpectralWavelength();
                            final AffineTransform i2m = spectralBand.getImageToModelTransform();
                            if (i2m.equals(currentView.getRaster().getImageToModelTransform())) {
                                if (pixelPosInRasterBounds && isPixelValid(spectralBand, rasterPixelX, rasterPixelY, rasterLevel)) {
                                    addToSeries(spectralBand, rasterPixelX, rasterPixelY, rasterLevel, series, wavelength);
                                    showsValidCursorSpectra = true;
                                }
                            } else {
                                //todo [Multisize_products] use scenerastertransform here
                                final PixelPos rasterPos = new PixelPos();
                                final MultiLevelModel multiLevelModel = spectralBand.getMultiLevelModel();
                                int level = getLevel(multiLevelModel);
                                multiLevelModel.getModelToImageTransform(level).transform(modelP, rasterPos);
                                final int rasterX = (int) rasterPos.getX();
                                final int rasterY = (int) rasterPos.getY();
                                if (coordinatesAreInRasterBounds(spectralBand, rasterX, rasterY, level) &&
                                        isPixelValid(spectralBand, rasterX, rasterY, level)) {
                                    addToSeries(spectralBand, rasterX, rasterY, level, series, wavelength);
                                    showsValidCursorSpectra = true;
                                }
                            }
                        }
                    }
                    updateRenderer(dataset.getSeriesCount(), Color.BLACK, spectrum, chart);
                    dataset.addSeries(series);
                }
            }
        }

        private void addToSeries(Band spectralBand, int x, int y, int level, XYSeries series, double wavelength) {
            final double energy = ProductUtils.getGeophysicalSampleAsDouble(spectralBand, x, y, level);
            if (energy != spectralBand.getGeophysicalNoDataValue()) {
                series.add(wavelength, energy);
            }
        }

        //todo code duplication with pixelinfoviewmodelupdater - move to single class - tf 20151119
        private boolean coordinatesAreInRasterBounds(RasterDataNode raster, int x, int y, int level) {
            final RenderedImage levelImage = raster.getSourceImage().getImage(level);
            return x >= 0 && y >= 0 && x < levelImage.getWidth() && y < levelImage.getHeight();
        }

        private void fillDatasetWithPinSeries(List<DisplayableSpectrum> spectra, XYSeriesCollection dataset, JFreeChart chart) {
            Placemark[] pins = getDisplayedPins();
            for (Placemark pin : pins) {
                List<XYSeries> pinSeries = createXYSeriesFromPin(pin, dataset.getSeriesCount(), spectra, chart);
                pinSeries.forEach(dataset::addSeries);
            }
        }

        private List<XYSeries> createXYSeriesFromPin(Placemark pin, int seriesIndex, List<DisplayableSpectrum> spectra, JFreeChart chart) {
            List<XYSeries> pinSeries = new ArrayList<>();
            Color pinColor = PlacemarkUtils.getPlacemarkColor(pin, currentView);
            for (DisplayableSpectrum spectrum : spectra) {
                XYSeries series = new XYSeries(spectrum.getName() + "_" + pin.getLabel());
                final Band[] spectralBands = spectrum.getSelectedBands();
                Map<Band, Double> bandToEnergy;
                if (pinToEnergies.containsKey(pin)) {
                    bandToEnergy = pinToEnergies.get(pin);
                } else {
                    bandToEnergy = new HashMap<>();
                    pinToEnergies.put(pin, bandToEnergy);
                }
                for (Band spectralBand : spectralBands) {
                    double energy;
                    if (bandToEnergy.containsKey(spectralBand)) {
                        energy = bandToEnergy.get(spectralBand);
                    } else {
                        energy = readEnergy(pin, spectralBand);
                        bandToEnergy.put(spectralBand, energy);
                    }
                    final float wavelength = spectralBand.getSpectralWavelength();
                    if (energy != spectralBand.getGeophysicalNoDataValue()) {
                        series.add(wavelength, energy);
                    }
                }
                updateRenderer(seriesIndex, pinColor, spectrum, chart);
                seriesIndex++;
                pinSeries.add(series);
            }
            return pinSeries;
        }


        private void updateRenderer(int seriesIndex, Color seriesColor, DisplayableSpectrum spectrum, JFreeChart chart) {
            final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();

            final Stroke lineStyle = spectrum.getLineStyle();
            renderer.setSeriesStroke(seriesIndex, lineStyle);

            Shape symbol = spectrum.getScaledShape();
            renderer.setSeriesShape(seriesIndex, symbol);
            renderer.setSeriesShapesVisible(seriesIndex, true);

            renderer.setSeriesPaint(seriesIndex, seriesColor);
        }

        private double readEnergy(Placemark pin, Band spectralBand) {
            //todo [Multisize_products] use scenerastertransform here
            final Object pinGeometry = pin.getFeature().getDefaultGeometry();
            if (pinGeometry == null || !(pinGeometry instanceof Point)) {
                return spectralBand.getGeophysicalNoDataValue();
            }
            final Point2D.Double modelPoint = new Point2D.Double(((Point) pinGeometry).getCoordinate().x,
                    ((Point) pinGeometry).getCoordinate().y);
            final MultiLevelModel multiLevelModel = spectralBand.getMultiLevelModel();
            int level = getLevel(multiLevelModel);
            final AffineTransform m2iTransform = multiLevelModel.getModelToImageTransform(level);
            final PixelPos pinLevelRasterPos = new PixelPos();
            m2iTransform.transform(modelPoint, pinLevelRasterPos);
            int pinLevelRasterX = (int) Math.floor(pinLevelRasterPos.getX());
            int pinLevelRasterY = (int) Math.floor(pinLevelRasterPos.getY());
            if (coordinatesAreInRasterBounds(spectralBand, pinLevelRasterX, pinLevelRasterY, level) &&
                    isPixelValid(spectralBand, pinLevelRasterX, pinLevelRasterY, level)) {
                return ProductUtils.getGeophysicalSampleAsDouble(spectralBand, pinLevelRasterX, pinLevelRasterY, level);
            }
            return spectralBand.getGeophysicalNoDataValue();
        }

        private void removePinInformation(Placemark pin) {
            pinToEnergies.remove(pin);
        }

        private void removeBandinformation(Band band) {
            for (Placemark pin : pinToEnergies.keySet()) {
                Map<Band, Double> bandToEnergiesMap = pinToEnergies.get(pin);
                bandToEnergiesMap.remove(band);
            }
        }

        public boolean showsValidCursorSpectra() {
            return showsValidCursorSpectra;
        }

        void removeCursorSpectraFromDataset() {
            modelP = null;
            if (showsValidCursorSpectra) {
                int numberOfSelectedSpectra = getSelectedSpectra().size();
                int numberOfPins = getDisplayedPins().length;
                int numberOfDisplayedGraphs = numberOfPins * numberOfSelectedSpectra;
                while (dataset.getSeriesCount() > numberOfDisplayedGraphs) {
                    dataset.removeSeries(dataset.getSeriesCount() - 1);
                }
            }
        }

        public boolean isDatasetEmpty() {
            return dataset == null || dataset.getSeriesCount() == 0;
        }

        //todo code duplication with pixelinfoviewmodelupdater - move to single class - tf 20151119
        private boolean isPixelValid(RasterDataNode raster, int pixelX, int pixelY, int level) {
            if (raster.isValidMaskUsed()) {
                PlanarImage image = ImageManager.getInstance().getValidMaskImage(raster, level);
                Raster data = getRasterTile(image, pixelX, pixelY);
                return data.getSample(pixelX, pixelY, 0) != 0;
            } else {
                return true;
            }
        }

        //todo code duplication with pixelinfoviewmodelupdater - move to single class - tf 20151119
        private Raster getRasterTile(PlanarImage image, int pixelX, int pixelY) {
            final int tileX = image.XToTileX(pixelX);
            final int tileY = image.YToTileY(pixelY);
            return image.getTile(tileX, tileY);
        }

        //todo code duplication with pixelinfoviewmodelupdater - move to single class - tf 20151119
        private int getLevel(MultiLevelModel multiLevelModel) {
            if (rasterLevel < multiLevelModel.getLevelCount()) {
                return rasterLevel;
            }
            return ImageLayer.getLevel(multiLevelModel, currentView.getViewport());
        }

        Map<Placemark, Map<Band, Double>> getPinToEnergies() {
            return pinToEnergies;
        }
    }

    private class SpectrumLegendItemSource implements LegendItemSource {

        @Override
        public LegendItemCollection getLegendItems() {
            LegendItemCollection itemCollection = new LegendItemCollection();
            final Placemark[] displayedPins = getDisplayedPins();
            final List<DisplayableSpectrum> spectra = getSelectedSpectra();
            for (Placemark pin : displayedPins) {
                Paint pinPaint = PlacemarkUtils.getPlacemarkColor(pin, currentView);
                spectra.stream().filter(DisplayableSpectrum::hasSelectedBands).forEach(spectrum -> {
                    String legendLabel = pin.getLabel() + "_" + spectrum.getName();
                    LegendItem item = createLegendItem(spectrum, pinPaint, legendLabel);
                    itemCollection.add(item);
                });
            }
            if (isShowingCursorSpectrum() && showsValidCursorSpectra()) {
                spectra.stream().filter(DisplayableSpectrum::hasSelectedBands).forEach(spectrum -> {
                    Paint defaultPaint = Color.BLACK;
                    LegendItem item = createLegendItem(spectrum, defaultPaint, spectrum.getName());
                    itemCollection.add(item);
                });
            }
            return itemCollection;
        }

        private LegendItem createLegendItem(DisplayableSpectrum spectrum, Paint paint, String legendLabel) {
            Stroke outlineStroke = new BasicStroke();
            Line2D lineShape = new Line2D.Double(0, 5, 40, 5);
            Stroke lineStyle = spectrum.getLineStyle();
            Shape symbol = spectrum.getScaledShape();
            return new LegendItem(legendLabel, legendLabel, legendLabel, legendLabel,
                    true, symbol, false,
                    paint, true, paint, outlineStroke,
                    true, lineShape, lineStyle, paint);
        }

    }

    /////////////////////////////////////////////////////////////////////////
    // Product change handling

    private class ProductNodeHandler extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            boolean chartHasChanged = false;
            if (event.getSourceNode() instanceof Band) {
                final String propertyName = event.getPropertyName();
                if (propertyName.equals(DataNode.PROPERTY_NAME_UNIT)) {
                    updateSpectraUnits();
                    chartHasChanged = true;
                } else if (propertyName.equals(Band.PROPERTY_NAME_SPECTRAL_WAVELENGTH)) {
                    setUpSpectra();
                    chartHasChanged = true;
                }
            } else if (event.getSourceNode() instanceof Placemark) {
                if ("geoPos".equals(event.getPropertyName()) || "pixelPos".equals(event.getPropertyName())) {
                    chartHandler.removePinInformation((Placemark) event.getSourceNode());
                }
                if (isShowingPinSpectra()) {
                    chartHasChanged = true;
                }
            } else if (event.getSourceNode() instanceof Product) {
                if ("autoGrouping".equals(event.getPropertyName())) {
                    setUpSpectra();
                    chartHasChanged = true;
                }
            }
            if (isActive() && chartHasChanged) {
                recreateChart();
            }
        }

        @Override
        public void nodeAdded(final ProductNodeEvent event) {
            if (!isActive()) {
                return;
            }
            if (event.getSourceNode() instanceof Band) {
                Band newBand = (Band) event.getSourceNode();
                if (isSpectralBand(newBand)) {
                    addBandToSpectra((Band) event.getSourceNode());
                    recreateChart();
                }
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinSpectra()) {
                    recreateChart();
                } else {
                    updateUIState();
                }
            }
        }

        @Override
        public void nodeRemoved(final ProductNodeEvent event) {
            if (!isActive()) {
                return;
            }
            if (event.getSourceNode() instanceof Band) {
                Band band = (Band) event.getSourceNode();
                removeBandFromSpectra(band);
                chartHandler.removeBandInformation(band);
                recreateChart();
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinSpectra()) {
                    recreateChart();
                }
            }
        }

        private void addBandToSpectra(Band band) {
            DisplayableSpectrum[] allSpectra = rasterToSpectraMap.get(currentView.getRaster());
            BandGroup autoGrouping = currentProduct.getAutoGrouping();
            if (autoGrouping != null) {
                final int bandIndex = autoGrouping.indexOf(band.getName());
                final DisplayableSpectrum spectrum;
                if (bandIndex != -1) {
                    spectrum = allSpectra[bandIndex];
                } else {
                    spectrum = allSpectra[allSpectra.length - 1];
                }
                spectrum.addBand(new SpectrumBand(band, spectrum.isSelected()));
            } else {
                allSpectra[0].addBand(new SpectrumBand(band, true));
            }
        }

        private void removeBandFromSpectra(Band band) {
            DisplayableSpectrum[] allSpectra = rasterToSpectraMap.get(currentView.getRaster());
            for (DisplayableSpectrum displayableSpectrum : allSpectra) {
                Band[] spectralBands = displayableSpectrum.getSpectralBands();
                for (int j = 0; j < spectralBands.length; j++) {
                    Band spectralBand = spectralBands[j];
                    if (spectralBand == band) {
                        displayableSpectrum.remove(j);
                        if (displayableSpectrum.getSelectedBands().length == 0) {
                            displayableSpectrum.setSelected(false);
                        }
                        return;
                    }
                }
            }
        }

        private boolean isActive() {
            return isVisible() && getCurrentProduct() != null;
        }
    }

    private class PinSelectionChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            recreateChart();
        }

    }

}
