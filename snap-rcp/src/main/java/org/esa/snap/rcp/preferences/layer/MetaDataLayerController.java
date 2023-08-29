
package org.esa.snap.rcp.preferences.layer;

import com.bc.ceres.binding.*;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.core.layer.MetaDataLayerType;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * * Panel handling metadata layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author Daniel Knowles
 */


@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerMetaData",
        keywords = "#Options_Keywords_LayerMetaData",
        keywordsCategory = "Layer",
        id = "LayerMetaData")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerMetaData=Annotation Metadata Layer",
        "Options_Keywords_LayerMetaData=layer, annotation, metadata"
})
public final class MetaDataLayerController extends DefaultConfigController {

    Property restoreDefaults;


    boolean propertyValueChangeEventsEnabled = true;


    protected PropertySet createPropertySet() {
        return createPropertySet(new MetaDataLayerBean());
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
            Property fontNameProperty = context.getPropertySet().getProperty(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_KEY);
            fontNameProperty.getDescriptor().setDefaultValue(null);
            fontNameProperty.getDescriptor().setValueSet(new ValueSet(fontNameArray));
            fontNameProperty.getDescriptor().setDefaultValue(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_DEFAULT);
        } catch (Exception e) {
        }

        try {
            Property fontNameProperty = context.getPropertySet().getProperty(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_KEY);
            fontNameProperty.getDescriptor().setDefaultValue(null);
            fontNameProperty.getDescriptor().setValueSet(new ValueSet(fontNameArray));
            fontNameProperty.getDescriptor().setDefaultValue(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_DEFAULT);
        } catch (Exception e) {
        }

        try {
            Property fontNameProperty = context.getPropertySet().getProperty(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_KEY);
            fontNameProperty.getDescriptor().setDefaultValue(null);
            fontNameProperty.getDescriptor().setValueSet(new ValueSet(fontNameArray));
            fontNameProperty.getDescriptor().setDefaultValue(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_DEFAULT);
        } catch (Exception e) {
        }


        try {
            Property property = context.getPropertySet().getProperty(MetaDataLayerType.PROPERTY_MARGIN_LOCATION_KEY);
            property.getDescriptor().setDefaultValue(null);
            property.getDescriptor().setValueSet(new ValueSet(MetaDataLayerType.getMarginLocationArray()));
            property.getDescriptor().setDefaultValue(MetaDataLayerType.PROPERTY_MARGIN_LOCATION_DEFAULT);
        } catch (Exception e) {
        }

        try {
            Property property = context.getPropertySet().getProperty(MetaDataLayerType.PROPERTY_HEADER_LOCATION_KEY);
            property.getDescriptor().setDefaultValue(null);
            property.getDescriptor().setValueSet(new ValueSet(MetaDataLayerType.getHeaderLocationArray()));
            property.getDescriptor().setDefaultValue(MetaDataLayerType.PROPERTY_HEADER_LOCATION_DEFAULT);
        } catch (Exception e) {
        }

        try {
            Property property = context.getPropertySet().getProperty(MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_KEY);
            property.getDescriptor().setDefaultValue(null);
            property.getDescriptor().setValueSet(new ValueSet(MetaDataLayerType.getFooter2LocationArray()));
            property.getDescriptor().setDefaultValue(MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_DEFAULT);
        } catch (Exception e) {
        }


        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_SECTION_KEY, true);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_DEFAULT);
//        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_LOCATION_KEY, MetaDataLayerType.PROPERTY_MARGIN_LOCATION_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_GAP_KEY, MetaDataLayerType.PROPERTY_MARGIN_GAP_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_KEY, MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_KEY, MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEY, MetaDataLayerType.PROPERTY_MARGIN_METADATA_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_METADATA2_KEY, MetaDataLayerType.PROPERTY_MARGIN_METADATA2_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_METADATA3_KEY, MetaDataLayerType.PROPERTY_MARGIN_METADATA3_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_METADATA4_KEY, MetaDataLayerType.PROPERTY_MARGIN_METADATA4_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_METADATA5_KEY, MetaDataLayerType.PROPERTY_MARGIN_METADATA5_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_KEY, MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_KEY, MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_KEY, MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_KEY, MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_KEY, MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_DEFAULT);


        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_FORMATTING_SECTION_KEY, true);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_KEY, MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_KEY, MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_DEFAULT);
//        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_KEY, MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_KEY, MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_KEY, MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_DEFAULT);


        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_SECTION_KEY, true);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_DEFAULT);
