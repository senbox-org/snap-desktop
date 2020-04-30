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


    Enablement enablementTickmarksInside;
    Enablement enablementTickmarksLength;
    Enablement enablementTickmarksColor;

    Enablement enablementBorderWidth;
    Enablement enablementBorderColor;

    Enablement enablementLabelsFontItalic;
    Enablement enablementLabelsFontBold;
    Enablement enablementLabelsFontName;
    Enablement enablementLabelsFontColor;

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

//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_KEY, true);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY, ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT);




        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY, ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT);




        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_NAME, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_NAME, ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_KEY, ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_LENGTH_KEY, ColorBarLayerType.PROPERTY_LEGEND_LENGTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LEGEND_WIDTH_KEY, ColorBarLayerType.PROPERTY_LEGEND_WIDTH_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_KEY, ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_KEY, ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY, ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_DEFAULT);



        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_SECTION_KEY, true);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, ColorBarLayerType.PROPERTY_LABELS_SHOW_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY, ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY, ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY, ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_DEFAULT);
        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY, ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_DEFAULT);




//
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BORDER_SECTION_KEY, true);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY, ColorBarLayerType.PROPERTY_BORDER_SHOW_DEFAULT);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_BORDER_WIDTH_DEFAULT);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BORDER_COLOR_KEY, ColorBarLayerType.PROPERTY_BORDER_COLOR_DEFAULT);
//
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_KEY, true);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY, ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);
//
//
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BACKDROP_SECTION_KEY, true);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY, ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT);
//        initPropertyDefaults(context, ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY, ColorBarLayerType.PROPERTY_BACKDROP_COLOR_DEFAULT);

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

//        configureTickmarksEnablement(context);
//        configureBorderEnablement(context);
        configureLabelsEnablement(context);


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
//        enablementTickmarksInside.apply();
        enablementTickmarksLength.apply();
        enablementTickmarksColor.apply();
    }







    /**
     * Configure enablement of the labels components
     *
     * @param context
     * @author Daniel Knowles
     */
    private void configureLabelsEnablement(BindingContext context) {


        enablementLabelsFontItalic = context.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY, true,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, true);

        enablementLabelsFontBold = context.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY, true,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, true);

        enablementLabelsFontName = context.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY, true,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, true);

        enablementLabelsFontColor = context.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY, true,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, true);

        // handle it the first time so bound properties get properly enabled
        handleLabelsEnablement();
    }



    /**
     * Handles enablement of the labels components
     *
     * @author Daniel Knowles
     */
    private void handleLabelsEnablement() {
        enablementLabelsFontBold.apply();
        enablementLabelsFontItalic.apply();
        enablementLabelsFontName.apply();
        enablementLabelsFontColor.apply();
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
        return new HelpCtx("colorBarLegendPreferences");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class ColorBarBean {

        // Label Values
//
//        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_LABEL,
//                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_KEY,
//                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_TOOLTIP)
//        boolean labelValuesSection = true;
//
//
//        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_LABEL,
//                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY,
//                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_TOOLTIP,
//                interval = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_INTERVAL)
//        int labelValuesCount = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT;







        // Labels Section

        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_TOOLTIP)
        boolean labelValuesSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_LABEL,
                key = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY,
                description = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_INTERVAL)
        int labelsCount = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT;




        // Size & Scaling Section

        @Preference(label = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_NAME,
                description = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_TOOLTIP)
        boolean sizeScalingSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_LABEL,
                key = ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_NAME,
                description = ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_TOOLTIP)
        boolean applyImageScaling = ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_LABEL,
                key = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_KEY,
                description = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_TOOLTIP)
        double legendScalingPercent = ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_LENGTH_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_LENGTH_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_LENGTH_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_LEGEND_LENGTH_VALUE_INTERVAL)
        int legendLength = ColorBarLayerType.PROPERTY_LEGEND_LENGTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LEGEND_WIDTH_LABEL,
                key = ColorBarLayerType.PROPERTY_LEGEND_WIDTH_KEY,
                description = ColorBarLayerType.PROPERTY_LEGEND_WIDTH_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_LEGEND_WIDTH_INTERVAL)
        int legendWidth = ColorBarLayerType.PROPERTY_LEGEND_WIDTH_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_LABEL,
                key = ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_KEY,
                description = ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_INTERVAL)
        int titleSize = ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_LABEL,
                key = ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_KEY,
                description = ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_INTERVAL)
        int unitsSize = ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_TOOLTIP,
                interval = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_VALUE_INTERVAL)
        int labelsSize = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_DEFAULT;




        // Labels Section

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_SECTION_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_SECTION_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_SECTION_TOOLTIP)
        boolean labelsFormattingSection = true;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_SHOW_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_SHOW_TOOLTIP)
        boolean labelsShow = ColorBarLayerType.PROPERTY_LABELS_SHOW_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_TOOLTIP)
        boolean labelsItalic = ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_DEFAULT;

        @Preference(label = ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_LABEL,
                key = ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY,
                description = ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_TOOLTIP)
        boolean labelsBold = ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_DEFAULT;

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








