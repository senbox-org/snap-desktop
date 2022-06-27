package org.esa.snap.rcp.statistics;

import org.esa.snap.core.util.NamingConvention;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

@TopComponent.Description(
        preferredID = "StatisticsTopComponent",
        iconBase = "org/esa/snap/rcp/icons/Statistics.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = "Statistics",
        openAtStartup = false,
        position = 40
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.StatisticsTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Analysis",position = 60),
        @ActionReference(path = "Toolbars/Analysis")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_StatisticsTopComponent_Name",
        preferredID = "StatisticsTopComponent"
)
@NbBundle.Messages({
        "CTL_StatisticsTopComponent_Name=Statistics",
        "CTL_StatisticsTopComponent_HelpId=statisticsDialog"
})
/**
 * @author Tonio Fincke
 */
public class StatisticsTopComponent extends AbstractStatisticsTopComponent {

    public static final String ID = StatisticsTopComponent.class.getName();

    private static final String PROPERTY_ROOT_KEY = "statistics";
    private static final String PROPERTY_BINS_DEFAULT_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".bins";

    public static final String PROPERTY_BINS_DEFAULT_SECTION_KEY = PROPERTY_BINS_DEFAULT_KEY_SUFFIX + ".section";
    public static final String PROPERTY_BINS_DEFAULT_SECTION_LABEL = "Bins";
    public static final String PROPERTY_BINS_DEFAULT_SECTION_TOOLTIP = "Total Bins";

    private static final String PROPERTY_FIELDS_DEFAULT_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".fields";
    public static final String PROPERTY_FIELDS_DEFAULT_SECTION_KEY = PROPERTY_FIELDS_DEFAULT_KEY_SUFFIX + ".section";
    public static final String PROPERTY_FIELDS_DEFAULT_SECTION_LABEL = "Fields";
    public static final String PROPERTY_FIELDS_DEFAULT_SECTION_TOOLTIP = "Fields";

    private static final String PROPERTY_FORMAT_DEFAULT_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".format";
    public static final String PROPERTY_FORMAT_DEFAULT_SECTION_KEY = PROPERTY_FORMAT_DEFAULT_KEY_SUFFIX + ".section";
    public static final String PROPERTY_FORMAT_DEFAULT_SECTION_LABEL = "Format";
    public static final String PROPERTY_FORMAT_DEFAULT_SECTION_TOOLTIP = "Format";

    private static final String PROPERTY_PLOTS_DEFAULT_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".plots";
    public static final String PROPERTY_PLOTS_DEFAULT_SECTION_KEY = PROPERTY_PLOTS_DEFAULT_KEY_SUFFIX + ".section";
    public static final String PROPERTY_PLOTS_DEFAULT_SECTION_LABEL = "Plots";
    public static final String PROPERTY_PLOTS_DEFAULT_SECTION_TOOLTIP = "Plots";

    private static final String PROPERTY_VIEW_DEFAULT_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".view";
    public static final String PROPERTY_VIEW_DEFAULT_SECTION_KEY = PROPERTY_VIEW_DEFAULT_KEY_SUFFIX + ".section";
    public static final String PROPERTY_VIEW_DEFAULT_SECTION_LABEL = "View";
    public static final String PROPERTY_VIEW_DEFAULT_SECTION_TOOLTIP = "View";

    public static final String PARAM_LABEL_HISTOGRAM_PLOT_ENABLED = "Show Histogram Plot";
    public static final String PARAM_KEY_HISTOGRAM_PLOT_ENABLED = "statistics.histogramPlot.enabled";
    public static final boolean PARAM_DEFVAL_HISTOGRAM_PLOT_ENABLED = true;

    public static final String PARAM_LABEL_PERCENT_PLOT_ENABLED = "Show Percentile Plot";
    public static final String PARAM_KEY_PERCENT_PLOT_ENABLED = "statistics.percentPlot.enabled";
    public static final boolean PARAM_DEFVAL_PERCENT_PLOT_ENABLED = true;

    public static final String PARAM_LABEL_STATS_LIST_ENABLED = "Show Statistics List";
    public static final String PARAM_KEY_STATS_LIST_ENABLED = "statistics.statsList.enabled";
    public static final boolean PARAM_DEFVAL_STATS_LIST_ENABLED = true;

