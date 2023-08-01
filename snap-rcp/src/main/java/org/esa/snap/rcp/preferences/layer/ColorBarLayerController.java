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
 * @author Daniel Knowles
 */


@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerColorBar",
        keywords = "#Options_Keywords_LayerColorBar",
        keywordsCategory = "Layer",
        id = "LayerColorBar")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerColorBar=Color Bar Legend Layer",
        "Options_Keywords_LayerColorBar=layer, colorbar"
})
public final class ColorBarLayerController extends DefaultConfigController {

    Property restoreDefaults;
    Property orientationComboBoxProperty;
    Property labelValuesModeProperty;


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

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_KEY, ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_KEY, ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_SCHEME_LABELS_RESTRICT_KEY, ColorBarLayerType.PROPERTY_SCHEME_LABELS_RESTRICT_DEFAULT);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_HEADER_TITLE_SECTION_KEY, true);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_ALT_USE_KEY, ColorBarLayerType.PROPERTY_TITLE_ALT_USE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_KEY, ColorBarLayerType.PROPERTY_TITLE_DEFAULT);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_ALT_KEY, ColorBarLayerType.PROPERTY_TITLE_ALT_DEFAULT);

//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_HEADER_UNITS_SECTION_KEY, true);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_ALT_USE_KEY, ColorBarLayerType.PROPERTY_UNITS_ALT_USE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_KEY, ColorBarLayerType.PROPERTY_UNITS_DEFAULT);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_ALT_KEY, ColorBarLayerType.PROPERTY_UNITS_ALT_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_NULL_KEY, ColorBarLayerType.PROPERTY_UNITS_NULL_DEFAULT);


        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_CONVERT_CARET_KEY, ColorBarLayerType.PROPERTY_CONVERT_CARET_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_KEY, ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_DEFAULT);


        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_KEY, true);
        orientationComboBoxProperty = initPropertyDefaults(context, ColorBarLayerType.PROPERTY_ORIENTATION_KEY, ColorBarLayerType.PROPERTY_ORIENTATION_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_KEY, ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_DEFAULT);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LOCATION_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_KEY, ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_VERTICAL_KEY, ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_VERTICAL_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_KEY, ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LOCATION_OFFSET_KEY, ColorBarLayerType.PROPERTY_LOCATION_OFFSET_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LOCATION_SHIFT_KEY, ColorBarLayerType.PROPERTY_LOCATION_SHIFT_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LOCATION_INSIDE_KEY, ColorBarLayerType.PROPERTY_LOCATION_INSIDE_DEFAULT);



        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_KEY, true);
        labelValuesModeProperty = initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY, ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY, ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY, ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY, ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY, ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_SCENE_ASPECT_BEST_FIT_KEY, ColorBarLayerType.PROPERTY_SCENE_ASPECT_BEST_FIT_DEFAULT);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_KEY, ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY, ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY, ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_KEY, ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_DEFAULT);




        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_KEY, ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_KEY, ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY, ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_KEY, ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_KEY, ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_KEY, ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_DEFAULT);


        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY, ColorBarLayerType.PROPERTY_TITLE_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_KEY, ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_KEY, ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_KEY, ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_KEY, ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY, ColorBarLayerType.PROPERTY_TITLE_COLOR_DEFAULT);


        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY, ColorBarLayerType.PROPERTY_UNITS_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_KEY, ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_KEY, ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_KEY, ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_KEY, ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_KEY, ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_DEFAULT);


        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, ColorBarLayerType.PROPERTY_LABELS_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY, ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY, ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY, ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY, ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY, ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_DEFAULT);


        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);


        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BACKDROP_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY, ColorBarLayerType.PROPERTY_BACKDROP_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY, ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY, ColorBarLayerType.PROPERTY_BACKDROP_COLOR_DEFAULT);


        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_PALETTE_BORDER_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY, ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY, ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_DEFAULT);


        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_DEFAULT);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_SIZING_SECTION_KEY, true);

        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_KEY, ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_KEY, ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_DEFAULT);



        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_EXPORT_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_EXPORT_EDITOR_SHOW_KEY, ColorBarLayerType.PROPERTY_EXPORT_EDITOR_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_KEY, ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_DEFAULT);



        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_RESTORE_SECTION_KEY, true);
        restoreDefaults =  initPropertyDefaults(context, ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_KEY, ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_DEFAULT);




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

        configureEnablement(context);

        // Handle resetDefaults events - set all other components to defaults
        restoreDefaults.addPropertyChangeListener(evt -> {
            handleRestoreDefaults(context);
        });

        // Handle handleOrientationComboBoxEnablement enablement events -
        orientationComboBoxProperty.addPropertyChangeListener(evt -> {
            handleOrientationComboBoxEnablement(context);
        });

        // Handle handleLabelValuesModeEnablement enablement events -
        labelValuesModeProperty.addPropertyChangeListener(evt -> {
            handleLabelValuesModeEnablement(context);
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

            context.setComponentsEnabled(ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_KEY, false);
        }
    }



    private void handleOrientationComboBoxEnablement(BindingContext context) {

        String alignment = orientationComboBoxProperty.getValue();

        boolean enabled = (ColorBarLayerType.OPTION_VERTICAL.equals(alignment) || ColorBarLayerType.OPTION_BEST_FIT.equals(alignment)) ? true : false;

        context.setComponentsEnabled(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_KEY, enabled);
    }



    private void handleLabelValuesModeEnablement(BindingContext context) {

        String mode = labelValuesModeProperty.getValue();

        if (ColorBarLayerType.DISTRIB_EXACT_STR.equals(mode)) {
            context.setComponentsEnabled(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY, false);
            context.setComponentsEnabled(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY, false);
        } else if (ColorBarLayerType.DISTRIB_MANUAL_STR.equals(mode)) {
            context.setComponentsEnabled(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY, false);
            context.setComponentsEnabled(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY, true);
        } else {
            context.setComponentsEnabled(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY, true);
            context.setComponentsEnabled(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY, false);
        }

    }





    /**
     * Configure enablement of the preferences components
     *
     * @param context
     * @author Daniel Knowles
     */
    private void configureEnablement(BindingContext context) {

        // Orientation
        handleOrientationComboBoxEnablement(context);


        // Label (Values)
        handleLabelValuesModeEnablement(context);


        // Image Scaling Section
        context.bindEnabledState(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_KEY, ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_KEY, ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY);


        // Title Section
        context.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_KEY, ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_KEY, ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_KEY, ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_KEY, ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY, ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY);


        // Units Section
        context.bindEnabledState(ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_KEY, ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_KEY, ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_KEY, ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_KEY, ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_KEY, ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY);


        // Tick-Mark Labels Section
        context.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY, ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY, ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY, ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY, ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY, ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY);


        // Tickmarks Section
        context.bindEnabledState(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY);



        // Palette Border Section
        context.bindEnabledState(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY, ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY);


        // Legend Border Section
        context.bindEnabledState(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_KEY);


        // Backdrop Section
        context.bindEnabledState(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY, ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY);
        context.bindEnabledState(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY, ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY);



        // note if problem occurs then try adding .apply() to method call as illustrated here:
        // context.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY, ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY).apply();
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
                context.setComponentsEnabled(ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_KEY, !isDefaults(context));
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

