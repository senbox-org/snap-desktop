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

import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.CloseAllProductsAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.File;


@ActionID(category = "File", id = "org.esa.snap.rcp.session.CloseSessionAction")
@ActionRegistration(displayName = "#CTL_CloseSessionAction_MenuText", lazy = false)
@ActionReference(path = "Menu/File/Session", position = 45)
@NbBundle.Messages({
        "CTL_CloseSessionAction_MenuText=Close Session",
        "CTL_CloseSessionAction_ShortDescription=Close the current SNAP session."
})
public class CloseSessionAction extends AbstractAction {

    public static final String ID = "closeSession";
    final SessionManager sessionManager = SessionManager.getDefault();

    public CloseSessionAction() {
        super(Bundle.CTL_CloseSessionAction_MenuText());
        ProductManager productManager = SnapApp.getDefault().getProductManager();
        productManager.addListener(new CloseSessionListener());
        setEnabled(false);
    }


    @Override
    public void actionPerformed(ActionEvent event) {
        sessionManager.setSessionFile((File) null);
        CloseAllProductsAction closeProductAction = new CloseAllProductsAction();
        closeProductAction.execute();
    }

    private class CloseSessionListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            updateEnableState();
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            updateEnableState();
        }

        private void updateEnableState() {
            setEnabled(SnapApp.getDefault().getProductManager().getProductCount() > 0 || sessionManager.getSessionFile()!=null);
        }
    }
}