    public static final String PARAM_LABEL_STATS_SPREADSHEET_ENABLED = "Show Statistics Spreadsheet";
    public static final String PARAM_KEY_STATS_SPREADSHEET_ENABLED = "statistics.statsSpreadSheet.enabled";
    public static final boolean PARAM_DEFVAL_STATS_SPREADSHEET_ENABLED = true;

    public static final String PARAM_LABEL_PERCENT_THRESHOLDS = "Include Thresholds";
    public static final String PARAM_KEY_PERCENT_THRESHOLDS = "statistics.percentThresholds";
    public static final String PARAM_DEFVAL_PERCENT_THRESHOLDS = "80,85,90,95,98";

    public static final String PARAM_LABEL_NUM_BINS = "Total Bins";
    public static final String PARAM_KEY_NUM_BINS = "statistics.numBins";
    public static final int PARAM_DEFVAL_NUM_BINS = 1000;
    public static final int PARAM_MINVAL_NUM_BINS = 1;
    public static final int PARAM_MAXVAL_NUM_BINS = 1000000;




    public static final String PARAM_LABEL_MEDIAN_ENABLED = "Include Median";
    public static final String PARAM_TOOLTIP_MEDIAN_ENABLED = "Calculation of median will increase computation time";
    public static final String PARAM_KEY_MEDIAN_ENABLED = "statistics.median.enabled";
    public static final boolean PARAM_DEFVAL_MEDIAN_ENABLED = false;

    public static final String PARAM_LABEL_MINMAX_ENABLED = "Include Min/Max";
    public static final String PARAM_TOOLTIP_MINMAX_ENABLED = "";
    public static final String PARAM_KEY_MINMAX_ENABLED = "statistics.minMax.enabled";
    public static final boolean PARAM_DEFVAL_MINMAX_ENABLED = false;

    public static final String PARAM_LABEL_TOTAL_PIXEL_COUNT_ENABLED = "Include Total Pixel Count";
    public static final String PARAM_KEY_TOTAL_PIXEL_COUNT_ENABLED = "statistics.totalPixelCount.enabled";
    public static final boolean PARAM_DEFVAL_TOTAL_PIXEL_COUNT_ENABLED = true;

    public static final String PARAM_LABEL_HISTOGRAM_STATS_ENABLED = "Include Histogram Statistics";
    public static final String PARAM_KEY_HISTOGRAM_STATS_ENABLED = "statistics.histogramStats.enabled";
    public static final boolean PARAM_DEFVAL_HISTOGRAM_STATS_ENABLED = false;

    public static final String PARAM_LABEL_FILE_METADATA_ENABLED = "Include File MetaData";
    public static final String PARAM_KEY_FILE_METADATA_ENABLED = "statistics.fileMetaData.enabled";
    public static final boolean PARAM_DEFVAL_FILE_METADATA_ENABLED = true;

    public static final String PARAM_LABEL_MASK_METADATA_ENABLED = "Include Mask MetaData";
    public static final String PARAM_KEY_MASK_METADATA_ENABLED = "statistics.maskMetaData.enabled";
    public static final boolean PARAM_DEFVAL_MASK_METADATA_ENABLED = true;

    public static final String PARAM_LABEL_BAND_METADATA_ENABLED = "Include Band MetaData";
    public static final String PARAM_KEY_BAND_METADATA_ENABLED = "statistics.bandMetaData.enabled";
    public static final boolean PARAM_DEFVAL_BAND_METADATA_ENABLED = true;


    public static final String PARAM_LABEL_BINNING_INFO_ENABLED = "Include Binning Info";
    public static final String PARAM_KEY_BINNING_INFO_ENABLED = "statistics.binningInfo.enabled";
    public static final boolean PARAM_DEFVAL_BINNING_INFO_ENABLED = true;

    public static final String PARAM_LABEL_TIME_SERIES_METADATA_ENABLED = "Include Time Series MetaData";
    public static final String PARAM_KEY_TIME_SERIES_METADATA_ENABLED = "statistics.timeSeriesMetaData.enabled";
    public static final boolean PARAM_DEFVAL_TIME_SERIES_METADATA_ENABLED = false;
    public static final String PARAM_TOOLTIPS_TIME_SERIES_METADATA_ENABLED = "See preferences for time series option";

