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

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.metadata.MetadataViewTopComponent;
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

/**
 * This action opens an Metadata View of the currently selected Metadata Node
 */
@ActionID(category = "File", id = "OpenMetadataViewAction" )
@ActionRegistration(displayName = "#CTL_OpenMetadataViewAction_MenuText" )
@ActionReferences({
        @ActionReference(path = "Context/Product/MetadataElement", position = 100),
        @ActionReference(path = "Menu/Window", position = 120)
})
@NbBundle.Messages("CTL_OpenMetadataViewAction_MenuText=Open Metadata Window")
public class OpenMetadataViewAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private Lookup lookup;
    private final Lookup.Result<ProductNode> result;

    public OpenMetadataViewAction() {
        this(Utilities.actionsGlobalContext());
    }

    public OpenMetadataViewAction(Lookup lookup) {
        this.lookup = lookup;
        result = lookup.lookupResult(ProductNode.class);
        result.addLookupListener(
                WeakListeners.create(LookupListener.class, this, result));
        setEnableState();
        putValue(Action.NAME, Bundle.CTL_OpenMetadataViewAction_MenuText());
    }

    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new OpenMetadataViewAction(lookup);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnableState();
    }

    private void setEnableState() {
        ProductNode productNode = lookup.lookup(ProductNode.class);
        setEnabled(productNode != null && productNode instanceof MetadataElement);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        openMetadataView((MetadataElement) SnapApp.getDefault().getSelectedProductNode());
    }
    
    public void openMetadataView(final MetadataElement element) {
        openDocumentWindow(element);
    }

    private MetadataViewTopComponent openDocumentWindow(final MetadataElement element) {
        final MetadataViewTopComponent metadataViewTopComponent = new MetadataViewTopComponent(element);
        DocumentWindowManager.getDefault().openWindow(metadataViewTopComponent);
        metadataViewTopComponent.requestSelected();
        return metadataViewTopComponent;
    }
}
