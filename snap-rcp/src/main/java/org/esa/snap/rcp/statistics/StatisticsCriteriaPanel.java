package org.esa.snap.rcp.statistics;

import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.TextFieldContainer;
import org.openide.awt.ColorComboBox;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    private int numBins = StatisticsTopComponent.PROPERTY_TOTAL_BINS_DEFAULT;
    private TextFieldContainer numBinsTextfieldContainer = null;

    private double binWidth = StatisticsTopComponent.PROPERTY_BIN_WIDTH_DEFAULT;
    private TextFieldContainer binWidthTextfieldContainer = null;

    private JCheckBox binWidthEnabledCheckBox = null;
    private JCheckBox logModeCheckBox = null;

    private boolean logMode;


    private JCheckBox binMinMaxCheckBox = null;
    double binMin = StatisticsTopComponent.PROPERTY_BIN_MIN_DEFAULT;
    double binMax = StatisticsTopComponent.PROPERTY_BIN_MAX_DEFAULT;
    private TextFieldContainer binMinTextfieldContainer = null;
    private TextFieldContainer binMaxTextfieldContainer = null;


    // "Fields" Tab Variables and Components

    private boolean handlersEnabled = true; //todo this may be temporary and not needed
    private String percentThresholds = StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_DEFAULT;
    private java.util.List<Integer> percentThresholdsList = null;
    private JTextField percentThresholdsTextField = null;
    private JLabel percentThresholdsLabel = null;

    private boolean includeTotalPixels = StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_DEFAULT;
    private JCheckBox includeTotalPixelsCheckBox = null;

    private boolean includeMedian = StatisticsTopComponent.PROPERTY_MEDIAN_DEFAULT;
    private JCheckBox includeMedianCheckBox = null;

    private boolean includeMinMax = StatisticsTopComponent.PROPERTY_MINMAX_DEFAULT;
    private JCheckBox includeMinMaxCheckBox = null;

    private boolean includeHistogramStats = StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_DEFAULT;
    private JCheckBox includeHistogramStatsCheckBox = null;

    private boolean includeFileRefNo = true;
    private boolean includeBandName = true;
    private boolean includeMaskName = true;

    private boolean includeFileMetaData = StatisticsTopComponent.PROPERTY_FILE_METADATA_DEFAULT;
    private JCheckBox includeFileMetaDataCheckBox = null;

    private boolean includeMaskMetaData = StatisticsTopComponent.PROPERTY_MASK_METADATA_DEFAULT;
    private JCheckBox includeMaskMetaDataCheckBox = null;


    private boolean includeBandMetaData = StatisticsTopComponent.PROPERTY_BAND_METADATA_DEFAULT;
    private JCheckBox includeBandMetaDataCheckBox = null;

    private boolean includeBinningInfo = StatisticsTopComponent.PROPERTY_BINNING_INFO_DEFAULT;
    private JCheckBox includeBinningInfoCheckBox = null;

    private boolean includeTimeSeriesMetaData = StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_DEFAULT;
    private JCheckBox includeTimeSeriesMetaDataCheckBox = null;

    private boolean includeTimeMetaData = StatisticsTopComponent.PROPERTY_TIME_METADATA_DEFAULT;
    private JCheckBox includeTimeMetaDataCheckBox = null;

    private boolean includeProjectionParameters = StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_DEFAULT;;
    private JCheckBox includeProjectionParametersCheckBox = null;




    // "Text" Tab Variables and Components

    private static final int COL_WIDTH_DEFAULT = StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_DEFAULT;
    private int colCharWidth = COL_WIDTH_DEFAULT;
    private TextFieldContainer spreadsheetColWidthTextfieldContainer = null;

    private int decimalPlaces = StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_DEFAULT;
    private TextFieldContainer decimalPlacesTextfieldContainer = null;

    private boolean includeColBreaks = StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_DEFAULT;
    private JCheckBox includeColBreaksCheckBox = null;



    // "Plots" Tab Variables and Components

    private boolean plotsThreshDomainSpan = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_DEFAULT;
    private double plotsThreshDomainLow = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_DEFAULT;
    private double plotsThreshDomainHigh = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_DEFAULT;

    private JCheckBox plotsThreshDomainSpanCheckBox = null;
    private TextFieldContainer plotsThreshDomainLowTextfieldContainer = null;
    private TextFieldContainer plotsThreshDomainHighTextfieldContainer = null;

    private boolean plotsDomainSpan = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_DEFAULT;
    private double plotsDomainLow = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_LOW_DEFAULT;
    private double plotsDomainHigh = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_HIGH_DEFAULT;

    private JCheckBox plotsDomainSpanCheckBox = null;
    private TextFieldContainer plotsDomainLowTextfieldContainer = null;
    private TextFieldContainer plotsDomainHighTextfieldContainer = null;

    private boolean exactPlotSize = StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_DEFAULT;
    private int plotSizeHeight = StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_DEFAULT;
    private int plotSizeWidth = StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_DEFAULT;

    private JCheckBox plotsSizeCheckBox = null;
    private TextFieldContainer plotsSizeHeightTextfieldContainer = null;
    private TextFieldContainer plotsSizeWidthTextfieldContainer = null;

    private Color plotColor = StatisticsTopComponent.PROPERTY_PLOTS_COLOR_DEFAULT;
    private Color plotBackgroundColor = StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_DEFAULT;
    private Color plotLabelColor = StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_DEFAULT;

    private JLabel plotsColorLabel = null;
    private JLabel plotsBackgroundColorLabel = null;
    private JLabel plotsLabelColorLabel = null;
    private ColorComboBox plotsColorComboBox = null;
    private ColorComboBox plotsBackgroundColorComboBox = null;
    private ColorComboBox plotsLabelColorComboBox = null;


    // "View" Tab Variables and Components

    private boolean showPercentPlots = StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_DEFAULT;
    private boolean showHistogramPlots = StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_DEFAULT;
    private boolean showStatsList = StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_DEFAULT;
    private boolean showStatsSpreadSheet = StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_DEFAULT;

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

        // toggle each checkbox to force event change in listeners and hence establish all proper initial enablement
        plotsSizeCheckBox.setSelected(!plotsSizeCheckBox.isSelected());
        plotsSizeCheckBox.setSelected(!plotsSizeCheckBox.isSelected());

        // These 2 fields are codependent
        boolean initial_plotsThreshDomainSpanCheckBox = plotsThreshDomainSpanCheckBox.isSelected();
        boolean initial_plotsDomainSpanCheckBox = plotsDomainSpanCheckBox.isSelected();
        plotsDomainSpanCheckBox.setSelected(!initial_plotsDomainSpanCheckBox);
        plotsThreshDomainSpanCheckBox.setSelected(!initial_plotsThreshDomainSpanCheckBox);
        plotsDomainSpanCheckBox.setSelected(initial_plotsDomainSpanCheckBox);
        plotsThreshDomainSpanCheckBox.setSelected(initial_plotsThreshDomainSpanCheckBox);


        binWidthEnabledCheckBox.setSelected(!binWidthEnabledCheckBox.isSelected());
        binWidthEnabledCheckBox.setSelected(!binWidthEnabledCheckBox.isSelected());
        binMinMaxCheckBox.setSelected(!binMinMaxCheckBox.isSelected());
        binMinMaxCheckBox.setSelected(!binMinMaxCheckBox.isSelected());
    }


    public void initPreferencesAndDefaults() {


        numBins = getPreferencesNumBins();

        binWidth = StatisticsTopComponent.PROPERTY_BIN_WIDTH_DEFAULT;
        binMin = StatisticsTopComponent.PROPERTY_BIN_MIN_DEFAULT;
        binMax = StatisticsTopComponent.PROPERTY_BIN_MAX_DEFAULT;

        logMode = getPreferencesLogScaledBins();


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
        plotsThreshDomainSpan = getPreferencesPlotDomainThresh();
        plotsThreshDomainLow = getPreferencesPlotDomainThreshLow();
        plotsThreshDomainHigh = getPreferencesPlotDomainThreshHigh();

        plotsDomainSpan = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_DEFAULT;
        plotsDomainLow = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_LOW_DEFAULT;
        plotsDomainHigh = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_HIGH_DEFAULT;


        exactPlotSize = getPreferencesSetPlotSize();
        plotSizeHeight = getPreferencesPlotSizeHeight();
        plotSizeWidth = getPreferencesPlotSizeWidth();

//        plotColor = StatisticsTopComponent.PROPERTY_PLOTS_COLOR_DEFAULT;
//        plotBackgroundColor = StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_DEFAULT;

        plotColor = getPreferencesPlotColor();
        plotBackgroundColor = getPreferencesPlotBackgroundColor();
        plotLabelColor = getPreferencesPlotLabelColor();

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
        binWidthEnabledCheckBox.setSelected(StatisticsTopComponent.PROPERTY_USE_BIN_WIDTH_DEFAULT);
        binMinMaxCheckBox.setSelected(StatisticsTopComponent.PROPERTY_BIN_SPAN_DEFAULT);
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

        plotsColorComboBox.setSelectedColor(plotColor);
        plotsBackgroundColorComboBox.setSelectedColor(plotBackgroundColor);
        plotsLabelColorComboBox.setSelectedColor(plotLabelColor);


        // View
        showStatsListCheckBox.setSelected(showStatsList);
        showStatsSpreadSheetCheckBox.setSelected(showStatsSpreadSheet);
        showPercentPlotCheckBox.setSelected(showPercentPlots);
        showHistogramPlotCheckBox.setSelected(showHistogramPlots);

    }

    private void createComponents() {

        // "Bins" Tab Variables and Components

        numBinsTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_TOTAL_BINS_LABEL,
                numBins,
                StatisticsTopComponent.PROPERTY_TOTAL_BINS_MIN,
                StatisticsTopComponent.PROPERTY_TOTAL_BINS_MAX,
                TextFieldContainer.NumType.INT,
                7,
                getParentDialogContentPane);
        numBinsTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_TOTAL_BINS_TOOLTIP);


        binWidthEnabledCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_USE_BIN_WIDTH_LABEL);
        binWidthEnabledCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_USE_BIN_WIDTH_TOOLTIP);

        binWidthTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_BIN_WIDTH_LABEL,
                StatisticsTopComponent.PROPERTY_BIN_WIDTH_DEFAULT,
                TextFieldContainer.NumType.DOUBLE,
                5,
                getParentDialogContentPane);
        binWidthTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_BIN_WIDTH_TOOLTIP);


        binMinMaxCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_BIN_SPAN_LABEL);
        binMinMaxCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_BIN_SPAN_TOOLTIP);

        binMinTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_BIN_MIN_LABEL_SHORT,
                StatisticsTopComponent.PROPERTY_BIN_MIN_DEFAULT,
                TextFieldContainer.NumType.DOUBLE,
                5,
                getParentDialogContentPane);
        binMinTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_BIN_MIN_TOOLTIP);


        binMaxTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_BIN_MAX_LABEL_SHORT,
                StatisticsTopComponent.PROPERTY_BIN_MAX_DEFAULT,
                TextFieldContainer.NumType.DOUBLE,
                5,
                getParentDialogContentPane);
        binMaxTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_BIN_MAX_TOOLTIP);

        logModeCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_LOG_SCALED_BINS_LABEL);
        logModeCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_LOG_SCALED_BINS_TOOLTIP);


        // "Fields" Tab Variables and Components

        includeTotalPixelsCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_LABEL);
        includeTotalPixelsCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_TOOLTIP);

        includeMedianCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_MEDIAN_LABEL);
        includeMedianCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_MEDIAN_TOOLTIP);

        includeMinMaxCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_MINMAX_LABEL);
        includeMinMaxCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_MINMAX_TOOLTIP);

        percentThresholdsLabel = new JLabel(StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_LABEL);
        percentThresholdsLabel.setToolTipText(StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_TOOLTIP);
        percentThresholdsTextField = new JTextField(14);
        percentThresholdsTextField.setMinimumSize(percentThresholdsTextField.getPreferredSize());
        percentThresholdsTextField.setName(StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_LABEL);
        percentThresholdsTextField.setToolTipText(StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_TOOLTIP);

        includeBinningInfoCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_BINNING_INFO_LABEL);
        includeBinningInfoCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_BINNING_INFO_TOOLTIP);

        includeHistogramStatsCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_LABEL);
        includeHistogramStatsCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_TOOLTIP);

        includeBandMetaDataCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_BAND_METADATA_LABEL);
        includeBandMetaDataCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_BAND_METADATA_TOOLTIP);

        includeFileMetaDataCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_FILE_METADATA_LABEL);
        includeFileMetaDataCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_FILE_METADATA_TOOLTIP);

        includeMaskMetaDataCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_MASK_METADATA_LABEL);
        includeMaskMetaDataCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_MASK_METADATA_TOOLTIP);

        includeProjectionParametersCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_LABEL);
        includeProjectionParametersCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_TOOLTIP);

        includeTimeMetaDataCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_TIME_METADATA_LABEL);
        includeTimeMetaDataCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_TIME_METADATA_TOOLTIP);

        includeTimeSeriesMetaDataCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_LABEL);
        includeTimeSeriesMetaDataCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_TOOLTIP);







        // "FORMAT" Tab Variables and Components

        decimalPlacesTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_LABEL,
                StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_DEFAULT,
                StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_MIN,
                StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_MAX,
                TextFieldContainer.NumType.INT,
                2,
                getParentDialogContentPane);
        decimalPlacesTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_TOOLTIP);

        spreadsheetColWidthTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_LABEL,
                StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_DEFAULT,
                StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_MIN,
                StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_MAX,
                TextFieldContainer.NumType.INT,
                2,
                getParentDialogContentPane);
        spreadsheetColWidthTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_TOOLTIP);

        includeColBreaksCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_COL_BREAKS_LABEL);
        includeColBreaksCheckBox.setSelected(includeColBreaks);
        includeColBreaksCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_COL_BREAKS_TOOLTIP);





        // "Plots" Tab Variables and Components
        plotsThreshDomainSpanCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LABEL);
        plotsThreshDomainSpanCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_TOOLTIP);

        plotsThreshDomainLowTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_LABEL_SHORT,
                plotsThreshDomainLow,
                StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_MIN,
                StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_MAX,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane);
        plotsThreshDomainLowTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_TOOLTIP);

        plotsThreshDomainHighTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_LABEL_SHORT,
                plotsThreshDomainHigh,
                StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_MIN,
                StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_MAX,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane);
        plotsThreshDomainHighTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_TOOLTIP);


        plotsDomainSpanCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_LABEL);
        plotsDomainSpanCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_TOOLTIP);

        plotsDomainLowTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_LOW_LABEL,
                plotsDomainLow,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane);
        plotsDomainLowTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_LOW_TOOLTIP);

        plotsDomainHighTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_HIGH_LABEL,
                plotsDomainHigh,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane);
        plotsDomainHighTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_VALUE_HIGH_TOOLTIP);


        plotsSizeCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_LABEL);
        plotsSizeCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_TOOLTIP);

        plotsSizeHeightTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_LABEL_SHORT,
                plotSizeHeight,
                StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_MIN,
                StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_MAX,
                TextFieldContainer.NumType.INT,
                4,
                getParentDialogContentPane);
        plotsSizeHeightTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_TOOLTIP);

        plotsSizeWidthTextfieldContainer = new TextFieldContainer(StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_LABEL_SHORT,
                plotSizeWidth,
                StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_MIN,
                StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_MAX,
                TextFieldContainer.NumType.INT,
                4,
                getParentDialogContentPane);
        plotsSizeWidthTextfieldContainer.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_TOOLTIP);

        plotsColorLabel = new JLabel(StatisticsTopComponent.PROPERTY_PLOTS_COLOR_LABEL);
        plotsColorLabel.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_COLOR_TOOLTIP);

        plotsColorComboBox = new ColorComboBox();
        plotsColorComboBox.setSelectedColor(StatisticsTopComponent.PROPERTY_PLOTS_COLOR_DEFAULT);
        plotsColorComboBox.setPreferredSize(plotsColorComboBox.getPreferredSize());
        plotsColorComboBox.setMinimumSize(plotsColorComboBox.getPreferredSize());

        plotsBackgroundColorLabel = new JLabel(StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_LABEL);
        plotsBackgroundColorLabel.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_TOOLTIP);

        plotsBackgroundColorComboBox = new ColorComboBox();
        plotsBackgroundColorComboBox.setSelectedColor(StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_DEFAULT);
        plotsBackgroundColorComboBox.setPreferredSize(plotsBackgroundColorComboBox.getPreferredSize());
        plotsBackgroundColorComboBox.setMinimumSize(plotsBackgroundColorComboBox.getPreferredSize());

        plotsLabelColorLabel = new JLabel(StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_LABEL);
        plotsLabelColorLabel.setToolTipText(StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_TOOLTIP);

        plotsLabelColorComboBox = new ColorComboBox();
        plotsLabelColorComboBox.setSelectedColor(StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_DEFAULT);
        plotsLabelColorComboBox.setPreferredSize(plotsLabelColorComboBox.getPreferredSize());
        plotsLabelColorComboBox.setMinimumSize(plotsLabelColorComboBox.getPreferredSize());

        // "View" Tab Variables and Components

        showHistogramPlotCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_LABEL);
        showHistogramPlotCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_TOOLTIP);

        showPercentPlotCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_LABEL);
        showPercentPlotCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_TOOLTIP);

        showStatsListCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_LABEL);
        showStatsListCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_TOOLTIP);

        showStatsSpreadSheetCheckBox = new JCheckBox(StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_LABEL);
        showStatsSpreadSheetCheckBox.setToolTipText(StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_TOOLTIP);





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


        plotsColorComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                plotColor = plotsColorComboBox.getSelectedColor();
            }
        });

        plotsBackgroundColorComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                plotBackgroundColor = plotsBackgroundColorComboBox.getSelectedColor();
            }
        });

        plotsLabelColorComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                plotLabelColor = plotsLabelColorComboBox.getSelectedColor();
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
            if (StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_LABEL.equals(textField.getName())) {
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

    public Color plotColor() { return plotColor; }

    public Color plotBackgroundColor() { return plotBackgroundColor; }

    public Color plotLabelColor() { return plotLabelColor; }

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
                                    "ERROR: Valid " + StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_LABEL + " range is (0 to 100)",
                                    "Invalid Input",
                                    JOptionPane.ERROR_MESSAGE);
                            return null;

                        } else {
                            percentThresholdsList.add(value);
                        }
                    } catch (NumberFormatException exception) {
                        JOptionPane.showMessageDialog(getParentDialogContentPane,
                                StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_LABEL + "field " + exception.toString(),
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

        int width = (int) (tabbedPane.getPreferredSize().width * 1.2);
        Dimension preferredSize = new Dimension(width, tabbedPane.getPreferredSize().height);
        tabbedPane.setPreferredSize(preferredSize);
        tabbedPane.setMinimumSize(preferredSize);

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


        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets.right = 10;
        gbc.insets.left = 10;
        panel.add(new JSeparator(), gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.right = 0;
        gbc.insets.left = 0;


        gbc.gridy += 1;
        panel.add(getPlotsSizePanel(), gbc);


        gbc.gridy += 1;
        panel.add(getPlotsColorPanel(), gbc);

        gbc.gridy += 1;
        panel.add(getPlotsBackgroundColorPanel(), gbc);

        gbc.gridy += 1;
        panel.add(getPlotsLabelColorPanel(), gbc);

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

    private JPanel getPlotsColorPanel() {

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbcChild = GridBagUtils.createConstraints();

        gbcChild.insets.right = 3;
        panel.add(plotsColorLabel, gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        panel.add(plotsColorComboBox, gbcChild);

        return panel;

    }

    private JPanel getPlotsBackgroundColorPanel() {

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbcChild = GridBagUtils.createConstraints();

        gbcChild.insets.right = 3;
        panel.add(plotsBackgroundColorLabel, gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        panel.add(plotsBackgroundColorComboBox, gbcChild);

        return panel;

    }

    private JPanel getPlotsLabelColorPanel() {

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbcChild = GridBagUtils.createConstraints();

        gbcChild.insets.right = 3;
        panel.add(plotsLabelColorLabel, gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        panel.add(plotsLabelColorComboBox, gbcChild);

        return panel;

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

        plotColor = plotsColorComboBox.getSelectedColor();
        plotBackgroundColor = plotsBackgroundColorComboBox.getSelectedColor();
        plotLabelColor = plotsLabelColorComboBox.getSelectedColor();

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
        if (numBins < StatisticsTopComponent.PROPERTY_TOTAL_BINS_MIN || numBins > StatisticsTopComponent.PROPERTY_TOTAL_BINS_MAX) {
            return false;
        }

        return true;
    }



    //
    //------------------------------- PREFERENCES -------------------------------------
    //





    public int getPreferencesDecimalPlaces() {

        return preferences.getInt(StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_KEY, StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_DEFAULT);
    }

    public int getPreferencesColWidth() {
        return preferences.getInt(StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_KEY, StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_DEFAULT);
    }

    public boolean getPreferencesPlotDomainThresh() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_KEY, StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_DEFAULT);
    }

    public double getPreferencesPlotDomainThreshLow() {
        return preferences.getDouble(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_KEY, StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_DEFAULT);
    }

    public double getPreferencesPlotDomainThreshHigh() {
        return preferences.getDouble(StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_KEY, StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_DEFAULT);
    }


    public boolean getPreferencesSetPlotSize() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_KEY, StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_DEFAULT);
    }

    public int getPreferencesPlotSizeWidth() {
        return preferences.getInt(StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_KEY, StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_DEFAULT);
    }

    public int getPreferencesPlotSizeHeight() {
        return preferences.getInt(StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_KEY, StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_DEFAULT);
    }

    public Color getPreferencesPlotColor() {
        return StringUtils.parseColor(preferences.get(StatisticsTopComponent.PROPERTY_PLOTS_COLOR_KEY, StringUtils.formatColor(StatisticsTopComponent.PROPERTY_PLOTS_COLOR_DEFAULT)));
    }

    public Color getPreferencesPlotBackgroundColor() {
        return StringUtils.parseColor(preferences.get(StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_KEY, StringUtils.formatColor(StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_DEFAULT)));
    }

    public Color getPreferencesPlotLabelColor() {
        return StringUtils.parseColor(preferences.get(StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_KEY, StringUtils.formatColor(StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_DEFAULT)));
    }

    public int getPreferencesNumBins() {
        return preferences.getInt(StatisticsTopComponent.PROPERTY_TOTAL_BINS_KEY, StatisticsTopComponent.PROPERTY_TOTAL_BINS_DEFAULT);
    }

    public boolean getPreferencesLogScaledBins() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_LOG_SCALED_BINS_KEY, StatisticsTopComponent.PROPERTY_LOG_SCALED_BINS_DEFAULT);
    }


    public boolean getPreferencesHistogramPlotEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_KEY, StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_DEFAULT);
    }

    public boolean getPreferencesPercentPlotEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_KEY, StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_DEFAULT);
    }

    public boolean getPreferencesStatsListEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_KEY, StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_DEFAULT);
    }

    public boolean getPreferencesStatsSpreadSheetEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_KEY, StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_DEFAULT);
    }

    public String getPreferencesPercentThresholds() {
        return preferences.get(StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_KEY, StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_DEFAULT);
    }

    public boolean getPreferencesFileMetaDataEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_FILE_METADATA_KEY, StatisticsTopComponent.PROPERTY_FILE_METADATA_DEFAULT);
    }

    public boolean getPreferencesMedianEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_MEDIAN_KEY, StatisticsTopComponent.PROPERTY_MEDIAN_DEFAULT);
    }

    public boolean getPreferencesMinMaxEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_MINMAX_KEY, StatisticsTopComponent.PROPERTY_MINMAX_DEFAULT);
    }

    public boolean getPreferencesTotalPixelsEnabled() {

        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_KEY, StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_DEFAULT);
    }

    public boolean getPreferencesHistogramStatsEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_KEY, StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_DEFAULT);
    }

    public boolean getPreferencesMaskMetaDataEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_MASK_METADATA_KEY, StatisticsTopComponent.PROPERTY_MASK_METADATA_DEFAULT);
    }


    public boolean getPreferencesBandMetaDataEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_BAND_METADATA_KEY, StatisticsTopComponent.PROPERTY_BAND_METADATA_DEFAULT);
    }

    public boolean getPreferencesBinningInfoEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_BINNING_INFO_KEY, StatisticsTopComponent.PROPERTY_BINNING_INFO_DEFAULT);
    }

    public boolean getPreferencesProjectionParametersEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_KEY, StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_DEFAULT);
    }


    public boolean getPreferencesColumnBreaksEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_COL_BREAKS_KEY, StatisticsTopComponent.PROPERTY_COL_BREAKS_DEFAULT);
    }



    public boolean getPreferencesTimeSeriesMetaDataEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_KEY, StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_DEFAULT);
    }

    public boolean getPreferencesFileTimeMetaDataEnabled() {
        return preferences.getBoolean(StatisticsTopComponent.PROPERTY_TIME_METADATA_KEY, StatisticsTopComponent.PROPERTY_TIME_METADATA_DEFAULT);
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
