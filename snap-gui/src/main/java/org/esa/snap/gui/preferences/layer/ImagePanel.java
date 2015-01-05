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
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.gui.preferences.ConfigProperty;
import org.esa.snap.gui.preferences.DefaultConfigController;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.awt.ColorComboBox;
import org.openide.util.HelpCtx;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * The first sub-panel of the layer preferences, handling general properties.
 *
 * @author thomas
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LayerImage=Image Layer",
        "Options_Keywords_LayerImage=layer, image"
})
@OptionsPanelController.SubRegistration(location = "LayerPreferences",
        displayName = "#Options_DisplayName_LayerImage",
        keywords = "#Options_Keywords_LayerImage",
        keywordsCategory = "Layer",
        id = "LayerImage")
public final class ImagePanel extends DefaultConfigController {

    /**
     * Preferences key for the memory capacity of the JAI tile cache in megabytes
     */
    public static final String PROPERTY_KEY_JAI_TILE_CACHE_CAPACITY = "jai.tileCache.memoryCapacity";
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

    protected Object createBean() {
        return new ImageLayerBean();
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);

        tableLayout.setCellColspan(3, 0, 2);
        tableLayout.setCellColspan(6, 0, 2);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property tileCacheCapacity = context.getPropertySet().getProperty(PROPERTY_KEY_JAI_TILE_CACHE_CAPACITY);
        Property backgroundColor = context.getPropertySet().getProperty(PROPERTY_KEY_IMAGE_BG_COLOR);
        Property showImageBorder = context.getPropertySet().getProperty(PROPERTY_KEY_IMAGE_BORDER_SHOWN);
        Property imageBorderSize = context.getPropertySet().getProperty(PROPERTY_KEY_IMAGE_BORDER_SIZE);
        Property imageBorderColor = context.getPropertySet().getProperty(PROPERTY_KEY_IMAGE_BORDER_COLOR);
        Property showPixelBorder = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_BORDER_SHOWN);
        Property pixelBorderSize = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_BORDER_SIZE);
        Property pixelBorderColor = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_BORDER_COLOR);

        JComponent[] tileCacheCapacityComponents = registry.findPropertyEditor(tileCacheCapacity.getDescriptor()).createComponents(tileCacheCapacity.getDescriptor(), context);
        JComponent[] backgroundColorComponents = createColorComponents(backgroundColor);
        JComponent[] showImageBorderComponents = registry.findPropertyEditor(showImageBorder.getDescriptor()).createComponents(showImageBorder.getDescriptor(), context);
        JComponent[] imageBorderSizeComponents = registry.findPropertyEditor(imageBorderSize.getDescriptor()).createComponents(imageBorderSize.getDescriptor(), context);
        JComponent[] imageBorderColorComponents = createColorComponents(imageBorderColor);
        JComponent[] showPixelBorderComponents = registry.findPropertyEditor(showPixelBorder.getDescriptor()).createComponents(showPixelBorder.getDescriptor(), context);
        JComponent[] pixelBorderSizeComponents = registry.findPropertyEditor(pixelBorderSize.getDescriptor()).createComponents(pixelBorderSize.getDescriptor(), context);
        JComponent[] pixelBorderColorComponents = createColorComponents(pixelBorderColor);

        pageUI.add(tileCacheCapacityComponents[1]);
        pageUI.add(tileCacheCapacityComponents[0]);
        pageUI.add(tableLayout.createHorizontalSpacer());
        addNote(pageUI, "Note: If you have enough memory, choose values > 256 MB for better performance.");
        pageUI.add(backgroundColorComponents[0]);
        pageUI.add(backgroundColorComponents[1]);
        pageUI.add(showImageBorderComponents[0]);
        pageUI.add(imageBorderSizeComponents[1]);
        pageUI.add(imageBorderSizeComponents[0]);
        pageUI.add(imageBorderColorComponents[0]);
        pageUI.add(imageBorderColorComponents[1]);
        pageUI.add(showPixelBorderComponents[0]);
        pageUI.add(pixelBorderSizeComponents[1]);
        pageUI.add(pixelBorderSizeComponents[0]);
        pageUI.add(pixelBorderColorComponents[0]);
        pageUI.add(pixelBorderColorComponents[1]);
        pageUI.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(100), BorderLayout.EAST);
        return parent;
    }

    private static JComponent[] createColorComponents(Property colorProperty) {
        JComponent[] components = new JComponent[2];
        components[0] = new JLabel(colorProperty.getDescriptor().getDisplayName() + ":");
        components[1] = createColorCombobox(colorProperty);
        return components;
    }

    private static ColorComboBox createColorCombobox(final Property property) {
        ColorComboBox colorComboBox = new ColorComboBox();
        colorComboBox.setSelectedColor(property.getValue());
        colorComboBox.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                try {
                    property.setValue(colorComboBox.getSelectedColor());
                } catch (ValidationException e1) {
//                  very basic exception handling because exception is not expected to be thrown
                    e1.printStackTrace();
                }
            }
        });
        colorComboBox.setPreferredSize(new Dimension(colorComboBox.getWidth(), 25));
        return colorComboBox;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("layer");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class ImageLayerBean {

        @ConfigProperty(label = "Tile cache capacity (MB)",
                key = PROPERTY_KEY_JAI_TILE_CACHE_CAPACITY)
        int tileCacheCapacity = 512;

        @ConfigProperty(label = "Background colour",
                key = PROPERTY_KEY_IMAGE_BG_COLOR)
        Color backgroundColor = ProductSceneView.DEFAULT_IMAGE_BACKGROUND_COLOR;

        @ConfigProperty(label = "Show image border",
                key = PROPERTY_KEY_IMAGE_BORDER_SHOWN)
        boolean showImageBorder = ImageLayer.DEFAULT_BORDER_SHOWN;

        @ConfigProperty(label = "Image border size",
                key = PROPERTY_KEY_IMAGE_BORDER_SIZE)
        double imageBorderSize = ImageLayer.DEFAULT_BORDER_WIDTH;

        @ConfigProperty(label = "Image border colour",
                key = PROPERTY_KEY_IMAGE_BORDER_COLOR)
        Color imageBorderColor = ImageLayer.DEFAULT_BORDER_COLOR;

        @ConfigProperty(label = "Show pixel borders in magnified views",
                key = PROPERTY_KEY_PIXEL_BORDER_SHOWN)
        boolean showPixelBorder = ImageLayer.DEFAULT_PIXEL_BORDER_SHOWN;

        @ConfigProperty(label = "Pixel border size",
                key = PROPERTY_KEY_PIXEL_BORDER_SIZE)
        double pixelBorderSize = ImageLayer.DEFAULT_PIXEL_BORDER_WIDTH;

        @ConfigProperty(label = "Pixel border colour",
                key = PROPERTY_KEY_PIXEL_BORDER_COLOR)
        Color pixelBorderColor = ImageLayer.DEFAULT_PIXEL_BORDER_COLOR;
    }

}
