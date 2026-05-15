/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.preferences.general;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.rcp.statistics.StatisticsTopComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;

import static org.esa.snap.rcp.statistics.StatisticsTopComponent.*;

/**
 * Panel handling general statistics preferences. Sub-panel of the "General"-panel.
 *
 * @author Daniel Knowles (NASA)
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_Statistics=Statistics",
        "Options_Keywords_Statistics=statistics, general"
})
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_Statistics",
        keywords = "#Options_Keywords_Statistics",
        keywordsCategory = "statistics",
        id = "statisticsController",
        position = 4)


public final class StatisticsController extends DefaultConfigController {

    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;

//    Enablement enablementGeneralPalette;
//    Enablement enablementGeneralRange;
//    Enablement enablementGeneralLog;
//


    protected PropertySet createPropertySet() {
        return createPropertySet(new GeneralLayerBean());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("statistics");
    }


    @Override
    protected JPanel createPanel(BindingContext context) {

//        initPropertyDefaults(context,  PARAM_KEY_RESET_TO_DEFAULTS, false);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_BINS_SECTION_KEY, true);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_TOTAL_BINS_KEY, StatisticsTopComponent.PROPERTY_TOTAL_BINS_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_LOG_SCALED_BINS_KEY, StatisticsTopComponent.PROPERTY_LOG_SCALED_BINS_DEFAULT);

        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_FIELDS_SECTION_KEY, true);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_KEY, StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_MEDIAN_KEY, StatisticsTopComponent.PROPERTY_MEDIAN_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_MINMAX_KEY, StatisticsTopComponent.PROPERTY_MINMAX_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_KEY, StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_BINNING_INFO_KEY, StatisticsTopComponent.PROPERTY_BINNING_INFO_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_KEY, StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_BAND_METADATA_KEY, StatisticsTopComponent.PROPERTY_BAND_METADATA_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_BAND_METADATA_VIEW_ANGLE_KEY, StatisticsTopComponent.PROPERTY_BAND_METADATA_VIEW_ANGLE_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_FILE_METADATA_KEY, StatisticsTopComponent.PROPERTY_FILE_METADATA_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_MASK_METADATA_KEY, StatisticsTopComponent.PROPERTY_MASK_METADATA_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_KEY, StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_TIME_METADATA_KEY, StatisticsTopComponent.PROPERTY_TIME_METADATA_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_KEY, StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_DEFAULT);

        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_FORMAT_SECTION_KEY, true);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_KEY, StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_KEY, StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_COL_BREAKS_KEY, StatisticsTopComponent.PROPERTY_COL_BREAKS_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_SCROLL_LINES_KEY, StatisticsTopComponent.PROPERTY_SCROLL_LINES_DEFAULT);

        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_SECTION_KEY, true);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_KEY, StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_KEY, StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_KEY, StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_KEY, StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_KEY, StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_KEY, StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_DEFAULT);

        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_COLOR_KEY, StatisticsTopComponent.PROPERTY_PLOTS_COLOR_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_KEY, StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_KEY, StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_DEFAULT);

        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_VIEW_SECTION_KEY, true);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_KEY, StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_KEY, StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_KEY, StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_DEFAULT);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_KEY, StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_DEFAULT);


//        restoreDefaults =  initPropertyDefaults(context, "statistics.restore.defaults.apply", false);
        restoreDefaults = initPropertyDefaults(context, PROPERTY_RESTORE_DEFAULTS_NAME, PROPERTY_RESTORE_DEFAULTS_DEFAULT);

//         Create UI


        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();

        PropertySet propertyContainer = context.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

        int currRow = 0;
        for (Property property : properties) {
            PropertyDescriptor descriptor = property.getDescriptor();
            PropertyPane.addComponent(currRow, tableLayout, pageUI, context, registry, descriptor);
            currRow++;
        }

        pageUI.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(50), BorderLayout.EAST);
        return parent;
    }


    @Override
    protected void configure(BindingContext context) {

        configureGeneralCustomEnablement(context);

        // Handle resetDefaults events - set all other components to defaults
        restoreDefaults.addPropertyChangeListener(evt -> {
            handleRestoreDefaults(context);
        });


        // Add listeners to all components in order to uncheck restoreDefaults checkbox accordingly

        PropertySet propertyContainer = context.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

        for (Property property : properties) {
            if (property != restoreDefaults) {
                property.addPropertyChangeListener(evt -> {
                    handlePreferencesPropertyValueChange(context);
                });
            }
        }
    }


    /**
     * Test all properties to determine whether the current value is the default value
     *
     * @param context
     * @return
     * @author Daniel Knowles
     */
    private boolean isDefaults(BindingContext context) {

        PropertySet propertyContainer = context.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

        for (Property property : properties) {
            if (property != restoreDefaults && property.getDescriptor().getDefaultValue() != null)
                if (!property.getValue().equals(property.getDescriptor().getDefaultValue())) {
                    return false;
                }
        }

        return true;
    }


    /**
     * Handles the restore defaults action
     *
     * @param context
     * @author Daniel Knowles
     */
    private void handleRestoreDefaults(BindingContext context) {
        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                if (restoreDefaults.getValue()) {

                    PropertySet propertyContainer = context.getPropertySet();
                    Property[] properties = propertyContainer.getProperties();

                    for (Property property : properties) {
                        if (property != restoreDefaults && property.getDescriptor().getDefaultValue() != null)
                            property.setValue(property.getDescriptor().getDefaultValue());
                    }
                }
            } catch (ValidationException e) {
                e.printStackTrace();
            }
            propertyValueChangeEventsEnabled = true;

            context.setComponentsEnabled(PROPERTY_RESTORE_DEFAULTS_NAME, false);
        }
    }


    /**
     * Set restoreDefault component because a property has changed
     *
     * @param context
     * @author Daniel Knowles
     */
    private void handlePreferencesPropertyValueChange(BindingContext context) {
        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                restoreDefaults.setValue(isDefaults(context));
                context.setComponentsEnabled(PROPERTY_RESTORE_DEFAULTS_NAME, !isDefaults(context));
            } catch (ValidationException e) {
                e.printStackTrace();
            }
            propertyValueChangeEventsEnabled = true;
        }
    }


    /**
     * Initialize the property descriptor default value
     *
     * @param context
     * @param propertyName
     * @param propertyDefault
     * @return
     * @author Daniel Knowles
     */
    private Property initPropertyDefaults(BindingContext context, String propertyName, Object propertyDefault) {

        Property property = context.getPropertySet().getProperty(propertyName);

        property.getDescriptor().setDefaultValue(propertyDefault);

        return property;
    }


    /**
     * Configure enablement of the components tied to PROPERTY_GENERAL_CUSTOM_KEY
     *
     * @param context
     * @author Daniel Knowles
     */
    private void configureGeneralCustomEnablement(BindingContext context) {


//        enablementGeneralPalette = context.bindEnabledState(ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_KEY, true,
//                ColorManipulationDefaults.PROPERTY_GENERAL_CUSTOM_KEY, true);
//
//        enablementGeneralRange = context.bindEnabledState(ColorManipulationDefaults.PROPERTY_GENERAL_RANGE_KEY, true,
//                ColorManipulationDefaults.PROPERTY_GENERAL_CUSTOM_KEY, true);
//
//        enablementGeneralLog = context.bindEnabledState(ColorManipulationDefaults.PROPERTY_GENERAL_LOG_KEY, true,
//                ColorManipulationDefaults.PROPERTY_GENERAL_CUSTOM_KEY, true);
//
//
//
//        // handle it the first time so bound properties get properly enabled
////        handleGeneralCustom();
//        enablementGeneralPalette.apply();
//        enablementGeneralRange.apply();
//        enablementGeneralLog.apply();
    }

