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

import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.graphbuilder.rcp.dialogs.BatchGraphDialog;
import org.esa.snap.graphbuilder.rcp.utils.ClipboardUtils;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.extensions.ProductLibraryActionExt;
import org.esa.snap.productlibrary.rcp.toolviews.extensions.ProductLibraryActionExtDescriptor;
import org.esa.snap.productlibrary.rcp.toolviews.extensions.ProductLibraryActionExtRegistry;
import org.esa.snap.productlibrary.rcp.toolviews.model.SortingDecorator;
import org.esa.snap.productlibrary.rcp.utils.ProductOpener;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.engine_utilities.util.ResourceUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * actions on product entry selections
 */
public class ProductLibraryActions {

    private static final ImageIcon selectAllIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/select-all24.png", ProductLibraryToolView.class);
    private static final ImageIcon openIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/open24.png", ProductLibraryToolView.class);
    private static final ImageIcon copyIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/copy24.png", ProductLibraryToolView.class);
    private static final ImageIcon batchIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/batch24.png", ProductLibraryToolView.class);

    private final JTable productEntryTable;
    private final ProductLibraryToolView toolView;
    private final ProductOpener openHandler;
    private JButton selectAllButton, openAllSelectedButton, copySelectedButton, batchProcessButton;

    private List<ProductLibraryActionExt> actionExtList = new ArrayList<>();

    private JMenuItem copyToItem,  moveToItem, deleteItem;

    private File currentDirectory;
    private final java.util.List<ProductLibraryActionListener> listenerList = new ArrayList<>(1);

    public ProductLibraryActions(final JTable productEntryTable, final ProductLibraryToolView toolView) {
        this.productEntryTable = productEntryTable;
        this.toolView = toolView;
        this.openHandler = new ProductOpener();
    }

