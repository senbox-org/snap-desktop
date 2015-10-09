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

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.ProductSubsetDialog;
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
 * taken from the currently visible image area, if any.
 *
 * @author Norman Fomferra
 */
@ActionID(category = "Raster", id = "CreateSubsetAction")
@ActionRegistration(
        displayName = "#CTL_CreateSubsetAction_Name"
)
@ActionReferences({
        @ActionReference(path = "Menu/Raster", position = 50)
})
@NbBundle.Messages({
        "CTL_CreateSubsetAction_Name=Subset...",
        "CTL_CreateSubsetAction_Title=Subset"
})
public class CreateSubsetAction extends AbstractAction {

    static int subsetNumber;

    private final ProductNode sourceNode;

    public CreateSubsetAction(ProductNode sourceNode) {
        this.sourceNode = sourceNode;
    }

    @Override
    public void actionPerformed(ActionEvent ignored) {
        Product product = sourceNode.getProduct();
        if (product != null) {
            createSubset(product, getInitialBounds(product));
        }
    }

    public static void createSubset(Product sourceProduct, Rectangle bounds) {
        final String subsetName = "subset_" + CreateSubsetAction.subsetNumber + "_of_" + sourceProduct.getName();
        final ProductSubsetDef initSubset = new ProductSubsetDef();
        initSubset.setRegion(bounds);
        initSubset.setNodeNames(sourceProduct.getBandNames());
        initSubset.addNodeNames(sourceProduct.getTiePointGridNames());
        initSubset.setIgnoreMetadata(false);
        final ProductSubsetDialog subsetDialog = new ProductSubsetDialog(SnapApp.getDefault().getMainFrame(),
                                                                         sourceProduct, initSubset);
        if (subsetDialog.show() != ProductSubsetDialog.ID_OK) {
            return;
        }
        final ProductSubsetDef subsetDef = subsetDialog.getProductSubsetDef();
        if (subsetDef == null) {
            SnapDialogs.showInformation(Bundle.CTL_CreateSubsetFromViewAction_Title(),
                                        "No product subset created.",
                                        null);
            return;
        }
        try {
            final Product subset = sourceProduct.createSubset(subsetDef, subsetName,
                                                              sourceProduct.getDescription());
            SnapApp.getDefault().getProductManager().addProduct(subset);
            CreateSubsetAction.subsetNumber++;
        } catch (Exception e) {
            final String msg = "An error occurred while creating the product subset:\n" +
                    e.getMessage();
            SnapApp.getDefault().handleError(msg, e);
        }
    }

    private Rectangle getInitialBounds(Product product) {
        Rectangle bounds = null;
        ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view != null && view.getProduct() == product) {
            bounds = view.getVisibleImageBounds();
        }
        return bounds;
    }
}
