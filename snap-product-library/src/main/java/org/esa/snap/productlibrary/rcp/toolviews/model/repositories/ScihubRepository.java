/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews.model.repositories;

import org.esa.snap.engine_utilities.db.ProductQueryInterface;
import org.esa.snap.engine_utilities.download.opensearch.CopernicusProductQuery;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryToolView;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;

/**
 * Created by luis on 24/02/2017.
 */
public class ScihubRepository implements RepositoryInterface {

    public static final String NAME = "ESA SciHub";

    private static final ImageIcon icon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/copernicus.png", ProductLibraryToolView.class);

    public String getName() {
        return NAME;
    }

    @Override
    public String toString() {
        return getName();
    }

    public ImageIcon getIconImage() {
        return icon;
    }

    public ProductQueryInterface getProductQueryInterface() {
        return CopernicusProductQuery.instance();
    }
}
