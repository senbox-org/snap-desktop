/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.layer.ColorBarLayerType;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.layer.AbstractLayerConfigurationEditor;

import static org.esa.snap.core.layer.ColorBarLayerType.*;

/**
 * Editor for colorbar layer.
 *
 * @author Daniel Knowles
 */


public class ColorBarLayerEditor extends AbstractLayerConfigurationEditor {

    BindingContext context;
    PropertyMap configuration;

    @Override
    protected void addEditablePropertyDescriptors() {

        configuration = SnapApp.getDefault().getSelectedProductSceneView().getSceneImage().getConfiguration();
        context = getBindingContext();


        addSchemesCheckbox();
        addSchemeLabelsApplyCheckbox();
//        addSchemeLabelsRestrictCheckbox();

        // Title Section
        addHeaderTitleSectionBreak();
        addTitleTextfield();

//        addTitleAltTextfield();
//        addTitleAltUseBoolean();


        // Units Section
//        addHeaderUnitsSectionBreak();
        addUnitsTextfield();
        addConvertCaretBoolean();

//        addUnitsAltTextfield();
//        addUnitsAltUseBoolean();


        // Label Values
        addLabelValuesSectionBreak();
        addLabelValuesMode();
        addLabelValuesCount();
        addLabelValuesActual();
        addAutoPopulateLabelValues();

//        addWeightTolerance();


        // Orientation Section
        addOrientationSectionBreak();
        addAlignment();
        addSceneAspectBestFit();
        addTitleAnchor();
        addReversePalette();


        // Color Bar Location Section
        addLocationSectionBreak();
        addLocationInside();
        addLocationPlacementHorizontal();
        addLocationPlacementVertical();
        addLocationGapFactor();
        addLocationOffset();
        addLocationShift();











        // Title Section
        addTitleSectionBreak();
        addTitleShow();
        addTitleFontBold();
        addTitleFontItalic();
        addTitleFontName();
        addTitleFontColor();


        // Units Section
        addUnitsSectionBreak();
        addUnitsShow();
        addUnitsFontBold();
        addUnitsFontItalic();
        addUnitsFontName();
        addUnitsFontColor();
        addUnitsNullTextfield();
        addUnitsParenthesisBoolean();

        // Labels Section
        addLabelsSectionBreak();
        addLabelsShow();
        addLabelsFontBold();
        addLabelsFontItalic();
        addLabelsFontName();
        addLabelsFontColor();
        addLabelValuesScalingFactor();
        addLabelValuesDecimalPlaces();
        addLabelValuesForceDecimalPlaces();


        // Tickmarks Section
        addTickMarksSectionBreak();
        addTickMarksShow();
        addTickMarksLength();
        addTickMarksWidth();
        addTickMarksColor();


        // Palette Border Section
        addPaletteBorderSectionBreak();
        addPaletteBorderShow();
        addPaletteBorderWidth();
        addPaletteBorderColor();


        // Legend Border Section
        addLegendBorderSectionBreak();
        addLegendBorderShow();
        addLegendBorderWidth();
        addLegendBorderColor();


        // Backdrop Section
        addBackdropSectionBreak();
        addBackdropShow();
        addBackdropTransparency();
        addBackdropColor();


        // Color Bar Scaling Section
        addSizeScalingSectionBreak();
        addImageScaling();
        addImageScalingPercent();



        // Sizing Section
        addLegendSizingSectionBreak();
        addTitleFontSize();
        addUnitsFontSize();
        addLabelsFontSize();
        addColorBarLength();
        addColorBarWidth();

        // Margins Section
        addLegendBorderGapSectionBreak();
        addLegendBorderGapTop();
        addLegendBorderGapBottom();
        addLegendBorderGapLeftSide();
        addLegendBorderGapRightSide();
        addLegendTitleGap();
        addLegendLabelGap();

//  Commented out because choosing NOT to set enablement of this component as it might confused user being location
        // far away from the "Show Title" and "Show Units" components
//        context.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_TEXT_KEY, true,
//                PROPERTY_TITLE_SHOW_KEY, true);
//
//
//        context.bindEnabledState(ColorBarLayerType.PROPERTY_UNITS_TEXT_KEY, true,
//                ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY, true);


    }


    private void addSectionBreak(String name, String label, String toolTip) {
        PropertyDescriptor descriptor = new PropertyDescriptor(name, Boolean.class);
        descriptor.setDisplayName(label);
        descriptor.setDescription(toolTip);
        addPropertyDescriptor(descriptor);
    }



