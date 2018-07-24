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

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.SnapFileChooser;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.prefs.Preferences;

/**
 * Export a list of files to text file
 */
public class ExportListActionExt implements ProductLibraryActionExt {

    private static final ImageIcon exportListIcon = TangoIcons.actions_document_save_as(TangoIcons.Res.R22);
    private JButton button = null;
    private ProductLibraryActions actionHandler;

    private static final String lastExportDirPreferenceKey = "snap.productlibrary.last_export_dir";

    public void setActionHandler(final ProductLibraryActions actionHandler) {
        this.actionHandler = actionHandler;
    }

    public JButton getButton(final JPanel panel) {
        if (button == null) {
            button = DialogUtils.createButton("exportListButton", "Export list of selected products", exportListIcon, panel, DialogUtils.ButtonStyle.Icon);
        }
        return button;
    }

    public void selectionChanged(final ProductEntry[] selections) {
        button.setEnabled(selections.length > 0 && ProductLibraryActions.allProductsExist(selections));
    }

    public void performAction(final com.bc.ceres.core.ProgressMonitor pm) {
        final File file = getExportFile();
        if (file != null) {
            final File[] fileList = actionHandler.getSelectedFiles();
            if (fileList.length != 0) {
                exportFileList(file, fileList);

                viewFile(file);
            }
        }
    }

    private static File getExportFile() {
        final Preferences preferences = SnapApp.getDefault().getPreferences();
        final File currentDir = new File(preferences.get(lastExportDirPreferenceKey, SystemUtils.getUserHomeDir().getPath()));

        final SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setDialogTitle("Export list of products");
        fileChooser.setCurrentDirectory(currentDir);
        final int result = fileChooser.showSaveDialog(SnapApp.getDefault().getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            if (file != null) {
                final File parentFolder = file.getAbsoluteFile().getParentFile();
                if (parentFolder != null) {
                    preferences.put(lastExportDirPreferenceKey, parentFolder.getPath());
                }

                return file;
            }
        }
        return null;
    }

    private static void exportFileList(final File file, final File[] fileList) {
        PrintStream p = null;
        try (FileOutputStream out = new FileOutputStream(file.getAbsolutePath(), false)) {
            p = new PrintStream(out);

            for (File f : fileList) {
                p.println(f.getAbsolutePath());
            }

        } catch (IOException e) {
            SnapApp.getDefault().handleError("Unable to export product list", e);
        } finally {
            if (p != null)
                p.close();
        }
    }

    private static void viewFile(final File file) {
        if (Desktop.isDesktopSupported() && file.exists()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (Exception e) {
                SnapApp.getDefault().handleError("Error opening file " + e.getMessage(), e);
            }
        }
    }
}