//    /**
//     * Handles enablement of the components
//     *
//     * @author Daniel Knowles
//     */
//    private void handleGeneralCustom() {
//        enablementGeneralPalette.apply();
//        enablementGeneralRange.apply();
//        enablementGeneralLog.apply();
//    }
//
//
//
//
//
//
//


    static class GeneralLayerBean {

        // Bins Section

        @Preference(label = StatisticsTopComponent.PROPERTY_BINS_SECTION_LABEL,
                key = StatisticsTopComponent.PROPERTY_BINS_SECTION_KEY,
                description = StatisticsTopComponent.PROPERTY_BINS_SECTION_TOOLTIP)
        boolean binsSection = true;

        @Preference(label = StatisticsTopComponent.PROPERTY_TOTAL_BINS_LABEL,
                key = StatisticsTopComponent.PROPERTY_TOTAL_BINS_KEY,
                description = StatisticsTopComponent.PROPERTY_TOTAL_BINS_TOOLTIP,
                interval = "[" + StatisticsTopComponent.PROPERTY_TOTAL_BINS_MIN + "," + StatisticsTopComponent.PROPERTY_TOTAL_BINS_MAX + "]"
        )
        int totalBins = StatisticsTopComponent.PROPERTY_TOTAL_BINS_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_LOG_SCALED_BINS_LABEL,
                key = StatisticsTopComponent.PROPERTY_LOG_SCALED_BINS_KEY,
                description = StatisticsTopComponent.PROPERTY_LOG_SCALED_BINS_TOOLTIP
        )
        boolean logScaledBins = StatisticsTopComponent.PROPERTY_LOG_SCALED_BINS_DEFAULT;




        // Fields Section

        @Preference(label = StatisticsTopComponent.PROPERTY_FIELDS_SECTION_LABEL,
                key = StatisticsTopComponent.PROPERTY_FIELDS_SECTION_KEY,
                description = StatisticsTopComponent.PROPERTY_FIELDS_SECTION_TOOLTIP)
        boolean fieldsSection = true;

        @Preference(label = StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_LABEL,
                key = StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_KEY,
                description = StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_TOOLTIP
        )
        boolean includeTotalPixelCount = StatisticsTopComponent.PROPERTY_TOTAL_PIXEL_COUNT_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_MEDIAN_LABEL,
                description = StatisticsTopComponent.PROPERTY_MEDIAN_TOOLTIP,
                key = StatisticsTopComponent.PROPERTY_MEDIAN_KEY)
        boolean includeMedian = StatisticsTopComponent.PROPERTY_MEDIAN_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_MINMAX_LABEL,
                key = StatisticsTopComponent.PROPERTY_MINMAX_KEY,
                description = StatisticsTopComponent.PROPERTY_MINMAX_TOOLTIP)
        boolean includeMinMax = StatisticsTopComponent.PROPERTY_MINMAX_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_LABEL,
                key = StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_KEY,
                description = StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_TOOLTIP)
        String includePercentThresholds = StatisticsTopComponent.PROPERTY_PERCENTILE_THRESHOLDS_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_BINNING_INFO_LABEL,
                key = StatisticsTopComponent.PROPERTY_BINNING_INFO_KEY,
                description = StatisticsTopComponent.PROPERTY_BINNING_INFO_TOOLTIP)
        boolean includeBinningInfo = StatisticsTopComponent.PROPERTY_BINNING_INFO_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_LABEL,
                key = StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_KEY,
                description = StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_TOOLTIP)
        boolean includeHistogram = StatisticsTopComponent.PROPERTY_HISTOGRAM_STATS_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_BAND_METADATA_LABEL,
                key = StatisticsTopComponent.PROPERTY_BAND_METADATA_KEY,
                description = StatisticsTopComponent.PROPERTY_BAND_METADATA_TOOLTIP)
        boolean includeBandMetadata = StatisticsTopComponent.PROPERTY_BAND_METADATA_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_BAND_METADATA_VIEW_ANGLE_LABEL,
                key = StatisticsTopComponent.PROPERTY_BAND_METADATA_VIEW_ANGLE_KEY,
                description = StatisticsTopComponent.PROPERTY_BAND_METADATA_VIEW_ANGLE_TOOLTIP)
        boolean includeBandMetadataViewAngle = StatisticsTopComponent.PROPERTY_BAND_METADATA_VIEW_ANGLE_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_FILE_METADATA_LABEL,
                key = StatisticsTopComponent.PROPERTY_FILE_METADATA_KEY,
                description = StatisticsTopComponent.PROPERTY_FILE_METADATA_TOOLTIP)
        boolean includeFileMetadata = StatisticsTopComponent.PROPERTY_FILE_METADATA_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_MASK_METADATA_LABEL,
                key = StatisticsTopComponent.PROPERTY_MASK_METADATA_KEY,
                description = StatisticsTopComponent.PROPERTY_MASK_METADATA_TOOLTIP)
        boolean includeMaskMetadata = StatisticsTopComponent.PROPERTY_MASK_METADATA_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_LABEL,
                key = StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_KEY,
                description = StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_TOOLTIP)
        boolean includeProjectionMetadata = StatisticsTopComponent.PROPERTY_PROJECTION_METADATA_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_TIME_METADATA_LABEL,
                key = StatisticsTopComponent.PROPERTY_TIME_METADATA_KEY,
                description = StatisticsTopComponent.PROPERTY_TIME_METADATA_TOOLTIP)
        boolean includeTimeMetadata = StatisticsTopComponent.PROPERTY_TIME_METADATA_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_LABEL,
                description = StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_TOOLTIP,
                key = StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_KEY)
        boolean includeTimeSeriesMetadata = StatisticsTopComponent.PROPERTY_TIME_SERIES_METADATA_DEFAULT;




        // Format Section

        @Preference(label = StatisticsTopComponent.PROPERTY_FORMAT_SECTION_LABEL,
                key = StatisticsTopComponent.PROPERTY_FORMAT_SECTION_KEY,
                description = StatisticsTopComponent.PROPERTY_FORMAT_SECTION_TOOLTIP)
        boolean formatSection = true;

        @Preference(label = StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_LABEL,
                key = StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_KEY,
                description = StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_TOOLTIP,
                interval = "[" + StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_MIN + "," + StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_MAX + "]"
        )
        int decimalPlaces = StatisticsTopComponent.PROPERTY_DECIMAL_PLACES_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_LABEL,
                key = StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_KEY,
                description = StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_TOOLTIP,
                interval = "[" + StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_MIN + "," + StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_MAX + "]"
        )
        int columnWidth = StatisticsTopComponent.PROPERTY_SPREADSHEET_COL_WIDTH_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_COL_BREAKS_LABEL,
                key = StatisticsTopComponent.PROPERTY_COL_BREAKS_KEY,
                description = StatisticsTopComponent.PROPERTY_COL_BREAKS_TOOLTIP)
        boolean includeColumnBreaks = StatisticsTopComponent.PROPERTY_COL_BREAKS_DEFAULT;

        @Preference(
                key = StatisticsTopComponent.PROPERTY_SCROLL_LINES_KEY,
                label = StatisticsTopComponent.PROPERTY_SCROLL_LINES_LABEL,
                description = StatisticsTopComponent.PROPERTY_SCROLL_LINES_TOOLTIP,
                interval = "[" + StatisticsTopComponent.PROPERTY_SCROLL_LINES_MIN + "," + StatisticsTopComponent.PROPERTY_SCROLL_LINES_MAX + "]"
        )
        int scrollLines = StatisticsTopComponent.PROPERTY_SCROLL_LINES_DEFAULT;



        // Plots Section

        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_SECTION_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_SECTION_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_SECTION_TOOLTIP)
        boolean plotsSection = true;

        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_TOOLTIP)
        boolean plotsDomainByThresh = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_DEFAULT;


        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_TOOLTIP,
                interval = "[" + StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_MIN + "," + StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_MAX + "]"
        )
        double plotsDomainByThreshLow = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_LOW_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_TOOLTIP,
                interval = "[" + StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_MIN + "," + StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_MAX + "]"
        )
        double plotsDomainByThreshHigh = StatisticsTopComponent.PROPERTY_PLOTS_DOMAIN_THRESH_HIGH_DEFAULT;


        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_TOOLTIP)
        boolean plotsSetExactSize = StatisticsTopComponent.PROPERTY_PLOTS_EXACT_SIZE_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_TOOLTIP,
                interval = "[" + StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_MIN + "," + StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_MAX + "]"
        )
        int plotsSizeWidth = StatisticsTopComponent.PROPERTY_PLOTS_SIZE_WIDTH_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_TOOLTIP,
                interval = "[" + StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_MIN + "," + StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_MAX + "]"
        )
        int plotsSizeHeight = StatisticsTopComponent.PROPERTY_PLOTS_SIZE_HEIGHT_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_COLOR_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_COLOR_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_COLOR_TOOLTIP
        )
        Color plotColor = StatisticsTopComponent.PROPERTY_PLOTS_COLOR_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_TOOLTIP
        )
        Color plotBackgroundColor = StatisticsTopComponent.PROPERTY_PLOTS_BACKGROUND_COLOR_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_TOOLTIP
        )
        Color plotLabelColor = StatisticsTopComponent.PROPERTY_PLOTS_LABEL_COLOR_DEFAULT;

        // View Section

        @Preference(label = StatisticsTopComponent.PROPERTY_VIEW_SECTION_LABEL,
                key = StatisticsTopComponent.PROPERTY_VIEW_SECTION_KEY,
                description = StatisticsTopComponent.PROPERTY_VIEW_SECTION_TOOLTIP)
        boolean viewSection = true;

        @Preference(label = StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_LABEL,
                key = StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_KEY,
                description = StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_TOOLTIP)
        boolean showHistogramPlots = StatisticsTopComponent.PROPERTY_SHOW_HISTOGRAM_PLOT_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_LABEL,
                key = StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_KEY,
                description = StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_TOOLTIP)
        boolean showPercentilePlots = StatisticsTopComponent.PROPERTY_SHOW_PERCENTILE_PLOT_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_LABEL,
                key = StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_KEY,
                description = StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_TOOLTIP)
        boolean showStatisticsList = StatisticsTopComponent.PROPERTY_SHOW_STATISTICS_LIST_DEFAULT;

        @Preference(label = StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_LABEL,
                key = StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_KEY,
                description = StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_TOOLTIP)
        boolean showStatisticsSpreadSheet = StatisticsTopComponent.PROPERTY_SHOW_SPREADSHEET_DEFAULT;


        // Restore Defaults

        @Preference(label = PROPERTY_RESTORE_SECTION_LABEL,
                key = PROPERTY_RESTORE_SECTION_KEY,
                description = PROPERTY_RESTORE_SECTION_TOOLTIP)
        boolean restoreDefaultsSection = true;

        @Preference(label = PROPERTY_RESTORE_DEFAULTS_LABEL,
                key = PROPERTY_RESTORE_DEFAULTS_NAME,
                description = PROPERTY_RESTORE_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = PROPERTY_RESTORE_DEFAULTS_DEFAULT;
    }
}

