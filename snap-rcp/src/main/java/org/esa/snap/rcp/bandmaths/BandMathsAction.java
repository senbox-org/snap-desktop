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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeList;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.EXPLORER;

@ActionID(
        category = "Tools",
        id = "BandMathsAction"
)
@ActionRegistration(
        displayName = "#CTL_BandMathsAction_MenuText",
        popupText = "#CTL_BandMathsAction_MenuText",
//        iconBase = "org/esa/snap/rcp/icons/BandMaths.gif", // icon is not nice
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Raster", position = 0),
        @ActionReference(path = "Context/Product/Product", position = 10),
        @ActionReference(path = "Context/Product/RasterDataNode", position = 20)
})
@Messages({
        "CTL_BandMathsAction_MenuText=Band Maths...",
        "CTL_BandMathsAction_ShortDescription=Create a new band using an arbitrary mathematical expression"
})
public class BandMathsAction extends AbstractAction implements HelpCtx.Provider {

    private static final String HELP_ID = "bandArithmetic";

    public BandMathsAction() {
        super(Bundle.CTL_BandMathsAction_MenuText());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_BandMathsAction_ShortDescription());
        final ProductManager productManager = SnapApp.getDefault().getProductManager();
        setEnabled(productManager.getProductCount() > 0);
        productManager.addListener(new PMListener());
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

        Product product = SnapApp.getDefault().getSelectedProduct(EXPLORER);
        if (product == null) {
            product = products.getAt(0);
        }

        Collection<? extends RasterDataNode> selectedRasters = Utilities.actionsGlobalContext().lookupAll(RasterDataNode.class);
        String expression = selectedRasters.stream().map(ProductNode::getName).collect(Collectors.joining(" + "));
        BandMathsDialog bandMathsDialog = new BandMathsDialog(product, products, expression, HELP_ID);
        bandMathsDialog.setResizable(false);
        bandMathsDialog.show();
    }

    private class PMListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            updateEnableState();
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            updateEnableState();
        }

        private void updateEnableState() {
            setEnabled(SnapApp.getDefault().getProductManager().getProductCount() > 0);
        }

    }
}
