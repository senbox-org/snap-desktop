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
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.validators.IntervalValidator;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.*;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.TextFieldContainer;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XIntervalSeries;
import org.jfree.data.xy.XIntervalSeriesCollection;
import org.openide.windows.TopComponent;

import javax.media.jai.Histogram;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.prefs.Preferences;


/**
 * A general pane within the statistics window.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Daniel Knowles
 */
class StatisticsPanel extends PagePanel implements MultipleRoiComputePanel.ComputeMasks, StatisticsDataProvider {

    final SnapApp snapApp = SnapApp.getDefault();
    private PropertyMap configuration = null;

    ProgressMonitorSwingWorker swingWorker;

    private StatisticsCriteriaPanel statisticsCriteriaPanel;

    private static final String DEFAULT_STATISTICS_TEXT = "<html>No statistics computed<br>Note: requires a view window to be selected</html>";  /*I18N*/
    private static final String TITLE_PREFIX = "Statistics";

    private double spreadsheetHeightWeight = 0.3;
    private int spreadsheetMinRowsBeforeWeight = 10;

    boolean invertPercentile = true;

    boolean fixedHistDomainAllPlots = false;
    boolean fixedHistDomainAllPlotsInitialized = false;
    double[] histDomainBoundsAllPlots = {0, 0};

    boolean fixedPercentileDomainAllPlots = true;
    boolean fixedPercentileDomainAllPlotsInitialized = false;
    double[] histRangeBoundsAllPlots = {0, 0};
    double[] percentileDomainBoundsAllPlots = {0, 0};
    double[] percentileRangeBoundsAllPlots = {0, 0};

    private MultipleRoiComputePanel computePanel;
    private JPanel backgroundPanel;
    private AbstractButton hideAndShowButton;
    private AbstractButton exportButton;
    private JPanel contentPanel;
    private JPanel spreadsheetPanel;
    JScrollPane spreadsheetScrollPane;
    JScrollPane contentScrollPane;
    JPanel leftPanel;

    JButton runButton = new JButton("Run");

    private final StatisticsPanel.PopupHandler popupHandler;
    private final StringBuilder resultText;

    private boolean init;
    private Histogram[] histograms;
    private ExportStatisticsAsCsvAction exportAsCsvAction;
    private PutStatisticsIntoVectorDataAction putStatisticsIntoVectorDataAction;

    private boolean exportButtonEnabled = false;
    private boolean exportButtonVisible = false;

    private Object[][] statsSpreadsheet;

    private int numStxFields = 0;
    private int numStxRegions = 0;
    private int totalRecordCount = 0;

    private String NO_NAN_BANDNAME = "Stx_No_NaN_temporary_band_2dw7gi4kg97kgkd9034kf";
    RasterDataNode noNanBandRaster = null;


    private Product currProduct = null;
    private RasterDataNode currRaster = null;
    boolean fieldsInitialized = false;


    TopComponent parentDialog;
    String helpID;


    private static String COLUMN_BREAK = "||";


    private enum PrimaryStatisticsFields {
        FileRefNum("File#"),
        BandName("Band"),
        MaskName("Regional_Mask"),
        QualityMaskName("Quality_Mask");

        PrimaryStatisticsFields(String name) {
            this.name = name;
        }

        private final String name;

        public String toString() {
            return name;
        }
    }


    private enum MetaDataFields {
        FileMetaDataBreak(COLUMN_BREAK),
        FileName("File"),
        FileType("File_Type"),
        FileWidth("File_Width"),
        FileHeight("File_Height"),
        FileFormat("File_Format"),
        Sensor("Sensor"),
        Resolution("Resolution"),
        DayNight("Day_Night"),
        Orbit("Orbit"),
        Platform("Platform"),
        ProcessingVersion("Processing_Version"),
        Projection("Projection"),
        ProjectionParameters("Projection Parameters"),
        TimeMetaDataBreak(COLUMN_BREAK),
        StartDate("Start_Date"),
        StartTime("Start_Time"),
        EndDate("End_Date"),
        EndTime("End_Time"),
        TimeSeriesDate("TimeSeries_Date"),
        TimeSeriesTime("TimeSeries_Time"),
        BandMetaDataBreak(COLUMN_BREAK),
        BandName("Band"),
        BandUnit("Unit"),
        BandValidExpression("Band_Valid_Expression"),
        BandDescription("Band_Description"),
        RegionalMaskMetaDataBreak(COLUMN_BREAK),
        RegionalMaskName("Regional_Mask"),
        RegionalMaskDescription("Regional_Mask_Description"),
        RegionalMaskExpression("Regional_Mask_Expression"),
        QualityMaskMetaDataBreak(COLUMN_BREAK),
        QualityMaskName("Quality_Mask"),
        QualityMaskDescription("Quality_Mask_Description"),
        QualityMaskExpression("Quality_Mask_Expression");

        MetaDataFields(String name) {
            this.name = name;
        }

        private final String name;

        public String toString() {
            return name;
        }
    }


    int stxFieldsStartIdx = -1;
    int stxFieldsEndIdx = -1;


    private HashMap<MetaDataFields, Integer> metaDataFieldsHashMap = new HashMap<MetaDataFields, Integer>();
    private HashMap<PrimaryStatisticsFields, Integer> primaryStatisticsFieldsHashMap = new HashMap<PrimaryStatisticsFields, Integer>();


    private TextFieldContainer spreadsheetColWidthTextfieldContainer = null;


    //   private HistDisplayHighThreshTextfield plotsThreshDomainHighTextfieldContainer = new HistDisplayHighThreshTextfield();

    public StatisticsPanel(final TopComponent parentDialog, String helpID) {
        super(parentDialog, helpID, TITLE_PREFIX);

        resultText = new StringBuilder();
        popupHandler = new PopupHandler();
        if (snapApp != null) {
            final Preferences preferences = SnapApp.getDefault().getPreferences();
            PropertyMap propertyMap = new PreferencesPropertyMap(preferences);

            this.configuration = propertyMap;
        }

        this.parentDialog = parentDialog;
        this.helpID = helpID;

        // Adjust to not be over 90% of screen size
        double minWidth = 1000;
        double minHeight = 1000;
        double prefWidth = 1000;
        double prefHeight = 1000;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screenSizeWidth = screenSize.getWidth();
        double screenSizeHeight = screenSize.getHeight();

        if (minWidth > screenSizeWidth * .9) {
            minWidth = screenSizeWidth * .9;
        }

        if (minHeight > screenSizeHeight * .9) {
            minHeight = screenSizeHeight * .9;
        }

        if (prefWidth > screenSizeWidth * .9) {
            prefWidth = screenSizeWidth * .9;
        }

        if (prefHeight > screenSizeHeight * .9) {
            prefHeight = screenSizeHeight * .9;
        }


//        setMinimumSize(new Dimension((int) minWidth, (int) minHeight));
        setPreferredSize(new Dimension((int) prefWidth, (int) prefHeight));

    }


    private void initHashMaps() {
        metaDataFieldsHashMap = new HashMap<MetaDataFields, Integer>();

        for (MetaDataFields field : MetaDataFields.values()) {
            metaDataFieldsHashMap.put(field, -1);
        }

        primaryStatisticsFieldsHashMap = new HashMap<PrimaryStatisticsFields, Integer>();

        for (PrimaryStatisticsFields field : PrimaryStatisticsFields.values()) {
            primaryStatisticsFieldsHashMap.put(field, -1);
        }

    }


    @Override
    protected void initComponents() {
        init = true;


        initHashMaps();


        statsSpreadsheet = null;


        statisticsCriteriaPanel = new StatisticsCriteriaPanel(getParentDialog());


        final JPanel rightPanel = getRightPanel();

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
                    hideAndShowButton.setVisible(true);
                    hideAndShowButton.setToolTipText("Collapse Options Panel");
                } else {
                    hideAndShowButton.setIcon(expandIcon);
                    hideAndShowButton.setRolloverIcon(expandRolloverIcon);
                    hideAndShowButton.setVisible(true);
                    hideAndShowButton.setToolTipText("Expand Options Panel");
                }
                rightPanelShown = !rightPanelShown;
            }
        });

        hideAndShowButton.setVisible(true);


        contentPanel = new JPanel(new GridLayout(-1, 1));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.addMouseListener(popupHandler);

        contentScrollPane = new JScrollPane(contentPanel);
        contentScrollPane.setBorder(null);
        contentScrollPane.setBackground(Color.WHITE);


        spreadsheetPanel = new JPanel(new GridLayout(-1, 1));
        //    spreadsheetPanel.setBackground(Color.WHITE);
        spreadsheetPanel.addMouseListener(popupHandler);

        spreadsheetScrollPane = new JScrollPane(spreadsheetPanel);


        spreadsheetScrollPane.setBorder(null);
        //    spreadsheetScrollPane.setBackground(Color.WHITE);
        spreadsheetScrollPane.setMinimumSize(new Dimension(100, 100));
        spreadsheetScrollPane.setBorder(UIUtils.createGroupBorder("Statistics Spreadsheet"));
        spreadsheetScrollPane.setVisible(statisticsCriteriaPanel.showStatsSpreadSheet());


        leftPanel = new JPanel(new GridBagLayout());
//        GridBagConstraints gbcLeftPanel = new GridBagConstraints();
//        GridBagUtils.addToPanel(leftPanel, contentScrollPane, gbcLeftPanel, "fill=BOTH, weightx=1.0, weighty=1.0, anchor=NORTHWEST");
//        GridBagUtils.addToPanel(leftPanel, spreadsheetScrollPane, gbcLeftPanel, "fill=BOTH, weightx=1.0, weighty=0.0, anchor=NORTHWEST, gridy=1,insets.top=5");

        initLeftPanel();

        backgroundPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagUtils.addToPanel(backgroundPanel, leftPanel, gbc, "fill=BOTH, weightx=1.0, weighty=1.0, anchor=NORTHWEST,insets.right=5");
        //   GridBagUtils.addToPanel(backgroundPanel, contentScrollPane, gbc, "fill=BOTH, weightx=1.0, weighty=1.0, anchor=NORTHWEST");
        //  GridBagUtils.addToPanel(backgroundPanel, spreadsheetScrollPane, gbc, "fill=BOTH, weightx=1.0, weighty=1.0, anchor=NORTHWEST, gridy=1");
        GridBagUtils.addToPanel(backgroundPanel, rightPanel, gbc, "gridx=1,gridy=0, fill=BOTH, weightx=0.0,anchor=NORTHEAST,insets.left=5");


        //   GridBagUtils.addToPanel(backgroundPanel, spreadsheetScrollPane, gbcLeftPanel, "fill=HORIZONTAL, weightx=1.0, weighty=1.0, anchor=NORTHWEST, gridy=1,gridx=0,gridwidth=2,insets.top=5");
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.add(backgroundPanel, new Integer(0));
        layeredPane.add(hideAndShowButton, new Integer(1));
        add(layeredPane);


        // Set size of initial statistics GUI

        // Determine sub component dimension
        double minWidth = leftPanel.getMinimumSize().width + rightPanel.getMinimumSize().width;
        double prefWidth = leftPanel.getPreferredSize().width + rightPanel.getPreferredSize().width;
        double minHeight = Math.max(leftPanel.getMinimumSize().height, rightPanel.getMinimumSize().height);
        double prefHeight = Math.max(leftPanel.getPreferredSize().height, rightPanel.getPreferredSize().height);

        // Adjust bigger if too small
        minWidth = (minWidth < 1000)  ? 1000 : minWidth;
        minHeight = (minHeight < 1000) ? 1000: minHeight;

        prefWidth = (prefWidth < 1000)  ? 1000 : prefWidth;
        prefHeight = (prefHeight < 1000) ? 1000: prefHeight;

        // Adjust to not be over 90% of screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screenSizeWidth = screenSize.getWidth();
        double screenSizeHeight = screenSize.getHeight();

        if (minWidth > screenSizeWidth * .9) {
            minWidth = screenSizeWidth * .9;
        }

        if (minHeight > screenSizeHeight * .9) {
            minHeight = screenSizeHeight * .9;
        }

        if (prefWidth > screenSizeWidth * .9) {
            prefWidth = screenSizeWidth * .9;
        }

        if (prefHeight > screenSizeHeight * .9) {
            prefHeight = screenSizeHeight * .9;
        }


