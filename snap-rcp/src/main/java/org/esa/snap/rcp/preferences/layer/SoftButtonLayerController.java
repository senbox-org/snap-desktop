
package org.esa.snap.rcp.preferences.layer;

import com.bc.ceres.binding.*;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.core.layer.MetaDataLayerType;
import org.esa.snap.rcp.actions.layer.overlay.OverlaySoftButtonLayerAction;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;

/**
 * * Panel handling soft button layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author Daniel Knowles
 */


@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerSoftButton",
        keywords = "#Options_Keywords_LayerSoftButton",
        keywordsCategory = "Layer",
        id = "LayerSoftButton")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerSoftButton=Soft Button Layer(s)",
        "Options_Keywords_LayerSoftButton=layer, annotation, metadata, colorbar legend, soft button"
})
public final class SoftButtonLayerController extends DefaultConfigController {

    Property restoreDefaults;


    boolean propertyValueChangeEventsEnabled = true;


    protected PropertySet createPropertySet() {
        return createPropertySet(new SoftButtonLayerBean());
    }



    @Override
    protected JPanel createPanel(BindingContext context) {

        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT);

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
        return new HelpCtx("annotationMetadataOverlay");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class SoftButtonLayerBean {


        // Margin Section

        @Preference(label = OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_TOOLTIP)
        boolean showAnnotationOverlayDefault = OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_DEFAULT;

        @Preference(label = OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_TOOLTIP)
        boolean showGridlinesOverlayDefault = OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_DEFAULT;

        @Preference(label = OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_TOOLTIP)
        boolean showNoDataOverlayDefault = OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_DEFAULT;


        @Preference(label = OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_TOOLTIP)
        boolean showColorBarLegendOverlayDefault = OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT;



        // Restore Defaults Section

        @Preference(label = MetaDataLayerType.PROPERTY_RESTORE_TO_DEFAULTS_LABEL,
                key = MetaDataLayerType.PROPERTY_RESTORE_DEFAULTS_NAME,
                description = MetaDataLayerType.PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = MetaDataLayerType.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT;

    }

}