//        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_LOCATION_KEY, MetaDataLayerType.PROPERTY_HEADER_LOCATION_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_GAP_KEY, MetaDataLayerType.PROPERTY_HEADER_GAP_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_KEY, MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_KEY);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_KEY, MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_KEY);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_KEY, MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_KEY);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_KEY, MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_KEY);


        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_FORMAT_SECTION_KEY, true);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_KEY, MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_KEY, MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_DEFAULT);
//        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_KEY, MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_KEY, MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_KEY, MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_DEFAULT);



        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_SECTION_KEY, true);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_DEFAULT);
//        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_KEY, MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_GAP_KEY, MetaDataLayerType.PROPERTY_FOOTER2_GAP_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_KEY, MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_KEY, MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_KEY, MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_KEY, MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_KEY, MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_DEFAULT);



        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_FORMATTING_SECTION_KEY, true);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_KEY, MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_KEY, MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_DEFAULT);
//        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_KEY, MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_KEY, MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_KEY, MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_DEFAULT);




        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MY_INFO_SECTION_KEY, true);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_KEY, MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_KEY, MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_KEY, MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_DEFAULT);
        initPropertyDefaults(context, MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_KEY, MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_DEFAULT);

        restoreDefaults =  initPropertyDefaults(context, MetaDataLayerType.PROPERTY_RESTORE_DEFAULTS_NAME, MetaDataLayerType.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT);


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

            context.setComponentsEnabled(MetaDataLayerType.PROPERTY_RESTORE_DEFAULTS_NAME, false);
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
                context.setComponentsEnabled(MetaDataLayerType.PROPERTY_RESTORE_DEFAULTS_NAME, !isDefaults(context));
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
        return new HelpCtx("options-metadatalayer");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class MetaDataLayerBean {


        // Margin Section

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_SECTION_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_SECTION_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_SECTION_TOOLTIP)
        boolean marginSection = true;


        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_SHOW_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_SHOW_TOOLTIP)
        boolean marginShowSection = MetaDataLayerType.PROPERTY_MARGIN_SHOW_DEFAULT;


        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_LOCATION_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_LOCATION_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_LOCATION_TOOLTIP)
        String marginLocation = MetaDataLayerType.PROPERTY_MARGIN_LOCATION_DEFAULT;


//        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_LOCATION_LABEL,
//                key = MetaDataLayerType.PROPERTY_MARGIN_LOCATION_KEY,
//                description = MetaDataLayerType.PROPERTY_MARGIN_LOCATION_TOOLTIP,
//                valueSet = {MetaDataLayerType.LOCATION_TOP_LEFT,
//                        MetaDataLayerType.LOCATION_TOP_CENTER,
//                        MetaDataLayerType.LOCATION_TOP_CENTER_JUSTIFY_LEFT,
//                        MetaDataLayerType.LOCATION_TOP_RIGHT,
//                        MetaDataLayerType.LOCATION_BOTTOM_LEFT,
//                        MetaDataLayerType.LOCATION_BOTTOM_CENTER,
//                        MetaDataLayerType.LOCATION_BOTTOM_CENTER_JUSTIFY_LEFT,
//                        MetaDataLayerType.LOCATION_BOTTOM_RIGHT,
//                        MetaDataLayerType.LOCATION_LEFT,
//                        MetaDataLayerType.LOCATION_RIGHT
//                })
//        String marginLocation = MetaDataLayerType.PROPERTY_MARGIN_LOCATION_DEFAULT;


        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_GAP_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_GAP_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_GAP_TOOLTIP,
                interval = MetaDataLayerType.PROPERTY_MARGIN_GAP_INTERVAL)
        double marginGap = MetaDataLayerType.PROPERTY_MARGIN_GAP_DEFAULT;


        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_TOOLTIP)
        String marginTextfield1 = MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_TOOLTIP)
        String marginTextfield2 = MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_METADATA_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_METADATA_TOOLTIP)
        String marginMetadata1 = MetaDataLayerType.PROPERTY_MARGIN_METADATA_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_METADATA2_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_METADATA2_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_METADATA2_TOOLTIP)
        String marginMetadata2 = MetaDataLayerType.PROPERTY_MARGIN_METADATA2_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_METADATA3_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_METADATA3_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_METADATA3_TOOLTIP)
        String marginMetadata3 = MetaDataLayerType.PROPERTY_MARGIN_METADATA3_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_METADATA4_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_METADATA4_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_METADATA4_TOOLTIP)
        String marginMetadata4 = MetaDataLayerType.PROPERTY_MARGIN_METADATA4_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_METADATA5_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_METADATA5_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_METADATA5_TOOLTIP)
        String marginMetadata5 = MetaDataLayerType.PROPERTY_MARGIN_METADATA5_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_TOOLTIP)
        String marginMetadataDelimiter = MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_TOOLTIP)
        boolean marginInfoShowAllSection = MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_TOOLTIP)
        boolean marginMetadataShowAllSection = MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_TOOLTIP)
        boolean isMarginMetadataProcessControlShowAllSection = MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_TOOLTIP)
        boolean marginBandMetadataShowAll = MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_DEFAULT;



        // Margin Format Section

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_FORMATTING_SECTION_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_FORMATTING_SECTION_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_FORMATTING_SECTION_TOOLTIP)
        boolean marginFormatSection = true;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_TOOLTIP,
                interval = MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_INTERVAL)
        int marginFontSize = MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_TOOLTIP)
        Color marginFontColor = MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_DEFAULT;


        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_TOOLTIP,
                valueSet = {MetaDataLayerType.PROPERTY_FONT_STYLE_1,
                        MetaDataLayerType.PROPERTY_FONT_STYLE_2,
                        MetaDataLayerType.PROPERTY_FONT_STYLE_3,
                        MetaDataLayerType.PROPERTY_FONT_STYLE_4})
        String marginFontStyle = MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_DEFAULT;


        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_TOOLTIP)
        boolean marginFontItalic = MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_LABEL,
                key = MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_KEY,
                description = MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_TOOLTIP)
        boolean marginFontBold = MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_DEFAULT;




        // Header Section

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_SECTION_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_SECTION_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_SECTION_TOOLTIP)
        boolean headerSection = true;

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_SHOW_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_SHOW_TOOLTIP)
        boolean headerShow = MetaDataLayerType.PROPERTY_HEADER_SHOW_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_LOCATION_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_LOCATION_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_LOCATION_TOOLTIP)
        String headerLocation = MetaDataLayerType.PROPERTY_HEADER_LOCATION_DEFAULT;


