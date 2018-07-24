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

/**
 * Created by luis on 30/01/2016.
 */
public class ProductEntryList extends ProductEntryTable {

    public ProductEntryList(final ProductLibraryActions productLibraryActions) {
        super(productLibraryActions);
    }

    public void setProductEntryList(final ProductEntry[] productEntryList) {
        final ProductEntryTableModel tableModel = new ProductEntryTableModel(productEntryList, true);
        setModel(new SortingDecorator(tableModel, getTableHeader()));
        setColumnModel(tableModel.getColumnModel());
    }
}
