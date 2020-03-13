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
import org.esa.snap.core.datamodel.ColorManipulationDefaults;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;

/**
 * Panel handling general layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author Daniel Knowles (NASA)
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_ColorManipulation=" + ColorManipulationDefaults.TOOLNAME_COLOR_MANIPULATION,
        "Options_Keywords_ColorManipulation=layer, general"
})
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_ColorManipulation",
        keywords = "#Options_Keywords_ColorManipulation",
        keywordsCategory = "color, layer",
        id = "colorManipulationController",
        position = 3)


public final class ColorManipulationController extends DefaultConfigController {

    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;


    Enablement enablementDefaultColorBlindCpd;
    Enablement enablementDefaultSchemeStandardCpd;
    Enablement enablementDefaultSchemeColorBlindCpd;
    Enablement enablementDefaultStandardCpd;


    protected PropertySet createPropertySet() {
        return createPropertySet(new GeneralLayerBean());
    }


    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("colorManipulationPreferences");
    }

    @Override
    protected JPanel createPanel(BindingContext context) {

        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_SECTION_KEY, true);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_KEY, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_KEY, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_KEY, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_KEY, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_DEFAULT);

        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_GENERAL_SECTION_KEY, true);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_KEY, ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_GENERAL_RANGE_KEY, ColorManipulationDefaults.PROPERTY_GENERAL_RANGE_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_GENERAL_LOG_KEY, ColorManipulationDefaults.PROPERTY_GENERAL_LOG_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_RESTORE_SECTION_KEY, true);

        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SCHEME_SECTION_KEY, true);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SCHEME_AUTO_APPLY_KEY, ColorManipulationDefaults.PROPERTY_SCHEME_AUTO_APPLY_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_KEY, ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_KEY, ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SCHEME_LOG_KEY, ColorManipulationDefaults.PROPERTY_SCHEME_LOG_DEFAULT);

        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SCHEME_SELECTOR_SECTION_KEY, true);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SCHEME_VERBOSE_KEY, ColorManipulationDefaults.PROPERTY_SCHEME_VERBOSE_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SCHEME_SORT_KEY, ColorManipulationDefaults.PROPERTY_SCHEME_SORT_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SCHEME_CATEGORIZE_DISPLAY_KEY, ColorManipulationDefaults.PROPERTY_SCHEME_CATEGORIZE_DISPLAY_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SCHEME_SHOW_DISABLED_KEY, ColorManipulationDefaults.PROPERTY_SCHEME_SHOW_DISABLED_DEFAULT);

        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SLIDERS_SECTION_KEY, true);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SLIDERS_ZOOM_IN_KEY, ColorManipulationDefaults.PROPERTY_SLIDERS_ZOOM_IN_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_SLIDERS_SHOW_INFORMATION_KEY, ColorManipulationDefaults.PROPERTY_SLIDERS_SHOW_INFORMATION_DEFAULT);


        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_BUTTONS_SECTION_KEY, true);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_ZOOM_VERTICAL_BUTTONS_KEY, ColorManipulationDefaults.PROPERTY_ZOOM_VERTICAL_BUTTONS_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_1_SIGMA_BUTTON_KEY, ColorManipulationDefaults.PROPERTY_1_SIGMA_BUTTON_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_2_SIGMA_BUTTON_KEY, ColorManipulationDefaults.PROPERTY_2_SIGMA_BUTTON_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_3_SIGMA_BUTTON_KEY, ColorManipulationDefaults.PROPERTY_3_SIGMA_BUTTON_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_95_PERCENT_BUTTON_KEY, ColorManipulationDefaults.PROPERTY_95_PERCENT_BUTTON_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_100_PERCENT_BUTTON_KEY, ColorManipulationDefaults.PROPERTY_100_PERCENT_BUTTON_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_INFORMATION_BUTTON_KEY, ColorManipulationDefaults.PROPERTY_INFORMATION_BUTTON_DEFAULT);


        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_SECTION_KEY, true);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MIN_KEY, ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MIN_DEFAULT);
        initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MAX_KEY, ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MAX_DEFAULT);





        restoreDefaults =  initPropertyDefaults(context, ColorManipulationDefaults.PROPERTY_RESTORE_DEFAULTS_NAME, ColorManipulationDefaults.PROPERTY_RESTORE_DEFAULTS_DEFAULT);




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

            context.setComponentsEnabled(ColorManipulationDefaults.PROPERTY_RESTORE_DEFAULTS_NAME, false);
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
                context.setComponentsEnabled(ColorManipulationDefaults.PROPERTY_RESTORE_DEFAULTS_NAME, !isDefaults(context));
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
     * Handles enablement of the components
     *
     * @author Daniel Knowles
     */
    private void handleUseColorBlindEnablement() {
        enablementDefaultColorBlindCpd.apply();
        enablementDefaultSchemeColorBlindCpd.apply();

        enablementDefaultStandardCpd.apply();
        enablementDefaultSchemeStandardCpd.apply();
    }




    @SuppressWarnings("UnusedDeclaration")
    static class GeneralLayerBean {

        // Default Palettes

        @Preference(label = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_SECTION_LABEL,
                key = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_SECTION_KEY,
                description = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_SECTION_TOOLTIP)
        boolean defaultPaletteSection = true;

        @Preference(label = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_LABEL,
                key = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_KEY,
                description = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_TOOLTIP)
        String grayScaleCpd = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_LABEL,
                key = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_KEY,
                description = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_TOOLTIP)
        String standardColorCpd = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_LABEL,
                key = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_KEY,
                description = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_TOOLTIP)
        String colorBlindCpd = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_LABEL,
                key = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_KEY,
                description = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_TOOLTIP)
        String otherCpd = ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_DEFAULT;





        // General Options

        @Preference(label = ColorManipulationDefaults.PROPERTY_GENERAL_SECTION_LABEL,
                key = ColorManipulationDefaults.PROPERTY_GENERAL_SECTION_KEY,
                description = ColorManipulationDefaults.PROPERTY_GENERAL_SECTION_TOOLTIP)
        boolean generalBehaviorSection = true;

        @Preference(label = ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_LABEL,
                key = ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_KEY,
                description = ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_TOOLTIP,
                valueSet = {ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_OPTION1,
                        ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_OPTION2,
                        ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_OPTION3,
                        ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_OPTION4})
        String generalCpd = ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_GENERAL_RANGE_LABEL,
                key = ColorManipulationDefaults.PROPERTY_GENERAL_RANGE_KEY,
                description = ColorManipulationDefaults.PROPERTY_GENERAL_RANGE_TOOLTIP,
                valueSet = {ColorManipulationDefaults.PROPERTY_GENERAL_RANGE_OPTION1,
                        ColorManipulationDefaults.PROPERTY_GENERAL_RANGE_OPTION2})
        String generalRange = ColorManipulationDefaults.PROPERTY_GENERAL_RANGE_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_GENERAL_LOG_LABEL,
                key = ColorManipulationDefaults.PROPERTY_GENERAL_LOG_KEY,
                description = ColorManipulationDefaults.PROPERTY_GENERAL_LOG_TOOLTIP,
                valueSet = {ColorManipulationDefaults.PROPERTY_GENERAL_LOG_OPTION1,
                        ColorManipulationDefaults.PROPERTY_GENERAL_LOG_OPTION2,
                        ColorManipulationDefaults.PROPERTY_GENERAL_LOG_OPTION3})
        String generalLog = ColorManipulationDefaults.PROPERTY_GENERAL_LOG_DEFAULT;



        // Scheme Options

        @Preference(label = ColorManipulationDefaults.PROPERTY_SCHEME_SECTION_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SCHEME_SECTION_KEY,
                description = ColorManipulationDefaults.PROPERTY_SCHEME_SECTION_TOOLTIP)
        boolean schemeOptionsSection = true;

        @Preference(label = ColorManipulationDefaults.PROPERTY_SCHEME_AUTO_APPLY_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SCHEME_AUTO_APPLY_KEY,
                description = ColorManipulationDefaults.PROPERTY_SCHEME_AUTO_APPLY_TOOLTIP)
        boolean autoApplySchemes = ColorManipulationDefaults.PROPERTY_SCHEME_AUTO_APPLY_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_KEY,
                description = ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_TOOLTIP,
                valueSet = {ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_OPTION1,
                        ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_OPTION2,
                        ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_OPTION3,
                        ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_OPTION4,
                        ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_OPTION5,
                        ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_OPTION6})
        String schemeCpd = ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_KEY,
                description = ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_TOOLTIP,
                valueSet = {ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_OPTION1,
                        ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_OPTION2,
                        ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_OPTION3})
        String schemeRange = ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_SCHEME_LOG_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SCHEME_LOG_KEY,
                description = ColorManipulationDefaults.PROPERTY_SCHEME_LOG_TOOLTIP,
                valueSet = {ColorManipulationDefaults.PROPERTY_SCHEME_LOG_OPTION1,
                        ColorManipulationDefaults.PROPERTY_SCHEME_LOG_OPTION2,
                        ColorManipulationDefaults.PROPERTY_SCHEME_LOG_OPTION3,
                        ColorManipulationDefaults.PROPERTY_SCHEME_LOG_OPTION4})
        String schemeLog = ColorManipulationDefaults.PROPERTY_SCHEME_LOG_DEFAULT;




        // Scheme Selector Options

        @Preference(label = ColorManipulationDefaults.PROPERTY_SCHEME_SELECTOR_SECTION_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SCHEME_SELECTOR_SECTION_KEY,
                description = ColorManipulationDefaults.PROPERTY_SCHEME_SELECTOR_SECTION_TOOLTIP)
        boolean schemeSelectorSection = true;

        @Preference(label = ColorManipulationDefaults.PROPERTY_SCHEME_VERBOSE_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SCHEME_VERBOSE_KEY,
                description = ColorManipulationDefaults.PROPERTY_SCHEME_VERBOSE_TOOLTIP)
        boolean schemeSelectorVerbose = ColorManipulationDefaults.PROPERTY_SCHEME_VERBOSE_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_SCHEME_SORT_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SCHEME_SORT_KEY,
                description = ColorManipulationDefaults.PROPERTY_SCHEME_SORT_TOOLTIP)
        boolean schemeSelectorSort = ColorManipulationDefaults.PROPERTY_SCHEME_SORT_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_SCHEME_CATEGORIZE_DISPLAY_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SCHEME_CATEGORIZE_DISPLAY_KEY,
                description = ColorManipulationDefaults.PROPERTY_SCHEME_CATEGORIZE_DISPLAY_TOOLTIP)
        boolean schemeSelectorSplit = ColorManipulationDefaults.PROPERTY_SCHEME_CATEGORIZE_DISPLAY_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_SCHEME_SHOW_DISABLED_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SCHEME_SHOW_DISABLED_KEY,
                description = ColorManipulationDefaults.PROPERTY_SCHEME_SHOW_DISABLED_TOOLTIP)
        boolean schemeSelectorShowDisabled = ColorManipulationDefaults.PROPERTY_SCHEME_SHOW_DISABLED_DEFAULT;



        // Slider and Range Options

        @Preference(label = ColorManipulationDefaults.PROPERTY_SLIDERS_SECTION_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SLIDERS_SECTION_KEY,
                description = ColorManipulationDefaults.PROPERTY_SLIDERS_SECTION_TOOLTIP)
        boolean sliderOptionsSection = true;

        @Preference(label = ColorManipulationDefaults.PROPERTY_SLIDERS_ZOOM_IN_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SLIDERS_ZOOM_IN_KEY,
                description = ColorManipulationDefaults.PROPERTY_SLIDERS_ZOOM_IN_TOOLTIP)
        boolean sliderZoom = ColorManipulationDefaults.PROPERTY_SLIDERS_ZOOM_IN_DEFAULT;


        @Preference(label = ColorManipulationDefaults.PROPERTY_SLIDERS_SHOW_INFORMATION_LABEL,
                key = ColorManipulationDefaults.PROPERTY_SLIDERS_SHOW_INFORMATION_KEY,
                description = ColorManipulationDefaults.PROPERTY_SLIDERS_SHOW_INFORMATION_TOOLTIP)
        boolean slidersShowExtraInfo = ColorManipulationDefaults.PROPERTY_SLIDERS_SHOW_INFORMATION_DEFAULT;



        // Button Options

        @Preference(label = ColorManipulationDefaults.PROPERTY_BUTTONS_SECTION_LABEL,
                key = ColorManipulationDefaults.PROPERTY_BUTTONS_SECTION_KEY,
                description = ColorManipulationDefaults.PROPERTY_BUTTONS_SECTION_TOOLTIP)
        boolean buttonsSection = true;

        @Preference(label = ColorManipulationDefaults.PROPERTY_ZOOM_VERTICAL_BUTTONS_LABEL,
                key = ColorManipulationDefaults.PROPERTY_ZOOM_VERTICAL_BUTTONS_KEY,
                description = ColorManipulationDefaults.PROPERTY_ZOOM_VERTICAL_BUTTONS_TOOLTIP)
        boolean sliderZoomVertical = ColorManipulationDefaults.PROPERTY_ZOOM_VERTICAL_BUTTONS_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_1_SIGMA_BUTTON_LABEL,
                key = ColorManipulationDefaults.PROPERTY_1_SIGMA_BUTTON_KEY,
                description = ColorManipulationDefaults.PROPERTY_1_SIGMA_BUTTON_TOOLTIP)
        boolean range1Sigma = ColorManipulationDefaults.PROPERTY_1_SIGMA_BUTTON_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_2_SIGMA_BUTTON_LABEL,
                key = ColorManipulationDefaults.PROPERTY_2_SIGMA_BUTTON_KEY,
                description = ColorManipulationDefaults.PROPERTY_2_SIGMA_BUTTON_TOOLTIP)
        boolean range2Sigma = ColorManipulationDefaults.PROPERTY_2_SIGMA_BUTTON_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_3_SIGMA_BUTTON_LABEL,
                key = ColorManipulationDefaults.PROPERTY_3_SIGMA_BUTTON_KEY,
                description = ColorManipulationDefaults.PROPERTY_3_SIGMA_BUTTON_TOOLTIP)
        boolean range3Sigma = ColorManipulationDefaults.PROPERTY_3_SIGMA_BUTTON_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_95_PERCENT_BUTTON_LABEL,
                key = ColorManipulationDefaults.PROPERTY_95_PERCENT_BUTTON_KEY,
                description = ColorManipulationDefaults.PROPERTY_95_PERCENT_BUTTON_TOOLTIP)
        boolean range95 = ColorManipulationDefaults.PROPERTY_95_PERCENT_BUTTON_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_100_PERCENT_BUTTON_LABEL,
                key = ColorManipulationDefaults.PROPERTY_100_PERCENT_BUTTON_KEY,
                description = ColorManipulationDefaults.PROPERTY_100_PERCENT_BUTTON_TOOLTIP)
        boolean range100 = ColorManipulationDefaults.PROPERTY_100_PERCENT_BUTTON_DEFAULT;


        @Preference(label = ColorManipulationDefaults.PROPERTY_INFORMATION_BUTTON_LABEL,
                key = ColorManipulationDefaults.PROPERTY_INFORMATION_BUTTON_KEY,
                description = ColorManipulationDefaults.PROPERTY_INFORMATION_BUTTON_TOOLTIP)
        boolean slidersShowExtraInfoButton = ColorManipulationDefaults.PROPERTY_INFORMATION_BUTTON_DEFAULT;




        // RGB Options

        @Preference(label = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_SECTION_LABEL,
                key = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_SECTION_KEY,
                description = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_SECTION_TOOLTIP)
        boolean rgbOptionsSection = true;

        @Preference(label = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MIN_LABEL,
                key = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MIN_KEY,
                description = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MIN_TOOLTIP)
        double rgbOptionsMin = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MIN_DEFAULT;

        @Preference(label = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MAX_LABEL,
                key = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MAX_KEY,
                description = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MAX_TOOLTIP)
        double rgbOptionsMax = ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MAX_DEFAULT;






        // Restore Defaults

        @Preference(label = ColorManipulationDefaults.PROPERTY_RESTORE_SECTION_LABEL,
                key = ColorManipulationDefaults.PROPERTY_RESTORE_SECTION_KEY,
                description = ColorManipulationDefaults.PROPERTY_RESTORE_SECTION_TOOLTIP)
        boolean restoreDefaultsSection = true;

        @Preference(label = ColorManipulationDefaults.PROPERTY_RESTORE_DEFAULTS_LABEL,
                key = ColorManipulationDefaults.PROPERTY_RESTORE_DEFAULTS_NAME,
                description = ColorManipulationDefaults.PROPERTY_RESTORE_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = ColorManipulationDefaults.PROPERTY_RESTORE_DEFAULTS_DEFAULT;
    }

}