//        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_LOCATION_LABEL,
//                key = MetaDataLayerType.PROPERTY_HEADER_LOCATION_KEY,
//                description = MetaDataLayerType.PROPERTY_HEADER_LOCATION_TOOLTIP,
//                valueSet = {MetaDataLayerType.LOCATION_TOP_LEFT,
//                        MetaDataLayerType.LOCATION_TOP_CENTER,
//                        MetaDataLayerType.LOCATION_TOP_CENTER_JUSTIFY_LEFT,
//                        MetaDataLayerType.LOCATION_TOP_RIGHT,
//                        MetaDataLayerType.LOCATION_BOTTOM_LEFT,
//                        MetaDataLayerType.LOCATION_BOTTOM_CENTER,
//                        MetaDataLayerType.LOCATION_BOTTOM_CENTER_JUSTIFY_LEFT,
//                        MetaDataLayerType.LOCATION_BOTTOM_RIGHT,
//                        MetaDataLayerType.LOCATION_LEFT,
//                        MetaDataLayerType.LOCATION_RIGHT
//                })
//        String headerLocation = MetaDataLayerType.PROPERTY_HEADER_LOCATION_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_GAP_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_GAP_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_GAP_TOOLTIP,
                interval = MetaDataLayerType.PROPERTY_HEADER_GAP_INTERVAL)
        double headerGap = MetaDataLayerType.PROPERTY_HEADER_GAP_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_TOOLTIP)
        String headerTextfield = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_TOOLTIP)
        String headerTextfield2 = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_DEFAULT;


        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_TOOLTIP)
        String headerTextfield3 = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_TOOLTIP)
        String headerTextfield4 = MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_DEFAULT;




        // Header Format Section

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_FORMAT_SECTION_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_FORMAT_SECTION_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_FORMAT_SECTION_TOOLTIP)
        boolean headerFormatSection = true;

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_TOOLTIP,
                interval = MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_INTERVAL)
        int headerFontSize = MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_TOOLTIP)
        Color headerFontColor = MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_DEFAULT;


//        private String[] fontStyleValueSet = fontStyleValueSet();
//        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_LABEL,
//                key = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_KEY,
//                description = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_TOOLTIP,
//                valueSet = fontStyleValueSet)
//        String headerFontStyle = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_DEFAULT;



        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_TOOLTIP,
                valueSet = {MetaDataLayerType.PROPERTY_FONT_STYLE_1,
                        MetaDataLayerType.PROPERTY_FONT_STYLE_2,
                        MetaDataLayerType.PROPERTY_FONT_STYLE_3,
                        MetaDataLayerType.PROPERTY_FONT_STYLE_4})
        String headerFontStyle = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_DEFAULT;

