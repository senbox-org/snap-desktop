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
package org.esa.snap.rcp.angularview;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelModel;
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
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.angularview.*;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;

import static java.lang.Math.abs;

@TopComponent.Description(preferredID = "AngularTopComponent"
        , iconBase = "org/esa/snap/rcp/icons/AngularView.png")
@TopComponent.Registration(mode = "Statistics", openAtStartup = false, position = 80)
@ActionID(category = "Window", id = "org.esa.snap.rcp.AngularTopComponent")
@ActionReferences({
//        @ActionReference(path = "Menu/Optical", position = 0),
        @ActionReference(path = "Menu/View/Tool Windows/Angular"),
        @ActionReference(path = "Toolbars/Angular")
})
@TopComponent.OpenActionRegistration(displayName = "#CTL_AngularTopComponent_Name", preferredID = "AngularTopComponent")
@NbBundle.Messages({"CTL_AngularTopComponent_Name=Angular View", "CTL_AngularTopComponent_HelpId=showAngularViewWnd"})
/**
 * A window which displays AngularViews at selected pixel positions.
 */
public class AngularTopComponent extends ToolTopComponent {

    public static final String ID = AngularTopComponent.class.getName();

    private static final String SUPPRESS_MESSAGE_KEY = "plugin.spectrum.tip";

    private final Map<RasterDataNode, DisplayableAngularview[]> rasterToAngularMap;
    private final Map<RasterDataNode, List<AngularBand>> rasterToAngularBandsMap;

    private final ProductNodeListenerAdapter productNodeHandler;
    private final PinSelectionChangeListener pinSelectionChangeListener;
    private final PixelPositionListener pixelPositionListener;

    private AbstractButton filterButton;
    private AbstractButton showAngularViewsForCursorButton;
    private AbstractButton showAngularViewsForSelectedPinsButton;
    private AbstractButton showAngularViewsForAllPinsButton;
    private AbstractButton showGridButton;
    private JRadioButton useSensorZenithButton;
    private JRadioButton useSensorAzimuthButton;
    private JRadioButton useScatteringAngleButton;
    private JRadioButton useViewAngleButton;

    private boolean tipShown;
    private boolean useViewAngle;
    private boolean useSensorZenith;
    private boolean useSensorAzimuth;
    private boolean useScatteringAngle;
    private ProductSceneView currentView;
    private Product currentProduct;
    private ChartPanel chartPanel;
    private ChartHandler chartHandler;

    private boolean domainAxisAdjustmentIsFrozen;
    private boolean rangeAxisAdjustmentIsFrozen;
    private boolean isCodeInducedAxisChange;
    private boolean isUserInducedAutomaticAdjustmentChosen;

    public List<AngularBand> sensor_azimuth_Bands = new ArrayList<>();
    public List<AngularBand> sensor_zenith_Bands = new ArrayList<>();
    public List<AngularBand> scattering_angle_Bands = new ArrayList<>();

    public AngularTopComponent() {
        productNodeHandler = new ProductNodeHandler();
        pinSelectionChangeListener = new PinSelectionChangeListener();
        rasterToAngularMap = new HashMap<>();
        rasterToAngularBandsMap = new HashMap<>();
        pixelPositionListener = new CursorAngularViewPixelPositionListener(this);
        initUI();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_AngularTopComponent_HelpId());
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
                if (currentProduct.getName().contains("HARP2")) {
                    currentProduct.setAutoGrouping("I_*_549:I_*_669:I_*_867:I_*_441:Q_*_549:Q_*_669:Q_*_867:Q_*_441:" +
                            "U_*_549:U_*_669:U_*_867:U_*_441:DOLP_*_549:DOLP_*_669:DOLP_*_867:DOLP_*_441:" +
                            "I_noise_*_549:I_noise_*_669:I_noise_*_867:I_noise_*_441:Q_noise_*_549:Q_noise_*_669:Q_noise_*_867:Q_noise_*_441:" +
                            "U_noise_*_549:U_noise_*_669:U_noise_*_867:U_noise_*_441:DOLP_noise_*_549:DOLP_noise_*_669:DOLP_noise_*_867:DOLP_noise_*_441:" +
                            "Sensor_Zenith:Sensor_Azimuth:Solar_Zenith:Solar_Azimuth:obs_per_view:view_time_offsets");
                };
                if (currentProduct.getName().contains("SPEXONE")) {
                    String autoGroupingStr = "QC:QC_bitwise:QC_polsample_bitwise:QC_polsample:";
                    for (int wvl = 380; wvl < 390; wvl++) {
                        autoGroupingStr += "I_*_" + wvl + ":";
                    }
                    autoGroupingStr += "I:I_noise:I_noisefree:I_polsample:" +
                            "I_polsample_noise:I_noisefree_polsample:DOLP:DOLP_noise:DOLP_noisefree:" +
                            "Q_over_I:Q_over_I_noise:Q_over_I_noisefree:AOLP:AOLP_noisefree:" +
                            "U_over_I:U_over_I_noise:U_over_I_noisefree:scattering_angle:" +
                            "sensor_azimuth:sensor_zenith:solar_azimuth:solar_zenith:" +
                            "obs_per_view:view_time_offsets";
                    currentProduct.setAutoGrouping(autoGroupingStr);
//                    currentProduct.setAutoGrouping("QC:QC_bitwise:QC_polsample_bitwise:QC_polsample:" +
//                                                    "I_*_380:I_*_381:I_*_382:I_*_383:I_*_384:I_*_385:I:I_noise:I_noisefree:I_polsample:" +
//                                                    "I_polsample_noise:I_noisefree_polsample:DOLP:DOLP_noise:DOLP_noisefree:" +
//                                                    "Q_over_I:Q_over_I_noise:Q_over_I_noisefree:AOLP:AOLP_noisefree:" +
//                                                    "U_over_I:U_over_I_noise:U_over_I_noisefree:scattering_angle:" +
//                                                    "sensor_azimuth:sensor_zenith:solar_azimuth:solar_zenith:" +
//                                                    "obs_per_view:view_time_offsets");
                }
                if (!rasterToAngularMap.containsKey(currentView.getRaster())) {
                    setUpAngularViews();
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
        boolean hasProduct = getCurrentProduct() != null;
        boolean hasSelectedPins = hasView && currentView.getSelectedPins().length > 0;
        boolean hasPins = hasProduct && getCurrentProduct().getPinGroup().getNodeCount() > 0;
        filterButton.setEnabled(hasProduct);
        showAngularViewsForCursorButton.setEnabled(hasView);
        showAngularViewsForSelectedPinsButton.setEnabled(hasSelectedPins);
        showAngularViewsForAllPinsButton.setEnabled(hasPins);
        showGridButton.setEnabled(hasView);
        chartPanel.setEnabled(hasProduct);    // todo - hasAngularViewsGraphs
        showGridButton.setSelected(hasView);
        chartHandler.setGridVisible(showGridButton.isSelected());
    }

