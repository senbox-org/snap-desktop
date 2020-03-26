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


        // Formatting Section

        addSectionBreak(ColorBarLayerType.PROPERTY_FORMATTING_SECTION_KEY,
                ColorBarLayerType.PROPERTY_FORMATTING_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_FORMATTING_SECTION_TOOLTIP);


        PropertyDescriptor orientationPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_FORMATTING_ORIENTATION_KEY, String.class);
        orientationPD.setDefaultValue(ColorBarLayerType.PROPERTY_FORMATTING_ORIENTATION_DEFAULT);
        orientationPD.setDisplayName(ColorBarLayerType.PROPERTY_FORMATTING_ORIENTATION_LABEL);
        orientationPD.setDescription(ColorBarLayerType.PROPERTY_FORMATTING_ORIENTATION_TOOLTIP);
        orientationPD.setValueSet(new ValueSet(ColorBarLayerType.PROPERTY_FORMATTING_ORIENTATION_VALUE_SET));
        orientationPD.setDefaultConverter();
        addPropertyDescriptor(orientationPD);









        // Color Bar Location Section

        addSectionBreak(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_SECTION_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_SECTION_TOOLTIP);

        PropertyDescriptor locationInsidePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_INSIDE_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_INSIDE_TYPE);
        locationInsidePD.setDefaultValue(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_INSIDE_DEFAULT);
        locationInsidePD.setDisplayName(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_INSIDE_LABEL);
        locationInsidePD.setDescription(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_INSIDE_TOOLTIP);
        locationInsidePD.setDefaultConverter();
        addPropertyDescriptor(locationInsidePD);

        PropertyDescriptor locationPlacementPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_PLACEMENT_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_PLACEMENT_TYPE);
        locationPlacementPD.setDefaultValue(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_PLACEMENT_DEFAULT);
        locationPlacementPD.setValueSet(new ValueSet(ColorBarLayerType.getColorBarLocationArray()));
        locationPlacementPD.setDisplayName(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_PLACEMENT_LABEL);
        locationPlacementPD.setDescription(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_PLACEMENT_TOOLTIP);
        locationPlacementPD.setDefaultConverter();
        addPropertyDescriptor(locationPlacementPD);


        PropertyDescriptor locationOffsetPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_OFFSET_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_OFFSET_TYPE);
        locationOffsetPD.setDefaultValue(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_OFFSET_DEFAULT);
        locationOffsetPD.setDisplayName(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_OFFSET_LABEL);
        locationOffsetPD.setDescription(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_OFFSET_TOOLTIP);
        locationOffsetPD.setDefaultConverter();
        addPropertyDescriptor(locationOffsetPD);

        PropertyDescriptor locationShiftPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_SHIFT_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_SHIFT_TYPE);
        locationShiftPD.setDefaultValue(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_SHIFT_DEFAULT);
        locationShiftPD.setDisplayName(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_SHIFT_LABEL);
        locationShiftPD.setDescription(ColorBarLayerType.PROPERTY_COLORBAR_LOCATION_SHIFT_TOOLTIP);
        locationShiftPD.setDefaultConverter();
        addPropertyDescriptor(locationShiftPD);







        // Color Bar Scaling Section

        addSectionBreak(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SECTION_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SECTION_TOOLTIP);

        PropertyDescriptor locationApplySizeScalingPlacementPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_TYPE);
        locationApplySizeScalingPlacementPD.setDefaultValue(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_DEFAULT);
        locationApplySizeScalingPlacementPD.setDisplayName(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_LABEL);
        locationApplySizeScalingPlacementPD.setDescription(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_TOOLTIP);
        locationApplySizeScalingPlacementPD.setDefaultConverter();
        addPropertyDescriptor(locationApplySizeScalingPlacementPD);

        PropertyDescriptor locationSizeScalingPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SIZE_SCALING_NAME,
                ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SIZE_SCALING_TYPE);
        locationSizeScalingPD.setDefaultValue(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SIZE_SCALING_DEFAULT);
        locationSizeScalingPD.setValueRange(new ValueRange(0.0, 100.00));
        locationSizeScalingPD.setDisplayName(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SIZE_SCALING_LABEL);
        locationSizeScalingPD.setDescription(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SIZE_SCALING_TOOLTIP);
        locationSizeScalingPD.setDefaultConverter();
        addPropertyDescriptor(locationSizeScalingPD);




        // Color Bar Title Section

        addSectionBreak(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SECTION_KEY,
                ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SECTION_TOOLTIP);

        PropertyDescriptor titleShowPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SHOW_TITLE_KEY,
                ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SHOW_TITLE_TYPE);
        titleShowPD.setDefaultValue(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SHOW_TITLE_DEFAULT);
        titleShowPD.setDisplayName(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SHOW_TITLE_LABEL);
        titleShowPD.setDescription(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SHOW_TITLE_TOOLTIP);
        titleShowPD.setDefaultConverter();
        addPropertyDescriptor(titleShowPD);

        PropertyDescriptor titleValuePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_TITLE_KEY,
                ColorBarLayerType.PROPERTY_COLORBAR_TITLE_TITLE_TYPE);
        titleValuePD.setDefaultValue(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_TITLE_DEFAULT);
        titleValuePD.setDisplayName(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_TITLE_LABEL);
        titleValuePD.setDescription(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_TITLE_TOOLTIP);
        titleValuePD.setDefaultConverter();
        addPropertyDescriptor(titleValuePD);

        PropertyDescriptor titleUnitsPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_UNITS_KEY,
                ColorBarLayerType.PROPERTY_COLORBAR_TITLE_UNITS_TYPE);
        titleUnitsPD.setDefaultValue(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_UNITS_DEFAULT);
        titleUnitsPD.setDisplayName(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_UNITS_LABEL);
        titleUnitsPD.setDescription(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_UNITS_TOOLTIP);
        titleUnitsPD.setDefaultConverter();
        addPropertyDescriptor(titleUnitsPD);



        PropertyDescriptor titleColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY, Color.class);
        titleColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_TITLE_COLOR_DEFAULT);
        titleColorPD.setDisplayName(ColorBarLayerType.PROPERTY_TITLE_COLOR_LABEL);
        titleColorPD.setDescription(ColorBarLayerType.PROPERTY_TITLE_COLOR_TOOLTIP);
        titleColorPD.setDefaultConverter();
        addPropertyDescriptor(titleColorPD);



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

        PropertyDescriptor tickmarksColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY, Color.class);
        tickmarksColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);
        tickmarksColorPD.setDisplayName(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_LABEL);
        tickmarksColorPD.setDescription(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_TOOLTIP);
        tickmarksColorPD.setDefaultConverter();
        addPropertyDescriptor(tickmarksColorPD);

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



        // Border Section

        addSectionBreak(ColorBarLayerType.PROPERTY_BORDER_SECTION_KEY,
                ColorBarLayerType.PROPERTY_BORDER_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_BORDER_SECTION_TOOLTIP);

        PropertyDescriptor borderShowPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY, Boolean.class);
        borderShowPD.setDefaultValue(ColorBarLayerType.PROPERTY_BORDER_SHOW_DEFAULT);
        borderShowPD.setDisplayName(ColorBarLayerType.PROPERTY_BORDER_SHOW_LABEL);
        borderShowPD.setDescription(ColorBarLayerType.PROPERTY_BORDER_SHOW_TOOLTIP);
        borderShowPD.setDefaultConverter();
        addPropertyDescriptor(borderShowPD);

        PropertyDescriptor borderWidthPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_BORDER_WIDTH_KEY, ColorBarLayerType.PROPERTY_BORDER_WIDTH_TYPE);
        borderWidthPD.setDefaultValue(ColorBarLayerType.PROPERTY_BORDER_WIDTH_DEFAULT);
        borderWidthPD.setDisplayName(ColorBarLayerType.PROPERTY_BORDER_WIDTH_LABEL);
        borderWidthPD.setDescription(ColorBarLayerType.PROPERTY_BORDER_WIDTH_TOOLTIP);
        borderWidthPD.setDefaultConverter();
        addPropertyDescriptor(borderWidthPD);

        PropertyDescriptor borderColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_BORDER_COLOR_KEY, Color.class);
        borderColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_BORDER_COLOR_DEFAULT);
        borderColorPD.setDisplayName(ColorBarLayerType.PROPERTY_BORDER_COLOR_LABEL);
        borderColorPD.setDescription(ColorBarLayerType.PROPERTY_BORDER_COLOR_TOOLTIP);
        borderColorPD.setDefaultConverter();
        addPropertyDescriptor(borderColorPD);



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















        // Labels Section

        addSectionBreak(ColorBarLayerType.PROPERTY_LABELS_SECTION_NAME,
                ColorBarLayerType.PROPERTY_LABELS_SECTION_LABEL,
                ColorBarLayerType.PROPERTY_LABELS_SECTION_TOOLTIP);





        PropertyDescriptor labelsItalicsPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_ITALIC_NAME, Boolean.class);
        labelsItalicsPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_ITALIC_DEFAULT);
        labelsItalicsPD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_ITALIC_LABEL);
        labelsItalicsPD.setDescription(ColorBarLayerType.PROPERTY_LABELS_ITALIC_TOOLTIP);
        labelsItalicsPD.setDefaultConverter();
        addPropertyDescriptor(labelsItalicsPD);

        PropertyDescriptor labelsBoldPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_BOLD_NAME, Boolean.class);
        labelsBoldPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_BOLD_DEFAULT);
        labelsBoldPD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_BOLD_LABEL);
        labelsBoldPD.setDescription(ColorBarLayerType.PROPERTY_LABELS_BOLD_TOOLTIP);
        labelsBoldPD.setDefaultConverter();
        addPropertyDescriptor(labelsBoldPD);


        PropertyDescriptor labelsFontPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME, String.class);
        labelsFontPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_FONT_DEFAULT);
        labelsFontPD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_FONT_LABEL);
        labelsFontPD.setDescription(ColorBarLayerType.PROPERTY_LABELS_FONT_TOOLTIP);
        labelsFontPD.setValueSet(new ValueSet(ColorBarLayerType.PROPERTY_LABELS_FONT_VALUE_SET));
        labelsFontPD.setDefaultConverter();
        addPropertyDescriptor(labelsFontPD);

        PropertyDescriptor labelSizePD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_SIZE_NAME, Integer.class);
        labelSizePD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_SIZE_DEFAULT);
        labelSizePD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_SIZE_LABEL);
        labelSizePD.setDescription(ColorBarLayerType.PROPERTY_LABELS_SIZE_TOOLTIP);
        labelSizePD.setValueRange(new ValueRange(ColorBarLayerType.PROPERTY_LABELS_SIZE_VALUE_MIN, ColorBarLayerType.PROPERTY_LABELS_SIZE_VALUE_MAX));
        labelSizePD.setDefaultConverter();
        addPropertyDescriptor(labelSizePD);

        PropertyDescriptor labelColorPD = new PropertyDescriptor(ColorBarLayerType.PROPERTY_LABELS_COLOR_NAME, Color.class);
        labelColorPD.setDefaultValue(ColorBarLayerType.PROPERTY_LABELS_COLOR_DEFAULT);
        labelColorPD.setDisplayName(ColorBarLayerType.PROPERTY_LABELS_COLOR_LABEL);
        labelColorPD.setDescription(ColorBarLayerType.PROPERTY_LABELS_COLOR_TOOLTIP);
        labelColorPD.setDefaultConverter();
        addPropertyDescriptor(labelColorPD);

















        BindingContext bindingContext = getBindingContext();


        boolean applySizeScaling = (Boolean) bindingContext.getPropertySet().getValue(
                ColorBarLayerType.PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_NAME);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_COLORBAR_SCALING_SIZE_SCALING_NAME, applySizeScaling,
                ColorBarLayerType.PROPERTY_COLORBAR_SCALING_APPLY_SIZE_SCALING_NAME, applySizeScaling);




        boolean showTitle = (Boolean) bindingContext.getPropertySet().getValue(
                ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SHOW_TITLE_KEY);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_TITLE_KEY, showTitle,
                ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SHOW_TITLE_KEY, showTitle);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_COLORBAR_TITLE_UNITS_KEY, showTitle,
                ColorBarLayerType.PROPERTY_COLORBAR_TITLE_SHOW_TITLE_KEY, showTitle);











        boolean borderEnabled = (Boolean) bindingContext.getPropertySet().getValue(
                ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY);

        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_BORDER_COLOR_KEY, borderEnabled,
                ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY, borderEnabled);


        bindingContext.bindEnabledState(ColorBarLayerType.PROPERTY_BORDER_WIDTH_KEY, borderEnabled,
                ColorBarLayerType.PROPERTY_BORDER_SHOW_KEY, borderEnabled);


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
