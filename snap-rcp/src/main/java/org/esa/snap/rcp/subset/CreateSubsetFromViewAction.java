/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.subset;

import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

/**
 * This action opens a product subset dialog with the initial spatial bounds
 * taken from the currently visible image area.
 *
 * @author Norman Fomferra
 */
@ActionID(category = "Raster", id = "CreateSubsetFromViewAction")
@ActionRegistration(
        displayName = "#CTL_CreateSubsetFromViewAction_Name"
)
@ActionReferences({
        @ActionReference(path = "Context/ProductSceneView", position = 100),
})
@NbBundle.Messages({
        "CTL_CreateSubsetFromViewAction_Name=Spatial Subset from View...",
        "CTL_CreateSubsetFromViewAction_Title=Spatial Subset from View"
})
public class CreateSubsetFromViewAction extends AbstractAction {

    private final ProductSceneView view;

    public CreateSubsetFromViewAction(ProductSceneView view) {
        this.view = view;
    }

    @Override
    public void actionPerformed(ActionEvent ignored) {
        Rectangle bounds = view.getVisibleImageBounds();
        if (bounds == null) {
            Dialogs.showWarning(Bundle.CTL_CreateSubsetFromViewAction_Title(),
                                "The selected area is entirely outside the product's spatial boundaries.", null);
        }
        CreateSubsetAction.createSubset(view.getProduct(), bounds, view.getRaster());
    }

}
