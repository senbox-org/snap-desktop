/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.util;

import com.bc.ceres.swing.binding.ComponentAdapter;
import org.esa.beam.framework.datamodel.ImageInfo;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A binding for the SNAP {@link ColorComboBox}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ColorComboBoxAdapter extends ComponentAdapter implements PropertyChangeListener {
    private ColorComboBox colorComboBox;

    public ColorComboBoxAdapter(ColorComboBox colorComboBox) {
        this.colorComboBox = colorComboBox;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[] {colorComboBox};
    }

    @Override
    public void bindComponents() {
        colorComboBox.addPropertyChangeListener(this);
    }

    @Override
    public void unbindComponents() {
        colorComboBox.removePropertyChangeListener(this);
    }

    @Override
    public void adjustComponents() {
        final Color color = (Color) getBinding().getPropertyValue();
        colorComboBox.setSelectedItem(ImageInfo.NO_COLOR.equals(color) ? null : color);
    }

    private void adjustPropertyValue() {
        final Color color = (Color) colorComboBox.getSelectedItem();
        getBinding().setPropertyValue(color == null ? ImageInfo.NO_COLOR : color);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        adjustPropertyValue();
    }
}
