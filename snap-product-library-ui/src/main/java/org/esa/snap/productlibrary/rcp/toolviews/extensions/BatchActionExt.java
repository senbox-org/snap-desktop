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
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.esa.snap.graphbuilder.rcp.dialogs.BatchGraphDialog;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryToolView;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Export a list of files to text file
 */
public class BatchActionExt implements ProductLibraryActionExt {

    private static final ImageIcon batchIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/batch24.png", ProductLibraryToolView.class);
    private JButton button = null;
    private ProductLibraryActions actionHandler;

    public void setActionHandler(final ProductLibraryActions actionHandler) {
        this.actionHandler = actionHandler;
    }

    public JButton getButton(final JPanel panel) {
        if (button == null) {
            button = DialogUtils.createButton("batchProcessButton", "Batch", batchIcon, panel, DialogUtils.ButtonStyle.Icon);
            button.setToolTipText("Right click to select a graph");
            button.setComponentPopupMenu(createGraphPopup());
        }
        return button;
    }

    private JPopupMenu createGraphPopup() {
        final Path graphPath = ResourceUtils.getGraphFolder("");

        final JPopupMenu popup = new JPopupMenu();
        if (Files.exists(graphPath)) {
            createGraphMenu(popup, graphPath.toFile());
        }
        return popup;
    }

    private void createGraphMenu(final JPopupMenu menu, final File path) {
        final File[] filesList = path.listFiles();
        if (filesList == null || filesList.length == 0) return;

        for (final File file : filesList) {
            final String name = file.getName();
            if (file.isDirectory() && !file.isHidden() && !name.equalsIgnoreCase("internal")) {
                final JMenu subMenu = new JMenu(name);
                menu.add(subMenu);
                createGraphMenu(subMenu, file);
            } else if (name.toLowerCase().endsWith(".xml")) {
                final JMenuItem item = new JMenuItem(name.substring(0, name.indexOf(".xml")));
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        if (button.isEnabled()) {
                            batchProcess(actionHandler.getToolView().getSelectedProductEntries(), file);
                        }
                    }
                });
                menu.add(item);
            }
        }
    }

    private void createGraphMenu(final JMenu menu, final File path) {
        final File[] filesList = path.listFiles();
        if (filesList == null || filesList.length == 0) return;

        for (final File file : filesList) {
            final String name = file.getName();
            if (file.isDirectory() && !file.isHidden() && !name.equalsIgnoreCase("internal")) {
                final JMenu subMenu = new JMenu(name);
                menu.add(subMenu);
                createGraphMenu(subMenu, file);
            } else if (name.toLowerCase().endsWith(".xml")) {
                final JMenuItem item = new JMenuItem(name.substring(0, name.indexOf(".xml")));
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        if (button.isEnabled()) {
                            batchProcess(actionHandler.getToolView().getSelectedProductEntries(), file);
                        }
                    }
                });
                menu.add(item);
            }
        }
    }

    public void selectionChanged(final ProductEntry[] selections) {
        button.setEnabled(selections.length > 0 && ProductLibraryActions.allProductsExist(selections));
    }

    public void performAction(final ProgressMonitor pm) {
        batchProcess(actionHandler.getToolView().getSelectedProductEntries(), null);
    }

    private void batchProcess(final ProductEntry[] productEntryList, final File graphFile) {
        final BatchGraphDialog batchDlg = new BatchGraphDialog(SnapApp.getDefault().getAppContext(),
                "Batch Processing", "batchProcessing", false);
        batchDlg.setInputFiles(productEntryList);
        if (graphFile != null) {
            batchDlg.LoadGraph(graphFile);
        }
        batchDlg.show();
    }
}
