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
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.ui.product.ProductSceneView;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Insets;

import static com.bc.ceres.swing.TableLayout.*;

/**
 * Panel handling general layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author thomas
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerGeneral=Image View",
        "Options_Keywords_LayerGeneral=layer, general"
})
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_LayerGeneral",
        keywords = "#Options_Keywords_LayerGeneral",
        keywordsCategory = "Image,Layer",
        id = "LayerGeneral",
        position = 3)
public final class ImageViewController extends DefaultConfigController {

    protected PropertySet createPropertySet() {
        return createPropertySet(new GeneralLayerBean());
    }


    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("layer");
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(Anchor.NORTHWEST);
        tableLayout.setTablePadding(4, 10);
        tableLayout.setTableFill(Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowWeightY(4, 1.0);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property showNavigationControl = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_IMAGE_NAV_CONTROL_SHOWN);
        Property showScrollBars = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_IMAGE_SCROLL_BARS_SHOWN);

        JComponent[] showNavigationControlComponents = registry.findPropertyEditor(showNavigationControl.getDescriptor()).createComponents(showNavigationControl.getDescriptor(), context);
        JComponent[] showScrollBarsComponents = registry.findPropertyEditor(showScrollBars.getDescriptor()).createComponents(showScrollBars.getDescriptor(), context);

        tableLayout.setRowPadding(0, new Insets(10, 80, 10, 4));
        pageUI.add(showNavigationControlComponents[0]);
        pageUI.add(showScrollBarsComponents[0]);
        pageUI.add(tableLayout.createVerticalSpacer());

        return pageUI;
    }

    @SuppressWarnings("UnusedDeclaration")
    static class GeneralLayerBean {

        @Preference(label = "Show a navigation control widget in image views",
                key = ProductSceneView.PREFERENCE_KEY_IMAGE_NAV_CONTROL_SHOWN)
        boolean showNavigationControl = true;

        @Preference(label = "Show scroll bars in image views",
                key = ProductSceneView.PREFERENCE_KEY_IMAGE_SCROLL_BARS_SHOWN)
        boolean showScrollBars = false;
    }

}