    private void addSchemesCheckbox() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_SCHEME_AUTO_APPLY_KEY,
                PROPERTY_SCHEME_AUTO_APPLY_TYPE);
        pd.setDefaultValue(PROPERTY_SCHEME_AUTO_APPLY_DEFAULT);
        pd.setDisplayName(PROPERTY_SCHEME_AUTO_APPLY_LABEL);
        pd.setDescription(PROPERTY_SCHEME_AUTO_APPLY_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }

    private void addSchemeLabelsApplyCheckbox() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_SCHEME_LABELS_APPLY_KEY,
                PROPERTY_SCHEME_LABELS_APPLY_TYPE);
        pd.setDefaultValue(PROPERTY_SCHEME_LABELS_APPLY_DEFAULT);
        pd.setDisplayName(PROPERTY_SCHEME_LABELS_APPLY_LABEL);
        pd.setDescription(PROPERTY_SCHEME_LABELS_APPLY_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }

    private void addSchemeLabelsRestrictCheckbox() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_SCHEME_LABELS_APPLY_KEY, PROPERTY_SCHEME_LABELS_APPLY_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_SCHEME_LABELS_RESTRICT_KEY,
                PROPERTY_SCHEME_LABELS_RESTRICT_TYPE);
        pd.setDefaultValue(PROPERTY_SCHEME_LABELS_RESTRICT_DEFAULT);
        pd.setDisplayName(PROPERTY_SCHEME_LABELS_RESTRICT_LABEL);
        pd.setDescription(PROPERTY_SCHEME_LABELS_RESTRICT_TOOLTIP);
        pd.setEnabled(enabled);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_SCHEME_LABELS_RESTRICT_KEY, true,
                PROPERTY_SCHEME_LABELS_APPLY_KEY, true);
    }

    // Title / Units


    private void addHeaderTitleSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_HEADER_TITLE_SECTION_KEY,
                PROPERTY_HEADER_TITLE_SECTION_LABEL,
                PROPERTY_HEADER_TITLE_SECTION_TOOLTIP);
    }

    private void addTitleAltUseBoolean() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_TITLE_ALT_USE_KEY,
                PROPERTY_TITLE_ALT_USE_TYPE);
        pd.setDefaultValue(PROPERTY_TITLE_ALT_USE_DEFAULT);
        pd.setDisplayName(PROPERTY_TITLE_ALT_USE_LABEL);
        pd.setDescription(PROPERTY_TITLE_ALT_USE_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }
    private void addTitleTextfield() {
        boolean titleAltUse = configuration.getPropertyBool(PROPERTY_TITLE_ALT_USE_KEY, PROPERTY_TITLE_ALT_USE_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_TITLE_KEY,
                PROPERTY_TITLE_TYPE);
        pd.setDefaultValue(PROPERTY_TITLE_DEFAULT);
        pd.setDisplayName(PROPERTY_TITLE_LABEL);
        pd.setDescription(PROPERTY_TITLE_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(!titleAltUse);
        addPropertyDescriptor(pd);


        context.bindEnabledState(PROPERTY_TITLE_KEY, false,
                PROPERTY_TITLE_ALT_USE_KEY, true);
    }

    private void addTitleAltTextfield() {
        boolean titleAltUse = configuration.getPropertyBool(PROPERTY_TITLE_ALT_USE_KEY, PROPERTY_TITLE_ALT_USE_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_TITLE_ALT_KEY,
                PROPERTY_TITLE_ALT_TYPE);
        pd.setDefaultValue(PROPERTY_TITLE_ALT_DEFAULT);
        pd.setDisplayName(PROPERTY_TITLE_ALT_LABEL);
        pd.setDescription(PROPERTY_TITLE_ALT_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(titleAltUse);

        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_TITLE_ALT_KEY, true,
                PROPERTY_TITLE_ALT_USE_KEY, true);
    }


    private void addHeaderUnitsSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_HEADER_UNITS_SECTION_KEY,
                PROPERTY_HEADER_UNITS_SECTION_LABEL,
                PROPERTY_HEADER_UNITS_SECTION_TOOLTIP);
    }

    private void addUnitsAltUseBoolean() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_ALT_USE_KEY,
                PROPERTY_UNITS_ALT_USE_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_ALT_USE_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_ALT_USE_LABEL);
        pd.setDescription(PROPERTY_UNITS_ALT_USE_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }



    private void addUnitsTextfield() {
        boolean unitsAltUse = configuration.getPropertyBool(PROPERTY_UNITS_ALT_USE_KEY, PROPERTY_UNITS_ALT_USE_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_KEY,
                PROPERTY_UNITS_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_LABEL);
        pd.setDescription(PROPERTY_UNITS_TOOLTIP);
        pd.setEnabled(!unitsAltUse);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_UNITS_KEY, true,
                PROPERTY_UNITS_ALT_USE_KEY, false);
    }

    private void addUnitsAltTextfield() {
        boolean unitsAltUse = configuration.getPropertyBool(PROPERTY_UNITS_ALT_USE_KEY, PROPERTY_UNITS_ALT_USE_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_ALT_KEY,
                PROPERTY_UNITS_ALT_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_ALT_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_ALT_LABEL);
        pd.setDescription(PROPERTY_UNITS_ALT_TOOLTIP);
        pd.setEnabled(unitsAltUse);

        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_UNITS_ALT_KEY, true,
                PROPERTY_UNITS_ALT_USE_KEY, true);
    }



    private void addUnitsNullTextfield() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_NULL_KEY,
                PROPERTY_UNITS_NULL_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_NULL_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_NULL_LABEL);
        pd.setDescription(PROPERTY_UNITS_NULL_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }


    private void addConvertCaretBoolean() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_CONVERT_CARET_KEY,
                PROPERTY_CONVERT_CARET_TYPE);
        pd.setDefaultValue(PROPERTY_CONVERT_CARET_DEFAULT);
        pd.setDisplayName(PROPERTY_CONVERT_CARET_LABEL);
        pd.setDescription(PROPERTY_CONVERT_CARET_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }

    private void addUnitsParenthesisBoolean() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_PARENTHESIS_KEY,
                PROPERTY_UNITS_PARENTHESIS_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_PARENTHESIS_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_PARENTHESIS_LABEL);
        pd.setDescription(PROPERTY_UNITS_PARENTHESIS_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }









    // Orientation


    private void addOrientationSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_KEY,
                PROPERTY_ORIENTATION_SECTION_LABEL,
                PROPERTY_ORIENTATION_SECTION_TOOLTIP);
    }


    private void addAlignment() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_ORIENTATION_KEY, PROPERTY_ORIENTATION_TYPE);

        pd.setDisplayName(PROPERTY_ORIENTATION_LABEL);
        pd.setDescription(PROPERTY_ORIENTATION_TOOLTIP);
        pd.setValueSet(new ValueSet(ColorBarLayerType.PROPERTY_ORIENTATION_VALUE_SET));
        pd.setDefaultValue(PROPERTY_ORIENTATION_DEFAULT);

        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }

    private void addSceneAspectBestFit() {
        String orientation = configuration.getPropertyString(PROPERTY_ORIENTATION_KEY, PROPERTY_ORIENTATION_DEFAULT);
        boolean enabled = OPTION_BEST_FIT.equals(orientation);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_SCENE_ASPECT_BEST_FIT_KEY, PROPERTY_SCENE_ASPECT_BEST_FIT_TYPE);

        pd.setDisplayName(PROPERTY_SCENE_ASPECT_BEST_FIT_LABEL);
        pd.setDescription(PROPERTY_SCENE_ASPECT_BEST_FIT_TOOLTIP);
        pd.setDefaultValue(PROPERTY_SCENE_ASPECT_BEST_FIT_DEFAULT);
        pd.setEnabled(enabled);

        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_SCENE_ASPECT_BEST_FIT_KEY, true,
                PROPERTY_ORIENTATION_KEY, OPTION_BEST_FIT);
    }



    private void addTitleAnchor() {
        // todo Enablement binding is not working so commented out

        boolean enabled = isPossiblyVertical(configuration);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LOCATION_TITLE_VERTICAL_KEY, PROPERTY_LOCATION_TITLE_VERTICAL_TYPE);
        pd.setDefaultValue(PROPERTY_LOCATION_TITLE_VERTICAL_DEFAULT);
        pd.setDisplayName(PROPERTY_LOCATION_TITLE_VERTICAL_LABEL);
        pd.setDescription(PROPERTY_LOCATION_TITLE_VERTICAL_TOOLTIP);

        pd.setValueSet(new ValueSet(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_VALUE_SET));
        pd.setEnabled(enabled);


        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

