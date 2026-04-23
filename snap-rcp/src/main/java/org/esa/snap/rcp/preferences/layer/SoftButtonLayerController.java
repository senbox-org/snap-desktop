
package org.esa.snap.rcp.preferences.layer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
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


@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_LayerSoftButton",
        keywords = "#Options_Keywords_LayerSoftButton",
        keywordsCategory = "Layer",
        id = "LayerSoftButton",
        position = 5)
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerSoftButton=Soft Button",
        "Options_Keywords_LayerSoftButton=layer, annotation, metadata, colorbar legend, soft button"
})
public final class SoftButtonLayerController extends DefaultConfigController {

    Property restoreDefaults;


    boolean propertyValueChangeEventsEnabled = true;

    Property zoomPosSelector;



    protected PropertySet createPropertySet() {
        return createPropertySet(new SoftButtonLayerBean());
    }



    @Override
    protected JPanel createPanel(BindingContext context) {

        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_STATE_KEY, OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_STATE_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_STATE_KEY, OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_STATE_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.MASK_LIST_KEY, OverlaySoftButtonLayerAction.MASK_LIST_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_PINS_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_PINS_OVERLAY_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_GCP_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_GCP_OVERLAY_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_GEOMETRY_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_GEOMETRY_OVERLAY_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_MASK_PARENT_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_MASK_PARENT_OVERLAY_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_MASK_LIST_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_MASK_LIST_OVERLAY_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_VECTOR_PARENT_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_VECTOR_PARENT_OVERLAY_DEFAULT);
        zoomPosSelector = initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_STATE_KEY, OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_STATE_DEFAULT);

        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_ZOOM1_SECTION_KEY, true);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_ZOOM2_SECTION_KEY, true);

        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_1_KEY, OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_1_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_2_KEY, OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_2_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_CENTERX_1_KEY, OverlaySoftButtonLayerAction.SET_CENTERX_1_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_CENTERX_2_KEY, OverlaySoftButtonLayerAction.SET_CENTERX_2_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_CENTERY_1_KEY, OverlaySoftButtonLayerAction.SET_CENTERY_1_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_CENTERY_2_KEY, OverlaySoftButtonLayerAction.SET_CENTERY_2_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_SHIFTX_1_KEY, OverlaySoftButtonLayerAction.SET_SHIFTX_1_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_SHIFTX_2_KEY, OverlaySoftButtonLayerAction.SET_SHIFTX_2_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_SHIFTY_1_KEY, OverlaySoftButtonLayerAction.SET_SHIFTY_1_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SET_SHIFTY_2_KEY, OverlaySoftButtonLayerAction.SET_SHIFTY_2_DEFAULT);
        initPropertyDefaults(context, OverlaySoftButtonLayerAction.SHOW_IN_ALL_BANDS_OVERLAY_KEY, OverlaySoftButtonLayerAction.SHOW_IN_ALL_BANDS_OVERLAY_DEFAULT);

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


        handleZoomPosSelectorEnablement(context);

        zoomPosSelector.addPropertyChangeListener(evt -> {
            handleZoomPosSelectorEnablement(context);
        });


        return parent;
    }


    private void handleZoomPosSelectorEnablement(BindingContext context) {

        String mode = zoomPosSelector.getValue();

        boolean zoom1Enabled = false;
        boolean zoom2Enabled = false;

        switch (mode) {
            case OverlaySoftButtonLayerAction.STATE_ZOOM1_ZOOM2:
                zoom1Enabled = true;
                zoom2Enabled = true;
                break;

            case OverlaySoftButtonLayerAction.STATE_ZOOM2_ZOOM1:
                zoom1Enabled = true;
                zoom2Enabled = true;
                break;

            case OverlaySoftButtonLayerAction.STATE_ZOOM1_ZOOM1:
                zoom1Enabled = true;
                break;

            case OverlaySoftButtonLayerAction.STATE_ZOOM2_ZOOM2:
                zoom2Enabled = true;
                break;

            case OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ZOOM1:
                zoom1Enabled = true;
                break;

            case OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ZOOM2:
                zoom2Enabled = true;
                break;
        }


        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_ZOOM1_SECTION_KEY, zoom1Enabled);
        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_1_KEY, zoom1Enabled);
        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_CENTERX_1_KEY, zoom1Enabled);
        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_CENTERY_1_KEY, zoom1Enabled);
        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_SHIFTX_1_KEY, zoom1Enabled);
        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_SHIFTY_1_KEY, zoom1Enabled);


        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_ZOOM2_SECTION_KEY, zoom2Enabled);
        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_2_KEY, zoom2Enabled);
        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_CENTERX_2_KEY, zoom2Enabled);
        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_CENTERY_2_KEY, zoom2Enabled);
        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_SHIFTX_2_KEY, zoom2Enabled);
        context.setComponentsEnabled(OverlaySoftButtonLayerAction.SET_SHIFTY_2_KEY, zoom2Enabled);

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



        @Preference(label = OverlaySoftButtonLayerAction.SHOW_IN_ALL_BANDS_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_IN_ALL_BANDS_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_IN_ALL_BANDS_OVERLAY_TOOLTIP)
        boolean showInAllBandsOverlayDefault = OverlaySoftButtonLayerAction.SHOW_IN_ALL_BANDS_OVERLAY_DEFAULT;


        @Preference(label = OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_STATE_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_STATE_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_STATE_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
                        OverlaySoftButtonLayerAction.STATE_ON_OFF,
                        OverlaySoftButtonLayerAction.STATE_OFF_ON,
                        OverlaySoftButtonLayerAction.STATE_ON_ON,
                        OverlaySoftButtonLayerAction.STATE_OFF_OFF,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ON,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_OFF})
        String showGridlinesOverlayStateDefault = OverlaySoftButtonLayerAction.SHOW_GRIDLINES_OVERLAY_STATE_DEFAULT;




        @Preference(label = OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
                        OverlaySoftButtonLayerAction.STATE_ON_OFF,
                        OverlaySoftButtonLayerAction.STATE_OFF_ON,
                        OverlaySoftButtonLayerAction.STATE_ON_ON,
                        OverlaySoftButtonLayerAction.STATE_OFF_OFF,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ON,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_OFF})
        String showColorBarLegendOverlayDefault = OverlaySoftButtonLayerAction.SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT;




        @Preference(label = OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_STATE_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_STATE_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_STATE_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
                        OverlaySoftButtonLayerAction.STATE_ON_OFF,
                        OverlaySoftButtonLayerAction.STATE_OFF_ON,
                        OverlaySoftButtonLayerAction.STATE_ON_ON,
                        OverlaySoftButtonLayerAction.STATE_OFF_OFF,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ON,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_OFF})
        String showAnnotationOverlayStateDefault = OverlaySoftButtonLayerAction.SHOW_ANNOTATION_OVERLAY_STATE_DEFAULT;



        @Preference(label = OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
                        OverlaySoftButtonLayerAction.STATE_ON_OFF,
                        OverlaySoftButtonLayerAction.STATE_OFF_ON,
                        OverlaySoftButtonLayerAction.STATE_ON_ON,
                        OverlaySoftButtonLayerAction.STATE_OFF_OFF,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ON,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_OFF})
        String showNoDataOverlayDefault = OverlaySoftButtonLayerAction.SHOW_NO_DATA_OVERLAY_DEFAULT;


        @Preference(label = OverlaySoftButtonLayerAction.SHOW_MASK_PARENT_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_MASK_PARENT_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_MASK_PARENT_OVERLAY_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
                        OverlaySoftButtonLayerAction.STATE_ON_OFF,
                        OverlaySoftButtonLayerAction.STATE_OFF_ON,
                        OverlaySoftButtonLayerAction.STATE_ON_ON,
                        OverlaySoftButtonLayerAction.STATE_OFF_OFF,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ON,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_OFF})
        String showMaskParentOverlayDefault = OverlaySoftButtonLayerAction.SHOW_MASK_PARENT_OVERLAY_DEFAULT;


        @Preference(label = OverlaySoftButtonLayerAction.SHOW_MASK_LIST_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_MASK_LIST_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_MASK_LIST_OVERLAY_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
                        OverlaySoftButtonLayerAction.STATE_ON_OFF,
                        OverlaySoftButtonLayerAction.STATE_OFF_ON,
                        OverlaySoftButtonLayerAction.STATE_ON_ON,
                        OverlaySoftButtonLayerAction.STATE_OFF_OFF,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ON,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_OFF})
        String showMaskListOverlayDefault = OverlaySoftButtonLayerAction.SHOW_MASK_LIST_OVERLAY_DEFAULT;



        @Preference(label = OverlaySoftButtonLayerAction.MASK_LIST_LABEL,
                key = OverlaySoftButtonLayerAction.MASK_LIST_KEY,
                description = OverlaySoftButtonLayerAction.MASK_LIST_TOOLTIP)
        String maskListShowDefault = OverlaySoftButtonLayerAction.MASK_LIST_DEFAULT;




        @Preference(label = OverlaySoftButtonLayerAction.SHOW_VECTOR_PARENT_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_VECTOR_PARENT_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_VECTOR_PARENT_OVERLAY_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
                        OverlaySoftButtonLayerAction.STATE_ON_OFF,
                        OverlaySoftButtonLayerAction.STATE_OFF_ON,
                        OverlaySoftButtonLayerAction.STATE_ON_ON,
                        OverlaySoftButtonLayerAction.STATE_OFF_OFF,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ON,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_OFF})
        String showVectorParentOverlayDefault = OverlaySoftButtonLayerAction.SHOW_VECTOR_PARENT_OVERLAY_DEFAULT;

        @Preference(label = OverlaySoftButtonLayerAction.SHOW_GEOMETRY_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_GEOMETRY_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_GEOMETRY_OVERLAY_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
                        OverlaySoftButtonLayerAction.STATE_ON_OFF,
                        OverlaySoftButtonLayerAction.STATE_OFF_ON,
                        OverlaySoftButtonLayerAction.STATE_ON_ON,
                        OverlaySoftButtonLayerAction.STATE_OFF_OFF,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ON,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_OFF})
        String showGeometryOverlayDefault = OverlaySoftButtonLayerAction.SHOW_GEOMETRY_OVERLAY_DEFAULT;



        @Preference(label = OverlaySoftButtonLayerAction.SHOW_PINS_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_PINS_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_PINS_OVERLAY_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
                        OverlaySoftButtonLayerAction.STATE_ON_OFF,
                        OverlaySoftButtonLayerAction.STATE_OFF_ON,
                        OverlaySoftButtonLayerAction.STATE_ON_ON,
                        OverlaySoftButtonLayerAction.STATE_OFF_OFF,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ON,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_OFF})
        String showPinsOverlayDefault = OverlaySoftButtonLayerAction.SHOW_PINS_OVERLAY_DEFAULT;


        @Preference(label = OverlaySoftButtonLayerAction.SHOW_GCP_OVERLAY_LABEL,
                key = OverlaySoftButtonLayerAction.SHOW_GCP_OVERLAY_KEY,
                description = OverlaySoftButtonLayerAction.SHOW_GCP_OVERLAY_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
                        OverlaySoftButtonLayerAction.STATE_ON_OFF,
                        OverlaySoftButtonLayerAction.STATE_OFF_ON,
                        OverlaySoftButtonLayerAction.STATE_ON_ON,
                        OverlaySoftButtonLayerAction.STATE_OFF_OFF,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ON,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_OFF})
        String showGcpOverlayDefault = OverlaySoftButtonLayerAction.SHOW_GCP_OVERLAY_DEFAULT;





        @Preference(label = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_STATE_LABEL,
                key = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_STATE_KEY,
                description = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_STATE_TOOLTIP,
                valueSet = {OverlaySoftButtonLayerAction.STATE_UNASSIGNED,
//                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ZOOM_DEFAULT,
//                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ZOOM_ALL,

//                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ZOOM2,
//                        OverlaySoftButtonLayerAction.STATE_ZOOM_DEFAULT_DEFAULT,
//                        OverlaySoftButtonLayerAction.STATE_ZOOM_DEFAULT_ALL,
//                        OverlaySoftButtonLayerAction.STATE_ZOOM_DEFAULT_ZOOM1,
//                        OverlaySoftButtonLayerAction.STATE_ZOOM_ALL_ZOOM_DEFAULT,
//                        OverlaySoftButtonLayerAction.STATE_ZOOM_ALL_ALL,
//                        OverlaySoftButtonLayerAction.STATE_ZOOM_ALL_ZOOM1,
//                        OverlaySoftButtonLayerAction.STATE_ZOOM1_ZOOM_DEFAULT,
//                        OverlaySoftButtonLayerAction.STATE_ZOOM1_ZOOM1,
//                        OverlaySoftButtonLayerAction.STATE_ZOOM1_ALL,
                        OverlaySoftButtonLayerAction.STATE_ZOOM1_ZOOM2,
                        OverlaySoftButtonLayerAction.STATE_ZOOM2_ZOOM1,
                        OverlaySoftButtonLayerAction.STATE_ZOOM1_ZOOM1,
                        OverlaySoftButtonLayerAction.STATE_ZOOM2_ZOOM2,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ZOOM1,
                        OverlaySoftButtonLayerAction.STATE_UNASSIGNED_ZOOM2

        })
        String setZoomFactorStateDefault = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_STATE_DEFAULT;


        @Preference(label = OverlaySoftButtonLayerAction.SET_ZOOM1_SECTION_LABEL,
                key = OverlaySoftButtonLayerAction.SET_ZOOM1_SECTION_KEY,
                description = OverlaySoftButtonLayerAction.SET_ZOOM1_SECTION_TOOLTIP)
        boolean zoom1Section = true;


        @Preference(label = OverlaySoftButtonLayerAction.SET_CENTERX_1_LABEL,
                key = OverlaySoftButtonLayerAction.SET_CENTERX_1_KEY,
                description = OverlaySoftButtonLayerAction.SET_CENTERX_1_TOOLTIP)
        boolean setCenterx1Default = OverlaySoftButtonLayerAction.SET_CENTERX_1_DEFAULT;

        @Preference(label = OverlaySoftButtonLayerAction.SET_CENTERY_1_LABEL,
                key = OverlaySoftButtonLayerAction.SET_CENTERY_1_KEY,
                description = OverlaySoftButtonLayerAction.SET_CENTERY_1_TOOLTIP)
        boolean setCentery1Default = OverlaySoftButtonLayerAction.SET_CENTERY_1_DEFAULT;


        @Preference(label = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_1_LABEL,
                key = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_1_KEY,
                description = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_1_TOOLTIP)
        double setZoomFactor1Default = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_1_DEFAULT;

        @Preference(label = OverlaySoftButtonLayerAction.SET_SHIFTX_1_LABEL,
                key = OverlaySoftButtonLayerAction.SET_SHIFTX_1_KEY,
                description = OverlaySoftButtonLayerAction.SET_SHIFTX_1_TOOLTIP)
        double setShiftx1Tooltip = OverlaySoftButtonLayerAction.SET_SHIFTX_1_DEFAULT;

        @Preference(label = OverlaySoftButtonLayerAction.SET_SHIFTY_1_LABEL,
                key = OverlaySoftButtonLayerAction.SET_SHIFTY_1_KEY,
                description = OverlaySoftButtonLayerAction.SET_SHIFTY_1_TOOLTIP)
        double setShifty1Default = OverlaySoftButtonLayerAction.SET_SHIFTY_1_DEFAULT;



        @Preference(label = OverlaySoftButtonLayerAction.SET_ZOOM2_SECTION_LABEL,
                key = OverlaySoftButtonLayerAction.SET_ZOOM2_SECTION_KEY,
                description = OverlaySoftButtonLayerAction.SET_ZOOM2_SECTION_TOOLTIP)
        boolean zoom2Section = true;

        @Preference(label = OverlaySoftButtonLayerAction.SET_CENTERX_2_LABEL,
                key = OverlaySoftButtonLayerAction.SET_CENTERX_2_KEY,
                description = OverlaySoftButtonLayerAction.SET_CENTERX_2_TOOLTIP)
        boolean setCenterx2Default = OverlaySoftButtonLayerAction.SET_CENTERX_2_DEFAULT;

        @Preference(label = OverlaySoftButtonLayerAction.SET_CENTERY_2_LABEL,
                key = OverlaySoftButtonLayerAction.SET_CENTERY_2_KEY,
                description = OverlaySoftButtonLayerAction.SET_CENTERY_2_TOOLTIP)
        boolean setCentery2Default = OverlaySoftButtonLayerAction.SET_CENTERY_2_DEFAULT;

        @Preference(label = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_2_LABEL,
                key = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_2_KEY,
                description = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_2_TOOLTIP)
        double setZoomFactor2Default = OverlaySoftButtonLayerAction.SET_ZOOM_FACTOR_2_DEFAULT;


        @Preference(label = OverlaySoftButtonLayerAction.SET_SHIFTX_2_LABEL,
                key = OverlaySoftButtonLayerAction.SET_SHIFTX_2_KEY,
                description = OverlaySoftButtonLayerAction.SET_SHIFTX_2_TOOLTIP)
        double setShiftx2Default = OverlaySoftButtonLayerAction.SET_SHIFTX_2_DEFAULT;

        @Preference(label = OverlaySoftButtonLayerAction.SET_SHIFTY_2_LABEL,
                key = OverlaySoftButtonLayerAction.SET_SHIFTY_2_KEY,
                description = OverlaySoftButtonLayerAction.SET_SHIFTY_2_TOOLTIP)
        double setShifty2Default = OverlaySoftButtonLayerAction.SET_SHIFTY_2_DEFAULT;



        // Restore Defaults Section

        @Preference(label = MetaDataLayerType.PROPERTY_RESTORE_TO_DEFAULTS_LABEL,
                key = MetaDataLayerType.PROPERTY_RESTORE_DEFAULTS_NAME,
                description = MetaDataLayerType.PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = MetaDataLayerType.PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT;

    }

}
