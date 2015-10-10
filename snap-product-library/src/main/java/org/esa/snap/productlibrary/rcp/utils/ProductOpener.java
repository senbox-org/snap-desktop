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
package org.esa.snap.productlibrary.rcp.utils;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.engine_utilities.db.CommonReaders;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;

import java.io.File;
import java.io.IOException;

/**

 */
public class ProductOpener {

    public ProductOpener() {
    }

    public void openProducts(final File[] productFiles) {
        for (File productFile : productFiles) {
            if (!productFile.exists()) {
                continue;
            }
            try {
                final Product product = CommonReaders.readProduct(productFile);

                final ProductManager productManager = SnapApp.getDefault().getProductManager();
                productManager.addProduct(product);
            } catch (IOException e) {
                SnapDialogs.showError("Not able to open product:\n" + productFile.getPath());
            }
        }
    }
}
