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
import org.esa.snap.ui.layer.AbstractLayerConfigurationEditor;

import java.awt.*;

/**
 * Editor for colorbar layer.
 *
 * @author Marco Zuehlke
 * @author Daniel Knowles
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
//SEP2018 - Daniel Knowles - adds numerous new properties and related binding contexts.
//JAN2018 - Daniel Knowles - updated with SeaDAS gridline revisions

public class ColorBarLayerEditor extends AbstractLayerConfigurationEditor {


    @Override
    protected void addEditablePropertyDescriptors() {









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


        PropertyDescriptor titleUnitsTextPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_TYPE);
        titleUnitsTextPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_DEFAULT);
        titleUnitsTextPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_LABEL);
        titleUnitsTextPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_TOOLTIP);
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

        addSectionBreak(ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_TOOLTIP);




        PropertyDescriptor labelValuesModePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY, String.class);
        labelValuesModePD.setDefaultValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_DEFAULT);
        labelValuesModePD.setDisplayName(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_LABEL);
        labelValuesModePD.setDescription(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_TOOLTIP);
        labelValuesModePD.setValueSet(new ValueSet(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_VALUE_SET));
        labelValuesModePD.setDefaultConverter();
        addPropertyDescriptor(labelValuesModePD);

        PropertyDescriptor labelValuesCountPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY, Integer.class);
        labelValuesCountPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT);
        labelValuesCountPD.setDisplayName(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_LABEL);
        labelValuesCountPD.setDescription(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_TOOLTIP);
        labelValuesCountPD.setEnabled(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_ENABLED);
        labelValuesCountPD.setValueRange(new ValueRange(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_MIN, ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_MAX));
        labelValuesCountPD.setDefaultConverter();
        addPropertyDescriptor(labelValuesCountPD);

        PropertyDescriptor labelValuesActualPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY, String.class);
        labelValuesActualPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT);
        labelValuesActualPD.setDisplayName(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_LABEL);
        labelValuesActualPD.setDescription(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_TOOLTIP);
        labelValuesActualPD.setEnabled(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_ENABLED);
        labelValuesActualPD.setDefaultConverter();
        addPropertyDescriptor(labelValuesActualPD);

        PropertyDescriptor labelValuesScalingFactorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_KEY, Double.class);
        labelValuesScalingFactorPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_DEFAULT);
        labelValuesScalingFactorPD.setDisplayName(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_LABEL);
        labelValuesScalingFactorPD.setDescription(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_TOOLTIP);
        labelValuesScalingFactorPD.setValueRange(new ValueRange(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_MIN, ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_MAX));
        labelValuesScalingFactorPD.setDefaultConverter();
        addPropertyDescriptor(labelValuesScalingFactorPD);


        PropertyDescriptor labelValuesDecimalPlacesPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY, Integer.class);
        labelValuesDecimalPlacesPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY);
        labelValuesDecimalPlacesPD.setDisplayName(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_LABEL);
        labelValuesDecimalPlacesPD.setDescription(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_TOOLTIP);
        labelValuesDecimalPlacesPD.setValueRange(new ValueRange(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MIN,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MAX));
        labelValuesDecimalPlacesPD.setDefaultConverter();
        addPropertyDescriptor(labelValuesDecimalPlacesPD);


        PropertyDescriptor labelValuesForceDecimalPlacesPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY, Boolean.class);
        labelValuesForceDecimalPlacesPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT);
        labelValuesForceDecimalPlacesPD.setDisplayName(ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_LABEL);
        labelValuesForceDecimalPlacesPD.setDescription(ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TOOLTIP);
        labelValuesForceDecimalPlacesPD.setDefaultConverter();
        addPropertyDescriptor(labelValuesForceDecimalPlacesPD);


















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

        addSectionBreak(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_KEY,
                ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_TOOLTIP);

        PropertyDescriptor locationApplySizeScalingPlacementPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_KEY,
                ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_TYPE);
        locationApplySizeScalingPlacementPD.setDefaultValue(ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_DEFAULT);
        locationApplySizeScalingPlacementPD.setDisplayName(ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_LABEL);
        locationApplySizeScalingPlacementPD.setDescription(ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_TOOLTIP);
        locationApplySizeScalingPlacementPD.setDefaultConverter();
        addPropertyDescriptor(locationApplySizeScalingPlacementPD);

        PropertyDescriptor locationSizeScalingPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_KEY,
                ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_TYPE);
        locationSizeScalingPD.setDefaultValue(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_DEFAULT);
        locationSizeScalingPD.setValueRange(new ValueRange(0.0, 100.00));
        locationSizeScalingPD.setDisplayName(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_LABEL);
        locationSizeScalingPD.setDescription(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_TOOLTIP);
        locationSizeScalingPD.setDefaultConverter();
        addPropertyDescriptor(locationSizeScalingPD);


        PropertyDescriptor legendLengthPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_LENGTH_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_LENGTH_TYPE);
        legendLengthPD.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_LENGTH_DEFAULT);
        legendLengthPD.setValueRange(new ValueRange(100, 4000));
        legendLengthPD.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_LENGTH_LABEL);
        legendLengthPD.setDescription(ColorBarLayerType.PROPERTY_LEGEND_LENGTH_TOOLTIP);
        legendLengthPD.setDefaultConverter();
        addPropertyDescriptor(legendLengthPD);


        PropertyDescriptor legendWidthPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_WIDTH_TYPE);
        legendWidthPD.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_WIDTH_DEFAULT);
        legendWidthPD.setValueRange(new ValueRange(10, 500));
        legendWidthPD.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_WIDTH_LABEL );
        legendWidthPD.setDescription(ColorBarLayerType.PROPERTY_LEGEND_WIDTH_TOOLTIP);
        legendWidthPD.setDefaultConverter();
        addPropertyDescriptor(legendWidthPD);


        PropertyDescriptor titleParameterFontSizePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_TYPE);
        titleParameterFontSizePD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_DEFAULT);
        titleParameterFontSizePD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_LABEL);
        titleParameterFontSizePD.setDescription(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_TOOLTIP);
        titleParameterFontSizePD.setValueRange(new ValueRange(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_VALUE_MIN,
                ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_VALUE_MAX));
        titleParameterFontSizePD.setDefaultConverter();
        addPropertyDescriptor(titleParameterFontSizePD);


        PropertyDescriptor titleUnitsFontSizePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_TYPE);
        titleUnitsFontSizePD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_DEFAULT);
        titleUnitsFontSizePD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_LABEL);
        titleUnitsFontSizePD.setDescription(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_TOOLTIP);
        titleUnitsFontSizePD.setValueRange(new ValueRange(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_VALUE_MIN,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_VALUE_MAX));
        titleUnitsFontSizePD.setDefaultConverter();
        addPropertyDescriptor(titleUnitsFontSizePD);


        PropertyDescriptor labelSizePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY, Integer.class);
        labelSizePD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_DEFAULT);
        labelSizePD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_LABEL);
        labelSizePD.setDescription(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_TOOLTIP);
        labelSizePD.setValueRange(new ValueRange(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_VALUE_MIN, ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_VALUE_MAX));
        labelSizePD.setDefaultConverter();
        addPropertyDescriptor(labelSizePD);







        // Color Bar Title Section

        addSectionBreak(ColorBarLayerType.PROPERTY_TITLE_SECTION_KEY,
                ColorBarLayerType.PROPERTY_TITLE_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_TITLE_SECTION_TOOLTIP);



        PropertyDescriptor titleParameterShowPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY,
                ColorBarLayerType.PROPERTY_TITLE_SHOW_TYPE);
        titleParameterShowPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_SHOW_DEFAULT);
        titleParameterShowPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_SHOW_LABEL);
        titleParameterShowPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_SHOW_TOOLTIP);
        titleParameterShowPD.setDefaultConverter();
        addPropertyDescriptor(titleParameterShowPD);



        PropertyDescriptor titleParameterFontBoldPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_TYPE);
        titleParameterFontBoldPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_DEFAULT);
        titleParameterFontBoldPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_LABEL);
        titleParameterFontBoldPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_TOOLTIP);
        titleParameterFontBoldPD.setDefaultConverter();
        addPropertyDescriptor(titleParameterFontBoldPD);

        PropertyDescriptor titleParameterFontItalicPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_TYPE);
        titleParameterFontItalicPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_DEFAULT);
        titleParameterFontItalicPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_LABEL);
        titleParameterFontItalicPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_TOOLTIP);
        titleParameterFontItalicPD.setDefaultConverter();
        addPropertyDescriptor(titleParameterFontItalicPD);


        PropertyDescriptor titleParameterFontNamePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_TYPE);
        titleParameterFontNamePD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_DEFAULT);
        titleParameterFontNamePD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_LABEL);
        titleParameterFontNamePD.setDescription(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_TOOLTIP);
        titleParameterFontNamePD.setValueSet(new ValueSet(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_VALUE_SET));
        titleParameterFontNamePD.setDefaultConverter();
        addPropertyDescriptor(titleParameterFontNamePD);






        PropertyDescriptor titleParameterFontColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY,
                ColorBarLayerType.PROPERTY_TITLE_COLOR_TYPE);
        titleParameterFontColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_COLOR_DEFAULT);
        titleParameterFontColorPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_COLOR_LABEL);
        titleParameterFontColorPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_COLOR_TOOLTIP);
        titleParameterFontColorPD.setDefaultConverter();
        addPropertyDescriptor(titleParameterFontColorPD);




        // Title Units Section

        addSectionBreak(ColorBarLayerType.PROPERTY_TITLE_UNITS_SECTION_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_SECTION_TOOLTIP);




        PropertyDescriptor titleUnitsShowPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_TYPE);
        titleUnitsShowPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_DEFAULT);
        titleUnitsShowPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_LABEL);
        titleUnitsShowPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_TOOLTIP);
        titleUnitsShowPD.setDefaultConverter();
        addPropertyDescriptor(titleUnitsShowPD);

        PropertyDescriptor titleUnitsFontBoldPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_BOLD_TYPE);
        titleUnitsFontBoldPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_BOLD_DEFAULT);
        titleUnitsFontBoldPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_BOLD_LABEL);
        titleUnitsFontBoldPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_BOLD_TOOLTIP);
        titleUnitsFontBoldPD.setDefaultConverter();
        addPropertyDescriptor(titleUnitsFontBoldPD);

        PropertyDescriptor titleUnitsFontItalicPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_ITALIC_TYPE);
        titleUnitsFontItalicPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_ITALIC_DEFAULT);
        titleUnitsFontItalicPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_ITALIC_LABEL);
        titleUnitsFontItalicPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_ITALIC_TOOLTIP);
        titleUnitsFontItalicPD.setDefaultConverter();
        addPropertyDescriptor(titleUnitsFontItalicPD);


        PropertyDescriptor titleUnitsFontNamePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_NAME_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_NAME_TYPE);
        titleUnitsFontNamePD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_NAME_DEFAULT);
        titleUnitsFontNamePD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_NAME_LABEL);
        titleUnitsFontNamePD.setDescription(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_NAME_TOOLTIP);
        titleUnitsFontNamePD.setValueSet(new ValueSet(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_NAME_VALUE_SET));
        titleUnitsFontNamePD.setDefaultConverter();
        addPropertyDescriptor(titleUnitsFontNamePD);



        PropertyDescriptor titleUnitsFontColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_COLOR_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_COLOR_TYPE);
        titleUnitsFontColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_COLOR_DEFAULT);
        titleUnitsFontColorPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_COLOR_LABEL);
        titleUnitsFontColorPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_COLOR_TOOLTIP);
        titleUnitsFontColorPD.setDefaultConverter();
        addPropertyDescriptor(titleUnitsFontColorPD);












        // Labels Section

        addSectionBreak(ColorBarLayerType.PROPERTY_LABELS_SECTION_KEY,
                ColorBarLayerType.PROPERTY_LABELS_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_LABELS_SECTION_TOOLTIP);




        PropertyDescriptor labelsShowPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, Boolean.class);
        labelsShowPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_SHOW_DEFAULT);
        labelsShowPD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_SHOW_LABEL);
        labelsShowPD.setDescription(ColorBarLayerType.PROPERTY_LABELS_SHOW_TOOLTIP);
        labelsShowPD.setDefaultConverter();
        addPropertyDescriptor(labelsShowPD);

        PropertyDescriptor labelsItalicsPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY, Boolean.class);
        labelsItalicsPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_DEFAULT);
        labelsItalicsPD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_LABEL);
        labelsItalicsPD.setDescription(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_TOOLTIP);
        labelsItalicsPD.setDefaultConverter();
        addPropertyDescriptor(labelsItalicsPD);

        PropertyDescriptor labelsBoldPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY, Boolean.class);
        labelsBoldPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_DEFAULT);
        labelsBoldPD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_LABEL);
        labelsBoldPD.setDescription(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_TOOLTIP);
        labelsBoldPD.setDefaultConverter();
        addPropertyDescriptor(labelsBoldPD);


        PropertyDescriptor labelsFontPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY, String.class);
        labelsFontPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_DEFAULT);
        labelsFontPD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_LABEL);
        labelsFontPD.setDescription(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_TOOLTIP);
        labelsFontPD.setValueSet(new ValueSet(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_VALUE_SET));
        labelsFontPD.setDefaultConverter();
        addPropertyDescriptor(labelsFontPD);



        PropertyDescriptor labelColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY, Color.class);
        labelColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_DEFAULT);
        labelColorPD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_LABEL);
        labelColorPD.setDescription(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_TOOLTIP);
        labelColorPD.setDefaultConverter();
        addPropertyDescriptor(labelColorPD);














        // Tickmarks Section

        addSectionBreak(ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_TOOLTIP);


        PropertyDescriptor tickmarksShowPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, Boolean.class);
        tickmarksShowPD.setDefaultValue(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT);
        tickmarksShowPD.setDisplayName(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_LABEL);
        tickmarksShowPD.setDescription(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_TOOLTIP);
        tickmarksShowPD.setDefaultConverter();
        addPropertyDescriptor(tickmarksShowPD);


        PropertyDescriptor tickmarksLengthPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY, Integer.class);
        tickmarksLengthPD.setDefaultValue(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT);
        tickmarksLengthPD.setDisplayName(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_LABEL);
        tickmarksLengthPD.setDescription(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_TOOLTIP);
        tickmarksLengthPD.setDefaultConverter();
        addPropertyDescriptor(tickmarksLengthPD);

        PropertyDescriptor tickmarksWidthPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_KEY, Integer.class);
        tickmarksWidthPD.setDefaultValue(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_DEFAULT);
        tickmarksWidthPD.setDisplayName(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_LABEL);
        tickmarksWidthPD.setDescription(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_TOOLTIP);
        tickmarksWidthPD.setDefaultConverter();
        addPropertyDescriptor(tickmarksWidthPD);




        PropertyDescriptor tickmarksColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY, Color.class);
        tickmarksColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);
        tickmarksColorPD.setDisplayName(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_LABEL);
        tickmarksColorPD.setDescription(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_TOOLTIP);
        tickmarksColorPD.setDefaultConverter();
        addPropertyDescriptor(tickmarksColorPD);



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








        // Border Section

        addSectionBreak(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SECTION_KEY,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_SECTION_TOOLTIP);

        PropertyDescriptor borderShowPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY, Boolean.class);
        borderShowPD.setDefaultValue(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_DEFAULT);
        borderShowPD.setDisplayName(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_LABEL);
        borderShowPD.setDescription(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_TOOLTIP);
        borderShowPD.setDefaultConverter();
        addPropertyDescriptor(borderShowPD);

        PropertyDescriptor borderWidthPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_TYPE);
        borderWidthPD.setDefaultValue(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_DEFAULT);
        borderWidthPD.setDisplayName(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_LABEL);
        borderWidthPD.setDescription(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_TOOLTIP);
        borderWidthPD.setDefaultConverter();
        addPropertyDescriptor(borderWidthPD);

        PropertyDescriptor borderColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY, Color.class);
        borderColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_DEFAULT);
        borderColorPD.setDisplayName(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_LABEL);
        borderColorPD.setDescription(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_TOOLTIP);
        borderColorPD.setDefaultConverter();
        addPropertyDescriptor(borderColorPD);



        addSectionBreak(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_TOOLTIP);


        PropertyDescriptor backdropBorderShowPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_KEY, Boolean.class);
        backdropBorderShowPD.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_DEFAULT);
        backdropBorderShowPD.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_LABEL);
        backdropBorderShowPD.setDescription(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_TOOLTIP);
        backdropBorderShowPD.setDefaultConverter();
        addPropertyDescriptor(backdropBorderShowPD);

        PropertyDescriptor backdropBorderWidthPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_TYPE);
        backdropBorderWidthPD.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_DEFAULT);
        backdropBorderWidthPD.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_LABEL);
        backdropBorderWidthPD.setDescription(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_TOOLTIP);
        backdropBorderWidthPD.setDefaultConverter();
        addPropertyDescriptor(backdropBorderWidthPD);

        PropertyDescriptor backdropBorderColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY, Color.class);
        backdropBorderColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_DEFAULT);
        backdropBorderColorPD.setDisplayName(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_LABEL);
        backdropBorderColorPD.setDescription(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_TOOLTIP);
        backdropBorderColorPD.setDefaultConverter();
        addPropertyDescriptor(backdropBorderColorPD);


















        BindingContext bindingContext = getBindingContext();


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY, true,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY, ColorBarLayerType.DISTRIB_EVEN_STR);

        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY, true,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY, ColorBarLayerType.DISTRIB_MANUAL_STR);







//
//        boolean applySizeScaling = (Boolean) bindingContext.getPropertySet().getValue(
//                ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_NAME);
//
//
//        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SIZE_NAME, applySizeScaling,
//                ColorBarLayerType.PROPERTY_IMAGE_SCALING_APPLY_SIZE_NAME, applySizeScaling);







        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_TEXT_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY, true);






        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_BOLD_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_ITALIC_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_NAME_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_COLOR_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_KEY, true,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_KEY, true);






        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY, true,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY, true,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY, true,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY, true,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, true);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY, true,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY, true);



//
//        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_KEY, showTitle,
//                ColorBarLayerType.PROPERTY_TITLE_PARAMETER_SHOW_KEY, showTitle);











        boolean borderEnabled = (Boolean) bindingContext.getPropertySet().getValue(
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY);

        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY, borderEnabled,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY, borderEnabled);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_KEY, borderEnabled,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY, borderEnabled);


        // Set enablement associated with "Labels Inside" checkbox




        boolean tickMarkEnabled = (Boolean) bindingContext.getPropertySet().getValue(
                ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY, tickMarkEnabled,
                ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, tickMarkEnabled);

        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_KEY, tickMarkEnabled,
                ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, tickMarkEnabled);

        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY, tickMarkEnabled,
                ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY, tickMarkEnabled);


    }


    private void addSectionBreak(String name, String label, String toolTip) {
        PropertyDescriptor descriptor = new PropertyDescriptor(name, Boolean.class);
        descriptor.setDisplayName(label);
        descriptor.setDescription(toolTip);
        addPropertyDescriptor(descriptor);
    }


}
