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

package org.esa.snap.gui.preferences;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import org.openide.awt.ColorComboBox;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Contains some static helper functions.
 *
 * @author thomas
 */
public class PreferenceUtils {

    public static void addNote(JPanel pageUI, String text) {
        JLabel note = new JLabel(text);
        if (note.getFont() != null) {
            note.setFont(note.getFont().deriveFont(Font.ITALIC));
        }
        note.setForeground(new Color(0, 0, 92));
        pageUI.add(note);
    }

    public static JComponent[] createColorComponents(Property colorProperty) {
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
}