//        setMinimumSize(new Dimension((int) minWidth, (int) minHeight));
        setPreferredSize(new Dimension((int) prefWidth, (int) prefHeight));
    }


    private JPanel getRightPanel() {


        computePanel = new MultipleRoiComputePanel(this, getRaster());


        final JPanel rightPanel = GridBagUtils.createPanel();


        final JPanel mainPane = GridBagUtils.createPanel();

        //  GridBagConstraints extendedOptionsPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1,insets.right=-2");
        GridBagConstraints extendedOptionsPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1");


        GridBagUtils.addToPanel(rightPanel, computePanel, extendedOptionsPanelConstraints, "gridy=0,fill=NONE,weighty=1,weightx=1");


        //  GridBagUtils.addToPanel(rightPanel, statisticsCriteriaPanel.getCriteriaFormattingTabbedPane(), extendedOptionsPanelConstraints, "gridy=1,fill=BOTH,weighty=0, insets.top=10");


        computePanel.getCriteriaPanel().setBorder(UIUtils.createGroupBorder(""));


        GridBagUtils.addToPanel(computePanel.getCriteriaPanel(), statisticsCriteriaPanel.getCriteriaPanel(), extendedOptionsPanelConstraints, "insets.top=10, insets.left=5, insets.right=5");

        JButton resetToDefaultsButton = new JButton("Reset");
        resetToDefaultsButton.addActionListener(new ActionListener() {
                                                    @Override
                                                    public void actionPerformed(ActionEvent e) {
                                                        statisticsCriteriaPanel.reset();
                                                        computePanel.reset();
                                                    }
                                                }
        );

        runButton.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            computePanel.run();
                                        }
                                    }
        );

        runButton.setEnabled(getRaster() != null);


        exportButton = getExportButton();
        exportButton.setToolTipText("Export: This only exports the binning portion of the statistics");
        exportButton.setVisible(exportButtonVisible);

        final JPanel exportAndHelpPanel = GridBagUtils.createPanel();
        GridBagConstraints helpPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1,ipadx=0");
        GridBagUtils.addToPanel(exportAndHelpPanel, new JSeparator(), helpPanelConstraints, "fill=HORIZONTAL,gridwidth=4,insets.left=5,insets.right=5");
        GridBagUtils.addToPanel(exportAndHelpPanel, exportButton, helpPanelConstraints, "gridy=1,anchor=WEST,fill=NONE, gridwidth=1");
        GridBagUtils.addToPanel(exportAndHelpPanel, runButton, helpPanelConstraints, "gridx=1, gridy=1,anchor=WEST,fill=NONE");
        GridBagUtils.addToPanel(exportAndHelpPanel, resetToDefaultsButton, helpPanelConstraints, "gridx=2, gridy=1,anchor=CENTER,fill=NONE");
        GridBagUtils.addToPanel(exportAndHelpPanel, getHelpButton(), helpPanelConstraints, "gridx=3,gridy=1,anchor=EAST,fill=NONE");

        GridBagUtils.addToPanel(rightPanel, exportAndHelpPanel, extendedOptionsPanelConstraints, "gridy=2,anchor=SOUTHWEST,fill=HORIZONTAL,weighty=0,insets.top=0");


        rightPanel.setMinimumSize(rightPanel.getPreferredSize());

        return rightPanel;
    }


    @Override
    protected void updateComponents() {
        if (!computePanel.isRunning()) {
            if (computePanel.isRunning()) {
                if (swingWorker != null) {
                    swingWorker.cancel(true);
                }
                computePanel.setRunning(false);
            }


            if (!init) {
                initComponents();
            }


            boolean productChanged = false;
            boolean rasterChanged = false;

            if (getProduct() != null) {
                Product prevProduct = currProduct;
                currProduct = getProduct();

                if (currProduct != null && currProduct != prevProduct) {
                    productChanged = true;
                    fieldsInitialized = false;
                }
            }


            if (getRaster() != null) {
                RasterDataNode prevRaster = currRaster;
                currRaster = getRaster();

                if (currRaster != null && currRaster != prevRaster) {
                    rasterChanged = true;
                }
            }


            if (!fieldsInitialized || productChanged || (rasterChanged && computePanel.forceUpdate)) {
                statisticsCriteriaPanel.resetProduct();

                statsSpreadsheet = null;

                final RasterDataNode raster = getRaster();
                computePanel.setRaster(raster);
                runButton.setEnabled(raster != null);
                contentPanel.removeAll();
                spreadsheetPanel.removeAll();
                resultText.setLength(0);


                if (raster != null && raster.isStxSet() && raster.getStx().getResolutionLevel() == 0) {

                    //    percentThresholdsList = statisticsCriteriaPanel.getPercentThresholdsList();
                    //   resultText.append(createText(raster.getStx(), null));
                    contentPanel.add(createStatPanel(raster.getStx(), null, null, 1, getRaster()));

                    PagePanel pagePanel = new StatisticsSpreadsheetPagePanel(parentDialog, helpID, statisticsCriteriaPanel, statsSpreadsheet, this);
                    pagePanel.initComponents();
                    spreadsheetPanel.add(pagePanel);

                    //   spreadsheetPanel.add(statsSpreadsheetPanel());
                    histograms = new Histogram[]{raster.getStx().getHistogram()};
                    exportAsCsvAction = new ExportStatisticsAsCsvAction(this);
                    putStatisticsIntoVectorDataAction = new PutStatisticsIntoVectorDataAction(this);
                    exportButton.setEnabled(exportButtonEnabled);

                } else {
                    contentPanel.add(new JLabel(DEFAULT_STATISTICS_TEXT));
                    exportButton.setEnabled(false);
                }


                contentPanel.revalidate();
                contentPanel.repaint();
                spreadsheetScrollPane.setVisible(statisticsCriteriaPanel.showStatsSpreadSheet());
                spreadsheetPanel.revalidate();
                spreadsheetPanel.repaint();
                backgroundPanel.revalidate();
                backgroundPanel.repaint();

                if (raster != null) {
                    exportButton.setEnabled(false);
                }
            }
        }
    }

    @Override
    public Histogram[] getHistograms() {
        return histograms;
    }

    private static class ComputeResult {

        final Stx stx;
        final Mask mask;

        ComputeResult(Stx stx, Mask mask) {
            this.stx = stx;
            this.mask = mask;
        }
    }

    @Override
    public void compute(final Mask[] selectedRegionMasks, final Mask[] selectedQualityMasks, final Band[] selectedBands) {

        //    computePanel.setRunning(true);
        //    System.out.print("Run2\n");
        spreadsheetPanel.removeAll();
        fixedHistDomainAllPlotsInitialized = false;


        if (selectedBands != null && selectedBands.length > 0 && selectedBands[0] != null) {
            if (selectedBands.length == 1) {
                if (getRaster() != null) {
                    String rasterName = getRaster().getName();
                    String selectedBandName = selectedBands[0].getName();
                    if (rasterName.equals(selectedBandName)) {
                        computePanel.setUseViewBandRaster(true);
                    } else {
                        computePanel.setUseViewBandRaster(false);
                    }
                } else {
                    computePanel.setUseViewBandRaster(false);
                }
            } else {
                computePanel.setUseViewBandRaster(false);
            }
        } else {
            computePanel.setUseViewBandRaster(true);
        }


        fieldsInitialized = true;

        int numBands = 0;
        int numRegionMasks = 0;
        int numQualityMasks = 0;

        if (computePanel.isUseViewBandRaster()) {
            numBands = 1;
        } else {
            numBands = selectedBands.length;
        }


        numQualityMasks = getMasksToProcessCount(selectedQualityMasks, computePanel.isIncludeNoQuality(), computePanel.getQualityMaskGrouping());
        numRegionMasks = getMasksToProcessCount(selectedRegionMasks, computePanel.isIncludeFullScene(), computePanel.getRegionalMaskGrouping());

        numStxRegions = numBands * numRegionMasks * numQualityMasks;
        System.out.println("numStxRegions=" + numStxRegions);

        this.histograms = new Histogram[numStxRegions];
        final String title = "Computing Statistics";


        if (statisticsCriteriaPanel.getPercentThresholdsList() == null) {
            abortRun();
            return;
        }

        statsSpreadsheet = null;  // reset this

        if (!retrieveValidateTextFields(true)) {
            abortRun();
            return;
        }

        // just in case: should not get here as it should have been caught earlier
        if (!validFields()) {
            Dialogs.showMessage("Invalid Input",
                    "Failed to compute statistics due to invalid fields",
                    JOptionPane.ERROR_MESSAGE, null);
            computePanel.setRunning(false);
            return;
        }


        swingWorker = new ProgressMonitorSwingWorker(this, title) {


            //   SwingWorker<Object, ComputeResult> swingWorker = new ProgressMonitorSwingWorker<Object, ComputeResult>(this, title) {

            @Override
            protected Object doInBackground(ProgressMonitor pm) {
                int numberOfProgressSteps = numStxRegions;
                if (statisticsCriteriaPanel.includeTotalPixels()) {
                    numberOfProgressSteps = numberOfProgressSteps * 2;  // adds additional stx calculation per each stxRegion
                }
                pm.beginTask(title, numberOfProgressSteps);

                Mask[] qualityMasksToProcess = null;
                Mask[] regionalMasksToProcess = null;


                try {
                    if (statisticsCriteriaPanel.includeTotalPixels()) {
                        addNoNanBand(getProduct());
                    }


                    qualityMasksToProcess = getMasksToProcess(selectedQualityMasks, computePanel.getQualityGroupMaskNameTextfield().getText(), computePanel.isIncludeNoQuality(), computePanel.getQualityMaskGrouping());
                    regionalMasksToProcess = getMasksToProcess(selectedRegionMasks, computePanel.getRegionalGroupMaskNameTextfield().getText(), computePanel.isIncludeFullScene(), computePanel.getRegionalMaskGrouping());

                    int stxIdx = 0;
                    totalRecordCount = 0;
                    int recordCount = 0;


                    if (computePanel.isUseViewBandRaster()) {
                        RasterDataNode raster = getRaster();

                        recordCount = computeAllStxForRaster(raster, pm, regionalMasksToProcess, qualityMasksToProcess, stxIdx);

                        stxIdx += recordCount;
                        totalRecordCount += recordCount;
                    } else {
                        for (int rasterIdx = 0; rasterIdx < selectedBands.length; rasterIdx++) {
                            final Band band = selectedBands[rasterIdx];
                            RasterDataNode raster = getProduct().getRasterDataNode(band.getName());

                            recordCount = computeAllStxForRaster(raster, pm, regionalMasksToProcess, qualityMasksToProcess, stxIdx);

                            stxIdx += recordCount;
                            totalRecordCount += recordCount;
                        }
                    }


                } finally {
                    if (statisticsCriteriaPanel.includeTotalPixels()) {
                        removeNoNanBand(getProduct());
                    }


                    //  this deletion of the mask causes code to freeze so currently is not enabled
//                    if (comboQualityMasks != null && comboQualityMasks[0] != null) {
//                        final ProductNodeGroup<Mask> maskGroup = getProduct().getMaskGroup();
//
//                        String[] maskGroupNodeNames = new String[maskGroup.getNodeNames().length];
//                        int i=0;
//                        for (String name : maskGroup.getNodeNames()) {
//                            maskGroupNodeNames[i] = name;
//                            i++;
//                        }
//
//                        for (String name : maskGroupNodeNames) {
//                            if (name.equals(comboQualityMasks[0].getName())) {
//                                maskGroup.remove(maskGroup.get(name));
//                            }
//                        }
//
////                        for (String name : maskGroup.getNodeNames()) {
////                            if (name.equals(comboQualityMasks[0].getName())) {
////                                maskGroup.remove(maskGroup.get(name));
////                            }
////                        }
//                    }


                    updateLeftPanel();

                    resultText.setLength(0);
                    resultText.append(createText());

                    pm.done();


                }
                return null;
            }


            @Override
            protected void done() {
                computePanel.setRunning(false);

                try {
                    if (totalRecordCount == 0) {
                        Dialogs.showMessage("Statistics",
                                "No statistics computed.\nMask and/or Full Scene must be selected.",
                                JOptionPane.ERROR_MESSAGE, null);
                    }


//                    get();
//                    if (exportAsCsvAction == null) {
//                        exportAsCsvAction = new ExportStatisticsAsCsvAction(StatisticsPanel.this);
//                    }
//                    exportAsCsvAction.setSelectedMasks(selectedMasks);
//                    if (putStatisticsIntoVectorDataAction == null) {
//                        putStatisticsIntoVectorDataAction = new PutStatisticsIntoVectorDataAction(StatisticsPanel.this);
//                    }
//                    putStatisticsIntoVectorDataAction.setSelectedMasks(selectedMasks);
//                    //       exportButton.setEnabled(exportButtonEnabled);


                    computePanel.setRunning(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    Dialogs.showMessage("Statistics",
                            "<html>Failed to compute statistics.<br/>An error occurred:"
                                    + e.getMessage() + "</html>",
                            JOptionPane.ERROR_MESSAGE, null);
                    computePanel.setRunning(false);
                }
            }
        };

        resultText.setLength(0);
        contentPanel.removeAll();

        // swingWorker.execute();

        swingWorker.executeWithBlocking();
    }


    // includes null mask if requested
    private int getMasksToProcessCount(Mask[] selectedMasks, boolean includeNull, MultipleRoiComputePanel.MaskGrouping maskGrouping) {

        int masksToProcessCount = 0;

        if (includeNull) {
            masksToProcessCount++;
        }

        int selectedMasksCount = getSelectMaskCount(selectedMasks);

        if (getSelectMaskCount(selectedMasks) > 0) {
            if (maskGrouping == MultipleRoiComputePanel.MaskGrouping.INDIVIDUAL) {
                masksToProcessCount += selectedMasksCount;
            } else {
                masksToProcessCount++;
            }
        }

        return masksToProcessCount;
    }


    // includes null mask if requested
    private Mask[] getMasksToProcess(Mask[] selectedMasks, String maskName, boolean includeNull, MultipleRoiComputePanel.MaskGrouping maskGrouping) {

        int masksToProcessCount = getMasksToProcessCount(selectedMasks, includeNull, maskGrouping);
        if (masksToProcessCount == 0) {
            return null;
        }

        Mask[] masksToProcess = new Mask[masksToProcessCount];
        int index = 0;

        if (includeNull) {
            masksToProcess[index] = null;
            index++;
        }

        if (getSelectMaskCount(selectedMasks) > 0) {
            if (maskGrouping == MultipleRoiComputePanel.MaskGrouping.INDIVIDUAL) {
                //todo  We may consider also adding an INDIVIDUAL_COMPLEMENT mask
                for (Mask mask : selectedMasks) {
                    if (mask != null) {
                        masksToProcess[index] = mask;
                        index++;
                    }
                }
            } else {
                Mask logicallyCombinedMask = getLogicallyCombinedMask(selectedMasks, maskName, maskGrouping);
                if (logicallyCombinedMask != null) {
                    masksToProcess[index] = logicallyCombinedMask;
                }
            }
        }

        return masksToProcess;
    }

    private int getSelectMaskCount(Mask[] selectedMasks) {
        int selectedMasksCount = 0;

        if (selectedMasks != null) {
            int length = selectedMasks.length;
            for (Mask mask : selectedMasks) {
                if (mask != null) {
                    selectedMasksCount++;
                }
            }
        }

        return selectedMasksCount;
    }

    private Mask getLogicallyCombinedMask(Mask[] selectedMasks, String maskName, MultipleRoiComputePanel.MaskGrouping maskGrouping) {

        if (selectedMasks == null || maskName == null || maskGrouping == null || getProduct() == null) {
            return null;
        }

        Mask logicallyCombinedMask = null;
        boolean maskAlreadyExists = false;

        String combinedMaskExpression = combineMaskExpressions(selectedMasks, maskGrouping, maskName);

        if (combinedMaskExpression != null && combinedMaskExpression.length() > 0 && getProduct().getMaskGroup() != null) {

            final ProductNodeGroup<Mask> maskGroup = getProduct().getMaskGroup();

            // determine if mask already exists and if it does overwrite it
            for (String name : maskGroup.getNodeNames()) {
                if (name != null && name.equals(maskName)) {
                    maskAlreadyExists = true;
                    maskGroup.get(name).getImageConfig().setValue("expression", combinedMaskExpression);
                    logicallyCombinedMask = maskGroup.get(name);
                }
            }


            //  if mask did not exist then create it
            if (!maskAlreadyExists) {
                logicallyCombinedMask = Mask.BandMathsType.create(maskName, "", getProduct().getSceneRasterWidth(), getProduct().getSceneRasterHeight(),
                        combinedMaskExpression, Color.RED, 0.5);
                maskGroup.add(logicallyCombinedMask);
            }

        }

        return logicallyCombinedMask;
    }


    private int computeAllStxForRaster(RasterDataNode raster, ProgressMonitor pm, final Mask[] regionMasks, final Mask[] qualityMasks, int stxIdx) {

        int recordCount = 0;

        if (raster == null || regionMasks == null || qualityMasks == null) {
            return recordCount;
        }

        for (Mask regionMask : regionMasks) {
            for (Mask qualityMask : qualityMasks) {
                computeStx(raster, pm, regionMask, qualityMask, stxIdx + recordCount);
                recordCount++;
            }
        }

        return recordCount;
    }


    private int getFullPixelCount(RasterDataNode raster, ProgressMonitor pm, Mask mask) {

        int pixelCount = -1;
        final Stx stx;
        final ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);

        if (mask != null) {
            stx = new StxFactory()
                    .withRoiMask(mask)
                    .create(raster, subPm);


        } else {
            stx = new StxFactory()
                    .create(raster, subPm);
        }


        Histogram histogram = stx.getHistogram();
        pixelCount = histogram.getTotals()[0];

        return pixelCount;
    }


    private String combineMaskExpressions(Mask[] masks, MultipleRoiComputePanel.MaskGrouping maskGrouping, String maskName) {

        if (masks == null || masks.length == 0) {
            return null;
        }

        ArrayList<String> expressionParts = new ArrayList<String>();

        for (Mask mask : masks) {
            if (mask != null && mask.getName() != null && mask.getName().length() > 0 && !mask.getName().equals(maskName)) {
                expressionParts.add(mask.getName());
            }
        }

        String[] expressionPartsArray = new String[expressionParts.size()];
        expressionPartsArray = expressionParts.toArray(expressionPartsArray);


        if (expressionPartsArray.length > 0) {
            switch (maskGrouping) {
                case INTERSECTION:
                    return StringUtils.join(expressionPartsArray, " && ");
                case UNION:
                    return StringUtils.join(expressionPartsArray, " || ");
                case COMPLEMENT:
                    for (int i = 0; i < expressionPartsArray.length; i++) {
                        expressionPartsArray[i] = "!" + expressionPartsArray[i];
                    }
                    return StringUtils.join(expressionPartsArray, " && ");
                default:
                    return null;
            }
        }

        return null;
    }


    private Mask combineMasks(Mask[] masks, String maskName, int combinationType, Product product) {

        ArrayList<String> expressionParts = new ArrayList<String>();

        int height = 0;
        int width = 0;
        Mask.ImageType imageType = null;

        for (Mask mask : masks) {
            if (mask != null && mask.getName() != null && mask.getName().length() > 0) {
                expressionParts.add(mask.getName());

                if (imageType == null) {
                    imageType = mask.getImageType();
                }
                if (height == 0) {
                    height = mask.getRasterHeight();
                }
                if (width == 0) {
                    width = mask.getRasterWidth();
                }
            }
        }

        String expression = "";
        if (expressionParts.size() > 0) {
            expression = StringUtils.join(expressionParts, " && ");
        }


        //   Mask maskCombined = new Mask("CombinedMask", width, height, new Mask.ImageType("Maths"));
        if (expression != null && expression.length() > 0) {
            //     Mask combinedMask = new Mask("CombinedMask", width, height, imageType);

            Mask combinedMask = Mask.BandMathsType.create(maskName, "", product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                    expression, Color.RED, 0.5);


            //     combinedMask.updateExpression(combinedMask.getValidMaskExpression(), expression);
            //  combinedMask.setSourceImage(VirtualBand.createVirtualSourceImage(combinedMask, expression));
            // combinedMask.setValidPixelExpression(expression);
            return combinedMask;
        } else {
            return null;
        }
    }

    private Mask combineMasksByExpression(Mask mask1, Mask mask2) {

        ArrayList<String> expressionParts = new ArrayList<String>();

        int height = 0;
        int width = 0;
        Mask.ImageType imageType = null;
        if (mask1 != null && mask1.getName() != null && mask1.getName().length() > 0) {
            expressionParts.add(mask1.getName());

            imageType = mask1.getImageType();
            height = mask1.getRasterHeight();
            width = mask1.getRasterWidth();
        }


        if (mask2 != null && mask2.getName() != null && mask2.getName().length() > 0) {
            expressionParts.add(mask2.getName());
            imageType = mask2.getImageType();
            height = mask2.getRasterHeight();
            width = mask2.getRasterWidth();
        }

        String expression = "";
        if (expressionParts.size() > 0) {
            expression = StringUtils.join(expressionParts, " && ");
        }


        //   Mask maskCombined = new Mask("CombinedMask", width, height, new Mask.ImageType("Maths"));
        if (expression != null && expression.length() > 0) {
            return new Mask("CombinedMask", width, height, imageType);
        } else {
            return null;
        }
    }


    private String getValidPixelExpressionWithQualityMask(String validPixExp, Mask qualityMask) {

        ArrayList<String> expressionParts = new ArrayList<String>();

        if (validPixExp != null && validPixExp.length() > 0) {
            expressionParts.add(validPixExp);
        }


        if (qualityMask != null && qualityMask.getImageConfig() != null && qualityMask.getImageConfig().getValue("expression") != null) {
            //     expressionParts.add(qualityMask.getName());
            expressionParts.add(qualityMask.getImageConfig().getValue("expression").toString());
        }

        if (expressionParts.size() > 0) {
            return StringUtils.join(expressionParts, " && ");
        } else {
            return "";
        }

    }

    private void computeStx(RasterDataNode raster, ProgressMonitor pm, Mask regionMask, Mask qualityMask, int stxIdx) {
        final Stx stx;
        final ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);

        String initialValidPixExp = raster.getValidPixelExpression();
        String newValidPixExp = getValidPixelExpressionWithQualityMask(initialValidPixExp, qualityMask);


        // todo this triggers Errors due to node listening
        raster.setValidPixelExpression(newValidPixExp);

        if (regionMask != null) {
            stx = new StxFactory()
                    .withHistogramBinCount(statisticsCriteriaPanel.getNumBins())
                    .withLogHistogram(statisticsCriteriaPanel.isLogMode())
                    .withMedian(statisticsCriteriaPanel.includeMedian())
                    .withBinMin(statisticsCriteriaPanel.getBinMin())
                    .withBinMax(statisticsCriteriaPanel.getBinMax())
                    .withBinWidth(statisticsCriteriaPanel.getBinWidth())
                    .withRoiMask(regionMask)
                    .create(raster, subPm);


        } else {
            stx = new StxFactory()
                    .withHistogramBinCount(statisticsCriteriaPanel.getNumBins())
                    .withLogHistogram(statisticsCriteriaPanel.isLogMode())
                    .withMedian(statisticsCriteriaPanel.includeMedian())
                    .withBinMin(statisticsCriteriaPanel.getBinMin())
                    .withBinMax(statisticsCriteriaPanel.getBinMax())
                    .withBinWidth(statisticsCriteriaPanel.getBinWidth())
                    .create(raster, subPm);
        }

        histograms[stxIdx] = stx.getHistogram();

        // todo this triggers Errors due to node listening
        if (statisticsCriteriaPanel.includeTotalPixels()) {
            addRawTotalToStx(raster, pm, regionMask, stx);
        }

        // publish(new ComputeResult(stx1, null));


        JPanel statPanel = createStatPanel(stx, regionMask, qualityMask, stxIdx, raster);
        contentPanel.add(statPanel);
        updateLeftPanel();

        // todo this triggers Errors due to node listening
        raster.setValidPixelExpression(initialValidPixExp);
    }



    // get full pixel count of ROI (including any NaN pixels)
    private void addRawTotalToStx(RasterDataNode raster, ProgressMonitor pm, Mask mask, Stx stx) {

        if (raster != null) {
            Product prod = raster.getProduct();

            if (prod != null) {
                boolean addedNoNanBandRaster = false;
                if (noNanBandRaster == null) {
                    addNoNanBand(prod);
                    addedNoNanBandRaster = true;
                }

                if (noNanBandRaster != null) {
                    int fullPixelCount = getFullPixelCount(noNanBandRaster, pm, mask);
                    stx.setRawTotal(fullPixelCount);

                    if (addedNoNanBandRaster) {
                        removeNoNanBand(prod);
                    }
                }
            }
        }
    }



    // creates a band "noNanBandRaster" which contains a value of "1" in every pixel.  Used to assess total number of pixels in a ROI
    private void addNoNanBand(Product prod) {

        if (prod != null) {
            final int width = prod.getSceneRasterWidth();
            final int height = prod.getSceneRasterHeight();

            Band band = new Band(NO_NAN_BANDNAME, ProductData.TYPE_FLOAT32, width, height);

            if (band != null) {
                prod.addBand(band);
                band.setSourceImage(VirtualBand.createSourceImage(band, "1"));
                noNanBandRaster = prod.getRasterDataNode(band.getName());
            }
        }
    }




    //  deletes band "noNanBandRaster" which contains a value of "1" in every pixel.
    private void removeNoNanBand(Product prod) {
        if (prod != null && noNanBandRaster != null) {
            Band band = prod.getBand(noNanBandRaster.getName());

            if (band != null) {
                prod.removeBand(band);
            }
        }
    }


    private void abortRun() {
        initLeftPanel();
        fixedHistDomainAllPlotsInitialized = false;
        computePanel.setRunning(false);
    }


    private void initLeftPanel() {
        leftPanel.removeAll();
        leftPanel.add(new JLabel(DEFAULT_STATISTICS_TEXT));
        leftPanel.revalidate();
        leftPanel.repaint();
        // leftPanel.setBackground(Color.WHITE);
        fixedHistDomainAllPlotsInitialized = false;
    }

    private void updateLeftPanel() {

        PagePanel pagePanel = new StatisticsSpreadsheetPagePanel(parentDialog, helpID, statisticsCriteriaPanel, statsSpreadsheet, this);
        pagePanel.initComponents();

        spreadsheetPanel.removeAll();
        //  JPanel statsSpeadPanel = statsSpreadsheetPanel();
        //  spreadsheetPanel.add(statsSpeadPanel);
        spreadsheetPanel.add(pagePanel);
        //   spreadsheetPanel.setBackground(Color.WHITE);
        spreadsheetScrollPane.setVisible(statisticsCriteriaPanel.showStatsSpreadSheet());
        spreadsheetScrollPane.setMinimumSize(new Dimension(100, 100));


        leftPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // histogramPanel.setVisible(showHistogramPlots);

        if (statisticsCriteriaPanel.showPercentPlots() || statisticsCriteriaPanel.showHistogramPlots() || statisticsCriteriaPanel.showStatsList()) {

            if ((numStxRegions + 1) > spreadsheetMinRowsBeforeWeight) {
                gbc.weighty = 1.0 - spreadsheetHeightWeight;
                leftPanel.add(contentScrollPane, gbc);

                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = spreadsheetHeightWeight;

            } else {
                leftPanel.add(contentScrollPane, gbc);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weighty = 0;

                int buffer = 50;
                int minHeight = spreadsheetPanel.getPreferredSize().height + buffer;
                spreadsheetScrollPane.setMinimumSize(new Dimension(100, minHeight));
            }

            gbc.gridy += 1;
            gbc.insets.top = 10;


        } else {
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;
        }

        if (statisticsCriteriaPanel.showStatsSpreadSheet()) {
            leftPanel.add(spreadsheetScrollPane, gbc);
        }


//        gbc.gridy += 1;
//        leftPanel.add(pagePanel, gbc);

        leftPanel.revalidate();
        leftPanel.repaint();

        contentPanel.revalidate();
        contentPanel.repaint();
        backgroundPanel.revalidate();
        backgroundPanel.repaint();

    }


    private JPanel createStatPanel(Stx stx, final Mask regionalMask, final Mask qualityMask, int stxIdx, RasterDataNode raster) {

        final Histogram histogram = stx.getHistogram();
        final int row = stxIdx + 1;  // account for header

        boolean includeFileMetaData = statisticsCriteriaPanel.isIncludeFileMetaData();
        boolean includeMaskMetaData = statisticsCriteriaPanel.isIncludeMaskMetaData();
        boolean includeBandMetaData = statisticsCriteriaPanel.isIncludeBandMetaData();
        boolean includeBinningInfo = statisticsCriteriaPanel.isIncludeBinningInfo();
        ;
        boolean includeTimeMetaData = statisticsCriteriaPanel.isIncludeTimeMetaData();
        boolean isIncludeTimeSeriesMetaData = statisticsCriteriaPanel.isIncludeTimeSeriesMetaData();
        boolean includeProjectionParameters = statisticsCriteriaPanel.isIncludeProjectionParameters();
        boolean includeColumnBreaks = statisticsCriteriaPanel.isIncludeColBreaks();


        // Initialize all spreadsheet table indices to -1 (default don't use value)
        if (stxIdx == 0 || metaDataFieldsHashMap == null || primaryStatisticsFieldsHashMap == null) {
            initHashMaps();
        }


        XIntervalSeries histogramSeries = new XIntervalSeries("Histogram");
        double histDomainBounds[] = {histogram.getLowValue(0), histogram.getHighValue(0)};
        double histRangeBounds[] = {Double.NaN, Double.NaN};

        if (!fixedHistDomainAllPlots || (fixedHistDomainAllPlots && !fixedHistDomainAllPlotsInitialized)) {
            if (!statisticsCriteriaPanel.isLogMode()) {
                if (statisticsCriteriaPanel.plotsThreshDomainSpan()) {

                    if (statisticsCriteriaPanel.plotsThreshDomainLow() >= 0.1) {
                        histDomainBounds[0] = histogram.getPTileThreshold((statisticsCriteriaPanel.plotsThreshDomainLow()) / 100)[0];
                    }

                    if (statisticsCriteriaPanel.plotsThreshDomainHigh() <= 99.9) {
                        histDomainBounds[1] = histogram.getPTileThreshold(statisticsCriteriaPanel.plotsThreshDomainHigh() / 100)[0];
                    }

                } else if (statisticsCriteriaPanel.plotsDomainSpan()) {
                    if (!Double.isNaN(statisticsCriteriaPanel.plotsDomainLow())) {
                        histDomainBounds[0] = statisticsCriteriaPanel.plotsDomainLow();
                    }
                    if (!Double.isNaN(statisticsCriteriaPanel.plotsDomainHigh())) {
                        histDomainBounds[1] = statisticsCriteriaPanel.plotsDomainHigh();
                    }
                }

            } else {
                histDomainBounds[0] = histogram.getBinLowValue(0, 0);
                histDomainBounds[1] = histogram.getHighValue(0);
            }


//            if (!LogMode && plotsThreshDomainSpan && plotsThreshDomainLow >= 0.1 && plotsThreshDomainHigh <= 99.9) {
//                histDomainBounds[0] = histogram.getPTileThreshold((plotsThreshDomainLow) / 100)[0];
//                histDomainBounds[1] = histogram.getPTileThreshold(plotsThreshDomainHigh / 100)[0];
//
//            } else {
//                histDomainBounds[0] = histogram.getBinLowValue(0, 0);
//                histDomainBounds[1] = histogram.getHighValue(0);
//            }

            if (fixedHistDomainAllPlots && !fixedHistDomainAllPlotsInitialized) {
                histDomainBoundsAllPlots[0] = histDomainBounds[0];
                histDomainBoundsAllPlots[1] = histDomainBounds[1];
                fixedHistDomainAllPlotsInitialized = true;
            }
        } else {
            histDomainBounds[0] = histDomainBoundsAllPlots[0];
            histDomainBounds[1] = histDomainBoundsAllPlots[1];
        }


        int[] bins = histogram.getBins(0);
        for (int j = 0; j < bins.length; j++) {

            histogramSeries.add(histogram.getBinLowValue(0, j),
                    histogram.getBinLowValue(0, j),
                    j < bins.length - 1 ? histogram.getBinLowValue(0, j + 1) : histogram.getHighValue(0),
                    bins[j]);
        }


        String logTitle = (statisticsCriteriaPanel.isLogMode()) ? "Log10 of " : "";

        ChartPanel histogramPanel = createChartPanel(histogramSeries,
                logTitle + raster.getName() + " (" + raster.getUnit() + ")",
                "Frequency in #Pixels",
                statisticsCriteriaPanel.plotColor(),
                statisticsCriteriaPanel.plotBackgroundColor(),
                statisticsCriteriaPanel.plotLabelColor(),
//                new Color(0, 0, 127),
                histDomainBounds, histRangeBounds);


        //  histogramPanel.setPreferredSize(new Dimension(300, 200));


        if (statisticsCriteriaPanel.exactPlotSize()) {
            histogramPanel.setMinimumSize(new Dimension(statisticsCriteriaPanel.plotSizeWidth(), statisticsCriteriaPanel.plotSizeHeight()));
            histogramPanel.setPreferredSize(new Dimension(statisticsCriteriaPanel.plotSizeWidth(), statisticsCriteriaPanel.plotSizeHeight()));
            histogramPanel.setMaximumSize(new Dimension(statisticsCriteriaPanel.plotSizeWidth(), statisticsCriteriaPanel.plotSizeHeight()));
        } else {
            histogramPanel.setMinimumSize(new Dimension(statisticsCriteriaPanel.plotSizeWidth(), statisticsCriteriaPanel.plotSizeHeight()));
            histogramPanel.setPreferredSize(new Dimension(statisticsCriteriaPanel.plotSizeWidth(), statisticsCriteriaPanel.plotSizeHeight()));
        }

        XIntervalSeries percentileSeries = new XIntervalSeries("Percentile");

//        if (1 == 2 && LogMode) {
//            percentileSeries.add(0,
//                    0,
//                    1,
//                    Math.pow(10, histogram.getLowValue(0)));
//            for (int j = 1; j < 99; j++) {
//                percentileSeries.add(j,
//                        j,
//                        j + 1,
//                        Math.pow(10, histogram.getPTileThreshold(j / 100.0)[0]));
//            }
//            percentileSeries.add(99,
//                    99,
//                    100,
//                    Math.pow(10, histogram.getHighValue(0)));
//
//        } else {
//            percentileSeries.add(0,
//                    0,
//                    0.25,
//                    histogram.getLowValue(0));
//
//            for (double j = 0.25; j < 99.75; j += .25) {
//                percentileSeries.add(j,
//                        j,
//                        j + 1,
//                        histogram.getPTileThreshold(j / 100.0)[0]);
//            }
//            percentileSeries.add(99.75,
//                    99.75,
//                    100,
//                    histogram.getHighValue(0));
//        }


//
//        double fraction = 0;
//        for (int j = 0; j < bins.length; j++) {
//
//             fraction = (1.0) * j / bins.length;
//
//            if (fraction > 0 && fraction < 1) {
//                percentileSeries.add(histogram.getBinLowValue(0, j),
//                        histogram.getBinLowValue(0, j),
//                        j < bins.length - 1 ? histogram.getBinLowValue(0, j + 1) : histogram.getHighValue(0),
//                        histogram.getPTileThreshold(fraction)[0]);
//            }
//
//
//        }
//
//        double test = fraction;


        double[] percentileDomainBounds = {Double.NaN, Double.NaN};
        double[] percentileRangeBounds = {Double.NaN, Double.NaN};
        ChartPanel percentilePanel = null;

        if (invertPercentile) {

            double increment = .01;
            for (double j = 0; j < 100; j += increment) {
                double fraction = j / 100.0;
                double nextFraction = (j + increment) / 100.0;

                if (fraction > 0.0 && fraction < 1.0 && nextFraction > 0.0 && nextFraction < 1.0) {
                    double thresh = histogram.getPTileThreshold(fraction)[0];
                    double nextThresh = histogram.getPTileThreshold(nextFraction)[0];

                    percentileSeries.add(thresh,
                            thresh,
                            nextThresh,
                            j);
                }
            }


            if (!statisticsCriteriaPanel.isLogMode()) {
                percentileDomainBounds[0] = histDomainBounds[0];
                percentileDomainBounds[1] = histDomainBounds[1];
            }
            percentileRangeBounds[0] = 0;
            percentileRangeBounds[1] = 100;

            percentilePanel = createScatterChartPanel(percentileSeries, logTitle + raster.getName() + " (" + raster.getUnit() + ")", "Percent Threshold",
                    statisticsCriteriaPanel.plotColor(),
                    statisticsCriteriaPanel.plotBackgroundColor(),
                    statisticsCriteriaPanel.plotLabelColor(),
                    percentileDomainBounds,
                    percentileRangeBounds);

        } else {
            percentileSeries.add(0,
                    0,
                    0.25,
                    histogram.getLowValue(0));

            for (double j = 0.25; j < 99.75; j += .25) {
                percentileSeries.add(j,
                        j,
                        j + 1,
                        histogram.getPTileThreshold(j / 100.0)[0]);
            }
            percentileSeries.add(99.75,
                    99.75,
                    100,
                    histogram.getHighValue(0));


            percentileDomainBounds[0] = 0;
            percentileDomainBounds[1] = 100;
            percentileRangeBounds[0] = histDomainBounds[0];
            percentileRangeBounds[1] = histDomainBounds[1];

            percentilePanel = createScatterChartPanel(percentileSeries, "Percent_Threshold", logTitle + raster.getName() + " (" + raster.getUnit() + ")",
                    statisticsCriteriaPanel.plotColor(),
                    statisticsCriteriaPanel.plotBackgroundColor(),
                    statisticsCriteriaPanel.plotLabelColor(),
                    percentileDomainBounds,
                    percentileRangeBounds);


        }

        //   percentilePanel.setPreferredSize(new Dimension(300, 200));
        if (statisticsCriteriaPanel.exactPlotSize()) {
            percentilePanel.setMinimumSize(new Dimension(statisticsCriteriaPanel.plotSizeWidth(), statisticsCriteriaPanel.plotSizeHeight()));
            percentilePanel.setPreferredSize(new Dimension(statisticsCriteriaPanel.plotSizeWidth(), statisticsCriteriaPanel.plotSizeHeight()));
            percentilePanel.setMaximumSize(new Dimension(statisticsCriteriaPanel.plotSizeWidth(), statisticsCriteriaPanel.plotSizeHeight()));
        } else {
            percentilePanel.setMinimumSize(new Dimension(statisticsCriteriaPanel.plotSizeWidth(), statisticsCriteriaPanel.plotSizeHeight()));
            percentilePanel.setPreferredSize(new Dimension(statisticsCriteriaPanel.plotSizeWidth(), statisticsCriteriaPanel.plotSizeHeight()));
        }

        int size = raster.getRasterHeight() * raster.getRasterWidth();

        int validPixelCount = histogram.getTotals()[0];

        int dataRows = 0;

//                new Object[]{"RasterSize(Pixels)", size},
//                new Object[]{"SampleSize(Pixels)", histogram.getTotals()[0]},

        Object[][] totalPixels = null;

        if (statisticsCriteriaPanel.includeTotalPixels()) {
            int totalPixelCount = stx.getRawTotal();
            double percentFilled = (totalPixelCount > 0) ? (1.0 * validPixelCount / totalPixelCount) : 0;

            totalPixels = new Object[][]{
                    new Object[]{"Regional_Pixels", stx.getRawTotal()},
                    new Object[]{"Valid_Pixels", validPixelCount},
                    new Object[]{"Fraction_Valid", percentFilled}
            };

        } else {
            totalPixels = new Object[][]{
                    new Object[]{"Valid_Pixels", validPixelCount}
            };
        }
        dataRows += totalPixels.length;


        Object[][] firstData =
                new Object[][]{
                        new Object[]{"Mean", stx.getMean()}
                };
        dataRows += firstData.length;


        Object[][] minMaxData = null;
        if (statisticsCriteriaPanel.includeMinMax()) {
            minMaxData = new Object[][]{
                    new Object[]{"Minimum", stx.getMinimum()},
                    new Object[]{"Maximum", stx.getMaximum()}
            };
            dataRows += minMaxData.length;
        }


        Object[] medianObject = null;

        if (statisticsCriteriaPanel.includeMedian()) {
            medianObject = new Object[]{"Median", stx.getMedianRaster()};

            dataRows++;
        }


        Object[][] secondData =
                new Object[][]{
                        new Object[]{"Standard_Deviation", stx.getStandardDeviation()},
                        new Object[]{"Variance", getVariance(stx)},
                        new Object[]{"Coefficient_of_Variation", getCoefficientOfVariation(stx)}
                };
        dataRows += secondData.length;


        Object[][] binningInfo = null;
        if (statisticsCriteriaPanel.isIncludeBinningInfo()) {
            binningInfo = new Object[][]{
                    new Object[]{"Total_Bins", histogram.getNumBins()[0]},
                    new Object[]{"Bin_Width", getBinSize(histogram)},
                    new Object[]{"Bin_Min", histogram.getLowValue(0)},
                    new Object[]{"Bin_Max", histogram.getHighValue(0)}
            };

            dataRows += binningInfo.length;
        }


        Object[][] histogramStats = null;
        if (statisticsCriteriaPanel.includeHistogramStats()) {
            if (statisticsCriteriaPanel.isLogMode()) {
                histogramStats = new Object[][]{
                        new Object[]{"Mean(LogBinned)", Math.pow(10, histogram.getMean()[0])},
                        new Object[]{"Median(LogBinned)", Math.pow(10, stx.getMedian())},
                        new Object[]{"StandardDeviation(LogBinned)", Math.pow(10, histogram.getStandardDeviation()[0])}
                };
            } else {
                histogramStats = new Object[][]{
                        new Object[]{"Mean(Binned)", histogram.getMean()[0]},
                        new Object[]{"Median(Binned)", stx.getMedian()},
                        new Object[]{"StandardDeviation(Binned)", histogram.getStandardDeviation()[0]}
                };
            }
            dataRows += histogramStats.length;
        }


        Object[][] percentData = new Object[statisticsCriteriaPanel.getPercentThresholdsList().size()][];
        for (int i = 0; i < statisticsCriteriaPanel.getPercentThresholdsList().size(); i++) {
            int value = statisticsCriteriaPanel.getPercentThresholdsList().get(i);
            double percent = value / 100.0;
            String percentString = Integer.toString(value);

            Object[] pTileThreshold;
            if (statisticsCriteriaPanel.isLogMode()) {
                pTileThreshold = new Object[]{percentString + "%Threshold(Log)", Math.pow(10, histogram.getPTileThreshold(percent)[0])};
            } else {
                pTileThreshold = new Object[]{percentString + "%Threshold", histogram.getPTileThreshold(percent)[0]};
            }
            percentData[i] = pTileThreshold;
        }
        dataRows += percentData.length;


        Object[][] tableData = new Object[dataRows][];
        int tableDataIdx = 0;

        if (totalPixels != null) {
            for (int i = 0; i < totalPixels.length; i++) {
                tableData[tableDataIdx] = totalPixels[i];
                tableDataIdx++;
            }
        }

        if (firstData != null) {
            for (int i = 0; i < firstData.length; i++) {
                tableData[tableDataIdx] = firstData[i];
                tableDataIdx++;
            }
        }

        if (medianObject != null) {
            tableData[tableDataIdx] = medianObject;
            tableDataIdx++;
        }

        if (minMaxData != null) {
            for (int i = 0; i < minMaxData.length; i++) {
                tableData[tableDataIdx] = minMaxData[i];
                tableDataIdx++;
            }
        }

        if (secondData != null) {
            for (int i = 0; i < secondData.length; i++) {
                tableData[tableDataIdx] = secondData[i];
                tableDataIdx++;
            }
        }

        if (binningInfo != null) {
            for (int i = 0; i < binningInfo.length; i++) {
                tableData[tableDataIdx] = binningInfo[i];
                tableDataIdx++;
            }
        }

        if (histogramStats != null) {
            for (int i = 0; i < histogramStats.length; i++) {
                tableData[tableDataIdx] = histogramStats[i];
                tableDataIdx++;
            }
        }

        if (percentData != null) {
            for (int i = 0; i < percentData.length; i++) {
                tableData[tableDataIdx] = percentData[i];
                tableDataIdx++;
            }
        }

        numStxFields = tableData.length;


        int fieldIdx = 0;

        // Initialize indices
        if (stxIdx == 0) {

            primaryStatisticsFieldsHashMap.put(PrimaryStatisticsFields.FileRefNum, fieldIdx);
            fieldIdx++;
            primaryStatisticsFieldsHashMap.put(PrimaryStatisticsFields.BandName, fieldIdx);
            fieldIdx++;
            primaryStatisticsFieldsHashMap.put(PrimaryStatisticsFields.MaskName, fieldIdx);
            fieldIdx++;
            primaryStatisticsFieldsHashMap.put(PrimaryStatisticsFields.QualityMaskName, fieldIdx);
            fieldIdx++;

            stxFieldsStartIdx = fieldIdx;
            fieldIdx += numStxFields;
            stxFieldsEndIdx = fieldIdx - 1;
            if (includeBandMetaData) {
                if (includeColumnBreaks) {
                    metaDataFieldsHashMap.put(MetaDataFields.BandMetaDataBreak, fieldIdx);
                    fieldIdx++;
                }
                metaDataFieldsHashMap.put(MetaDataFields.BandName, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.BandUnit, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.BandValidExpression, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.BandDescription, fieldIdx);
                fieldIdx++;

            }


            if (includeMaskMetaData) {
                if (includeColumnBreaks) {
                    metaDataFieldsHashMap.put(MetaDataFields.RegionalMaskMetaDataBreak, fieldIdx);
                    fieldIdx++;
                }
                metaDataFieldsHashMap.put(MetaDataFields.RegionalMaskName, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.RegionalMaskDescription, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.RegionalMaskExpression, fieldIdx);
                fieldIdx++;

                if (includeColumnBreaks) {
                    metaDataFieldsHashMap.put(MetaDataFields.QualityMaskMetaDataBreak, fieldIdx);
                    fieldIdx++;
                }
                metaDataFieldsHashMap.put(MetaDataFields.QualityMaskName, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.QualityMaskDescription, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.QualityMaskExpression, fieldIdx);
                fieldIdx++;
            }

            if (includeTimeMetaData || isIncludeTimeSeriesMetaData) {
                if (includeColumnBreaks) {
                    metaDataFieldsHashMap.put(MetaDataFields.TimeMetaDataBreak, fieldIdx);
                    fieldIdx++;
                }

                if (includeTimeMetaData) {
                    metaDataFieldsHashMap.put(MetaDataFields.StartDate, fieldIdx);
                    fieldIdx++;
                    metaDataFieldsHashMap.put(MetaDataFields.StartTime, fieldIdx);
                    fieldIdx++;
                    metaDataFieldsHashMap.put(MetaDataFields.EndDate, fieldIdx);
                    fieldIdx++;
                    metaDataFieldsHashMap.put(MetaDataFields.EndTime, fieldIdx);
                    fieldIdx++;
                }

                if (isIncludeTimeSeriesMetaData) {
                    metaDataFieldsHashMap.put(MetaDataFields.TimeSeriesDate, fieldIdx);
                    fieldIdx++;
                    metaDataFieldsHashMap.put(MetaDataFields.TimeSeriesTime, fieldIdx);
                    fieldIdx++;
                }
            }


            if (includeFileMetaData) {
                if (includeColumnBreaks) {
                    metaDataFieldsHashMap.put(MetaDataFields.FileMetaDataBreak, fieldIdx);
                    fieldIdx++;
                }
                metaDataFieldsHashMap.put(MetaDataFields.FileName, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.FileType, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.FileFormat, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.FileWidth, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.FileHeight, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.Sensor, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.Platform, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.Resolution, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.DayNight, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.Orbit, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.ProcessingVersion, fieldIdx);
                fieldIdx++;
                metaDataFieldsHashMap.put(MetaDataFields.Projection, fieldIdx);
                fieldIdx++;

            }


            if (includeProjectionParameters) {
                metaDataFieldsHashMap.put(MetaDataFields.ProjectionParameters, fieldIdx);
                fieldIdx++;
            }


        }


        if (statsSpreadsheet == null) {
            statsSpreadsheet = new Object[numStxRegions + 2][fieldIdx];
            // add 1 row to account for the header and 1 more empty row because JTable for some reason displays
            // only half of the last row when row count is large
        }


        String startDateString = "";
        String startTimeString = "";
        String endDateString = "";
        String endTimeString = "";

        if (includeTimeMetaData) {
            ProductData.UTC startDateTimeCorrected;
            ProductData.UTC endDateTimeCorrected;

            // correct time (invert start and end time if end time later than start time
            if (getProduct().getStartTime() != null && getProduct().getEndTime() != null) {
                if (getProduct().getStartTime().getMJD() <= getProduct().getEndTime().getMJD()) {

                    startDateTimeCorrected = getProduct().getStartTime();
                    endDateTimeCorrected = getProduct().getEndTime();
                } else {

                    startDateTimeCorrected = getProduct().getEndTime();
                    endDateTimeCorrected = getProduct().getStartTime();
                }

                if (startDateTimeCorrected != null) {
                    String[] startDateTimeStringArray = startDateTimeCorrected.toString().split(" ");
                    if (startDateTimeStringArray.length >= 2) {
                        startDateString = startDateTimeStringArray[0].trim();
                        startTimeString = startDateTimeStringArray[1].trim();
                    }
                }

                if (endDateTimeCorrected != null) {
                    String[] endDateTimeStringArray = endDateTimeCorrected.toString().split(" ");
                    if (endDateTimeStringArray.length >= 2) {
                        endDateString = endDateTimeStringArray[0].trim();
                        endTimeString = endDateTimeStringArray[1].trim();
                    }
                }
            }
        }


        String timeSeriesDate = "";
        String timeSeriesTime = "";
        if (isIncludeTimeSeriesMetaData) {
            String bandName = raster.getName();

            String productDateTime = convertBandNameToProductTime(bandName);

            if (productDateTime != null) {
                String[] endDateTimeStringArray = productDateTime.split(" ");
                if (endDateTimeStringArray.length >= 2) {
                    timeSeriesDate = endDateTimeStringArray[0].trim();
                    timeSeriesTime = endDateTimeStringArray[1].trim();
                }
            }
        }


        String maskName = "";
        String maskDescription = "";
        String maskExpression = "";
        if (regionalMask != null) {
            maskName = regionalMask.getName();
            maskDescription = regionalMask.getDescription();
            maskExpression = regionalMask.getImageConfig().getValue("expression");
        }

        String qualityMaskName = "";
        String qualityMaskDescription = "";
        String qualityMaskExpression = "";
        if (qualityMask != null) {
            qualityMaskName = qualityMask.getName();
            qualityMaskDescription = qualityMask.getDescription();
            qualityMaskExpression = qualityMask.getImageConfig().getValue("expression");
        }


        addFieldToSpreadsheet(row, PrimaryStatisticsFields.FileRefNum, getProduct().getRefNo());
        addFieldToSpreadsheet(row, PrimaryStatisticsFields.BandName, raster.getName());
        addFieldToSpreadsheet(row, PrimaryStatisticsFields.MaskName, maskName);
        addFieldToSpreadsheet(row, PrimaryStatisticsFields.QualityMaskName, qualityMaskName);

        addFieldToSpreadsheet(row, MetaDataFields.TimeMetaDataBreak, COLUMN_BREAK);
        addFieldToSpreadsheet(row, MetaDataFields.StartDate, startDateString);
        addFieldToSpreadsheet(row, MetaDataFields.StartTime, startTimeString);
        addFieldToSpreadsheet(row, MetaDataFields.EndDate, endDateString);
        addFieldToSpreadsheet(row, MetaDataFields.EndTime, endTimeString);

        addFieldToSpreadsheet(row, MetaDataFields.TimeSeriesDate, timeSeriesDate);
        addFieldToSpreadsheet(row, MetaDataFields.TimeSeriesTime, timeSeriesTime);

        addFieldToSpreadsheet(row, MetaDataFields.FileMetaDataBreak, COLUMN_BREAK);
        addFieldToSpreadsheet(row, MetaDataFields.FileName, getProduct().getName());
        addFieldToSpreadsheet(row, MetaDataFields.FileType, getProduct().getProductType());
        addFieldToSpreadsheet(row, MetaDataFields.FileWidth, getProduct().getSceneRasterWidth());
        addFieldToSpreadsheet(row, MetaDataFields.FileFormat, getProductFormatName(getProduct()));
        addFieldToSpreadsheet(row, MetaDataFields.FileHeight, getProduct().getSceneRasterHeight());
        addFieldToSpreadsheet(row, MetaDataFields.Sensor, ProductUtils.getMetaData(getProduct(), ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS));
        addFieldToSpreadsheet(row, MetaDataFields.Platform, ProductUtils.getMetaData(getProduct(), ProductUtils.METADATA_POSSIBLE_PLATFORM_KEYS));
        addFieldToSpreadsheet(row, MetaDataFields.Resolution, ProductUtils.getMetaData(getProduct(), ProductUtils.METADATA_POSSIBLE_RESOLUTION_KEYS));
        addFieldToSpreadsheet(row, MetaDataFields.DayNight, ProductUtils.getMetaData(getProduct(), ProductUtils.METADATA_POSSIBLE_DAY_NIGHT_KEYS));
        addFieldToSpreadsheet(row, MetaDataFields.Orbit, ProductUtils.getMetaDataOrbit(getProduct()));
        addFieldToSpreadsheet(row, MetaDataFields.ProcessingVersion, ProductUtils.getMetaData(getProduct(), ProductUtils.METADATA_POSSIBLE_PROCESSING_VERSION_KEYS));


        // Determine projection
        String projection = "";
        String projectionParameters = "";
        GeoCoding geo = getProduct().getSceneGeoCoding();
        // determine if using class CrsGeoCoding otherwise display class
        if (geo != null) {
            if (geo instanceof CrsGeoCoding) {
                projection = geo.getMapCRS().getName().toString() + "(obtained from CrsGeoCoding)";
                projectionParameters = geo.getMapCRS().toString().replaceAll("\n", " ").replaceAll(" ", "");
            } else if (geo.toString() != null) {
                String projectionFromMetaData = ProductUtils.getMetaData(getProduct(), ProductUtils.METADATA_POSSIBLE_PROJECTION_KEYS);

                if (projectionFromMetaData != null && projectionFromMetaData.length() > 0) {
                    projection = projectionFromMetaData + "(obtained from MetaData)";
                } else {
                    projection = "unknown (" + geo.getClass().toString() + ")";
                }
            }
        }
        addFieldToSpreadsheet(row, MetaDataFields.Projection, projection);
        addFieldToSpreadsheet(row, MetaDataFields.ProjectionParameters, projectionParameters);


        addFieldToSpreadsheet(row, MetaDataFields.BandMetaDataBreak, COLUMN_BREAK);
        addFieldToSpreadsheet(row, MetaDataFields.BandName, raster.getName());
        addFieldToSpreadsheet(row, MetaDataFields.BandUnit, raster.getUnit());
        addFieldToSpreadsheet(row, MetaDataFields.BandValidExpression, raster.getValidPixelExpression());
        addFieldToSpreadsheet(row, MetaDataFields.BandDescription, raster.getDescription());

        addFieldToSpreadsheet(row, MetaDataFields.RegionalMaskMetaDataBreak, COLUMN_BREAK);
        addFieldToSpreadsheet(row, MetaDataFields.RegionalMaskName, maskName);
        addFieldToSpreadsheet(row, MetaDataFields.RegionalMaskDescription, maskDescription);
        addFieldToSpreadsheet(row, MetaDataFields.RegionalMaskExpression, maskExpression);

        addFieldToSpreadsheet(row, MetaDataFields.QualityMaskMetaDataBreak, COLUMN_BREAK);
        addFieldToSpreadsheet(row, MetaDataFields.QualityMaskName, qualityMaskName);
        addFieldToSpreadsheet(row, MetaDataFields.QualityMaskDescription, qualityMaskDescription);
        addFieldToSpreadsheet(row, MetaDataFields.QualityMaskExpression, qualityMaskExpression);


        // Add Header first time through
        if (row <= 1) {

            int k = stxFieldsStartIdx;
            for (int i = 0; i < tableData.length; i++) {
                Object value = tableData[i][0];


                if (k < statsSpreadsheet[0].length && k <= stxFieldsEndIdx) {
                    statsSpreadsheet[0][k] = value;
                    k++;
                }
            }

        }


        // account for header as added row
        if (row < statsSpreadsheet.length) {

            int k = stxFieldsStartIdx;
            for (int i = 0; i < tableData.length; i++) {
                Object value = tableData[i][1];

                if (k < statsSpreadsheet[row].length && k <= stxFieldsEndIdx) {
                    statsSpreadsheet[row][k] = value;
                    k++;
                }
            }

        }


        int numPlots = 0;
        if (statisticsCriteriaPanel.showPercentPlots()) {
            numPlots++;
        }

        if (statisticsCriteriaPanel.showHistogramPlots()) {
            numPlots++;
        }

        JPanel plotContainerPanel = null;

        if (numPlots > 0) {
            plotContainerPanel = new JPanel(new GridLayout(1, numPlots));

            if (statisticsCriteriaPanel.showHistogramPlots()) {
                plotContainerPanel.add(histogramPanel);
            }

            if (statisticsCriteriaPanel.showPercentPlots()) {
                plotContainerPanel.add(percentilePanel);
            }
        }


        TableModel tableModel = new DefaultTableModel(tableData, new String[]{"Name", "Value"}) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : Number.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };


        final JTable table = new JTable(tableModel);
        table.setDefaultRenderer(Number.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Float || value instanceof Double) {
                    setHorizontalTextPosition(RIGHT);
                    setText(getFormattedValue((Number) value));
                }
                return label;
            }

            private String getFormattedValue(Number value) {
                if (value.doubleValue() < 0.001 && value.doubleValue() > -0.001 && value.doubleValue() != 0.0) {
                    return new DecimalFormat("0.####E0").format(value.doubleValue());
                }
                String format = "%." + Integer.toString(statisticsCriteriaPanel.decimalPlaces()) + "f";

                return String.format(format, value.doubleValue());
            }
        });
        table.addMouseListener(popupHandler);


        // TEST CODE generically preferred size of each column based on longest expected entry
        // fails a bit because decimal formatting is not captured
        // stub of code commented out in case we want to make it work
        // meanwhile longest entry is being used SEE below