//        context.setComponentsEnabled(PROPERTY_LOCATION_TITLE_VERTICAL_KEY, isPossiblyVertical(configuration));


//        context.bindEnabledState(PROPERTY_LOCATION_TITLE_VERTICAL_KEY, true, new Enablement.Condition() {
//            @Override
//            public boolean evaluate(BindingContext bindingContext) {
//                return isPossiblyVertical();
//            }
//        });

        context.bindEnabledState(PROPERTY_LOCATION_TITLE_VERTICAL_KEY, false,
                PROPERTY_ORIENTATION_KEY, OPTION_HORIZONTAL);
    }



    public static boolean isPossiblyVertical(PropertyMap configuration) {
        if (configuration == null) {
            return true;
        }
        String orientation = configuration.getPropertyString(PROPERTY_ORIENTATION_KEY, PROPERTY_ORIENTATION_DEFAULT);

        if (ColorBarLayerType.OPTION_BEST_FIT.equals(orientation) || OPTION_VERTICAL.equals(orientation)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isPossiblyHorizontal(PropertyMap configuration) {
        if (configuration == null) {
            return true;
        }
        String orientation = configuration.getPropertyString(PROPERTY_ORIENTATION_KEY, PROPERTY_ORIENTATION_DEFAULT);

        if (ColorBarLayerType.OPTION_BEST_FIT.equals(orientation) || OPTION_HORIZONTAL.equals(orientation)) {
            return true;
        } else {
            return false;
        }
    }



    private void addReversePalette() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY,
                PROPERTY_ORIENTATION_REVERSE_PALETTE_TYPE);
        pd.setDefaultValue(PROPERTY_ORIENTATION_REVERSE_PALETTE_DEFAULT);
        pd.setDisplayName(PROPERTY_ORIENTATION_REVERSE_PALETTE_LABEL);
        pd.setDescription(PROPERTY_ORIENTATION_REVERSE_PALETTE_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }







    // Label Values


    private void addLabelValuesSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_TOOLTIP);
    }


    private void addLabelValuesMode() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABEL_VALUES_MODE_KEY, PROPERTY_LABEL_VALUES_MODE_TYPE);

        pd.setDisplayName(PROPERTY_LABEL_VALUES_MODE_LABEL);
        pd.setDescription(PROPERTY_LABEL_VALUES_MODE_TOOLTIP);
        pd.setValueSet(new ValueSet(PROPERTY_LABEL_VALUES_MODE_VALUE_SET));
        pd.setDefaultValue(PROPERTY_LABEL_VALUES_MODE_DEFAULT);

        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }



    private void addLabelValuesCount() {
        String labelsMode = configuration.getPropertyString(PROPERTY_LABEL_VALUES_MODE_KEY, PROPERTY_LABEL_VALUES_MODE_DEFAULT);

        boolean enabled = DISTRIB_EVEN_STR.equals(labelsMode);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABEL_VALUES_COUNT_KEY, PROPERTY_LABEL_VALUES_COUNT_TYPE);
        pd.setDefaultValue(PROPERTY_LABEL_VALUES_COUNT_DEFAULT);
        pd.setDisplayName(PROPERTY_LABEL_VALUES_COUNT_LABEL);
        pd.setDescription(PROPERTY_LABEL_VALUES_COUNT_TOOLTIP);
