package org.esa.snap.rcp.statistics;

import org.esa.snap.core.util.NamingConvention;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import java.awt.*;

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
 * @author Daniel Knowles
 */
public class StatisticsTopComponent extends AbstractStatisticsTopComponent {

    public static final String ID = StatisticsTopComponent.class.getName();

    private static final String PROPERTY_ROOT_KEY = "statistics";



    // BINS Tab

    private static final String PROPERTY_BINS_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".bins";

    public static final String PROPERTY_BINS_SECTION_KEY = PROPERTY_BINS_KEY_SUFFIX + ".section";
    public static final String PROPERTY_BINS_SECTION_LABEL = "Bins";
    public static final String PROPERTY_BINS_SECTION_TOOLTIP = "Bins";

    public static final String PROPERTY_TOTAL_BINS_KEY = PROPERTY_BINS_KEY_SUFFIX + ".total.bins";
    public static final String PROPERTY_TOTAL_BINS_LABEL = "Total Bins";
    public static final String PROPERTY_TOTAL_BINS_TOOLTIP = "Total bins to use in histogram statistics";
    public static final int PROPERTY_TOTAL_BINS_DEFAULT = 1000;
    public static final int PROPERTY_TOTAL_BINS_MIN = 1;
    public static final int PROPERTY_TOTAL_BINS_MAX = 1000000;


    public static final String PROPERTY_USE_BIN_WIDTH_LABEL = "Set Total Bins from Bin Width";
    public static final String PROPERTY_USE_BIN_WIDTH_TOOLTIP = "Set total bins from bin width";
    public static final boolean PROPERTY_USE_BIN_WIDTH_DEFAULT = false;

    public static final String PROPERTY_BIN_WIDTH_LABEL = "Bin Width";
    public static final String PROPERTY_BIN_WIDTH_TOOLTIP = "Bin Width";
    public static final double PROPERTY_BIN_WIDTH_DEFAULT = Double.NaN;

    public static final String PROPERTY_BIN_SPAN_LABEL = "Bin Span";
    public static final String PROPERTY_BIN_SPAN_TOOLTIP = "Set total bins from bin span";
    public static final boolean PROPERTY_BIN_SPAN_DEFAULT = false;

    public static final String PROPERTY_BIN_MIN_LABEL = "Bin Min";
    public static final String PROPERTY_BIN_MIN_LABEL_SHORT = "Min";
    public static final String PROPERTY_BIN_MIN_TOOLTIP = "Min bin span";
    public static final double PROPERTY_BIN_MIN_DEFAULT = Double.NaN;

    public static final String PROPERTY_BIN_MAX_LABEL = "Bin Max";
    public static final String PROPERTY_BIN_MAX_LABEL_SHORT = "Max";
    public static final String PROPERTY_BIN_MAX_TOOLTIP = "Max bin span";
    public static final double PROPERTY_BIN_MAX_DEFAULT = Double.NaN;

    public static final String PROPERTY_LOG_SCALED_BINS_KEY = PROPERTY_BINS_KEY_SUFFIX + ".log.scaled.bins";
    public static final String PROPERTY_LOG_SCALED_BINS_LABEL = "Log Scaled Bins";
    public static final String PROPERTY_LOG_SCALED_BINS_TOOLTIP = "Log scaled bins used in histogram statistics";
    public static final boolean PROPERTY_LOG_SCALED_BINS_DEFAULT = false;





    // FIELDS

    private static final String PROPERTY_FIELDS_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".fields";

    public static final String PROPERTY_FIELDS_SECTION_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".section";
    public static final String PROPERTY_FIELDS_SECTION_LABEL = "Fields";
    public static final String PROPERTY_FIELDS_SECTION_TOOLTIP = "Fields";

    public static final String PROPERTY_TOTAL_PIXEL_COUNT_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.total.pixel.count";
    public static final String PROPERTY_TOTAL_PIXEL_COUNT_LABEL = "Include Total Pixel Count";
    public static final String PROPERTY_TOTAL_PIXEL_COUNT_TOOLTIP = "Include total pixel count in statistics results";
    public static final boolean PROPERTY_TOTAL_PIXEL_COUNT_DEFAULT = true;

