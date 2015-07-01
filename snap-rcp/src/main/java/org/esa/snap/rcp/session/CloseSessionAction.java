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
package org.esa.snap.rcp.session;

import org.esa.snap.framework.datamodel.ProductNode;
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

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;


@ActionID( category = "File", id = "org.esa.snap.rcp.session.CloseSessionAction" )
@ActionRegistration( displayName = "#CTL_CloseSessionAction_MenuText", lazy = false )
@ActionReference(path = "Menu/File", position = 35,separatorAfter = 37)
@NbBundle.Messages({
        "CTL_CloseSessionAction_MenuText=Close Session",
        "CTL_CloseSessionAction_ShortDescription=Close the current SNAP session."
})
public class CloseSessionAction extends AbstractAction implements LookupListener,ContextAwareAction {

    public static final String ID = "closeSession";
    private final Lookup.Result<ProductNode> result;
    private final Lookup lookup;


    public CloseSessionAction(){this(Utilities.actionsGlobalContext());}
    public CloseSessionAction(Lookup lookup) {

        super(Bundle.CTL_CloseSessionAction_MenuText());
        this.lookup = lookup;
        result = lookup.lookupResult(ProductNode.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class,this,result));
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final VisatApp app = VisatApp.getApp();
        app.closeAllProducts();
        app.setSessionFile(null);
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new CloseSessionAction(actionContext);
    }


    @Override
    public void resultChanged(LookupEvent ev) {
        ProductNode productNode = lookup.lookup(ProductNode.class);
        setEnabled(productNode != null);
    }
}