    public JPanel createCommandPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        selectAllButton = DialogUtils.createButton("selectAllButton", "Select all", selectAllIcon, panel, DialogUtils.ButtonStyle.Icon);
        selectAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performSelectAllAction();
            }
        });

        openAllSelectedButton = DialogUtils.createButton("openAllSelectedButton", "Open selected", openIcon, panel, DialogUtils.ButtonStyle.Icon);
        openAllSelectedButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performOpenAction();
            }
        });

        copySelectedButton = DialogUtils.createButton("copySelectedButton", "Copy to clipboard", copyIcon, panel, DialogUtils.ButtonStyle.Icon);
        copySelectedButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performCopyAction();
            }
        });

        batchProcessButton = DialogUtils.createButton("batchProcessButton", "Batch", batchIcon, panel, DialogUtils.ButtonStyle.Icon);
        batchProcessButton.setToolTipText("Right click to select a graph");
        batchProcessButton.setComponentPopupMenu(createGraphPopup());
        batchProcessButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                batchProcess(getSelectedProductEntries(), null);
            }
        });

        panel.add(selectAllButton);
        panel.add(openAllSelectedButton);
        panel.add(copySelectedButton);
        panel.add(batchProcessButton);
        panel.add(Box.createRigidArea(new Dimension(24,24))); // separator

        for(ProductLibraryActionExtDescriptor desc : ProductLibraryActionExtRegistry.getInstance().getDescriptors()) {
            final ProductLibraryActionExt action = desc.createActionExt(this);
            actionExtList.add(action);

            final JButton button = action.getButton(panel);
            panel.add(button);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    action.performAction();
                }
            });
        }

        return panel;
    }

    private static void batchProcess(final ProductEntry[] productEntryList, final File graphFile) {
        final BatchGraphDialog batchDlg = new BatchGraphDialog(SnapApp.getDefault().getAppContext(),
                "Batch Processing", "batchProcessing", false);
        batchDlg.setInputFiles(productEntryList);
        if (graphFile != null) {
            batchDlg.LoadGraph(graphFile);
        }
        batchDlg.show();
    }

    public void selectionChanged(final ProductEntry[] selections) {
        final boolean enable = selections.length > 0;

        openAllSelectedButton.setEnabled(enable);
        copySelectedButton.setEnabled(enable);
        batchProcessButton.setEnabled(enable);

        for(ProductLibraryActionExt action : actionExtList) {
            action.selectionChanged(selections);
        }
    }

    public ProductEntry[] getSelectedProductEntries() {
        final int[] selectedRows = productEntryTable.getSelectedRows();
        final ProductEntry[] selectedEntries = new ProductEntry[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            final Object entry = productEntryTable.getValueAt(selectedRows[i], 0);
            if (entry instanceof ProductEntry) {
                selectedEntries[i] = (ProductEntry) entry;
            }
        }
        return selectedEntries;
    }

    public File[] getSelectedFiles() {
        final int[] selectedRows = productEntryTable.getSelectedRows();
        final File[] selectedFiles = new File[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            final Object entry = productEntryTable.getValueAt(selectedRows[i], 0);
            if (entry instanceof ProductEntry) {
                selectedFiles[i] = ((ProductEntry) entry).getFile();
            }
        }
        return selectedFiles;
    }

    private void performSelectAllAction() {
        productEntryTable.selectAll();
        notifySelectionChanged();
    }

    private void performSelectNoneAction() {
        productEntryTable.clearSelection();
        notifySelectionChanged();
    }

    /**
     * Copy the selected file list to the clipboard
     */
    private void performCopyAction() {
        final File[] fileList = getSelectedFiles();
        if (fileList.length != 0)
            ClipboardUtils.copyToClipboard(fileList);
    }

    private void performFileAction(final ProductFileHandler.TYPE operationType) {
        final File targetFolder;
        if(operationType.equals(ProductFileHandler.TYPE.DELETE)) {
            targetFolder = null;
        } else {
            targetFolder = promptForRepositoryBaseDir();
            if (targetFolder == null) return;
        }

        final ProductEntry[] entries = getSelectedProductEntries();
        final LabelBarProgressMonitor progMon = toolView.createLabelBarProgressMonitor();

        final ProductFileHandler fileHandler = new ProductFileHandler(entries, operationType, targetFolder, progMon);
        fileHandler.addListener(new MyFileHandlerListener());
        fileHandler.execute();
    }

    public void performOpenAction() {
        if (openHandler != null) {
            openHandler.openProducts(getSelectedFiles());
        }
    }

    public void findSlices(final int dataTakeId) {
        toolView.findSlices(dataTakeId);
    }

    public File promptForRepositoryBaseDir() {
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
                performSelectAllAction();
            }
        });
        popup.add(selectAllItem);
        final JMenuItem selectNoneItem = new JMenuItem("Select None");
        selectNoneItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performSelectNoneAction();
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
                final SnapDialogs.Answer status = SnapDialogs.requestDecision("Deleting selected files",
                                                                              "Are you sure you want to delete these products",
                                                                              true, null);
                if (status == SnapDialogs.Answer.YES)
                    performFileAction(ProductFileHandler.TYPE.DELETE);
            }
        });
        popup.add(deleteItem);

        final JMenuItem exploreItem = new JMenuItem("Browse Folder");
        exploreItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Point pos = productEntryTable.getMousePosition();
                int row = 0;
                if (pos != null)
                    row = productEntryTable.rowAtPoint(pos);
                final Object entry = productEntryTable.getValueAt(row, 0);
                if (entry != null && entry instanceof ProductEntry) {
                    final ProductEntry prodEntry = (ProductEntry) entry;
                    try {
                        Desktop.getDesktop().open(prodEntry.getFile().getParentFile());
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
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
                final TableModel model = productEntryTable.getModel();
                if(model instanceof SortingDecorator) {
                    SortingDecorator sortedModel = (SortingDecorator) model;
                    sortedModel.sortBy(sortBy);
                }
            }
        });
        return item;
    }

    public JPopupMenu createGraphPopup() {
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
                        //todo
                        if (batchProcessButton.isEnabled())
                            batchProcess(getSelectedProductEntries(), file);
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
                        //todo
                        if (batchProcessButton.isEnabled())
                            batchProcess(getSelectedProductEntries(), file);
                    }
                });
                menu.add(item);
            }
        }
    }

    public void updateContextMenu(final ProductEntry[] selections) {
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

    public void addListener(final ProductLibraryActionListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    private void notifyDirectoryChanged() {
        for(ProductLibraryActionListener listener : listenerList) {
            listener.notifyDirectoryChanged();
        }
    }

    private void notifySelectionChanged() {
        for(ProductLibraryActionListener listener : listenerList) {
            listener.notifySelectionChanged();
        }
    }

    public interface ProductLibraryActionListener {

        void notifyDirectoryChanged();
        void notifySelectionChanged();
    }

    private class MyFileHandlerListener implements ProductFileHandler.ProductFileHandlerListener {

        public void notifyMSG(final ProductFileHandler fileHandler, final MSG msg) {
            if (msg.equals(ProductFileHandler.ProductFileHandlerListener.MSG.DONE)) {
                final java.util.List<DBScanner.ErrorFile> errorList = fileHandler.getErrorList();
                if (!errorList.isEmpty()) {
                    toolView.handleErrorList(errorList);
                }
                if(fileHandler.getOperationType().equals(ProductFileHandler.TYPE.MOVE_TO) ||
                        fileHandler.getOperationType().equals(ProductFileHandler.TYPE.DELETE)) {
                    notifyDirectoryChanged();
                }
            }
            toolView.UpdateUI();
        }
    }
}