    public static final String PARAM_LABEL_TIME_METADATA_ENABLED = "Include Time MetaData";
    public static final String PARAM_KEY_TIME_METADATA_ENABLED = "statistics.timeMetaData.enabled";
    public static final boolean PARAM_DEFVAL_TIME_METADATA_ENABLED = true;

    public static final String PARAM_LABEL_PROJECTION_PARAMETERS_METADATA_ENABLED = "Include Projection Parameters";
    public static final String PARAM_KEY_PROJECTION_PARAMETERS_METADATA_ENABLED = "statistics.projectionParameters.enabled";
    public static final boolean PARAM_DEFVAL_PROJECTION_PARAMETERS_METADATA_ENABLED = false;

    public static final String PARAM_LABEL_PLOTS_THRESH_DOMAIN_LOW = "Plot Domain Low Threshold";
    public static final String PARAM_SHORTLABEL_PLOTS_THRESH_DOMAIN_LOW = "Low";
    public static final String PARAM_KEY_PLOTS_THRESH_DOMAIN_LOW = "statistics.plotsThreshDomainLow";

    public static final double PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_LOW = 5;
    public static final double PARAM_MINVAL_PLOTS_THRESH_DOMAIN_LOW = 0;
    public static final double PARAM_MAXVAL_PLOTS_THRESH_DOMAIN_LOW = 100;

    public static final String PARAM_LABEL_PLOTS_THRESH_DOMAIN_HIGH = "Plot Domain High Threshold";
    public static final String PARAM_SHORTLABEL_PLOTS_THRESH_DOMAIN_HIGH = "High";
    public static final String PARAM_KEY_PLOTS_THRESH_DOMAIN_HIGH = "statistics.plotsThreshDomainHigh";

    public static final double PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_HIGH = 95;
    public static final double PARAM_MINVAL_PLOTS_THRESH_DOMAIN_HIGH = 0;
    public static final double PARAM_MAXVAL_PLOTS_THRESH_DOMAIN_HIGH = 100;

    public static final String PARAM_LABEL_PLOTS_DOMAIN_LOW = "Low";
    public static final double PARAM_DEFVAL_PLOTS_DOMAIN_LOW = Double.NaN;

    public static final String PARAM_LABEL_PLOTS_DOMAIN_HIGH = "High";
    public static final double PARAM_DEFVAL_PLOTS_DOMAIN_HIGH = Double.NaN;


    public static final String PARAM_LABEL_PLOTS_DOMAIN_SPAN = "Set Plot Domain: (by Value)";
    public static final String PARAM_SHORTLABEL_PLOTS_DOMAIN_SPAN = "Set Domain: (by Value)";
    public static final boolean PARAM_DEFVAL_PLOTS_DOMAIN_SPAN = false;

    public static final String PARAM_LABEL_PLOTS_THRESH_DOMAIN_SPAN = "Set Plot Domain: (by Threshold)";
    public static final String PARAM_SHORTLABEL_PLOTS_THRESH_DOMAIN_SPAN = "Set Domain: (by Threshold)";
    public static final boolean PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_SPAN = true;


    public static final String PARAM_LABEL_PLOTS_SIZE = "Set Plot Size";
    public static final String PARAM_SHORTLABEL_PLOTS_SIZE = "Set Size (includes labels)";
    public static final boolean PARAM_DEFVAL_PLOTS_SIZE = false;

    public static final String PARAM_LABEL_PLOTS_SIZE_HEIGHT = "Plot Size (Height)";
    public static final String PARAM_SHORTLABEL_PLOTS_SIZE_HEIGHT = "Height";
    public static final String PARAM_KEY_PLOTS_SIZE_HEIGHT = "statistics.plotsSizeHeight";
    public static final int PARAM_DEFVAL_PLOTS_SIZE_HEIGHT = 300;
    public static final int PARAM_MINVAL_PLOTS_SIZE_HEIGHT = 50;
    public static final int PARAM_MAXVAL_PLOTS_SIZE_HEIGHT = 2000;

