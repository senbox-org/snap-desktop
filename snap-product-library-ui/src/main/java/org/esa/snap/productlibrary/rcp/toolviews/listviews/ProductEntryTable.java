/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews.listviews;

import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;
import org.esa.snap.productlibrary.rcp.toolviews.model.ProductEntryTableModel;
import org.esa.snap.productlibrary.rcp.toolviews.support.SortingDecorator;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Created by luis on 29/01/2016.
 */
public class ProductEntryTable extends JTable implements ListView {

    public ProductEntryTable(final ProductLibraryActions productLibraryActions) {
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setComponentPopupMenu(productLibraryActions.createEntryTablePopup());

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                final int clickCount = e.getClickCount();
                if (clickCount == 2) {
                    productLibraryActions.performOpenAction();
                } else if (clickCount == 1) {
                    notifySelectionChanged();
                }
            }
        });
    }

    public void setProductEntryList(final ProductEntry[] productEntryList) {
        final ProductEntryTableModel tableModel = new ProductEntryTableModel(productEntryList, false);
        setModel(new SortingDecorator(tableModel, getTableHeader()));
        setColumnModel(tableModel.getColumnModel());
    }

    public File[] getSelectedFiles() {
        final int[] selectedRows = getSelectedRows();
        final File[] selectedFiles = new File[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            final Object entry = getValueAt(selectedRows[i], 0);
            if (entry instanceof ProductEntry) {
                File file = ((ProductEntry) entry).getFile();
                if(file != null) {
                    selectedFiles[i] = file;
                }
            }
        }
        return selectedFiles;
    }

    public ProductEntry[] getSelectedProductEntries() {
        final int[] selectedRows = getSelectedRows();
        final ProductEntry[] selectedEntries = new ProductEntry[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            final Object entry = getValueAt(selectedRows[i], 0);
            if (entry instanceof ProductEntry) {
                selectedEntries[i] = (ProductEntry) entry;
            }
        }
        return selectedEntries;
    }

    public ProductEntry getEntryOverMouse() {
        final Point pos = getMousePosition();
        int row = 0;
        if (pos != null)
            row = rowAtPoint(pos);
        return (ProductEntry) getValueAt(row, 0);
    }

    public void sort(final SortingDecorator.SORT_BY sortBy) {
        final TableModel model = getModel();
        if (model instanceof SortingDecorator) {
            SortingDecorator sortedModel = (SortingDecorator) model;
            sortedModel.sortBy(sortBy);
        }
    }

    public int getTotalCount() {
        return getRowCount();
    }

    public int getSelectionCount() {
        return getSelectedRowCount();
    }
}