    void setPrepareForUpdateMessage() {
        chartHandler.setCollectingAngularInformationMessage();
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
            Dialogs.showInformation("Angular View Tip", message, SUPPRESS_MESSAGE_KEY);
            tipShown = true;
        }
    }

    private AngularBand[] getAvailableAngularBands(RasterDataNode currentRaster) {
        if (!rasterToAngularBandsMap.containsKey(currentRaster)) {
            rasterToAngularBandsMap.put(currentRaster, new ArrayList<>());
        }
        List<AngularBand> angularViewBands = rasterToAngularBandsMap.get(currentRaster);
        Band[] bands = currentProduct.getBands();
        for (Band band : bands) {
            if (isAngularBand(band) && !band.isFlagBand()) {
                boolean isAlreadyIncluded = false;
                for (AngularBand angularViewBand : angularViewBands) {
                    if (angularViewBand.getOriginalBand() == band) {
                        isAlreadyIncluded = true;
                        break;
                    }
                }
                if (!isAlreadyIncluded) {
                    angularViewBands.add(new AngularBand(band, true));
                }
            }
        }
        setScatteringZenithBands(angularViewBands);
        if (angularViewBands.isEmpty()) {
//            Dialogs.showWarning("<html>Angular View Tool <br>requires bands with View Angle property,<br>" +
//                    "such as PACE OCI, HARP2,and SPEXone bands</html>");
        }
        return angularViewBands.toArray(new AngularBand[angularViewBands.size()]);
    }

    private void setScatteringZenithBands(List<AngularBand> angularBands) {
        sensor_azimuth_Bands = new ArrayList<>();
        sensor_zenith_Bands = new ArrayList<>();
        scattering_angle_Bands = new ArrayList<>();

        for (AngularBand angularBand : angularBands) {
            if (angularBand.getName().contains("Sensor_Azimuth") || angularBand.getName().contains("sensor_azimuth")) {
                sensor_azimuth_Bands.add(angularBand);
            }
            if (angularBand.getName().contains("Sensor_Zenith") || angularBand.getName().contains("sensor_zenith")) {
                sensor_zenith_Bands.add(angularBand);
            }

            if (angularBand.getName().contains("scattering_angle")) {
                scattering_angle_Bands.add(angularBand);
            }
        }
        useScatteringAngleButton.setEnabled((scattering_angle_Bands.size() != 0));

        useViewAngleButton.setSelected((true));
    }

    private boolean isAngularBand(Band band) { return (band.getAngularBandIndex() != -1 );}

    private void initUI() {
        final JFreeChart chart = ChartFactory.createXYLineChart(Bundle.CTL_AngularTopComponent_Name(),
                "View Angle", "", null, PlotOrientation.VERTICAL,
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
            selectAngularBands();
            recreateChart();
        });

        showAngularViewsForCursorButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/CursorSpectrum24.gif"), true);
        showAngularViewsForCursorButton.addActionListener(e -> recreateChart());
        showAngularViewsForCursorButton.setName("showAngularViewsForCursorButton");
        showAngularViewsForCursorButton.setSelected(true);
        showAngularViewsForCursorButton.setToolTipText("Show angular views at cursor position.");

        showAngularViewsForSelectedPinsButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/SelectedPinSpectra24.gif"), true);
        showAngularViewsForSelectedPinsButton.addActionListener(e -> {
            if (isShowingAngularViewsForAllPins()) {
                showAngularViewsForAllPinsButton.setSelected(false);
            } else if (!isShowingAngularViewsForSelectedPins()) {
                plotMarker.setInvisible();
            }
            recreateChart();
        });
        showAngularViewsForSelectedPinsButton.setName("showAngularViewsForSelectedPinsButton");
        showAngularViewsForSelectedPinsButton.setToolTipText("Show AngularViews for selected pins.");

        showAngularViewsForAllPinsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/PinSpectra24.gif"),
                true);
        showAngularViewsForAllPinsButton.addActionListener(e -> {
            if (isShowingAngularViewsForSelectedPins()) {
                showAngularViewsForSelectedPinsButton.setSelected(false);
            } else if (!isShowingAngularViewsForAllPins()) {
                plotMarker.setInvisible();
            }
            recreateChart();
        });
        showAngularViewsForAllPinsButton.setName("showAngularViewsForAllPinsButton");
        showAngularViewsForAllPinsButton.setToolTipText("Show angular Views for all pins.");

        showGridButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/SpectrumGrid24.gif"), true);
        showGridButton.addActionListener(e -> chartHandler.setGridVisible(showGridButton.isSelected()));
        showGridButton.setName("showGridButton");
        showGridButton.setToolTipText("Show diagram grid.");

        useViewAngleButton = new JRadioButton("View Angle");
        useViewAngleButton.setToolTipText("Use View Angle as x-axis.");
        useViewAngleButton.setName("useViewAngleButton");
        useSensorZenithButton = new JRadioButton("Sensor Zenith");
        useSensorZenithButton.setToolTipText("Use Sensor Zenith Angle as x-axis.");
        useSensorZenithButton.setName("useSensorZenith");
        useSensorAzimuthButton = new JRadioButton("Sensor Azimuth");
        useSensorAzimuthButton.setToolTipText("Use Sensor Azimuth as x-axis.");
        useSensorAzimuthButton.setName("useuseSensorAzimuthButton");
        useScatteringAngleButton = new JRadioButton("Scattering Angle");
        useScatteringAngleButton.setToolTipText("Use Scattering Angle as x-axis.");
        useScatteringAngleButton.setName("useScatteringAngleButton");
        final ButtonGroup angleAxisGroup = new ButtonGroup();
        angleAxisGroup.add(useViewAngleButton);
        angleAxisGroup.add(useSensorZenithButton);
        angleAxisGroup.add(useSensorAzimuthButton);
        angleAxisGroup.add(useScatteringAngleButton);
        useViewAngleButton.setSelected(true);
        useViewAngleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useViewAngle = useViewAngleButton.isSelected();
                recreateChart();
            }
        });
        useSensorZenithButton.setSelected(false);
        useSensorZenithButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useSensorZenith = useSensorZenithButton.isSelected();
                recreateChart();
            }
        });
        useSensorAzimuthButton.setSelected(false);
        useSensorAzimuthButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useSensorAzimuth = useSensorAzimuthButton.isSelected();
                recreateChart();
            }
        });
        useScatteringAngleButton.setSelected(false);
        useScatteringAngleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useScatteringAngle = useScatteringAngleButton.isSelected();
                recreateChart();
            }
        });

        JPanel angleButtonPanel = new JPanel( new GridLayout(4, 1) );
        angleButtonPanel.add(useViewAngleButton);
        angleButtonPanel.add(useSensorZenithButton);
        angleButtonPanel.add(useSensorAzimuthButton);
        angleButtonPanel.add(useScatteringAngleButton);

        AbstractButton exportAngularViewsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Export24.gif"),
                false);
        exportAngularViewsButton.addActionListener(new AngularViewsExportAction(this));
        exportAngularViewsButton.setToolTipText("Export angular Views to text file.");
        exportAngularViewsButton.setName("exportAngularViewsButton");

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
        buttonPane.add(showAngularViewsForCursorButton, gbc);
        gbc.gridy++;
        buttonPane.add(showAngularViewsForSelectedPinsButton, gbc);
        gbc.gridy++;
        buttonPane.add(showAngularViewsForAllPinsButton, gbc);
        gbc.gridy++;
        buttonPane.add(showGridButton, gbc);
        gbc.gridy++;
        buttonPane.add(exportAngularViewsButton, gbc);
        gbc.gridy++;
        buttonPane.add(angleButtonPanel,gbc);
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
                    if (rasterToAngularMap.containsKey(currentRaster)) {
                        rasterToAngularMap.remove(currentRaster);
                    }
                    if (rasterToAngularBandsMap.containsKey(currentRaster)) {
                        rasterToAngularBandsMap.remove(currentRaster);
                    }
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
        setDisplayName(Bundle.CTL_AngularTopComponent_Name());
        setLayout(new BorderLayout());
        add(mainPane, BorderLayout.CENTER);
        updateUIState();
    }

    private void selectAngularBands() {
        final RasterDataNode currentRaster = currentView.getRaster();
        final DisplayableAngularview[] allAngularViews = rasterToAngularMap.get(currentRaster);
        final AngularViewChooser angularViewChooser = new AngularViewChooser(SwingUtilities.getWindowAncestor(this), allAngularViews);
        if (angularViewChooser.show() == ModalDialog.ID_OK) {
            final DisplayableAngularview[] angularViews = angularViewChooser.getAngularViews();
            rasterToAngularMap.put(currentRaster, angularViews);
        }
    }

    boolean isShowingCursorAngularView() {
        return showAngularViewsForCursorButton.isSelected();
    }

    private boolean isShowingPinAngularViews() {
        return isShowingAngularViewsForSelectedPins() || isShowingAngularViewsForAllPins();
    }

    private boolean isShowingAngularViewsForAllPins() {
        return showAngularViewsForAllPinsButton.isSelected();
    }

    private void recreateChart() {
        chartHandler.updateData();
        chartHandler.updateChart();
        chartPanel.repaint();
        updateUIState();
    }

    Placemark[] getDisplayedPins() {
        if (isShowingAngularViewsForSelectedPins() && currentView != null) {
            return currentView.getSelectedPins();
        } else if (isShowingAngularViewsForAllPins() && getCurrentProduct() != null) {
            ProductNodeGroup<Placemark> pinGroup = getCurrentProduct().getPinGroup();
            return pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
        } else {
            return new Placemark[0];
        }
    }

    private void setUpAngularViews() {
        if (currentView == null) {
            return;
        }
        DisplayableAngularview[] angularViews;
        final RasterDataNode raster = currentView.getRaster();
        final AngularBand[] availableAngularBands = getAvailableAngularBands(raster);
        if (availableAngularBands.length == 0) {
            angularViews = new DisplayableAngularview[]{};
        } else {
//            if (currentProduct.getName().contains("HARP2")) {
//                currentProduct.setAutoGrouping("I_*_549:I_*_669:I_*_867:I_*_441:Q_*_549:Q_*_669:Q_*_867:Q_*_441:" +
//                        "U_*_549:U_*_669:U_*_867:U_*_441:DOLP_*_549:DOLP_*_669:DOLP_*_867:DOLP_*_441:" +
//                        "I_noise_*_549:I_noise_*_669:I_noise_*_867:I_noise_*_441:Q_noise_*_549:Q_noise_*_669:Q_noise_*_867:Q_noise_*_441:" +
//                        "U_noise_*_549:U_noise_*_669:U_noise_*_867:U_noise_*_441:DOLP_noise_*_549:DOLP_noise_*_669:DOLP_noise_*_867:DOLP_noise_*_441:" +
//                        "Sensor_Zenith:Sensor_Azimuth:Solar_Zenith:Solar_Azimuth:obs_per_view:view_time_offsets");
//            }
            final Product.AutoGrouping autoGrouping = currentProduct.getAutoGrouping();
            if (autoGrouping != null) {
                final int selectedAngularViewIndex = autoGrouping.indexOf(raster.getName());
                DisplayableAngularview[] autoGroupingAngularViews = new DisplayableAngularview[autoGrouping.size()];
                final Iterator<String[]> iterator = autoGrouping.iterator();
                int i = 0;
                while (iterator.hasNext()) {
                    final String[] autoGroupingNameAsArray = iterator.next();
                    StringBuilder angularViewNameBuilder = new StringBuilder(autoGroupingNameAsArray[0]);
                    if (autoGroupingNameAsArray.length > 1) {
                        for (int j = 1; j < autoGroupingNameAsArray.length; j++) {
                            String autoGroupingNamePart = autoGroupingNameAsArray[j];
                            angularViewNameBuilder.append("_").append(autoGroupingNamePart);
                        }
                    }
                    final String angularViewName = angularViewNameBuilder.toString();
                    int symbolIndex = AngularViewShapeProvider.getValidIndex(i, false);
                    DisplayableAngularview angularView = new DisplayableAngularview(angularViewName, symbolIndex);
                    angularView.setSelected(i == selectedAngularViewIndex);
                    angularView.setLineStyle(AngularViewStrokeProvider.getStroke(i));
                    autoGroupingAngularViews[i++] = angularView;
                }
                List<AngularBand> ungroupedBandsList = new ArrayList<>();
                for (AngularBand availableAngularBand : availableAngularBands) {
                    final String bandName = availableAngularBand.getName();
                    availableAngularBand.setSelected(false);
                    if (currentProduct.getName().contains("SPEXONE")) {
                        if (bandName.contains("385") && availableAngularBand.getOriginalBand().getDescription().equals("I")) {
                            availableAngularBand.setSelected(true);
                        }
                    }
                    if (currentProduct.getName().contains("HARP2")) {
                        if (bandName.contains("549") && availableAngularBand.getOriginalBand().getDescription().equals("I")) {
                            availableAngularBand.setSelected(true);
                        }
                    }
                    final int angularViewIndex = autoGrouping.indexOf(bandName);
                    if (angularViewIndex != -1) {
                        autoGroupingAngularViews[angularViewIndex].addBand(availableAngularBand);
                    } else {
                        ungroupedBandsList.add(availableAngularBand);
                    }
                }
                if (ungroupedBandsList.size() == 0) {
                    angularViews = autoGroupingAngularViews;
                } else {
                    final DisplayableAngularview[] angularViewsFromUngroupedBands =
                            createAngularViewsFromUngroupedBands(ungroupedBandsList.toArray(new AngularBand[0]),
                                    AngularViewShapeProvider.getValidIndex(i, false), i);
                    angularViews = new DisplayableAngularview[autoGroupingAngularViews.length + angularViewsFromUngroupedBands.length];
                    System.arraycopy(autoGroupingAngularViews, 0, angularViews, 0, autoGroupingAngularViews.length);
                    System.arraycopy(angularViewsFromUngroupedBands, 0, angularViews, autoGroupingAngularViews.length, angularViewsFromUngroupedBands.length);
                }
            } else {
                angularViews = createAngularViewsFromUngroupedBands(availableAngularBands, 1, 0);
            }
        }
        rasterToAngularMap.put(raster, angularViews);
    }

    //package local for testing
    static DisplayableAngularview[] createAngularViewsFromUngroupedBands(AngularBand[] ungroupedBands, int symbolIndex, int strokeIndex) {
        List<String> knownUnits = new ArrayList<>();
        List<DisplayableAngularview> displayableAngularViewList = new ArrayList<>();
        DisplayableAngularview defaultAngularView = new DisplayableAngularview("tbd", -1);
        for (AngularBand ungroupedBand : ungroupedBands) {
            final String unit = ungroupedBand.getOriginalBand().getUnit();
            if (StringUtils.isNullOrEmpty(unit)) {
                defaultAngularView.addBand(ungroupedBand);
            } else if (knownUnits.contains(unit)) {
                displayableAngularViewList.get(knownUnits.indexOf(unit)).addBand(ungroupedBand);
            } else {
                knownUnits.add(unit);
                final DisplayableAngularview angularView = new DisplayableAngularview("Bands measured in " + unit, symbolIndex++);
                angularView.setLineStyle(AngularViewStrokeProvider.getStroke(strokeIndex++));
                angularView.addBand(ungroupedBand);
                displayableAngularViewList.add(angularView);
            }
        }
        if (strokeIndex == 0) {
            defaultAngularView.setName(DisplayableAngularview.DEFAULT_SPECTRUM_NAME);
        } else {
            defaultAngularView.setName(DisplayableAngularview.REMAINING_BANDS_NAME);
        }
        defaultAngularView.setSymbolIndex(symbolIndex);
        defaultAngularView.setLineStyle(AngularViewStrokeProvider.getStroke(strokeIndex));
        displayableAngularViewList.add(defaultAngularView);
        return displayableAngularViewList.toArray(new DisplayableAngularview[displayableAngularViewList.size()]);
    }

    private DisplayableAngularview[] getAllAngularViews() {
        if (currentView == null || !rasterToAngularMap.containsKey(currentView.getRaster())) {
            return new DisplayableAngularview[0];
        }
        return rasterToAngularMap.get(currentView.getRaster());
    }

    private boolean isShowingAngularViewsForSelectedPins() {
        return showAngularViewsForSelectedPinsButton.isSelected();
    }

    List<DisplayableAngularview> getSelectedAngularViews() {
        List<DisplayableAngularview> selectedAngularViews = new ArrayList<>();
        if (currentView != null) {
            final RasterDataNode currentRaster = currentView.getRaster();
            if (currentProduct != null && rasterToAngularMap.containsKey(currentRaster)) {
                DisplayableAngularview[] allAngularViews = rasterToAngularMap.get(currentRaster);
                for (DisplayableAngularview displayableAngularView : allAngularViews) {
                    if (displayableAngularView.isSelected()) {
                        selectedAngularViews.add(displayableAngularView);
                    }
                }
            }
        }
        return selectedAngularViews;
    }

    private void updateAngularViewsUnits() {
        for (DisplayableAngularview angularView : getAllAngularViews()) {
            angularView.updateUnit();
        }
    }

    void removeCursorAngularViewsFromDataset() {
        chartHandler.removeCursorAngularViewsFromDataset();
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

    public boolean showsValidCursorAngularViews() {
        return chartHandler.showsValidCursorAngularViews();
    }

    private class ChartHandler {

        private static final String MESSAGE_NO_ANGULAR_BANDS = "No angular bands available";   /*I18N*/
        private static final String MESSAGE_NO_PRODUCT_SELECTED = "No product selected";
        private static final String MESSAGE_NO_AngularView_SELECTED = "No angular View selected";
        private static final String MESSAGE_COLLECTING_ANGULAR_INFORMATION = "Collecting angular information...";

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
            final LegendTitle legend = new LegendTitle(new AngularViewLegendItemSource());
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
            List<DisplayableAngularview> angularViews = getSelectedAngularViews();
            chartUpdater.updateChart(chart, angularViews);
            chart.getXYPlot().clearAnnotations();
        }

        private void updateData() {
            List<DisplayableAngularview> angularViews = getSelectedAngularViews();
            chartUpdater.updateData(chart, angularViews);
        }

        private void setEmptyPlot() {
            chart.getXYPlot().setDataset(null);
            if (getCurrentProduct() == null) {
                setPlotMessage(MESSAGE_NO_PRODUCT_SELECTED);
            } else if (!chartUpdater.showsValidCursorAngularViews()) {
            } else if (getAllAngularViews().length == 0) {
                setPlotMessage(MESSAGE_NO_AngularView_SELECTED);
            } else {
                setPlotMessage(MESSAGE_NO_ANGULAR_BANDS);
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

        public boolean showsValidCursorAngularViews() {
            return chartUpdater.showsValidCursorAngularViews();
        }

        public void removeCursorAngularViewsFromDataset() {
            chartUpdater.removeCursorAngularViewsFromDataset();
        }

        public void setCollectingAngularInformationMessage() {
            setPlotMessage(MESSAGE_COLLECTING_ANGULAR_INFORMATION);
        }
    }

    private class ChartUpdater {

        private final static int domain_axis_index = 0;
        private final static int range_axis_index = 1;
        private final static double relativePlotInset = 0.05;

        private final Map<Placemark, Map<Band, Double>> pinToEnergies;
        private boolean showsValidCursorAngularViews;
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

        private void updateData(JFreeChart chart, List<DisplayableAngularview> angularViews) {
            dataset = new XYSeriesCollection();
            if (rasterLevel >= 0) {
                fillDatasetWithPinSeries(angularViews, dataset, chart);
                fillDatasetWithCursorSeries(angularViews, dataset, chart);
            }
        }

        private void updateChart(JFreeChart chart, List<DisplayableAngularview> angularViews) {
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
            String oldLabel = plot.getDomainAxis().getLabel();
            String newLabel;
            if (useSensorZenithButton.isSelected()) {
                newLabel = "Sensor Zenith Angle";
            } else if (useSensorAzimuthButton.isSelected()){
                newLabel = "Sensor Azimuth Angle";
            } else if (useScatteringAngleButton.isSelected()){
                newLabel = "Scattering Angle";
            } else {
                newLabel = "View Angle";
            }
            if (!newLabel.equals(oldLabel)) {
                plot.getDomainAxis().setLabel(newLabel);
            }
            plot.setDataset(dataset);
            setPlotUnit(angularViews, plot);
        }

        private void setPlotUnit(List<DisplayableAngularview> angularViews, XYPlot plot) {
            String unitToBeDisplayed = "";
            if (angularViews.size() > 0) {
                unitToBeDisplayed = angularViews.get(0).getUnit();
                int i = 1;
                while (i < angularViews.size() && !unitToBeDisplayed.equals(DisplayableAngularview.MIXED_UNITS)) {
                    DisplayableAngularview displayableAngularView = angularViews.get(i++);
                    if (displayableAngularView.hasSelectedBands() && !unitToBeDisplayed.equals(displayableAngularView.getUnit())) {
                        unitToBeDisplayed = DisplayableAngularview.MIXED_UNITS;
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
            if (bounds.getLowerBound() > -180.0) {
                return new Range(bounds.getLowerBound() - delta, bounds.getUpperBound() + delta);
            } else {
                return new Range(Math.max(0, bounds.getLowerBound() - delta),
                        bounds.getUpperBound() + delta);
            }
        }

        private void fillDatasetWithCursorSeries(List<DisplayableAngularview> angularViews, XYSeriesCollection dataset, JFreeChart chart) {
            showsValidCursorAngularViews = false;
            if (modelP == null) {
                return;
            }
            if (isShowingCursorAngularView() && currentView != null) {
                for (DisplayableAngularview angularView : angularViews) {
                    XYSeries series = new XYSeries(angularView.getName());
                    final Band[] angularBands = angularView.getSelectedBands();
                    if (!currentProduct.isMultiSize()) {
                        for (Band angularBand : angularBands) {
                            final float viewAngle = angularBand.getAngularValue();
                            float angle_axis;
                            if (useSensorZenithButton.isSelected()) {
                                angle_axis = (float) get_sensor_zenith(viewAngle);
                            } else if (useSensorAzimuthButton.isSelected()){
                                angle_axis = (float) get_sensor_azimuth(viewAngle);
                            } else if (useScatteringAngleButton.isSelected()){
                                angle_axis = (float) get_scattering_angle(viewAngle);
                            } else {
                                angle_axis  = viewAngle;
                            }
                            if (abs(angle_axis) >= 180.0 || abs(angle_axis) <= 0) {
                                return;
                            }
                            if (pixelPosInRasterBounds && isPixelValid(angularBand, rasterPixelX, rasterPixelY, rasterLevel)) {
                                addToSeries(angularBand, rasterPixelX, rasterPixelY, rasterLevel, series, angle_axis);
                                showsValidCursorAngularViews = true;
                            }
                        }
                    } else {
                        for (Band angularBand : angularBands) {
                            final float viewAngle = angularBand.getAngularValue();
                            float angle_axis;
                            if (useSensorZenithButton.isSelected()) {
                                angle_axis = (float) get_sensor_zenith(viewAngle);
                            } else if (useSensorAzimuthButton.isSelected()){
                                angle_axis = (float) get_sensor_azimuth(viewAngle);
                            } else if (useScatteringAngleButton.isSelected()){
                                angle_axis = (float) get_scattering_angle(viewAngle);
                            } else {
                                angle_axis  = viewAngle;
                            }
                            if (abs(angle_axis) >= 180.0 || abs(angle_axis) <= 0) {
                                return;
                            }
                            final AffineTransform i2m = angularBand.getImageToModelTransform();
                            if (i2m.equals(currentView.getRaster().getImageToModelTransform())) {
                                if (pixelPosInRasterBounds && isPixelValid(angularBand, rasterPixelX, rasterPixelY, rasterLevel)) {
                                    addToSeries(angularBand, rasterPixelX, rasterPixelY, rasterLevel, series, angle_axis);
                                    showsValidCursorAngularViews = true;
                                }
                            } else {
                                //todo [Multisize_products] use scenerastertransform here
                                final PixelPos rasterPos = new PixelPos();
                                final MultiLevelModel multiLevelModel = angularBand.getMultiLevelModel();
                                int level = getLevel(multiLevelModel);
                                multiLevelModel.getModelToImageTransform(level).transform(modelP, rasterPos);
                                final int rasterX = (int) rasterPos.getX();
                                final int rasterY = (int) rasterPos.getY();
                                if (coordinatesAreInRasterBounds(angularBand, rasterX, rasterY, level) &&
                                        isPixelValid(angularBand, rasterX, rasterY, level)) {
                                    addToSeries(angularBand, rasterX, rasterY, level, series, angle_axis);
                                    showsValidCursorAngularViews = true;
                                }
                            }
                        }
                    }
                    updateRenderer(dataset.getSeriesCount(), Color.BLACK, angularView, chart);
                    dataset.addSeries(series);
                }
            }
        }

        private void addToSeries(Band angularBand, int x, int y, int level, XYSeries series, double wavelength) {
            final double energy = ProductUtils.getGeophysicalSampleAsDouble(angularBand, x, y, level);
            if (energy != angularBand.getGeophysicalNoDataValue()) {
                series.add(wavelength, energy);
            }
        }

        //todo code duplication with pixelinfoviewmodelupdater - move to single class - tf 20151119
        private boolean coordinatesAreInRasterBounds(RasterDataNode raster, int x, int y, int level) {
            final RenderedImage levelImage = raster.getSourceImage().getImage(level);
            return x >= 0 && y >= 0 && x < levelImage.getWidth() && y < levelImage.getHeight();
        }

        private void fillDatasetWithPinSeries(List<DisplayableAngularview> angularViews, XYSeriesCollection dataset, JFreeChart chart) {
            Placemark[] pins = getDisplayedPins();
            for (Placemark pin : pins) {
                List<XYSeries> pinSeries = createXYSeriesFromPin(pin, dataset.getSeriesCount(), angularViews, chart);
                pinSeries.forEach(dataset::addSeries);
            }
        }

        private List<XYSeries> createXYSeriesFromPin(Placemark pin, int seriesIndex, List<DisplayableAngularview> angularViews, JFreeChart chart) {
            List<XYSeries> pinSeries = new ArrayList<>();
            Color pinColor = PlacemarkUtils.getPlacemarkColor(pin, currentView);
            for (DisplayableAngularview angularView : angularViews) {
                XYSeries series = new XYSeries(angularView.getName() + "_" + pin.getLabel());
                final Band[] angularBands = angularView.getSelectedBands();
                Map<Band, Double> bandToEnergy;
                if (pinToEnergies.containsKey(pin)) {
                    bandToEnergy = pinToEnergies.get(pin);
                } else {
                    bandToEnergy = new HashMap<>();
                    pinToEnergies.put(pin, bandToEnergy);
                }
                for (Band angularBand : angularBands) {
                    double energy;
                    if (bandToEnergy.containsKey(angularBand)) {
                        energy = bandToEnergy.get(angularBand);
                    } else {
                        energy = readEnergy(pin, angularBand);
                        bandToEnergy.put(angularBand, energy);
                    }
                    final float viewAngle = angularBand.getAngularValue();
                    float angle_axis;
                    if (useSensorZenithButton.isSelected()) {
                        angle_axis = (float) get_sensor_zenith(viewAngle);
                    } else if (useSensorAzimuthButton.isSelected()){
                        angle_axis = (float) get_sensor_azimuth(viewAngle);
                    } else {
                        angle_axis  = viewAngle;
                    }
                    if (abs(angle_axis) >= 180.0 || abs(angle_axis) <= 0) {
                        return pinSeries;
                    }
                    if (energy != angularBand.getGeophysicalNoDataValue()) {
                        series.add(angle_axis, energy);
                    }
                }
                updateRenderer(seriesIndex++, pinColor, angularView, chart);
                pinSeries.add(series);
            }
            return pinSeries;
        }

        private double get_scattering_angle(double view_angle) {
            double scattering_angle = 0.0;
            for (AngularBand scattering_angle_Band : scattering_angle_Bands) {
                if (scattering_angle_Band.getOriginalBand().getAngularValue() == view_angle) {
                    scattering_angle = ProductUtils.getGeophysicalSampleAsDouble(scattering_angle_Band.getOriginalBand(), rasterPixelX, rasterPixelY, rasterLevel);
                    break;
                }
            }
            return scattering_angle;
        }

        private double get_sensor_azimuth(double view_angle) {
            double sensor_azimuth = 0.0;

            for (AngularBand sensor_azimuth_Band : sensor_azimuth_Bands) {
                if (sensor_azimuth_Band.getOriginalBand().getAngularValue() == view_angle) {
                    sensor_azimuth = ProductUtils.getGeophysicalSampleAsDouble(sensor_azimuth_Band.getOriginalBand(), rasterPixelX, rasterPixelY, rasterLevel);
                    break;
                }
            }
            return sensor_azimuth;
        }

        private double get_sensor_zenith(double view_angle) {
            double sensor_zenith_value = 0.0;
            double sensor_zenith;

            for (AngularBand sensor_zenith_Band : sensor_zenith_Bands) {
                if (sensor_zenith_Band.getOriginalBand().getAngularValue() == view_angle) {
                    sensor_zenith_value = ProductUtils.getGeophysicalSampleAsDouble(sensor_zenith_Band.getOriginalBand(), rasterPixelX, rasterPixelY, rasterLevel);
                    break;
                }
            }
            sensor_zenith = sensor_zenith_value * view_angle/abs(view_angle);
            return sensor_zenith;
        }

        private void updateRenderer(int seriesIndex, Color seriesColor, DisplayableAngularview angularView, JFreeChart chart) {
            final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();

            final Stroke lineStyle = angularView.getLineStyle();
            renderer.setSeriesStroke(seriesIndex, lineStyle);

            Shape symbol = angularView.getScaledShape();
            renderer.setSeriesShape(seriesIndex, symbol);
            renderer.setSeriesShapesVisible(seriesIndex, true);

            renderer.setSeriesPaint(seriesIndex, seriesColor);
        }

        private double readEnergy(Placemark pin, Band angularBand) {
            //todo [Multisize_products] use scenerastertransform here
            final Object pinGeometry = pin.getFeature().getDefaultGeometry();
            if (pinGeometry == null || !(pinGeometry instanceof Point)) {
                return angularBand.getGeophysicalNoDataValue();
            }
            final Point2D.Double modelPoint = new Point2D.Double(((Point) pinGeometry).getCoordinate().x,
                    ((Point) pinGeometry).getCoordinate().y);
            final MultiLevelModel multiLevelModel = angularBand.getMultiLevelModel();
            int level = getLevel(multiLevelModel);
            final AffineTransform m2iTransform = multiLevelModel.getModelToImageTransform(level);
            final PixelPos pinLevelRasterPos = new PixelPos();
            m2iTransform.transform(modelPoint, pinLevelRasterPos);
            int pinLevelRasterX = (int) Math.floor(pinLevelRasterPos.getX());
            int pinLevelRasterY = (int) Math.floor(pinLevelRasterPos.getY());
            if (coordinatesAreInRasterBounds(angularBand, pinLevelRasterX, pinLevelRasterY, level) &&
                    isPixelValid(angularBand, pinLevelRasterX, pinLevelRasterY, level)) {
                return ProductUtils.getGeophysicalSampleAsDouble(angularBand, pinLevelRasterX, pinLevelRasterY, level);
            }
            return angularBand.getGeophysicalNoDataValue();
        }

        private void removePinInformation(Placemark pin) {
            pinToEnergies.remove(pin);
        }

        private void removeBandinformation(Band band) {
            for (Placemark pin : pinToEnergies.keySet()) {
                Map<Band, Double> bandToEnergiesMap = pinToEnergies.get(pin);
                if (bandToEnergiesMap.containsKey(band)) {
                    bandToEnergiesMap.remove(band);
                }
            }
        }

        public boolean showsValidCursorAngularViews() {
            return showsValidCursorAngularViews;
        }

        void removeCursorAngularViewsFromDataset() {
            modelP = null;
            if (showsValidCursorAngularViews) {
                int numberOfSelectedAngularViews = getSelectedAngularViews().size();
                int numberOfPins = getDisplayedPins().length;
                int numberOfDisplayedGraphs = numberOfPins * numberOfSelectedAngularViews;
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

    private class AngularViewLegendItemSource implements LegendItemSource {

        @Override
        public LegendItemCollection getLegendItems() {
            LegendItemCollection itemCollection = new LegendItemCollection();
            final Placemark[] displayedPins = getDisplayedPins();
            final List<DisplayableAngularview> angularViews = getSelectedAngularViews();
            for (Placemark pin : displayedPins) {
                Paint pinPaint = PlacemarkUtils.getPlacemarkColor(pin, currentView);
                angularViews.stream().filter(DisplayableAngularview::hasSelectedBands).forEach(angularView -> {
                    String legendLabel = pin.getLabel() + "_" + angularView.getName();
                    LegendItem item = createLegendItem(angularView, pinPaint, legendLabel);
                    itemCollection.add(item);
                });
            }
            if (isShowingCursorAngularView() && showsValidCursorAngularViews()) {
                angularViews.stream().filter(DisplayableAngularview::hasSelectedBands).forEach(angularView -> {
                    Paint defaultPaint = Color.BLACK;
                    LegendItem item = createLegendItem(angularView, defaultPaint, angularView.getName());
                    itemCollection.add(item);
                });
            }
            return itemCollection;
        }

        private LegendItem createLegendItem(DisplayableAngularview angularView, Paint paint, String legendLabel) {
            Stroke outlineStroke = new BasicStroke();
            Line2D lineShape = new Line2D.Double(0, 5, 40, 5);
            Stroke lineStyle = angularView.getLineStyle();
            Shape symbol = angularView.getScaledShape();
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
                    updateAngularViewsUnits();
                    chartHasChanged = true;
                } else if (propertyName.equals(Band.PROPERTY_NAME_ANGULAR_VALUE)) {
                    setUpAngularViews();
                    chartHasChanged = true;
                }
            } else if (event.getSourceNode() instanceof Placemark) {
                if (event.getPropertyName().equals("geoPos") || event.getPropertyName().equals("pixelPos")) {
                    chartHandler.removePinInformation((Placemark) event.getSourceNode());
                }
                if (isShowingPinAngularViews()) {
                    chartHasChanged = true;
                }
            } else if (event.getSourceNode() instanceof Product) {
                if (event.getPropertyName().equals("autoGrouping")) {
                    setUpAngularViews();
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
                if (isAngularBand(newBand)) {
                    addBandToAngularViews((Band) event.getSourceNode());
                    recreateChart();
                }
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinAngularViews()) {
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
                removeBandFromAngularViews(band);
                chartHandler.removeBandInformation(band);
                recreateChart();
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinAngularViews()) {
                    recreateChart();
                }
            }
        }

        private void addBandToAngularViews(Band band) {
            DisplayableAngularview[] allAngularViews = rasterToAngularMap.get(currentView.getRaster());
            Product.AutoGrouping autoGrouping = currentProduct.getAutoGrouping();
            if (autoGrouping != null) {
                final int bandIndex = autoGrouping.indexOf(band.getName());
                final DisplayableAngularview angularView;
                if (bandIndex != -1) {
                    angularView = allAngularViews[bandIndex];
                } else {
                    angularView = allAngularViews[allAngularViews.length - 1];
                }
                angularView.addBand(new AngularBand(band, angularView.isSelected()));
            } else {
                allAngularViews[0].addBand(new AngularBand(band, true));
            }
        }

        private void removeBandFromAngularViews(Band band) {
            DisplayableAngularview[] allAngularViews = rasterToAngularMap.get(currentView.getRaster());
            for (DisplayableAngularview displayableAngularView : allAngularViews) {
                Band[] angularBands = displayableAngularView.getAngularBands();
                for (int j = 0; j < angularBands.length; j++) {
                    Band angularBand = angularBands[j];
                    if (angularBand == band) {
                        displayableAngularView.remove(j);
                        if (displayableAngularView.getSelectedBands().length == 0) {
                            displayableAngularView.setSelected(false);
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
