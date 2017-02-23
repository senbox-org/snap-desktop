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

import org.esa.snap.rcp.SnapApp;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**

 */
public class ProductOpener {

    public ProductOpener() {
    }

    public static void openProducts(final File[] productFiles) {
        final ExecutorService es = Executors.newSingleThreadExecutor();
        for (File productFile : productFiles) {
            if (!productFile.exists()) {
                continue;
            }
            es.submit(new Opener(productFile));
        }
    }

    private static class Opener implements Runnable {
        private final File productFile;

        public Opener(File file) {
            productFile = file;
        }

        public void run() {
            try {
                //Product product = CommonReaders.readProduct(productFile);
                //SnapApp.getDefault().getProductManager().addProduct(product);

                final org.esa.snap.rcp.actions.file.ProductOpener opener = new org.esa.snap.rcp.actions.file.ProductOpener();
                opener.setFiles(productFile);
                opener.setMultiSelectionEnabled(true);
                opener.openProduct();
            } catch (Exception e) {
                SnapApp.getDefault().handleError("Not able to open product:\n" + productFile.getPath(), e);
            }
        }
    }
}
