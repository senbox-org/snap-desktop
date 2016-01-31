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
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.productlibrary.rcp.toolviews.model.SortingDecorator;
import org.esa.snap.rcp.quicklooks.ThumbnailPanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luis on 29/01/2016.
 */
public class ThumbnailView extends ThumbnailPanel implements ListView {


    public ThumbnailView() {
        super(true);
    }

    public File[] getSelectedFiles() {
        return new File[] {};
    }

    public ProductEntry[] getSelectedProductEntries() {
        return new ProductEntry[] {};
    }

    public ProductEntry getEntryOverMouse() {
        return null;
    }

    public void sort(final SortingDecorator.SORT_BY sortBy) {

    }

    public void selectAll() {

    }

    public void clearSelection() {

    }

    public int getSelectionCount() {
        return 0;
    }

    public void updateUI() {

    }

    public void setProductEntryList(final ProductEntry[] productEntryList) {
        final List<Quicklook> thumbnails = new ArrayList<>(productEntryList.length);
        for(ProductEntry productEntry : productEntryList) {
            if(productEntry.getQuickLook() != null) {
                thumbnails.add(productEntry.getQuickLook());
            }
        }
        update(thumbnails.toArray(new Thumbnail[thumbnails.size()]));
    }
}
