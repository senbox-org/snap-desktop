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
import org.esa.snap.core.datamodel.ColorPaletteSchemes;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;

/**
 * Panel handling general layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author Daniel Knowles
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_ColorManipulation=Color Manipulation",
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
        return new HelpCtx("layer");
    }

    @Override
    protected JPanel createPanel(BindingContext context) {

        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_GENERAL_BEHAVIOR_SECTION_KEY, true);
        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_GENERAL_CPD_KEY, ColorPaletteSchemes.PROPERTY_GENERAL_CPD_DEFAULT);
        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_GENERAL_RANGE_KEY, ColorPaletteSchemes.PROPERTY_GENERAL_RANGE_DEFAULT);
        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_GENERAL_LOG_KEY, ColorPaletteSchemes.PROPERTY_GENERAL_LOG_DEFAULT);



        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_SCHEME_BEHAVIOR_SECTION_KEY, true);

        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_AUTO_APPLY_SCHEMES_KEY, ColorPaletteSchemes.PROPERTY_AUTO_APPLY_SCHEMES_DEFAULT);

        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_SCHEME_CPD_KEY, ColorPaletteSchemes.PROPERTY_SCHEME_CPD_DEFAULT);
        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_SCHEME_RANGE_KEY, ColorPaletteSchemes.PROPERTY_SCHEME_RANGE_DEFAULT);
        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_SCHEME_LOG_KEY, ColorPaletteSchemes.PROPERTY_SCHEME_LOG_DEFAULT);
//        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_USE_SCHEME_PALETTE_STX_KEY, ColorPaletteSchemes.PROPERTY_USE_SCHEME_PALETTE_STX_DEFAULT);


        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_DEFAULT_CPD_SECTION_KEY, true);
        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_STANDARD_COLOR_CPD_KEY, ColorPaletteSchemes.PROPERTY_STANDARD_COLOR_CPD_DEFAULT);
        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_COLOR_BLIND_CPD_KEY, ColorPaletteSchemes.PROPERTY_COLOR_BLIND_CPD_DEFAULT);
        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_GRAY_SCALE_CPD_KEY, ColorPaletteSchemes.PROPERTY_GRAY_SCALE_CPD_DEFAULT);
        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_OTHER_CPD_KEY, ColorPaletteSchemes.PROPERTY_OTHER_CPD_DEFAULT);



        initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_RESTORE_SECTION_KEY, true);

        restoreDefaults =  initPropertyDefaults(context, ColorPaletteSchemes.PROPERTY_RESTORE_DEFAULTS_NAME, ColorPaletteSchemes.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT);





/*


*Default Palettes:
Gray Scale
Standard Color
Color Blind

*General Behavior:
Default Palette
     Gray Scale Default
     Standard Color Default
     Color Blind Default

*Scheme Behavior:
Auto-Apply From Band Name

Palette
     Use Standard Scheme Palette
     Use Color Blind Scheme Palette
     Gray Scale Default
     Standard Color Default
     Color Blind Default
     Maintain Current?
Range
     Use Scheme Range
     Use Data Range
     Maintain Current Range?
Log
     Use Scheme Log Scaling
     Maintain Current Log Scaling?


 */






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

