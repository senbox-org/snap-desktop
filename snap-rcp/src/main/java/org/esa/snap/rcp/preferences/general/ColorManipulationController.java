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
import org.esa.snap.core.datamodel.ColorSchemeDefaults;
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
        "Options_DisplayName_ColorManipulation=" + ColorSchemeDefaults.TOOLNAME_COLOR_MANIPULATION,
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

        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_GENERAL_SECTION_KEY, true);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_GENERAL_CPD_KEY, ColorSchemeDefaults.PROPERTY_GENERAL_CPD_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_GENERAL_RANGE_KEY, ColorSchemeDefaults.PROPERTY_GENERAL_RANGE_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_GENERAL_LOG_KEY, ColorSchemeDefaults.PROPERTY_GENERAL_LOG_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_RESTORE_SECTION_KEY, true);

        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_SCHEME_SECTION_KEY, true);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_SCHEME_AUTO_APPLY_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_AUTO_APPLY_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_SCHEME_CPD_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_CPD_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_SCHEME_LOG_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_LOG_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_SCHEME_VERBOSE_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_VERBOSE_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_SCHEME_SHOW_DISABLED_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_SHOW_DISABLED_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_SCHEME_SORT_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_SORT_DEFAULT);

        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_CPD_SECTION_KEY, true);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_CPD_STANDARD_KEY, ColorSchemeDefaults.PROPERTY_CPD_STANDARD_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_CPD_UNIVERSAL_KEY, ColorSchemeDefaults.PROPERTY_CPD_UNIVERSAL_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_CPD_GRAY_SCALE_KEY, ColorSchemeDefaults.PROPERTY_CPD_GRAY_SCALE_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_CPD_ANOMALIES_KEY, ColorSchemeDefaults.PROPERTY_CPD_ANOMALIES_DEFAULT);

        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_SECTION_KEY, true);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_98_KEY, ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_98_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_95_KEY, ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_95_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_90_KEY, ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_90_DEFAULT);
        initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_RANGE_ZOOM_IN_KEY, ColorSchemeDefaults.PROPERTY_RANGE_ZOOM_IN_DEFAULT);

        restoreDefaults =  initPropertyDefaults(context, ColorSchemeDefaults.PROPERTY_RESTORE_DEFAULTS_NAME, ColorSchemeDefaults.PROPERTY_RESTORE_DEFAULTS_DEFAULT);




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

            context.setComponentsEnabled(ColorSchemeDefaults.PROPERTY_RESTORE_DEFAULTS_NAME, false);
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
                context.setComponentsEnabled(ColorSchemeDefaults.PROPERTY_RESTORE_DEFAULTS_NAME, !isDefaults(context));
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

        @Preference(label = ColorSchemeDefaults.PROPERTY_GENERAL_SECTION_LABEL,
                key = ColorSchemeDefaults.PROPERTY_GENERAL_SECTION_KEY,
                description = ColorSchemeDefaults.PROPERTY_GENERAL_SECTION_TOOLTIP)
        boolean generalBehaviorSection = true;

        @Preference(label = ColorSchemeDefaults.PROPERTY_GENERAL_CPD_LABEL,
                key = ColorSchemeDefaults.PROPERTY_GENERAL_CPD_KEY,
                description = ColorSchemeDefaults.PROPERTY_GENERAL_CPD_TOOLTIP,
                valueSet = {ColorSchemeDefaults.PROPERTY_GENERAL_CPD_OPTION1,
                        ColorSchemeDefaults.PROPERTY_GENERAL_CPD_OPTION2,
                        ColorSchemeDefaults.PROPERTY_GENERAL_CPD_OPTION3,
                        ColorSchemeDefaults.PROPERTY_GENERAL_CPD_OPTION4})
        String generalCpd = ColorSchemeDefaults.PROPERTY_GENERAL_CPD_DEFAULT;



        @Preference(label = ColorSchemeDefaults.PROPERTY_GENERAL_RANGE_LABEL,
                key = ColorSchemeDefaults.PROPERTY_GENERAL_RANGE_KEY,
                description = ColorSchemeDefaults.PROPERTY_GENERAL_RANGE_TOOLTIP,
                valueSet = {ColorSchemeDefaults.PROPERTY_GENERAL_RANGE_OPTION1,
                        ColorSchemeDefaults.PROPERTY_GENERAL_RANGE_OPTION2})
        String generalRange = ColorSchemeDefaults.PROPERTY_GENERAL_RANGE_DEFAULT;



        @Preference(label = ColorSchemeDefaults.PROPERTY_GENERAL_LOG_LABEL,
                key = ColorSchemeDefaults.PROPERTY_GENERAL_LOG_KEY,
                description = ColorSchemeDefaults.PROPERTY_GENERAL_LOG_TOOLTIP,
                valueSet = {ColorSchemeDefaults.PROPERTY_GENERAL_LOG_OPTION1,
                        ColorSchemeDefaults.PROPERTY_GENERAL_LOG_OPTION2,
                        ColorSchemeDefaults.PROPERTY_GENERAL_LOG_OPTION3})
        String generalLog = ColorSchemeDefaults.PROPERTY_GENERAL_LOG_DEFAULT;



        @Preference(label = ColorSchemeDefaults.PROPERTY_SCHEME_SECTION_LABEL,
                key = ColorSchemeDefaults.PROPERTY_SCHEME_SECTION_KEY,
                description = ColorSchemeDefaults.PROPERTY_SCHEME_SECTION_TOOLTIP)
        boolean schemeBehaviorSection = true;




        @Preference(label = ColorSchemeDefaults.PROPERTY_SCHEME_AUTO_APPLY_LABEL,
                key = ColorSchemeDefaults.PROPERTY_SCHEME_AUTO_APPLY_KEY,
                description = ColorSchemeDefaults.PROPERTY_SCHEME_AUTO_APPLY_TOOLTIP)
        boolean autoApplySchemes = ColorSchemeDefaults.PROPERTY_SCHEME_AUTO_APPLY_DEFAULT;


        @Preference(label = ColorSchemeDefaults.PROPERTY_SCHEME_CPD_LABEL,
                key = ColorSchemeDefaults.PROPERTY_SCHEME_CPD_KEY,
                description = ColorSchemeDefaults.PROPERTY_SCHEME_CPD_TOOLTIP,
                valueSet = {ColorSchemeDefaults.PROPERTY_SCHEME_CPD_OPTION1,
                        ColorSchemeDefaults.PROPERTY_SCHEME_CPD_OPTION2,
                        ColorSchemeDefaults.PROPERTY_SCHEME_CPD_OPTION3,
                        ColorSchemeDefaults.PROPERTY_SCHEME_CPD_OPTION4,
                        ColorSchemeDefaults.PROPERTY_SCHEME_CPD_OPTION5,
                        ColorSchemeDefaults.PROPERTY_SCHEME_CPD_OPTION6})
        String schemeCpd = ColorSchemeDefaults.PROPERTY_SCHEME_CPD_DEFAULT;



        @Preference(label = ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_LABEL,
                key = ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_KEY,
                description = ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_TOOLTIP,
                valueSet = {ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_OPTION1,
                        ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_OPTION2,
                        ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_OPTION3})
        String schemeRange = ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_DEFAULT;


        @Preference(label = ColorSchemeDefaults.PROPERTY_SCHEME_LOG_LABEL,
                key = ColorSchemeDefaults.PROPERTY_SCHEME_LOG_KEY,
                description = ColorSchemeDefaults.PROPERTY_SCHEME_LOG_TOOLTIP,
                valueSet = {ColorSchemeDefaults.PROPERTY_SCHEME_LOG_OPTION1,
                        ColorSchemeDefaults.PROPERTY_SCHEME_LOG_OPTION2,
                        ColorSchemeDefaults.PROPERTY_SCHEME_LOG_OPTION3,
                        ColorSchemeDefaults.PROPERTY_SCHEME_LOG_OPTION4})
        String schemeLog = ColorSchemeDefaults.PROPERTY_SCHEME_LOG_DEFAULT;


        @Preference(label = ColorSchemeDefaults.PROPERTY_SCHEME_VERBOSE_LABEL,
                key = ColorSchemeDefaults.PROPERTY_SCHEME_VERBOSE_KEY,
                description = ColorSchemeDefaults.PROPERTY_SCHEME_VERBOSE_TOOLTIP)
        boolean schemeVerbose = ColorSchemeDefaults.PROPERTY_SCHEME_VERBOSE_DEFAULT;

        @Preference(label = ColorSchemeDefaults.PROPERTY_SCHEME_SHOW_DISABLED_LABEL,
                key = ColorSchemeDefaults.PROPERTY_SCHEME_SHOW_DISABLED_KEY,
                description = ColorSchemeDefaults.PROPERTY_SCHEME_SHOW_DISABLED_TOOLTIP)
        boolean schemeShowDisabled = ColorSchemeDefaults.PROPERTY_SCHEME_SHOW_DISABLED_DEFAULT;

        @Preference(label = ColorSchemeDefaults.PROPERTY_SCHEME_SORT_LABEL,
                key = ColorSchemeDefaults.PROPERTY_SCHEME_SORT_KEY,
                description = ColorSchemeDefaults.PROPERTY_SCHEME_SORT_TOOLTIP)
        boolean schemeSort = ColorSchemeDefaults.PROPERTY_SCHEME_SORT_DEFAULT;


        @Preference(label = ColorSchemeDefaults.PROPERTY_CPD_SECTION_LABEL,
                key = ColorSchemeDefaults.PROPERTY_CPD_SECTION_KEY,
                description = ColorSchemeDefaults.PROPERTY_CPD_SECTION_TOOLTIP)
        boolean defaultPaletteSection = true;

        @Preference(label = ColorSchemeDefaults.PROPERTY_CPD_GRAY_SCALE_LABEL,
                key = ColorSchemeDefaults.PROPERTY_CPD_GRAY_SCALE_KEY,
                description = ColorSchemeDefaults.PROPERTY_CPD_GRAY_SCALE_TOOLTIP)
        String grayScaleCpd = ColorSchemeDefaults.PROPERTY_CPD_GRAY_SCALE_DEFAULT;

        @Preference(label = ColorSchemeDefaults.PROPERTY_CPD_STANDARD_LABEL,
                key = ColorSchemeDefaults.PROPERTY_CPD_STANDARD_KEY,
                description = ColorSchemeDefaults.PROPERTY_CPD_STANDARD_TOOLTIP)
        String standardColorCpd = ColorSchemeDefaults.PROPERTY_CPD_STANDARD_DEFAULT;


        @Preference(label = ColorSchemeDefaults.PROPERTY_CPD_UNIVERSAL_LABEL,
                key = ColorSchemeDefaults.PROPERTY_CPD_UNIVERSAL_KEY,
                description = ColorSchemeDefaults.PROPERTY_CPD_UNIVERSAL_TOOLTIP)
        String colorBlindCpd = ColorSchemeDefaults.PROPERTY_CPD_UNIVERSAL_DEFAULT;



        @Preference(label = ColorSchemeDefaults.PROPERTY_CPD_ANOMALIES_LABEL,
                key = ColorSchemeDefaults.PROPERTY_CPD_ANOMALIES_KEY,
                description = ColorSchemeDefaults.PROPERTY_CPD_ANOMALIES_TOOLTIP)
        String otherCpd = ColorSchemeDefaults.PROPERTY_CPD_ANOMALIES_DEFAULT;


        // Range Option Section

        @Preference(label = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_SECTION_LABEL,
                key = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_SECTION_KEY,
                description = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_SECTION_TOOLTIP)
        boolean rangeOptionsSection = true;

        @Preference(label = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_98_LABEL,
                key = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_98_KEY,
                description = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_98_TOOLTIP)
        boolean range98 = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_98_DEFAULT;

        @Preference(label = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_95_LABEL,
                key = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_95_KEY,
                description = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_95_TOOLTIP)
        boolean range95 = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_95_DEFAULT;

        @Preference(label = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_90_LABEL,
                key = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_90_KEY,
                description = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_90_TOOLTIP)
        boolean range90 = ColorSchemeDefaults.PROPERTY_RANGE_BUTTON_90_DEFAULT;

        @Preference(label = ColorSchemeDefaults.PROPERTY_RANGE_ZOOM_IN_LABEL,
                key = ColorSchemeDefaults.PROPERTY_RANGE_ZOOM_IN_KEY,
                description = ColorSchemeDefaults.PROPERTY_RANGE_ZOOM_IN_TOOLTIP)
        boolean sliderZoom = ColorSchemeDefaults.PROPERTY_RANGE_ZOOM_IN_DEFAULT;




        // Restore Defaults Section

        @Preference(label = ColorSchemeDefaults.PROPERTY_RESTORE_SECTION_LABEL,
                key = ColorSchemeDefaults.PROPERTY_RESTORE_SECTION_KEY,
                description = ColorSchemeDefaults.PROPERTY_RESTORE_SECTION_TOOLTIP)
        boolean restoreDefaultsSection = true;

        @Preference(label = ColorSchemeDefaults.PROPERTY_RESTORE_DEFAULTS_LABEL,
                key = ColorSchemeDefaults.PROPERTY_RESTORE_DEFAULTS_NAME,
                description = ColorSchemeDefaults.PROPERTY_RESTORE_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = ColorSchemeDefaults.PROPERTY_RESTORE_DEFAULTS_DEFAULT;
    }

}
