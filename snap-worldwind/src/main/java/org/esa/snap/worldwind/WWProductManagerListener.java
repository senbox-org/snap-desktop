/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.worldwind;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.rcp.SnapApp;

/**
 * WWProductManagerListener
 */
public class WWProductManagerListener implements ProductManager.Listener {

    private final WWView wwView;

    public WWProductManagerListener(final WWView wwView) {
        this.wwView = wwView;
    }

    @Override
    public void productAdded(ProductManager.Event event) {
        final Product product = event.getProduct();
        wwView.setSelectedProduct(product);
        wwView.setProducts(SnapApp.getDefault().getProductManager().getProducts());
    }

    @Override
    public void productRemoved(ProductManager.Event event) {
        final Product product = event.getProduct();
        if (wwView.getSelectedProduct() == product) {
            wwView.setSelectedProduct(null);
        }
        wwView.removeProduct(product);
    }
}
