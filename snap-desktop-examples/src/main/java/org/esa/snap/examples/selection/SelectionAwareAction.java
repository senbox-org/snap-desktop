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

package org.esa.snap.examples.selection;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.rcp.actions.tools.AttachPixelGeoCodingAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Collection;

@ActionID(
        category = "Example",
        id = "SelectionAwareAction"
)
@ActionRegistration(
        displayName = "#CTL_SelectionAwareActionActionText",
        popupText = "#CTL_SelectionAwareActionDialogTitle",
        lazy = false
)
@ActionReference(
        path = "Menu/Tools/Examples",
        position = 153
)
@NbBundle.Messages({
        "CTL_SelectionAwareActionActionText=Number of Selected Products",
        "CTL_SelectionAwareActionDialogTitle=#CTL_SelectionAwareActionActionText",
        "CTL_SelectionAwareActionDescription=Shows the number of selected products, if any"
})
/**
 * This class is an example implementation and does not belong to the public API.
 **/
public class SelectionAwareAction extends AbstractSnapAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public SelectionAwareAction() {
        this(Utilities.actionsGlobalContext());
    }

    public SelectionAwareAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<Product> lkpContext = lkp.lookupResult(Product.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_SelectionAwareActionActionText());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_SelectionAwareActionDescription());
        updateEnableState();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new AttachPixelGeoCodingAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        updateEnableState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // if the action is invoked the number of selected products is shown.
        SnapDialogs.showInformation(Bundle.CTL_SelectionAwareActionDialogTitle(),
                                    String.format("Number of selected products is %d", getNumSelectedProducts()), null);

    }

    private void updateEnableState() {
        setEnabled(getNumSelectedProducts() > 0);
    }

    private int getNumSelectedProducts() {
        Lookup.Result<Product> result = lkp.lookupResult(Product.class);
        Collection<? extends Product> products = result.allInstances();
        return products.size();
    }

}
