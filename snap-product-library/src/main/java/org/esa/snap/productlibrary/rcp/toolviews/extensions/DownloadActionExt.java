/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews.extensions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.engine_utilities.download.opendata.OpenData;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;
import java.io.File;

/**
 * Export a list of files to text file
 */
public class DownloadActionExt implements ProductLibraryActionExt {

    private static final ImageIcon downloadIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/downloads-icon22.png", DownloadActionExt.class);
    private JButton button = null;
    private ProductLibraryActions actionHandler;

    private static final String COPERNICUS_HOST = "https://scihub.copernicus.eu";
    private static final String COPERNICUS_ODATA_ROOT = "https://scihub.copernicus.eu/dhus/odata/v1/";

    private static final File outputFolder = new File("e:\\tmp\\");

    public void setActionHandler(final ProductLibraryActions actionHandler) {
        this.actionHandler = actionHandler;
    }

    public JButton getButton(final JPanel panel) {
        if (button == null) {
            button = DialogUtils.createButton("downloadButton", "Download selected products", downloadIcon, panel, DialogUtils.ButtonStyle.Icon);
        }
        return button;
    }

    public void selectionChanged(final ProductEntry[] selections) {
        button.setEnabled(selections.length > 0 && !ProductLibraryActions.allProductsExist(selections));
    }

    public void performAction(final ProgressMonitor pm) {

        final ProductEntry[] selections = actionHandler.getSelectedProductEntries();
        pm.beginTask("Downloading...", selections.length);
        try {
            final OpenData openData = new OpenData(COPERNICUS_HOST, COPERNICUS_ODATA_ROOT);

            for(ProductEntry entry : selections) {
                OpenData.Entry oData = openData.getEntryByID(entry.getRefID());
                SystemUtils.LOG.info(oData.fileName);

                openData.getProduct(entry.getRefID(), oData, outputFolder, new SubProgressMonitor(pm, 1));
            }
        } catch (Exception e) {
            Dialogs.showError("unable to download " + e.getMessage());
        } finally {
            pm.done();
        }
    }
}
