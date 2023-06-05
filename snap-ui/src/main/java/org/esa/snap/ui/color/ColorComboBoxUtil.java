package org.esa.snap.ui.color;
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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.util.SystemUtils;
import org.openide.awt.ColorComboBox;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;


/**
 * Contains some static helper functions.
 *
 * @author thomas
 * @author Daniel Knowles
 */
// SEP2018 - Daniel Knowles - Fixes bug where colorComboBox was not listening to properties change event when
// DefaultConfigController was loading the user saved preferences

// JUN2023 - Daniel Knowles - moved this file from org.esa.snap.rcp.preferences.PreferenceUtils

public class ColorComboBoxUtil {

    /**
     * Creates color combobox components for a given property.
     *
     * @param colorProperty The property to create the components for.
     * @return A new color combobox.
     */
    public static JComponent[] createColorComponents(Property colorProperty) {
        JComponent[] components = new JComponent[2];
        components[0] = new JLabel(colorProperty.getDescriptor().getDisplayName() + ":");
        components[1] = createColorCombobox(colorProperty);
        return components;
    }

    public static ColorComboBox createColorComboBoxInternal() {
        Color TRANSPARENT = new Color(0, 0, 0, 0);
        String TRANSPARENT_NAME = "Transparent";

        Color[] colors = new Color[]{
                Color.WHITE,
                Color.LIGHT_GRAY,
                Color.GRAY,
                Color.DARK_GRAY,
                Color.BLACK,
                Color.BLUE,
                Color.GREEN,
                Color.RED,
                Color.CYAN,
                Color.YELLOW,
                Color.MAGENTA,
                Color.ORANGE,
                Color.PINK
        };

        Color[] customColors = new Color[]{
                TRANSPARENT
        };

        String[] customColorNames = new String[]{
                TRANSPARENT_NAME
        };

        Color[] colorsAll = new Color[colors.length + customColors.length];
        String[] colorNamesAll = new String[colors.length + customColors.length];

        for (int i = 0; i < colors.length; i++) {
            colorsAll[i] = colors[i];
            colorNamesAll[i] = null;
        }
        for (int i = 0; i < customColors.length; i++) {
            colorsAll[colors.length + i] = customColors[i];
            colorNamesAll[colors.length + i] = customColorNames[i];
        }

//        ColorComboBox colorComboBox = new ColorComboBox(colorsAll, new String[0], true);
        ColorComboBox colorComboBox = new ColorComboBox(colorsAll, colorNamesAll, true);
//        colorComboBox.setSelectedColor(TRANSPARENT);
//        colorComboBox.repaint();
//        colorComboBox.firePropertyChange("selectedColor", 0,1);
//        colorComboBox.repaint();

        return colorComboBox;

    }


    public static ColorComboBox createColorCombobox() {

        ColorComboBox colorComboBox = createColorComboBoxInternal();
        colorComboBox.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
            }
        });
        colorComboBox.setPreferredSize(new Dimension(colorComboBox.getWidth(), 25));


        return colorComboBox;
    }


    public static ColorComboBox createColorCombobox(final Property property) {

        ColorComboBox colorComboBox = createColorComboBoxInternal();

        colorComboBox.setSelectedColor(property.getValue());
        colorComboBox.repaint();

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
                    SystemUtils.LOG.warning("Color conversion error: " + e1.getMessage());
                }
            }
        });
        colorComboBox.setPreferredSize(new Dimension(colorComboBox.getWidth(), 30));


        // Modification by Daniel Knowles SEP2018
        // Add PropertyChangeListener to the passed in property which when triggered sets the colorComboBox selected color.
        // This fixes bug where colorComboBox was not listening to properties change event when DefaultConfigController was
        // loading the user saved preferences

        property.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                colorComboBox.setSelectedColor(property.getValue());
            }
        });

        return colorComboBox;
    }
}
