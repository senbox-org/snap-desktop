/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * This action closes all opened products other than the one selected.
 */
@ActionID(category = "File", id = "CloseOtherProductsAction")
@ActionRegistration(displayName = "#CTL_CloseAllOthersActionName",lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/File", position = 30),
        @ActionReference(path = "Context/Product/Product", position = 80, separatorAfter = 85),
})
@NbBundle.Messages({"CTL_CloseAllOthersActionName=Close Other Products"})
public class CloseOtherProductsAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;
    private Product[] products;

    public CloseOtherProductsAction() {
        this(Utilities.actionsGlobalContext());
    }

    public CloseOtherProductsAction(Lookup lkp) {
        super(Bundle.CTL_CloseAllOthersActionName());
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        ProductManager productManager = SnapApp.getDefault().getProductManager();
        productManager.addListener(new CloseOtherProductListener());
        setEnableState();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new CloseOtherProductsAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    private void setEnableState() {
        products = SnapApp.getDefault().getProductManager().getProducts();
        ProductNode productNode = lkp.lookup(ProductNode.class);
        setEnabled(productNode != null && products.length>1);
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        final ProductNode productNode = lkp.lookup(ProductNode.class);
        products = SnapApp.getDefault().getProductManager().getProducts();
        final Product selectedProduct = productNode.getProduct();
        final List<Product> productsToClose = new ArrayList<>(products.length);
        for (Product product : products) {
            if (product != selectedProduct) {
                productsToClose.add(product);
            }
        }
        new CloseProductAction(productsToClose).execute();
        setEnableState();
    }

    private class CloseOtherProductListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            updateEnableState();
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            updateEnableState();
        }

        private void updateEnableState() {
            setEnableState();
        }
    }
}