//        configureColorBlindEnablement(context);


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

            context.setComponentsEnabled(ColorPaletteSchemes.PROPERTY_RESTORE_DEFAULTS_NAME, false);
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
                context.setComponentsEnabled(ColorPaletteSchemes.PROPERTY_RESTORE_DEFAULTS_NAME, !isDefaults(context));
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
     * Configure enablement of the components
     *
     * @param context
     * @author Daniel Knowles
     */
    private void configureColorBlindEnablement(BindingContext context) {



        enablementDefaultColorBlindCpd = context.bindEnabledState(ColorPaletteSchemes.PROPERTY_COLOR_BLIND_CPD_KEY, true,
                ColorPaletteSchemes.PROPERTY_USE_COLOR_BLIND_CPD_KEY, true);

        enablementDefaultSchemeColorBlindCpd = context.bindEnabledState(ColorPaletteSchemes.PROPERTY_GENERAL_CPD_KEY, true,
                ColorPaletteSchemes.PROPERTY_USE_COLOR_BLIND_CPD_KEY, true);

        enablementDefaultSchemeStandardCpd = context.bindEnabledState(ColorPaletteSchemes.PROPERTY_GRAY_SCALE_CPD_KEY, false,
                ColorPaletteSchemes.PROPERTY_USE_COLOR_BLIND_CPD_KEY, true);

        enablementDefaultStandardCpd = context.bindEnabledState(ColorPaletteSchemes.PROPERTY_STANDARD_COLOR_CPD_KEY, false,
                ColorPaletteSchemes.PROPERTY_USE_COLOR_BLIND_CPD_KEY, true);





        // handle it the first time so bound properties get properly enabled
        handleUseColorBlindEnablement();
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











        @Preference(label = ColorPaletteSchemes.PROPERTY_GENERAL_BEHAVIOR_SECTION_LABEL,
                key = ColorPaletteSchemes.PROPERTY_GENERAL_BEHAVIOR_SECTION_KEY,
                description = ColorPaletteSchemes.PROPERTY_GENERAL_BEHAVIOR_SECTION_TOOLTIP)
        boolean generalBehaviorSection = true;

        @Preference(label = ColorPaletteSchemes.PROPERTY_GENERAL_CPD_LABEL,
                key = ColorPaletteSchemes.PROPERTY_GENERAL_CPD_KEY,
                description = ColorPaletteSchemes.PROPERTY_GENERAL_CPD_TOOLTIP,
                valueSet = {ColorPaletteSchemes.PROPERTY_GENERAL_CPD_OPTION1,
                        ColorPaletteSchemes.PROPERTY_GENERAL_CPD_OPTION2,
                        ColorPaletteSchemes.PROPERTY_GENERAL_CPD_OPTION3,
                        ColorPaletteSchemes.PROPERTY_GENERAL_CPD_OPTION4})
        String generalCpd = ColorPaletteSchemes.PROPERTY_GENERAL_CPD_DEFAULT;



        @Preference(label = ColorPaletteSchemes.PROPERTY_GENERAL_RANGE_LABEL,
                key = ColorPaletteSchemes.PROPERTY_GENERAL_RANGE_KEY,
                description = ColorPaletteSchemes.PROPERTY_GENERAL_RANGE_TOOLTIP,
                valueSet = {ColorPaletteSchemes.PROPERTY_GENERAL_RANGE_OPTION1,
                        ColorPaletteSchemes.PROPERTY_GENERAL_RANGE_OPTION2})
        String generalRange = ColorPaletteSchemes.PROPERTY_GENERAL_RANGE_DEFAULT;



        @Preference(label = ColorPaletteSchemes.PROPERTY_GENERAL_LOG_LABEL,
                key = ColorPaletteSchemes.PROPERTY_GENERAL_LOG_KEY,
                description = ColorPaletteSchemes.PROPERTY_GENERAL_LOG_TOOLTIP,
                valueSet = {ColorPaletteSchemes.PROPERTY_GENERAL_LOG_OPTION1,
                        ColorPaletteSchemes.PROPERTY_GENERAL_LOG_OPTION2,
                        ColorPaletteSchemes.PROPERTY_GENERAL_LOG_OPTION3})
        String generalLog = ColorPaletteSchemes.PROPERTY_GENERAL_LOG_DEFAULT;












        @Preference(label = ColorPaletteSchemes.PROPERTY_SCHEME_BEHAVIOR_SECTION_LABEL,
                key = ColorPaletteSchemes.PROPERTY_SCHEME_BEHAVIOR_SECTION_KEY,
                description = ColorPaletteSchemes.PROPERTY_SCHEME_BEHAVIOR_SECTION_TOOLTIP)
        boolean schemeBehaviorSection = true;




        @Preference(label = ColorPaletteSchemes.PROPERTY_AUTO_APPLY_SCHEMES_LABEL,
                key = ColorPaletteSchemes.PROPERTY_AUTO_APPLY_SCHEMES_KEY,
                description = ColorPaletteSchemes.PROPERTY_AUTO_APPLY_SCHEMES_TOOLTIP)
        boolean autoApplySchemes = ColorPaletteSchemes.PROPERTY_AUTO_APPLY_SCHEMES_DEFAULT;


        @Preference(label = ColorPaletteSchemes.PROPERTY_SCHEME_CPD_LABEL,
                key = ColorPaletteSchemes.PROPERTY_SCHEME_CPD_KEY,
                description = ColorPaletteSchemes.PROPERTY_SCHEME_CPD_TOOLTIP,
                valueSet = {ColorPaletteSchemes.PROPERTY_SCHEME_CPD_OPTION1,
                        ColorPaletteSchemes.PROPERTY_SCHEME_CPD_OPTION2,
                        ColorPaletteSchemes.PROPERTY_SCHEME_CPD_OPTION3,
                        ColorPaletteSchemes.PROPERTY_SCHEME_CPD_OPTION4,
                        ColorPaletteSchemes.PROPERTY_SCHEME_CPD_OPTION5,
                        ColorPaletteSchemes.PROPERTY_SCHEME_CPD_OPTION6})
        String schemeCpd = ColorPaletteSchemes.PROPERTY_SCHEME_CPD_DEFAULT;



        @Preference(label = ColorPaletteSchemes.PROPERTY_SCHEME_RANGE_LABEL,
                key = ColorPaletteSchemes.PROPERTY_SCHEME_RANGE_KEY,
                description = ColorPaletteSchemes.PROPERTY_SCHEME_RANGE_TOOLTIP,
                valueSet = {ColorPaletteSchemes.PROPERTY_SCHEME_RANGE_OPTION1,
                        ColorPaletteSchemes.PROPERTY_SCHEME_RANGE_OPTION2,
                        ColorPaletteSchemes.PROPERTY_SCHEME_RANGE_OPTION3})
        String schemeRange = ColorPaletteSchemes.PROPERTY_SCHEME_RANGE_DEFAULT;




        @Preference(label = ColorPaletteSchemes.PROPERTY_SCHEME_LOG_LABEL,
                key = ColorPaletteSchemes.PROPERTY_SCHEME_LOG_KEY,
                description = ColorPaletteSchemes.PROPERTY_SCHEME_LOG_TOOLTIP,
                valueSet = {ColorPaletteSchemes.PROPERTY_SCHEME_LOG_OPTION1,
                        ColorPaletteSchemes.PROPERTY_SCHEME_LOG_OPTION2,
                        ColorPaletteSchemes.PROPERTY_SCHEME_LOG_OPTION3,
                        ColorPaletteSchemes.PROPERTY_SCHEME_LOG_OPTION4})
        String schemeLog = ColorPaletteSchemes.PROPERTY_SCHEME_LOG_DEFAULT;










//
//        @Preference(label = ColorPaletteSchemes.PROPERTY_USE_COLOR_BLIND_CPD_LABEL,
//                key = ColorPaletteSchemes.PROPERTY_USE_COLOR_BLIND_CPD_KEY,
//                description = ColorPaletteSchemes.PROPERTY_USE_COLOR_BLIND_CPD_TOOLTIP)
//        boolean useColorBlindCpd = ColorPaletteSchemes.PROPERTY_USE_COLOR_BLIND_CPD_DEFAULT;







//        @Preference(label = ColorPaletteSchemes.PROPERTY_USE_SCHEME_PALETTE_STX_LABEL,
//                key = ColorPaletteSchemes.PROPERTY_USE_SCHEME_PALETTE_STX_KEY,
//                description = ColorPaletteSchemes.PROPERTY_USE_SCHEME_PALETTE_STX_TOOLTIP)
//        boolean useSchemePaletteStx = ColorPaletteSchemes.PROPERTY_USE_SCHEME_PALETTE_STX_DEFAULT;




        @Preference(label = ColorPaletteSchemes.PROPERTY_DEFAULT_CPD_SECTION_LABEL,
                key = ColorPaletteSchemes.PROPERTY_DEFAULT_CPD_SECTION_KEY,
                description = ColorPaletteSchemes.PROPERTY_DEFAULT_CPD_SECTION_TOOLTIP)
        boolean defaultPaletteSection = true;

        @Preference(label = ColorPaletteSchemes.PROPERTY_GRAY_SCALE_CPD_LABEL,
                key = ColorPaletteSchemes.PROPERTY_GRAY_SCALE_CPD_KEY,
                description = ColorPaletteSchemes.PROPERTY_GRAY_SCALE_CPD_TOOLTIP)
        String grayScaleCpd = ColorPaletteSchemes.PROPERTY_GRAY_SCALE_CPD_DEFAULT;

        @Preference(label = ColorPaletteSchemes.PROPERTY_STANDARD_COLOR_CPD_LABEL,
                key = ColorPaletteSchemes.PROPERTY_STANDARD_COLOR_CPD_KEY,
                description = ColorPaletteSchemes.PROPERTY_STANDARD_COLOR_CPD_TOOLTIP)
        String standardColorCpd = ColorPaletteSchemes.PROPERTY_STANDARD_COLOR_CPD_DEFAULT;


        @Preference(label = ColorPaletteSchemes.PROPERTY_COLOR_BLIND_CPD_LABEL,
                key = ColorPaletteSchemes.PROPERTY_COLOR_BLIND_CPD_KEY,
                description = ColorPaletteSchemes.PROPERTY_COLOR_BLIND_CPD_TOOLTIP)
        String colorBlindCpd = ColorPaletteSchemes.PROPERTY_COLOR_BLIND_CPD_DEFAULT;



        @Preference(label = ColorPaletteSchemes.PROPERTY_OTHER_CPD_LABEL,
                key = ColorPaletteSchemes.PROPERTY_OTHER_CPD_KEY,
                description = ColorPaletteSchemes.PROPERTY_OTHER_CPD_TOOLTIP)
        String otherCpd = ColorPaletteSchemes.PROPERTY_OTHER_CPD_DEFAULT;


        // Restore Defaults Section

        @Preference(label = ColorPaletteSchemes.PROPERTY_RESTORE_SECTION_LABEL,
                key = ColorPaletteSchemes.PROPERTY_RESTORE_SECTION_KEY,
                description = ColorPaletteSchemes.PROPERTY_RESTORE_SECTION_TOOLTIP)
        boolean restoreDefaultsSection = true;

        @Preference(label = ColorPaletteSchemes.PROPERTY_RESTORE_TO_DEFAULTS_LABEL,
                key = ColorPaletteSchemes.PROPERTY_RESTORE_DEFAULTS_NAME,
                description = ColorPaletteSchemes.PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = ColorPaletteSchemes.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT;
    }

}
