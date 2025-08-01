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
import com.bc.ceres.multilevel.MultiLevelModel;
import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.help.HelpAction;
import org.esa.snap.rcp.placemark.PlacemarkUtils;
import org.esa.snap.rcp.preferences.general.SpectrumViewController;
import org.esa.snap.rcp.statistics.XYPlotMarker;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.smart.configurator.ConfigurationOptimizer;
import org.esa.snap.smart.configurator.PerformanceParameters;
import org.esa.snap.ui.*;
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

@TopComponent.Description(preferredID = "SpectrumTopComponent",
        iconBase = "org/esa/snap/rcp/icons/" + PackageDefaults.SPECTRUM_ICON,
        persistenceType = TopComponent.PERSISTENCE_NEVER
)


@TopComponent.Registration(mode = PackageDefaults.SPECTRUM_WS_MODE, openAtStartup = false, position = 80)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.SpectrumTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Optical", position = 0),
        @ActionReference(path = "Menu/View/Tool Windows/Optical"),
        @ActionReference(path = "Toolbars/Spectral Angular View", position = 10)
})
@TopComponent.OpenActionRegistration(displayName = "#CTL_SpectrumTopComponent_Name", preferredID = "SpectrumTopComponent")
@NbBundle.Messages({"CTL_SpectrumTopComponent_Name=Spectrum View", "CTL_SpectrumTopComponent_HelpId=showSpectrumWnd"})
/*
 * A window which displays spectra at selected pixel positions.
 */
public class SpectrumTopComponent extends ToolTopComponent {

    public static final String ID = SpectrumTopComponent.class.getName();

    private static final String SUPPRESS_MESSAGE_KEY = "plugin.spectrum.tip";

    private boolean wasOpenedBefore = false;

    private boolean recreatingChart = false;



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

    private JCheckBox rangeXCheckBox;
    private JTextField rangeXLowerTextField;
    private JLabel rangeXLowerLabel;
    private JTextField rangeXUpperTextField;
    private JLabel rangeXUpperLabel;

    private JCheckBox rangeYCheckBox;
    private JTextField rangeYLowerTextField;
    private JLabel rangeYLowerLabel;
    private JTextField rangeYUpperTextField;
    private JLabel rangeYUpperLabel;

    private boolean useRangeX;
    private double rangeXLower;
    private double rangeXUpper;

    private boolean useRangeY;
    private double rangeYLower;
    private double rangeYUpper;

    private boolean tipShown;
    private boolean spectrumViewToolIsOpen;
    private boolean loadingPreferences;
    private ProductSceneView currentView;
    private Product currentProduct;
    private ChartPanel chartPanel;
    private ChartHandler chartHandler;

    private boolean domainAxisAdjustmentIsFrozen;
    private boolean rangeAxisAdjustmentIsFrozen;
    private boolean isCodeInducedAxisChange;
    private boolean isUserInducedAutomaticAdjustmentChosen;

    private int workDoneMaster = 0;
    private int totalWorkPlannedMaster = 100;

    public SpectrumTopComponent() {
        // System.out.println("Spectrum View Tool is Open");
        spectrumViewToolIsOpen = false;

        tipShown = true;
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
                if (currentProduct.getName().contains("SPEX")) {
                    List<Integer> view_Angles = new ArrayList<Integer>();
                    for (int  i = 0; i < currentProduct.getNumBands(); i++ ) {
                        int viewAngle = (int) currentProduct.getBandAt(i).getAngularValue();
                        if (!view_Angles.contains(viewAngle)) {
                            view_Angles.add(viewAngle);
                            if (view_Angles.size()  == 5) {
                                break;
                            }
                        }
                    }
                    String autoGroupingStr = "QC:QC_bitwise:QC_polsample_bitwise:QC_polsample:";
                    if (view_Angles != null) {
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "I_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "DOLP_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "AOLP_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "i_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "i_stdev_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "i_polsample_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "i_polsample_stdev_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "aolp_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "aolp_stdev_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "dolp_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "dolp_stdev_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "q_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "q_stdev_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "u_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "u_stdev_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "q_over_i_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "q_over_i_stdev_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "u_over_i_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "u_over_i_stdev_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "qc_" + view_Angles.get(i);
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "qc_polsample_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "i_noisefree_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "i_noisefree_polsample_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "dolp_noisefree_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "q_over_i_noisefree_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "aolp_noisefree_" + view_Angles.get(i) + "_*:";
                        }
                        for (int i = 0; i < 5; i ++) {
                            autoGroupingStr += "u_over_i_noisefree_" + view_Angles.get(i) + "_*:";
                        }
                    }
                    autoGroupingStr += "I:I_noise:I_noisefree:I_polsample:" +
                            "I_polsample_noise:I_noisefree_polsample:DOLP:DOLP_noise:DOLP_noisefree:" +
                            "Q_over_I:Q_over_I_noise:Q_over_I_noisefree:AOLP:AOLP_noise:AOLP_noisefree:" +
                            "U_over_I:U_over_I_noise:U_over_I_noisefree:scattering_angle:rotation_angle:" +
                            "sensor_azimuth:sensor_azimuth_angle:sensor_zenith:sensor_zenith_angle:"  +
                            "solar_azimuth:solar_azimuth_angle:solar_zenith:solar_zenith_angle:" +
                            "obs_per_view:view_time_offsets:number_of_observations";

                    currentProduct.setAutoGrouping(autoGroupingStr);
//                    currentProduct.setAutoGrouping("I:I_58_*:I_22_*:I_4_*:I_-22_*:I_-58_*:" +
//                            "AOLP:AOLP_58_*:AOLP_22_*:AOLP_4_*:AOLP_-22_*:AOLP_-58_*:" +
//                            "DOLP:DOLP_58_*:DOLP_22_*:DOLP_4_*:DOLP_-22_*:DOLP_-58_*:" +
//                            "QC:QC_58_*:QC_22_*:QC_4_*:QC_-22_*:QC_-58_*:" +
//                            "I_57_*:I_50_*:I_20_*:I_0_*:I_-20_*:I_-57_*:I_-50*:" +
//                            "AOLP_57_*:AOLP_50:AOLP_20_*:AOLP_0_*:AOLP_-20_*:AOLP_-57_*:AOLP_-50" +
//                            "DOLP_57_*:DOLP_20_*:DOLP_0_*:DOLP_-20_*:DOLP_-57_*:" +
//                            "QC_57_*:QC_20_*:QC_0_*:QC_-22_*:QC_-57_*:" +
//                            "QC_bitwise:QC_polsample_bitwise:QC_polsample:" +
//                            "I_noise:I_noisefree:I_polsample:I_polsample_noise:I_noisefree_polsample:" +
//                            "DOLP_noise:DOLP_noisefree:AOLP_noise:AOLP_noisefree:" +
//                            "Q_over_I:Q_over_I_noise:Q_over_I_noisefree:" +
//                            "U_over_I:U_over_I_noise:U_over_I_noisefree:scattering_angle:" +
//                            "sensor_azimuth:sensor_zenith:solar_azimuth:solar_zenith:" +
//                            "obs_per_view:view_time_offsets");
                }