//        pd.setEnabled(PROPERTY_LABEL_VALUES_COUNT_ENABLED);

        pd.setEnabled(enabled);

        pd.setValueRange(new ValueRange(PROPERTY_LABEL_VALUES_COUNT_MIN, PROPERTY_LABEL_VALUES_COUNT_MAX));

        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABEL_VALUES_COUNT_KEY, true,
                PROPERTY_LABEL_VALUES_MODE_KEY, DISTRIB_EVEN_STR);
    }





    private void addLabelValuesActual() {
        String labelsMode = configuration.getPropertyString(PROPERTY_LABEL_VALUES_MODE_KEY, PROPERTY_LABEL_VALUES_MODE_DEFAULT);
//        String labelsMode = context.getPropertySet().getProperty(PROPERTY_LABEL_VALUES_MODE_KEY).getValue();
        boolean enabled = DISTRIB_MANUAL_STR.equals(labelsMode);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABEL_VALUES_ACTUAL_KEY,
                PROPERTY_LABEL_VALUES_ACTUAL_TYPE);
        pd.setDefaultValue(PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT);
        pd.setDisplayName(PROPERTY_LABEL_VALUES_ACTUAL_LABEL);
        pd.setDescription(PROPERTY_LABEL_VALUES_ACTUAL_TOOLTIP);
//        pd.setEnabled(PROPERTY_LABEL_VALUES_ACTUAL_ENABLED);
        pd.setEnabled(enabled);


        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABEL_VALUES_ACTUAL_KEY, true,
                PROPERTY_LABEL_VALUES_MODE_KEY, DISTRIB_MANUAL_STR);

    }

    private void  addAutoPopulateLabelValues() {

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY, PROPERTY_POPULATE_VALUES_TEXTFIELD_TYPE);
        pd.setDefaultValue(PROPERTY_POPULATE_VALUES_TEXTFIELD_DEFAULT);
        pd.setDisplayName(PROPERTY_POPULATE_VALUES_TEXTFIELD_LABEL);
        pd.setDescription(PROPERTY_POPULATE_VALUES_TEXTFIELD_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(true);
        addPropertyDescriptor(pd);
    }



    private void addLabelValuesScalingFactor() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABEL_VALUES_SCALING_KEY,
                PROPERTY_LABEL_VALUES_SCALING_TYPE);
        pd.setDefaultValue(PROPERTY_LABEL_VALUES_SCALING_DEFAULT);
        pd.setDisplayName(PROPERTY_LABEL_VALUES_SCALING_LABEL);
        pd.setDescription(PROPERTY_LABEL_VALUES_SCALING_TOOLTIP);
        pd.setValueRange(new ValueRange(PROPERTY_LABEL_VALUES_SCALING_MIN, PROPERTY_LABEL_VALUES_SCALING_MAX));
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }



    private void addLabelValuesDecimalPlaces() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY,
                PROPERTY_LABEL_VALUES_DECIMAL_PLACES_TYPE);
        pd.setDefaultValue(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY);
        pd.setDisplayName(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_LABEL);
        pd.setDescription(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_TOOLTIP);
        pd.setValueRange(new ValueRange(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MIN,
                PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MAX));
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY, false,
                PROPERTY_LABEL_VALUES_MODE_KEY, DISTRIB_MANUAL_STR);
    }



    private void addLabelValuesForceDecimalPlaces() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY,
                PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TYPE);
        pd.setDefaultValue(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT);
        pd.setDisplayName(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_LABEL);
        pd.setDescription(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY, false,
                PROPERTY_LABEL_VALUES_MODE_KEY, DISTRIB_MANUAL_STR);    }




    private void  addWeightTolerance() {

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_WEIGHT_TOLERANCE_KEY, PROPERTY_WEIGHT_TOLERANCE_TYPE);
        pd.setDefaultValue(PROPERTY_WEIGHT_TOLERANCE_DEFAULT);
        pd.setDisplayName(PROPERTY_WEIGHT_TOLERANCE_LABEL);
        pd.setDescription(PROPERTY_WEIGHT_TOLERANCE_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(true);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_WEIGHT_TOLERANCE_KEY, false,
                PROPERTY_LABEL_VALUES_MODE_KEY, DISTRIB_MANUAL_STR);
    }






    // Color Bar Location Section



    private void addLocationSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_LOCATION_SECTION_KEY,
                PROPERTY_LOCATION_SECTION_LABEL,
                PROPERTY_LOCATION_SECTION_TOOLTIP);
    }


    private void addLocationInside() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LOCATION_INSIDE_KEY,
                PROPERTY_LOCATION_INSIDE_TYPE);
        pd.setDefaultValue(PROPERTY_LOCATION_INSIDE_DEFAULT);
        pd.setDisplayName(PROPERTY_LOCATION_INSIDE_LABEL);
        pd.setDescription(PROPERTY_LOCATION_INSIDE_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);


    }


    private void addLocationPlacementHorizontal() {
        // todo Enablement binding is not working so commented out

        boolean enabled = isPossiblyHorizontal(configuration);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_KEY,
                PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_TYPE);
        pd.setDefaultValue(PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_DEFAULT);
        pd.setValueSet(new ValueSet(ColorBarLayerType.getColorBarLocationHorizontalArray()));
        pd.setDisplayName(PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_LABEL);
        pd.setDescription(PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_TOOLTIP);
        pd.setEnabled(enabled);

        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

//        context.setComponentsEnabled(PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_KEY, isPossiblyHorizontal(configuration));

