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

package org.esa.snap.rcp.preferences;

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


/**
 * Contains some static helper functions.
 *
 * @author thomas
 * @author Daniel Knowles
 */
// SEP2018 - Daniel Knowles - Fixes bug where colorComboBox was not listening to properties change event when
// DefaultConfigController was loading the user saved preferences

public class PreferenceUtils {

    /**
     * Adds a text note to the given <code>JPanel</code>.
     *
     * @param panel A panel to add the note to.
     * @param text  The note text.
     */
    public static void addNote(JPanel panel, String text) {
        JLabel note = new JLabel(text);
        if (note.getFont() != null) {
            note.setFont(note.getFont().deriveFont(Font.ITALIC));
        }
        note.setForeground(new Color(0, 0, 92));
        panel.add(note);
    }

    /**
     * Creates color combobox components for a given property.
     *
     * @param colorProperty The property to create the components for.
     *
     * @return A new color combobox.
     */
    public static JComponent[] createColorComponents(Property colorProperty) {
        JComponent[] components = new JComponent[2];
        components[0] = new JLabel(colorProperty.getDescriptor().getDisplayName() + ":");
        components[1] = createColorCombobox(colorProperty);
        return components;
    }

    /**
     * Creates a <code>JPanel</code> containing a label with the given text and a horizontal line.
     *
     * @param title The label text.
     *
     * @return A <code>JPanel</code> with the title label.
     */
    public static JPanel createTitleLabel(String title) {
        TableLayout layout = new TableLayout(3);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 0.0);
        layout.setColumnWeightX(2, 1.0);
        layout.setColumnFill(0, TableLayout.Fill.NONE);
        layout.setColumnFill(1, TableLayout.Fill.NONE);
        layout.setColumnFill(2, TableLayout.Fill.HORIZONTAL);
        JPanel comp = new JPanel(layout);
        JLabel label = new JLabel(title);
        comp.add(label);
        comp.add(new JLabel("   "));
        comp.add(new JSeparator());
        return comp;
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
                    SystemUtils.LOG.warning("Color preference conversion error: " + e1.getMessage());
                }
            }
        });
        colorComboBox.setPreferredSize(new Dimension(colorComboBox.getWidth(), 25));

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
