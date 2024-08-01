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
        return new HelpCtx("options-imageview");
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(Anchor.NORTHWEST);
        tableLayout.setTablePadding(4, 10);
        tableLayout.setTableFill(Fill.BOTH);
        tableLayout.setTableWeightX(1.0);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property showNavigationControl = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_IMAGE_NAV_CONTROL_SHOWN);
        Property showScrollBars = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_IMAGE_SCROLL_BARS_SHOWN);
        Property reverseZoom = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_INVERT_ZOOMING);
        Property zoomInitial = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_KEY);
//        Property zoomInitialWide = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_WIDE_KEY);
//        Property zoomInitialTall = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_TALL_KEY);
//        Property zoomInitialAspectWide = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_ASPECT_WIDE_KEY);
//        Property zoomInitialAspectTall = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_ASPECT_TALL_KEY);
        Property shiftXInitial = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_SHIFT_X_INITIAL_KEY);
        Property shiftYInitial = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_KEY_SHIFT_Y_INITIAL_KEY);
        Property positionCenterX = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_POSITION_CENTER_X_KEY);
        Property positionCenterY = context.getPropertySet().getProperty(ProductSceneView.PREFERENCE_POSITION_CENTER_Y_KEY);
        Property initialShowAnnotationLayer = context.getPropertySet().getProperty(ProductSceneView.SHOW_ANNOTATION_OVERLAY_STATE_KEY);
        Property initialShowGridlinesLayer = context.getPropertySet().getProperty(ProductSceneView.SHOW_GRIDLINES_OVERLAY_STATE_KEY);
        Property initialShowColorBarLegendLayer = context.getPropertySet().getProperty(ProductSceneView.SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY);
        Property initialShowNoDataLayer = context.getPropertySet().getProperty(ProductSceneView.SHOW_NO_DATA_OVERLAY_KEY);

        JComponent[] showNavigationControlComponents = registry.findPropertyEditor(showNavigationControl.getDescriptor()).createComponents(showNavigationControl.getDescriptor(), context);
        JComponent[] showScrollBarsComponents = registry.findPropertyEditor(showScrollBars.getDescriptor()).createComponents(showScrollBars.getDescriptor(), context);
        JComponent[] reverseZoomComponents = registry.findPropertyEditor(reverseZoom.getDescriptor()).createComponents(reverseZoom.getDescriptor(), context);
        JComponent[] zoomInitialComponents = registry.findPropertyEditor(zoomInitial.getDescriptor()).createComponents(zoomInitial.getDescriptor(), context);