    public static final String PROPERTY_MEDIAN_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.median";
    public static final String PROPERTY_MEDIAN_LABEL = "Include Median";
    public static final String PROPERTY_MEDIAN_TOOLTIP = "<html>Include median in statistics results<br>Note: calculation of median will increase computation time</html>";
    public static final boolean PROPERTY_MEDIAN_DEFAULT = false;

    public static final String PROPERTY_MINMAX_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.min.max";
    public static final String PROPERTY_MINMAX_LABEL = "Include Min/Max";
    public static final String PROPERTY_MINMAX_TOOLTIP = "Include min and max field in statistics results";
    public static final boolean PROPERTY_MINMAX_DEFAULT = false;

    public static final String PROPERTY_PERCENTILE_THRESHOLDS_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.percentile.thresholds";
    public static final String PROPERTY_PERCENTILE_THRESHOLDS_LABEL = "Include Percentile Thresholds";
    public static final String PROPERTY_PERCENTILE_THRESHOLDS_TOOLTIP = "Include histogram percentile thresholds in statistics results";
    public static final String PROPERTY_PERCENTILE_THRESHOLDS_DEFAULT = "80,85,90,95,98";

    public static final String PROPERTY_BINNING_INFO_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.binning.info";
    public static final String PROPERTY_BINNING_INFO_LABEL = "Include Binning Info";
    public static final String PROPERTY_BINNING_INFO_TOOLTIP = "Include binning info in statistics results";
    public static final boolean PROPERTY_BINNING_INFO_DEFAULT = true;

    public static final String PROPERTY_HISTOGRAM_STATS_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.histogram.stats";
    public static final String PROPERTY_HISTOGRAM_STATS_LABEL = "Include Histogram Statistics";
    public static final String PROPERTY_HISTOGRAM_STATS_TOOLTIP = "Include histogram statistics in statistics results";
    public static final boolean PROPERTY_HISTOGRAM_STATS_DEFAULT = false;

    public static final String PROPERTY_BAND_METADATA_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.band.metadata";
    public static final String PROPERTY_BAND_METADATA_LABEL = "Include Band MetaData";
    public static final String PROPERTY_BAND_METADATA_TOOLTIP = "Include band metadata in statistics results";
    public static final boolean PROPERTY_BAND_METADATA_DEFAULT = true;

    public static final String PROPERTY_FILE_METADATA_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.file.metadata";
    public static final String PROPERTY_FILE_METADATA_LABEL = "Include File MetaData";
    public static final String PROPERTY_FILE_METADATA_TOOLTIP = "Include file metadata in statistics results";
    public static final boolean PROPERTY_FILE_METADATA_DEFAULT = true;

    public static final String PROPERTY_MASK_METADATA_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.mask.metadata";
    public static final String PROPERTY_MASK_METADATA_LABEL = "Include Mask MetaData";
    public static final String PROPERTY_MASK_METADATA_TOOLTIP = "Include mask metadata in statistics results";
    public static final boolean PROPERTY_MASK_METADATA_DEFAULT = true;

    public static final String PROPERTY_PROJECTION_METADATA_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.projection.metadata";
    public static final String PROPERTY_PROJECTION_METADATA_LABEL = "Include Projection Metadata";
    public static final String PROPERTY_PROJECTION_METADATA_TOOLTIP = "Include projection metadata in statistics (spreadsheet) results";
    public static final boolean PROPERTY_PROJECTION_METADATA_DEFAULT = false;

    public static final String PROPERTY_TIME_METADATA_KEY = PROPERTY_FIELDS_KEY_SUFFIX + ".include.time.metadata";
    public static final String PROPERTY_TIME_METADATA_LABEL = "Include Time MetaData";
    public static final String PROPERTY_TIME_METADATA_TOOLTIP = "Include time metadata in statistics results";
    public static final boolean PROPERTY_TIME_METADATA_DEFAULT = true;

    public static final String PROPERTY_TIME_SERIES_METADATA_KEY = PROPERTY_FIELDS_KEY_SUFFIX + "include.timeseries.metadata";
    public static final String PROPERTY_TIME_SERIES_METADATA_LABEL = "Include Time Series MetaData";
    public static final String PROPERTY_TIME_SERIES_METADATA_TOOLTIP = "See preferences for time series option";
    public static final boolean PROPERTY_TIME_SERIES_METADATA_DEFAULT = false;




