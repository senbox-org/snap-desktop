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

public class EmptyImageInfoForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;

    public EmptyImageInfoForm(ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
    }

    @Override
    public ColorManipulationForm getParentForm() {
        return parentForm;
    }

    @Override
    public void handleFormShown(ColorFormModel formModel) {
    }

    @Override
    public void handleFormHidden(ColorFormModel formModel) {
    }

    @Override
    public void updateFormModel(ColorFormModel formModel) {
    }

    @Override
    public void resetFormModel(ColorFormModel formModel) {
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
    }

    @Override
    public AbstractButton[] getToolButtons() {
        return new AbstractButton[0];
    }

    @Override
    public Component getContentPanel() {
        return parentForm.getFormModel().createEmptyContentPanel();
    }

    @Override
    public RasterDataNode[] getRasters() {
        return new RasterDataNode[0];
    }

    @Override
    public MoreOptionsForm getMoreOptionsForm() {
        return null;
    }
}
