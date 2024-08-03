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
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.core.layer.NoDataLayerType;
import org.esa.snap.core.util.NamingConvention;
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

/**
 * Panel handling no-data layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerNoData",
        keywords = "#Options_Keywords_LayerNoData",
        keywordsCategory = "Layer",
        id = "LayerNoData")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerNoData=No-Data Layer",
        "Options_Keywords_LayerNoData=layer, no-data"
})
public final class NoDataLayerController extends DefaultConfigController {

    /**
     * Preferences key for the no-data overlay color
     */
    public static final String PROPERTY_KEY_NO_DATA_OVERLAY_COLOR = "noDataOverlay.color";
    /**
     * Preferences key for the no-data overlay transparency
     */
    public static final String PROPERTY_KEY_NO_DATA_OVERLAY_TRANSPARENCY = "noDataOverlay.transparency";

    protected PropertySet createPropertySet() {
        return createPropertySet(new NoDataBean());
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property noDataOverlayColor = context.getPropertySet().getProperty(PROPERTY_KEY_NO_DATA_OVERLAY_COLOR);
        Property noDataOverlayTransparency = context.getPropertySet().getProperty(PROPERTY_KEY_NO_DATA_OVERLAY_TRANSPARENCY);

        JComponent[] noDataOverlayColorComponents = PreferenceUtils.createColorComponents(noDataOverlayColor);
        JComponent[] noDataOverlayTransparencyComponents = registry.findPropertyEditor(noDataOverlayTransparency.getDescriptor()).createComponents(noDataOverlayTransparency.getDescriptor(), context);

        pageUI.add(noDataOverlayColorComponents[0]);
        pageUI.add(noDataOverlayColorComponents[1]);
        pageUI.add(noDataOverlayTransparencyComponents[1]);
        pageUI.add(noDataOverlayTransparencyComponents[0]);
        pageUI.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(100), BorderLayout.EAST);
        return parent;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("options-nodatalayer");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class NoDataBean {

        @Preference(label = "No-Data Overlay " + NamingConvention.COLOR_MIXED_CASE,
                key = PROPERTY_KEY_NO_DATA_OVERLAY_COLOR)
        Color noDataOverlayColor = NoDataLayerType.DEFAULT_COLOR;

        @Preference(label = "No-Data Overlay Transparency",
                key = PROPERTY_KEY_NO_DATA_OVERLAY_TRANSPARENCY,
                interval = "[0.0,1.0]")
        double noDataOverlayTransparency = 0.3;
    }

}
