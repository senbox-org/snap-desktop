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

package org.esa.snap.rcp.preferences.layer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.core.layer.GraticuleLayerType;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * * Panel handling graticule layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author thomas
 * @author Daniel Knowles
 */
//JAN2018 - Daniel Knowles - updated with SeaDAS gridline revisions


@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerGraticule",
        keywords = "#Options_Keywords_LayerGraticule",
        keywordsCategory = "Layer",
        id = "LayerGraticule")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerGraticule=Map Gridlines Layer",
        "Options_Keywords_LayerGraticule=layer, graticule"
})
public final class GraticuleLayerController extends DefaultConfigController {

    Property restoreDefaults;

    Enablement enablementGridlinesWidth;
    Enablement enablementGridlinesDashedPhase;
    Enablement enablementGridlinesTransparency;
    Enablement enablementGridlinesColor;

    Enablement enablementTickmarksInside;
    Enablement enablementTickmarksLength;
    Enablement enablementTickmarksColor;

    Enablement enablementBorderWidth;
    Enablement enablementBorderColor;

    boolean propertyValueChangeEventsEnabled = true;


    protected PropertySet createPropertySet() {
        return createPropertySet(new GraticuleBean());
    }



    @Override
    protected JPanel createPanel(BindingContext context) {

        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        ArrayList<String> fontNames = new ArrayList<String>();
        for (Font font: fonts) {
            font.getName();
            if (font.getName() != null && font.getName().length() > 0) {
                fontNames.add(font.getName());
            }
        }
        String[] fontNameArray = new String[fontNames.size()];
        fontNameArray =  fontNames.toArray(fontNameArray);

        try {
            Property fontNameProperty = context.getPropertySet().getProperty(GraticuleLayerType.PROPERTY_LABELS_FONT_NAME);
            fontNameProperty.getDescriptor().setDefaultValue(null);
            fontNameProperty.getDescriptor().setValueSet(new ValueSet(fontNameArray));
            fontNameProperty.getDescriptor().setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_FONT_DEFAULT);
        } catch (Exception e) {
        }


        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_GRID_SPACING_SECTION_NAME, true);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_NAME, GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_GRID_SPACING_LON_NAME, GraticuleLayerType.PROPERTY_GRID_SPACING_LON_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_NUM_GRID_LINES_NAME, GraticuleLayerType.PROPERTY_NUM_GRID_LINES_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_MINOR_STEPS_NAME, GraticuleLayerType.PROPERTY_MINOR_STEPS_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_INTERPOLATE_KEY, GraticuleLayerType.PROPERTY_INTERPOLATE_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_TOLERANCE_KEY, GraticuleLayerType.PROPERTY_TOLERANCE_DEFAULT);

        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_SECTION_NAME, true);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_NORTH_NAME, GraticuleLayerType.PROPERTY_LABELS_NORTH_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_SOUTH_NAME, GraticuleLayerType.PROPERTY_LABELS_SOUTH_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_WEST_NAME, GraticuleLayerType.PROPERTY_LABELS_WEST_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_EAST_NAME, GraticuleLayerType.PROPERTY_LABELS_EAST_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_NAME, GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_NAME, GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME, GraticuleLayerType.PROPERTY_LABELS_INSIDE_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_ITALIC_NAME, GraticuleLayerType.PROPERTY_LABELS_ITALIC_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_BOLD_NAME, GraticuleLayerType.PROPERTY_LABELS_BOLD_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_FONT_NAME, GraticuleLayerType.PROPERTY_LABELS_FONT_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_NAME, GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_NAME, GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_SIZE_NAME, GraticuleLayerType.PROPERTY_LABELS_SIZE_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_NAME, GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_LABELS_COLOR_NAME, GraticuleLayerType.PROPERTY_LABELS_COLOR_DEFAULT);

        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_GRIDLINES_SECTION_NAME, true);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_NAME, GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_NAME, GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_NAME, GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_NAME, GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_DEFAULT);

        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_BORDER_SECTION_NAME, true);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_BORDER_SHOW_NAME, GraticuleLayerType.PROPERTY_BORDER_SHOW_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_BORDER_WIDTH_NAME, GraticuleLayerType.PROPERTY_BORDER_WIDTH_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_BORDER_COLOR_NAME, GraticuleLayerType.PROPERTY_BORDER_COLOR_DEFAULT);

        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_TICKMARKS_SECTION_NAME, true);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME, GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_NAME, GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_NAME, GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_NAME, GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);

        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_CORNER_LABELS_SECTION_NAME, true);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_NAME, GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_NAME, GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_NAME, GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_NAME, GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_DEFAULT);

        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_INSIDE_LABELS_SECTION_NAME, true);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_NAME, GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_NAME, GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_DEFAULT);

        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_FLIP_WARNING_SECTION_KEY, true);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_FLIP_WARNING_ENABLE_KEY, GraticuleLayerType.PROPERTY_FLIP_WARNING_ENABLE_DEFAULT);
        initPropertyDefaults(context, GraticuleLayerType.PROPERTY_FLIP_WARNING_COLOR_KEY, GraticuleLayerType.PROPERTY_FLIP_WARNING_COLOR_DEFAULT);

        restoreDefaults =  initPropertyDefaults(context, GraticuleLayerType.PROPERTY_RESTORE_DEFAULTS_NAME, GraticuleLayerType.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT);


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

        configureGridlinesEnablement(context);
        configureTickmarksEnablement(context);
        configureBorderEnablement(context);


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

            context.setComponentsEnabled(GraticuleLayerType.PROPERTY_RESTORE_DEFAULTS_NAME, false);
        }
    }


    /**
     * Configure enablement of the tickmarks components
     *
     * @param context
     * @author Daniel Knowles
     */
    private void configureTickmarksEnablement(BindingContext context) {
        enablementTickmarksInside = context.bindEnabledState(GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_NAME, true,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME, true);

        enablementTickmarksLength = context.bindEnabledState(GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_NAME, true,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME, true);

        enablementTickmarksColor = context.bindEnabledState(GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_NAME, true,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME, true);


        // handle it the first time so bound properties get properly enabled
        handleTickmarksEnablement();
    }


    /**
     * Handles enablement of the tickmarks components
     *
     * @author Daniel Knowles
     */
    private void handleTickmarksEnablement() {
        enablementTickmarksInside.apply();
        enablementTickmarksLength.apply();
        enablementTickmarksColor.apply();
    }


    /**
     * Configure enablement of the gridlines components
     *
     * @param context
     * @author Daniel Knowles
     */
    private void configureGridlinesEnablement(BindingContext context) {
        enablementGridlinesWidth = context.bindEnabledState(GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_NAME, true,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, true);

        enablementGridlinesDashedPhase = context.bindEnabledState(GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_NAME, true,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, true);

        enablementGridlinesTransparency = context.bindEnabledState(GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_NAME, true,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, true);

        enablementGridlinesColor = context.bindEnabledState(GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_NAME, true,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, true);

        // handle it the first time so bound properties get properly enabled
        handleGridlinesEnablement();
    }

    /**
     * Handles enablement of the gridlines components
     *
     * @author Daniel Knowles
     */
    private void handleGridlinesEnablement() {
        enablementGridlinesWidth.apply();
        enablementGridlinesDashedPhase.apply();
        enablementGridlinesTransparency.apply();
        enablementGridlinesColor.apply();
    }


    /**
     * Configure enablement of the border components
     *
     * @param context
     * @author Daniel Knowles
     */
    private void configureBorderEnablement(BindingContext context) {

        enablementBorderWidth = context.bindEnabledState(GraticuleLayerType.PROPERTY_BORDER_WIDTH_NAME, true,
                GraticuleLayerType.PROPERTY_BORDER_SHOW_NAME, true);

        enablementBorderColor = context.bindEnabledState(GraticuleLayerType.PROPERTY_BORDER_COLOR_NAME, true,
                GraticuleLayerType.PROPERTY_BORDER_SHOW_NAME, true);


        // handle it the first time so bound properties get properly enabled
        handleBorderEnablement();
    }

    /**
     * Handles enablement of the gridlines components
     *
     * @author Daniel Knowles
     */
    private void handleBorderEnablement() {
        enablementBorderWidth.apply();
        enablementBorderColor.apply();
        enablementGridlinesTransparency.apply();
        enablementGridlinesColor.apply();
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
                context.setComponentsEnabled(GraticuleLayerType.PROPERTY_RESTORE_DEFAULTS_NAME, !isDefaults(context));
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


    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("options-graticulelayer");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class GraticuleBean {

        // Grid Spacing Section

        @Preference(label = GraticuleLayerType.PROPERTY_GRID_SPACING_SECTION_LABEL,
                key = GraticuleLayerType.PROPERTY_GRID_SPACING_SECTION_NAME,
                description = GraticuleLayerType.PROPERTY_GRID_SPACING_SECTION_TOOLTIP)
        boolean gridSpacingSection = true;

        @Preference(label = GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_LABEL,
                key = GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_NAME,
                description = GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_TOOLTIP,
                interval = "[0.00,90.0]")
        double gridSpacingLat = GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_GRID_SPACING_LON_LABEL,
                key = GraticuleLayerType.PROPERTY_GRID_SPACING_LON_NAME,
                description = GraticuleLayerType.PROPERTY_GRID_SPACING_LON_TOOLTIP,
                interval = "[0.00,90.0]")
        double gridSpacingLon = GraticuleLayerType.PROPERTY_GRID_SPACING_LON_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_NUM_GRID_LINES_LABEL,
                key = GraticuleLayerType.PROPERTY_NUM_GRID_LINES_NAME,
                description = GraticuleLayerType.PROPERTY_NUM_GRID_LINES_TOOLTIP,
                interval = "[2,40]")
        int numGridLines = GraticuleLayerType.PROPERTY_NUM_GRID_LINES_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_MINOR_STEPS_LABEL,
                key = GraticuleLayerType.PROPERTY_MINOR_STEPS_NAME,
                description = GraticuleLayerType.PROPERTY_MINOR_STEPS_TOOLTIP,
                interval = "[0,1000]")
        int minorSteps = GraticuleLayerType.PROPERTY_MINOR_STEPS_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_INTERPOLATE_LABEL,
                key = GraticuleLayerType.PROPERTY_INTERPOLATE_KEY,
                description = GraticuleLayerType.PROPERTY_INTERPOLATE_TOOLTIP)
        boolean interpolateDefault = GraticuleLayerType.PROPERTY_INTERPOLATE_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_TOLERANCE_LABEL,
                key = GraticuleLayerType.PROPERTY_TOLERANCE_KEY,
                description = GraticuleLayerType.PROPERTY_TOLERANCE_TOOLTIP,
                interval = "[0,100]")
        double toleranceDefault = GraticuleLayerType.PROPERTY_TOLERANCE_DEFAULT;

        // Labels Section

        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_SECTION_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_SECTION_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_SECTION_TOOLTIP)
        boolean labelsSection = true;

        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_TOOLTIP,
                interval = "[0.00,90.0]")
        double labelsRotationLat = GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_TOOLTIP,
                interval = "[0.00,90.0]")
        double labelsRotationLon = GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_DEFAULT;


        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_TOOLTIP)
        boolean labelsSuffix = GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_TOOLTIP)
        boolean labelsDecimal = GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_DEFAULT;


        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_NORTH_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_NORTH_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_NORTH_TOOLTIP)
        boolean labelsNorth = GraticuleLayerType.PROPERTY_LABELS_NORTH_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_SOUTH_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_SOUTH_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_SOUTH_TOOLTIP)
        boolean labelsSouth = GraticuleLayerType.PROPERTY_LABELS_SOUTH_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_WEST_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_WEST_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_WEST_TOOLTIP)
        boolean labelsWest = GraticuleLayerType.PROPERTY_LABELS_WEST_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_EAST_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_EAST_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_EAST_TOOLTIP)
        boolean labelsEast = GraticuleLayerType.PROPERTY_LABELS_EAST_DEFAULT;



        @Preference(label = GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_LABEL,
                key = GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_NAME,
                description = GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_TOOLTIP)
        boolean cornerLabelsNorth = GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_LABEL,
                key = GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_NAME,
                description = GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_TOOLTIP)
        boolean cornerLabelsSouth = GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_LABEL,
                key = GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_NAME,
                description = GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_TOOLTIP)
        boolean cornerLabelsWest = GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_LABEL,
                key = GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_NAME,
                description = GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_TOOLTIP)
        boolean cornerLabelsEast = GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_INSIDE_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_INSIDE_TOOLTIP)
        boolean labelsInside = GraticuleLayerType.PROPERTY_LABELS_INSIDE_DEFAULT;






        @Preference(label = GraticuleLayerType.PROPERTY_CORNER_LABELS_SECTION_LABEL,
                key = GraticuleLayerType.PROPERTY_CORNER_LABELS_SECTION_NAME,
                description = GraticuleLayerType.PROPERTY_CORNER_LABELS_SECTION_TOOLTIP)
        boolean cornerLabelsSection = true;




        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_SIZE_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_SIZE_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_SIZE_TOOLTIP,
                interval = GraticuleLayerType.PROPERTY_LABELS_SIZE_INTERVAL)
        int labelsSize = GraticuleLayerType.PROPERTY_LABELS_SIZE_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_LABEL,
                key = GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_NAME,
                description = GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_TOOLTIP,
                interval = GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_INTERVAL)
        int edgeLabelsSpacerDefault = GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_DEFAULT;


        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_COLOR_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_COLOR_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_COLOR_TOOLTIP)
        Color labelsColor = GraticuleLayerType.PROPERTY_LABELS_COLOR_DEFAULT;


        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_FONT_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_FONT_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_FONT_TOOLTIP,
                valueSet = {GraticuleLayerType.PROPERTY_LABELS_FONT_VALUE_1,
                        GraticuleLayerType.PROPERTY_LABELS_FONT_VALUE_2,
                        GraticuleLayerType.PROPERTY_LABELS_FONT_VALUE_3,
                        GraticuleLayerType.PROPERTY_LABELS_FONT_VALUE_4})
        String labelsFont = GraticuleLayerType.PROPERTY_LABELS_FONT_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_ITALIC_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_ITALIC_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_ITALIC_TOOLTIP)
        boolean labelsItalic = GraticuleLayerType.PROPERTY_LABELS_ITALIC_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_LABELS_BOLD_LABEL,
                key = GraticuleLayerType.PROPERTY_LABELS_BOLD_NAME,
                description = GraticuleLayerType.PROPERTY_LABELS_BOLD_TOOLTIP)
        boolean labelsBold = GraticuleLayerType.PROPERTY_LABELS_BOLD_DEFAULT;




        // Gridlines Section

        @Preference(label = GraticuleLayerType.PROPERTY_GRIDLINES_SECTION_LABEL,
                key = GraticuleLayerType.PROPERTY_GRIDLINES_SECTION_NAME,
                description = GraticuleLayerType.PROPERTY_GRIDLINES_SECTION_TOOLTIP)
        boolean gridlinesSection = true;

        @Preference(label = GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_LABEL,
                key = GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME,
                description = GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_TOOLTIP)
        boolean gridlinesShow = GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_LABEL,
                key = GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_NAME,
                description = GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_TOOLTIP)
        double gridlinesWidth = GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_LABEL,
                key = GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_NAME,
                description = GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_TOOLTIP)
        double gridlinesDashed = GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_LABEL,
                key = GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_NAME,
                description = GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_TOOLTIP,
                interval = "[0.0,1.0]")
        double gridlinesTransparency = GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_LABEL,
                key = GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_NAME,
                description = GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_TOOLTIP)
        Color gridlinesColor = GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_DEFAULT;


        // Border Section

        @Preference(label = GraticuleLayerType.PROPERTY_BORDER_SECTION_LABEL,
                key = GraticuleLayerType.PROPERTY_BORDER_SECTION_NAME,
                description = GraticuleLayerType.PROPERTY_BORDER_SECTION_TOOLTIP)
        boolean borderSection = true;

        @Preference(label = GraticuleLayerType.PROPERTY_BORDER_SHOW_LABEL,
                key = GraticuleLayerType.PROPERTY_BORDER_SHOW_NAME,
                description = GraticuleLayerType.PROPERTY_BORDER_SHOW_TOOLTIP)
        boolean borderShow = GraticuleLayerType.PROPERTY_BORDER_SHOW_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_BORDER_WIDTH_LABEL,
                key = GraticuleLayerType.PROPERTY_BORDER_WIDTH_NAME,
                description = GraticuleLayerType.PROPERTY_BORDER_WIDTH_TOOLTIP)
        double borderWidth = GraticuleLayerType.PROPERTY_BORDER_WIDTH_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_BORDER_COLOR_LABEL,
                key = GraticuleLayerType.PROPERTY_BORDER_COLOR_NAME,
                description = GraticuleLayerType.PROPERTY_BORDER_COLOR_TOOLTIP)
        Color borderColor = GraticuleLayerType.PROPERTY_BORDER_COLOR_DEFAULT;


        // Tickmarks Section

        @Preference(label = GraticuleLayerType.PROPERTY_TICKMARKS_SECTION_LABEL,
                key = GraticuleLayerType.PROPERTY_TICKMARKS_SECTION_NAME,
                description = GraticuleLayerType.PROPERTY_TICKMARKS_SECTION_TOOLTIP)
        boolean tickmarksSection = true;

        @Preference(label = GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_LABEL,
                key = GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME,
                description = GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_TOOLTIP)
        boolean tickmarksShow = GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_LABEL,
                key = GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_NAME,
                description = GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_TOOLTIP)
        boolean tickmarkInside = GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_LABEL,
                key = GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_NAME,
                description = GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_TOOLTIP)
        double tickmarksLength = GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_LABEL,
                key = GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_NAME,
                description = GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_TOOLTIP)
        Color tickmarksColor = GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT;




        // Inside Labels Section

        @Preference(label = GraticuleLayerType.PROPERTY_INSIDE_LABELS_SECTION_LABEL,
                key = GraticuleLayerType.PROPERTY_INSIDE_LABELS_SECTION_NAME,
                description = GraticuleLayerType.PROPERTY_INSIDE_LABELS_SECTION_TOOLTIP)
        boolean insideLabelsSection = true;

        @Preference(label = GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_LABEL,
                key = GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_NAME,
                description = GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_TOOLTIP,
                interval = "[0.0,1.0]")
        double insideLabelsBgTransparency = GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_LABEL,
                key = GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_NAME,
                description = GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_TOOLTIP)
        Color insideLabelsBgColor = GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_DEFAULT;



        // Flip Warning Section

        @Preference(label = GraticuleLayerType.PROPERTY_FLIP_WARNING_SECTION_LABEL,
                key = GraticuleLayerType.PROPERTY_FLIP_WARNING_SECTION_KEY,
                description = GraticuleLayerType.PROPERTY_FLIP_WARNING_SECTION_TOOLTIP)
        boolean flipWarningSection = true;

        @Preference(label = GraticuleLayerType.PROPERTY_FLIP_WARNING_ENABLE_LABEL,
                key = GraticuleLayerType.PROPERTY_FLIP_WARNING_ENABLE_KEY,
                description = GraticuleLayerType.PROPERTY_FLIP_WARNING_ENABLE_TOOLTIP)
        boolean flipWarningEnableDefault = GraticuleLayerType.PROPERTY_FLIP_WARNING_ENABLE_DEFAULT;

        @Preference(label = GraticuleLayerType.PROPERTY_FLIP_WARNING_COLOR_LABEL,
                key = GraticuleLayerType.PROPERTY_FLIP_WARNING_COLOR_KEY,
                description = GraticuleLayerType.PROPERTY_FLIP_WARNING_COLOR_TOOLTIP)
        Color flipWarningColorDefault = GraticuleLayerType.PROPERTY_FLIP_WARNING_COLOR_DEFAULT;


        // Restore Defaults Section

        @Preference(label = GraticuleLayerType.PROPERTY_RESTORE_TO_DEFAULTS_LABEL,
                key = GraticuleLayerType.PROPERTY_RESTORE_DEFAULTS_NAME,
                description = GraticuleLayerType.PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = GraticuleLayerType.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT;

    }

}