    public static final String PARAM_LABEL_PLOTS_SIZE_WIDTH = "Plot Size (Width)";
    public static final String PARAM_SHORTLABEL_PLOTS_SIZE_WIDTH = "Width";
    public static final String PARAM_KEY_PLOTS_SIZE_WIDTH = "statistics.plotsSizeWidth";
    public static final int PARAM_DEFVAL_PLOTS_SIZE_WIDTH = 300;
    public static final int PARAM_MINVAL_PLOTS_SIZE_WIDTH = 50;
    public static final int PARAM_MAXVAL_PLOTS_SIZE_WIDTH = 2000;



    public static final String PARAM_KEY_SPREADSHEET_COL_WIDTH = "statistics.spreadsheetColWidth";
    public static final String PARAM_LABEL_SPREADSHEET_COL_WIDTH = "Column Width (Spreadsheet)";
    public static final int PARAM_DEFVAL_SPREADSHEET_COL_WIDTH = 0;
    public static final int PARAM_MINVAL_SPREADSHEET_COL_WIDTH = 0;
    public static final int PARAM_MAXVAL_SPREADSHEET_COL_WIDTH = 50;


    public static final String PARAM_KEY_SPREADSHEET_DECIMAL_PLACES = "statistics.textDecimalPlaces";
    public static final String PARAM_LABEL_SPREADSHEET_DECIMAL_PLACES = "Decimal Places";
    public static final int PARAM_DEFVAL_SPREADSHEET_DECIMAL_PLACES = 4;
    public static final int PARAM_MINVAL_SPREADSHEET_DECIMAL_PLACES = 0;
    public static final int PARAM_MAXVAL_SPREADSHEET_DECIMAL_PLACES = 20;


    public static final String PARAM_LABEL_COL_BREAKS_ENABLED = "Include Column Group Breaks";
    public static final String PARAM_KEY_COL_BREAKS_ENABLED = "statistics.columnBreaks.enabled";
    public static final boolean PARAM_DEFVAL_COL_BREAKS_ENABLED = true;




    public static final String PARAM_LABEL_BIN_MIN_MAX_ENABLED = "Bin Span";
    public static final String PARAM_KEY_BIN_MIN_MAX_ENABLED = "statistics.binMinMaxEnabled";
    public static final boolean PARAM_DEFVAL_BIN_MIN_MAX_ENABLED = false;

    public static final String PARAM_LABEL_BIN_MIN = "Bin Min";
    public static final String PARAM_SHORTLABEL_BIN_MIN = "Min";
    public static final String PARAM_KEY_BIN_MIN = "statistics.binMin";
    public static final double PARAM_DEFVAL_BIN_MIN = Double.NaN;

    public static final String PARAM_LABEL_BIN_MAX = "Bin Max";
    public static final String PARAM_SHORTLABEL_BIN_MAX = "Max";
    public static final String PARAM_KEY_BIN_MAX = "statistics.binMax";
    public static final double PARAM_DEFVAL_BIN_MAX = Double.NaN;


    public static final String PARAM_LABEL_BIN_WIDTH = "Bin Width";
    public static final String PARAM_SHORTLABEL_BIN_WIDTH = "Width";
    public static final String PARAM_KEY_BIN_WIDTH = "statistics.binWidthMax";
    public static final double PARAM_DEFVAL_BIN_WIDTH = Double.NaN;

    public static final String PARAM_LABEL_BIN_WIDTH_ENABLED = "Set Total Bins from Bin Width";
    public static final String PARAM_KEY_BIN_WIDTH_ENABLED = "statistics.binWidthEnabled";
    public static final boolean PARAM_DEFVAL_BIN_WIDTH_ENABLED = false;


    public static final String PARAM_KEY_RESET_TO_DEFAULTS = "statistics.resetToDefaults.enabled";

    // Restore to defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Default (statistics Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all " + NamingConvention.COLOR_LOWER_CASE + " preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;

    @Override
    protected PagePanel createPagePanel() {
        return new StatisticsPanel(this, Bundle.CTL_StatisticsTopComponent_HelpId());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_StatisticsTopComponent_HelpId());
    }
}