//        JComponent[] zoomInitialWideComponents = registry.findPropertyEditor(zoomInitialWide.getDescriptor()).createComponents(zoomInitialWide.getDescriptor(), context);
//        JComponent[] zoomInitialTallComponents = registry.findPropertyEditor(zoomInitialTall.getDescriptor()).createComponents(zoomInitialTall.getDescriptor(), context);
//        JComponent[] zoomInitialAspectWideComponents = registry.findPropertyEditor(zoomInitialAspectWide.getDescriptor()).createComponents(zoomInitialAspectWide.getDescriptor(), context);
//        JComponent[] zoomInitialAspectTallComponents = registry.findPropertyEditor(zoomInitialAspectTall.getDescriptor()).createComponents(zoomInitialAspectTall.getDescriptor(), context);
        JComponent[] shiftXInitialComponents = registry.findPropertyEditor(shiftXInitial.getDescriptor()).createComponents(shiftXInitial.getDescriptor(), context);
        JComponent[] shiftYInitialComponents = registry.findPropertyEditor(shiftYInitial.getDescriptor()).createComponents(shiftYInitial.getDescriptor(), context);
        JComponent[] positionCenterXComponents = registry.findPropertyEditor(positionCenterX.getDescriptor()).createComponents(positionCenterX.getDescriptor(), context);
        JComponent[] positionCenterYComponents = registry.findPropertyEditor(positionCenterY.getDescriptor()).createComponents(positionCenterY.getDescriptor(), context);

        JComponent[] initialShowAnnotationLayerComponents = registry.findPropertyEditor(initialShowAnnotationLayer.getDescriptor()).createComponents(initialShowAnnotationLayer.getDescriptor(), context);
        JComponent[] initialShowGridlinesLayerComponents = registry.findPropertyEditor(initialShowGridlinesLayer.getDescriptor()).createComponents(initialShowGridlinesLayer.getDescriptor(), context);
        JComponent[] initialShowColorBarLegendLayerComponents = registry.findPropertyEditor(initialShowColorBarLegendLayer.getDescriptor()).createComponents(initialShowColorBarLegendLayer.getDescriptor(), context);
        JComponent[] initialShowNoDataLayerComponents = registry.findPropertyEditor(initialShowNoDataLayer.getDescriptor()).createComponents(initialShowNoDataLayer.getDescriptor(), context);

        tableLayout.setRowPadding(0, new Insets(10, 80, 10, 4));

        int row = 0;

        tableLayout.setCellColspan(row, 0, 2);
        tableLayout.setCellWeightX(row, 0, 1.0);
        pageUI.add(showNavigationControlComponents[0]);

        row++;
        tableLayout.setCellColspan(row, 0, 2);
        tableLayout.setCellWeightX(row, 0, 1.0);
        pageUI.add(showScrollBarsComponents[0]);

        row++;
        tableLayout.setCellColspan(row, 0, 2);
        tableLayout.setCellWeightX(row, 0, 1.0);
        pageUI.add(reverseZoomComponents[0]);

        row++;
        tableLayout.setCellColspan(row, 0, 2);
        tableLayout.setCellWeightX(row, 0, 1.0);
        pageUI.add(initialShowAnnotationLayerComponents[0]);

        row++;
        tableLayout.setCellColspan(row, 0, 2);
        tableLayout.setCellWeightX(row, 0, 1.0);
        pageUI.add(initialShowGridlinesLayerComponents[0]);

        row++;
        tableLayout.setCellColspan(row, 0, 2);
        tableLayout.setCellWeightX(row, 0, 1.0);
        pageUI.add(initialShowColorBarLegendLayerComponents[0]);

        row++;
        tableLayout.setCellColspan(row, 0, 2);
        tableLayout.setCellWeightX(row, 0, 1.0);
        pageUI.add(initialShowNoDataLayerComponents[0]);


        row++;
        tableLayout.setCellColspan(row, 0, 2);
        tableLayout.setCellWeightX(row, 0, 1.0);
        pageUI.add(positionCenterXComponents[0]);



        row++;
        tableLayout.setCellColspan(row, 0, 2);
        tableLayout.setCellWeightX(row, 0, 1.0);
        pageUI.add(positionCenterYComponents[0]);




        row++;
        tableLayout.setCellColspan(row, 0, 1);
        tableLayout.setCellColspan(row, 1, 1);
        tableLayout.setCellWeightX(row, 0, 0.0);
        pageUI.add(zoomInitialComponents[1]);
        tableLayout.setCellWeightX(row, 1, 1.0);
        pageUI.add(zoomInitialComponents[0]);

        row++;
        tableLayout.setCellColspan(row, 0, 1);
        tableLayout.setCellColspan(row, 1, 1);
        tableLayout.setCellWeightX(row, 0, 0.0);
        pageUI.add(shiftXInitialComponents[1]);
        tableLayout.setCellWeightX(row, 1, 1.0);
        pageUI.add(shiftXInitialComponents[0]);


        row++;
        tableLayout.setCellColspan(row, 0, 1);
        tableLayout.setCellColspan(row, 1, 1);
        tableLayout.setCellWeightX(row, 0, 0.0);
        pageUI.add(shiftYInitialComponents[1]);
        tableLayout.setCellWeightX(row, 1, 1.0);
        pageUI.add(shiftYInitialComponents[0]);


