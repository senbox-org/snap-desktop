package org.esa.snap.rcp.statistics;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.TextFieldContainer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Created by knowles on 4/18/17.
 */
public class StatisticsCriteriaPanel {

    private Container getParentDialogContentPane;
    Preferences preferences = SnapApp.getDefault().getPreferences();


    // "Bins" Tab Variables and Components

    private int numBins = StatisticsTopComponent.PARAM_DEFVAL_NUM_BINS;
    private TextFieldContainer numBinsTextfieldContainer = null;

    private double binWidth = StatisticsTopComponent.PARAM_DEFVAL_BIN_WIDTH;
    private TextFieldContainer binWidthTextfieldContainer = null;

    private JCheckBox binWidthEnabledCheckBox = null;
    private JCheckBox logModeCheckBox = null;

    private boolean logMode;


    private JCheckBox binMinMaxCheckBox = null;
    double binMin = StatisticsTopComponent.PARAM_DEFVAL_BIN_MIN;
    double binMax = StatisticsTopComponent.PARAM_DEFVAL_BIN_MAX;
    private TextFieldContainer binMinTextfieldContainer = null;
    private TextFieldContainer binMaxTextfieldContainer = null;


    // "Fields" Tab Variables and Components

    private boolean handlersEnabled = true; //todo this may be temporary and not needed
    private String percentThresholds = StatisticsTopComponent.PARAM_DEFVAL_PERCENT_THRESHOLDS;
    private java.util.List<Integer> percentThresholdsList = null;
    private JTextField percentThresholdsTextField = null;
    private JLabel percentThresholdsLabel = null;

    private boolean includeTotalPixels = StatisticsTopComponent.PARAM_DEFVAL_TOTAL_PIXEL_COUNT_ENABLED;
    private JCheckBox includeTotalPixelsCheckBox = null;

    private boolean includeMedian = StatisticsTopComponent.PARAM_DEFVAL_MEDIAN_ENABLED;
    private JCheckBox includeMedianCheckBox = null;

    private boolean includeMinMax = StatisticsTopComponent.PARAM_DEFVAL_MINMAX_ENABLED;
    private JCheckBox includeMinMaxCheckBox = null;

    private boolean includeHistogramStats = StatisticsTopComponent.PARAM_DEFVAL_HISTOGRAM_STATS_ENABLED;
    private JCheckBox includeHistogramStatsCheckBox = null;

    private boolean includeFileRefNo = true;
    private boolean includeBandName = true;
    private boolean includeMaskName = true;

    private boolean includeFileMetaData = StatisticsTopComponent.PARAM_DEFVAL_FILE_METADATA_ENABLED;
    private JCheckBox includeFileMetaDataCheckBox = null;

    private boolean includeMaskMetaData = StatisticsTopComponent.PARAM_DEFVAL_MASK_METADATA_ENABLED;
    private JCheckBox includeMaskMetaDataCheckBox = null;


    private boolean includeBandMetaData = StatisticsTopComponent.PARAM_DEFVAL_BAND_METADATA_ENABLED;
    private JCheckBox includeBandMetaDataCheckBox = null;

    private boolean includeBinningInfo = StatisticsTopComponent.PARAM_DEFVAL_BINNING_INFO_ENABLED;
    private JCheckBox includeBinningInfoCheckBox = null;

    private boolean includeTimeSeriesMetaData = StatisticsTopComponent.PARAM_DEFVAL_TIME_SERIES_METADATA_ENABLED;
    private JCheckBox includeTimeSeriesMetaDataCheckBox = null;

    private boolean includeTimeMetaData = StatisticsTopComponent.PARAM_DEFVAL_TIME_METADATA_ENABLED;
    private JCheckBox includeTimeMetaDataCheckBox = null;

    private boolean includeProjectionParameters = StatisticsTopComponent.PARAM_DEFVAL_PROJECTION_PARAMETERS_METADATA_ENABLED;;
    private JCheckBox includeProjectionParametersCheckBox = null;




    // "Text" Tab Variables and Components

    private static final int COL_WIDTH_DEFAULT = StatisticsTopComponent.PARAM_DEFVAL_SPREADSHEET_COL_WIDTH;
    private int colCharWidth = COL_WIDTH_DEFAULT;
    private TextFieldContainer spreadsheetColWidthTextfieldContainer = null;

    private int decimalPlaces = StatisticsTopComponent.PARAM_DEFVAL_SPREADSHEET_DECIMAL_PLACES;
    private TextFieldContainer decimalPlacesTextfieldContainer = null;

    private boolean includeColBreaks = StatisticsTopComponent.PARAM_DEFVAL_TIME_SERIES_METADATA_ENABLED;
    private JCheckBox includeColBreaksCheckBox = null;



    // "Plots" Tab Variables and Components

    private boolean plotsThreshDomainSpan = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_SPAN;
    private double plotsThreshDomainLow = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_LOW;
    private double plotsThreshDomainHigh = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_HIGH;

    private JCheckBox plotsThreshDomainSpanCheckBox = null;
    private TextFieldContainer plotsThreshDomainLowTextfieldContainer = null;
    private TextFieldContainer plotsThreshDomainHighTextfieldContainer = null;

    private boolean plotsDomainSpan = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_DOMAIN_SPAN;
    private double plotsDomainLow = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_DOMAIN_LOW;
    private double plotsDomainHigh = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_DOMAIN_HIGH;

    private JCheckBox plotsDomainSpanCheckBox = null;
    private TextFieldContainer plotsDomainLowTextfieldContainer = null;
    private TextFieldContainer plotsDomainHighTextfieldContainer = null;

    private boolean exactPlotSize = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_SIZE;
    private int plotSizeHeight = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_SIZE_HEIGHT;
    private int plotSizeWidth = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_SIZE_WIDTH;

    private JCheckBox plotsSizeCheckBox = null;
    private TextFieldContainer plotsSizeHeightTextfieldContainer = null;
    private TextFieldContainer plotsSizeWidthTextfieldContainer = null;


    // "View" Tab Variables and Components

