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

import org.esa.snap.engine_utilities.util.ProductFunctions;
import org.esa.snap.graphbuilder.rcp.utils.ClipboardUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.nodes.PNode;
import org.esa.snap.rcp.util.Dialogs;
import org.openide.util.datatransfer.MultiTransferObject;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public void setFiles(final Path[] fileList) {
        if (fileList != null) {
            fileModel.clear();
            for (Path file : fileList) {
                fileModel.addFile(file);
            }
        }
    }

    public void setFiles(final String[] fileList) {
        if (fileList != null) {
            fileModel.clear();
            for (String str : fileList) {
                fileModel.addFile(Path.of(str));
            }
        }
    }

    public int getFileCount() {
        int cnt = fileModel.getRowCount();
        if (cnt == 1) {
            Path file = fileModel.getFileAt(0);
            if (file != null && file.getFileName().toString().isEmpty())
                return 0;
        }
        return cnt;
    }

    public Path[] getFileList() {
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
            final Path[] fileList = ClipboardUtils.getClipboardFileList();
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
                        final Path file = Path.of(value);
                        if (Files.exists(file)) {
                            if (ProductFunctions.isValidProduct(file)) {
                                fileModel.addFile(file);
                            }
                        }
                    }
                } else {

                    Object transferData = t.getTransferData(info.getDataFlavors()[0]);
                    if (transferData instanceof PNode) {
                        final PNode node = (PNode)transferData;
                        Path file = node.getProduct().getFileLocation();
                        if (Files.exists(file)) {
                            fileModel.addFile(file);
                        }
                    } else if(transferData instanceof MultiTransferObject) {
                        final MultiTransferObject multi = (MultiTransferObject) transferData;
                        DataFlavor dataFlavor = multi.getTransferDataFlavors(0)[0];
                        for(int i=0; i < multi.getCount(); ++i) {
                            Object data = multi.getTransferData(i, dataFlavor);
                            if(data instanceof PNode) {
                                final PNode node = (PNode)data;
                                Path file = node.getProduct().getFileLocation();
                                if (Files.exists(file)) {
                                    fileModel.addFile(file);
                                }
                            }
                        }
                    }
                }

                if(fileModel.getRowCount() < 100) {
                    fileModel.refresh();
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
                final Path file = fileModel.getFileAt(row);
                listStr.append(file.toAbsolutePath());
                listStr.append('\n');
            }
            if (rows.length != 0) {
                return new StringSelection(listStr.toString());
            }
            return null;
        }
    }
}
