/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.rcp.dialogs.support;

import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.engine_utilities.util.ProductFunctions;
import org.esa.snap.graphbuilder.rcp.utils.ClipboardUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.nodes.PNode;
import org.esa.snap.rcp.util.Dialogs;
import org.openide.util.datatransfer.MultiTransferObject;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Table for listing product files
 */
public class FileTable extends JTable {

    private final FileTableModel fileModel;

    public FileTable() {
        this(new FileModel());
    }

    public FileTable(FileTableModel fileModel) {
        this(fileModel, new Dimension(500, 100));
    }

    public FileTable(FileTableModel fileModel, Dimension dim) {
        if (fileModel == null) {
            fileModel = new FileModel();
        }
        this.fileModel = fileModel;
        this.setModel(fileModel);

        setPreferredScrollableViewportSize(dim);
        fileModel.setColumnWidths(getColumnModel());
        setColumnSelectionAllowed(true);
        setDropMode(DropMode.ON);
        setDragEnabled(true);
        setComponentPopupMenu(createTablePopup());
        setTransferHandler(new ProductSetTransferHandler(fileModel));
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        String tip = null;
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);

        try {
            tip = getValueAt(rowIndex, colIndex).toString();
        } catch (RuntimeException e1) {
            //catch null pointer exception if mouse is over an empty line
        }
        return tip;
    }

    public void setFiles(final File[] fileList) {
        if (fileList != null) {
            fileModel.clear();
            for (File file : fileList) {
                fileModel.addFile(file);
            }
        }
    }

    public void setFiles(final String[] fileList) {
        if (fileList != null) {
            fileModel.clear();
            for (String str : fileList) {
                fileModel.addFile(new File(str));
            }
        }
    }

    public void setProductEntries(final ProductEntry[] productEntryList) {
        if (productEntryList != null) {
            fileModel.clear();
            for (ProductEntry entry : productEntryList) {
                fileModel.addFile(entry);
            }
        }
    }

    public int getFileCount() {
        int cnt = fileModel.getRowCount();
        if (cnt == 1) {
            File file = fileModel.getFileAt(0);
            if (file != null && file.getName().isEmpty())
                return 0;
        }
        return cnt;
    }

    public File[] getFileList() {
        return fileModel.getFileList();
    }

    public FileTableModel getModel() {
        return fileModel;
    }

    private JPopupMenu createTablePopup() {
        final JPopupMenu popup = new JPopupMenu();
        final JMenuItem pastelItem = new JMenuItem("Paste");
        pastelItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                paste();
            }
        });
        popup.add(pastelItem);

        return popup;
    }

    private void paste() {
        try {
            final File[] fileList = ClipboardUtils.getClipboardFileList();
            if (fileList != null) {
                setFiles(fileList);
            }
        } catch (Exception e) {
            if (SnapApp.getDefault() != null) {
                Dialogs.showError("Unable to paste from clipboard: " + e.getMessage());
            }
        }
    }

    public static class ProductSetTransferHandler extends TransferHandler {

        private final FileTableModel fileModel;

        ProductSetTransferHandler(FileTableModel model) {
            fileModel = model;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            if(info.isDataFlavorSupported(DataFlavor.stringFlavor))
                return true;
            try {
                if(info.getDataFlavors().length > 0) {

                    Object transferData = info.getTransferable().getTransferData(info.getDataFlavors()[0]);
                    if (transferData instanceof PNode) {
                        return true;
                    } else if(transferData instanceof MultiTransferObject) {
                        final MultiTransferObject multi = (MultiTransferObject) transferData;
                        boolean allPNode = true;
                        DataFlavor dataFlavor = multi.getTransferDataFlavors(0)[0];
                        for(int i=0; i < multi.getCount(); ++i) {
                            Object data = multi.getTransferData(i, dataFlavor);
                            if(!(data instanceof PNode)) {
                                allPNode = false;
                                break;
                            }
                        }
                        return allPNode;
                    }
                }
            } catch (Exception e) {
                return false;
            }
            return false;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY;
        }

        /**
         * Perform the actual import
         */
        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            // Get the string that is being dropped.
            final Transferable t = info.getTransferable();
            try {
                if (info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String data = (String) t.getTransferData(DataFlavor.stringFlavor);

                    // Wherever there is a newline in the incoming data,
                    // break it into a separate item in the list.
                    final String[] values = data.split("\n");
                    
                    for (String value : values) {
                        final File file = new File(value);
                        if (file.exists()) {
                            if (ProductFunctions.isValidProduct(file)) {
                                fileModel.addFile(file);
                            }
                        }
                    }
                } else {

                    Object transferData = t.getTransferData(info.getDataFlavors()[0]);
                    if (transferData instanceof PNode) {
                        final PNode node = (PNode)transferData;
                        File file = node.getProduct().getFileLocation();
                        if (file.exists()) {
                            fileModel.addFile(file);
                        }
                    } else if(transferData instanceof MultiTransferObject) {
                        final MultiTransferObject multi = (MultiTransferObject) transferData;
                        DataFlavor dataFlavor = multi.getTransferDataFlavors(0)[0];
                        for(int i=0; i < multi.getCount(); ++i) {
                            Object data = multi.getTransferData(i, dataFlavor);
                            if(data instanceof PNode) {
                                final PNode node = (PNode)data;
                                File file = node.getProduct().getFileLocation();
                                if (file.exists()) {
                                    fileModel.addFile(file);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                return false;
            }

            return true;
        }

        // export
        @Override
        protected Transferable createTransferable(JComponent c) {
            final JTable table = (JTable) c;
            final int[] rows = table.getSelectedRows();

            final StringBuilder listStr = new StringBuilder(256);
            for (int row : rows) {
                final File file = fileModel.getFileAt(row);
                listStr.append(file.getAbsolutePath());
                listStr.append('\n');
            }
            if (rows.length != 0) {
                return new StringSelection(listStr.toString());
            }
            return null;
        }
    }
}