                boolean showProgressMonitor = false;
                if (getAllSpectra() == null && getAllSpectra().length == 0) {
                    showProgressMonitor = true;
                }

                if (!rasterToSpectraMap.containsKey(currentView.getRaster())) {
                    showProgressMonitor = true;
                    setUpSpectra();
                }

                recreateChart(showProgressMonitor);

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

    void setPlotChartMessage(String message) {
        chartHandler.setPlotMessage(message);
    }

    void setMessageCursorModeOff() {
        chartHandler.setMessageCursorModeOff();
    }
    void setMessageCursorModeNan() {
        chartHandler.setMessageCursorModeNan();
    }
    void setMessageCursorNotOnImage() {
        chartHandler.setMessageCursorNotOnImage();
    }

    void clearPrepareForUpdateMessage() {
        chartHandler.setPlotMessage("");
    }

    void updateData(int pixelX, int pixelY, int level, boolean pixelPosInRasterBounds) {
        updateData(pixelX, pixelY, level, pixelPosInRasterBounds, null,0);
    }

    void updateData(int pixelX, int pixelY, int level, boolean pixelPosInRasterBounds, com.bc.ceres.core.ProgressMonitor pm, int totalWorkPlanned) {
        chartHandler.setPosition(pixelX, pixelY, level, pixelPosInRasterBounds);
        chartHandler.updateData(pm, totalWorkPlanned);
    }





    void updateChart(boolean adjustAxes) {
//        if (!loadingPreferences) {
        chartHandler.setAutomaticRangeAdjustments(adjustAxes);
        updateChart();
//        }
    }

    void updateChart() {
//        if (!loadingPreferences) {
        maybeShowTip();
        chartHandler.updateChart();
        chartPanel.repaint();
//        }
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




        rangeXCheckBox = new JCheckBox(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_LABEL);
        rangeXCheckBox.setToolTipText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_TOOLTIP);

