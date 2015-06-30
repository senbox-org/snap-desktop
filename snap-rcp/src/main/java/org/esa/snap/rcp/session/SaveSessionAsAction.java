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

import org.esa.snap.framework.ui.product.ProductSceneView;
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

/*
Not used now.
Session handling is not implemented yet.

@ActionID( category = "File", id = "org.esa.snap.rcp.session.SaveSessionAsAction" )
@ActionRegistration( displayName = "#CTL_SaveSessionAsAction_MenuText", lazy = false )
@ActionReference(path = "Menu/File", position = 55,separatorAfter = 57)
*/
@NbBundle.Messages({
        "CTL_SaveSessionAsAction_MenuText=Save Session As...",
        "CTL_SaveSessionAsAction_ShortDescription=Save the current SNAP session using a different name."
})
public class SaveSessionAsAction extends AbstractAction implements ContextAwareAction, LookupListener {
    public static final String ID = "saveSessionAs";
    private final Lookup.Result<ProductSceneView> result;
    private final Lookup lookup;

    public SaveSessionAsAction() {
        this(Utilities.actionsGlobalContext());
    }

    public SaveSessionAsAction(Lookup lookup) {
        super(Bundle.CTL_SaveSessionAsAction_MenuText());
        this.lookup = lookup;
        result = lookup.lookupResult(ProductSceneView.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);
    }

    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new SaveSessionAsAction(lookup);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final SaveSessionAction action = new SaveSessionAction();
        action.saveSession(true);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        final VisatApp app = VisatApp.getApp();
        setEnabled(app.getSessionFile() != null);
    }
}