//        System.out.println("propertyName=" + propertyName);

        if (context == null) {
            System.out.println("WARNING: context is null");
        }

        Property property = context.getPropertySet().getProperty(propertyName);
        if (property == null) {
            System.out.println("WARNING: property is null");
        }

        property.getDescriptor().setDefaultValue(propertyDefault);

        return property;
    }


    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("colorBarLegendPreferences");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class ColorBarBean {














        // Title
        @Preference(label = ColorBarLayerType.PROPERTY_HEADER_TITLE_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_HEADER_TITLE_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_HEADER_TITLE_SECTION_TOOLTIP)
        boolean headerTitleSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_LABEL,
                key = ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_KEY,
                description = ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_TOOLTIP)
        boolean autoApply = ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_LABEL,
                key = ColorBarLayerType.PROPERTY_TITLE_KEY,
                description = ColorBarLayerType.PROPERTY_TITLE_TOOLTIP)
        String title = ColorBarLayerType.PROPERTY_TITLE_DEFAULT;


//        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_ALT_LABEL,
//                key = ColorBarLayerType.PROPERTY_TITLE_ALT_KEY,
//                description = ColorBarLayerType.PROPERTY_TITLE_ALT_TOOLTIP)
//        String titleAlt = ColorBarLayerType.PROPERTY_TITLE_ALT_DEFAULT;
//
//        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_ALT_USE_LABEL,
//                key = ColorBarLayerType.PROPERTY_TITLE_ALT_USE_KEY,
//                description = ColorBarLayerType.PROPERTY_TITLE_ALT_USE_TOOLTIP)
//        boolean titleAltUse = ColorBarLayerType.PROPERTY_TITLE_ALT_USE_DEFAULT;



        // Units

