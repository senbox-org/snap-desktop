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

import java.awt.*;

import static org.esa.snap.core.layer.ColorBarLayerType.*;

/**
 * Editor for colorbar layer.
 *
 * @author Daniel Knowles
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */


public class ColorBarLayerEditor extends AbstractLayerConfigurationEditor {

    BindingContext context;
    PropertyMap configuration;
    boolean discrete;

    @Override
    protected void addEditablePropertyDescriptors() {


        configuration = SnapApp.getDefault().getSelectedProductSceneView().getSceneImage().getConfiguration();
        discrete = SnapApp.getDefault().getSelectedProductSceneView().getImageInfo().getColorPaletteDef().isDiscrete();

        context = getBindingContext();


        // Title Section

//        addSectionBreak(ColorBarLayerType.PROPERTY_TITLE_SECTION_KEY,
//                ColorBarLayerType.PROPERTY_TITLE_SECTION_LABEL,
//                ColorBarLayerType.PROPERTY_TITLE_SECTION_TOOLTIP);


        PropertyDescriptor titleParameterTextPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_TEXT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_TEXT_TYPE);
        titleParameterTextPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_TEXT_DEFAULT);
        titleParameterTextPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_TEXT_LABEL);
        titleParameterTextPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_TEXT_TOOLTIP);
        titleParameterTextPD.setDefaultConverter();
        addPropertyDescriptor(titleParameterTextPD);


        PropertyDescriptor titleUnitsTextPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_UNITS_TEXT_KEY,
                ColorBarLayerType.PROPERTY_UNITS_TEXT_TYPE);
        titleUnitsTextPD.setDefaultValue(ColorBarLayerType.PROPERTY_UNITS_TEXT_DEFAULT);
        titleUnitsTextPD.setDisplayName(ColorBarLayerType.PROPERTY_UNITS_TEXT_LABEL);
        titleUnitsTextPD.setDescription(ColorBarLayerType.PROPERTY_UNITS_TEXT_TOOLTIP);
        titleUnitsTextPD.setDefaultConverter();
        addPropertyDescriptor(titleUnitsTextPD);


        // Orientation Section

        addSectionBreak(ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_TOOLTIP);


        PropertyDescriptor orientationPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_ORIENTATION_KEY, String.class);
        orientationPD.setDefaultValue(ColorBarLayerType.PROPERTY_ORIENTATION_DEFAULT);
        orientationPD.setDisplayName(ColorBarLayerType.PROPERTY_ORIENTATION_LABEL);
        orientationPD.setDescription(ColorBarLayerType.PROPERTY_ORIENTATION_TOOLTIP);
        orientationPD.setValueSet(new ValueSet(ColorBarLayerType.PROPERTY_ORIENTATION_VALUE_SET));
        orientationPD.setDefaultConverter();
        addPropertyDescriptor(orientationPD);

        PropertyDescriptor reversePalettePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_TYPE);
        reversePalettePD.setDefaultValue(ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_DEFAULT);
        reversePalettePD.setDisplayName(ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_LABEL);
        reversePalettePD.setDescription(ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_TOOLTIP);
        reversePalettePD.setDefaultConverter();
        addPropertyDescriptor(reversePalettePD);



        // Label Values

        addLabelValuesSectionBreak();
        addLabelValuesMode();
        addLabelValuesCount();
        addLabelValuesActual();
        addLabelValuesScalingFactor();
        addLabelValuesDecimalPlaces();
        addLabelValuesForceDecimalPlaces();




        // Color Bar Location Section

        addSectionBreak(ColorBarLayerType.PROPERTY_LOCATION_SECTION_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_LOCATION_SECTION_TOOLTIP);

        PropertyDescriptor locationInsidePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LOCATION_INSIDE_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_INSIDE_TYPE);
        locationInsidePD.setDefaultValue(ColorBarLayerType.PROPERTY_LOCATION_INSIDE_DEFAULT);
        locationInsidePD.setDisplayName(ColorBarLayerType.PROPERTY_LOCATION_INSIDE_LABEL);
        locationInsidePD.setDescription(ColorBarLayerType.PROPERTY_LOCATION_INSIDE_TOOLTIP);
        locationInsidePD.setDefaultConverter();
        addPropertyDescriptor(locationInsidePD);

        PropertyDescriptor locationPlacementPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_TYPE);
        locationPlacementPD.setDefaultValue(ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_DEFAULT);
        locationPlacementPD.setValueSet(new ValueSet(ColorBarLayerType.getColorBarLocationArray()));
        locationPlacementPD.setDisplayName(ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_LABEL);
        locationPlacementPD.setDescription(ColorBarLayerType.PROPERTY_LOCATION_PLACEMENT_TOOLTIP);
        locationPlacementPD.setDefaultConverter();
        addPropertyDescriptor(locationPlacementPD);


        PropertyDescriptor locationOffsetPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LOCATION_OFFSET_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_OFFSET_TYPE);
        locationOffsetPD.setDefaultValue(ColorBarLayerType.PROPERTY_LOCATION_OFFSET_DEFAULT);
        locationOffsetPD.setDisplayName(ColorBarLayerType.PROPERTY_LOCATION_OFFSET_LABEL);
        locationOffsetPD.setDescription(ColorBarLayerType.PROPERTY_LOCATION_OFFSET_TOOLTIP);
        locationOffsetPD.setDefaultConverter();
        addPropertyDescriptor(locationOffsetPD);

        PropertyDescriptor locationShiftPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LOCATION_SHIFT_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_SHIFT_TYPE);
        locationShiftPD.setDefaultValue(ColorBarLayerType.PROPERTY_LOCATION_SHIFT_DEFAULT);
        locationShiftPD.setDisplayName(ColorBarLayerType.PROPERTY_LOCATION_SHIFT_LABEL);
        locationShiftPD.setDescription(ColorBarLayerType.PROPERTY_LOCATION_SHIFT_TOOLTIP);
        locationShiftPD.setDefaultConverter();
        addPropertyDescriptor(locationShiftPD);


        PropertyDescriptor titleVerticalAnchorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_TYPE);
        titleVerticalAnchorPD.setDefaultValue(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_DEFAULT);
        titleVerticalAnchorPD.setValueSet(new ValueSet(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_VALUE_SET));
        titleVerticalAnchorPD.setDisplayName(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_LABEL);
        titleVerticalAnchorPD.setDescription(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_TOOLTIP);
        titleVerticalAnchorPD.setDefaultConverter();
        addPropertyDescriptor(titleVerticalAnchorPD);




        // Color Bar Scaling Section
        addSizeScalingSectionBreak();
        addImageScaling();
        addImageScalingPercent();
        addColorBarLength();
        addColorBarWidth();


        // Title Section
        addTitleSectionBreak();
        addTitleShow();
        addTitleFontSize();
        addTitleFontBold();
        addTitleFontItalic();
        addTitleFontName();
        addTitleFontColor();


        // Units Section
        addUnitsSectionBreak();
        addUnitsShow();
        addUnitsFontSize();
        addUnitsFontBold();
        addUnitsFontItalic();
        addUnitsFontName();
        addUnitsFontColor();


        // Labels Section
        addLabelsSectionBreak();
        addLabelsShow();
        addLabelsFontSize();
        addLabelsFontBold();
        addLabelsFontItalic();
        addLabelsFontName();
        addLabelsFontColor();


        // Tickmarks Section
        addTickMarksSectionBreak();
        addTickMarksShow();
        addTickMarksLength();
        addTickMarksWidth();
        addTickMarksColor();






        // Backdrop Section

        addSectionBreak(ColorBarLayerType.PROPERTY_BACKDROP_SECTION_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_BACKDROP_SECTION_TOOLTIP);

        PropertyDescriptor backdropShowPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY, Boolean.class);
        backdropShowPD.setDefaultValue(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_DEFAULT);
        backdropShowPD.setDisplayName(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_LABEL);
        backdropShowPD.setDescription(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_TOOLTIP);
        backdropShowPD.setDefaultConverter();
        addPropertyDescriptor(backdropShowPD);

        PropertyDescriptor backdropTransparencyPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY, Double.class);
        backdropTransparencyPD.setDefaultValue(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT);
        backdropTransparencyPD.setValueRange(new ValueRange(0, 1));
        backdropTransparencyPD.setDisplayName(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_LABEL);
        backdropTransparencyPD.setDescription(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_TOOLTIP);
        backdropTransparencyPD.setDefaultConverter();
        addPropertyDescriptor(backdropTransparencyPD);

        PropertyDescriptor backdropColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY, Color.class);
        backdropColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_DEFAULT);
        backdropColorPD.setDisplayName(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_LABEL);
        backdropColorPD.setDescription(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_TOOLTIP);
        backdropColorPD.setDefaultConverter();
        addPropertyDescriptor(backdropColorPD);




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




        context.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_TEXT_KEY, true,
                PROPERTY_TITLE_SHOW_KEY, true);


        context.bindEnabledState(ColorBarLayerType.PROPERTY_UNITS_TEXT_KEY, true,
                ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY, true);






    }


    private void addSectionBreak(String name, String label, String toolTip) {
        PropertyDescriptor descriptor = new PropertyDescriptor(name, Boolean.class);
        descriptor.setDisplayName(label);
        descriptor.setDescription(toolTip);
        addPropertyDescriptor(descriptor);
    }





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
        if (discrete) {
            pd.setDefaultValue(DISTRIB_EXACT_STR);
        } else {
            pd.setDefaultValue(PROPERTY_LABEL_VALUES_MODE_DEFAULT);
        }

        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }



    private void addLabelValuesCount() {
        String labelsMode = configuration.getPropertyString(PROPERTY_LABEL_VALUES_MODE_KEY, PROPERTY_LABEL_VALUES_MODE_DEFAULT);

        boolean enabled = (DISTRIB_EVEN_STR.equals(labelsMode)) ? true : false;

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABEL_VALUES_COUNT_KEY, PROPERTY_LABEL_VALUES_COUNT_TYPE);
        pd.setDefaultValue(PROPERTY_LABEL_VALUES_COUNT_DEFAULT);
        pd.setDisplayName(PROPERTY_LABEL_VALUES_COUNT_LABEL);
        pd.setDescription(PROPERTY_LABEL_VALUES_COUNT_TOOLTIP);
        if (discrete) {
            pd.setEnabled(false);
        } else {
            pd.setEnabled(PROPERTY_LABEL_VALUES_COUNT_ENABLED);
        }

        pd.setValueRange(new ValueRange(PROPERTY_LABEL_VALUES_COUNT_MIN, PROPERTY_LABEL_VALUES_COUNT_MAX));
        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABEL_VALUES_COUNT_KEY, true,
                PROPERTY_LABEL_VALUES_MODE_KEY, DISTRIB_EVEN_STR);
    }





    private void addLabelValuesActual() {
        String labelsMode = configuration.getPropertyString(PROPERTY_LABEL_VALUES_MODE_KEY, PROPERTY_LABEL_VALUES_MODE_DEFAULT);

        boolean enabled = (DISTRIB_MANUAL_STR.equals(labelsMode)) ? true : false;

        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABEL_VALUES_ACTUAL_KEY,
                PROPERTY_LABEL_VALUES_ACTUAL_TYPE);
        pd.setDefaultValue(PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT);
        pd.setDisplayName(PROPERTY_LABEL_VALUES_ACTUAL_LABEL);
        pd.setDescription(PROPERTY_LABEL_VALUES_ACTUAL_TOOLTIP);

        if (discrete) {
            pd.setEnabled(false);
        } else {
            pd.setEnabled(PROPERTY_LABEL_VALUES_ACTUAL_ENABLED);
        }

        pd.setDefaultConverter();
        pd.setEnabled(enabled);
        addPropertyDescriptor(pd);

        context.bindEnabledState(PROPERTY_LABEL_VALUES_ACTUAL_KEY, true,
                PROPERTY_LABEL_VALUES_MODE_KEY, DISTRIB_MANUAL_STR);

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
    }



    private void addLabelValuesForceDecimalPlaces() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY,
                PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TYPE);
        pd.setDefaultValue(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT);
        pd.setDisplayName(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_LABEL);
        pd.setDescription(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TOOLTIP);
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
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LEGEND_LENGTH_KEY, PROPERTY_LEGEND_LENGTH_TYPE);
        pd.setDefaultValue(PROPERTY_LEGEND_LENGTH_DEFAULT);
        pd.setValueRange(new ValueRange(PROPERTY_LEGEND_LENGTH_VALUE_MIN, PROPERTY_LEGEND_LENGTH_VALUE_MAX));
        pd.setDisplayName(PROPERTY_LEGEND_LENGTH_LABEL);
        pd.setDescription(PROPERTY_LEGEND_LENGTH_TOOLTIP);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
    }


    private void  addColorBarWidth() {
        PropertyDescriptor pd = new PropertyDescriptor(PROPERTY_LEGEND_WIDTH_KEY, PROPERTY_LEGEND_WIDTH_TYPE);
        pd.setDefaultValue(PROPERTY_LEGEND_WIDTH_DEFAULT);
        pd.setValueRange(new ValueRange(PROPERTY_LEGEND_WIDTH_MIN, PROPERTY_LEGEND_WIDTH_MAX));
        pd.setDisplayName(PROPERTY_LEGEND_WIDTH_LABEL);
        pd.setDescription(PROPERTY_LEGEND_WIDTH_TOOLTIP);
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





}






