    private boolean showPercentPlots = StatisticsTopComponent.PARAM_DEFVAL_PERCENT_PLOT_ENABLED;
    private boolean showHistogramPlots = StatisticsTopComponent.PARAM_DEFVAL_HISTOGRAM_PLOT_ENABLED;
    private boolean showStatsList = StatisticsTopComponent.PARAM_DEFVAL_STATS_LIST_ENABLED;
    private boolean showStatsSpreadSheet = StatisticsTopComponent.PARAM_DEFVAL_STATS_SPREADSHEET_ENABLED;

    private JCheckBox showPercentPlotCheckBox = null;
    private JCheckBox showHistogramPlotCheckBox = null;
    private JCheckBox showStatsListCheckBox = null;
    private JCheckBox showStatsSpreadSheetCheckBox = null;




    public StatisticsCriteriaPanel(Container getParentDialogContentPane) {
        this.getParentDialogContentPane = getParentDialogContentPane;

        initPreferencesAndDefaults();
        createComponents();
    }


    //
    //------------------------------- INIT/RESET -------------------------------------
    //

    public void reset() {
        initPreferencesAndDefaults();
        updateComponents();


        // toggle each checkbox to force event change in listeners and hence establishe all proper initial enablement
        plotsSizeCheckBox.setSelected(!StatisticsTopComponent.PARAM_DEFVAL_PLOTS_SIZE);
        plotsSizeCheckBox.setSelected(StatisticsTopComponent.PARAM_DEFVAL_PLOTS_SIZE);
        plotsDomainSpanCheckBox.setSelected(!StatisticsTopComponent.PARAM_DEFVAL_PLOTS_DOMAIN_SPAN);
        plotsDomainSpanCheckBox.setSelected(StatisticsTopComponent.PARAM_DEFVAL_PLOTS_DOMAIN_SPAN);
        plotsThreshDomainSpanCheckBox.setSelected(!StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_SPAN);
        plotsThreshDomainSpanCheckBox.setSelected(StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_SPAN);
        binWidthEnabledCheckBox.setSelected(!StatisticsTopComponent.PARAM_DEFVAL_BIN_WIDTH_ENABLED);
        binWidthEnabledCheckBox.setSelected(StatisticsTopComponent.PARAM_DEFVAL_BIN_WIDTH_ENABLED);
        binMinMaxCheckBox.setSelected(!StatisticsTopComponent.PARAM_DEFVAL_BIN_MIN_MAX_ENABLED);
        binMinMaxCheckBox.setSelected(StatisticsTopComponent.PARAM_DEFVAL_BIN_MIN_MAX_ENABLED);

    }


    public void initPreferencesAndDefaults() {


        numBins = getPreferencesNumBins();

        binWidth = StatisticsTopComponent.PARAM_DEFVAL_BIN_WIDTH;
        binMin = StatisticsTopComponent.PARAM_DEFVAL_BIN_MIN;
        binMax = StatisticsTopComponent.PARAM_DEFVAL_BIN_MAX;

        logMode = false;


        // Fields
        includeMedian = getPreferencesMedianEnabled();
        includeMinMax = getPreferencesMinMaxEnabled();
        includeTotalPixels = getPreferencesTotalPixelsEnabled();
        includeHistogramStats = getPreferencesHistogramStatsEnabled();
        includeFileMetaData = getPreferencesFileMetaDataEnabled();
        includeMaskMetaData = getPreferencesMaskMetaDataEnabled();
        includeBandMetaData = getPreferencesBandMetaDataEnabled();
        includeBinningInfo = getPreferencesBinningInfoEnabled();
        includeTimeMetaData = getPreferencesFileTimeMetaDataEnabled();
        includeTimeSeriesMetaData = getPreferencesTimeSeriesMetaDataEnabled();
        includeProjectionParameters = getPreferencesProjectionParametersEnabled();
        percentThresholds = getPreferencesPercentThresholds();




        // Text
        colCharWidth = getPreferencesColWidth();
        decimalPlaces = getPreferencesDecimalPlaces();
        includeColBreaks = getPreferencesColumnBreaksEnabled();



        // Plots
        plotsThreshDomainSpan = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_SPAN;
        plotsThreshDomainLow = getPreferencesPlotDomainThreshLow();
        plotsThreshDomainHigh = getPreferencesPlotDomainThreshHigh();

        plotsDomainSpan = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_DOMAIN_SPAN;
        plotsDomainLow = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_DOMAIN_LOW;
        plotsDomainHigh = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_DOMAIN_HIGH;

        exactPlotSize = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_SIZE;
        plotSizeHeight = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_SIZE_HEIGHT;
        plotSizeWidth = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_SIZE_WIDTH;


        // View
        showHistogramPlots = getPreferencesHistogramPlotEnabled();
        showPercentPlots = getPreferencesPercentPlotEnabled();
        showStatsList = getPreferencesStatsListEnabled();
        showStatsSpreadSheet = getPreferencesStatsSpreadSheetEnabled();

    }



