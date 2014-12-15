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

package org.esa.snap.gui.preferences.layer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.gui.preferences.ConfigProperty;
import org.esa.snap.gui.preferences.DefaultConfigController;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import static com.bc.ceres.swing.TableLayout.*;

/**
 * The first sub-panel of the layer preferences, handling general properties.
 *
 * @author thomas
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerGeneral=General",
        "Options_Keywords_LayerGeneral=layer, general"
})
@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerGeneral",
        keywords = "#Options_Keywords_LayerGeneral",
        keywordsCategory = "Layer",
        id = "LayerGeneral")
public final class GeneralPanel extends DefaultConfigController {

    protected Object createBean() {
        return new GeneralLayerBean();
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
        Property antiAliasing = context.getPropertySet().getProperty(ProductSceneView.PROPERTY_KEY_GRAPHICS_ANTIALIASING);
        Property showNavigationControl = context.getPropertySet().getProperty(ProductSceneView.PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN);
        Property showScrollBars = context.getPropertySet().getProperty(ProductSceneView.PROPERTY_KEY_IMAGE_SCROLL_BARS_SHOWN);

        JComponent[] antiAliasingComponents = registry.findPropertyEditor(antiAliasing.getDescriptor()).createComponents(antiAliasing.getDescriptor(), context);
        JComponent[] showNavigationControlComponents = registry.findPropertyEditor(showNavigationControl.getDescriptor()).createComponents(showNavigationControl.getDescriptor(), context);
        JComponent[] showScrollBarsComponents = registry.findPropertyEditor(showScrollBars.getDescriptor()).createComponents(showScrollBars.getDescriptor(), context);

        tableLayout.setRowPadding(1, new Insets(10, 80, 10, 4));
        pageUI.add(antiAliasingComponents[0]);
        addNote(pageUI);
        pageUI.add(showNavigationControlComponents[0]);
        pageUI.add(showScrollBarsComponents[0]);
        pageUI.add(tableLayout.createVerticalSpacer());

        return pageUI;
    }

    private static void addNote(JPanel pageUI) {
        JLabel note = new JLabel("Note: For best performance turn anti-aliasing off.");
        if (note.getFont() != null) {
            note.setFont(note.getFont().deriveFont(Font.ITALIC));
        }
        note.setForeground(new Color(0, 0, 92));
        note.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));// indentation
        pageUI.add(note);
    }


    @SuppressWarnings("UnusedDeclaration")
    static class GeneralLayerBean {

        @ConfigProperty(label = "Use anti-aliasing for rendering text and vector graphics",
                key = ProductSceneView.PROPERTY_KEY_GRAPHICS_ANTIALIASING)
        boolean antiAliasing = true;

        @ConfigProperty(label = "Show a navigation control widget in image views",
                key = ProductSceneView.PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN)
        boolean showNavigationControl = true;

        @ConfigProperty(label = "Show a navigation control widget in image views",
                key = ProductSceneView.PROPERTY_KEY_IMAGE_SCROLL_BARS_SHOWN)
        boolean showScrollBars = false;
    }

}
