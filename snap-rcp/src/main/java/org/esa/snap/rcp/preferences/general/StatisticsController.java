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

    @Override
    protected PropertySet createPropertySet() {
        return createPropertySet(new StatisticsBean());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("statistics");
    }

    static class StatisticsBean {
        // Default Bins

        @Preference(label = StatisticsTopComponent.PROPERTY_BINS_DEFAULT_SECTION_LABEL,
                key = StatisticsTopComponent.PROPERTY_BINS_DEFAULT_SECTION_KEY,
                description = StatisticsTopComponent.PROPERTY_BINS_DEFAULT_SECTION_TOOLTIP)
        boolean defaultBinsSection = true;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_NUM_BINS,
                key = StatisticsTopComponent.PARAM_KEY_NUM_BINS)
        int numBins = StatisticsTopComponent.PARAM_DEFVAL_NUM_BINS;

        // Fields Options

        @Preference(label = StatisticsTopComponent.PROPERTY_FIELDS_DEFAULT_SECTION_LABEL,
                key = StatisticsTopComponent.PROPERTY_FIELDS_DEFAULT_SECTION_KEY,
                description = StatisticsTopComponent.PROPERTY_FIELDS_DEFAULT_SECTION_TOOLTIP)
        boolean defaultFiledsSection = true;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_TOTAL_PIXEL_COUNT_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_TOTAL_PIXEL_COUNT_ENABLED)
        boolean includeTotalPixelCount = StatisticsTopComponent.PARAM_DEFVAL_TOTAL_PIXEL_COUNT_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_MEDIAN_ENABLED,
                description = StatisticsTopComponent.PARAM_TOOLTIP_MEDIAN_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_MEDIAN_ENABLED)
        boolean includeMedian = StatisticsTopComponent.PARAM_DEFVAL_MEDIAN_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_MINMAX_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_MINMAX_ENABLED)
        boolean includeMinMax = StatisticsTopComponent.PARAM_DEFVAL_MINMAX_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_PERCENT_THRESHOLDS,
                key = StatisticsTopComponent.PARAM_KEY_PERCENT_THRESHOLDS)
        String percentThresholds = StatisticsTopComponent.PARAM_DEFVAL_PERCENT_THRESHOLDS;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_BINNING_INFO_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_BINNING_INFO_ENABLED)
        boolean includeBinnignInfo = StatisticsTopComponent.PARAM_DEFVAL_BINNING_INFO_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_HISTOGRAM_STATS_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_HISTOGRAM_STATS_ENABLED)
        boolean includeHistogram = StatisticsTopComponent.PARAM_DEFVAL_HISTOGRAM_STATS_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_BAND_METADATA_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_BAND_METADATA_ENABLED)
        boolean includeBandMetadata = StatisticsTopComponent.PARAM_DEFVAL_BAND_METADATA_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_FILE_METADATA_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_FILE_METADATA_ENABLED)
        boolean includeFileMetadata = StatisticsTopComponent.PARAM_DEFVAL_FILE_METADATA_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_MASK_METADATA_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_MASK_METADATA_ENABLED)
        boolean includeMaskMetadata = StatisticsTopComponent.PARAM_DEFVAL_MASK_METADATA_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_PROJECTION_PARAMETERS_METADATA_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_PROJECTION_PARAMETERS_METADATA_ENABLED)
        boolean includeProjectionParameters = StatisticsTopComponent.PARAM_DEFVAL_PROJECTION_PARAMETERS_METADATA_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_TIME_METADATA_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_TIME_METADATA_ENABLED)
        boolean includeTimeMetadata = StatisticsTopComponent.PARAM_DEFVAL_TIME_METADATA_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_TIME_SERIES_METADATA_ENABLED,
                description = StatisticsTopComponent.PARAM_TOOLTIPS_TIME_SERIES_METADATA_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_TIME_SERIES_METADATA_ENABLED)
        boolean includeTimeSeriesMetadata = StatisticsTopComponent.PARAM_DEFVAL_TIME_SERIES_METADATA_ENABLED;

        //Format Options

        @Preference(label = StatisticsTopComponent.PROPERTY_FORMAT_DEFAULT_SECTION_LABEL,
                key = StatisticsTopComponent.PROPERTY_FORMAT_DEFAULT_SECTION_KEY,
                description = StatisticsTopComponent.PROPERTY_FORMAT_DEFAULT_SECTION_TOOLTIP)
        boolean defaultFormatSection = true;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_SPREADSHEET_DECIMAL_PLACES,
                key = StatisticsTopComponent.PARAM_KEY_SPREADSHEET_DECIMAL_PLACES)
        int decimalPlaces = StatisticsTopComponent.PARAM_DEFVAL_SPREADSHEET_DECIMAL_PLACES;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_SPREADSHEET_COL_WIDTH,
                key = StatisticsTopComponent.PARAM_KEY_SPREADSHEET_COL_WIDTH)
        int colCharWidth = StatisticsTopComponent.PARAM_DEFVAL_SPREADSHEET_COL_WIDTH;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_COL_BREAKS_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_COL_BREAKS_ENABLED)
        boolean includeColBreaks = StatisticsTopComponent.PARAM_DEFVAL_COL_BREAKS_ENABLED;

        //Plots Options

        @Preference(label = StatisticsTopComponent.PROPERTY_PLOTS_DEFAULT_SECTION_LABEL,
                key = StatisticsTopComponent.PROPERTY_PLOTS_DEFAULT_SECTION_KEY,
                description = StatisticsTopComponent.PROPERTY_PLOTS_DEFAULT_SECTION_TOOLTIP)
        boolean defaultPlotsSection = true;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_PLOTS_THRESH_DOMAIN_LOW,
                key = StatisticsTopComponent.PARAM_KEY_PLOTS_THRESH_DOMAIN_LOW)
        double plotsThreshDomainLow = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_LOW;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_PLOTS_THRESH_DOMAIN_HIGH,
                key = StatisticsTopComponent.PARAM_KEY_PLOTS_THRESH_DOMAIN_HIGH)
        double plotsThreshDomainHigh = StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_HIGH;

        //View Options

        @Preference(label = StatisticsTopComponent.PROPERTY_VIEW_DEFAULT_SECTION_LABEL,
                key = StatisticsTopComponent.PROPERTY_VIEW_DEFAULT_SECTION_KEY,
                description = StatisticsTopComponent.PROPERTY_VIEW_DEFAULT_SECTION_TOOLTIP)
        boolean defaultViewSection = true;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_HISTOGRAM_PLOT_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_HISTOGRAM_PLOT_ENABLED)
        boolean showHistogramPlots = StatisticsTopComponent.PARAM_DEFVAL_HISTOGRAM_PLOT_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_PERCENT_PLOT_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_PERCENT_PLOT_ENABLED)
        boolean showPercentPlots = StatisticsTopComponent.PARAM_DEFVAL_PERCENT_PLOT_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_STATS_LIST_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_STATS_LIST_ENABLED)
        boolean showStatsList = StatisticsTopComponent.PARAM_DEFVAL_STATS_LIST_ENABLED;

        @Preference(label = StatisticsTopComponent.PARAM_LABEL_STATS_SPREADSHEET_ENABLED,
                key = StatisticsTopComponent.PARAM_KEY_STATS_SPREADSHEET_ENABLED)
        boolean showStatsSpreadSheet = StatisticsTopComponent.PARAM_DEFVAL_STATS_SPREADSHEET_ENABLED;

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

    @Override
    protected JPanel createPanel(BindingContext context) {

//        initPropertyDefaults(context,  PARAM_KEY_RESET_TO_DEFAULTS, false);
        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_BINS_DEFAULT_SECTION_KEY, true);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_NUM_BINS, StatisticsTopComponent.PARAM_DEFVAL_NUM_BINS);

        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_FIELDS_DEFAULT_SECTION_KEY, true);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_TOTAL_PIXEL_COUNT_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_TOTAL_PIXEL_COUNT_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_MEDIAN_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_MEDIAN_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_MINMAX_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_MINMAX_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_PERCENT_THRESHOLDS, StatisticsTopComponent.PARAM_DEFVAL_PERCENT_THRESHOLDS);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_BINNING_INFO_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_BINNING_INFO_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_HISTOGRAM_STATS_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_HISTOGRAM_STATS_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_BAND_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_BAND_METADATA_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_FILE_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_FILE_METADATA_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_MASK_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_MASK_METADATA_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_PROJECTION_PARAMETERS_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_PROJECTION_PARAMETERS_METADATA_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_TIME_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_TIME_METADATA_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_TIME_SERIES_METADATA_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_TIME_SERIES_METADATA_ENABLED);

        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_FORMAT_DEFAULT_SECTION_KEY, true);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_SPREADSHEET_DECIMAL_PLACES, StatisticsTopComponent.PARAM_DEFVAL_SPREADSHEET_DECIMAL_PLACES);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_SPREADSHEET_COL_WIDTH, StatisticsTopComponent.PARAM_DEFVAL_SPREADSHEET_COL_WIDTH);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_COL_BREAKS_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_COL_BREAKS_ENABLED);

        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_PLOTS_DEFAULT_SECTION_KEY, true);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_PLOTS_THRESH_DOMAIN_LOW, StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_LOW);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_PLOTS_THRESH_DOMAIN_HIGH, StatisticsTopComponent.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_HIGH);

        initPropertyDefaults(context, StatisticsTopComponent.PROPERTY_VIEW_DEFAULT_SECTION_KEY, true);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_HISTOGRAM_PLOT_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_HISTOGRAM_PLOT_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_PERCENT_PLOT_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_PERCENT_PLOT_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_STATS_LIST_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_STATS_LIST_ENABLED);
        initPropertyDefaults(context, StatisticsTopComponent.PARAM_KEY_STATS_SPREADSHEET_ENABLED, StatisticsTopComponent.PARAM_DEFVAL_STATS_SPREADSHEET_ENABLED);

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
}