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

package org.esa.snap.rcp.preferences.general;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.core.datamodel.Mask;
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
 * Panel handling mask layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_LayerMask",
        keywords = "#Options_Keywords_LayerMask",
        keywordsCategory = "Layer",
        id = "LayerMask",
position = 6)
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerMask=New Masks",
        "Options_Keywords_LayerMask=layer, mask"
})
public final class MaskLayerController extends DefaultConfigController {

    /**
     * Preferences key for the mask overlay color
     */
    public static final String PREFERENCE_KEY_MASK_COLOR = "mask.color";
    /**
     * Preferences key for the mask overlay transparency
     */
    public static final String PREFERENCE_KEY_MASK_TRANSPARENCY = "mask.transparency";

    protected PropertySet createPropertySet() {
        return createPropertySet(new MaskBean());
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
        Property maskOverlayColor = context.getPropertySet().getProperty(PREFERENCE_KEY_MASK_COLOR);
        Property maskOverlayTransparency = context.getPropertySet().getProperty(PREFERENCE_KEY_MASK_TRANSPARENCY);

        JComponent[] maskOverlayColorComponents = PreferenceUtils.createColorComponents(maskOverlayColor);
        JComponent[] maskOverlayTransparencyComponents = registry.findPropertyEditor(maskOverlayTransparency.getDescriptor()).createComponents(maskOverlayTransparency.getDescriptor(), context);

        pageUI.add(maskOverlayColorComponents[0]);
        pageUI.add(maskOverlayColorComponents[1]);
        pageUI.add(maskOverlayTransparencyComponents[1]);
        pageUI.add(maskOverlayTransparencyComponents[0]);
        pageUI.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(100), BorderLayout.EAST);
        return parent;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("layer");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class MaskBean {

        @SuppressWarnings("AccessStaticViaInstance")
        @Preference(label = "Default mask overlay colour",
                key = PREFERENCE_KEY_MASK_COLOR)
        Color maskOverlayColor = Mask.ImageType.DEFAULT_COLOR.RED;

        @Preference(label = "Default mask overlay transparency",
                key = PREFERENCE_KEY_MASK_TRANSPARENCY,
                interval = "[0.0,1.0]")
        double maskOverlayTransparency = Mask.ImageType.DEFAULT_TRANSPARENCY;
    }

}