//        @Preference(label = ColorBarLayerType.PROPERTY_HEADER_UNITS_SECTION_LABEL,
//                key = ColorBarLayerType.PROPERTY_HEADER_UNITS_SECTION_KEY,
//                description = ColorBarLayerType.PROPERTY_HEADER_UNITS_SECTION_TOOLTIP)
//        boolean headerUnitsSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_LABEL,
                key = ColorBarLayerType.PROPERTY_UNITS_KEY,
                description = ColorBarLayerType.PROPERTY_UNITS_TOOLTIP)
        String units = ColorBarLayerType.PROPERTY_UNITS_DEFAULT;

//        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_ALT_LABEL,
//                key = ColorBarLayerType.PROPERTY_UNITS_ALT_KEY,
//                description = ColorBarLayerType.PROPERTY_UNITS_ALT_TOOLTIP)
//        String unitsAlt = ColorBarLayerType.PROPERTY_UNITS_ALT_DEFAULT;
//
//        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_ALT_USE_LABEL,
//                key = ColorBarLayerType.PROPERTY_UNITS_ALT_USE_KEY,
//                description = ColorBarLayerType.PROPERTY_UNITS_ALT_USE_TOOLTIP)
//        boolean unitsAltUse = ColorBarLayerType.PROPERTY_UNITS_ALT_USE_DEFAULT;



        // Labels

        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_TOOLTIP)
        boolean labelValuesSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_LABEL,
                key = ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_KEY,
                description = ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_TOOLTIP)
        boolean schemeLabelsApply = ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_LABEL,
                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY,
                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_TOOLTIP,
                valueSet = {ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_OPTION1,
                        ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_OPTION2,
                        ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_OPTION3})
        String labelValuesMode = ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_LABEL,
                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY,
                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_INTERVAL)
        int labelsCount = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_LABEL,
                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY,
                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_TOOLTIP)
        String labelValuesActual = ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT;



        @Preference(label = ColorBarLayerType.PROPERTY_SCHEME_LABELS_RESTRICT_LABEL,
                key = ColorBarLayerType.PROPERTY_SCHEME_LABELS_RESTRICT_KEY,
                description = ColorBarLayerType.PROPERTY_SCHEME_LABELS_RESTRICT_TOOLTIP)
        boolean schemeLabelsRestrict = ColorBarLayerType.PROPERTY_SCHEME_LABELS_RESTRICT_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_LABEL,
                key = ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY,
                description = ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_TOOLTIP)
        boolean populateLabelValuesTextfield = ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_DEFAULT;



        // Orientation

        @Preference(label = ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_TOOLTIP)
        boolean orientationSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_ORIENTATION_LABEL,
                key = ColorBarLayerType.PROPERTY_ORIENTATION_KEY,
                description = ColorBarLayerType.PROPERTY_ORIENTATION_TOOLTIP,
                valueSet = {ColorBarLayerType.PROPERTY_ORIENTATION_OPTION1,
                        ColorBarLayerType.PROPERTY_ORIENTATION_OPTION2,
                        ColorBarLayerType.PROPERTY_ORIENTATION_OPTION3})
        String orientation = ColorBarLayerType.PROPERTY_ORIENTATION_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_SCENE_ASPECT_BEST_FIT_LABEL,
                key = ColorBarLayerType.PROPERTY_SCENE_ASPECT_BEST_FIT_KEY,
                description = ColorBarLayerType.PROPERTY_SCENE_ASPECT_BEST_FIT_TOOLTIP)
        double sceneAspectBestFit = ColorBarLayerType.PROPERTY_SCENE_ASPECT_BEST_FIT_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_LABEL,
                key = ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_KEY,
                description = ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_TOOLTIP,
                valueSet = {ColorBarLayerType.VERTICAL_TITLE_LEFT,
                        ColorBarLayerType.VERTICAL_TITLE_RIGHT,
                        ColorBarLayerType.VERTICAL_TITLE_TOP,
                        ColorBarLayerType.VERTICAL_TITLE_BOTTOM})
        String titleVerticalLocation = ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_LABEL,
                key = ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY,
                description = ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_TOOLTIP)
        boolean reversePaletteAndLabels = ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_DEFAULT;




        // Location


        @Preference(label = ColorBarLayerType.PROPERTY_LOCATION_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_LOCATION_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_LOCATION_SECTION_TOOLTIP)
        boolean placementLocationSection = true;


        @Preference(label = ColorBarLayerType.PROPERTY_LOCATION_INSIDE_LABEL,
                key = ColorBarLayerType.PROPERTY_LOCATION_INSIDE_KEY,
                description = ColorBarLayerType.PROPERTY_LOCATION_INSIDE_TOOLTIP)
        boolean placementInside = ColorBarLayerType.PROPERTY_LOCATION_INSIDE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_LABEL,
                key = ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_KEY,
                description = ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_TOOLTIP,
                valueSet = {ColorBarLayerType.LOCATION_UPPER_LEFT,
                        ColorBarLayerType.LOCATION_UPPER_CENTER,
                        ColorBarLayerType.LOCATION_UPPER_RIGHT,
                        ColorBarLayerType.LOCATION_LOWER_LEFT,
                        ColorBarLayerType.LOCATION_LOWER_CENTER,
                        ColorBarLayerType.LOCATION_LOWER_RIGHT})
        //        ColorBarLayerType.LOCATION_LEFT_CENTER,
