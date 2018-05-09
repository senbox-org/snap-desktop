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

package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.RasterDataNode;

import javax.swing.AbstractButton;
import java.awt.Component;


public interface ColorManipulationChildForm {
    ColorManipulationForm getParentForm();

    void handleFormShown(ColorFormModel formModel);

    void handleFormHidden(ColorFormModel formModel);

    void updateFormModel(ColorFormModel formModel);

    void resetFormModel(ColorFormModel formModel);

    void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster);

    Component getContentPanel();

    AbstractButton[] getToolButtons();

    MoreOptionsForm getMoreOptionsForm();

    RasterDataNode[] getRasters();
}