//        context.bindEnabledState(PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_KEY, true, new Enablement.Condition() {
//            @Override
//            public boolean evaluate(BindingContext context) {
//                context.setComponentsEnabled();
//                return isPossiblyHorizontal();
//            }
//        });

        context.bindEnabledState(PROPERTY_LOCATION_PLACEMENT_HORIZONTAL_KEY, false,
                PROPERTY_ORIENTATION_KEY, OPTION_VERTICAL);
    }






    private void addLocationPlacementVertical() {
        // todo Enablement binding is not working so commented out

        boolean enabled = (isPossiblyVertical(configuration)) ? true : false;

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LOCATION_PLACEMENT_VERTICAL_KEY,
                PROPERTY_LOCATION_PLACEMENT_VERTICAL_TYPE);
        pd.setDefaultValue(PROPERTY_LOCATION_PLACEMENT_VERTICAL_DEFAULT);
        pd.setValueSet(new ValueSet(ColorBarLayerType.getColorBarLocationVerticalArray()));
        pd.setDisplayName(PROPERTY_LOCATION_PLACEMENT_VERTICAL_LABEL);
        pd.setDescription(PROPERTY_LOCATION_PLACEMENT_VERTICAL_TOOLTIP);
        pd.setEnabled(enabled);

        pd.setDefaultConverter();
        addPropertyDescriptor(pd);


//        context.setComponentsEnabled(PROPERTY_LOCATION_PLACEMENT_VERTICAL_KEY, isPossiblyVertical(configuration));

