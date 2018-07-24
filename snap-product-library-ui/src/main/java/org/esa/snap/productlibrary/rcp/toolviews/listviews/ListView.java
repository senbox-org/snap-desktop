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
import org.esa.snap.productlibrary.rcp.toolviews.support.SortingDecorator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luis on 29/01/2016.
 */
public interface ListView {

    List<ListViewListener> listenerList = new ArrayList<>();

    File[] getSelectedFiles();

    ProductEntry[] getSelectedProductEntries();

    ProductEntry getEntryOverMouse();

    void sort(final SortingDecorator.SORT_BY sortBy);

    void selectAll();

    void clearSelection();

    int getTotalCount();

    int getSelectionCount();

    void updateUI();

    void setProductEntryList(final ProductEntry[] productEntryList);

    default void addListener(final ListViewListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    default void notifySelectionChanged() {
        for (final ListViewListener listener : listenerList) {
            listener.notifySelectionChanged();
        }
    }

    default void notifyOpenAction() {
        for (final ListViewListener listener : listenerList) {
            listener.notifyOpenAction();
        }
    }

    interface ListViewListener {
        void notifySelectionChanged();

        void notifyOpenAction();
    }
}
