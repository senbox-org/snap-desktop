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
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;

import javax.swing.*;
import java.awt.*;

/**
 * Non-public helper class.
 *
 * @author thomas
 */
class PreferencesPanel {

    private final BindingContext bindingContext;

    private JPanel panel;
    private boolean changed = false;

    PreferencesPanel(JPanel panel, BindingContext bindingContext) {
        this.panel = panel;
        this.bindingContext = bindingContext;
        for (Property property : bindingContext.getPropertySet().getProperties()) {
            property.addPropertyChangeListener(evt -> {
                changed = true;
            });
        }
    }

    JPanel getComponent() {
        if (panel == null) {
            panel = new JPanel(new BorderLayout());
            panel.add(new PropertyPane(bindingContext).createPanel(), BorderLayout.CENTER);
        }
        return panel;
    }

    boolean isChanged() {
        return changed;
    }

    void setChanged(boolean changed) {
        this.changed = changed;
    }
}