    public void updateComponents() {
        // Bins
        numBinsTextfieldContainer.reset(numBins);
        binWidthTextfieldContainer.reset(binWidth);
        binMinTextfieldContainer.reset(binMin);
        binMaxTextfieldContainer.reset(binMax);
        binWidthEnabledCheckBox.setSelected(StatisticsTopComponent.PARAM_DEFVAL_BIN_WIDTH_ENABLED);
        binMinMaxCheckBox.setSelected(StatisticsTopComponent.PARAM_DEFVAL_BIN_MIN_MAX_ENABLED);
        logModeCheckBox.setSelected(logMode);


        // Fields
        includeTotalPixelsCheckBox.setSelected(includeTotalPixels);
        includeMedianCheckBox.setSelected(includeMedian);
        includeMinMaxCheckBox.setSelected(includeMinMax);
        includeHistogramStatsCheckBox.setSelected(includeHistogramStats);
        includeFileMetaDataCheckBox.setSelected(includeFileMetaData);
        includeMaskMetaDataCheckBox.setSelected(includeMaskMetaData);
        includeBandMetaDataCheckBox.setSelected(includeBandMetaData);
        includeBinningInfoCheckBox.setSelected(includeBinningInfo);
        includeTimeMetaDataCheckBox.setSelected(includeTimeMetaData);
        includeTimeSeriesMetaDataCheckBox.setSelected(includeTimeSeriesMetaData);
        includeProjectionParametersCheckBox.setSelected(includeProjectionParameters);
        percentThresholdsTextField.setText(percentThresholds);

        includeHistogramStatsCheckBox.setVisible(getPreferencesHistogramStatsEnabled());
        includeTimeSeriesMetaDataCheckBox.setVisible(getPreferencesTimeSeriesMetaDataEnabled());
        includeProjectionParametersCheckBox.setVisible(getPreferencesProjectionParametersEnabled());



        // Text
        decimalPlacesTextfieldContainer.reset(decimalPlaces);
        spreadsheetColWidthTextfieldContainer.reset(colCharWidth);
        includeColBreaksCheckBox.setSelected(includeColBreaks);



        // Plots
        plotsThreshDomainSpanCheckBox.setSelected(plotsThreshDomainSpan);
        plotsThreshDomainLowTextfieldContainer.reset(plotsThreshDomainLow);
        plotsThreshDomainHighTextfieldContainer.reset(plotsThreshDomainHigh);

        plotsDomainSpanCheckBox.setSelected(plotsDomainSpan);
        plotsDomainLowTextfieldContainer.reset(plotsDomainLow);
        plotsDomainHighTextfieldContainer.reset(plotsDomainHigh);

        plotsSizeCheckBox.setSelected(exactPlotSize);
        plotsSizeHeightTextfieldContainer.reset(plotSizeHeight);
        plotsSizeWidthTextfieldContainer.reset(plotSizeWidth);


        // View
        showStatsListCheckBox.setSelected(showStatsList);
        showStatsSpreadSheetCheckBox.setSelected(showStatsSpreadSheet);
        showPercentPlotCheckBox.setSelected(showPercentPlots);
        showHistogramPlotCheckBox.setSelected(showHistogramPlots);

    }

    private void createComponents() {

        // "Bins" Tab Variables and Components

        numBinsTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_LABEL_NUM_BINS,
                numBins,
                StatisticsTopComponent.PARAM_MINVAL_NUM_BINS,
                StatisticsTopComponent.PARAM_MAXVAL_NUM_BINS,
                TextFieldContainer.NumType.INT,
                7,
                getParentDialogContentPane);


        binWidthEnabledCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_BIN_WIDTH_ENABLED);

        binWidthTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_LABEL_BIN_WIDTH,
                StatisticsTopComponent.PARAM_DEFVAL_BIN_WIDTH,
                TextFieldContainer.NumType.DOUBLE,
                5,
                getParentDialogContentPane);



        binMinMaxCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_BIN_MIN_MAX_ENABLED);

        binMinTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_SHORTLABEL_BIN_MIN,
                StatisticsTopComponent.PARAM_DEFVAL_BIN_MIN,
                TextFieldContainer.NumType.DOUBLE,
                5,
                getParentDialogContentPane);


        binMaxTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_SHORTLABEL_BIN_MAX,
                StatisticsTopComponent.PARAM_DEFVAL_BIN_MAX,
                TextFieldContainer.NumType.DOUBLE,
                5,
                getParentDialogContentPane);


        logModeCheckBox = new JCheckBox("Log Scaled Bins");


        // "Fields" Tab Variables and Components
        includeTotalPixelsCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_TOTAL_PIXEL_COUNT_ENABLED);

        includeMedianCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_MEDIAN_ENABLED);
        includeMedianCheckBox.setToolTipText(StatisticsTopComponent.PARAM_TOOLTIP_MEDIAN_ENABLED);

        includeMinMaxCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_MINMAX_ENABLED);
        includeMinMaxCheckBox.setToolTipText(StatisticsTopComponent.PARAM_TOOLTIP_MINMAX_ENABLED);

        includeHistogramStatsCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_HISTOGRAM_STATS_ENABLED);

        includeFileMetaDataCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_FILE_METADATA_ENABLED);

        includeMaskMetaDataCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_MASK_METADATA_ENABLED);

        includeBandMetaDataCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_BAND_METADATA_ENABLED);

        includeBinningInfoCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_BINNING_INFO_ENABLED);

        includeTimeMetaDataCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_TIME_METADATA_ENABLED);

        includeTimeSeriesMetaDataCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_TIME_SERIES_METADATA_ENABLED);
        includeTimeSeriesMetaDataCheckBox.setToolTipText(StatisticsTopComponent.PARAM_TOOLTIPS_TIME_SERIES_METADATA_ENABLED);

        includeProjectionParametersCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_PROJECTION_PARAMETERS_METADATA_ENABLED);

        percentThresholdsLabel = new JLabel(StatisticsTopComponent.PARAM_LABEL_PERCENT_THRESHOLDS);
        percentThresholdsTextField = new JTextField(14);
        percentThresholdsTextField.setName(StatisticsTopComponent.PARAM_LABEL_PERCENT_THRESHOLDS);



        // "Text" Tab Variables and Components
        spreadsheetColWidthTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_LABEL_SPREADSHEET_COL_WIDTH,
                StatisticsTopComponent.PARAM_DEFVAL_SPREADSHEET_COL_WIDTH,
                StatisticsTopComponent.PARAM_MINVAL_SPREADSHEET_COL_WIDTH,
                StatisticsTopComponent.PARAM_MAXVAL_SPREADSHEET_COL_WIDTH,
                TextFieldContainer.NumType.INT,
                2,
                getParentDialogContentPane);


        decimalPlacesTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_LABEL_SPREADSHEET_DECIMAL_PLACES,
                StatisticsTopComponent.PARAM_DEFVAL_SPREADSHEET_DECIMAL_PLACES,
                StatisticsTopComponent.PARAM_MINVAL_SPREADSHEET_DECIMAL_PLACES,
                StatisticsTopComponent.PARAM_MAXVAL_SPREADSHEET_DECIMAL_PLACES,
                TextFieldContainer.NumType.INT,
                2,
                getParentDialogContentPane);


        includeColBreaksCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_COL_BREAKS_ENABLED);
        includeColBreaksCheckBox.setSelected(includeColBreaks);



        // "Plots" Tab Variables and Components
        plotsThreshDomainSpanCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_SHORTLABEL_PLOTS_THRESH_DOMAIN_SPAN);

        plotsThreshDomainLowTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_SHORTLABEL_PLOTS_THRESH_DOMAIN_LOW,
                plotsThreshDomainLow,
                StatisticsTopComponent.PARAM_MINVAL_PLOTS_THRESH_DOMAIN_LOW,
                StatisticsTopComponent.PARAM_MAXVAL_PLOTS_THRESH_DOMAIN_LOW,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane);


        plotsThreshDomainHighTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_SHORTLABEL_PLOTS_THRESH_DOMAIN_HIGH,
                plotsThreshDomainHigh,
                StatisticsTopComponent.PARAM_MINVAL_PLOTS_THRESH_DOMAIN_HIGH,
                StatisticsTopComponent.PARAM_MAXVAL_PLOTS_THRESH_DOMAIN_HIGH,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane);



        plotsDomainSpanCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_SHORTLABEL_PLOTS_DOMAIN_SPAN);

        plotsDomainLowTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_LABEL_PLOTS_DOMAIN_LOW,
                plotsDomainLow,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane);

        plotsDomainHighTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_LABEL_PLOTS_DOMAIN_HIGH,
                plotsDomainHigh,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane);



        plotsSizeCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_SHORTLABEL_PLOTS_SIZE);

        plotsSizeHeightTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_SHORTLABEL_PLOTS_SIZE_HEIGHT,
                plotSizeHeight,
                StatisticsTopComponent.PARAM_MINVAL_PLOTS_SIZE_HEIGHT,
                StatisticsTopComponent.PARAM_MAXVAL_PLOTS_SIZE_HEIGHT,
                TextFieldContainer.NumType.INT,
                4,
                getParentDialogContentPane);

        plotsSizeWidthTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PARAM_SHORTLABEL_PLOTS_SIZE_WIDTH,
                plotSizeWidth,
                StatisticsTopComponent.PARAM_MINVAL_PLOTS_SIZE_WIDTH,
                StatisticsTopComponent.PARAM_MAXVAL_PLOTS_SIZE_WIDTH,
                TextFieldContainer.NumType.INT,
                4,
                getParentDialogContentPane);




        // "View" Tab Variables and Components
        showStatsListCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_STATS_LIST_ENABLED);
        showStatsSpreadSheetCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_STATS_SPREADSHEET_ENABLED);
        showPercentPlotCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_PERCENT_PLOT_ENABLED);
        showHistogramPlotCheckBox = new JCheckBox(StatisticsTopComponent.PARAM_LABEL_HISTOGRAM_PLOT_ENABLED);



        addListeners();

        reset();



    }


    //
    //------------------------------- LISTENERS / HANDLERS / -------------------------------------
    //





    private void addListeners() {

        // "Bins" Tab Variables and Components

        binWidthEnabledCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                numBinsTextfieldContainer.setEnabled(!binWidthEnabledCheckBox.isSelected());

                binWidthTextfieldContainer.reset();
                binWidthTextfieldContainer.setEnabled(binWidthEnabledCheckBox.isSelected());
            }
        });

        binMinMaxCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                binMinTextfieldContainer.reset();
                binMaxTextfieldContainer.reset();
                binMinTextfieldContainer.setEnabled(binMinMaxCheckBox.isSelected());
                binMaxTextfieldContainer.setEnabled(binMinMaxCheckBox.isSelected());
            }
        });

        logModeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                logMode = logModeCheckBox.isSelected();

            }
        });


        // "Fields" Tab Variables and Components

        textfieldHandler(percentThresholdsTextField);

        includeTotalPixelsCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeTotalPixels = includeTotalPixelsCheckBox.isSelected();

            }
        });


        includeMedianCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeMedian = includeMedianCheckBox.isSelected();

            }
        });

        includeMinMaxCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeMinMax = includeMinMaxCheckBox.isSelected();

            }
        });

        includeHistogramStatsCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeHistogramStats = includeHistogramStatsCheckBox.isSelected();

            }
        });

        includeFileMetaDataCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeFileMetaData = includeFileMetaDataCheckBox.isSelected();
            }
        });

        includeMaskMetaDataCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeMaskMetaData = includeMaskMetaDataCheckBox.isSelected();
            }
        });

        includeBinningInfoCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeBinningInfo = includeBinningInfoCheckBox.isSelected();
            }
        });

        includeBandMetaDataCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeBandMetaData = includeBandMetaDataCheckBox.isSelected();
            }
        });

        includeTimeSeriesMetaDataCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeTimeSeriesMetaData = includeTimeSeriesMetaDataCheckBox.isSelected();
            }
        });

        includeTimeMetaDataCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeTimeMetaData = includeTimeMetaDataCheckBox.isSelected();
            }
        });

        includeProjectionParametersCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeProjectionParameters = includeProjectionParametersCheckBox.isSelected();
            }
        });




        // "Text" Tab Variables and Components

        includeColBreaksCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeColBreaks = includeColBreaksCheckBox.isSelected();
            }
        });


        // "Plots" Tab Variables and Components


        plotsSizeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                plotsSizeHeightTextfieldContainer.reset();
                plotsSizeWidthTextfieldContainer.reset();
                plotsSizeHeightTextfieldContainer.setEnabled(plotsSizeCheckBox.isSelected());
                plotsSizeWidthTextfieldContainer.setEnabled(plotsSizeCheckBox.isSelected());
            }
        });


        plotsThreshDomainSpanCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                plotsThreshDomainHighTextfieldContainer.reset();
                plotsThreshDomainLowTextfieldContainer.reset();
                plotsThreshDomainHighTextfieldContainer.setEnabled(plotsThreshDomainSpanCheckBox.isSelected());
                plotsThreshDomainLowTextfieldContainer.setEnabled(plotsThreshDomainSpanCheckBox.isSelected());

                if (plotsThreshDomainSpanCheckBox.isSelected()) {
                    plotsDomainSpanCheckBox.setSelected(false);
                }
            }
        });

        plotsDomainSpanCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                plotsDomainHighTextfieldContainer.reset();
                plotsDomainLowTextfieldContainer.reset();
                plotsDomainHighTextfieldContainer.setEnabled(plotsDomainSpanCheckBox.isSelected());
                plotsDomainLowTextfieldContainer.setEnabled(plotsDomainSpanCheckBox.isSelected());

                if (plotsDomainSpanCheckBox.isSelected()) {
                    plotsThreshDomainSpanCheckBox.setSelected(false);
                }
            }
        });



        // "View" Tab Variables and Components

        showStatsListCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                showStatsList = showStatsListCheckBox.isSelected();
            }
        });

        showStatsSpreadSheetCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                showStatsSpreadSheet = showStatsSpreadSheetCheckBox.isSelected();
                // todo Danny this listener needs to be outside in the calling program
                //            updateLeftPanel();
            }
        });

        showPercentPlotCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                showPercentPlots = showPercentPlotCheckBox.isSelected();
            }
        });

        showHistogramPlotCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                showHistogramPlots = showHistogramPlotCheckBox.isSelected();
            }
        });

    }


    private void textfieldHandler(final JTextField textField) {

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                textfieldHandlerAction(textField);
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                textfieldHandlerAction(textField);
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                textfieldHandlerAction(textField);
            }
        });
    }

    private void textfieldHandlerAction(final JTextField textField) {

        if (handlersEnabled) {
            if (StatisticsTopComponent.PARAM_LABEL_PERCENT_THRESHOLDS.equals(textField.getName())) {
                percentThresholds = textField.getText().toString();
            }
        }

    }

    public void resetProduct() {
        binWidthTextfieldContainer.reset();
        binMinTextfieldContainer.reset();
        binMaxTextfieldContainer.reset();
        plotsDomainLowTextfieldContainer.reset();
        plotsDomainHighTextfieldContainer.reset();
    }


    //
    //------------------------------- FIELD RETRIEVALS -------------------------------------
    //

    // "Bins" Tab Variables and Components

    public int getNumBins() {
        return numBins;
    }

    public double getBinWidth() {
        return binWidth;
    }

    public double getBinMin() {
        return binMin;
    }

    public double getBinMax() {
        return binMax;
    }

    public boolean isLogMode() {
        return logMode;
    }


    // "Plots" Tab Variables and Components

    public boolean plotsThreshDomainSpan() {
        return plotsThreshDomainSpan;
    }

    public double plotsThreshDomainLow() {
        return plotsThreshDomainLow;
    }

    public double plotsThreshDomainHigh() {
        return plotsThreshDomainHigh;
    }

    public boolean plotsDomainSpan() {
        return plotsDomainSpan;
    }

    public double plotsDomainLow() {
        return plotsDomainLow;
    }

    public double plotsDomainHigh() {
        return plotsDomainHigh;
    }

    public boolean exactPlotSize() {
        return exactPlotSize;
    }

    public int plotSizeHeight() {
        return plotSizeHeight;
    }

    public int plotSizeWidth() {
        return plotSizeWidth;
    }





    // "Fields" Tab Variables and Components


    public boolean includeMedian() {
        return includeMedian;
    }

    public boolean includeMinMax() {
        return includeMinMax;
    }

    public boolean includeTotalPixels() {
        return includeTotalPixels;
    }

    public boolean includeHistogramStats() {
        return includeHistogramStats;
    }

    public List<Integer> getPercentThresholdsList() {
        List<Integer> percentThresholdsList = new ArrayList<Integer>();

        String[] thresholds = percentThresholds.split(",");


        for (String threshold : thresholds) {
            if (threshold != null) {
                threshold.trim();
                if (threshold.length() > 0) {
                    int value;
                    try {
                        value = Integer.parseInt(threshold);
                        if (value < 0 || value > 100) {
                            JOptionPane.showMessageDialog(getParentDialogContentPane,
                                    "ERROR: Valid " + StatisticsTopComponent.PARAM_LABEL_PERCENT_THRESHOLDS + " range is (0 to 100)",
                                    "Invalid Input",
                                    JOptionPane.ERROR_MESSAGE);
                            return null;

                        } else {
                            percentThresholdsList.add(value);
                        }
                    } catch (NumberFormatException exception) {
                        JOptionPane.showMessageDialog(getParentDialogContentPane,
                                StatisticsTopComponent.PARAM_LABEL_PERCENT_THRESHOLDS + "field " + exception.toString(),
                                "Invalid Input",
                                JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                }
            }
        }

        return percentThresholdsList;
    }



    public boolean isIncludeFileRefNo() {
        return includeFileRefNo;
    }

    public boolean isIncludeBandName() {
        return includeBandName;
    }


    public boolean isIncludeMaskName() {
        return includeMaskName;
    }





    public boolean isIncludeFileMetaData() {
        return includeFileMetaData;
    }

    public boolean isIncludeMaskMetaData() {
        return includeMaskMetaData;
    }


    public boolean isIncludeTimeSeriesMetaData() {
        return includeTimeSeriesMetaData;
    }


    public boolean isIncludeBandMetaData() {
        return includeBandMetaData;
    }

    public boolean isIncludeBinningInfo() {
        return includeBinningInfo;
    }

    public boolean isIncludeTimeMetaData() {
        return includeTimeMetaData;
    }

    public boolean isIncludeProjectionParameters() {
        return includeProjectionParameters;
    }

    public boolean isIncludeColBreaks() {
        return includeColBreaks;
    }


    // "Text" Tab Variables and Components

    public int colCharWidth() {
        return colCharWidth;
    }

    public int decimalPlaces() {
        return decimalPlaces;
    }



    // "View" Tab Variables and Components

    public boolean showPercentPlots() {
        return showPercentPlots;
    }

    public boolean showHistogramPlots() {
        return showHistogramPlots;
    }

    public boolean showStatsList() {
        return showStatsList;
    }

    public boolean showStatsSpreadSheet() {
        return showStatsSpreadSheet;
    }


    //
    //------------------------------- PUBLIC TABBED PANEL -------------------------------------
    //

    // "Bins" Tab Variables and Components


    public JPanel getCriteriaPanel() {

        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.insets.top = 10;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(getCriteriaFormattingTabbedPane(), gbc);


        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    public JTabbedPane getCriteriaFormattingTabbedPane() {

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Bins", getBinningCriteriaPanel());
        tabbedPane.setToolTipTextAt(0, "Histogram statistics binning criteria");

        tabbedPane.addTab("Fields", getFieldOptionsPanel());
        tabbedPane.setToolTipTextAt(1, "Statistic fields to display within text and spreadsheet");

        tabbedPane.addTab("Format", getTextOptionsPanel());
        tabbedPane.setToolTipTextAt(2, "Text and spreadsheet formatting");

        tabbedPane.addTab("Plots", getPlotsOptionsPanel());
        tabbedPane.setToolTipTextAt(3, "Plot formatting");

        tabbedPane.addTab("View", getViewPanel());
        tabbedPane.setToolTipTextAt(4, "View options");

        return  tabbedPane;

    }


    //
    //------------------------------- PRIVATE TABBED PANELS -------------------------------------
    //

    // "Bins" Tab Variables and Components

    private JPanel getBinningCriteriaPanel() {

        JPanel numBinsPanel = getNumBinsPanel();


        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.insets.top = 5;
        gbc.weighty = 0;
        panel.add(numBinsPanel, gbc);

        gbc.insets.top = 0;
        gbc.gridy += 1;

        panel.add(getBinWidthPanel(), gbc);


        gbc.gridy += 1;
        panel.add(getBinMinMaxPanel(), gbc);

        gbc.gridy += 1;
        panel.add(logModeCheckBox, gbc);

        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        return panel;
    }


    // "Fields" Tab Variables and Components


    private JPanel getFieldOptionsPanel() {

        JPanel thresholdsPanel = getThresholdsPanel();


        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.weighty = 0;
        gbc.insets.top = 5;
        panel.add(includeTotalPixelsCheckBox, gbc);

        gbc.insets.top = 0;
        gbc.gridy += 1;
        panel.add(includeMedianCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(includeMinMaxCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(thresholdsPanel, gbc);


        gbc.gridy += 1;
        panel.add(includeBinningInfoCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(includeHistogramStatsCheckBox, gbc);



        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets.top = 5;
        gbc.insets.right = 10;
        panel.add(new JSeparator(), gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.right = 0;


//        GridBagUtils.addToPanel(exportAndHelpPanel, new JSeparator(), helpPanelConstraints, "fill=HORIZONTAL,gridwidth=4,insets.left=5,insets.right=5");

        gbc.insets.top = 5;
        gbc.gridy += 1;
        panel.add(includeBandMetaDataCheckBox, gbc);

        gbc.insets.top = 0;
        gbc.gridy += 1;
        panel.add(includeFileMetaDataCheckBox, gbc);



        gbc.gridy += 1;
        panel.add(includeMaskMetaDataCheckBox, gbc);


        gbc.gridy += 1;
        panel.add(includeProjectionParametersCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(includeTimeMetaDataCheckBox, gbc);


        gbc.gridy += 1;
        panel.add(includeTimeSeriesMetaDataCheckBox, gbc);





        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        return panel;
    }


    // "Text" Tab Variables and Components

    private JPanel getTextOptionsPanel() {

        JPanel decimalPlacesPanel = getDecimalPlacesPanel();


        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.weighty = 0;
        gbc.insets.top = 5;
        panel.add(decimalPlacesPanel, gbc);

        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets.right = 10;
        gbc.insets.left = 10;
        panel.add(new JSeparator(), gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.right = 0;
        gbc.insets.left = 0;



        gbc.insets.top = 5;
        gbc.gridy += 1;
        panel.add(getColWidthPanel(), gbc);

        gbc.insets.top = 0;
        gbc.gridy += 1;
        panel.add(includeColBreaksCheckBox, gbc);


        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        return panel;
    }


    // "Plots" Tab Variables and Components


    private JPanel getPlotsOptionsPanel() {

        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.weighty = 0;
        gbc.insets.top = 5;
        panel.add(getPlotsThreshDomainSpanPanel(), gbc);

        gbc.insets.top = 0;
        gbc.gridy += 1;
        panel.add(getPlotsDomainSpanPanel(), gbc);

        // todo PlotSize
        gbc.gridy += 1;
        panel.add(getPlotsSizePanel(), gbc);


        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        return panel;
    }




    // "View" Tab Variables and Components

    private JPanel getViewPanel() {

        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();


        gbc.weighty = 0;
        gbc.insets.top = 5;
        panel.add(showHistogramPlotCheckBox, gbc);

        gbc.insets.top = 0;
        gbc.gridy += 1;
        panel.add(showPercentPlotCheckBox, gbc);


        gbc.gridy += 1;
        panel.add(showStatsListCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(showStatsSpreadSheetCheckBox, gbc);

        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        return panel;
    }


    //
    //------------------------------- PRIVATE CHILDREN PANELS -------------------------------------
    //


    // "Bins" Tab Variables and Components

    private JPanel getNumBinsPanel() {

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 3;
        panel.add(numBinsTextfieldContainer.getLabel(), gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        panel.add(numBinsTextfieldContainer.getTextfield(), gbc);

        return panel;
    }


    private JPanel getBinWidthPanel() {


        JPanel childPanel = GridBagUtils.createPanel();
        GridBagConstraints gbcChild = GridBagUtils.createConstraints();

        gbcChild.gridx += 1;
        gbcChild.insets.left = new JCheckBox(" ").getPreferredSize().width;
        gbcChild.insets.right = 3;
        childPanel.add(binWidthTextfieldContainer.getLabel(), gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        childPanel.add(binWidthTextfieldContainer.getTextfield(), gbcChild);


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        panel.add(binWidthEnabledCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(childPanel, gbc);


        return panel;
    }


    private JPanel getBinMinMaxPanel() {

        return getCheckboxTextFieldGroupPanel(binMinMaxCheckBox, binMinTextfieldContainer, binMaxTextfieldContainer);

    }


    // "Fields" Tab Variables and Components

    private JPanel getThresholdsPanel() {

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 3;
        panel.add(percentThresholdsLabel, gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        panel.add(percentThresholdsTextField, gbc);

        return panel;
    }



    // "Text" Tab Variables and Components


    private JPanel getColWidthPanel() {


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 3;
        panel.add(spreadsheetColWidthTextfieldContainer.getLabel(), gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        panel.add(spreadsheetColWidthTextfieldContainer.getTextfield(), gbc);

        return panel;
    }


    private JPanel getDecimalPlacesPanel() {

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 3;
        panel.add(decimalPlacesTextfieldContainer.getLabel(), gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        panel.add(decimalPlacesTextfieldContainer.getTextfield(), gbc);

        return panel;
    }

    // "Plots" Tab Variables and Components

    private JPanel getPlotsThreshDomainSpanPanel() {

        return getCheckboxTextFieldGroupPanel(plotsThreshDomainSpanCheckBox, plotsThreshDomainLowTextfieldContainer, plotsThreshDomainHighTextfieldContainer);

    }


    private JPanel getPlotsDomainSpanPanel() {

        return getCheckboxTextFieldGroupPanel(plotsDomainSpanCheckBox, plotsDomainLowTextfieldContainer, plotsDomainHighTextfieldContainer);

    }



    private JPanel getPlotsSizePanel() {

        return getCheckboxTextFieldGroupPanel(plotsSizeCheckBox, plotsSizeWidthTextfieldContainer, plotsSizeHeightTextfieldContainer);

    }


    //
    //------------------------------- VALIDATION -------------------------------------
    //


    public boolean validatePrepare() {
        if (numBinsTextfieldContainer != null && numBinsTextfieldContainer.isValid(true) && numBinsTextfieldContainer.getValue() != null) {
            numBins = numBinsTextfieldContainer.getValue().intValue();
            if (!validNumBins()) {
                return false;
            }
        } else {
            return false;
        }

        if (binWidthEnabledCheckBox.isSelected()) {
            if (binWidthTextfieldContainer != null && binWidthTextfieldContainer.isValid(true) && binWidthTextfieldContainer.getValue() != null) {
                binWidth = binWidthTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }

        } else {
            binWidth = Double.NaN;
        }


        if (binMinMaxCheckBox.isSelected()) {
            if (binMinTextfieldContainer != null && binMinTextfieldContainer.isValid(true) && binMinTextfieldContainer.getValue() != null) {
                binMin = binMinTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }


            if (binMaxTextfieldContainer != null && binMaxTextfieldContainer.isValid(true) && binMaxTextfieldContainer.getValue() != null) {
                binMax = binMaxTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }

            String lowName = binMinTextfieldContainer.getName();
            String highName = binMaxTextfieldContainer.getName();
            double lowVal = binMinTextfieldContainer.getValue().doubleValue();
            double highVal = binMaxTextfieldContainer.getValue().doubleValue();
            if (!compareFields(lowVal, highVal, lowName, highName, true)) {
                return false;
            }

        } else {
            binMin = Double.NaN;
            binMax = Double.NaN;
        }



        exactPlotSize = plotsSizeCheckBox.isSelected();
        plotsDomainSpan = plotsDomainSpanCheckBox.isSelected();
        plotsThreshDomainSpan = plotsThreshDomainSpanCheckBox.isSelected();

        if (plotsSizeCheckBox.isSelected()) {
            if (plotsSizeHeightTextfieldContainer != null && plotsSizeHeightTextfieldContainer.isValid(true) && plotsSizeHeightTextfieldContainer.getValue() != null) {
                plotSizeHeight = plotsSizeHeightTextfieldContainer.getValue().intValue();
            } else {
                return false;
            }

            if (plotsSizeWidthTextfieldContainer != null && plotsSizeWidthTextfieldContainer.isValid(true) && plotsSizeWidthTextfieldContainer.getValue() != null) {
                plotSizeWidth = plotsSizeWidthTextfieldContainer.getValue().intValue();
            } else {
                return false;
            }
        } else {
//            plotSizeHeight = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_SIZE_HEIGHT;
//            plotSizeWidth = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_SIZE_WIDTH;
        }


        if (plotsDomainSpanCheckBox.isSelected()) {
            if (plotsDomainLowTextfieldContainer != null && plotsDomainLowTextfieldContainer.isValid(true) && plotsDomainLowTextfieldContainer.getValue() != null) {
                plotsDomainLow = plotsDomainLowTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }


            if (plotsDomainHighTextfieldContainer != null && plotsDomainHighTextfieldContainer.isValid(true) && plotsDomainHighTextfieldContainer.getValue() != null) {
                plotsDomainHigh = plotsDomainHighTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }

            String lowName = plotsDomainLowTextfieldContainer.getName();
            String highName = plotsDomainHighTextfieldContainer.getName();
            double lowVal = plotsDomainLowTextfieldContainer.getValue().doubleValue();
            double highVal = plotsDomainHighTextfieldContainer.getValue().doubleValue();
            if (!compareFields(lowVal, highVal, lowName, highName, true)) {
                return false;
            }

        } else {
//            plotsDomainLow = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_DOMAIN_LOW;
//            plotsDomainHigh = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_DOMAIN_HIGH;
        }


        if (plotsThreshDomainSpanCheckBox.isSelected()) {
            if (plotsThreshDomainLowTextfieldContainer != null && plotsThreshDomainLowTextfieldContainer.isValid(true) && plotsThreshDomainLowTextfieldContainer.getValue() != null) {
                plotsThreshDomainLow = plotsThreshDomainLowTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }


            if (plotsThreshDomainHighTextfieldContainer != null && plotsThreshDomainHighTextfieldContainer.isValid(true) && plotsThreshDomainHighTextfieldContainer.getValue() != null) {
                plotsThreshDomainHigh = plotsThreshDomainHighTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }

            String lowName = plotsThreshDomainLowTextfieldContainer.getName();
            String highName = plotsThreshDomainHighTextfieldContainer.getName();
            double lowVal = plotsThreshDomainLowTextfieldContainer.getValue().doubleValue();
            double highVal = plotsThreshDomainHighTextfieldContainer.getValue().doubleValue();
            if (!compareFields(lowVal, highVal, lowName, highName, true)) {
                return false;
            }

        } else {
//            plotsThreshDomainLow = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_LOW;
//            plotsThreshDomainHigh = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_HIGH;
        }




        if (spreadsheetColWidthTextfieldContainer != null && spreadsheetColWidthTextfieldContainer.isValid(true) && spreadsheetColWidthTextfieldContainer.getValue() != null) {
            colCharWidth = spreadsheetColWidthTextfieldContainer.getValue().intValue();
        } else {
            return false;
        }

        if (decimalPlacesTextfieldContainer != null && decimalPlacesTextfieldContainer.isValid(true) && decimalPlacesTextfieldContainer.getValue() != null) {
            decimalPlaces = decimalPlacesTextfieldContainer.getValue().intValue();
        } else {
            return false;
        }


        return true;



    }

    private boolean validNumBins() {
        if (numBins < StatisticsTopComponent.PARAM_MINVAL_NUM_BINS || numBins > StatisticsTopComponent.PARAM_MAXVAL_NUM_BINS) {
            return false;
        }

        return true;
    }



    //
    //------------------------------- PREFERENCES -------------------------------------
    //





    public int getPreferencesDecimalPlaces() {

        return preferences.getInt(StatisticsTopComponent.PARAM_KEY_SPREADSHEET_DECIMAL_PLACES, StatisticsTopComponent.PARAM_DEFVAL_SPREADSHEET_DECIMAL_PLACES);
    }

    public int getPreferencesColWidth() {

        return preferences.getInt(StatisticsTopComponent.PARAM_KEY_SPREADSHEET_COL_WIDTH, StatisticsTopComponent.PARAM_DEFVAL_SPREADSHEET_COL_WIDTH);
    }

    public double getPreferencesPlotDomainThreshLow() {

        return preferences.getDouble(StatisticsTopComponent.PARAM_KEY_PLOTS_THRESH_DOMAIN_LOW, StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_LOW);

    }

    public double getPreferencesPlotDomainThreshHigh() {

        return preferences.getDouble(StatisticsTopComponent.PARAM_KEY_PLOTS_THRESH_DOMAIN_HIGH, StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_HIGH);

    }


    public int getPreferencesNumBins() {

        return preferences.getInt(StatisticsTopComponent.PARAM_KEY_NUM_BINS, StatisticsTopComponent.PARAM_DEFVAL_NUM_BINS);

    }

    public boolean getPreferencesHistogramPlotEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_HISTOGRAM_PLOT_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_HISTOGRAM_PLOT_ENABLED);

    }

    public boolean getPreferencesPercentPlotEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_PERCENT_PLOT_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_PERCENT_PLOT_ENABLED);

    }

    public boolean getPreferencesStatsListEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_STATS_LIST_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_STATS_LIST_ENABLED);
    }

    public boolean getPreferencesStatsSpreadSheetEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_STATS_SPREADSHEET_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_STATS_SPREADSHEET_ENABLED);
    }

    public String getPreferencesPercentThresholds() {

        return preferences.get(StatisticsTopComponent.PARAM_KEY_PERCENT_THRESHOLDS, StatisticsTopComponent.PARAM_DEFVAL_PERCENT_THRESHOLDS);
    }

    public boolean getPreferencesFileMetaDataEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_FILE_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_FILE_METADATA_ENABLED);
    }

    public boolean getPreferencesMedianEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_MEDIAN_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_MEDIAN_ENABLED);
    }

    public boolean getPreferencesMinMaxEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_MINMAX_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_MINMAX_ENABLED);
    }

    public boolean getPreferencesTotalPixelsEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_TOTAL_PIXEL_COUNT_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_TOTAL_PIXEL_COUNT_ENABLED);
    }

    public boolean getPreferencesHistogramStatsEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_HISTOGRAM_STATS_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_HISTOGRAM_STATS_ENABLED);
    }

    public boolean getPreferencesMaskMetaDataEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_MASK_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_MASK_METADATA_ENABLED);
    }


    public boolean getPreferencesBandMetaDataEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_BAND_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_BAND_METADATA_ENABLED);
    }

    public boolean getPreferencesBinningInfoEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_BINNING_INFO_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_BINNING_INFO_ENABLED);
    }

    public boolean getPreferencesProjectionParametersEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_PROJECTION_PARAMETERS_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_PROJECTION_PARAMETERS_METADATA_ENABLED);
    }


    public boolean getPreferencesColumnBreaksEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_COL_BREAKS_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_COL_BREAKS_ENABLED);
    }



    public boolean getPreferencesTimeSeriesMetaDataEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_TIME_SERIES_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_TIME_SERIES_METADATA_ENABLED);
    }

    public boolean getPreferencesFileTimeMetaDataEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PARAM_KEY_TIME_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_TIME_METADATA_ENABLED);
    }


    //
    //------------------------------- GENERAL PURPOSE -------------------------------------
    //


    private boolean compareFields(double lowVal, double highVal, String lowName, String HighName, boolean showDialog) {
        if (lowVal >= highVal) {
            if (showDialog) {
                JOptionPane.showMessageDialog(getParentDialogContentPane,
                        "ERROR: Value of " + lowName + " must be greater than value of " + HighName,
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            }

            return false;
        }

        return true;

    }

    public JPanel getCheckboxTextFieldGroupPanel(JCheckBox checkbox, TextFieldContainer textFieldContainer) {

        return getCheckboxTextFieldGroupPanel(checkbox, textFieldContainer, null);
    }

    public JPanel getCheckboxTextFieldGroupPanel(JCheckBox checkbox, TextFieldContainer textFieldContainer1, TextFieldContainer textFieldContainer2) {


        JPanel childPanel = GridBagUtils.createPanel();
        GridBagConstraints gbcChild = GridBagUtils.createConstraints();
        gbcChild.insets.left = new JCheckBox(" ").getPreferredSize().width;

        gbcChild.insets.right = 3;
        childPanel.add(textFieldContainer1.getLabel(), gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        childPanel.add(textFieldContainer1.getTextfield(), gbcChild);

        if (textFieldContainer2 != null) {
            gbcChild.gridx += 1;
            gbcChild.insets.right = 3;
            childPanel.add(textFieldContainer2.getLabel(), gbcChild);
            gbcChild = GridBagUtils.restoreConstraints(gbcChild);

            gbcChild.gridx += 1;
            childPanel.add(textFieldContainer2.getTextfield(), gbcChild);
        }

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        panel.add(checkbox, gbc);

        gbc.gridy += 1;
        panel.add(childPanel, gbc);


        return panel;
    }
}