//
//        row++;
//        tableLayout.setCellColspan(row, 0, 1);
//        tableLayout.setCellColspan(row, 1, 1);
//        tableLayout.setCellWeightX(row, 0, 0.0);
//        pageUI.add(zoomInitialWideComponents[1]);
//        tableLayout.setCellWeightX(row, 1, 1.0);
//        pageUI.add(zoomInitialWideComponents[0]);
//
//        row++;
//        tableLayout.setCellColspan(row, 0, 1);
//        tableLayout.setCellColspan(row, 1, 1);
//        tableLayout.setCellWeightX(row, 0, 0.0);
//        pageUI.add(zoomInitialTallComponents[1]);
//        tableLayout.setCellWeightX(row, 1, 1.0);
//        pageUI.add(zoomInitialTallComponents[0]);
//
//        row++;
//        tableLayout.setCellColspan(row, 0, 1);
//        tableLayout.setCellColspan(row, 1, 1);
//        tableLayout.setCellWeightX(row, 0, 0.0);
//        pageUI.add(zoomInitialAspectWideComponents[1]);
//        tableLayout.setCellWeightX(row, 1, 1.0);
//        pageUI.add(zoomInitialAspectWideComponents[0]);
//
//        row++;
//        tableLayout.setCellColspan(row, 0, 1);
//        tableLayout.setCellColspan(row, 1, 1);
//        tableLayout.setCellWeightX(row, 0, 0.0);
//        pageUI.add(zoomInitialAspectTallComponents[1]);
//        tableLayout.setCellWeightX(row, 1, 1.0);
//        pageUI.add(zoomInitialAspectTallComponents[0]);
//



        row++;
        tableLayout.setRowWeightY(row, 1.0);

        pageUI.add(tableLayout.createVerticalSpacer());

        return pageUI;
    }

    @SuppressWarnings("UnusedDeclaration")
    static class GeneralLayerBean {

        @Preference(label = "Show a navigation control widget in image views",
                key = ProductSceneView.PREFERENCE_KEY_IMAGE_NAV_CONTROL_SHOWN)
        boolean showNavigationControl = ProductSceneView.PREFERENCE_KEY_IMAGE_NAV_CONTROL_SHOWN_DEFAULT;

        @Preference(label = "Show scroll bars in image views",
                key = ProductSceneView.PREFERENCE_KEY_IMAGE_SCROLL_BARS_SHOWN)
        boolean showScrollBars = false;

        @Preference(label = "Invert mouse wheel scrolling (zoom-in/out)",
                key = ProductSceneView.PREFERENCE_KEY_INVERT_ZOOMING)
        boolean reverseZom = true;

        @Preference(label = "Image Zoom",
                key = ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_KEY,
                description = "Zoom percent for opening scene image",
                interval = "[10,1000]")
        double zoomInitial = ProductSceneView.PREFERENCE_ZOOM_INITIAL_DEFAULT;

//        @Preference(label = "Default Image Zoom (Wide Scene)",
//                key = ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_WIDE_KEY,
//                description = "Zoom factor for wide scene (see Wide Scene Aspect)",
//                interval = "[10,1000]")
//        double zoomInitialWide = ProductSceneView.PREFERENCE_ZOOM_INITIAL_WIDE_DEFAULT;
//
//        @Preference(label = "Default Image Zoom (Tall Scene)",
//                key = ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_TALL_KEY,
//                description = "Zoom factor for tall scene (see Tall Scene Aspect)",
//                interval = "[10,1000]")
//        double zoomInitialTall = ProductSceneView.PREFERENCE_ZOOM_INITIAL_TALL_DEFAULT;
//
//        @Preference(label = "Wide Scene Aspect",
//                key = ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_ASPECT_WIDE_KEY,
//                description = "Scene aspect ratio which indicate scene is wide scene for initial zoom",
//                interval = "[10,1000]")
//        double zoomInitialAspectWide = ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_ASPECT_WIDE_DEFAULT;
//
//        @Preference(label = "Tall Scene Aspect",
//                key = ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_ASPECT_TALL_KEY,
//                description = "Scene aspect ratio which indicate scene is tall scene for initial zoom",
//                interval = "[10,1000]")
//        double zoomInitialAspectTall = ProductSceneView.PREFERENCE_KEY_ZOOM_INITIAL_ASPECT_TALL_DEFAULT;

        @Preference(label = "Image Shift (Horizontal)",
                key = ProductSceneView.PREFERENCE_KEY_SHIFT_X_INITIAL_KEY,
                description = "Shift image horizontally in image display (in percent image width)",
                interval = "[-500,500]")
        double shiftXInitialKey = ProductSceneView.PREFERENCE_KEY_SHIFT_X_INITIAL_DEFAULT;

        @Preference(label = "Image Shift (Vertical)",
                key = ProductSceneView.PREFERENCE_KEY_SHIFT_Y_INITIAL_KEY,
                description = "Shift image vertically downwards (in percent of image height)",
                interval = "[-500,500]")
        double shiftYInitialDefault = ProductSceneView.PREFERENCE_KEY_SHIFT_Y_INITIAL_DEFAULT;

        @Preference(label = "Center Image (Horizontal)",
                key = ProductSceneView.PREFERENCE_POSITION_CENTER_X_KEY,
                description = "Set position to center (horizontal) for initial image display")
        boolean positionCenterXDefault = ProductSceneView.PREFERENCE_POSITION_CENTER_X_DEFAULT;

        @Preference(label = "Center Image (Vertical)",
                key = ProductSceneView.PREFERENCE_POSITION_CENTER_Y_KEY,
                description = "Set position to center (vertical) for initial image display")
        boolean positionCenterYDefault = ProductSceneView.PREFERENCE_POSITION_CENTER_Y_DEFAULT;


        @Preference(label = "Show Annotation Metadata Layer",
                key = ProductSceneView.SHOW_ANNOTATION_OVERLAY_STATE_KEY,
                description = "Show Annotation Metadata layer when new scene view window is created")
        boolean showAnnotationOverlayStateDefault = ProductSceneView.SHOW_ANNOTATION_OVERLAY_STATE_DEFAULT;

        @Preference(label = "Show Map Gridlines Layer",
                key = ProductSceneView.SHOW_GRIDLINES_OVERLAY_STATE_KEY,
                description = "Show Map Gridlines layer when new scene view window is created")
        boolean showGridlinesOverlayStateDefault = ProductSceneView.SHOW_GRIDLINES_OVERLAY_STATE_DEFAULT;

        @Preference(label = "Show Color Bar Legend Layer",
                key = ProductSceneView.SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY,
                description = "Show Color Bar Legend layer when new scene view window is created")
        boolean showColorBarLegendOverlayDefault = ProductSceneView.SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT;

        @Preference(label = "Show No-Data Layer",
                key = ProductSceneView.SHOW_NO_DATA_OVERLAY_KEY,
                description = "Show No-Data layer when new scene view window is created")
        boolean showNoDataOverlayDefault = ProductSceneView.SHOW_NO_DATA_OVERLAY_DEFAULT;

    }

}
