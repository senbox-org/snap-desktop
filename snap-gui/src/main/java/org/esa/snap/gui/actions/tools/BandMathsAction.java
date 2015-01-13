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

package org.esa.snap.gui.actions.tools;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.bandmaths.BandMathsDialog;
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
        id = "org.esa.snap.gui.actions.tools.BandMathsAction"
)
@ActionRegistration(
        displayName = "#CTL_BandMathsActionText",
        popupText = "#CTL_BandMathsActionPopupText",
        iconBase = "org/esa/snap/gui/icons/BandMaths24.gif",
        lazy = false
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
                path = "Context/Product/Band",
                position = 200
        ),
        @ActionReference(
                path = "Context/Product/TPGrid",
                position = 200
        )}
)
@Messages({
        "CTL_BandMathsActionText=Create Band from Math Expression...",
        "CTL_BandMathsActionPopupText=Create Band from Math Expression...",
        "CTL_BandMathsShortDescription=Create a new band using an arbitrary mathematical expression"
})
public class BandMathsAction extends AbstractAction implements HelpCtx.Provider, ContextAwareAction, LookupListener {

    private static final String HELP_ID = "bandArithmetic";
    private final Lookup lookup;
    private final Lookup.Result<ProductNode> context;

    public BandMathsAction() {
        this(Utilities.actionsGlobalContext());
    }

    public BandMathsAction(Lookup lookup) {
        super(Bundle.CTL_BandMathsActionText());
        this.lookup = lookup;
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_BandMathsShortDescription());
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("icons/BandMaths16.gif", false));
        putValue(Action.LARGE_ICON_KEY, ImageUtilities.loadImageIcon("icons/BandMaths24.gif", false));
        context = lookup.lookupResult(ProductNode.class);
        context.addLookupListener(WeakListeners.create(LookupListener.class, this, lookup));
        setEnableState();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new BandMathsAction(actionContext);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        SnapApp snapApp = SnapApp.getInstance();

        final ProductNodeList<Product> products = new ProductNodeList<>();
        Product[] openedProducts = SnapApp.getInstance().getProductManager().getProducts();
        for (ProductNode prodNode : openedProducts) {
            products.add(prodNode.getProduct());
        }

        Product currentProduct = lookup.lookup(ProductNode.class).getProduct();
        BandMathsDialog bandMathsDialog = new BandMathsDialog(snapApp,
                                                              currentProduct,
                                                              products,
                                                              HELP_ID);
        bandMathsDialog.show();

    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    private void setEnableState() {
        setEnabled(!context.allInstances().isEmpty());
    }

}