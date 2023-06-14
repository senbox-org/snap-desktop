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
package org.esa.snap.rcp.mask;

import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.help.HelpDisplayer;
import org.esa.snap.ui.product.ProductSceneView;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.util.HelpCtx;

import javax.swing.AbstractButton;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.EXPLORER;

public abstract class MaskToolTopComponent extends ToolTopComponent implements HelpCtx.Provider {

    private MaskForm maskForm;

    public void initUI() {
        setLayout(new BorderLayout());
        maskForm = createMaskForm(this, e -> {
            final ProductSceneView sceneView = getSelectedProductSceneView();
            if (sceneView != null) {
                Mask selectedMask = maskForm.getSelectedMask();
                if (selectedMask != null) {
                    VectorDataNode vectorDataNode = Mask.VectorDataType.getVectorData(selectedMask);
                    if (vectorDataNode != null) {
                        sceneView.selectVectorDataLayer(vectorDataNode);
                    }
                }
            }
        });

        AbstractButton helpButton = maskForm.getHelpButton();
        if (helpButton != null) {
            helpButton.addActionListener(e -> HelpDisplayer.show(getHelpCtx()));
            helpButton.setName("helpButton");
        }

        updateMaskForm(getSelectedProductSceneView());

        SnapApp.getDefault().getProductManager().addListener(new MaskPTL());
        SnapApp.getDefault().getSelectionSupport(Product.class).addHandler((oldValue, newValue) -> updateMaskForm(getSelectedProductSceneView()));
        maskForm.updateState();
        setDisplayName(getTitle());
        add(maskForm.createContentPanel(), BorderLayout.CENTER);
    }

    private void updateMaskForm(ProductSceneView view) {
        if (view == null) {
            final ProductNode selectedProductNode = SnapApp.getDefault().getSelectedProductNode(EXPLORER);
            if (selectedProductNode instanceof RasterDataNode) {
                final RasterDataNode rdn = (RasterDataNode) selectedProductNode;
                maskForm.reconfigureMaskTable(rdn.getProduct(), rdn);
            } else if (selectedProductNode instanceof Product) {
                final Product product = (Product) selectedProductNode;
                maskForm.reconfigureMaskTable(product, null);
            } else if (selectedProductNode != null && selectedProductNode.getProduct() != null) {
                maskForm.reconfigureMaskTable(selectedProductNode.getProduct(), null);
            } else {
                maskForm.clearMaskTable();
            }
        } else {
            maskForm.reconfigureMaskTable(view.getProduct(), view.getRaster());
        }
    }

    private class MaskPTL implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            //do nothing
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            if (maskForm.getProduct() == event.getProduct()) {
                updateMaskForm(getSelectedProductSceneView());
            }
        }
    }

    @Override
    protected void productSceneViewSelected(@NonNull ProductSceneView view) {
        updateMaskForm(view);
    }

    @Override
    protected void productSceneViewDeselected(@NonNull ProductSceneView view) {
        updateMaskForm(getSelectedProductSceneView());
    }

    protected abstract MaskForm createMaskForm(ToolTopComponent topComponent, ListSelectionListener selectionListener);

    protected abstract String getTitle();

}
