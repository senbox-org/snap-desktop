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
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.math.Array;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.help.HelpAction;
import org.esa.snap.rcp.placemark.PlacemarkUtils;
import org.esa.snap.rcp.preferences.general.SpectrumViewController;
import org.esa.snap.rcp.statistics.XYPlotMarker;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.PackageDefaults;
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

import javax.media.jai.ImageLayout;
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

    private final Map<RasterDataNode, DisplayableSpectrum[]> rasterToSpectraMap;
    private final Map<RasterDataNode, List<SpectrumBand>> rasterToSpectralBandsMap;

    private final ProductNodeListenerAdapter productNodeHandler;
    private final PinSelectionChangeListener pinSelectionChangeListener;
    private final PixelPositionListener pixelPositionListener;

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

    public SpectrumTopComponent() {
        // System.out.println("Spectrum View Tool is Open");
        spectrumViewToolIsOpen = false;

        tipShown = true;
        productNodeHandler = new ProductNodeHandler();
        pinSelectionChangeListener = new PinSelectionChangeListener();
        rasterToSpectraMap = new HashMap<>();
        rasterToSpectralBandsMap = new HashMap<>();
        pixelPositionListener = new CursorSpectrumPixelPositionListener(this);
        initUI();
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
                final DisplayableSpectrum spectrum = new DisplayableSpectrum("Bands measured in " + unit, symbolIndex++);
                spectrum.setLineStyle(SpectrumStrokeProvider.getStroke(strokeIndex++));
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
        boolean hasProduct = getCurrentProduct() != null;
        boolean hasSelectedPins = hasView && currentView.getSelectedPins().length > 0;
        boolean hasPins = hasProduct && getCurrentProduct().getPinGroup().getNodeCount() > 0;
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

        rangeXLowerLabel = new JLabel(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_LABEL);
        rangeXLowerTextField = new JTextField("1234567890");
        rangeXLowerTextField.setMinimumSize(rangeXLowerTextField.getPreferredSize());
        rangeXLowerTextField.setPreferredSize(rangeXLowerTextField.getPreferredSize());

        rangeXUpperLabel = new JLabel(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_LABEL);
        rangeXUpperTextField = new JTextField("1234567890");
        rangeXUpperTextField.setMinimumSize(rangeXUpperTextField.getPreferredSize());
        rangeXUpperTextField.setPreferredSize(rangeXUpperTextField.getPreferredSize());
        rangeXUpperTextField.setText(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_DEFAULT);



        rangeYCheckBox = new JCheckBox(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_LABEL);

        rangeYLowerLabel = new JLabel(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_LABEL);
        rangeYLowerTextField = new JTextField("1234567890");
        rangeYLowerTextField.setMinimumSize(rangeYLowerTextField.getPreferredSize());
        rangeYLowerTextField.setPreferredSize(rangeYLowerTextField.getPreferredSize());

        rangeYUpperLabel = new JLabel(SpectrumViewController.PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_LABEL);
        rangeYUpperTextField = new JTextField("1234567890");
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

        showSpectrumForCursorButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/CursorSpectrum24.gif"), true);
        showSpectrumForCursorButton.addActionListener(e -> {
            if (showSpectrumForCursorButton.isSelected()) {
                // System.out.println("Listening to showSpectrumForCursorButton - true");
                runProgressMonitorForCursor();
            } else {
                // System.out.println("Listening to showSpectrumForCursorButton - false");
                recreateChart();
            }
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
        if (spectrumChooser.show() == ModalDialog.ID_OK) {
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
        // System.out.println("INSIDE: runProgressMonitorForCursor - PROGRESS 1");

        ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(SnapApp.getDefault().getMainFrame(),
                "Collecting Spectral Data") {

            @Override
            protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                int totalWorkPlanned = 100;
                pm.beginTask("Collecting spectral data: this can take several minutes on larger files", totalWorkPlanned);

                try {
                    // System.out.println("INSIDE: runProgressMonitorForCursor - PROGRESS 2");

                    updateData(0, 0, 0, true, pm, (totalWorkPlanned - 10));
                    chartHandler.setEmptyPlot();

                    if (pm != null && pm.isCanceled()) {
                        cancelActions();
                        pm.done();
                        return null;
                    }

                    chartPanel.repaint();

                    updateUIState();
                    // System.out.println("INSIDE: runProgressMonitorForCursor - PROGRESS 3");

                } finally {
                    // System.out.println("INSIDE: runProgressMonitorForCursor - PROGRESS Finally");

                    if (pm != null && pm.isCanceled()) {
                        cancelActions();
                        return null;
                    }
                    pm.done();
                }

                // System.out.println("INSIDE: runProgressMonitorForCursor - PROGRESS END");

                return null;
            }
        };

        pmSwingWorker.executeWithBlocking();
        // System.out.println("INSIDE: runProgressMonitorForCursor - PROGRESS END2");

    }


    private void recreateChart(boolean showProgress) {
        // System.out.println("INSIDE: recreateChart(boolean showProgress)");
        // System.out.println("SnapApp.getDefault().getInstanceName() = " + SnapApp.getDefault().getInstanceName() );

        if (showProgress) {
            // System.out.println("INSIDE: recreateChart(boolean showProgress) - PROGRESS 1");

            ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(SnapApp.getDefault().getMainFrame(),
                    "Collecting Spectral Data") {

                @Override
                protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                    int totalWorkPlanned = 100;
                    pm.beginTask("Collecting spectral data: this can take several minutes on larger files", totalWorkPlanned);


                    try {
                        // System.out.println("INSIDE: recreateChart(boolean showProgress) - PROGRESS 2");

                        chartHandler.updateData(pm,(totalWorkPlanned - 10));
                        if (pm != null && pm.isCanceled()) {
                            cancelActions();
                            pm.done();
                            return null;
                        }

                        chartHandler.updateChart();

                        updateChart(true);
                        if (pm != null) {
                            pm.worked(1);
                        }
                        chartPanel.repaint();
                        if (pm != null) {
                            pm.worked(1);
                        }
                        updateUIState();
                        // System.out.println("INSIDE: recreateChart(boolean showProgress) - PROGRESS 3");

                    } finally {
                        // System.out.println("INSIDE: recreateChart(boolean showProgress) - PROGRESS Finally");

                        if (pm != null && pm.isCanceled()) {
                            cancelActions();
                            return null;
                        }
                        pm.done();
                    }

                    // System.out.println("INSIDE: recreateChart(boolean showProgress) - PROGRESS END");

                    return null;
                }
            };

            pmSwingWorker.executeWithBlocking();
            // System.out.println("INSIDE: recreateChart(boolean showProgress) - PROGRESS END2");


        } else {
            // System.out.println("INSIDE: recreateChart(boolean showProgress) - NO PROGRESS");

            chartHandler.updateData();
            chartHandler.updateChart();
            chartPanel.repaint();
            updateUIState();
            // System.out.println("INSIDE: recreateChart(boolean showProgress) - NO PROGRESS END");
        }
    }


    Placemark[] getDisplayedPins() {
        if (isShowingSpectraForSelectedPins() && currentView != null) {
            return currentView.getSelectedPins();
        } else if (isShowingSpectraForAllPins() && getCurrentProduct() != null) {
            ProductNodeGroup<Placemark> pinGroup = getCurrentProduct().getPinGroup();
            return pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
        } else {
            return new Placemark[0];
        }
    }

    private void setUpSpectra() {
        if (currentView == null) {
            return;
        }
        DisplayableSpectrum[] spectra;
        final RasterDataNode raster = currentView.getRaster();
        final SpectrumBand[] availableSpectralBands = getAvailableSpectralBands(raster);
        if (availableSpectralBands.length == 0) {
            spectra = new DisplayableSpectrum[]{};
        } else {
            final Product.AutoGrouping autoGrouping = currentProduct.getAutoGrouping();
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
                    int symbolIndex = SpectrumShapeProvider.getValidIndex(i, false);
                    DisplayableSpectrum spectrum = new DisplayableSpectrum(spectrumName, symbolIndex);
                    spectrum.setSelected(i == selectedSpectrumIndex);
                    spectrum.setLineStyle(SpectrumStrokeProvider.getStroke(i));
                    autoGroupingSpectra[i++] = spectrum;
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
                if (ungroupedBandsList.size() == 0) {
                    spectra = autoGroupingSpectra;
                } else {
                    final DisplayableSpectrum[] spectraFromUngroupedBands =
                            createSpectraFromUngroupedBands(ungroupedBandsList.toArray(new SpectrumBand[0]),
                                    SpectrumShapeProvider.getValidIndex(i, false), i);
                    spectra = new DisplayableSpectrum[autoGroupingSpectra.length + spectraFromUngroupedBands.length];
                    System.arraycopy(autoGroupingSpectra, 0, spectra, 0, autoGroupingSpectra.length);
                    System.arraycopy(spectraFromUngroupedBands, 0, spectra, autoGroupingSpectra.length, spectraFromUngroupedBands.length);
                }
            } else {
                spectra = createSpectraFromUngroupedBands(availableSpectralBands, 1, 0);
            }
        }
        rasterToSpectraMap.put(raster, spectra);
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
            chart.getXYPlot().clearAnnotations();
        }


        private void updateData() {
            updateData(null, 0);
        }
        private void updateData(com.bc.ceres.core.ProgressMonitor pm, int totalWorkPlanned) {
            List<DisplayableSpectrum> spectra = getSelectedSpectra();
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

                if (getDisplayedPins().length > 0 && isShowingCursorSpectrum()) {
                    totalWorkPlanned = totalWorkPlanned/2;
                }

                // System.out.println("totalWorkPlanned=" + totalWorkPlanned);

                fillDatasetWithPinSeries(spectra, dataset, chart, pm, totalWorkPlanned);
                // System.out.println("Finish fillDatasetWithPinSeries");

                if (pm != null && pm.isCanceled()) {
                    cancelActions();
                    return;
                }
                fillDatasetWithCursorSeries(spectra, dataset, chart, pm, totalWorkPlanned);
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
                    DisplayableSpectrum displayableSpectrum = spectra.get(i++);
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

        private void fillDatasetWithCursorSeries(List<DisplayableSpectrum> spectra, XYSeriesCollection dataset, JFreeChart chart, com.bc.ceres.core.ProgressMonitor pm, int totalWorkPlanned) {
            showsValidCursorSpectra = false;
            if (modelP == null) {
                return;
            }
            if (isShowingCursorSpectrum() && currentView != null) {
                int totalWorkPlannedPerSpectra = (int) Math.floor(1.0 * totalWorkPlanned / spectra.size());
                int workDone = 0;

                for (DisplayableSpectrum spectrum : spectra) {
                    XYSeries series = new XYSeries(spectrum.getName());
                    final Band[] spectralBands = spectrum.getSelectedBands();

                    int numBands = spectralBands.length;
                    double incrementLengthNumBands = (totalWorkPlannedPerSpectra > 0) ? numBands / totalWorkPlannedPerSpectra : numBands;
                    double nextIncrementFinishedBandCount = incrementLengthNumBands;
                    int bandCount = 0;

                    if (!currentProduct.isMultiSize()) {

                        for (Band spectralBand : spectralBands) {
                            if (pm != null && pm.isCanceled()) {
                                cancelActions();
                                return;
                            }
                            final float wavelength = spectralBand.getSpectralWavelength();
                            if (pixelPosInRasterBounds && isPixelValid(spectralBand, rasterPixelX, rasterPixelY, rasterLevel)) {
                                addToSeries(spectralBand, rasterPixelX, rasterPixelY, rasterLevel, series, wavelength);
                                showsValidCursorSpectra = true;
                            }

                            bandCount++;
                            if (workDone < totalWorkPlanned && bandCount > nextIncrementFinishedBandCount) {
                                if (totalWorkPlanned != 0) {
                                    if (pm != null) {pm.worked(1);}
                                }
                                nextIncrementFinishedBandCount += incrementLengthNumBands;
                                workDone++;
                            }
                        }
                    } else {
                        for (Band spectralBand : spectralBands) {
                            if (pm != null && pm.isCanceled()) {
                                cancelActions();
                                return;
                            }
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

                            bandCount++;
                            if (workDone < totalWorkPlanned && bandCount > nextIncrementFinishedBandCount) {
                                if (totalWorkPlanned != 0) {
                                    if (pm != null) {pm.worked(1);}
                                }
                                nextIncrementFinishedBandCount += incrementLengthNumBands;
                                workDone++;
                            }
                        }
                    }
                    if (pm != null && pm.isCanceled()) {
                        cancelActions();
                        return;
                    }
                    updateRenderer(dataset.getSeriesCount(), Color.BLACK, spectrum, chart);
                    if (pm != null && pm.isCanceled()) {
                        cancelActions();
                        return;
                    }
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

        private void fillDatasetWithPinSeries(List<DisplayableSpectrum> spectra, XYSeriesCollection dataset, JFreeChart chart, com.bc.ceres.core.ProgressMonitor pm, int totalWorkPlanned) {
            Placemark[] pins = getDisplayedPins();

            totalWorkPlanned = (int) Math.floor(1.0 * totalWorkPlanned / pins.length);
            // System.out.println("Number of pins =" + pins.length);
            // System.out.println("(For each pin) totalWorkPlanned=" + totalWorkPlanned);

            for (Placemark pin : pins) {
                List<XYSeries> pinSeries = createXYSeriesFromPin(pin, dataset.getSeriesCount(), spectra, chart, pm, totalWorkPlanned);
                if (pm != null && pm.isCanceled()) {
                    cancelActions();
                    return;
                }
                pinSeries.forEach(dataset::addSeries);
            }
        }

        private List<XYSeries> createXYSeriesFromPin(Placemark pin, int seriesIndex, List<DisplayableSpectrum> spectra, JFreeChart chart, com.bc.ceres.core.ProgressMonitor pm, int totalWorkPlanned) {
            List<XYSeries> pinSeries = new ArrayList<>();
            Color pinColor = PlacemarkUtils.getPlacemarkColor(pin, currentView);

            int totalWorkPlannedPerSpectra = (int) Math.floor(1.0 * totalWorkPlanned / spectra.size());
            int workDone = 0;
            // System.out.println("totalWorkPlannedPerSpectra=" + totalWorkPlannedPerSpectra);

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
                double incrementLengthNumBands = (totalWorkPlannedPerSpectra > 0) ? numBands / totalWorkPlannedPerSpectra : numBands;
                // System.out.println("incrementLengthNumBands=" + incrementLengthNumBands);
                double nextIncrementFinishedBandCount = incrementLengthNumBands;
                int bandCount = 0;

                for (Band spectralBand : spectralBands) {
                    if (pm != null && pm.isCanceled()) {
                        cancelActions();
                        return null;
                    }
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

                    bandCount++;
                    if (workDone < totalWorkPlanned && bandCount > nextIncrementFinishedBandCount) {
                        if (totalWorkPlanned != 0) {
                            if (pm != null) {pm.worked(1);}
                        }
                        nextIncrementFinishedBandCount += incrementLengthNumBands;
                        workDone++;
                    }
                }

                if (pm != null && pm.isCanceled()) {
                    cancelActions();
                    return null;
                }
                updateRenderer(seriesIndex++, pinColor, spectrum, chart);
                if (pm != null && pm.isCanceled()) {
                    cancelActions();
                    return null;
                }
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
                if (event.getPropertyName().equals("geoPos") || event.getPropertyName().equals("pixelPos")) {
                    chartHandler.removePinInformation((Placemark) event.getSourceNode());
                }
                if (isShowingPinSpectra()) {
                    chartHasChanged = true;
                }
            } else if (event.getSourceNode() instanceof Product) {
                if (event.getPropertyName().equals("autoGrouping")) {
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
            Product.AutoGrouping autoGrouping = currentProduct.getAutoGrouping();
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
