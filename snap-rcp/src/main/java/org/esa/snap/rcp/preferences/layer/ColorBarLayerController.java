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
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.core.layer.ColorBarLayerType;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;

/**
 * * Panel handling colorbar layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author thomas
 * @author Daniel Knowles
 */
//JAN2018 - Daniel Knowles - updated with SeaDAS gridline revisions


@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerColorBar",
        keywords = "#Options_Keywords_LayerColorBar",
        keywordsCategory = "Layer",
        id = "LayerColorBar")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerColorBar=ColorBar Layer",
        "Options_Keywords_LayerColorBar=layer, colorbar"
})
public final class ColorBarLayerController extends DefaultConfigController {

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
        return createPropertySet(new ColorBarBean());
    }



    @Override
    protected JPanel createPanel(BindingContext context) {

        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_GRID_SPACING_SECTION_NAME, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_GRID_SPACING_LAT_NAME, ColorBarLayerType.PROPERTY_GRID_SPACING_LAT_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_GRID_SPACING_LON_NAME, ColorBarLayerType.PROPERTY_GRID_SPACING_LON_DEFAULT);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_SECTION_NAME, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_NORTH_NAME, ColorBarLayerType.PROPERTY_LABELS_NORTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_SOUTH_NAME, ColorBarLayerType.PROPERTY_LABELS_SOUTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_WEST_NAME, ColorBarLayerType.PROPERTY_LABELS_WEST_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_EAST_NAME, ColorBarLayerType.PROPERTY_LABELS_EAST_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_SUFFIX_NSWE_NAME, ColorBarLayerType.PROPERTY_LABELS_SUFFIX_NSWE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_DECIMAL_VALUE_NAME, ColorBarLayerType.PROPERTY_LABELS_DECIMAL_VALUE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_INSIDE_NAME, ColorBarLayerType.PROPERTY_LABELS_INSIDE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_ITALIC_NAME, ColorBarLayerType.PROPERTY_LABELS_ITALIC_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_BOLD_NAME, ColorBarLayerType.PROPERTY_LABELS_BOLD_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_NAME, ColorBarLayerType.PROPERTY_LABELS_FONT_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_ROTATION_LON_NAME, ColorBarLayerType.PROPERTY_LABELS_ROTATION_LON_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_ROTATION_LAT_NAME, ColorBarLayerType.PROPERTY_LABELS_ROTATION_LAT_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_SIZE_NAME, ColorBarLayerType.PROPERTY_LABELS_SIZE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_COLOR_NAME, ColorBarLayerType.PROPERTY_LABELS_COLOR_DEFAULT);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_GRIDLINES_SECTION_NAME, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_GRIDLINES_SHOW_NAME, ColorBarLayerType.PROPERTY_GRIDLINES_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_GRIDLINES_WIDTH_NAME, ColorBarLayerType.PROPERTY_GRIDLINES_WIDTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_NAME, ColorBarLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_NAME, ColorBarLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_GRIDLINES_COLOR_NAME, ColorBarLayerType.PROPERTY_GRIDLINES_COLOR_DEFAULT);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BORDER_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY, ColorBarLayerType.PROPERTY_BORDER_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_BORDER_WIDTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BORDER_COLOR_KEY, ColorBarLayerType.PROPERTY_BORDER_COLOR_DEFAULT);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_INSIDE_NAME, ColorBarLayerType.PROPERTY_TICKMARKS_INSIDE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_CORNER_LABELS_SECTION_NAME, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_CORNER_LABELS_NORTH_NAME, ColorBarLayerType.PROPERTY_CORNER_LABELS_NORTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_CORNER_LABELS_SOUTH_NAME, ColorBarLayerType.PROPERTY_CORNER_LABELS_SOUTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_CORNER_LABELS_WEST_NAME, ColorBarLayerType.PROPERTY_CORNER_LABELS_WEST_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_CORNER_LABELS_EAST_NAME, ColorBarLayerType.PROPERTY_CORNER_LABELS_EAST_DEFAULT);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_INSIDE_LABELS_SECTION_NAME, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_NAME, ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_NAME, ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_DEFAULT);

        restoreDefaults =  initPropertyDefaults(context, ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_NAME, ColorBarLayerType.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT);


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

            context.setComponentsEnabled(ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_NAME, false);
        }
    }


    /**
     * Configure enablement of the tickmarks components
     *
     * @param context
     * @author Daniel Knowles
     */
    private void configureTickmarksEnablement(BindingContext context) {
        enablementTickmarksInside = context.bindEnabledState(ColorBarLayerType.PROPERTY_TICKMARKS_INSIDE_NAME, true,
                ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, true);

        enablementTickmarksLength = context.bindEnabledState(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY, true,
                ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, true);

        enablementTickmarksColor = context.bindEnabledState(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY, true,
                ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, true);


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
        enablementGridlinesWidth = context.bindEnabledState(ColorBarLayerType.PROPERTY_GRIDLINES_WIDTH_NAME, true,
                ColorBarLayerType.PROPERTY_GRIDLINES_SHOW_NAME, true);

        enablementGridlinesDashedPhase = context.bindEnabledState(ColorBarLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_NAME, true,
                ColorBarLayerType.PROPERTY_GRIDLINES_SHOW_NAME, true);

        enablementGridlinesTransparency = context.bindEnabledState(ColorBarLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_NAME, true,
                ColorBarLayerType.PROPERTY_GRIDLINES_SHOW_NAME, true);

        enablementGridlinesColor = context.bindEnabledState(ColorBarLayerType.PROPERTY_GRIDLINES_COLOR_NAME, true,
                ColorBarLayerType.PROPERTY_GRIDLINES_SHOW_NAME, true);

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

        enablementBorderWidth = context.bindEnabledState(ColorBarLayerType.PROPERTY_BORDER_WIDTH_KEY, true,
                ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY, true);

        enablementBorderColor = context.bindEnabledState(ColorBarLayerType.PROPERTY_BORDER_COLOR_KEY, true,
                ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY, true);


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
                context.setComponentsEnabled(ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_NAME, !isDefaults(context));
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
        return new HelpCtx("layer");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class ColorBarBean {

        // Grid Spacing Section

        @Preference(label = ColorBarLayerType.PROPERTY_GRID_SPACING_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_GRID_SPACING_SECTION_NAME,
                description = ColorBarLayerType.PROPERTY_GRID_SPACING_SECTION_TOOLTIP)
        boolean gridSpacingSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_GRID_SPACING_LAT_LABEL,
                key = ColorBarLayerType.PROPERTY_GRID_SPACING_LAT_NAME,
                description = ColorBarLayerType.PROPERTY_GRID_SPACING_LAT_TOOLTIP,
                interval = "[0.00,90.0]")
        double gridSpacingLat = ColorBarLayerType.PROPERTY_GRID_SPACING_LAT_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_GRID_SPACING_LON_LABEL,
                key = ColorBarLayerType.PROPERTY_GRID_SPACING_LON_NAME,
                description = ColorBarLayerType.PROPERTY_GRID_SPACING_LON_TOOLTIP,
                interval = "[0.00,90.0]")
        double gridSpacingLon = ColorBarLayerType.PROPERTY_GRID_SPACING_LON_DEFAULT;


        // Labels Section

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_SECTION_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_SECTION_TOOLTIP)
        boolean labelsSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_NORTH_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_NORTH_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_NORTH_TOOLTIP)
        boolean labelsNorth = ColorBarLayerType.PROPERTY_LABELS_NORTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_SOUTH_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_SOUTH_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_SOUTH_TOOLTIP)
        boolean labelsSouth = ColorBarLayerType.PROPERTY_LABELS_SOUTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_WEST_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_WEST_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_WEST_TOOLTIP)
        boolean labelsWest = ColorBarLayerType.PROPERTY_LABELS_WEST_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_EAST_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_EAST_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_EAST_TOOLTIP)
        boolean labelsEast = ColorBarLayerType.PROPERTY_LABELS_EAST_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_SUFFIX_NSWE_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_SUFFIX_NSWE_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_SUFFIX_NSWE_TOOLTIP)
        boolean labelsSuffix = ColorBarLayerType.PROPERTY_LABELS_SUFFIX_NSWE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_DECIMAL_VALUE_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_DECIMAL_VALUE_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_DECIMAL_VALUE_TOOLTIP)
        boolean labelsDecimal = ColorBarLayerType.PROPERTY_LABELS_DECIMAL_VALUE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_INSIDE_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_INSIDE_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_INSIDE_TOOLTIP)
        boolean labelsInside = ColorBarLayerType.PROPERTY_LABELS_INSIDE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_ITALIC_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_ITALIC_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_ITALIC_TOOLTIP)
        boolean labelsItalic = ColorBarLayerType.PROPERTY_LABELS_ITALIC_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_BOLD_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_BOLD_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_BOLD_TOOLTIP)
        boolean labelsBold = ColorBarLayerType.PROPERTY_LABELS_BOLD_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_FONT_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_FONT_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_FONT_TOOLTIP,
                valueSet = {ColorBarLayerType.PROPERTY_LABELS_FONT_VALUE_1,
                        ColorBarLayerType.PROPERTY_LABELS_FONT_VALUE_2,
                        ColorBarLayerType.PROPERTY_LABELS_FONT_VALUE_3,
                        ColorBarLayerType.PROPERTY_LABELS_FONT_VALUE_4})
        String labelsFont = ColorBarLayerType.PROPERTY_LABELS_FONT_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_ROTATION_LON_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_ROTATION_LON_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_ROTATION_LON_TOOLTIP,
                interval = "[0.00,90.0]")
        double labelsRotationLon = ColorBarLayerType.PROPERTY_LABELS_ROTATION_LON_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_ROTATION_LAT_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_ROTATION_LAT_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_ROTATION_LAT_TOOLTIP,
                interval = "[0.00,90.0]")
        double labelsRotationLat = ColorBarLayerType.PROPERTY_LABELS_ROTATION_LAT_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_SIZE_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_SIZE_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_SIZE_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_LABELS_SIZE_INTERVAL)
        int labelsSize = ColorBarLayerType.PROPERTY_LABELS_SIZE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_COLOR_NAME,
                description = ColorBarLayerType.PROPERTY_LABELS_COLOR_TOOLTIP)
        Color labelsColor = ColorBarLayerType.PROPERTY_LABELS_COLOR_DEFAULT;


        // Gridlines Section

        @Preference(label = ColorBarLayerType.PROPERTY_GRIDLINES_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_GRIDLINES_SECTION_NAME,
                description = ColorBarLayerType.PROPERTY_GRIDLINES_SECTION_TOOLTIP)
        boolean gridlinesSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_GRIDLINES_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_GRIDLINES_SHOW_NAME,
                description = ColorBarLayerType.PROPERTY_GRIDLINES_SHOW_TOOLTIP)
        boolean gridlinesShow = ColorBarLayerType.PROPERTY_GRIDLINES_SHOW_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_GRIDLINES_WIDTH_LABEL,
                key = ColorBarLayerType.PROPERTY_GRIDLINES_WIDTH_NAME,
                description = ColorBarLayerType.PROPERTY_GRIDLINES_WIDTH_TOOLTIP)
        double gridlinesWidth = ColorBarLayerType.PROPERTY_GRIDLINES_WIDTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_LABEL,
                key = ColorBarLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_NAME,
                description = ColorBarLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_TOOLTIP)
        double gridlinesDashed = ColorBarLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_LABEL,
                key = ColorBarLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_NAME,
                description = ColorBarLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_TOOLTIP,
                interval = "[0.0,1.0]")
        double gridlinesTransparency = ColorBarLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_GRIDLINES_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_GRIDLINES_COLOR_NAME,
                description = ColorBarLayerType.PROPERTY_GRIDLINES_COLOR_TOOLTIP)
        Color gridlinesColor = ColorBarLayerType.PROPERTY_GRIDLINES_COLOR_DEFAULT;


        // Border Section

        @Preference(label = ColorBarLayerType.PROPERTY_BORDER_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_BORDER_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_BORDER_SECTION_TOOLTIP)
        boolean borderSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_BORDER_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_BORDER_SHOW_TOOLTIP)
        boolean borderShow = ColorBarLayerType.PROPERTY_BORDER_SHOW_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_BORDER_WIDTH_LABEL,
                key = ColorBarLayerType.PROPERTY_BORDER_WIDTH_KEY,
                description = ColorBarLayerType.PROPERTY_BORDER_WIDTH_TOOLTIP)
        double borderWidth = ColorBarLayerType.PROPERTY_BORDER_WIDTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_BORDER_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_BORDER_COLOR_KEY,
                description = ColorBarLayerType.PROPERTY_BORDER_COLOR_TOOLTIP)
        Color borderColor = ColorBarLayerType.PROPERTY_BORDER_COLOR_DEFAULT;


        // Tickmarks Section

        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_TOOLTIP)
        boolean tickmarksSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_TOOLTIP)
        boolean tickmarksShow = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_INSIDE_LABEL,
                key = ColorBarLayerType.PROPERTY_TICKMARKS_INSIDE_NAME,
                description = ColorBarLayerType.PROPERTY_TICKMARKS_INSIDE_TOOLTIP)
        boolean tickmarkInside = ColorBarLayerType.PROPERTY_TICKMARKS_INSIDE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_LABEL,
                key = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY,
                description = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_TOOLTIP)
        int tickmarksLength = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY,
                description = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_TOOLTIP)
        Color tickmarksColor = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT;


        // Corner Labels Section

        @Preference(label = ColorBarLayerType.PROPERTY_CORNER_LABELS_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_CORNER_LABELS_SECTION_NAME,
                description = ColorBarLayerType.PROPERTY_CORNER_LABELS_SECTION_TOOLTIP)
        boolean cornerLabelsSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_CORNER_LABELS_NORTH_LABEL,
                key = ColorBarLayerType.PROPERTY_CORNER_LABELS_NORTH_NAME,
                description = ColorBarLayerType.PROPERTY_CORNER_LABELS_NORTH_TOOLTIP)
        boolean cornerLabelsNorth = ColorBarLayerType.PROPERTY_CORNER_LABELS_NORTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_CORNER_LABELS_SOUTH_LABEL,
                key = ColorBarLayerType.PROPERTY_CORNER_LABELS_SOUTH_NAME,
                description = ColorBarLayerType.PROPERTY_CORNER_LABELS_SOUTH_TOOLTIP)
        boolean cornerLabelsSouth = ColorBarLayerType.PROPERTY_CORNER_LABELS_SOUTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_CORNER_LABELS_WEST_LABEL,
                key = ColorBarLayerType.PROPERTY_CORNER_LABELS_WEST_NAME,
                description = ColorBarLayerType.PROPERTY_CORNER_LABELS_WEST_TOOLTIP)
        boolean cornerLabelsWest = ColorBarLayerType.PROPERTY_CORNER_LABELS_WEST_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_CORNER_LABELS_EAST_LABEL,
                key = ColorBarLayerType.PROPERTY_CORNER_LABELS_EAST_NAME,
                description = ColorBarLayerType.PROPERTY_CORNER_LABELS_EAST_TOOLTIP)
        boolean cornerLabelsEast = ColorBarLayerType.PROPERTY_CORNER_LABELS_EAST_DEFAULT;


        // Inside Labels Section

        @Preference(label = ColorBarLayerType.PROPERTY_INSIDE_LABELS_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_INSIDE_LABELS_SECTION_NAME,
                description = ColorBarLayerType.PROPERTY_INSIDE_LABELS_SECTION_TOOLTIP)
        boolean insideLabelsSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_LABEL,
                key = ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_NAME,
                description = ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_TOOLTIP,
                interval = "[0.0,1.0]")
        double insideLabelsBgTransparency = ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_NAME,
                description = ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_TOOLTIP)
        Color insideLabelsBgColor = ColorBarLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_DEFAULT;


        // Restore Defaults Section

        @Preference(label = ColorBarLayerType.PROPERTY_RESTORE_TO_DEFAULTS_LABEL,
                key = ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_NAME,
                description = ColorBarLayerType.PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = ColorBarLayerType.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT;

    }

}
