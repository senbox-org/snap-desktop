/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.windows;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneView;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.windows.TopComponent;

/**
 * A base class for the implementation of SNAP tool windows.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public abstract class ToolTopComponent extends TopComponent {

    protected ToolTopComponent() {
        SnapApp.getDefault().getSelectionSupport(ProductSceneView.class).addHandler((oldValue, newValue) -> {
            if (oldValue != null) {
                productSceneViewDeselected(oldValue);
            }
            if (newValue != null) {
                productSceneViewSelected(newValue);
            }
        });

        SnapApp.getDefault().getSelectionSupport(Product.class).addHandler((oldValue, newValue) -> {
            if (oldValue != null) {
                productDeselected(oldValue);
            }
            if (newValue != null) {
                productSelected(newValue);
            }
        });
    }

    public ProductSceneView getSelectedProductSceneView() {
        return SnapApp.getDefault().getSelectedProductSceneView();
    }

    protected void productSceneViewSelected(@NonNull ProductSceneView view) {
    }

    protected void productSceneViewDeselected(@NonNull ProductSceneView view) {
    }

    protected void productSelected(@NonNull Product product) {
    }

    protected void productDeselected(@NonNull Product product) {
    }
}