//        int column0Length = 0;
//        int column1Length = 0;
//        FontMetrics fm = table.getFontMetrics(table.getFont());
//        for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
//            String test = table.getValueAt(rowIndex,0).toString();
//            int currColumn0Length = fm.stringWidth(table.getValueAt(rowIndex,0).toString());
//            if (currColumn0Length > column0Length) {
//                column0Length = currColumn0Length;
//            }
//
//            String test2 = table.getValueAt(rowIndex,1).toString();
//            int currColumn1Length = fm.stringWidth(table.getValueAt(rowIndex,1).toString());
//            if (currColumn1Length > column1Length) {
//                column1Length = currColumn1Length;
//            }
//        }


        // Set preferred size of each column based on longest expected entry
        FontMetrics fm = table.getFontMetrics(table.getFont());
        TableColumn column = null;
        int col1PreferredWidth = -1;
        if (statisticsCriteriaPanel.isLogMode()) {
            col1PreferredWidth = fm.stringWidth("StandardDeviation(LogBinned):") + 10;
        } else {
            col1PreferredWidth = fm.stringWidth("StandardDeviation(Binned):") + 10;
        }


        // int col1PreferredWidth = fm.stringWidth("wwwwwwwwwwwwwwwwwwwwwwwwww");
        int col2PreferredWidth = fm.stringWidth("1234567890") + 10;
        int tablePreferredWidth = col1PreferredWidth + col2PreferredWidth;
        for (int i = 0; i < 2; i++) {
            column = table.getColumnModel().getColumn(i);
            if (i == 0) {
                column.setPreferredWidth(col1PreferredWidth);
                column.setMaxWidth(col1PreferredWidth);
            } else {
                column.setPreferredWidth(col2PreferredWidth);
            }
        }


        JPanel textContainerPanel = new JPanel(new BorderLayout(2, 2));
        //   textContainerPanel.setBackground(Color.WHITE);
        textContainerPanel.add(table, BorderLayout.CENTER);
        textContainerPanel.addMouseListener(popupHandler);

        JPanel statsPane = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints("");
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.weighty = 1;

        Dimension dim = table.getPreferredSize();
        table.setPreferredSize(new Dimension(tablePreferredWidth, dim.height));
        statsPane.add(table, gbc);
        statsPane.setPreferredSize(new Dimension(tablePreferredWidth, dim.height));

        JPanel plotsPane = null;

        if (plotContainerPanel != null) {
            plotsPane = GridBagUtils.createPanel();
            plotsPane.setBackground(statisticsCriteriaPanel.plotBackgroundColor());
//            plotsPane.setBackground(Color.WHITE);
            //    plotsPane.setBorder(UIUtils.createGroupBorder(" ")); /*I18N*/
            GridBagConstraints gbcPlots = GridBagUtils.createConstraints("");
            gbcPlots.gridy = 0;
            if (statisticsCriteriaPanel.exactPlotSize()) {
                gbcPlots.fill = GridBagConstraints.NONE;
            } else {
                gbcPlots.fill = GridBagConstraints.BOTH;
            }

            gbcPlots.anchor = GridBagConstraints.NORTHWEST;
            gbcPlots.weightx = 0.5;
            gbcPlots.weighty = 1;
            plotsPane.add(plotContainerPanel, gbcPlots);
        }


        JPanel mainPane = GridBagUtils.createPanel();
        mainPane.setBorder(UIUtils.createGroupBorder(getSubPanelTitle(regionalMask, qualityMask, raster))); /*I18N*/
        GridBagConstraints gbcMain = GridBagUtils.createConstraints("");
        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.anchor = GridBagConstraints.NORTHWEST;
        if (plotsPane != null) {
            gbcMain.fill = GridBagConstraints.VERTICAL;
            gbcMain.weightx = 0;
        } else {
            gbcMain.fill = GridBagConstraints.BOTH;
            gbcMain.weightx = 1;
        }

        if (statisticsCriteriaPanel.showStatsList()) {
            gbcMain.weighty = 1;
            mainPane.add(statsPane, gbcMain);
            gbcMain.gridx++;
        }


        gbcMain.weightx = 1;
        gbcMain.weighty = 1;
        gbcMain.fill = GridBagConstraints.BOTH;


        if (plotsPane != null) {
            mainPane.add(plotsPane, gbcMain);
        }


        return mainPane;
    }


    private void addFieldToSpreadsheet(int idx, int row, String header, String entry) {

        if (idx > 0 && idx < statsSpreadsheet[0].length) {
            if (row <= 1) {
                statsSpreadsheet[0][idx] = header;
            }

            if (row < statsSpreadsheet[0].length) {
                statsSpreadsheet[row][idx] = entry;
            }
        }
    }

    private void addFieldToSpreadsheet(int row, MetaDataFields fileMetaDataField, Object entry) {

        int idx = metaDataFieldsHashMap.get(fileMetaDataField);
        if (idx > 0 && idx < statsSpreadsheet[0].length) {
            if (row <= 1) {
                statsSpreadsheet[0][idx] = fileMetaDataField.toString();
            }

            if (row < statsSpreadsheet[0].length) {
                statsSpreadsheet[row][idx] = entry;
            }
        }
    }

    private void addFieldToSpreadsheet(int row, PrimaryStatisticsFields primaryStatisticsField, Object entry) {

        int idx = primaryStatisticsFieldsHashMap.get(primaryStatisticsField);
        if (idx >= 0 && idx < statsSpreadsheet[0].length) {
            if (row <= 1) {
                statsSpreadsheet[0][idx] = primaryStatisticsField.toString();
            }

            if (row < statsSpreadsheet[0].length) {
                statsSpreadsheet[row][idx] = entry;
            }
        }
    }


    static double getBinSize(Histogram histogram) {
        return (histogram.getHighValue(0) - histogram.getLowValue(0)) / histogram.getNumBins(0);
    }

    static double getBinSizeLogMode(Histogram histogram) {
        return (Math.pow(10, histogram.getHighValue(0)) - Math.pow(10, histogram.getLowValue(0))) / histogram.getNumBins(0);
    }

    private String getSubPanelTitle(Mask regionalMask, Mask qualityMask, RasterDataNode raster) {
        StringBuilder sb = new StringBuilder("");

        if (regionalMask != null && regionalMask.getName() != null) {
            sb.append("regional_mask=" + regionalMask.getName() + "  ");
        }

        if (qualityMask != null && qualityMask.getName() != null) {
            sb.append("quality_mask=" + qualityMask.getName() + " ");
        }

        final String title;
        if (sb.length() > 0) {
            title = String.format("<html><b>%s  (%s)</b></html>", raster.getName(), sb.toString());
        } else {
            title = String.format("<html><b>%s</b></html>", raster.getName());
        }
        return title;
    }

    @Override
    protected String getDataAsText() {
        return resultText.toString();
    }


    // todo Possibly delete this method as it has been replaced ...
    private String createText(final Stx stx, final Mask mask) {

        if (stx.getSampleCount() == 0) {
            if (mask != null) {
                return "The ROI-Mask '" + mask.getName() + "' is empty.";
            } else {
                return "The scene contains no valid pixels.";
            }
        }

        RasterDataNode raster = getRaster();
        boolean maskUsed = mask != null;
        final String unit = (StringUtils.isNotNullAndNotEmpty(raster.getUnit()) ? raster.getUnit() : "1");
        final long numPixelTotal = (long) raster.getRasterWidth() * (long) raster.getRasterHeight();
        final StringBuilder sb = new StringBuilder(1024);

        sb.append("Only ROI-mask pixels considered:\t");
        sb.append(maskUsed ? "Yes" : "No");
        sb.append("\n");

        if (maskUsed) {
            sb.append("ROI-mask name:\t");
            sb.append(mask.getName());
            sb.append("\n");
        }

        sb.append("Number of pixels total:\t");
        sb.append(numPixelTotal);
        sb.append("\n");

        sb.append("Number of considered pixels:\t");
        sb.append(stx.getSampleCount());
        sb.append("\n");

        sb.append("Ratio of considered pixels:\t");
        sb.append(100.0 * stx.getSampleCount() / numPixelTotal);
        sb.append("\t");
        sb.append("%");
        sb.append("\n");

        sb.append("Minimum:\t");
        sb.append(stx.getMinimum());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Maximum:\t");
        sb.append(stx.getMaximum());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Mean:\t");
        sb.append(stx.getMean());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Standard deviation:\t");
        sb.append(stx.getStandardDeviation());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Coefficient of variation:\t");
        sb.append(getCoefficientOfVariation(stx));
        sb.append("\t");
        sb.append("");
        sb.append("\n");

        sb.append("Bin Median:\t");
        sb.append(stx.getMedianRaster());
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        for (int percentile = 5; percentile <= 95; percentile += 5) {
            sb.append("P").append(percentile).append(" threshold:\t");
            sb.append(stx.getHistogram().getPTileThreshold(percentile / 100.0)[0]);
            sb.append("\t");
            sb.append(unit);
            sb.append("\n");
        }

        sb.append("Threshold max error:\t");
        sb.append(getBinSize(stx.getHistogram()));
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        return sb.toString();
    }


    private String createText() {

        if (statsSpreadsheet == null || statsSpreadsheet.length == 0 || statsSpreadsheet[0].length == 0) {
            return "No Statistics Processed";
        }

        final StringBuilder sb = new StringBuilder();


        for (int rowIdx = 1; rowIdx < statsSpreadsheet.length; rowIdx++) {

            for (int colIdx = 0; colIdx < statsSpreadsheet[0].length; colIdx++) {
                Object valueObject = statsSpreadsheet[rowIdx][colIdx];
                Object fieldObject = statsSpreadsheet[0][colIdx];

                if (valueObject == null || fieldObject == null) {
                    sb.append("");
                } else {
                    String field = fieldObject.toString();
                    sb.append(field + ": ");

                    if (valueObject instanceof Float || valueObject instanceof Double) {
                        String valueFormatted = getFormattedValue((Number) valueObject);
                        sb.append(valueFormatted);
                    } else {
                        sb.append(valueObject.toString());
                    }
                }

                if (colIdx < statsSpreadsheet[0].length - 1) {
                    sb.append("\n");
                }
            }

            sb.append("\n\n");
        }

        return sb.toString();
    }


    private double getCoefficientOfVariation(Stx stx) {
        return stx.getStandardDeviation() / stx.getMean();
    }

    private double getVariance(Stx stx) {
        return stx.getStandardDeviation() * stx.getStandardDeviation();
    }


    @Override
    public void doLayout() {
        super.doLayout();
        backgroundPanel.setBounds(0, 0, getWidth() - 8, getHeight() - 8);
        hideAndShowButton.setBounds(getWidth() - hideAndShowButton.getWidth() - 12, 6, 24, 24);
    }

    private static ChartPanel createChartPanel(XIntervalSeries percentileSeries, String xAxisLabel, String yAxisLabel, Color color, Color backgroundColor, Color labelColor,double domainBounds[], double rangeBounds[]) {
        XIntervalSeriesCollection percentileDataset = new XIntervalSeriesCollection();
        percentileDataset.addSeries(percentileSeries);
        return getHistogramPlotPanel(percentileDataset, xAxisLabel, yAxisLabel, color, backgroundColor, labelColor, domainBounds, rangeBounds);
    }

    private static ChartPanel createScatterChartPanel(XIntervalSeries percentileSeries, String xAxisLabel, String yAxisLabel, Color color, Color backgroundColor, Color labelColor, double domainBounds[], double rangeBounds[]) {
        XIntervalSeriesCollection percentileDataset = new XIntervalSeriesCollection();
        percentileDataset.addSeries(percentileSeries);
        return getScatterPlotPanel(percentileDataset, xAxisLabel, yAxisLabel, color, backgroundColor, labelColor, domainBounds, rangeBounds);
    }

    private static ChartPanel getHistogramPlotPanel(XIntervalSeriesCollection dataset, String xAxisLabel, String yAxisLabel, Color color, Color backgoundColor, Color labelColor, double domainBounds[], double rangeBounds[]) {
        JFreeChart chart = ChartFactory.createHistogram(
                null,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                false,  // Legend?
                true,   // tooltips
                false   // url
        );
        final XYPlot xyPlot = chart.getXYPlot();
        //xyPlot.setForegroundAlpha(0.85f);
        xyPlot.setBackgroundPaint(backgoundColor);
        xyPlot.setNoDataMessage("No data");
        xyPlot.getDomainAxis().setLabelPaint(labelColor);
        xyPlot.getDomainAxis().setTickLabelPaint(labelColor);
        xyPlot.getRangeAxis().setLabelPaint(labelColor);
        xyPlot.getRangeAxis().setTickLabelPaint(labelColor);
        xyPlot.setAxisOffset(new RectangleInsets(5, 5, 5, 10));
        // xyPlot.setInsets(new RectangleInsets(0,0,0,0));



        if (!Double.isNaN(domainBounds[0])) {
            xyPlot.getDomainAxis().setLowerBound(domainBounds[0]);
        }

        if (!Double.isNaN(domainBounds[1])) {
            xyPlot.getDomainAxis().setUpperBound(domainBounds[1]);
        }

        if (!Double.isNaN(rangeBounds[0])) {
            xyPlot.getRangeAxis().setLowerBound(rangeBounds[0]);
        }

        if (!Double.isNaN(rangeBounds[1])) {
            xyPlot.getRangeAxis().setUpperBound(rangeBounds[1]);
        }


        final XYBarRenderer renderer = (XYBarRenderer) xyPlot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setSeriesPaint(0, color);
        StandardXYBarPainter painter = new StandardXYBarPainter();
        renderer.setBarPainter(painter);

        ChartPanel chartPanel = new ChartPanel(chart);

        return chartPanel;
    }

    private static ChartPanel getScatterPlotPanel(XIntervalSeriesCollection dataset, String xAxisLabel, String yAxisLabel, Color color, Color backgroundColor, Color labelColor, double domainBounds[], double rangeBounds[]) {
        //  JFreeChart chart = ChartFactory.createScatterPlot(
        JFreeChart chart = ChartFactory.createXYLineChart(
                null,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                false,  // Legend?
                true,   // tooltips
                false   // url
        );
        final XYPlot xyPlot = chart.getXYPlot();
        xyPlot.setBackgroundPaint(backgroundColor);
//        xyPlot.setBackgroundAlpha(0.0f);
        xyPlot.setNoDataMessage("No data");
        xyPlot.getDomainAxis().setLabelPaint(labelColor);
        xyPlot.getDomainAxis().setTickLabelPaint(labelColor);
        xyPlot.getRangeAxis().setLabelPaint(labelColor);
        xyPlot.getRangeAxis().setTickLabelPaint(labelColor);
        xyPlot.setAxisOffset(new RectangleInsets(5, 5, 5, 10));



        if (!Double.isNaN(domainBounds[0])) {
            xyPlot.getDomainAxis().setLowerBound(domainBounds[0]);
        }

        if (!Double.isNaN(domainBounds[1])) {
            xyPlot.getDomainAxis().setUpperBound(domainBounds[1]);
        }

        if (!Double.isNaN(rangeBounds[0])) {
            xyPlot.getRangeAxis().setLowerBound(rangeBounds[0]);
        }

        if (!Double.isNaN(rangeBounds[1])) {
            xyPlot.getRangeAxis().setUpperBound(rangeBounds[1]);
        }


        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) xyPlot.getRenderer();
        renderer.setSeriesPaint(0, color);
        renderer.setUseFillPaint(true);
        renderer.setDrawOutlines(true);
        renderer.setSeriesShapesFilled(0, true);
        renderer.setSeriesFillPaint(0, color);


        ChartPanel chartPanel = new ChartPanel(chart);
        //    chartPanel.setPreferredSize(new Dimension(300, 200));

        return chartPanel;
    }


    private AbstractButton getExportButton() {
        final AbstractButton export = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Export24.gif"),
                false);
        export.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPopupMenu viewPopup = new JPopupMenu("Export");
                viewPopup.add(exportAsCsvAction);
                viewPopup.add(putStatisticsIntoVectorDataAction);
                final Rectangle buttonBounds = export.getBounds();
                viewPopup.show(export, 1, buttonBounds.height + 1);
            }
        });
        export.setEnabled(false);
        return export;
    }

    @Override
    public RasterDataNode getRasterDataNode() {
        return getRaster();
    }

    @Override
    public ProductNodeGroup<VectorDataNode> getVectorDataNodeGroup() {
        return getRasterDataNode().getProduct().getVectorDataGroup();
    }

    private class PopupHandler extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == 2 || e.isPopupTrigger()) {
                final JPopupMenu menu = new JPopupMenu();
                menu.add(createCopyDataToClipboardMenuItem());
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    // The fields of this class are used by the binding framework
    @SuppressWarnings("UnusedDeclaration")
    static class AccuracyModel {

        private int accuracy = 3;
        private boolean useAutoAccuracy = true;
    }


    private boolean retrieveValidateTextFields(boolean showDialog) {

        if (!statisticsCriteriaPanel.validatePrepare()) {
            return false;
        }

        return true;
    }


    private String getFormattedValue(Number value) {
        if (value.doubleValue() < 0.001 && value.doubleValue() > -0.001 && value.doubleValue() != 0.0) {
            return new DecimalFormat("0.####E0").format(value.doubleValue());
        }
        String format = "%." + Integer.toString(statisticsCriteriaPanel.decimalPlaces()) + "f";

        return String.format(format, value.doubleValue());
    }


    private boolean validFields() {
//        if (!validNumBins()) {
//            return false;
//        }

        return true;
    }

    // todo - make this a method in ProductReader and ProductWriter
    private static String getProductFormatName(final Product product) {
        final ProductReader productReader = product.getProductReader();
        if (productReader == null) {
            return null;
        }
        final ProductReaderPlugIn readerPlugIn = productReader.getReaderPlugIn();
        if (readerPlugIn != null) {
            return getProductFormatName(readerPlugIn);
        }
        return null;
    }

    // todo - make this a method in ProductReader and ProductWriter
    private static String getProductFormatName(final ProductReaderPlugIn readerPlugIn) {
        final String[] formatNames = readerPlugIn.getFormatNames();
        if (formatNames != null && formatNames.length > 0) {
            return formatNames[0];
        }
        return null;
    }

    private static String convertBandNameToProductTime(String bandName) {

        Guardian.assertNotNull("bandName", bandName);

        String bandNameDateTime = null;
        String[] bandNameDateTimeArray = bandName.split("_");
        if (bandNameDateTimeArray.length >= 2) {
            // get last one as band name can also have underscore in it.
            bandNameDateTime = bandNameDateTimeArray[bandNameDateTimeArray.length - 1].trim();
        }

        if (bandNameDateTime != null && bandNameDateTime.length() > 13) {
            String year = bandNameDateTime.substring(0, 4);
            String month = bandNameDateTime.substring(4, 6);
            String day = bandNameDateTime.substring(6, 8);

            String hour = bandNameDateTime.substring(9, 11);
            String min = bandNameDateTime.substring(11, 13);
            String sec = bandNameDateTime.substring(13);

            String[] monthNamesArray = new String[]{"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

            int monthIdx = Integer.valueOf(month);
            String monthStr = monthNamesArray[monthIdx - 1];

            String productDate = day + "-" + monthStr + "-" + year;
            String productTime = hour + ":" + min + ":" + sec;

            return productDate + " " + productTime;
        } else {
            return null;
        }
    }


}