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
import org.esa.snap.core.layer.GraticuleLayerType;
import org.esa.snap.ui.layer.AbstractLayerConfigurationEditor;

import java.awt.*;

/**
 * Editor for graticule layer.
 *
 * @author Marco Zuehlke
 * @author Daniel Knowles
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
//SEP2018 - Daniel Knowles - adds numerous new properties and related binding contexts.
//JAN2018 - Daniel Knowles - updated with SeaDAS gridline revisions

public class GraticuleLayerEditor extends AbstractLayerConfigurationEditor {


    @Override
    protected void addEditablePropertyDescriptors() {


        // Grid Spacing Section

        addSectionBreak(GraticuleLayerType.PROPERTY_GRID_SPACING_SECTION_NAME,
                GraticuleLayerType.PROPERTY_GRID_SPACING_SECTION_LABEL,
                GraticuleLayerType.PROPERTY_GRID_SPACING_SECTION_TOOLTIP);

        PropertyDescriptor gridSpacingLatPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_NAME, Double.class);
        gridSpacingLatPD.setDefaultValue(GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_DEFAULT);
        gridSpacingLatPD.setValueRange(new ValueRange(0.0, 90.00));
        gridSpacingLatPD.setDisplayName(GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_LABEL);
        gridSpacingLatPD.setDescription(GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_TOOLTIP);
        gridSpacingLatPD.setDefaultConverter();
        addPropertyDescriptor(gridSpacingLatPD);

        PropertyDescriptor gridSpacingLonPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_GRID_SPACING_LON_NAME, Double.class);
        gridSpacingLonPD.setDefaultValue(GraticuleLayerType.PROPERTY_GRID_SPACING_LON_DEFAULT);
        gridSpacingLonPD.setValueRange(new ValueRange(0.0, 180.00));
        gridSpacingLonPD.setDisplayName(GraticuleLayerType.PROPERTY_GRID_SPACING_LON_LABEL);
        gridSpacingLonPD.setDescription(GraticuleLayerType.PROPERTY_GRID_SPACING_LON_TOOLTIP);
        gridSpacingLonPD.setDefaultConverter();
        addPropertyDescriptor(gridSpacingLonPD);


        PropertyDescriptor numGridLinesPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NUM_GRID_LINES_NAME, Integer.class);
        numGridLinesPD.setDefaultValue(GraticuleLayerType.PROPERTY_NUM_GRID_LINES_DEFAULT);
        numGridLinesPD.setValueRange(new ValueRange(0, 40));
        numGridLinesPD.setDisplayName(GraticuleLayerType.PROPERTY_NUM_GRID_LINES_LABEL);
        numGridLinesPD.setDescription(GraticuleLayerType.PROPERTY_NUM_GRID_LINES_TOOLTIP);
        numGridLinesPD.setDefaultConverter();
        addPropertyDescriptor(numGridLinesPD);

        PropertyDescriptor numMinorStepsPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_MINOR_STEPS_NAME, Integer.class);
        numMinorStepsPD.setDefaultValue(GraticuleLayerType.PROPERTY_MINOR_STEPS_DEFAULT);
        numMinorStepsPD.setValueRange(new ValueRange(0, 20));
        numMinorStepsPD.setDisplayName(GraticuleLayerType.PROPERTY_MINOR_STEPS_LABEL);
        numMinorStepsPD.setDescription(GraticuleLayerType.PROPERTY_MINOR_STEPS_TOOLTIP);
        numMinorStepsPD.setDefaultConverter();
        addPropertyDescriptor(numMinorStepsPD);

        // Labels Section

        addSectionBreak(GraticuleLayerType.PROPERTY_LABELS_SECTION_NAME,
                GraticuleLayerType.PROPERTY_LABELS_SECTION_LABEL,
                GraticuleLayerType.PROPERTY_LABELS_SECTION_TOOLTIP);

        PropertyDescriptor labelsNorthPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_NORTH_NAME, Boolean.class);
        labelsNorthPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_NORTH_DEFAULT);
        labelsNorthPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_NORTH_LABEL);
        labelsNorthPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_NORTH_TOOLTIP);
        labelsNorthPD.setDefaultConverter();
        addPropertyDescriptor(labelsNorthPD);

        PropertyDescriptor labelsSouthPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_SOUTH_NAME, Boolean.class);
        labelsSouthPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_SOUTH_DEFAULT);
        labelsSouthPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_SOUTH_LABEL);
        labelsSouthPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_SOUTH_TOOLTIP);
        labelsSouthPD.setDefaultConverter();
        addPropertyDescriptor(labelsSouthPD);

        PropertyDescriptor labelsWestPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_WEST_NAME, Boolean.class);
        labelsWestPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_WEST_DEFAULT);
        labelsWestPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_WEST_LABEL);
        labelsWestPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_WEST_TOOLTIP);
        labelsWestPD.setDefaultConverter();
        addPropertyDescriptor(labelsWestPD);

        PropertyDescriptor labelsEastPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_EAST_NAME, Boolean.class);
        labelsEastPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_EAST_DEFAULT);
        labelsEastPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_EAST_LABEL);
        labelsEastPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_EAST_TOOLTIP);
        labelsEastPD.setDefaultConverter();
        addPropertyDescriptor(labelsEastPD);

        PropertyDescriptor labelsSuffixPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_NAME, Boolean.class);
        labelsSuffixPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_DEFAULT);
        labelsSuffixPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_LABEL);
        labelsSuffixPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_TOOLTIP);
        labelsSuffixPD.setDefaultConverter();
        addPropertyDescriptor(labelsSuffixPD);

        PropertyDescriptor labelsDecimalPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_NAME, Boolean.class);
        labelsDecimalPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_DEFAULT);
        labelsDecimalPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_LABEL);
        labelsDecimalPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_TOOLTIP);
        labelsDecimalPD.setDefaultConverter();
        addPropertyDescriptor(labelsDecimalPD);

        PropertyDescriptor labelsInsidePD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME, Boolean.class);
        labelsInsidePD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_INSIDE_DEFAULT);
        labelsInsidePD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_INSIDE_LABEL);
        labelsInsidePD.setDescription(GraticuleLayerType.PROPERTY_LABELS_INSIDE_TOOLTIP);
        labelsInsidePD.setDefaultConverter();
        addPropertyDescriptor(labelsInsidePD);

        PropertyDescriptor labelsItalicsPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_ITALIC_NAME, Boolean.class);
        labelsItalicsPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_ITALIC_DEFAULT);
        labelsItalicsPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_ITALIC_LABEL);
        labelsItalicsPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_ITALIC_TOOLTIP);
        labelsItalicsPD.setDefaultConverter();
        addPropertyDescriptor(labelsItalicsPD);

        PropertyDescriptor labelsBoldPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_BOLD_NAME, Boolean.class);
        labelsBoldPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_BOLD_DEFAULT);
        labelsBoldPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_BOLD_LABEL);
        labelsBoldPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_BOLD_TOOLTIP);
        labelsBoldPD.setDefaultConverter();
        addPropertyDescriptor(labelsBoldPD);

        PropertyDescriptor labelsRotationLatPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_NAME, Double.class);
        labelsRotationLatPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_DEFAULT);
        labelsRotationLatPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_LABEL);
        labelsRotationLatPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_TOOLTIP);
        labelsRotationLatPD.setDefaultConverter();
        labelsRotationLatPD.setValueRange(new ValueRange(0, 90));
        addPropertyDescriptor(labelsRotationLatPD);

        PropertyDescriptor labelsRotationLonPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_NAME, Double.class);
        labelsRotationLonPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_DEFAULT);
        labelsRotationLonPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_LABEL);
        labelsRotationLonPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_TOOLTIP);
        labelsRotationLonPD.setDefaultConverter();
        labelsRotationLonPD.setValueRange(new ValueRange(0, 90));
        addPropertyDescriptor(labelsRotationLonPD);

        PropertyDescriptor labelsFontPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_FONT_NAME, String.class);
        labelsFontPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_FONT_DEFAULT);
        labelsFontPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_FONT_LABEL);
        labelsFontPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_FONT_TOOLTIP);
        labelsFontPD.setValueSet(new ValueSet(GraticuleLayerType.PROPERTY_LABELS_FONT_VALUE_SET));
        labelsFontPD.setDefaultConverter();
        addPropertyDescriptor(labelsFontPD);

        PropertyDescriptor labelSizePD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_SIZE_NAME, Integer.class);
        labelSizePD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_SIZE_DEFAULT);
        labelSizePD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_SIZE_LABEL);
        labelSizePD.setDescription(GraticuleLayerType.PROPERTY_LABELS_SIZE_TOOLTIP);
        labelSizePD.setValueRange(new ValueRange(GraticuleLayerType.PROPERTY_LABELS_SIZE_VALUE_MIN, GraticuleLayerType.PROPERTY_LABELS_SIZE_VALUE_MAX));
        labelSizePD.setDefaultConverter();
        addPropertyDescriptor(labelSizePD);

        PropertyDescriptor labelColorPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_LABELS_COLOR_NAME, Color.class);
        labelColorPD.setDefaultValue(GraticuleLayerType.PROPERTY_LABELS_COLOR_DEFAULT);
        labelColorPD.setDisplayName(GraticuleLayerType.PROPERTY_LABELS_COLOR_LABEL);
        labelColorPD.setDescription(GraticuleLayerType.PROPERTY_LABELS_COLOR_TOOLTIP);
        labelColorPD.setDefaultConverter();
        addPropertyDescriptor(labelColorPD);


        // Gridlines Section

        addSectionBreak(GraticuleLayerType.PROPERTY_GRIDLINES_SECTION_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_SECTION_LABEL,
                GraticuleLayerType.PROPERTY_GRIDLINES_SECTION_TOOLTIP);


        PropertyDescriptor gridlinesShowPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, Boolean.class);
        gridlinesShowPD.setDefaultValue(GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_DEFAULT);
        gridlinesShowPD.setDisplayName(GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_LABEL);
        gridlinesShowPD.setDescription(GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_TOOLTIP);
        gridlinesShowPD.setDefaultConverter();
        addPropertyDescriptor(gridlinesShowPD);

        PropertyDescriptor girdlinesWidthPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_NAME, Double.class);
        girdlinesWidthPD.setDefaultValue(GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_DEFAULT);
        girdlinesWidthPD.setDisplayName(GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_LABEL);
        girdlinesWidthPD.setDescription(GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_TOOLTIP);
        girdlinesWidthPD.setDefaultConverter();
        addPropertyDescriptor(girdlinesWidthPD);

        PropertyDescriptor gridlinesDashedPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_NAME, Double.class);
        gridlinesDashedPD.setDefaultValue(GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_DEFAULT);
        gridlinesDashedPD.setDisplayName(GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_LABEL);
        gridlinesDashedPD.setDescription(GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_TOOLTIP);
        gridlinesDashedPD.setDefaultConverter();
        addPropertyDescriptor(gridlinesDashedPD);

        PropertyDescriptor gridlinesTransparencyPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_NAME, Double.class);
        gridlinesTransparencyPD.setDefaultValue(GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_DEFAULT);
        gridlinesTransparencyPD.setValueRange(new ValueRange(0, 1));
        gridlinesTransparencyPD.setDisplayName(GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_LABEL);
        gridlinesTransparencyPD.setDescription(GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_TOOLTIP);
        gridlinesTransparencyPD.setDefaultConverter();
        addPropertyDescriptor(gridlinesTransparencyPD);

        PropertyDescriptor gridlinesColorPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_NAME, Color.class);
        gridlinesColorPD.setDefaultValue(GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_DEFAULT);
        gridlinesColorPD.setDisplayName(GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_LABEL);
        gridlinesColorPD.setDescription(GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_TOOLTIP);
        gridlinesColorPD.setDefaultConverter();
        addPropertyDescriptor(gridlinesColorPD);


        // Border Section

        addSectionBreak(GraticuleLayerType.PROPERTY_BORDER_SECTION_NAME,
                GraticuleLayerType.PROPERTY_BORDER_SECTION_LABEL,
                GraticuleLayerType.PROPERTY_BORDER_SECTION_TOOLTIP);

        PropertyDescriptor borderShowPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_BORDER_SHOW_NAME, Boolean.class);
        borderShowPD.setDefaultValue(GraticuleLayerType.PROPERTY_BORDER_SHOW_DEFAULT);
        borderShowPD.setDisplayName(GraticuleLayerType.PROPERTY_BORDER_SHOW_LABEL);
        borderShowPD.setDescription(GraticuleLayerType.PROPERTY_BORDER_SHOW_TOOLTIP);
        borderShowPD.setDefaultConverter();
        addPropertyDescriptor(borderShowPD);

        PropertyDescriptor borderWidthPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_BORDER_WIDTH_NAME, Double.class);
        borderWidthPD.setDefaultValue(GraticuleLayerType.PROPERTY_BORDER_WIDTH_DEFAULT);
        borderWidthPD.setDisplayName(GraticuleLayerType.PROPERTY_BORDER_WIDTH_LABEL);
        borderWidthPD.setDescription(GraticuleLayerType.PROPERTY_BORDER_WIDTH_TOOLTIP);
        borderWidthPD.setDefaultConverter();
        addPropertyDescriptor(borderWidthPD);

        PropertyDescriptor borderColorPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_BORDER_COLOR_NAME, Color.class);
        borderColorPD.setDefaultValue(GraticuleLayerType.PROPERTY_BORDER_COLOR_DEFAULT);
        borderColorPD.setDisplayName(GraticuleLayerType.PROPERTY_BORDER_COLOR_LABEL);
        borderColorPD.setDescription(GraticuleLayerType.PROPERTY_BORDER_COLOR_TOOLTIP);
        borderColorPD.setDefaultConverter();
        addPropertyDescriptor(borderColorPD);


        // Tickmark Section

        addSectionBreak(GraticuleLayerType.PROPERTY_TICKMARKS_SECTION_NAME,
                GraticuleLayerType.PROPERTY_TICKMARKS_SECTION_LABEL,
                GraticuleLayerType.PROPERTY_TICKMARKS_SECTION_TOOLTIP);

        PropertyDescriptor tickmarksShowPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME, Boolean.class);
        tickmarksShowPD.setDefaultValue(GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT);
        tickmarksShowPD.setDisplayName(GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_LABEL);
        tickmarksShowPD.setDescription(GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_TOOLTIP);
        tickmarksShowPD.setDefaultConverter();
        addPropertyDescriptor(tickmarksShowPD);

        PropertyDescriptor tickmarksInsidePD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_NAME, Boolean.class);
        tickmarksInsidePD.setDefaultValue(GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_DEFAULT);
        tickmarksInsidePD.setDisplayName(GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_LABEL);
        tickmarksInsidePD.setDescription(GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_TOOLTIP);
        tickmarksInsidePD.setDefaultConverter();
        addPropertyDescriptor(tickmarksInsidePD);

        PropertyDescriptor tickmarksLengthPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_NAME, Double.class);
        tickmarksLengthPD.setDefaultValue(GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT);
        tickmarksLengthPD.setDisplayName(GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_LABEL);
        tickmarksLengthPD.setDescription(GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_TOOLTIP);
        tickmarksLengthPD.setDefaultConverter();
        addPropertyDescriptor(tickmarksLengthPD);

        PropertyDescriptor tickmarksColorPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_NAME, Color.class);
        tickmarksColorPD.setDefaultValue(GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);
        tickmarksColorPD.setDisplayName(GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_LABEL);
        tickmarksColorPD.setDescription(GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_TOOLTIP);
        tickmarksColorPD.setDefaultConverter();
        addPropertyDescriptor(tickmarksColorPD);


        // Corner Label Section

        addSectionBreak(GraticuleLayerType.PROPERTY_CORNER_LABELS_SECTION_NAME,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_SECTION_LABEL,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_SECTION_TOOLTIP);

        PropertyDescriptor cornerLabelsNorthPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_NAME, Boolean.class);
        cornerLabelsNorthPD.setDefaultValue(GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_DEFAULT);
        cornerLabelsNorthPD.setDisplayName(GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_LABEL);
        cornerLabelsNorthPD.setDescription(GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_TOOLTIP);
        cornerLabelsNorthPD.setDefaultConverter();
        addPropertyDescriptor(cornerLabelsNorthPD);

        PropertyDescriptor cornerLabelsSouthPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_NAME, Boolean.class);
        cornerLabelsSouthPD.setDefaultValue(GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_DEFAULT);
        cornerLabelsSouthPD.setDisplayName(GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_LABEL);
        cornerLabelsSouthPD.setDescription(GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_TOOLTIP);
        cornerLabelsSouthPD.setDefaultConverter();
        addPropertyDescriptor(cornerLabelsSouthPD);

        PropertyDescriptor cornerLabelsWestPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_NAME, Boolean.class);
        cornerLabelsWestPD.setDefaultValue(GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_DEFAULT);
        cornerLabelsWestPD.setDisplayName(GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_LABEL);
        cornerLabelsWestPD.setDescription(GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_TOOLTIP);
        cornerLabelsWestPD.setDefaultConverter();
        addPropertyDescriptor(cornerLabelsWestPD);

        PropertyDescriptor cornerLabelsEastPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_NAME, Boolean.class);
        cornerLabelsEastPD.setDefaultValue(GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_DEFAULT);
        cornerLabelsEastPD.setDisplayName(GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_LABEL);
        cornerLabelsEastPD.setDescription(GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_TOOLTIP);
        cornerLabelsEastPD.setDefaultConverter();
        addPropertyDescriptor(cornerLabelsEastPD);


        // Inner Labels Section

        addSectionBreak(GraticuleLayerType.PROPERTY_INSIDE_LABELS_SECTION_NAME,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_SECTION_LABEL,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_SECTION_TOOLTIP);

        PropertyDescriptor innerLabelsBgTransparencyPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_NAME, Double.class);
        innerLabelsBgTransparencyPD.setDefaultValue(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_DEFAULT);
        innerLabelsBgTransparencyPD.setValueRange(new ValueRange(0, 1));
        innerLabelsBgTransparencyPD.setDisplayName(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_LABEL);
        innerLabelsBgTransparencyPD.setDescription(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_TOOLTIP);
        innerLabelsBgTransparencyPD.setDefaultConverter();
        addPropertyDescriptor(innerLabelsBgTransparencyPD);


        PropertyDescriptor innerLabelsBgColorPD = new PropertyDescriptor(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_NAME, Color.class);
        innerLabelsBgColorPD.setDefaultValue(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_DEFAULT);
        innerLabelsBgColorPD.setDisplayName(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_LABEL);
        innerLabelsBgColorPD.setDescription(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_TOOLTIP);
        innerLabelsBgColorPD.setDefaultConverter();
        addPropertyDescriptor(innerLabelsBgColorPD);


        BindingContext bindingContext = getBindingContext();


        boolean lineEnabled = (Boolean) bindingContext.getPropertySet().getValue(
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME);


        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_NAME, lineEnabled,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, lineEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_NAME, lineEnabled,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, lineEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_NAME, lineEnabled,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, lineEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_NAME, lineEnabled,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, lineEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_NAME, lineEnabled,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME, lineEnabled);


        boolean borderEnabled = (Boolean) bindingContext.getPropertySet().getValue(
                GraticuleLayerType.PROPERTY_BORDER_SHOW_NAME);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_BORDER_COLOR_NAME, borderEnabled,
                GraticuleLayerType.PROPERTY_BORDER_SHOW_NAME, borderEnabled);


        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_BORDER_WIDTH_NAME, borderEnabled,
                GraticuleLayerType.PROPERTY_BORDER_SHOW_NAME, borderEnabled);


        // Set enablement associated with "Labels Inside" checkbox

        boolean textInsideEnabled = (Boolean) bindingContext.getPropertySet().getValue(
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_NAME, textInsideEnabled,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME, textInsideEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_NAME, textInsideEnabled,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME, textInsideEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_NAME, !textInsideEnabled,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME, textInsideEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_NAME, !textInsideEnabled,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME, textInsideEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_NAME, !textInsideEnabled,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME, textInsideEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_NAME, !textInsideEnabled,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME, textInsideEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_NAME, !textInsideEnabled,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME, textInsideEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_NAME, !textInsideEnabled,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME, textInsideEnabled);


        boolean tickMarkEnabled = (Boolean) bindingContext.getPropertySet().getValue(
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_NAME, tickMarkEnabled,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME, tickMarkEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_NAME, tickMarkEnabled,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME, tickMarkEnabled);

        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_NAME, tickMarkEnabled,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME, tickMarkEnabled);


    }


    private void addSectionBreak(String name, String label, String toolTip) {
        PropertyDescriptor descriptor = new PropertyDescriptor(name, Boolean.class);
        descriptor.setDisplayName(label);
        descriptor.setDescription(toolTip);
        addPropertyDescriptor(descriptor);
    }


}