        rangeXLowerLabel = new JLabel(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_LABEL);
        rangeXLowerTextField = new JTextField("1234567890");
        rangeXLowerLabel.setToolTipText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_TOOLTIP);
        rangeXLowerTextField.setToolTipText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_TOOLTIP);
        rangeXLowerTextField.setMinimumSize(rangeXLowerTextField.getPreferredSize());
        rangeXLowerTextField.setPreferredSize(rangeXLowerTextField.getPreferredSize());

        rangeXUpperLabel = new JLabel(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_LABEL);
        rangeXUpperTextField = new JTextField("1234567890");
        rangeXUpperLabel.setToolTipText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_TOOLTIP);
        rangeXUpperTextField.setToolTipText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_TOOLTIP);
        rangeXUpperTextField.setMinimumSize(rangeXUpperTextField.getPreferredSize());
        rangeXUpperTextField.setPreferredSize(rangeXUpperTextField.getPreferredSize());
        rangeXUpperTextField.setText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_DEFAULT);



        rangeYCheckBox = new JCheckBox(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_LABEL);
        rangeYCheckBox.setToolTipText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_TOOLTIP);

        rangeYLowerLabel = new JLabel(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_LABEL);
        rangeYLowerTextField = new JTextField("1234567890");
        rangeYLowerLabel.setToolTipText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_TOOLTIP);
        rangeYLowerTextField.setToolTipText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_TOOLTIP);
        rangeYLowerTextField.setMinimumSize(rangeYLowerTextField.getPreferredSize());
        rangeYLowerTextField.setPreferredSize(rangeYLowerTextField.getPreferredSize());

        rangeYUpperLabel = new JLabel(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_LABEL);
        rangeYUpperTextField = new JTextField("1234567890");
        rangeYUpperLabel.setToolTipText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_TOOLTIP);
        rangeYUpperTextField.setToolTipText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_TOOLTIP);
        rangeYUpperTextField.setMinimumSize(rangeYUpperTextField.getPreferredSize());
        rangeYUpperTextField.setPreferredSize(rangeYUpperTextField.getPreferredSize());
        rangeYUpperTextField.setText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_DEFAULT);


        rangeXLowerTextField.addActionListener(e -> {
            if (!loadingPreferences) {
                updateChart(false);
            }
        });
        rangeXUpperTextField.addActionListener(e -> {
            if (!loadingPreferences) {
                updateChart(false);
            }
        });

        rangeYLowerTextField.addActionListener(e -> {
            if (!loadingPreferences) {
                updateChart(false);
            }
        });
        rangeYUpperTextField.addActionListener(e -> {
            if (!loadingPreferences) {
                updateChart(false);
            }
        });


        rangeXCheckBox.addActionListener(e -> {
            rangeXLowerLabel.setEnabled(rangeXCheckBox.isSelected());
            rangeXLowerTextField.setEnabled(rangeXCheckBox.isSelected());
            rangeXUpperLabel.setEnabled(rangeXCheckBox.isSelected());
            rangeXUpperTextField.setEnabled(rangeXCheckBox.isSelected());
            if (!loadingPreferences) {
                if (rangeXCheckBox.isSelected()) {
                    updateChart(false);
                } else {
                    updateChart(true);
                }
            }
        });

        rangeYCheckBox.addActionListener(e -> {
            rangeYLowerLabel.setEnabled(rangeYCheckBox.isSelected());
            rangeYLowerTextField.setEnabled(rangeYCheckBox.isSelected());
            rangeYUpperLabel.setEnabled(rangeYCheckBox.isSelected());
            rangeYUpperTextField.setEnabled(rangeYCheckBox.isSelected());
            if (!loadingPreferences) {
                if (rangeYCheckBox.isSelected()) {
                    updateChart(false);
                } else {
                    updateChart(true);
                }
            }
        });



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

            final ProductSceneView selectedProductSceneView = getSelectedProductSceneView();
            if (selectedProductSceneView != null) {
                selectedProductSceneView.addPixelPositionListener(pixelPositionListener);
                setCurrentView(selectedProductSceneView);
            }

            updateChart(true);

            // System.out.println("Listening to filterButton");

            selectSpectralBands();
            recreateChart();
        });
        filterButton.setToolTipText("Filter bands");

        showSpectrumForCursorButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/CursorSpectrum24.gif"), true);
        showSpectrumForCursorButton.addActionListener(e -> {
            if (!showSpectrumForCursorButton.isSelected()) {
                setMessageCursorModeOff();

            }
            recreateChart();

            if (showSpectrumForCursorButton.isSelected()) {
                setMessageCursorNotOnImage();
            }


//            if (showSpectrumForCursorButton.isSelected()) {
//                // System.out.println("Listening to showSpectrumForCursorButton - true");
//                runProgressMonitorForCursor();
//            } else {
//                // System.out.println("Listening to showSpectrumForCursorButton - false");
//                recreateChart();
//            }
        });
        showSpectrumForCursorButton.setName("showSpectrumForCursorButton");
        showSpectrumForCursorButton.setSelected(false);
        showSpectrumForCursorButton.setToolTipText("Show spectrum at cursor position.");

        showSpectraForSelectedPinsButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/SelectedPinSpectra24.gif"), true);
        showSpectraForSelectedPinsButton.addActionListener(e -> {
            if (isShowingSpectraForAllPins()) {
                showSpectraForAllPinsButton.setSelected(false);
            } else if (!isShowingSpectraForSelectedPins()) {
                plotMarker.setInvisible();
            }
            if (showSpectraForSelectedPinsButton.isSelected()) {
                // System.out.println("Listening to showSpectraForSelectedPinsButton - true");
                recreateChart(true);
            } else {
                // System.out.println("Listening to showSpectraForSelectedPinsButton -false");
                recreateChart();
            }
        });
        showSpectraForSelectedPinsButton.setName("showSpectraForSelectedPinsButton");
        showSpectraForSelectedPinsButton.setSelected(false);
        showSpectraForSelectedPinsButton.setToolTipText("Show spectra for selected pins.");

        showSpectraForAllPinsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/PinSpectra24.gif"),
                true);
        showSpectraForAllPinsButton.addActionListener(e -> {
            if (isShowingSpectraForSelectedPins()) {
                showSpectraForSelectedPinsButton.setSelected(false);
            } else if (!isShowingSpectraForAllPins()) {
                plotMarker.setInvisible();
            }
            if (showSpectraForAllPinsButton.isSelected()) {
                // System.out.println("Listening to showSpectraForAllPinsButton - true");
                recreateChart(true);
            } else {
                // System.out.println("Listening to showSpectraForAllPinsButton - false");
                recreateChart();
            }
        });
        showSpectraForAllPinsButton.setName("showSpectraForAllPinsButton");
        showSpectraForAllPinsButton.setSelected(false);
        showSpectraForAllPinsButton.setToolTipText("Show spectra for all pins.");

        showGridButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/SpectrumGrid24.gif"), true);

        showGridButton.addActionListener(e -> {
            if (!loadingPreferences) {
                chartHandler.setGridVisible(showGridButton.isSelected());
            }
        });
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

        loadPreferences();


        final JPanel buttonPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
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
        gbc.gridwidth = 2;
        buttonPane.add(rangeXCheckBox, gbc);

        gbc.gridy++;
        gbc.gridx=0;
        gbc.gridwidth = 1;
        buttonPane.add(rangeXLowerLabel, gbc);
        gbc.gridx=1;
        buttonPane.add(rangeXLowerTextField, gbc);
        gbc.gridy++;
        gbc.gridx=0;
        buttonPane.add(rangeXUpperLabel, gbc);
        gbc.gridx=1;
        buttonPane.add(rangeXUpperTextField, gbc);


        gbc.gridy++;
        gbc.gridx=0;
        gbc.gridwidth = 2;
        buttonPane.add(rangeYCheckBox, gbc);

        gbc.gridy++;
        gbc.gridx=0;
        gbc.gridwidth = 1;
        buttonPane.add(rangeYLowerLabel, gbc);
        gbc.gridx=1;
        buttonPane.add(rangeYLowerTextField, gbc);
        gbc.gridy++;
        gbc.gridx=0;
        buttonPane.add(rangeYUpperLabel, gbc);
        gbc.gridx=1;
        buttonPane.add(rangeYUpperTextField, gbc);


        gbc.gridwidth = 2;
        gbc.gridy++;
        gbc.gridx=0;
        buttonPane.add(exportSpectraButton, gbc);
        gbc.gridy++;
        gbc.insets.bottom = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPane.add(new JLabel(" "), gbc); // filler
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
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

    private void loadPreferences() {
        loadingPreferences = true;

        try {
            chartHandler.chart.setTitle(SpectrumViewController.getPreferenceTitle());

            rangeYCheckBox.setSelected(SpectrumViewController.getPreferenceYaxisSetBounds());
            rangeYLowerTextField.setText(SpectrumViewController.getPreferenceYaxisMin());
            rangeYUpperTextField.setText(SpectrumViewController.getPreferenceYaxisMax());

            rangeXCheckBox.setSelected(SpectrumViewController.getPreferenceXaxisSetBounds());
            rangeXLowerTextField.setText(SpectrumViewController.getPreferenceXaxisMin());
            rangeXUpperTextField.setText(SpectrumViewController.getPreferenceXaxisMax());



            Color gridlinesColor = SpectrumViewController.getPreferenceGridlinesColor();
            chartHandler.chart.getXYPlot().setDomainGridlinePaint(gridlinesColor);
            chartHandler.chart.getXYPlot().setRangeGridlinePaint(gridlinesColor);
            chartHandler.chart.getXYPlot().setDomainMinorGridlinePaint(gridlinesColor);
            chartHandler.chart.getXYPlot().setRangeMinorGridlinePaint(gridlinesColor);


            Color plotBackgroundColor = SpectrumViewController.getPreferenceBackgroundColor();
            chartHandler.chart.getPlot().setBackgroundPaint(plotBackgroundColor);
            chartHandler.chart.getXYPlot().setBackgroundPaint(plotBackgroundColor);


            Color foregroundColor = SpectrumViewController.PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_DEFAULT;

            chartHandler.chart.getXYPlot().getRangeAxis().setTickMarkPaint(foregroundColor);
            chartHandler.chart.getXYPlot().getRangeAxis().setAxisLinePaint(foregroundColor);
            chartHandler.chart.getXYPlot().getDomainAxis().setTickMarkPaint(foregroundColor);
            chartHandler.chart.getXYPlot().getDomainAxis().setAxisLinePaint(foregroundColor);
            chartHandler.chart.getTitle().setPaint(foregroundColor);
            chartHandler.chart.getXYPlot().getDomainAxis().setLabelPaint(foregroundColor);
            chartHandler.chart.getXYPlot().getRangeAxis().setLabelPaint(foregroundColor);
            chartHandler.chart.getXYPlot().getDomainAxis().setTickLabelPaint(foregroundColor);
            chartHandler.chart.getXYPlot().getRangeAxis().setTickLabelPaint(foregroundColor);



            Color marginBackgroundColor = SpectrumViewController.PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_DEFAULT;
            chartHandler.chart.setBackgroundPaint(marginBackgroundColor);

            Color legendBackgroundColor = SpectrumViewController.PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_DEFAULT;
            chartHandler.chart.getLegend().setBackgroundPaint(legendBackgroundColor);


            chartHandler.setGridVisible(SpectrumViewController.getPreferenceGridlinesShow());
            showGridButton.setSelected(SpectrumViewController.getPreferenceGridlinesShow());

            rangeXLowerLabel.setEnabled(rangeXCheckBox.isSelected());
            rangeXLowerTextField.setEnabled(rangeXCheckBox.isSelected());
            rangeXUpperLabel.setEnabled(rangeXCheckBox.isSelected());
            rangeXUpperTextField.setEnabled(rangeXCheckBox.isSelected());
            rangeYLowerLabel.setEnabled(rangeYCheckBox.isSelected());
            rangeYLowerTextField.setEnabled(rangeYCheckBox.isSelected());
            rangeYUpperLabel.setEnabled(rangeYCheckBox.isSelected());
            rangeYUpperTextField.setEnabled(rangeYCheckBox.isSelected());
        } catch (Exception e) {
        }
        loadingPreferences = false;


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

    private  void recreateChart() {
        // System.out.println("INSIDE: recreateChart()");

        recreateChart(false);
    }




    private void runProgressMonitorForCursor() {
        printDebugMsg("INSIDE: runProgressMonitorForCursor - PROGRESS 1");

        ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(SnapApp.getDefault().getMainFrame(),
                "Collecting Spectral Data for cursor") {

            @Override
            protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                totalWorkPlannedMaster = 100;
                workDoneMaster = 0;
                pm.beginTask("Collecting spectral data: this can take several minutes on larger files", totalWorkPlannedMaster);

                try {
                    printDebugMsg("INSIDE: runProgressMonitorForCursor - PROGRESS 2");

                    updateData(0, 0, 0, true, pm, (totalWorkPlannedMaster - 10));
                    chartHandler.setEmptyPlot();

                    if (pm != null && pm.isCanceled()) {
                        cancelActions();
                        pm.done();
                        return null;
                    }

                    chartPanel.repaint();

                    updateUIState();
                    printDebugMsg("INSIDE: runProgressMonitorForCursor - PROGRESS 3");

                } finally {
                    printDebugMsg("INSIDE: runProgressMonitorForCursor - PROGRESS Finally");

                    if (pm != null && pm.isCanceled()) {
                        cancelActions();
                        return null;
                    }
                    pm.done();
                }

                printDebugMsg("INSIDE: runProgressMonitorForCursor - PROGRESS END");

                return null;
            }
        };

        pmSwingWorker.executeWithBlocking();
        printDebugMsg("INSIDE: runProgressMonitorForCursor - PROGRESS END2 After Blocking");

    }


    private void recreateChart(boolean showProgress) {
        if (recreatingChart) {
            // waiting
            printDebugMsg("WARNING!!!! need to wait");
        }
        recreatingChart = true;
        printDebugMsg("INSIDE: recreateChart(boolean showProgress)");
        // System.out.println("SnapApp.getDefault().getInstanceName() = " + SnapApp.getDefault().getInstanceName() );

        if (showProgress) {
            printDebugMsg("INSIDE: recreateChart(boolean showProgress) - PROGRESS 1");

            ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(SnapApp.getDefault().getMainFrame(),
                    "Collecting Spectral Data for pins") {

                @Override
                protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                    totalWorkPlannedMaster = 100;
                    workDoneMaster = 0;
                    if (pm == null) {
                        return null;
                    }

                    pm.beginTask("Collecting spectral data: this can take several minutes on larger files", totalWorkPlannedMaster);


                    try {
                        printDebugMsg("INSIDE: recreateChart(boolean showProgress) - PROGRESS 2");

                        // Allow progress monitor to fill up to 95% -- to prevent its premature closing
                        int work95Percent = (int) Math.floor(totalWorkPlannedMaster * 0.95);
                        chartHandler.updateData(pm, work95Percent);
                        if (pm.isCanceled()) {
                            cancelActions();
                            pm.done();
                            return null;
                        }
                        printDebugMsg("INSIDE: recreateChart(boolean showProgress) - PROGRESS 3");

                        chartHandler.updateChart();

                        updateChart(true);
                        if (workDoneMaster > totalWorkPlannedMaster) {
                            pm.worked(1);
                        }

                        printDebugMsg("INSIDE: recreateChart(boolean showProgress) - PROGRESS 4");

                        chartPanel.repaint();
                        if (workDoneMaster > totalWorkPlannedMaster) {
                            pm.worked(1);
                        }

                        updateUIState();

                        printDebugMsg("INSIDE: recreateChart(boolean showProgress) - PROGRESS 5");

                    } finally {
                        printDebugMsg("INSIDE: recreateChart(boolean showProgress) - PROGRESS Finally");

                        if (pm.isCanceled()) {
                            cancelActions();
                            return null;
                        }
                        pm.done();
                    }

                    printDebugMsg("INSIDE: recreateChart(boolean showProgress) - PROGRESS END");

                    return null;
                }
            };

            pmSwingWorker.executeWithBlocking();
            printDebugMsg("INSIDE: recreateChart(boolean showProgress) - PROGRESS END after blocking");


        } else {
            printDebugMsg("INSIDE: recreateChart(boolean showProgress) - NO PROGRESS");

            chartHandler.updateData();
            chartHandler.updateChart();
            chartPanel.repaint();
            updateUIState();
            printDebugMsg("INSIDE: recreateChart(boolean showProgress) - NO PROGRESS END");
        }

        recreatingChart = false;

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
        printDebugMsg("setUpSpectra START");
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
                if (currentProduct.getName().contains("SPEX")) {
                    availableSpectralBand.setSelected(false);
                }
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
        rasterToSpectraMap.put(raster, spectra.toArray(new DisplayableSpectrum[0]));
        printDebugMsg("setUpSpectra FINISH");
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


    protected void cancelActions() {
        showSpectraForAllPinsButton.setSelected(false);
        showSpectrumForCursorButton.setSelected(false);
        showSpectraForSelectedPinsButton.setSelected(false);

        chartHandler.setEmptyPlot();
        removeCursorSpectraFromDataset();
        updateUIState();
    }

    @Override
    protected void productSceneViewSelected(ProductSceneView view) {
        if (spectrumViewToolIsOpen) {
            showSpectraForAllPinsButton.setSelected(false);
            showSpectrumForCursorButton.setSelected(false);
            showSpectraForSelectedPinsButton.setSelected(false);
            loadingPreferences = false;


            // System.out.println("Listening: productSceneViewSelected");
            view.addPixelPositionListener(pixelPositionListener);
            setCurrentView(view);
            updateChart(true);

        }
    }


    @Override
    protected void productSceneViewDeselected(ProductSceneView view) {
        if (spectrumViewToolIsOpen) {
            showSpectraForAllPinsButton.setSelected(false);
            showSpectrumForCursorButton.setSelected(false);
            showSpectraForSelectedPinsButton.setSelected(false);
            loadingPreferences = false;


            // System.out.println("Listening: productSceneViewDeselected");
            view.removePixelPositionListener(pixelPositionListener);
            setCurrentView(null);
            chartHandler.setEmptyPlot();
        }
    }

    @Override
    protected void componentOpened() {
        if (this.wasOpenedBefore) {
            // commenting out as this gets handled in the setCurrentView method
//            setUpSpectra();
        } else {
            this.wasOpenedBefore = true;
        }

        loadingPreferences = false;
        // System.out.println("Listening: componentOpened");
        spectrumViewToolIsOpen = true;

        showSpectraForAllPinsButton.setSelected(false);
        showSpectrumForCursorButton.setSelected(false);
        showSpectraForSelectedPinsButton.setSelected(false);

        final ProductSceneView selectedProductSceneView = getSelectedProductSceneView();
        if (selectedProductSceneView != null) {


            selectedProductSceneView.addPixelPositionListener(pixelPositionListener);
            setCurrentView(selectedProductSceneView);
//            loadPreferences();
//            updateChart(true);
        }

        loadPreferences();
        updateChart(true);
    }

    @Override
    protected void componentClosed() {
        // System.out.println("Listening: componentClosed");
        spectrumViewToolIsOpen = false;
        loadingPreferences = false;


        showSpectraForAllPinsButton.setSelected(false);
        showSpectrumForCursorButton.setSelected(false);
        showSpectraForSelectedPinsButton.setSelected(false);

        if (currentView != null) {
            currentView.removePixelPositionListener(pixelPositionListener);
            setCurrentView(null);
        }

        chartHandler.setEmptyPlot();
        removeCursorSpectraFromDataset();
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
        private static final String MESSAGE_COLLECTING_SPECTRAL_INFORMATION = "Collecting data (possible memory limitations)...";
        private static final String MESSAGE_CURSOR_MODE_OFF = "Cursor Mode de-activated.";
        private static final String MESSAGE_CURSOR_MODE_NAN = "Cursor Mode activated.\n  \nData likely masked out or NaN at cursor pixel position.";
        private static final String MESSAGE_CURSOR_NOT_ON_IMAGE = "Cursor Mode activated.\n  \nHover cursor over the image.";




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

            // todo this block gets overridden due to presence of min/max fields, keep it until it is decided whether code is still useful
//            if (userInducesAutomaticAdjustment) {
//                if (!isUserInducedAutomaticAdjustmentChosen) {
//                    isUserInducedAutomaticAdjustmentChosen = true;
//                    if (!isAutomaticDomainAdjustmentSet()) {
//                        plot.getDomainAxis().setAutoRange(true);
//                        domainAxisAdjustmentIsFrozen = false;
//                        adjustmentHasChanged = true;
//                    }
//                    if (!isAutomaticRangeAdjustmentSet()) {
//                        plot.getRangeAxis().setAutoRange(true);
//                        rangeAxisAdjustmentIsFrozen = false;
//                        adjustmentHasChanged = true;
//                    }
//                }
//            } else {
//                if (isUserInducedAutomaticAdjustmentChosen) {
//                    isUserInducedAutomaticAdjustmentChosen = false;
//                    if (isAutomaticDomainAdjustmentSet()) {
//                        plot.getDomainAxis().setAutoRange(false);
//                        domainAxisAdjustmentIsFrozen = false;
//                        adjustmentHasChanged = true;
//                    }
//                    if (isAutomaticRangeAdjustmentSet()) {
//                        plot.getRangeAxis().setAutoRange(false);
//                        rangeAxisAdjustmentIsFrozen = false;
//                        adjustmentHasChanged = true;
//                    }
//                }
//            }


            //todo this block overrides previous block

            domainAxisAdjustmentIsFrozen = false;
            rangeAxisAdjustmentIsFrozen = false;

            if (rangeXCheckBox.isSelected()) {
                try {
                    boolean validLower = false;
                    boolean validUpper = false;
                    if (rangeXLowerTextField.getText() != null && rangeXLowerTextField.getText().trim().length() > 0) {
                        rangeXLower = Double.parseDouble(rangeXLowerTextField.getText());
                        validLower = true;
                    }

                    if (rangeXUpperTextField.getText() != null && rangeXUpperTextField.getText().trim().length() > 0) {
                        rangeXUpper = Double.parseDouble(rangeXUpperTextField.getText());
                        validUpper = true;
                    }

                    if (validLower && validUpper) {
                        plot.getDomainAxis().setRange(rangeXLower, rangeXUpper);
                    } else {
                        plot.getDomainAxis().setAutoRange(true);
                        if (validLower) {
                            plot.getDomainAxis().setLowerBound(rangeXLower);
                        }
                        if (validUpper) {
                            plot.getDomainAxis().setUpperBound(rangeXUpper);
                        }
                    }
                    adjustmentHasChanged = true;
                } catch (Exception e) {
                }
            } else {
                plot.getDomainAxis().setAutoRange(true);
                adjustmentHasChanged = true;
            }



            if (rangeYCheckBox.isSelected()) {
                try {
                    boolean validLower = false;
                    boolean validUpper = false;
                    if (rangeYLowerTextField.getText() != null && rangeYLowerTextField.getText().trim().length() > 0) {
                        rangeYLower = Double.parseDouble(rangeYLowerTextField.getText());
                        validLower = true;
                    }

                    if (rangeYUpperTextField.getText() != null && rangeYUpperTextField.getText().trim().length() > 0) {
                        rangeYUpper = Double.parseDouble(rangeYUpperTextField.getText());
                        validUpper = true;
                    }

                    if (validLower && validUpper) {
                        plot.getRangeAxis().setRange(rangeYLower, rangeYUpper);
                    } else {
                        plot.getRangeAxis().setAutoRange(true);
                        if (validLower) {
                            plot.getRangeAxis().setLowerBound(rangeYLower);
                        }
                        if (validUpper) {
                            plot.getRangeAxis().setUpperBound(rangeYUpper);
                        }
                    }
                    adjustmentHasChanged = true;
                } catch (Exception e) {
                }
            } else {
                plot.getRangeAxis().setAutoRange(true);
                adjustmentHasChanged = true;
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
//            chart.getXYPlot().clearAnnotations();
        }


        private void updateData() {
            updateData(null, 0);
        }
        private void updateData(com.bc.ceres.core.ProgressMonitor pm, int totalWorkPlanned) {
            printDebugMsg("getSelectedSpectra: START");
            List<DisplayableSpectrum> spectra = getSelectedSpectra();
            printDebugMsg("getSelectedSpectra: FINISH");
            chartUpdater.updateData(chart, spectra, pm, totalWorkPlanned);
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

        public void setPlotMessage(String messageText) {
            chart.getXYPlot().clearAnnotations();
            if (messageText != null && messageText.trim().length() > 0) {
                TextTitle tt = new TextTitle(messageText);
                tt.setTextAlignment(HorizontalAlignment.LEFT);
                tt.setFont(chart.getLegend().getItemFont());
                tt.setPaint(new Color(0, 0, 150, 255));
                tt.setBackgroundPaint(new Color(240, 240, 240, 150));
                tt.setFrame(new BlockBorder(1,1,1,1, new Color(130, 130, 130, 150)));
                tt.setPosition(RectangleEdge.BOTTOM);
                tt.setPadding(10,10,10,10);
                tt.setToolTipText("Operational display message");
                XYTitleAnnotation message = new XYTitleAnnotation(0.5, 0.5, tt, RectangleAnchor.CENTER);
                chart.getXYPlot().addAnnotation(message);
            }
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

        public void setMessageCursorNotOnImage() {
            setPlotMessage(MESSAGE_CURSOR_NOT_ON_IMAGE);
        }

        public void setMessageCursorModeNan() {
            setPlotMessage(MESSAGE_CURSOR_MODE_NAN);
        }

        public void setMessageCursorModeOff() {
            setPlotMessage(MESSAGE_CURSOR_MODE_OFF);
        }



//        public void setPlotMessage2(String message) {
//            setPlotMessage(message);
//        }


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
            this.rasterPixelX = pixelX;
            this.rasterPixelY = pixelY;
            this.rasterLevel = level;
            this.pixelPosInRasterBounds = pixelPosInRasterBounds;
            final AffineTransform i2m = currentView.getBaseImageLayer().getImageToModelTransform(level);
            modelP = i2m.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), new Point2D.Double());
        }

        private void updateData(JFreeChart chart, List<DisplayableSpectrum> spectra, com.bc.ceres.core.ProgressMonitor pm, int totalWorkPlanned) {
            dataset = new XYSeriesCollection();

            if (rasterLevel >= 0) {

                printDebugMsg("clearPrepareForUpdateMessage");
//                clearPrepareForUpdateMessage();

//                if (getDisplayedPins().length > 0 && isShowingCursorSpectrum()) {
//                    totalWorkPlanned = (int) Math.floor(1.0 * totalWorkPlanned / 2.0);
//                }

                printDebugMsg("totalWorkPlanned=" + totalWorkPlanned);

                if (getDisplayedPins().length > 0) {
                printDebugMsg("fillDatasetWithPinSeries: START");
                fillDatasetWithPinSeries(spectra, dataset, chart, pm, totalWorkPlanned);
                printDebugMsg("fillDatasetWithPinSeries: FINISH");
                }

                if (pm != null && pm.isCanceled()) {
                    cancelActions();
                    return;
                }

                if (isShowingCursorSpectrum()) {
                    printDebugMsg("fillDatasetWithCursorSeries: START");
//                    fillDatasetWithCursorSeries(spectra, dataset, chart, pm, totalWorkPlanned);
                    fillDatasetWithCursorSeries(spectra, dataset, chart);
                    printDebugMsg("fillDatasetWithCursorSeries: FINISH");
                }

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
                final Range oldBounds = this.plotBounds[index];
                this.plotBounds[index] = getNewRange(newBounds, this.plotBounds[index], axisBounds);
                if (oldBounds != this.plotBounds[index]) {
                    axis.setRange(getNewPlotBounds(this.plotBounds[index]));
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

//            clearPrepareForUpdateMessage();

            boolean cursorOverScene = false;


            long start = System.currentTimeMillis();
            printDebugMsg("fillDatasetWithCursorSeries: START");

            int workDone2 = 0;
            int totalWorkPlanned2 = 100;
            if (isShowingCursorSpectrum() && currentView != null) {
//                clearPrepareForUpdateMessage();


                printDebugMsg("fillDatasetWithCursorSeries: START 2");


                int totalWorkPlannedPerSpectra = (int) Math.floor(1.0 * totalWorkPlanned2 / spectra.size());


                PerformanceParameters currentPerformanceParameters = ConfigurationOptimizer.getInstance().getActualPerformanceParameters();


                for (DisplayableSpectrum spectrum : spectra) {
                    printDebugMsg("fillDatasetWithCursorSeries: spectra");

                    XYSeries series = new XYSeries(spectrum.getName());
                    final Band[] spectralBands = spectrum.getSelectedBands();


                    int numBands = spectralBands.length;
                    double incrementLengthNumBands = (totalWorkPlannedPerSpectra > 0) ? (double) numBands / totalWorkPlannedPerSpectra : (double) numBands;
                    double nextIncrementFinishedBandCount = incrementLengthNumBands;
                    int bandCount = 0;

                    boolean plotDisplaySetEmpty = false;


                    if (!currentProduct.isMultiSize()) {

                        for (Band spectralBand : spectralBands) {
                            printDebugMsg("fillDatasetWithCursorSeries:  spectralBand=" + spectralBand.getName());
                            long finish = System.currentTimeMillis();
                            long timeElapsed = finish - start;

                            plotDisplaySetEmpty = timedPlotMessages(timeElapsed,  plotDisplaySetEmpty,   spectralBand,  currentPerformanceParameters, true, workDone2);

                            if (timeElapsed > 600000) {
                                // it is taking too long
                                showSpectrumForCursorButton.setSelected(false);
                                chart.getXYPlot().setDataset(null);
                                dataset.removeAllSeries();
                                setPlotChartMessage("Chart not generated due to excessive processing time");
                                return;
                            }

                            final float wavelength = spectralBand.getSpectralWavelength();
                            if (pixelPosInRasterBounds && isPixelValid(spectralBand, rasterPixelX, rasterPixelY, rasterLevel)) {
                                addToSeries(spectralBand, rasterPixelX, rasterPixelY, rasterLevel, series, wavelength);
                                showsValidCursorSpectra = true;
                            }
                            cursorOverScene = pixelPosInRasterBounds;

                            bandCount++;
                            if (bandCount > nextIncrementFinishedBandCount) {
                                printDebugMsg("workDone2=" + workDone2 + "  totalWorkPlanned2=" + totalWorkPlanned2);

                                if (workDone2 < totalWorkPlanned2) {
                                    workDone2++;
                                }

                                nextIncrementFinishedBandCount += incrementLengthNumBands;
                            }
                        }

                    } else {


                        for (Band spectralBand : spectralBands) {
                            printDebugMsg("fillDatasetWithCursorSeries:  MultiSize spectralBand=" + spectralBand.getName());


                            long finish = System.currentTimeMillis();
                            long timeElapsed = finish - start;

                            plotDisplaySetEmpty = timedPlotMessages(timeElapsed,  plotDisplaySetEmpty,   spectralBand,  currentPerformanceParameters, true, workDone2);

                            if (timeElapsed > 600000) {
                                // it is taking too long
                                showSpectrumForCursorButton.setSelected(false);
                                chart.getXYPlot().setDataset(null);
                                dataset.removeAllSeries();
                                setPlotChartMessage("Chart not generated due to excessive processing time");
                                return;
                            }


                            final float wavelength = spectralBand.getSpectralWavelength();
                            final AffineTransform i2m = spectralBand.getImageToModelTransform();
                            if (i2m.equals(currentView.getRaster().getImageToModelTransform())) {
                                if (pixelPosInRasterBounds && isPixelValid(spectralBand, rasterPixelX, rasterPixelY, rasterLevel)) {
                                    addToSeries(spectralBand, rasterPixelX, rasterPixelY, rasterLevel, series, wavelength);
                                    showsValidCursorSpectra = true;
                                    cursorOverScene = pixelPosInRasterBounds;
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
                                    cursorOverScene = true;
                                }
                            }

                            bandCount++;

                            if (bandCount > nextIncrementFinishedBandCount) {
                                printDebugMsg("workDone2=" + workDone2 + "  totalWorkPlanned2=" + totalWorkPlanned2);

                                if (workDone2 < totalWorkPlanned2) {
                                    workDone2++;
                                }

                                nextIncrementFinishedBandCount += incrementLengthNumBands;
                            }

                        }


                    }

                    updateRenderer(dataset.getSeriesCount(), Color.BLACK, spectrum, chart);

                    dataset.addSeries(series);
                }


                if (showsValidCursorSpectra) {
                    clearPrepareForUpdateMessage();
                } else {
                    if (cursorOverScene) {
                        setMessageCursorModeNan();
//                        setPlotChartMessage("Cursor Mode activated.\n  \nData likely masked out or NaN at cursor pixel position.");
                    } else {
                        setMessageCursorNotOnImage();
//                        setPlotChartMessage("Cursor Mode activated.\n  \nHover cursor over the image.");
                    }
                }
            }

//            clearPrepareForUpdateMessage();

        }


        private boolean timedPlotMessages(long timeElapsed, boolean plotDisplaySetEmpty, Band  band, PerformanceParameters currentPerformanceParameters, boolean isCursorMode, int workdone) {

            int tileSize = currentPerformanceParameters.getDefaultTileSize();
            int cacheSize = currentPerformanceParameters.getCacheSize();
            long vmXMX = currentPerformanceParameters.getVmXMX();
            int width = band.getRasterSize().width;
            int height = band.getRasterSize().height;
            double sceneArea = (double) width * (double) height;
            double tileArea = (double) tileSize * (double) tileSize;
            String tileSizeNotice = "";
            if (tileArea > (sceneArea / 4.0)) {
                tileSizeNotice = "Note: reducing Tile Size may improve performance\n";
            } else if (tileArea < (sceneArea/20.0)) {
                tileSizeNotice = "Note: increasing Tile Size may improve performance\n";
            }


            if (timeElapsed > 500 && !plotDisplaySetEmpty) {
//                                chart.getXYPlot().setDataset(null);
                chartHandler.setEmptyPlot();
                plotDisplaySetEmpty = true;
//                                clearPrepareForUpdateMessage();
            }

            String workdoneStr = workdone + "% " ;
            for (int i = 0; i < 100; i +=5) {
                if (i < workdone) {
                    workdoneStr += "-";
                }
            }

            String msg = "";
            if (isCursorMode) {
                msg = "Please maintain current mouse hover position until fully completed.\n" +
                        "Spectral data is being initialized for tile at cursor hover position.";
            } else {
                msg = "Spectral data is being initialized for pinned tiles.";
            }


            if (timeElapsed > 500) {
                setPlotChartMessage(
                        "Processing spectral band: " + band.getName() + "   Progress: "  + workdoneStr + "\n  \n" +
                                msg + "\n  \n" +
                                "Note: see 'Performance Preferences' to optimize tile sizes relative to scene size.\n" +
                                tileSizeNotice +
                                "Currently:" +  "\n" +
                                "Virtual Memory - Vmx (MB): " + vmXMX  + "\n" +
                                "Cache Size (MB): " + cacheSize + "\n" +
                                "Tile Size: (pixels): " + tileSize + "\n" +
                                "Scene Width (pixels): " + currentView.getRaster().getRasterWidth() + "\n" +
                                "Scene Height (pixels): " + currentView.getRaster().getRasterHeight()
                );
            }

            return plotDisplaySetEmpty;

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

        private void fillDatasetWithPinSeries(List<DisplayableSpectrum> spectra, XYSeriesCollection dataset, JFreeChart chart, com.bc.ceres.core.ProgressMonitor pm, int totalWorkPlanned2) {
            Placemark[] pins = getDisplayedPins();

            // it seems like most of the work is done on first pin so setting this to false for now
            boolean splitWorkAcrossPins = false;
            if (splitWorkAcrossPins) {
                totalWorkPlanned2 = (int) Math.floor(1.0 * totalWorkPlanned2 / pins.length);
            }

            printDebugMsg("Number of pins =" + pins.length);
            printDebugMsg("(For each pin) totalWorkPlanned=" + totalWorkPlanned2);

            for (Placemark pin : pins) {
                printDebugMsg("Processing a pin");
                List<XYSeries> pinSeries = createXYSeriesFromPin(pin, dataset.getSeriesCount(), spectra, chart, pm, totalWorkPlanned2);
                if (pm != null && pm.isCanceled()) {
                    cancelActions();
                    return;
                }

                printDebugMsg("Processing a pin (PART 2)");
                pinSeries.forEach(dataset::addSeries);
            }
        }

        private List<XYSeries> createXYSeriesFromPin(Placemark pin, int seriesIndex, List<DisplayableSpectrum> spectra, JFreeChart chart, com.bc.ceres.core.ProgressMonitor pm, int totalWorkPlanned2) {
            List<XYSeries> pinSeries = new ArrayList<>();
            Color pinColor = PlacemarkUtils.getPlacemarkColor(pin, currentView);

            int workDone2 = 0;

            int totalWorkPlannedPerSpectra = (int) Math.floor(1.0 * totalWorkPlanned2 / spectra.size());
            printDebugMsg("createXYSeriesFromPin: spectra.size()=" + spectra.size());
            printDebugMsg("createXYSeriesFromPin: totalWorkPlannedPerSpectra=" + totalWorkPlannedPerSpectra);


            long start = System.currentTimeMillis();
            boolean plotDisplaySetEmpty = false;
            PerformanceParameters currentPerformanceParameters = ConfigurationOptimizer.getInstance().getActualPerformanceParameters();


            clearPrepareForUpdateMessage();


            for (DisplayableSpectrum spectrum : spectra) {
                if (pm != null && pm.isCanceled()) {
                    cancelActions();
                    return null;
                }
                XYSeries series = new XYSeries(spectrum.getName() + "_" + pin.getLabel());
                final Band[] spectralBands = spectrum.getSelectedBands();
                Map<Band, Double> bandToEnergy;
                if (pinToEnergies.containsKey(pin)) {
                    bandToEnergy = pinToEnergies.get(pin);
                } else {
                    bandToEnergy = new HashMap<>();
                    pinToEnergies.put(pin, bandToEnergy);
                }

                int numBands = spectralBands.length;
                printDebugMsg("createXYSeriesFromPin: numBands=" + numBands);

                double incrementLengthNumBands = (totalWorkPlannedPerSpectra > 0) ? (double) numBands / totalWorkPlannedPerSpectra : (double) numBands;
                printDebugMsg("createXYSeriesFromPin: incrementLengthNumBands=" + incrementLengthNumBands);

                double nextIncrementFinishedBandCount = incrementLengthNumBands;
                int bandCount = 0;


                for (Band spectralBand : spectralBands) {
                    printDebugMsg("createXYSeriesFromPin: spectralBand=" + spectralBand);

                    long finish = System.currentTimeMillis();
                    long timeElapsed = finish - start;

                    plotDisplaySetEmpty = timedPlotMessages(timeElapsed,  plotDisplaySetEmpty,   spectralBand,  currentPerformanceParameters, false, workDone2);

//
//                    if (timeElapsed > 500) {
//                        setPlotChartMessage("Processing band=" + spectralBand.getName());
//                    }



                    if (pm != null && pm.isCanceled()) {
                        cancelActions();
                        return null;
                    }
                    double energy;
                    if (bandToEnergy.containsKey(spectralBand)) {
                        printDebugMsg("createXYSeriesFromPin: bandToEnergy.containsKey=" + spectralBand);
                        energy = bandToEnergy.get(spectralBand);
                    } else {
                        energy = readEnergy(pin, spectralBand);
                        printDebugMsg("createXYSeriesFromPin: !bandToEnergy.containsKey=" + spectralBand);
                        bandToEnergy.put(spectralBand, energy);
                    }
                    final float wavelength = spectralBand.getSpectralWavelength();
                    if (energy != spectralBand.getGeophysicalNoDataValue()) {
                        printDebugMsg("createXYSeriesFromPin: series.add(wavelength, energy)" + wavelength);
                        series.add(wavelength, energy);
                    }

                    bandCount++;

                    if (bandCount > nextIncrementFinishedBandCount) {
                        printDebugMsg("createXYSeriesFromPin: workDoneMaster=" + workDoneMaster + "  totalWorkPlannedMaster=" + totalWorkPlannedMaster);
                        printDebugMsg("createXYSeriesFromPin: workDone2=" + workDone2 + "  totalWorkPlanned2=" + totalWorkPlanned2);

                        if (workDoneMaster < totalWorkPlannedMaster && workDone2 < totalWorkPlanned2) {
                            if (totalWorkPlanned2 != 0) {
                                if (pm != null) {pm.worked(1);}
                            }
                            workDoneMaster++;
                            workDone2++;
                        }

                        nextIncrementFinishedBandCount += incrementLengthNumBands;
                    }

                }

                clearPrepareForUpdateMessage();


                if (pm != null && pm.isCanceled()) {
                    cancelActions();
                    return null;
                }

                printDebugMsg("createXYSeriesFromPin: test1");

                updateRenderer(seriesIndex, pinColor, spectrum, chart);
                seriesIndex++;
                if (pm != null && pm.isCanceled()) {
                    cancelActions();
                    return null;
                }
                printDebugMsg("createXYSeriesFromPin: test2");

                pinSeries.add(series);
                printDebugMsg("createXYSeriesFromPin: test3");

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

    static void printDebugMsg(String msg) {
        boolean debugOn = false;
        if (debugOn) {
            System.out.println(msg);
        }
    }


}
