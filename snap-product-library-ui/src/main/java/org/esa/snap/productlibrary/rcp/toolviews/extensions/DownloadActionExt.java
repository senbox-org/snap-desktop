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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.productlibrary.opendata.OpenData;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.SnapFileChooser;
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

    private File outputFolder;

    public void setActionHandler(final ProductLibraryActions actionHandler) {
        this.actionHandler = actionHandler;
    }

    public JButton getButton(final JPanel panel) {
        if (button == null) {
            button = DialogUtils.createButton("downloadButton", "Download", downloadIcon, panel, DialogUtils.ButtonStyle.Icon);
        }
        return button;
    }

    public void selectionChanged(final ProductEntry[] selections) {
        button.setEnabled(selections.length > 0 && !ProductLibraryActions.allProductsExist(selections));
    }

    private static File requestFolderForSave(String title, String preferenceKey) {

        File file;
        do {
            file = requestFolderForSave2(title, preferenceKey);
            if (file == null) {
                return null; // Cancelled
            }
        } while (file == null);
        SystemUtils.LOG.info("Download to " + file.getAbsolutePath());
        return file;
    }

    private static File requestFolderForSave2(String title, final String preferenceKey) {

        Assert.notNull(preferenceKey, "preferenceKey");

        String lastDir = SnapApp.getDefault().getPreferences().get(preferenceKey, SystemUtils.getUserHomeDir().getPath());
        File currentDir = new File(lastDir);

        SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setCurrentDirectory(currentDir);

        fileChooser.setDialogTitle(Dialogs.getDialogTitle(title));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showSaveDialog(SnapApp.getDefault().getMainFrame());
        if (fileChooser.getCurrentDirectory() != null) {
            SnapApp.getDefault().getPreferences().put(preferenceKey, fileChooser.getCurrentDirectory().getPath());
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file == null || file.getName().equals("")) {
                return null;
            }
            String path = file.getPath();

            return new File(path);
        }
        return null;
    }


    public void performAction(final ProgressMonitor pm) {

        outputFolder =  requestFolderForSave("Download product(s) to folder", "snap.download.folder");

        if (outputFolder == null) return;

        final ProductEntry[] selections = actionHandler.getSelectedProductEntries();
        pm.beginTask("Downloading...", selections.length);
        try {
            final OpenData openData = new OpenData(COPERNICUS_HOST, COPERNICUS_ODATA_ROOT);

            for(ProductEntry entry : selections) {
                if (pm.isCanceled()) {
                    SystemUtils.LOG.info("DownloadActionExt: Download is cancelled");
                    break;
                }
                OpenData.Entry oData = openData.getEntryByID(entry.getRefID());
                SystemUtils.LOG.info(oData.fileName);

                openData.getProduct(entry.getRefID(), oData, outputFolder, new SubProgressMonitor(pm, 1));
            }

            for (ActionExtListener listener : listenerList) {
                listener.notifyMSG(this, ActionExtListener.MSG.NEW_REPO);
            }

        } catch (Exception e) {
            Dialogs.showError("unable to download " + e.getMessage());
        } finally {
            pm.done();
        }
    }

    public File getNewRepoFolder() {
        return outputFolder;
    }
}