    // FORMAT TAB

    private static final String PROPERTY_FORMAT_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".format";

    public static final String PROPERTY_FORMAT_SECTION_KEY = PROPERTY_FORMAT_KEY_SUFFIX + ".section";
    public static final String PROPERTY_FORMAT_SECTION_LABEL = "Format";
    public static final String PROPERTY_FORMAT_SECTION_TOOLTIP = "Format";

    public static final String PROPERTY_DECIMAL_PLACES_KEY = PROPERTY_FORMAT_KEY_SUFFIX + ".decimal.places";
    public static final String PROPERTY_DECIMAL_PLACES_LABEL = "Decimal Places";
    public static final String PROPERTY_DECIMAL_PLACES_TOOLTIP = "Decimal places to format statistics results";
    public static final int PROPERTY_DECIMAL_PLACES_DEFAULT = 4;
    public static final int PROPERTY_DECIMAL_PLACES_MIN = 0;
    public static final int PROPERTY_DECIMAL_PLACES_MAX = 20;

    public static final String PROPERTY_SPREADSHEET_COL_WIDTH_KEY = PROPERTY_FORMAT_KEY_SUFFIX + ".spreadsheet.column.width";
    public static final String PROPERTY_SPREADSHEET_COL_WIDTH_LABEL = "Column Width (Spreadsheet)";
    public static final String PROPERTY_SPREADSHEET_COL_WIDTH_TOOLTIP = "Column width (in spreadsheet)";
    public static final int PROPERTY_SPREADSHEET_COL_WIDTH_DEFAULT = 0;
    public static final int PROPERTY_SPREADSHEET_COL_WIDTH_MIN = 0;
    public static final int PROPERTY_SPREADSHEET_COL_WIDTH_MAX = 50;

    public static final String PROPERTY_COL_BREAKS_KEY = PROPERTY_FORMAT_KEY_SUFFIX + ".column.breaks";
    public static final String PROPERTY_COL_BREAKS_LABEL = "Include Column Group Breaks";
    public static final String PROPERTY_COL_BREAKS_TOOLTIP = "Include column group breaks (in spreadsheet)";
    public static final boolean PROPERTY_COL_BREAKS_DEFAULT = true;

    public static final String PROPERTY_SCROLL_LINES_KEY = PROPERTY_FORMAT_KEY_SUFFIX + ".scroll.lines";
    public static final String PROPERTY_SCROLL_LINES_LABEL = "Scroll List Preferred Lines";
    public static final String PROPERTY_SCROLL_LINES_TOOLTIP = "Scroll list (bands and masks) preferred lines visible";
    public static final int PROPERTY_SCROLL_LINES_DEFAULT = 16;
    public static final int PROPERTY_SCROLL_LINES_MIN = 4;
    public static final int PROPERTY_SCROLL_LINES_MAX = 30;


    // PLOTS Tab

    private static final String PROPERTY_PLOTS_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".plots";

    public static final String PROPERTY_PLOTS_SECTION_KEY = PROPERTY_PLOTS_KEY_SUFFIX + ".section";
    public static final String PROPERTY_PLOTS_SECTION_LABEL = "Plots";
    public static final String PROPERTY_PLOTS_SECTION_TOOLTIP = "Plots";

    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_KEY = PROPERTY_PLOTS_KEY_SUFFIX + ".plots.domain.thresh";
    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_LABEL = "Set Domain: (by Threshold)";
    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_TOOLTIP = "Set domain (by threshold)";
    public static final boolean PROPERTY_PLOTS_DOMAIN_THRESH_DEFAULT = true;

    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_LOW_KEY = PROPERTY_PLOTS_KEY_SUFFIX + ".plots.domain.thresh.low";
    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_LOW_LABEL = "Plot Domain Low Threshold";
    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_LOW_LABEL_SHORT = "Low";
    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_LOW_TOOLTIP = "Plot domain by threshold (low value)";
    public static final double PROPERTY_PLOTS_DOMAIN_THRESH_LOW_DEFAULT = 5;
    public static final double PROPERTY_PLOTS_DOMAIN_THRESH_LOW_MIN = 0;
    public static final double PROPERTY_PLOTS_DOMAIN_THRESH_LOW_MAX = 100;

    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_KEY = PROPERTY_PLOTS_KEY_SUFFIX + ".plots.domain.thresh.high";
    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_LABEL = "Plot Domain High Threshold";
    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_LABEL_SHORT = "High";
    public static final String PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_TOOLTIP = "Plot domain by threshold (high value)";
    public static final double PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_DEFAULT = 95;
    public static final double PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_MIN = 0;
    public static final double PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_MAX = 100;