//        context.bindEnabledState(PROPERTY_LOCATION_PLACEMENT_VERTICAL_KEY, true, new Enablement.Condition() {
//            @Override
//            public boolean evaluate(BindingContext bindingContext) {
//                return isPossiblyVertical();
//            }
//        });

        context.bindEnabledState(PROPERTY_LOCATION_PLACEMENT_VERTICAL_KEY, false,
                PROPERTY_ORIENTATION_KEY, OPTION_HORIZONTAL);
    }


    private void addLocationGapFactor() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LOCATION_GAP_FACTOR_KEY,
                PROPERTY_LOCATION_GAP_FACTOR_TYPE);
        pd.setDefaultValue(PROPERTY_LOCATION_GAP_FACTOR_DEFAULT);
        pd.setDisplayName(PROPERTY_LOCATION_GAP_FACTOR_LABEL);
        pd.setValueRange(new ValueRange(PROPERTY_LOCATION_GAP_FACTOR_MIN, PROPERTY_LOCATION_GAP_FACTOR_MAX));
        pd.setDescription(PROPERTY_LOCATION_GAP_FACTOR_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }


    private void addLocationOffset() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LOCATION_OFFSET_KEY,
                PROPERTY_LOCATION_OFFSET_TYPE);
        pd.setDefaultValue(PROPERTY_LOCATION_OFFSET_DEFAULT);
        pd.setDisplayName(PROPERTY_LOCATION_OFFSET_LABEL);
        pd.setDescription(PROPERTY_LOCATION_OFFSET_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }

    private void addLocationShift() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LOCATION_SHIFT_KEY,
                PROPERTY_LOCATION_SHIFT_TYPE);
        pd.setDefaultValue(PROPERTY_LOCATION_SHIFT_DEFAULT);
        pd.setDisplayName(PROPERTY_LOCATION_SHIFT_LABEL);
        pd.setDescription(PROPERTY_LOCATION_SHIFT_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }







    // Size and Scaling Section

    private void addSizeScalingSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_KEY,
                ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_TOOLTIP);
    }


    private void  addImageScaling() {
        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_KEY,
                ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

    }


    private void  addImageScalingPercent() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_IMAGE_SCALING_APPLY_SIZE_KEY, PROPERTY_IMAGE_SCALING_APPLY_SIZE_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_KEY,
                ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_DEFAULT);
        pd.setValueRange(new ValueRange(PROPERTY_IMAGE_SCALING_SIZE_MIN, PROPERTY_IMAGE_SCALING_SIZE_MAX));
        pd.setDisplayName(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_TOOLTIP);
        pd.setEnabled(enabled);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_IMAGE_SCALING_SIZE_KEY, PROPERTY_IMAGE_SCALING_APPLY_SIZE_KEY);
    }



    private void  addColorBarLength() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_COLORBAR_LENGTH_KEY, PROPERTY_COLORBAR_LENGTH_TYPE);
        pd.setDefaultValue(PROPERTY_COLORBAR_LENGTH_DEFAULT);
        pd.setValueRange(new ValueRange(PROPERTY_COLORBAR_LENGTH_VALUE_MIN, PROPERTY_COLORBAR_LENGTH_VALUE_MAX));
        pd.setDisplayName(PROPERTY_COLORBAR_LENGTH_LABEL);
        pd.setDescription(PROPERTY_COLORBAR_LENGTH_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }


    private void  addColorBarWidth() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_COLORBAR_WIDTH_KEY, PROPERTY_COLORBAR_WIDTH_TYPE);
        pd.setDefaultValue(PROPERTY_COLORBAR_WIDTH_DEFAULT);
        pd.setValueRange(new ValueRange(PROPERTY_COLORBAR_WIDTH_MIN, PROPERTY_COLORBAR_WIDTH_MAX));
        pd.setDisplayName(PROPERTY_COLORBAR_WIDTH_LABEL);
        pd.setDescription(PROPERTY_COLORBAR_WIDTH_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }















    // Title Section


    private void  addTitleSectionBreak() {
        addSectionBreak(PROPERTY_TITLE_SECTION_KEY, PROPERTY_TITLE_SECTION_LABEL, PROPERTY_TITLE_SECTION_TOOLTIP);
    }



    private void  addTitleShow() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_TITLE_SHOW_KEY, PROPERTY_TITLE_SHOW_TYPE);
        pd.setDefaultValue(PROPERTY_TITLE_SHOW_DEFAULT);
        pd.setDisplayName(PROPERTY_TITLE_SHOW_LABEL);
        pd.setDescription(PROPERTY_TITLE_SHOW_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }





    private void  addTitleFontSize() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_TITLE_SHOW_KEY, PROPERTY_TITLE_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_TITLE_FONT_SIZE_KEY, PROPERTY_TITLE_FONT_SIZE_TYPE);
        pd.setDefaultValue(PROPERTY_TITLE_FONT_SIZE_DEFAULT);
        pd.setDisplayName(PROPERTY_TITLE_FONT_SIZE_LABEL);
        pd.setDescription(PROPERTY_TITLE_FONT_SIZE_TOOLTIP);
        pd.setValueRange(new ValueRange(PROPERTY_TITLE_FONT_SIZE_VALUE_MIN, PROPERTY_TITLE_FONT_SIZE_VALUE_MAX));
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_TITLE_FONT_SIZE_KEY, PROPERTY_TITLE_SHOW_KEY);

    }



    private void  addTitleFontBold() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_TITLE_SHOW_KEY, PROPERTY_TITLE_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_TITLE_FONT_BOLD_KEY, PROPERTY_TITLE_FONT_BOLD_TYPE);
        pd.setDefaultValue(PROPERTY_TITLE_FONT_BOLD_DEFAULT);
        pd.setDisplayName(PROPERTY_TITLE_FONT_BOLD_LABEL);
        pd.setDescription(PROPERTY_TITLE_FONT_BOLD_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_TITLE_FONT_BOLD_KEY, PROPERTY_TITLE_SHOW_KEY);
    }




    private void  addTitleFontItalic() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_TITLE_SHOW_KEY, PROPERTY_TITLE_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_TITLE_FONT_ITALIC_KEY, PROPERTY_TITLE_FONT_ITALIC_TYPE);
        pd.setDefaultValue(PROPERTY_TITLE_FONT_ITALIC_DEFAULT);
        pd.setDisplayName(PROPERTY_TITLE_FONT_ITALIC_LABEL);
        pd.setDescription(PROPERTY_TITLE_FONT_ITALIC_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_TITLE_FONT_ITALIC_KEY, PROPERTY_TITLE_SHOW_KEY);
    }



    private void  addTitleFontName() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_TITLE_SHOW_KEY, PROPERTY_TITLE_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_TITLE_FONT_NAME_KEY, PROPERTY_TITLE_FONT_NAME_TYPE);
        pd.setDefaultValue(PROPERTY_TITLE_FONT_NAME_DEFAULT);
        pd.setDisplayName(PROPERTY_TITLE_FONT_NAME_LABEL);
        pd.setDescription(PROPERTY_TITLE_FONT_NAME_TOOLTIP);
        pd.setValueSet(new ValueSet(PROPERTY_TITLE_FONT_NAME_VALUE_SET));
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_TITLE_FONT_NAME_KEY, PROPERTY_TITLE_SHOW_KEY);
    }


    private void  addTitleFontColor() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_TITLE_SHOW_KEY, PROPERTY_TITLE_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_TITLE_COLOR_KEY, PROPERTY_TITLE_COLOR_TYPE);
        pd.setDefaultValue(PROPERTY_TITLE_COLOR_DEFAULT);
        pd.setDisplayName(PROPERTY_TITLE_COLOR_LABEL);
        pd.setDescription(PROPERTY_TITLE_COLOR_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_TITLE_COLOR_KEY, PROPERTY_TITLE_SHOW_KEY);
    }






    // Units Section

    private void  addUnitsSectionBreak() {
        addSectionBreak(PROPERTY_UNITS_SECTION_KEY, PROPERTY_UNITS_SECTION_LABEL, PROPERTY_UNITS_SECTION_TOOLTIP);
    }



    private void  addUnitsShow() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_SHOW_KEY, PROPERTY_UNITS_SHOW_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_SHOW_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_SHOW_LABEL);
        pd.setDescription(PROPERTY_UNITS_SHOW_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }




    private void  addUnitsFontSize() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_UNITS_SHOW_KEY, PROPERTY_UNITS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_FONT_SIZE_KEY, PROPERTY_UNITS_FONT_SIZE_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_FONT_SIZE_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_FONT_SIZE_LABEL);
        pd.setDescription(PROPERTY_UNITS_FONT_SIZE_TOOLTIP);
        pd.setValueRange(new ValueRange(PROPERTY_UNITS_FONT_SIZE_VALUE_MIN, PROPERTY_UNITS_FONT_SIZE_VALUE_MAX));
        pd.setEnabled(enabled);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_UNITS_FONT_SIZE_KEY, PROPERTY_UNITS_SHOW_KEY);
    }




    private void  addUnitsFontBold() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_UNITS_SHOW_KEY, PROPERTY_UNITS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_FONT_BOLD_KEY, PROPERTY_UNITS_FONT_BOLD_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_FONT_BOLD_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_FONT_BOLD_LABEL);
        pd.setDescription(PROPERTY_UNITS_FONT_BOLD_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_UNITS_FONT_BOLD_KEY, PROPERTY_UNITS_SHOW_KEY);
    }



    private void  addUnitsFontItalic() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_UNITS_SHOW_KEY, PROPERTY_UNITS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_FONT_ITALIC_KEY, PROPERTY_UNITS_FONT_ITALIC_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_FONT_ITALIC_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_FONT_ITALIC_LABEL);
        pd.setDescription(PROPERTY_UNITS_FONT_ITALIC_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_UNITS_FONT_ITALIC_KEY, PROPERTY_UNITS_SHOW_KEY);
    }



    private void  addUnitsFontName() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_UNITS_SHOW_KEY, PROPERTY_UNITS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_FONT_NAME_KEY, PROPERTY_UNITS_FONT_NAME_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_FONT_NAME_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_FONT_NAME_LABEL);
        pd.setDescription(PROPERTY_UNITS_FONT_NAME_TOOLTIP);
        pd.setValueSet(new ValueSet(PROPERTY_UNITS_FONT_NAME_VALUE_SET));
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_UNITS_FONT_NAME_KEY, PROPERTY_UNITS_SHOW_KEY);
    }



    private void  addUnitsFontColor() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_UNITS_SHOW_KEY, PROPERTY_UNITS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_UNITS_FONT_COLOR_KEY, PROPERTY_UNITS_FONT_COLOR_TYPE);
        pd.setDefaultValue(PROPERTY_UNITS_FONT_COLOR_DEFAULT);
        pd.setDisplayName(PROPERTY_UNITS_FONT_COLOR_LABEL);
        pd.setDescription(PROPERTY_UNITS_FONT_COLOR_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_UNITS_FONT_COLOR_KEY, PROPERTY_UNITS_SHOW_KEY);
    }




    // Labels Section

    private void  addLabelsSectionBreak() {
        addSectionBreak(PROPERTY_LABELS_SECTION_KEY, PROPERTY_LABELS_SECTION_LABEL, PROPERTY_LABELS_SECTION_TOOLTIP);
    }



    private void  addLabelsShow() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABELS_SHOW_KEY, PROPERTY_LABELS_SHOW_TYPE);
        pd.setDefaultValue(PROPERTY_LABELS_SHOW_DEFAULT);
        pd.setDisplayName(PROPERTY_LABELS_SHOW_LABEL);
        pd.setDescription(PROPERTY_LABELS_SHOW_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }




    private void  addLabelsFontSize() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_LABELS_SHOW_KEY, PROPERTY_LABELS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABELS_FONT_SIZE_KEY, PROPERTY_LABELS_FONT_SIZE_TYPE);
        pd.setDefaultValue(PROPERTY_LABELS_FONT_SIZE_DEFAULT);
        pd.setDisplayName(PROPERTY_LABELS_FONT_SIZE_LABEL);
        pd.setDescription(PROPERTY_LABELS_FONT_SIZE_TOOLTIP);
        pd.setValueRange(new ValueRange(PROPERTY_LABELS_FONT_SIZE_VALUE_MIN, PROPERTY_LABELS_FONT_SIZE_VALUE_MAX));
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABELS_FONT_SIZE_KEY, PROPERTY_LABELS_SHOW_KEY);
    }




    private void  addLabelsFontBold() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_LABELS_SHOW_KEY, PROPERTY_LABELS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABELS_FONT_BOLD_KEY, PROPERTY_LABELS_FONT_BOLD_TYPE);
        pd.setDefaultValue(PROPERTY_LABELS_FONT_BOLD_DEFAULT);
        pd.setDisplayName(PROPERTY_LABELS_FONT_BOLD_LABEL);
        pd.setDescription(PROPERTY_LABELS_FONT_BOLD_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABELS_FONT_BOLD_KEY, PROPERTY_LABELS_SHOW_KEY);
    }







    private void  addLabelsFontItalic() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_LABELS_SHOW_KEY, PROPERTY_LABELS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABELS_FONT_ITALIC_KEY, PROPERTY_LABELS_FONT_ITALIC_TYPE);
        pd.setDefaultValue(PROPERTY_LABELS_FONT_ITALIC_DEFAULT);
        pd.setDisplayName(PROPERTY_LABELS_FONT_ITALIC_LABEL);
        pd.setDescription(PROPERTY_LABELS_FONT_ITALIC_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABELS_FONT_ITALIC_KEY, PROPERTY_LABELS_SHOW_KEY);
    }



    private void  addLabelsFontName() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_LABELS_SHOW_KEY, PROPERTY_LABELS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABELS_FONT_NAME_KEY, PROPERTY_LABELS_FONT_NAME_TYPE);
        pd.setDefaultValue(PROPERTY_LABELS_FONT_NAME_DEFAULT);
        pd.setDisplayName(PROPERTY_LABELS_FONT_NAME_LABEL);
        pd.setDescription(PROPERTY_LABELS_FONT_NAME_TOOLTIP);
        pd.setValueSet(new ValueSet(PROPERTY_LABELS_FONT_NAME_VALUE_SET));
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABELS_FONT_NAME_KEY, PROPERTY_LABELS_SHOW_KEY);
    }


    private void  addLabelsFontColor() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_LABELS_SHOW_KEY, PROPERTY_LABELS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABELS_FONT_COLOR_KEY, PROPERTY_LABELS_FONT_COLOR_TYPE);
        pd.setDefaultValue(PROPERTY_LABELS_FONT_COLOR_DEFAULT);
        pd.setDisplayName(PROPERTY_LABELS_FONT_COLOR_LABEL);
        pd.setDescription(PROPERTY_LABELS_FONT_COLOR_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABELS_FONT_COLOR_KEY, PROPERTY_LABELS_SHOW_KEY);
    }




    // Tickmarks Section

    private void  addTickMarksSectionBreak() {
        addSectionBreak(PROPERTY_TICKMARKS_SECTION_KEY, PROPERTY_TICKMARKS_SECTION_LABEL, PROPERTY_TICKMARKS_SECTION_TOOLTIP);
    }



    private void  addTickMarksShow() {
        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, PROPERTY_TICKMARKS_SHOW_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }


    private void  addTickMarksLength() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_TICKMARKS_SHOW_KEY, PROPERTY_TICKMARKS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY, PROPERTY_TICKMARKS_LENGTH_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_TICKMARKS_LENGTH_KEY, PROPERTY_TICKMARKS_SHOW_KEY);
    }


    private void  addTickMarksWidth() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_TICKMARKS_SHOW_KEY, PROPERTY_TICKMARKS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_KEY, PROPERTY_TICKMARKS_WIDTH_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_TOOLTIP);
        pd.setEnabled(enabled);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_TICKMARKS_WIDTH_KEY, PROPERTY_TICKMARKS_SHOW_KEY);
    }


    private void  addTickMarksColor() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_TICKMARKS_SHOW_KEY, PROPERTY_TICKMARKS_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY, PROPERTY_TICKMARKS_COLOR_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_TICKMARKS_COLOR_KEY, PROPERTY_TICKMARKS_SHOW_KEY);
    }














    // Backdrop Section

    private void  addBackdropSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_BACKDROP_SECTION_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_BACKDROP_SECTION_TOOLTIP);
    }

    private void  addBackdropShow() {
        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY, ColorBarLayerType.PROPERTY_BACKDROP_SHOW_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }

    private void  addBackdropTransparency() {
        boolean enabled = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY, ColorBarLayerType.PROPERTY_BACKDROP_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY, ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT);
        pd.setValueRange(new ValueRange(0, 1));
        pd.setDisplayName(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY, ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY);
    }


    private void  addBackdropColor() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_BACKDROP_SHOW_KEY, PROPERTY_BACKDROP_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY, PROPERTY_BACKDROP_COLOR_TYPE);
        pd.setDefaultValue(PROPERTY_BACKDROP_COLOR_DEFAULT);
        pd.setDisplayName(PROPERTY_BACKDROP_COLOR_LABEL);
        pd.setDescription(PROPERTY_BACKDROP_COLOR_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_BACKDROP_COLOR_KEY, PROPERTY_BACKDROP_SHOW_KEY);
    }









    // Palette Border Section

    private void  addPaletteBorderSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SECTION_KEY,
                PROPERTY_PALETTE_BORDER_SECTION_LABEL,
                PROPERTY_PALETTE_BORDER_SECTION_TOOLTIP);
    }



    private void  addPaletteBorderShow() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_PALETTE_BORDER_SHOW_KEY, PROPERTY_PALETTE_BORDER_SHOW_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }


    private void  addPaletteBorderWidth() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_PALETTE_BORDER_SHOW_KEY, PROPERTY_PALETTE_BORDER_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_PALETTE_BORDER_WIDTH_KEY, PROPERTY_PALETTE_BORDER_SHOW_KEY);
    }



    private void  addPaletteBorderColor() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_PALETTE_BORDER_SHOW_KEY, PROPERTY_PALETTE_BORDER_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY, PROPERTY_PALETTE_BORDER_COLOR_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);


        context.bindEnabledState(PROPERTY_PALETTE_BORDER_COLOR_KEY, PROPERTY_PALETTE_BORDER_SHOW_KEY);
    }













    // Legend Border Section

    private void  addLegendBorderSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_TOOLTIP);
    }



    private void  addLegendBorderShow() {
        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_KEY, PROPERTY_LEGEND_BORDER_SHOW_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }


    private void  addLegendBorderWidth() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_LEGEND_BORDER_SHOW_KEY, PROPERTY_LEGEND_BORDER_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LEGEND_BORDER_WIDTH_KEY, PROPERTY_LEGEND_BORDER_SHOW_KEY);
    }



    private void  addLegendBorderColor() {
        boolean enabled = configuration.getPropertyBool(PROPERTY_LEGEND_BORDER_SHOW_KEY, PROPERTY_LEGEND_BORDER_SHOW_DEFAULT);

        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY, PROPERTY_LEGEND_BORDER_COLOR_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_TOOLTIP);
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LEGEND_BORDER_COLOR_KEY, PROPERTY_LEGEND_BORDER_SHOW_KEY);
    }


    // Legend Border Gap Section

    private void  addLegendSizingSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_LEGEND_SIZING_SECTION_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_SIZING_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_LEGEND_SIZING_SECTION_TOOLTIP);
    }


    // Legend Border Gap Section

    private void  addLegendBorderGapSectionBreak() {
        addSectionBreak(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_SECTION_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_SECTION_TOOLTIP);
    }

    private void  addLegendBorderGapTop() {
        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }

    private void  addLegendBorderGapBottom() {
        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }


    private void  addLegendBorderGapLeftSide() {
        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }


    private void  addLegendBorderGapRightSide() {
        PropertyDescriptor pd = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }

    private void  addLegendTitleGap() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LEGEND_TITLE_GAP_KEY, PROPERTY_LEGEND_TITLE_GAP_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }

    private void  addLegendLabelGap() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LEGEND_LABEL_GAP_KEY, PROPERTY_LEGEND_LABEL_GAP_TYPE);
        pd.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_DEFAULT);
        pd.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_LABEL);
        pd.setDescription(ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }


}






















