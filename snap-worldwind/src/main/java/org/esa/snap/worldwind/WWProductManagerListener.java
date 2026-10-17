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

import javax.swing.*;
import java.util.function.Supplier;

/**
 * WWProductManagerListener
 * <p>
 * Product events may be fired from any thread (e.g. the GPF dialogs add and remove their target product from a
 * worker thread). Both additions and removals are therefore handled on the EDT. A removal that overtakes its
 * addition is safe because the addition is skipped once the product is no longer in the product manager.
 */
public class WWProductManagerListener implements ProductManager.Listener {

    private final WWView wwView;
    private final Supplier<ProductManager> productManager;

    public WWProductManagerListener(final WWView wwView) {
        this(wwView, () -> SnapApp.getDefault().getProductManager());
    }

    WWProductManagerListener(final WWView wwView, final Supplier<ProductManager> productManager) {
        this.wwView = wwView;
        this.productManager = productManager;
    }

    @Override
    public void productAdded(ProductManager.Event event) {
        final Product product = event.getProduct();
        runOnEDT(() -> {
            if (!productManager.get().contains(product)) {
                return;
            }
            wwView.setProducts(new Product[]{product});
            wwView.setSelectedProduct(product);
        });
    }

    @Override
    public void productRemoved(ProductManager.Event event) {
        final Product product = event.getProduct();
        runOnEDT(() -> {
            if (wwView.getSelectedProduct() == product) {
                wwView.setSelectedProduct(null);
            }
            wwView.removeProduct(product);
        });
    }

    private static void runOnEDT(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
