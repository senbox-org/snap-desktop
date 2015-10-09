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
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.rcp.preferences.PreferenceUtils;
import org.esa.snap.ui.product.ProductSceneView;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;

/**
 * * Panel handling image layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerImage",
        keywords = "#Options_Keywords_LayerImage",
        keywordsCategory = "Layer",
        id = "LayerImage")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerImage=Image Layer",
        "Options_Keywords_LayerImage=layer, image"
})
public final class ImageLayerController extends DefaultConfigController {


    /**
     * Preferences key for the background color
     */
    public static final String PROPERTY_KEY_IMAGE_BG_COLOR = "image.background.color";
    /**
     * Preferences key for showing image border
     */
    public static final String PROPERTY_KEY_IMAGE_BORDER_SHOWN = "image.border.shown";
    /**
     * Preferences key for image border size
     */
    public static final String PROPERTY_KEY_IMAGE_BORDER_SIZE = "image.border.size";
    /**
     * Preferences key for image border color
     */
    public static final String PROPERTY_KEY_IMAGE_BORDER_COLOR = "image.border.color";
    /**
     * Preferences key for showing pixel image border
     */
    public static final String PROPERTY_KEY_PIXEL_BORDER_SHOWN = "pixel.border.shown";
    /**
     * Preferences key for pixel border size
     */
    public static final String PROPERTY_KEY_PIXEL_BORDER_SIZE = "pixel.border.size";
    /**
     * Preferences key for pixel border color
     */
    public static final String PROPERTY_KEY_PIXEL_BORDER_COLOR = "pixel.border.color";

    private JComponent[] imageBorderColorComponents;
    private JComponent[] pixelBorderColorComponents;

    protected PropertySet createPropertySet() {
        return createPropertySet(new ImageLayerBean());
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);

        tableLayout.setCellColspan(1, 0, 2);
        tableLayout.setCellColspan(4, 0, 2);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property backgroundColor = context.getPropertySet().getProperty(PROPERTY_KEY_IMAGE_BG_COLOR);
        Property showImageBorder = context.getPropertySet().getProperty(PROPERTY_KEY_IMAGE_BORDER_SHOWN);
        Property imageBorderSize = context.getPropertySet().getProperty(PROPERTY_KEY_IMAGE_BORDER_SIZE);
        Property imageBorderColor = context.getPropertySet().getProperty(PROPERTY_KEY_IMAGE_BORDER_COLOR);
        Property showPixelBorder = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_BORDER_SHOWN);
        Property pixelBorderSize = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_BORDER_SIZE);
        Property pixelBorderColor = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_BORDER_COLOR);

        JComponent[] backgroundColorComponents = PreferenceUtils.createColorComponents(backgroundColor);
        JComponent[] showImageBorderComponents = registry.findPropertyEditor(showImageBorder.getDescriptor()).createComponents(showImageBorder.getDescriptor(), context);
        JComponent[] imageBorderSizeComponents = registry.findPropertyEditor(imageBorderSize.getDescriptor()).createComponents(imageBorderSize.getDescriptor(), context);
        imageBorderColorComponents = PreferenceUtils.createColorComponents(imageBorderColor);
        JComponent[] showPixelBorderComponents = registry.findPropertyEditor(showPixelBorder.getDescriptor()).createComponents(showPixelBorder.getDescriptor(), context);
        JComponent[] pixelBorderSizeComponents = registry.findPropertyEditor(pixelBorderSize.getDescriptor()).createComponents(pixelBorderSize.getDescriptor(), context);
        pixelBorderColorComponents = PreferenceUtils.createColorComponents(pixelBorderColor);

        // row 0
        pageUI.add(backgroundColorComponents[0]);
        pageUI.add(backgroundColorComponents[1]);

        // row 1
        pageUI.add(showImageBorderComponents[0]);

        // row 2
        pageUI.add(imageBorderSizeComponents[1]);
        pageUI.add(imageBorderSizeComponents[0]);

        // row 3
        pageUI.add(imageBorderColorComponents[0]);
        pageUI.add(imageBorderColorComponents[1]);

        // row 4
        pageUI.add(showPixelBorderComponents[0]);

        // row 5
        pageUI.add(pixelBorderSizeComponents[1]);
        pageUI.add(pixelBorderSizeComponents[0]);

        // row 6
        pageUI.add(pixelBorderColorComponents[0]);
        pageUI.add(pixelBorderColorComponents[1]);

        // row 7+
        pageUI.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(100), BorderLayout.EAST);
        return parent;
    }

    @Override
    protected void configure(BindingContext context) {
        Enablement enablementImageBorderSize = context.bindEnabledState(PROPERTY_KEY_IMAGE_BORDER_SIZE, false, PROPERTY_KEY_IMAGE_BORDER_SHOWN, false);
        context.getPropertySet().getProperty(PROPERTY_KEY_IMAGE_BORDER_SHOWN).addPropertyChangeListener(evt -> {
            enablementImageBorderSize.apply();
            for (JComponent imageBorderColorComponent : imageBorderColorComponents) {
                imageBorderColorComponent.setEnabled(((Boolean) evt.getNewValue()));
            }
        });

        Enablement enablementPixelBorderSize = context.bindEnabledState(PROPERTY_KEY_PIXEL_BORDER_SIZE, false, PROPERTY_KEY_PIXEL_BORDER_SHOWN, false);
        context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_BORDER_SHOWN).addPropertyChangeListener(evt -> {
            enablementPixelBorderSize.apply();
            for (JComponent pixelBorderColorComponent : pixelBorderColorComponents) {
                pixelBorderColorComponent.setEnabled(((Boolean) evt.getNewValue()));
            }
        });

        for (JComponent imageBorderColorComponent : imageBorderColorComponents) {
            imageBorderColorComponent.setEnabled(ImageLayer.DEFAULT_BORDER_SHOWN);
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("layer");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class ImageLayerBean {

        @Preference(label = "Background colour",
                key = PROPERTY_KEY_IMAGE_BG_COLOR)
        Color backgroundColor = ProductSceneView.DEFAULT_IMAGE_BACKGROUND_COLOR;

        @Preference(label = "Show image border",
                key = PROPERTY_KEY_IMAGE_BORDER_SHOWN)
        boolean showImageBorder = ImageLayer.DEFAULT_BORDER_SHOWN;

        @Preference(label = "Image border size",
                key = PROPERTY_KEY_IMAGE_BORDER_SIZE)
        double imageBorderSize = ImageLayer.DEFAULT_BORDER_WIDTH;

        @Preference(label = "Image border colour",
                key = PROPERTY_KEY_IMAGE_BORDER_COLOR)
        Color imageBorderColor = ImageLayer.DEFAULT_BORDER_COLOR;

        @Preference(label = "Show pixel borders in magnified views",
                key = PROPERTY_KEY_PIXEL_BORDER_SHOWN)
        boolean showPixelBorder = ImageLayer.DEFAULT_PIXEL_BORDER_SHOWN;

        @Preference(label = "Pixel border size",
                key = PROPERTY_KEY_PIXEL_BORDER_SIZE)
        double pixelBorderSize = ImageLayer.DEFAULT_PIXEL_BORDER_WIDTH;

        @Preference(label = "Pixel border colour",
                key = PROPERTY_KEY_PIXEL_BORDER_COLOR)
        Color pixelBorderColor = ImageLayer.DEFAULT_PIXEL_BORDER_COLOR;
    }

}
