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
package org.esa.snap.rcp.placemark;

import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.Placemark;

import java.awt.*;

/**
 * A tool used to create (single click), select (single click on a pin) or edit (double click on a pin) the pins
 * displayed in product scene view.
 */
public class InsertPinInteractor extends InsertPlacemarkInteractor {

    private Color color = Color.BLUE; // Default color

    public InsertPinInteractor() {
        super(PinDescriptor.getInstance());
    }

    public void setCurrentColor(Color color) {
        this.color = color;
    }

    @Override
    protected void updatePlacemarkStyle(Placemark placemark, String defaultStyleCss) {
        if (placemark.getStyleCss().isEmpty()) {
            DefaultFigureStyle placemarkStyle = new DefaultFigureStyle(defaultStyleCss);
            placemarkStyle.setFillColor(color);
            placemark.setStyleCss(placemarkStyle.toCssString());
        }
    }
}
