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

import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.datamodel.quicklooks.Thumbnail;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;
import org.esa.snap.productlibrary.rcp.toolviews.support.SortingDecorator;
import org.esa.snap.rcp.quicklooks.ThumbnailPanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luis on 29/01/2016.
 */
public class ThumbnailView extends ThumbnailPanel implements ListView {

    private ProductEntry[] productEntryList;

    public ThumbnailView(final ProductLibraryActions productLibraryActions) {
        super(true);

        setComponentPopupMenu(productLibraryActions.createEntryTablePopup());
    }

    @Override
    public void onSelectionChanged() {
        notifySelectionChanged();
    }

    @Override
    public void onOpenAction() {
        notifyOpenAction();
    }

    public File[] getSelectedFiles() {
        final List<File> list = new ArrayList<>();
        for (ThumbnailDrawing item : getSelection()) {
            Quicklook ql = (Quicklook) item.getThumbnail();
            for (ProductEntry entry : productEntryList) {
                if (entry.getQuickLook().equals(ql) && entry.getFile() != null) {
                    list.add(entry.getFile());
                }
            }
        }
        return list.toArray(new File[list.size()]);
    }

    public ProductEntry[] getSelectedProductEntries() {
        final List<ProductEntry> list = new ArrayList<>();
        for (ThumbnailDrawing item : getSelection()) {
            Quicklook ql = (Quicklook) item.getThumbnail();
            for (ProductEntry entry : productEntryList) {
                if (entry.getQuickLook().equals(ql)) {
                    list.add(entry);
                }
            }
        }
        return list.toArray(new ProductEntry[list.size()]);
    }

    public ProductEntry getEntryOverMouse() {
        return null;
    }

    public void sort(final SortingDecorator.SORT_BY sortBy) {

    }

    public int getTotalCount() {
        return productEntryList.length;
    }

    public int getSelectionCount() {
        return getSelection().length;
    }

    public void updateUI() {

    }

    public void setProductEntryList(final ProductEntry[] productEntryList) {
        this.productEntryList = productEntryList;
        final List<Quicklook> thumbnails = new ArrayList<>(productEntryList.length);
        for (ProductEntry productEntry : productEntryList) {
            if (productEntry.getQuickLook() != null) {
                thumbnails.add(productEntry.getQuickLook());
            }
        }
        update(thumbnails.toArray(new Thumbnail[thumbnails.size()]));
    }
}