    public static final String PROPERTY_PLOTS_DOMAIN_VALUE_LABEL = "Set Domain: (by Value)";
    public static final String PROPERTY_PLOTS_DOMAIN_VALUE_TOOLTIP = "Set domain (by value)";
    public static final boolean PROPERTY_PLOTS_DOMAIN_VALUE_DEFAULT = false;

    public static final String PROPERTY_PLOTS_DOMAIN_VALUE_LOW_LABEL = "Low";
    public static final String PROPERTY_PLOTS_DOMAIN_VALUE_LOW_TOOLTIP = "Plot domain by value (low)";
    public static final double PROPERTY_PLOTS_DOMAIN_VALUE_LOW_DEFAULT = Double.NaN;

    public static final String PROPERTY_PLOTS_DOMAIN_VALUE_HIGH_LABEL = "High";
    public static final String PROPERTY_PLOTS_DOMAIN_VALUE_HIGH_TOOLTIP = "Plot domain by value (high)";
    public static final double PROPERTY_PLOTS_DOMAIN_VALUE_HIGH_DEFAULT = Double.NaN;


    public static final String PROPERTY_PLOTS_EXACT_SIZE_KEY = PROPERTY_PLOTS_KEY_SUFFIX + ".set.plot.size";
    public static final String PROPERTY_PLOTS_EXACT_SIZE_LABEL = "Set Exact Size (includes labels)";
    public static final String PROPERTY_PLOTS_EXACT_SIZE_TOOLTIP = "Set exact size of plots (includes labels)";
    public static final boolean PROPERTY_PLOTS_EXACT_SIZE_DEFAULT = false;

    public static final String PROPERTY_PLOTS_SIZE_WIDTH_KEY = PROPERTY_PLOTS_KEY_SUFFIX + ".plot.size.width";
    public static final String PROPERTY_PLOTS_SIZE_WIDTH_LABEL = "Plot Size (Width)";
    public static final String PROPERTY_PLOTS_SIZE_WIDTH_LABEL_SHORT = "Width";
    public static final String PROPERTY_PLOTS_SIZE_WIDTH_TOOLTIP = "Plot Size (Width)";
    public static final int PROPERTY_PLOTS_SIZE_WIDTH_DEFAULT = 300;
    public static final int PROPERTY_PLOTS_SIZE_WIDTH_MIN = 50;
    public static final int PROPERTY_PLOTS_SIZE_WIDTH_MAX = 2000;

    public static final String PROPERTY_PLOTS_SIZE_HEIGHT_KEY = PROPERTY_PLOTS_KEY_SUFFIX + ".plot.size.height";
    public static final String PROPERTY_PLOTS_SIZE_HEIGHT_LABEL = "Plot Size (Height)";
    public static final String PROPERTY_PLOTS_SIZE_HEIGHT_LABEL_SHORT = "Height";
    public static final String PROPERTY_PLOTS_SIZE_HEIGHT_TOOLTIP = "Plot Size (Height)";
    public static final int PROPERTY_PLOTS_SIZE_HEIGHT_DEFAULT = 300;
    public static final int PROPERTY_PLOTS_SIZE_HEIGHT_MIN = 50;
    public static final int PROPERTY_PLOTS_SIZE_HEIGHT_MAX = 2000;

