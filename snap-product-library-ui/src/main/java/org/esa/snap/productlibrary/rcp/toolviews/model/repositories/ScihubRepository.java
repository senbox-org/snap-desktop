/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews.model.repositories;

import org.esa.snap.productlibrary.db.Credentials;
import org.esa.snap.productlibrary.db.ProductQueryInterface;
import org.esa.snap.productlibrary.opensearch.CopernicusProductQuery;
import org.esa.snap.graphbuilder.rcp.dialogs.PromptDialog;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryToolView;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;

/**
 * Created by luis on 24/02/2017.
 */
public class ScihubRepository implements RepositoryInterface {

    private static final ImageIcon icon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/scihub24.png", ProductLibraryToolView.class);

    public String getName() {
        return CopernicusProductQuery.NAME;
    }

    @Override
    public String toString() {
        return getName();
    }

    public ImageIcon getIconImage() {
        return icon;
    }

    public ProductQueryInterface getProductQueryInterface() {
        checkCredentials();

        return CopernicusProductQuery.instance();
    }

    public void resetCredentials() {
        promptForCredentials();
    }

    private static void promptForCredentials() {
        final PromptDialog dlg = new PromptDialog(CopernicusProductQuery.COPERNICUS_HOST, new PromptDialog.Descriptor[] {
                new PromptDialog.Descriptor("User name:", "", PromptDialog.TYPE.TEXTFIELD),
                new PromptDialog.Descriptor("Password:", "", PromptDialog.TYPE.PASSWORD)
        });
        dlg.show();
        if (dlg.IsOK()) {
            try {
                final String user = dlg.getValue("User name:");
                final String password = dlg.getValue("Password:");

                Credentials.instance().put(CopernicusProductQuery.COPERNICUS_HOST, user, password);
            } catch (Exception ex) {
                Dialogs.showError(ex.getMessage());
            }
        }
    }

    private static void checkCredentials() {
        Credentials.CredentialInfo credentialInfo = Credentials.instance().get(CopernicusProductQuery.COPERNICUS_HOST);
        if (credentialInfo == null) {
            // prompt for user name and password
            promptForCredentials();
        }
    }
}