//
//
//        // Border Section
//
//        @Preference(label = ColorBarLayerType.PROPERTY_BORDER_SECTION_LABEL,
//                key = ColorBarLayerType.PROPERTY_BORDER_SECTION_KEY,
//                description = ColorBarLayerType.PROPERTY_BORDER_SECTION_TOOLTIP)
//        boolean borderSection = true;
//
//        @Preference(label = ColorBarLayerType.PROPERTY_BORDER_SHOW_LABEL,
//                key = ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY,
//                description = ColorBarLayerType.PROPERTY_BORDER_SHOW_TOOLTIP)
//        boolean borderShow = ColorBarLayerType.PROPERTY_BORDER_SHOW_DEFAULT;
//
//        @Preference(label = ColorBarLayerType.PROPERTY_BORDER_WIDTH_LABEL,
//                key = ColorBarLayerType.PROPERTY_BORDER_WIDTH_KEY,
//                description = ColorBarLayerType.PROPERTY_BORDER_WIDTH_TOOLTIP)
//        double borderWidth = ColorBarLayerType.PROPERTY_BORDER_WIDTH_DEFAULT;
//
//        @Preference(label = ColorBarLayerType.PROPERTY_BORDER_COLOR_LABEL,
//                key = ColorBarLayerType.PROPERTY_BORDER_COLOR_KEY,
//                description = ColorBarLayerType.PROPERTY_BORDER_COLOR_TOOLTIP)
//        Color borderColor = ColorBarLayerType.PROPERTY_BORDER_COLOR_DEFAULT;
//
//
//        // Tickmarks Section
//
//        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_LABEL,
//                key = ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_KEY,
//                description = ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_TOOLTIP)
//        boolean tickmarksSection = true;
//
//        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_LABEL,
//                key = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY,
//                description = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_TOOLTIP)
//        boolean tickmarksShow = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT;
//
//
//
//        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_LABEL,
//                key = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY,
//                description = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_TOOLTIP)
//        int tickmarksLength = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT;
//
//        @Preference(label = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_LABEL,
//                key = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY,
//                description = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_TOOLTIP)
//        Color tickmarksColor = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT;
//
//
//
//
//
//        // Inside Labels Section
//
//        @Preference(label = ColorBarLayerType.PROPERTY_BACKDROP_SECTION_LABEL,
//                key = ColorBarLayerType.PROPERTY_BACKDROP_SECTION_KEY,
//                description = ColorBarLayerType.PROPERTY_BACKDROP_SECTION_TOOLTIP)
//        boolean insideLabelsSection = true;
//
//        @Preference(label = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_LABEL,
//                key = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY,
//                description = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_TOOLTIP,
//                interval = "[0.0,1.0]")
//        double insideLabelsBgTransparency = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT;
//
//        @Preference(label = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_LABEL,
//                key = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY,
//                description = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_TOOLTIP)
//        Color insideLabelsBgColor = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_DEFAULT;
//

        // Restore Defaults Section

        @Preference(label = ColorBarLayerType.PROPERTY_RESTORE_TO_DEFAULTS_LABEL,
                key = ColorBarLayerType.PROPERTY_RESTORE_DEFAULTS_NAME,
                description = ColorBarLayerType.PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = ColorBarLayerType.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT;

    }

}
