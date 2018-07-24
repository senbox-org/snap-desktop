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
package org.esa.snap.productlibrary.rcp.toolviews;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.graphbuilder.rcp.progress.LabelBarProgressMonitor;
import org.esa.snap.graphbuilder.rcp.utils.ClipboardUtils;
import org.esa.snap.productlibrary.rcp.toolviews.extensions.ProductLibraryActionExt;
import org.esa.snap.productlibrary.rcp.toolviews.extensions.ProductLibraryActionExtDescriptor;
import org.esa.snap.productlibrary.rcp.toolviews.extensions.ProductLibraryActionExtRegistry;
import org.esa.snap.productlibrary.rcp.toolviews.support.SortingDecorator;
import org.esa.snap.productlibrary.rcp.utils.ProductOpener;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.SnapFileChooser;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * actions on product entry selections
 */
public class ProductLibraryActions {

    private final ProductLibraryToolView toolView;
    private List<ProductLibraryActionExt> actionExtList = new ArrayList<>();

    private JMenuItem copyToItem, moveToItem, deleteItem;

    private File currentDirectory;

    public ProductLibraryActions(final ProductLibraryToolView toolView) {
        this.toolView = toolView;
    }

    public ProductLibraryToolView getToolView() {
        return this.toolView;
    }

    JPanel createCommandPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        for (ProductLibraryActionExtDescriptor desc : ProductLibraryActionExtRegistry.getInstance().getDescriptors()) {
            if(desc.isSeperator()) {
                panel.add(Box.createRigidArea(new Dimension(24, 24))); // separator
            } else {
                final ProductLibraryActionExt action = desc.createActionExt(this);
                actionExtList.add(action);

                final JButton button = action.getButton(panel);
                panel.add(button);
                button.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {

                        final DBWorker worker = new DBWorker(DBWorker.TYPE.EXECUTEACTION, action,
                                toolView.getLabelBarProgressMonitor());
                        worker.addListener(toolView.createDBListener());
                        worker.execute();
                    }
                });
            }
        }

        return panel;
    }

    void selectionChanged(final ProductEntry[] selections) {
        for (ProductLibraryActionExt action : actionExtList) {
            action.selectionChanged(selections);
        }
    }

    public static boolean allProductsExist(final ProductEntry[] selections) {
        boolean allProductsExits = true;
        for(ProductEntry entry : selections) {
            if(entry.getFile() == null || !entry.getFile().exists()) {
                allProductsExits = false;
                break;
            }
        }
        return allProductsExits;
    }

    /**
     * Copy the selected file list to the clipboard
     */
    private void performCopyAction() {
        final File[] fileList = toolView.getSelectedFiles();
        if (fileList.length != 0) {
            ClipboardUtils.copyToClipboard(fileList);
        }
    }

    private void performFileAction(final ProductFileHandler.TYPE operationType) {
        final File targetFolder;
        if (operationType.equals(ProductFileHandler.TYPE.DELETE)) {
            targetFolder = null;
        } else {
            targetFolder = promptForRepositoryBaseDir();
            if (targetFolder == null) return;
        }

        final ProductEntry[] entries = toolView.getSelectedProductEntries();
        final LabelBarProgressMonitor progMon = toolView.getLabelBarProgressMonitor();

        final ProductFileHandler fileHandler = new ProductFileHandler(entries, operationType, targetFolder, progMon);
        fileHandler.addListener(toolView.createProductFileHandlerListener());
        fileHandler.execute();
    }

    public File[] getSelectedFiles() {
        return toolView.getSelectedFiles();
    }

    public ProductEntry[] getSelectedProductEntries() {
        return toolView.getSelectedProductEntries();
    }

    public void performOpenAction() {
        ProductOpener.openProducts(getSelectedFiles());
    }

    File promptForRepositoryBaseDir() {
        final JFileChooser fileChooser = createDirectoryChooser();
        fileChooser.setCurrentDirectory(currentDirectory);
        final int response = fileChooser.showOpenDialog(SnapApp.getDefault().getMainFrame());
        currentDirectory = fileChooser.getCurrentDirectory();
        File selectedDir = fileChooser.getSelectedFile();
        if (selectedDir != null && selectedDir.isFile())
            selectedDir = selectedDir.getParentFile();
        if (response == JFileChooser.APPROVE_OPTION) {
            return selectedDir;
        }
        return null;
    }

    private static JFileChooser createDirectoryChooser() {
        final JFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(final File f) {
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Directories"; /* I18N */
            }
        });
        fileChooser.setDialogTitle("Select Directory"); /* I18N */
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setApproveButtonText("Select"); /* I18N */
        fileChooser.setApproveButtonMnemonic('S');
        return fileChooser;
    }

    // Context Menu

    public JPopupMenu createEntryTablePopup() {
        final JPopupMenu popup = new JPopupMenu();

        final JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                toolView.selectAll();
            }
        });
        popup.add(selectAllItem);
        final JMenuItem selectNoneItem = new JMenuItem("Select None");
        selectNoneItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                toolView.selectNone();
            }
        });
        popup.add(selectNoneItem);

        final JMenuItem openSelectedItem = new JMenuItem("Open Selected");
        openSelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performOpenAction();
            }
        });
        popup.add(openSelectedItem);

        final JMenuItem copySelectedItem = new JMenuItem("Copy Selected");
        copySelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performCopyAction();
            }
        });
        popup.add(copySelectedItem);

        popup.addSeparator();

        copyToItem = new JMenuItem("Copy Selected Files To...");
        copyToItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performFileAction(ProductFileHandler.TYPE.COPY_TO);
            }
        });
        popup.add(copyToItem);

        moveToItem = new JMenuItem("Move Selected Files To...");
        moveToItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performFileAction(ProductFileHandler.TYPE.MOVE_TO);
            }
        });
        popup.add(moveToItem);

        deleteItem = new JMenuItem("Delete Selected Files");
        deleteItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Dialogs.Answer status = Dialogs.requestDecision("Deleting selected files",
                        "Are you sure you want to delete these products",
                        true, null);
                if (status == Dialogs.Answer.YES)
                    performFileAction(ProductFileHandler.TYPE.DELETE);
            }
        });
        popup.add(deleteItem);

        final JMenuItem exploreItem = new JMenuItem("Browse Folder");
        exploreItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final ProductEntry entry = toolView.getEntryOverMouse();
                if (entry != null && entry instanceof ProductEntry) {
                    final ProductEntry prodEntry = entry;
                    if(prodEntry.getFile() != null) {
                        try {
                            Desktop.getDesktop().open(prodEntry.getFile().getParentFile());
                        } catch (Exception ex) {
                            SystemUtils.LOG.severe(ex.getMessage());
                        }
                    }
                }
            }
        });
        popup.add(exploreItem);

        popup.addSeparator();

        final JMenu sortMenu = new JMenu("Sort By");
        popup.add(sortMenu);

        sortMenu.add(createSortItem("Product Name", SortingDecorator.SORT_BY.NAME));
        sortMenu.add(createSortItem("Product Type", SortingDecorator.SORT_BY.TYPE));
        sortMenu.add(createSortItem("Acquisition Date", SortingDecorator.SORT_BY.DATE));
        sortMenu.add(createSortItem("Mission", SortingDecorator.SORT_BY.MISSON));
        sortMenu.add(createSortItem("File Size", SortingDecorator.SORT_BY.FILESIZE));

        return popup;
    }

    private JMenuItem createSortItem(final String name, final SortingDecorator.SORT_BY sortBy) {
        final JMenuItem item = new JMenuItem(name);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                toolView.sort(sortBy);
            }
        });
        return item;
    }

    void updateContextMenu(final ProductEntry[] selections) {
        boolean allValid = true;
        for (ProductEntry entry : selections) {
            if (!ProductFileHandler.canMove(entry)) {
                allValid = false;
                break;
            }
        }
        copyToItem.setEnabled(allValid);
        moveToItem.setEnabled(allValid);
        deleteItem.setEnabled(allValid);
    }
}
