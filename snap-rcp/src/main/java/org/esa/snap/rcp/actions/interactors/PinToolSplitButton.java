/*
 * Copyright (C) 2025 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.actions.interactors;

import org.esa.snap.ui.color.ColorComboBox;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A split-button component for the Pin Tool that combines a toggle button with a color button.
 * The toggle button activates the pin tool, and the color button shows the current color and
 * allows selecting a new color for pins.
 */
public class PinToolSplitButton extends JPanel {

    /**
     * Property name for color change events.
     */
    public static final String COLOR_PROPERTY = "pinColor";

    private final ColorComboBox colorComboBox;
    private final PropertyChangeSupport propertyChangeSupport;

    /**
     * Creates a new PinToolSplitButton with the given action.
     *
     * @param action the action to be performed when the toggle button is clicked
     */
    public PinToolSplitButton(Action action) {
        super(new BorderLayout(0, 0));
        setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));

        this.propertyChangeSupport = new PropertyChangeSupport(this);

        JToggleButton toggleButton = new JToggleButton(action);
        toggleButton.setText(null);
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        toggleButton.setBorderPainted(false);

        // Create the color button with initial color
        colorComboBox = new ColorComboBox(Color.BLUE);
        // enforce the size
        Dimension comboxBoxSize = new Dimension(25, toggleButton.getPreferredSize().height - 10);
        colorComboBox.setPreferredSize(comboxBoxSize);
        colorComboBox.setSize(comboxBoxSize);
        colorComboBox.setMinimumSize(comboxBoxSize);
        colorComboBox.setMinimumSize(comboxBoxSize);
        colorComboBox.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0) );


        // Add a property change listener to forward color change events with the correct property name
        colorComboBox.addPropertyChangeListener(ColorComboBox.SELECTED_COLOR_PROPERTY, evt -> {
            Color oldColor = (Color) evt.getOldValue();
            Color newColor = (Color) evt.getNewValue();
            propertyChangeSupport.firePropertyChange(COLOR_PROPERTY, oldColor, newColor);
        });

        add(toggleButton, BorderLayout.CENTER);
        add(colorComboBox, BorderLayout.EAST);
    }

    /**
     * Adds a property change listener.
     *
     * @param listener the listener to add
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // Always call super to ensure proper Swing component behavior
        super.addPropertyChangeListener(listener);

        // Only delegate to our propertyChangeSupport if it's initialized
        if (propertyChangeSupport != null) {
            propertyChangeSupport.addPropertyChangeListener(listener);
        }
    }

    /**
     * Adds a property change listener for the specified property.
     *
     * @param propertyName the name of the property to listen on
     * @param listener     the listener to add
     */
    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        // Always call super to ensure proper Swing component behavior
        super.addPropertyChangeListener(propertyName, listener);

        // Only delegate to our propertyChangeSupport if it's initialized
        if (propertyChangeSupport != null) {
            propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
        }
    }

    /**
     * Removes a property change listener.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // Always call super to ensure proper Swing component behavior
        super.removePropertyChangeListener(listener);

        // Only delegate to our propertyChangeSupport if it's initialized
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
     * Removes a property change listener for the specified property.
     *
     * @param propertyName the name of the property that was listened on
     * @param listener     the listener to remove
     */
    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        // Always call super to ensure proper Swing component behavior
        super.removePropertyChangeListener(propertyName, listener);

        // Only delegate to our propertyChangeSupport if it's initialized
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }

    /**
     * Gets the current color.
     *
     * @return the current color
     */
    public Color getCurrentColor() {
        return colorComboBox.getSelectedColor();
    }
}