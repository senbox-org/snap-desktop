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

package org.esa.snap.gui.bandmaths;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.snap.gui.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Tools",
        id = "org.esa.snap.gui.bandmaths.BandMathsAction"
)
@ActionRegistration(
        displayName = "#ACT_MenuText",
        popupText = "#ACT_MenuText",
        iconBase = "org/esa/snap/gui/icons/BandMaths24.gif",
        lazy = true
)
@ActionReferences({
        @ActionReference(
                path = "Menu/Tools",
                position = 110
        ),
        @ActionReference(
                path = "Shortcuts",
                name = "D-M"
        ),
        @ActionReference(
                path = "Context/Product/Product",
                position = 200
        ),
        @ActionReference(
                path = "Context/Product/RasterDataNode",
                position = 200
        ),
})
@Messages({
        "ACT_MenuText=Create Band from Math Expression...",
        "ACT_ShortDescription=Create a new band using an arbitrary mathematical expression"
})
public class BandMathsAction extends AbstractAction implements HelpCtx.Provider {

    private static final String HELP_ID = "bandArithmetic";
    private final Product product;

    public BandMathsAction(ProductNode node) {
        super(Bundle.ACT_MenuText());
        product = node.getProduct();
        putValue(Action.SHORT_DESCRIPTION, Bundle.ACT_ShortDescription());
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("icons/BandMaths16.gif", false));
        putValue(Action.LARGE_ICON_KEY, ImageUtilities.loadImageIcon("icons/BandMaths24.gif", false));
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        SnapApp snapApp = SnapApp.getDefault();

        final ProductNodeList<Product> products = new ProductNodeList<>();
        Product[] openedProducts = SnapApp.getDefault().getProductManager().getProducts();
        for (ProductNode prodNode : openedProducts) {
            products.add(prodNode.getProduct());
        }

        BandMathsDialog bandMathsDialog = new BandMathsDialog(snapApp,
                                                              product,
                                                              products,
                                                              HELP_ID);
        bandMathsDialog.show();

    }

}