//        ColorBarLayerType.LOCATION_RIGHT_CENTER
        String placementHorizontalMode = ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_DEFAULT;



        @Preference(label = ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_VERTICAL_LABEL,
                key = ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_VERTICAL_KEY,
                description = ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_VERTICAL_TOOLTIP,
                valueSet = {ColorBarLayerType.LOCATION_UPPER_LEFT,
                        ColorBarLayerType.LOCATION_LEFT_CENTER,
                        ColorBarLayerType.LOCATION_LOWER_LEFT,
                        ColorBarLayerType.LOCATION_UPPER_RIGHT,
                        ColorBarLayerType.LOCATION_RIGHT_CENTER,
                        ColorBarLayerType.LOCATION_LOWER_RIGHT})
        String placementVerticalMode = ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_VERTICAL_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_LABEL,
                key = ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_KEY,
                description = ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_INTERVAL)

        double locationGapFactor = ColorBarLayerType.PROPERTY_LOCATION_GAP_FACTOR_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LOCATION_OFFSET_LABEL,
                key = ColorBarLayerType.PROPERTY_LOCATION_OFFSET_KEY,
                description = ColorBarLayerType.PROPERTY_LOCATION_OFFSET_TOOLTIP)
        double locationOffset = ColorBarLayerType.PROPERTY_LOCATION_OFFSET_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LOCATION_SHIFT_LABEL,
                key = ColorBarLayerType.PROPERTY_LOCATION_SHIFT_KEY,
                description = ColorBarLayerType.PROPERTY_LOCATION_SHIFT_TOOLTIP)
        double locationShift = ColorBarLayerType.PROPERTY_LOCATION_SHIFT_DEFAULT;



























        // Title Format Section

        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_TITLE_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_TITLE_SECTION_TOOLTIP)
        boolean titleSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_TITLE_SHOW_TOOLTIP)
        boolean titleShow = ColorBarLayerType.PROPERTY_TITLE_SHOW_DEFAULT;




        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_LABEL,
                key = ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_KEY,
                description = ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_TOOLTIP)
        boolean titleBold = ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_LABEL,
                key = ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_KEY,
                description = ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_TOOLTIP)
        boolean titleItalic = ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_DEFAULT;



        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_LABEL,
                key = ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_KEY,
                description = ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_TOOLTIP,
                valueSet = {ColorBarLayerType.FONT_NAME_SANSERIF,
                        ColorBarLayerType.FONT_NAME_SERIF,
                        ColorBarLayerType.FONT_NAME_COURIER,
                        ColorBarLayerType.FONT_NAME_MONOSPACED})
        String titleFont = ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY,
                description = ColorBarLayerType.PROPERTY_TITLE_COLOR_TOOLTIP)
        Color titleColor = ColorBarLayerType.PROPERTY_TITLE_COLOR_DEFAULT;











        // Units Format Section

        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_UNITS_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_UNITS_SECTION_TOOLTIP)
        boolean unitsSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_UNITS_SHOW_TOOLTIP)
        boolean unitsShow = ColorBarLayerType.PROPERTY_UNITS_SHOW_DEFAULT;



        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_LABEL,
                key = ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_KEY,
                description = ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_TOOLTIP)
        boolean unitsBold = ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_LABEL,
                key = ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_KEY,
                description = ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_TOOLTIP)
        boolean unitsItalic = ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_LABEL,
                key = ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_KEY,
                description = ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_TOOLTIP,
                valueSet = {ColorBarLayerType.FONT_NAME_SANSERIF,
                        ColorBarLayerType.FONT_NAME_SERIF,
                        ColorBarLayerType.FONT_NAME_COURIER,
                        ColorBarLayerType.FONT_NAME_MONOSPACED})
        String unitsFont = ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_KEY,
                description = ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_TOOLTIP)
        Color unitsColor = ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_NULL_LABEL,
                key = ColorBarLayerType.PROPERTY_UNITS_NULL_KEY,
                description = ColorBarLayerType.PROPERTY_UNITS_NULL_TOOLTIP)
        String unitsNull = ColorBarLayerType.PROPERTY_UNITS_NULL_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_CONVERT_CARET_LABEL,
                key = ColorBarLayerType.PROPERTY_CONVERT_CARET_KEY,
                description = ColorBarLayerType.PROPERTY_CONVERT_CARET_TOOLTIP)
        boolean convertCaret = ColorBarLayerType.PROPERTY_CONVERT_CARET_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_LABEL,
                key = ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_KEY,
                description = ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_TOOLTIP)
        boolean unitsParenthesis = ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_DEFAULT;
















        // Tick Label Format Section

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_SECTION_TOOLTIP)
        boolean labelsFormattingSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_SHOW_TOOLTIP)
        boolean labelsShow = ColorBarLayerType.PROPERTY_LABELS_SHOW_DEFAULT;




        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_TOOLTIP)
        boolean labelsBold = ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_TOOLTIP)
        boolean labelsItalic = ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_TOOLTIP,
                valueSet = {ColorBarLayerType.FONT_NAME_SANSERIF,
                        ColorBarLayerType.FONT_NAME_SERIF,
                        ColorBarLayerType.FONT_NAME_COURIER,
                        ColorBarLayerType.FONT_NAME_MONOSPACED})
        String labelsFont = ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_TOOLTIP)
        Color labelsColor = ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_LABEL,
                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_KEY,
                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_INTERVAL)
        double labelScaling = ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_LABEL,
                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY,
                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_INTERVAL)
        int decimalPlaces = ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_LABEL,
                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY,
                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TOOLTIP)
        boolean decimalPlacesForce = ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT;

