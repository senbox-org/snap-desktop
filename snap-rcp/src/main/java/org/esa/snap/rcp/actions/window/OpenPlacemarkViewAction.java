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
package org.esa.snap.rcp.actions.window;

import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.placemark.PlacemarkViewTopComponent;
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

@ActionID(category = "View", id = "OpenPlacemarkViewAction" )
@ActionRegistration(
        displayName = "#CTL_OpenPlacemarkViewAction_MenuText",
        iconBase = "org/esa/snap/rcp/icons/RsVector16.gif"
)
@ActionReferences({
        @ActionReference(path = "Menu/Window", position = 125),
        @ActionReference(path = "Context/Product/VectorDataNode", position = 100),
})
@NbBundle.Messages("CTL_OpenPlacemarkViewAction_MenuText=Open Placemark Window")
public class OpenPlacemarkViewAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private Lookup lookup;
    private final Lookup.Result<VectorDataNode> result;

    public OpenPlacemarkViewAction() {
        this(Utilities.actionsGlobalContext());
    }

    public OpenPlacemarkViewAction(Lookup lookup) {
        this.lookup = lookup;
        result = lookup.lookupResult(VectorDataNode.class);
        result.addLookupListener(
                WeakListeners.create(LookupListener.class, this, result));
        setEnableState();
        putValue(Action.NAME, Bundle.CTL_OpenPlacemarkViewAction_MenuText());
    }

    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new OpenPlacemarkViewAction(lookup);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnableState();
    }

    private void setEnableState() {
        setEnabled(lookup.lookup(VectorDataNode.class) != null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ProductNode selectedProductNode = SnapApp.getDefault().getSelectedProductNode();
        if (selectedProductNode instanceof VectorDataNode) {
            VectorDataNode vectorDataNode = (VectorDataNode) selectedProductNode;
            openView(vectorDataNode);
        }
    }

    public void openView(final VectorDataNode vectorDataNode) {
        final PlacemarkViewTopComponent placemarkViewTopComponent = new PlacemarkViewTopComponent(vectorDataNode);
        DocumentWindowManager.getDefault().openWindow(placemarkViewTopComponent);
        placemarkViewTopComponent.requestSelected();
    }
}
