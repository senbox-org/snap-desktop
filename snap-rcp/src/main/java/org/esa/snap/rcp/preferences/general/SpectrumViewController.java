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
import com.bc.ceres.swing.binding.Enablement;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.PropertyPane;
import org.checkerframework.checker.units.qual.C;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.rcp.statistics.StatisticsTopComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;

/**
 * Panel handling general preferences for Spectrum View.
 *
 * @author Daniel Knowles (NASA)
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_SpectrumView=" + "Spectrum View",
        "Options_Keywords_SpectrumView=Spectrum View, general"
})
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_SpectrumView",
        keywords = "#Options_Keywords_SpectrumView",
        keywordsCategory = "Spectrum, spectral",
        id = "spectrumViewController",
        position = 10)


public final class SpectrumViewController extends DefaultConfigController {


    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;

    private static final String PROPERTY_ROOT_KEY = "spectrum.view";

    public static final String PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_KEY = PROPERTY_ROOT_KEY + ".xaxis.set.bounds";
    public static final String PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_LABEL = "Set Bounds X-Axis";
    public static final String PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_TOOLTIP = "Sets bounds of x-axis (uses X-Axis Min and X-Axis Max)";
    public static boolean PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_DEFAULT = false;
    
    public static final String PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_KEY = PROPERTY_ROOT_KEY + ".xaxis.min";
    public static final String PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_LABEL = "X-Axis Min";
    public static final String PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_TOOLTIP = "Sets minimum bound of x-axis (blank entry uses auto-bounding to data)";
    public static String PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_DEFAULT = "";

    public static final String PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_KEY = PROPERTY_ROOT_KEY + ".xaxis.max";
    public static final String PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_LABEL = "X-Axis Max";
    public static final String PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_TOOLTIP = "Sets maximum bound of x-axis (blank entry uses auto-bounding to data)";
    public static String PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_DEFAULT = "";


    public static final String PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_KEY = PROPERTY_ROOT_KEY + ".yaxis.set.bounds";
    public static final String PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_LABEL = "Set Bounds Y-Axis";
    public static final String PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_TOOLTIP = "Sets bounds of y-axis (uses Y-Axis Min and Y-Axis Max)";
    public static boolean PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_DEFAULT = false;
    
    public static final String PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_KEY = PROPERTY_ROOT_KEY + ".yaxis.min";
    public static final String PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_LABEL = "Y-Axis Min";
    public static final String PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_TOOLTIP = "Sets minimum bound of x-axis (blank entry uses auto-bounding to data)";
    public static String PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_DEFAULT = "";

    public static final String PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_KEY = PROPERTY_ROOT_KEY + ".yaxis.max";
    public static final String PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_LABEL = "Y-Axis Max";
    public static final String PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_TOOLTIP = "Sets maximum bound of x-axis (blank entry uses auto-bounding to data)";
    public static String PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_DEFAULT = "";

    public static final String PROPERTY_SPECTRUM_VIEW_TITLE_KEY = PROPERTY_ROOT_KEY + ".plot.title";
    public static final String PROPERTY_SPECTRUM_VIEW_TITLE_LABEL = "Plot Title";
    public static final String PROPERTY_SPECTRUM_VIEW_TITLE_TOOLTIP = "Sets title of plot";
    public static String PROPERTY_SPECTRUM_VIEW_TITLE_DEFAULT = "Spectrum Plot";


//    public static final String PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_KEY = PROPERTY_ROOT_KEY + ".foreground.color";
//    public static final String PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_LABEL = "Foreground Color";
//    public static final String PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_TOOLTIP = "Sets foreground color of the plot";
    public static Color PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_DEFAULT = Color.BLACK;

    public static final String PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_KEY = PROPERTY_ROOT_KEY + "plot.background.color";
    public static final String PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_LABEL = "Plot Background Color";
    public static final String PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_TOOLTIP = "Sets background color of the plot";
    public static Color PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_DEFAULT = Color.WHITE;

//    public static final String PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_KEY = PROPERTY_ROOT_KEY + ".margin.background.color";
//    public static final String PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_LABEL = "Margin Background Color";
//    public static final String PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_TOOLTIP = "Sets background color of the margin";
    public static Color PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_DEFAULT = Color.WHITE;

//    public static final String PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_KEY = PROPERTY_ROOT_KEY + ".legend.background.color";
//    public static final String PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_LABEL = "Legend Background Color";
//    public static final String PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_TOOLTIP = "Sets background color of the legend";
    public static Color PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_DEFAULT = Color.WHITE;

    
    public static final String PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_KEY = PROPERTY_ROOT_KEY + ".gridlines.color";
    public static final String PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_LABEL = "Plot Gridlines Color";
    public static final String PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_TOOLTIP = "Sets color of the plot gridlines";
    public static Color PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_DEFAULT = Color.LIGHT_GRAY;

    public static final String PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_KEY = PROPERTY_ROOT_KEY + ".gridlines.show";
    public static final String PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_LABEL = "Show Gridlines";
    public static final String PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_TOOLTIP = "show plot gridlines";
    public static boolean PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_DEFAULT = true;

    // Restore to defaults


    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Restore Defaults (Spectrum View Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all RGB Image preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;




    protected PropertySet createPropertySet() {
        return createPropertySet(new GeneralLayerBean());
    }


    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("colorManipulationPreferences");
    }

    @Override
    protected JPanel createPanel(BindingContext context) {


        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_KEY, PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_DEFAULT);
        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_KEY, PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_DEFAULT);
        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_KEY, PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_DEFAULT);

        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_KEY, PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_DEFAULT);
        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_KEY, PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_DEFAULT);
        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_KEY, PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_DEFAULT);
        
        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_TITLE_KEY, PROPERTY_SPECTRUM_VIEW_TITLE_DEFAULT);

//        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_KEY, PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_KEY, PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_DEFAULT);
//        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_KEY, PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_DEFAULT);
//        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_KEY, PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_KEY, PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_KEY, PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_DEFAULT);

        
        restoreDefaults =  initPropertyDefaults(context, PROPERTY_RESTORE_DEFAULTS_NAME, PROPERTY_RESTORE_DEFAULTS_DEFAULT);




        //
        // Create UI
        //

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










    @SuppressWarnings("UnusedDeclaration")
    static class GeneralLayerBean {
        
        @Preference(label = PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_LABEL,
                key = PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_KEY,
                description = PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_TOOLTIP)
        boolean spectrumViewXaxisSetBoundsDefault = PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_DEFAULT;

        @Preference(label = PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_LABEL,
                key = PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_KEY,
                description = PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_TOOLTIP)
        String spectrumViewXaxisMinDefault = PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_DEFAULT;

        @Preference(label = PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_LABEL,
                key = PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_KEY,
                description = PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_TOOLTIP)
        String spectrumViewXaxisMaxDefault = PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_DEFAULT;

        @Preference(label = PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_LABEL,
                key = PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_KEY,
                description = PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_TOOLTIP)
        boolean spectrumViewYaxisSetBoundsDefault = PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_DEFAULT;

        @Preference(label = PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_LABEL,
                key = PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_KEY,
                description = PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_TOOLTIP)
        String spectrumViewYaxisMinDefault = PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_DEFAULT;

        @Preference(label = PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_LABEL,
                key = PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_KEY,
                description = PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_TOOLTIP)
        String spectrumViewYaxisMaxDefault = PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_DEFAULT;

        @Preference(label = PROPERTY_SPECTRUM_VIEW_TITLE_LABEL,
                key = PROPERTY_SPECTRUM_VIEW_TITLE_KEY,
                description = PROPERTY_SPECTRUM_VIEW_TITLE_TOOLTIP)
        String spectrumViewTitleDefault = PROPERTY_SPECTRUM_VIEW_TITLE_DEFAULT;

        
//        @Preference(label = PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_LABEL,
//                key = PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_KEY,
//                description = PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_TOOLTIP)
//        Color spectrumViewForegroundColorDefault = PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_DEFAULT;

        @Preference(label = PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_LABEL,
                key = PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_KEY,
                description = PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_TOOLTIP)
        Color spectrumViewBackgroundColorDefault = PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_DEFAULT;

//        @Preference(label = PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_LABEL,
//                key = PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_KEY,
//                description = PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_TOOLTIP)
//        Color spectrumViewMarginBackgroundColorDefault = PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_DEFAULT;
//
//        @Preference(label = PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_LABEL,
//                key = PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_KEY,
//                description = PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_TOOLTIP)
//        Color spectrumViewLegendBackgroundColorDefault = PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_DEFAULT;

        @Preference(label = PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_LABEL,
                key = PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_KEY,
                description = PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_TOOLTIP)
        Color spectrumViewGridlineColorDefault = PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_DEFAULT;


        @Preference(label = PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_LABEL,
                key = PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_KEY,
                description = PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_TOOLTIP)
        boolean spectrumViewGridlineShowDefault = PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_DEFAULT;




        
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



    public static boolean getPreferenceXaxisSetBounds() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_KEY, PROPERTY_SPECTRUM_VIEW_XAXIS_SET_BOUNDS_DEFAULT);
    }

    public static String getPreferenceXaxisMin() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_KEY, PROPERTY_SPECTRUM_VIEW_XAXIS_MIN_DEFAULT);
    }

    public static String getPreferenceXaxisMax() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_KEY, PROPERTY_SPECTRUM_VIEW_XAXIS_MAX_DEFAULT);
    }


    public static boolean getPreferenceYaxisSetBounds() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_KEY, PROPERTY_SPECTRUM_VIEW_YAXIS_SET_BOUNDS_DEFAULT);
    }

    public static String getPreferenceYaxisMin() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_KEY, PROPERTY_SPECTRUM_VIEW_YAXIS_MIN_DEFAULT);
    }

    public static String getPreferenceYaxisMax() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_KEY, PROPERTY_SPECTRUM_VIEW_YAXIS_MAX_DEFAULT);
    }


    public static String getPreferenceTitle() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_SPECTRUM_VIEW_TITLE_KEY, PROPERTY_SPECTRUM_VIEW_TITLE_DEFAULT);
    }



//    public static Color getPreferenceForegroundColor() {
//        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
//        return preferences.getPropertyColor(PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_KEY, PROPERTY_SPECTRUM_VIEW_FOREGROUND_COLOR_DEFAULT);
//    }

    public static Color getPreferenceBackgroundColor() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_KEY, PROPERTY_SPECTRUM_VIEW_BACKGROUND_COLOR_DEFAULT);
    }

//    public static Color getPreferenceMarginBackgroundColor() {
//        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
//        return preferences.getPropertyColor(PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_KEY, PROPERTY_SPECTRUM_VIEW_MARGIN_BACKGROUND_COLOR_DEFAULT);
//    }
//
//    public static Color getPreferenceLegendBackgroundColor() {
//        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
//        return preferences.getPropertyColor(PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_KEY, PROPERTY_SPECTRUM_VIEW_LEGEND_BACKGROUND_COLOR_DEFAULT);
//    }



    public static Color getPreferenceGridlinesColor() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_KEY, PROPERTY_SPECTRUM_VIEW_GRIDLINE_COLOR_DEFAULT);
    }

    public static boolean getPreferenceGridlinesShow() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_KEY, PROPERTY_SPECTRUM_VIEW_GRIDLINE_SHOW_DEFAULT);
    }
}