//
//        @Preference(label = ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_LABEL,
//                key = ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_KEY,
//                description = ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_TOOLTIP)
//        double weightTolerance = ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_DEFAULT;




        // Tick Marks Section

        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_TOOLTIP)
        boolean tickMarksSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_TOOLTIP)
        boolean tickMarksShow = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_LABEL,
                key = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY,
                description = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_TOOLTIP)
        int tickMarksLength = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_LABEL,
                key = ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_KEY,
                description = ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_TOOLTIP)
        int tickMarksWidth = ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY,
                description = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_TOOLTIP)
        Color tickMarksColor = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT;







        // Palette Border

        @Preference(label = ColorBarLayerType.PROPERTY_PALETTE_BORDER_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_PALETTE_BORDER_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_PALETTE_BORDER_SECTION_TOOLTIP)
        boolean paletteBorderSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_TOOLTIP)
        boolean paletteBorderShow = ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_LABEL,
                key = ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_KEY,
                description = ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_TOOLTIP)
        int paletteBorderWidth = ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY,
                description = ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_TOOLTIP)
        Color paletteBorderColor = ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_DEFAULT;






        // Legend Border

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_TOOLTIP)
        boolean legendBorderSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_TOOLTIP)
        boolean legendBorderShow = ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_TOOLTIP)
        int legendBorderWidth = ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_TOOLTIP)
        Color legendBorderColor = ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_DEFAULT;



        // Backdrop Section

        @Preference(label = ColorBarLayerType.PROPERTY_BACKDROP_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_BACKDROP_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_BACKDROP_SECTION_TOOLTIP)
        boolean backdropSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_BACKDROP_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_BACKDROP_SHOW_TOOLTIP)
        boolean backdropShow = ColorBarLayerType.PROPERTY_BACKDROP_SHOW_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_LABEL,
                key = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY,
                description = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_TOOLTIP)
        double backdropTransparency = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_LABEL,
                key = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY,
                description = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_TOOLTIP)
        Color backdropColor = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_DEFAULT;



        // Scaling Section

        @Preference(label = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_TOOLTIP)
        boolean sizeScalingSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_LABEL + " (LAYER ONLY)",
                key = ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_KEY,
                description = ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_TOOLTIP)
        boolean applyImageScaling = ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_LABEL + " (LAYER ONLY)",
                key = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_KEY,
                description = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_INTERVAL)
        double legendScalingPercent = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_DEFAULT;



        @Preference(label = ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_LABEL + " (EXPORT ONLY)",
                key = ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY,
                description = ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_TOOLTIP)
        boolean exportUseLegendWidth = ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_LABEL + " (EXPORT ONLY)",
                key = ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_KEY,
                description = ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_TOOLTIP)
        int exportLegendWidth = ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_DEFAULT;



        // Sizing Section

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_SIZING_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_SIZING_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_SIZING_SECTION_TOOLTIP)
        boolean sizing = true;




        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_LABEL,
                key = ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_KEY,
                description = ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_INTERVAL)
        int titleSize = ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_LABEL,
                key = ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_KEY,
                description = ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_INTERVAL)
        int unitsSize = ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_DEFAULT;


        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_VALUE_INTERVAL)
        int labelsSize = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_LABEL,
                key = ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_KEY,
                description = ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_VALUE_INTERVAL)
        int colorbarLength = ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_LABEL,
                key = ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_KEY,
                description = ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_INTERVAL)
        int colorbarWidth = ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_DEFAULT;


        // Border Gap Section

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_SECTION_TOOLTIP)
        boolean borderGap = true;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_TOOLTIP)
        double borderGapTop = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_TOOLTIP)
        double borderGapBottom = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_TOOLTIP)
        double borderGapLeftside = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_TOOLTIP)
        double borderGapRightside = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_TOOLTIP)
        double titleGap = ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_TOOLTIP)
        double labelGap = ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_DEFAULT;


        // Color Bar Export

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_EXPORT_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_EXPORT_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_EXPORT_SECTION_TOOLTIP)
        boolean exportSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_EXPORT_EDITOR_SHOW_LABEL + " (EXPORT ONLY)",
                key = ColorBarLayerType.PROPERTY_EXPORT_EDITOR_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_EXPORT_EDITOR_SHOW_TOOLTIP)
        boolean exportEditorShow = ColorBarLayerType.PROPERTY_EXPORT_EDITOR_SHOW_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_LABEL + " (EXPORT ONLY)",
                key = ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_KEY,
                description = ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_TOOLTIP)
        boolean exportBWColorUse = ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_DEFAULT;












        // Restore Defaults Section




        @Preference(label = ColorBarLayerType.PROPERTY_RESTORE_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_RESTORE_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_RESTORE_SECTION_TOOLTIP)
        boolean restoreDefaultsSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_LABEL,
                key = ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_KEY,
                description = ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_DEFAULT;

    }

}
