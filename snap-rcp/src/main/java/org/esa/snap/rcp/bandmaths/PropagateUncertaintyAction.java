/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.bandmaths;

import org.esa.snap.core.datamodel.VirtualBand;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

@ActionID(category = "Tools", id = "PropagateUncertaintyAction" )
@ActionRegistration(displayName = "#CTL_PropagateUncertaintyAction_MenuText", lazy = true )
@ActionReferences({
        @ActionReference(path = "Menu/Raster", position = 25),
        @ActionReference(path = "Context/Product/VirtualBand", position = 200), })
@Messages({
        "CTL_PropagateUncertaintyAction_MenuText=Propagate Uncertainty...",
        "CTL_PropagateUncertaintyAction_ShortDescription=Perform Gaussian uncertainty propagation from virtual band according to GUM 1995, chapter 5"
})
public class PropagateUncertaintyAction extends AbstractAction {

    private VirtualBand virtualBand;

    public PropagateUncertaintyAction(VirtualBand virtualBand) {
        super(Bundle.CTL_PropagateUncertaintyAction_MenuText());
        this.virtualBand = virtualBand;
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_PropagateUncertaintyAction_ShortDescription());
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        PropagateUncertaintyDialog dialog = new PropagateUncertaintyDialog(virtualBand);
        dialog.show();
    }
}
