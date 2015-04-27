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

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.datamodel.ProductNodeList;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Tools",
        id = "BandMathsAction"
)
@ActionRegistration(
        displayName = "#CTL_BandMathsAction_MenuText",
        popupText = "#CTL_BandMathsAction_MenuText",
        iconBase = "org/esa/snap/rcp/icons/BandMaths.gif",
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
        "CTL_BandMathsAction_MenuText=Create Band from Math Expression...",
        "CTL_BandMathsAction_ShortDescription=Create a new band using an arbitrary mathematical expression"
})
public class BandMathsAction extends AbstractAction implements HelpCtx.Provider {

    private static final String HELP_ID = "bandArithmetic";
    private final Product product;

    public BandMathsAction(ProductNode node) {
        super(Bundle.CTL_BandMathsAction_MenuText());
        product = node.getProduct();
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_BandMathsAction_ShortDescription());
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("icons/BandMaths.gif", false));
        putValue(Action.LARGE_ICON_KEY, ImageUtilities.loadImageIcon("icons/BandMaths24.gif", false));
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final ProductNodeList<Product> products = new ProductNodeList<>();
        Product[] openedProducts = SnapApp.getDefault().getProductManager().getProducts();
        for (Product prod : openedProducts) {
            products.add(prod);
        }

        BandMathsDialog bandMathsDialog = new BandMathsDialog(product, products, HELP_ID);
        bandMathsDialog.show();

    }

}
