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

package org.esa.snap.rcp.preferences.layer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.core.layer.ColorBarLayerType;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.rcp.preferences.PreferenceUtils;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;

import static org.esa.snap.rcp.preferences.PreferenceUtils.*;

/**
 * * Panel handling graticule layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerColorBar",
        keywords = "#Options_Keywords_LayerColorBar",
        keywordsCategory = "Layer",
        id = "LayerColorBar")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerColorBar=ColorBar Layer",
        "Options_Keywords_LayerColorBar=layer, colorbar"
})
public final class ColorBarLayerController extends DefaultConfigController {

    private JComponent[] textFgColorComponents;
    private JComponent[] textBgColorComponents;

    protected PropertySet createPropertySet() {
        return createPropertySet(new ColorBarBean());
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setCellColspan(0, 0, 2);
        tableLayout.setCellColspan(1, 0, 2);
        tableLayout.setCellColspan(8, 0, 2);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property computeLatLonSteps = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_RES_AUTO);
        Property avgGridSize = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_RES_PIXELS);
        Property latStep = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_RES_LAT);
        Property lonStep = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_RES_LON);
        Property lineColor = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_LINE_COLOR);
        Property lineWidth = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_LINE_WIDTH);
        Property lineTransparency = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_LINE_TRANSPARENCY);
        Property showTextLabels = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_TEXT_ENABLED);
        Property textFgColor = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_TEXT_FG_COLOR);
        Property textBgColor = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_TEXT_BG_COLOR);
        Property textBgTransparency = context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_TEXT_BG_TRANSPARENCY);

        JComponent[] computeLatLonStepsComponents = registry.findPropertyEditor(computeLatLonSteps.getDescriptor()).createComponents(computeLatLonSteps.getDescriptor(), context);
        JComponent[] avgGridSizeComponents = registry.findPropertyEditor(avgGridSize.getDescriptor()).createComponents(avgGridSize.getDescriptor(), context);
        JComponent[] latStepComponents = registry.findPropertyEditor(latStep.getDescriptor()).createComponents(latStep.getDescriptor(), context);
        JComponent[] lonStepComponents = registry.findPropertyEditor(lonStep.getDescriptor()).createComponents(lonStep.getDescriptor(), context);
        JComponent[] lineColorComponents = PreferenceUtils.createColorComponents(lineColor);
        JComponent[] lineWidthComponents = registry.findPropertyEditor(lineWidth.getDescriptor()).createComponents(lineWidth.getDescriptor(), context);
        JComponent[] lineTransparencyComponents = registry.findPropertyEditor(lineTransparency.getDescriptor()).createComponents(lineTransparency.getDescriptor(), context);
        JComponent[] showTextLabelsComponents = registry.findPropertyEditor(showTextLabels.getDescriptor()).createComponents(showTextLabels.getDescriptor(), context);
        textFgColorComponents = PreferenceUtils.createColorComponents(textFgColor);
        textBgColorComponents = PreferenceUtils.createColorComponents(textBgColor);
        JComponent[] textBgTransparencyComponents = registry.findPropertyEditor(textBgTransparency.getDescriptor()).createComponents(textBgTransparency.getDescriptor(), context);

        pageUI.add(computeLatLonStepsComponents[0]);
        addNote(pageUI, "<html>Note: Deselect this option only very carefully. The latitude and longitude<br>" +
                "steps you enter will be used for low and high resolution products.</html>");
        pageUI.add(avgGridSizeComponents[1]);
        pageUI.add(avgGridSizeComponents[0]);
        pageUI.add(latStepComponents[1]);
        pageUI.add(latStepComponents[0]);
        pageUI.add(lonStepComponents[1]);
        pageUI.add(lonStepComponents[0]);
        pageUI.add(lineColorComponents[0]);
        pageUI.add(lineColorComponents[1]);
        pageUI.add(lineWidthComponents[1]);
        pageUI.add(lineWidthComponents[0]);
        pageUI.add(lineTransparencyComponents[1]);
        pageUI.add(lineTransparencyComponents[0]);
        pageUI.add(showTextLabelsComponents[0]);
        pageUI.add(textFgColorComponents[0]);
        pageUI.add(textFgColorComponents[1]);
        pageUI.add(textBgColorComponents[0]);
        pageUI.add(textBgColorComponents[1]);
        pageUI.add(textBgTransparencyComponents[1]);
        pageUI.add(textBgTransparencyComponents[0]);

        pageUI.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(100), BorderLayout.EAST);
        return parent;
    }

    @Override
    protected void configure(BindingContext context) {
        Enablement enablementAvgGridSize = context.bindEnabledState(ColorBarLayerType.PROPERTY_NAME_RES_PIXELS, true,
                ColorBarLayerType.PROPERTY_NAME_RES_AUTO, true);
        Enablement enablementLatStep = context.bindEnabledState(ColorBarLayerType.PROPERTY_NAME_RES_LAT, true,
                ColorBarLayerType.PROPERTY_NAME_RES_AUTO, false);
        Enablement enablementLonStep = context.bindEnabledState(ColorBarLayerType.PROPERTY_NAME_RES_LON, true,
                ColorBarLayerType.PROPERTY_NAME_RES_AUTO, false);

        context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_RES_AUTO).addPropertyChangeListener(evt -> {
            enablementAvgGridSize.apply();
            enablementLatStep.apply();
            enablementLonStep.apply();
        });

        Enablement enablementTextBgTransparency = context.bindEnabledState(ColorBarLayerType.PROPERTY_NAME_TEXT_BG_TRANSPARENCY, true,
                ColorBarLayerType.PROPERTY_NAME_TEXT_ENABLED, true);

        context.getPropertySet().getProperty(ColorBarLayerType.PROPERTY_NAME_TEXT_ENABLED).addPropertyChangeListener(evt -> {
            enablementTextBgTransparency.apply();
            for (JComponent component : textFgColorComponents) {
                component.setEnabled(((Boolean) evt.getNewValue()));
            }
            for (JComponent component : textBgColorComponents) {
                component.setEnabled(((Boolean) evt.getNewValue()));
            }
        });

        for (JComponent component : textFgColorComponents) {
            component.setEnabled(true);
        }
        for (JComponent component : textBgColorComponents) {
            component.setEnabled(true);
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("layer");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class ColorBarBean {

        @Preference(label = "Compute latitude and longitude steps",
                key = ColorBarLayerType.PROPERTY_NAME_RES_AUTO)
        boolean computeLatLonSteps = true;

        @Preference(label = "Average grid size in pixels",
                key = ColorBarLayerType.PROPERTY_NAME_RES_PIXELS,
                interval = "[16,512]")
        int averageGridSize = 128;

        @Preference(label = "Latitude step (dec. degree)",
                key = ColorBarLayerType.PROPERTY_NAME_RES_LAT,
                interval = "[0.01,90.0]")
        double latStep = 1.0;

        @Preference(label = "Longitude step (dec. degree)",
                key = ColorBarLayerType.PROPERTY_NAME_RES_LON,
                interval = "[0.01,180.0]")
        double lonStep = 1.0;

        @Preference(label = "Line colour",
                key = ColorBarLayerType.PROPERTY_NAME_LINE_COLOR)
        Color lineColor = new Color(204, 204, 255);

        @Preference(label = "Line width",
                key = ColorBarLayerType.PROPERTY_NAME_LINE_WIDTH)
        double lineWidth = 0.5;

        @Preference(label = "Line transparency",
                key = ColorBarLayerType.PROPERTY_NAME_LINE_TRANSPARENCY,
                interval = "[0.0,1.0]")
        double lineTransparency = 0.0;

        @Preference(label = "Show text labels",
                key = ColorBarLayerType.PROPERTY_NAME_TEXT_ENABLED)
        boolean showTextLabels = true;

        @Preference(label = "Text foreground colour",
                key = ColorBarLayerType.PROPERTY_NAME_TEXT_FG_COLOR)
        Color fgColor = Color.white;

        @Preference(label = "Text background colour",
                key = ColorBarLayerType.PROPERTY_NAME_TEXT_BG_COLOR)
        Color bgColor = Color.black;

        @Preference(label = "Text background transparency",
                key = ColorBarLayerType.PROPERTY_NAME_TEXT_BG_TRANSPARENCY,
                interval = "[0.0,1.0]")
        double textBgTransparency = 0.7;
    }

}