//        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_LABEL,
//                key = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_KEY,
//                description = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_TOOLTIP)
//        String headerFontStyle = MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_TOOLTIP)
        boolean headerFontItalic = MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_LABEL,
                key = MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_KEY,
                description = MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_TOOLTIP)
        boolean headerFontBold = MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_DEFAULT;




        // Footer Section

        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_SECTION_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_SECTION_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_SECTION_TOOLTIP)
        boolean footerSection = true;

        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_SHOW_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_SHOW_TOOLTIP)
        boolean footerShow = MetaDataLayerType.PROPERTY_FOOTER2_SHOW_DEFAULT;


        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_TOOLTIP)
        String footerLocation = MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_DEFAULT;


//        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_LABEL,
//                key = MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_KEY,
//                description = MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_TOOLTIP,
//                valueSet = {MetaDataLayerType.LOCATION_TOP_LEFT,
//                        MetaDataLayerType.LOCATION_TOP_CENTER,
//                        MetaDataLayerType.LOCATION_TOP_CENTER_JUSTIFY_LEFT,
//                        MetaDataLayerType.LOCATION_TOP_RIGHT,
//                        MetaDataLayerType.LOCATION_BOTTOM_LEFT,
//                        MetaDataLayerType.LOCATION_BOTTOM_CENTER,
//                        MetaDataLayerType.LOCATION_BOTTOM_CENTER_JUSTIFY_LEFT,
//                        MetaDataLayerType.LOCATION_BOTTOM_RIGHT,
//                        MetaDataLayerType.LOCATION_LEFT,
//                        MetaDataLayerType.LOCATION_RIGHT
//                })
//        String footerLocation = MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_DEFAULT;


        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_GAP_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_GAP_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_GAP_TOOLTIP,
                interval = MetaDataLayerType.PROPERTY_FOOTER2_GAP_INTERVAL)
        double footerGap = MetaDataLayerType.PROPERTY_FOOTER2_GAP_DEFAULT;



        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_TOOLTIP)
        String footerTextfieldDefault = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_TOOLTIP)
        String footerTextfield2Default = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_TOOLTIP)
        String footerTextfield3Default = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_TOOLTIP)
        String footerTextfield4Default = MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_DEFAULT;

    @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_TOOLTIP)
        boolean footerMyInfoShowDefault = MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_DEFAULT;


    // Footer Format Section


        // Header Format Section

        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_FORMATTING_SECTION_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_FORMATTING_SECTION_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_FORMATTING_SECTION_ALIAS)
        boolean footerFormatSection = true;

        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_TOOLTIP,
                interval = MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_INTERVAL)
        int footerFontSize = MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_TOOLTIP)
        Color footerFontColor = MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_DEFAULT;


        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_TOOLTIP,
                valueSet = {MetaDataLayerType.PROPERTY_FONT_STYLE_1,
                        MetaDataLayerType.PROPERTY_FONT_STYLE_2,
                        MetaDataLayerType.PROPERTY_FONT_STYLE_3,
                        MetaDataLayerType.PROPERTY_FONT_STYLE_4})
        String footerFontStyle = MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_DEFAULT;


        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_TOOLTIP)
        boolean footerFontItalic = MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_LABEL,
                key = MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_KEY,
                description = MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_TOOLTIP)
        boolean footerFontBold = MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_DEFAULT;





        // My Info Section

        @Preference(label = MetaDataLayerType.PROPERTY_MY_INFO_SECTION_LABEL,
                key = MetaDataLayerType.PROPERTY_MY_INFO_SECTION_KEY,
                description = MetaDataLayerType.PROPERTY_MY_INFO_SECTION_TOOLTIP)
        boolean myInfoSection = true;

        @Preference(label = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_LABEL,
                key = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_KEY,
                description = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_TOOLTIP)
        String myInfo1 = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_LABEL,
                key = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_KEY,
                description = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_TOOLTIP)
        String myInfo2 = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_LABEL,
                key = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_KEY,
                description = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_TOOLTIP)
        String myInfo3 = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_DEFAULT;

        @Preference(label = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_LABEL,
                key = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_KEY,
                description = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_TOOLTIP)
        String myInfo4 = MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_DEFAULT;


        // Restore Defaults Section

        @Preference(label = MetaDataLayerType.PROPERTY_RESTORE_TO_DEFAULTS_LABEL,
                key = MetaDataLayerType.PROPERTY_RESTORE_DEFAULTS_NAME,
                description = MetaDataLayerType.PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = MetaDataLayerType.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT;

        private String[] fontStyleValueSet() {
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
            return fontNameArray;
        }
    }

}