    public static final String PROPERTY_PLOTS_COLOR_KEY = PROPERTY_PLOTS_KEY_SUFFIX + ".plot.color";
    public static final String PROPERTY_PLOTS_COLOR_LABEL = "Plot Color";
    public static final String PROPERTY_PLOTS_COLOR_TOOLTIP = "Plot Color";
    public static final Color PROPERTY_PLOTS_COLOR_DEFAULT = Color.BLACK;
    public static final String PROPERTY_PLOTS_BACKGROUND_COLOR_KEY = PROPERTY_PLOTS_KEY_SUFFIX + ".plot.backgroundcolor";
    public static final String PROPERTY_PLOTS_BACKGROUND_COLOR_LABEL = "Plot Backgound Color";
    public static final String PROPERTY_PLOTS_BACKGROUND_COLOR_TOOLTIP = "Plot Backgound Color";
    public static final Color PROPERTY_PLOTS_BACKGROUND_COLOR_DEFAULT = Color.WHITE;
    public static final String PROPERTY_PLOTS_LABEL_COLOR_KEY = PROPERTY_PLOTS_KEY_SUFFIX + ".plot.labelcolor";
    public static final String PROPERTY_PLOTS_LABEL_COLOR_LABEL = "Plot Label Color";
    public static final String PROPERTY_PLOTS_LABEL_COLOR_TOOLTIP = "Plot Label Color";
    public static final Color PROPERTY_PLOTS_LABEL_COLOR_DEFAULT = Color.BLACK;



    // VIEW Tab

    private static final String PROPERTY_VIEW_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".view";

    public static final String PROPERTY_VIEW_SECTION_KEY = PROPERTY_VIEW_KEY_SUFFIX + ".section";
    public static final String PROPERTY_VIEW_SECTION_LABEL = "View";
    public static final String PROPERTY_VIEW_SECTION_TOOLTIP = "View";

    public static final String PROPERTY_SHOW_HISTOGRAM_PLOT_KEY = PROPERTY_VIEW_KEY_SUFFIX + ".show.histogram.plot";
    public static final String PROPERTY_SHOW_HISTOGRAM_PLOT_LABEL = "Show Histogram Plot";
    public static final String PROPERTY_SHOW_HISTOGRAM_PLOT_TOOLTIP = "Show histogram plot in results window";
    public static final boolean PROPERTY_SHOW_HISTOGRAM_PLOT_DEFAULT = true;

    public static final String PROPERTY_SHOW_PERCENTILE_PLOT_KEY = PROPERTY_VIEW_KEY_SUFFIX + ".show.percent.plot";
    public static final String PROPERTY_SHOW_PERCENTILE_PLOT_LABEL = "Show Percentile Plot";
    public static final String PROPERTY_SHOW_PERCENTILE_PLOT_TOOLTIP = "Show percentile plot in results window";
    public static final boolean PROPERTY_SHOW_PERCENTILE_PLOT_DEFAULT = true;

    public static final String PROPERTY_SHOW_STATISTICS_LIST_KEY = PROPERTY_VIEW_KEY_SUFFIX + ".show.statistics.list";
    public static final String PROPERTY_SHOW_STATISTICS_LIST_LABEL = "Show Statistics List";
    public static final String PROPERTY_SHOW_STATISTICS_LIST_TOOLTIP = "Show statistics list in results window";
    public static final boolean PROPERTY_SHOW_STATISTICS_LIST_DEFAULT = true;

    public static final String PROPERTY_SHOW_SPREADSHEET_KEY = PROPERTY_VIEW_KEY_SUFFIX + ".show.spreadsheet";
    public static final String PROPERTY_SHOW_SPREADSHEET_LABEL = "Show Statistics Spreadsheet";
    public static final String PROPERTY_SHOW_SPREADSHEET_TOOLTIP = "Show statistics spreadsheet in results window";
    public static final boolean PROPERTY_SHOW_SPREADSHEET_DEFAULT = true;




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
        PagePanel pagePanel = new StatisticsPanel(this, Bundle.CTL_StatisticsTopComponent_HelpId());
        setMinimumSize(pagePanel.getMinimumSize());
        setPreferredSize(pagePanel.getPreferredSize());
        int minWidth = getMinimumSize().width;
        int prefWidth = getPreferredSize().width;
        return pagePanel;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_StatisticsTopComponent_HelpId());
    }
}
