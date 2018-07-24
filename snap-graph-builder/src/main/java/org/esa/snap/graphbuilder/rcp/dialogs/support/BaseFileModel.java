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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.productlibrary.db.ProductDB;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseFileModel extends AbstractTableModel implements FileTableModel {

    protected String titles[] = null;
    protected Class types[] = null;
    protected int widths[] = null;

    protected final List<File> fileList = new ArrayList<>(10);
    protected final List<TableData> dataList = new ArrayList<>(10);

    public BaseFileModel() {
        setColumnData();
        addBlankFile();
    }

    protected abstract void setColumnData();

    protected abstract TableData createFileStats(final File file);

    protected abstract TableData createFileStats(final ProductEntry entry);

    public File[] getFileList() {
        return fileList.toArray(new File[fileList.size()]);
    }

    private static ProductEntry getProductEntry(final File file) {
        try {
            return ProductDB.instance().getProductEntry(file);
        } catch (Exception e) {
            if (SnapApp.getDefault() != null) {
                Dialogs.showError("Error getting product entry: " + e.getMessage());
            }
        }
        return null;
    }

    public void addFile(final File file) {
        fileList.add(file);
        clearBlankFile();

        // check if already exists in db
        final ProductEntry existingEntry = file.getName().isEmpty() ? null : getProductEntry(file);
        if (existingEntry != null) {
            dataList.add(createFileStats(existingEntry));
        } else {
            dataList.add(createFileStats(file));
        }
        fireTableDataChanged();
    }

    public void addFile(final ProductEntry entry) {
        fileList.add(entry.getFile());
        clearBlankFile();

        dataList.add(createFileStats(entry));
        fireTableDataChanged();
    }

    public void addFile(final File file, final String[] values) {
        fileList.add(file);
        clearBlankFile();

        dataList.add(new TableData(values));

        fireTableDataChanged();
    }

    public void removeFile(final int index) {
        fileList.remove(index);
        dataList.remove(index);

        fireTableDataChanged();
    }

    public void move(final int oldIndex, final int newIndex) {
        if ((oldIndex < 1 && oldIndex > newIndex) || oldIndex > fileList.size() ||
                newIndex < 0 || newIndex >= fileList.size())
            return;
        final File file = fileList.get(oldIndex);
        final TableData data = dataList.get(oldIndex);

        fileList.remove(oldIndex);
        dataList.remove(oldIndex);
        fileList.add(newIndex, file);
        dataList.add(newIndex, data);

        fireTableDataChanged();
    }

    public int getIndexOf(final File file) {
        return fileList.indexOf(file);
    }

    /**
     * Needed for drag and drop
     */
    private void addBlankFile() {
        addFile(new File(""));
    }

    protected void clearBlankFile() {
        if (fileList.size() > 1 && fileList.get(0).getName().isEmpty()) {
            removeFile(0);
        }
    }

    public void refresh() {
        for(TableData data : dataList) {
            data.refresh();
        }
    }

    public void clear() {
        fileList.clear();
        dataList.clear();
        addBlankFile();

        fireTableDataChanged();
    }

    // Implement the methods of the TableModel interface we're interested
    // in.  Only getRowCount(), getColumnCount() and getValueAt() are
    // required.  The other methods tailor the look of the table.
    public int getRowCount() {
        return dataList.size();
    }

    public int getColumnCount() {
        return titles.length;
    }

    @Override
    public String getColumnName(final int c) {
        return titles[c];
    }

    @Override
    public Class getColumnClass(final int c) {
        return types[c];
    }

    public Object getValueAt(final int r, final int c) {
        return dataList.get(r).data[c];
    }

    public File getFileAt(final int index) {
        return fileList.get(index);
    }

    public File[] getFilesAt(final int[] indices) {
        final List<File> files = new ArrayList<>(indices.length);
        for (int i : indices) {
            files.add(fileList.get(i));
        }
        return files.toArray(new File[files.size()]);
    }

    public void setColumnWidths(final TableColumnModel columnModel) {
        for (int i = 0; i < widths.length; ++i) {
            columnModel.getColumn(i).setMinWidth(widths[i]);
            columnModel.getColumn(i).setPreferredWidth(widths[i]);
            columnModel.getColumn(i).setWidth(widths[i]);
        }
    }

    public class TableData {
        protected final String data[] = new String[titles.length];
        protected final File file;
        protected final ProductEntry entry;

        public TableData(final File file) {
            this.file = file;
            this.entry = null;
            updateData();
        }

        public TableData(final ProductEntry entry) {
            this.file = null;
            this.entry = entry;
            updateData();
        }

        public TableData(final String[] values) {
            System.arraycopy(values, 0, data, 0, data.length);
            this.entry = null;
            this.file = null;
        }

        public void refresh() {
            readProduct(file);
        }

        protected void updateData() {
        }

        protected void updateData(final Product product) {
        }

        private void readProduct(final File file) {

            if(file == null)
                return;

            final SwingWorker worker = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    try {
                        if (!file.getName().isEmpty()) {
                            try {
                                Product product = getProductFromProductManager(file);
                                if (product == null) {
                                    product = CommonReaders.readProduct(file);
                                }
                                updateData(product);
                            } catch (Exception ex) {
                                updateData();
                            }
                        }
                    } finally {
                        fireTableDataChanged();
                    }
                    return null;
                }
            };
            worker.execute();
        }

        protected Product getProductFromProductManager(final File file) {
            final SnapApp app = SnapApp.getDefault();
            if(app != null) {
                final Product[] products = app.getProductManager().getProducts();
                for(Product p : products) {
                    if(file.equals(p.getFileLocation())) {
                        return p;
                    }
                }
            }
            return null;
        }
    }

}