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

package org.esa.snap.core.gpf.ui.mosaic;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * Mosaicing action.
 *
 * @author Norman Fomferra
 */
@ActionID(category = "Operators", id = "org.esa.snap.core.gpf.ui.mosaic.MosaicAction")
@ActionRegistration(displayName = "#CTL_MosaicAction_Name")
@ActionReference(path = "Menu/Raster/Geometric Operations")
@NbBundle.Messages("CTL_MosaicAction_Name=Mosaicing")
public class MosaicAction extends AbstractSnapAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new MosaicDialog(Bundle.CTL_MosaicAction_Name(),
                                      "mosaicAction", getAppContext());
        }
        dialog.show();
    }

    @Override
    public boolean isEnabled() {
        return GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Mosaic") != null;
    }
}
