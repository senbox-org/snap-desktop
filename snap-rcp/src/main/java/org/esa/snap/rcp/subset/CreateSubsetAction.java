/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.subset;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.ProductSubsetDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


/**
 * This action opens a product subset dialog with the initial spatial bounds
 * taken from the currently visible image area, if any.
 * Enablement: when a product is selected
 *
 * @author Norman Fomferra
 * @author Daniel Knowles
 * @author Bing Yang
 */
//Apr2019 - Knowles/Yang - Added access to this tool in the "Raster" toolbar including enablement, tooltips and related icon.


@ActionID(category = "Raster", id = "CreateSubsetAction")
@ActionRegistration(displayName = "#CTL_CreateSubsetAction_Name", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/Raster", position = 50),
        @ActionReference(path = "Toolbars/Raster", position = 40)
})
@NbBundle.Messages({
        "CTL_CreateSubsetAction_Name=Subset",
        "CTL_CreateSubsetAction_ShortDescription=Creates a subset (spatial, subsample, raster) of the current file"
})




public class CreateSubsetAction extends AbstractAction implements LookupListener, Presenter.Menu, Presenter.Toolbar{

    static int subsetNumber;


    private static final String ICONS_DIRECTORY = "org/esa/snap/rcp/icons/";
    private static final String TOOL_ICON_LARGE = ICONS_DIRECTORY + "Subset24.png";
    private static final String TOOL_ICON_SMALL = ICONS_DIRECTORY + "Subset16.png";

    private Lookup lookup;
    private final Lookup.Result<ProductNode> viewResult;


    public CreateSubsetAction() {
        putValue(NAME, Bundle.CTL_CreateSubsetAction_Name()+"...");
        putValue(SHORT_DESCRIPTION, Bundle.CTL_CreateSubsetAction_ShortDescription());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(TOOL_ICON_SMALL, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(TOOL_ICON_LARGE, false));

        Lookup lookup = Utilities.actionsGlobalContext();
        this.lookup = lookup;
        this.viewResult = lookup.lookupResult(ProductNode.class);
        this.viewResult.addLookupListener(WeakListeners.create(LookupListener.class, this, viewResult));
        updateEnabledState();
    }


    @Override
    public void actionPerformed(ActionEvent ignored) {
        Product product = this.lookup.lookup(ProductNode.class).getProduct();

        RasterDataNode rasterDataNode = null;
        ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view != null && view.getProduct() == product && view.getRaster() != null) {
            rasterDataNode = view.getRaster();
        }

        if (product != null) {
            createSubset(product, getInitialBounds(product), rasterDataNode);
        }
    }

    public static void createSubset(Product sourceProduct, Rectangle bounds, RasterDataNode rdn) {
        final String subsetName = "subset_" + CreateSubsetAction.subsetNumber + "_of_" + sourceProduct.getName();
        final ProductSubsetDef initSubset = new ProductSubsetDef();

        initSubset.setSubsetRegion(new PixelSubsetRegion(bounds, 0));
        if(sourceProduct.isMultiSize() && rdn != null) {
            initSubset.setRegionMap(SubsetOp.computeRegionMap(initSubset.getRegion(),rdn.getName(),sourceProduct,null));
        } else if(sourceProduct.isMultiSize()) {
            initSubset.setRegionMap(SubsetOp.computeRegionMap(initSubset.getRegion(),sourceProduct,null));
        }
        initSubset.setNodeNames(sourceProduct.getBandNames());
        initSubset.addNodeNames(sourceProduct.getTiePointGridNames());
        initSubset.setIgnoreMetadata(false);
        final ProductSubsetDialog subsetDialog = new ProductSubsetDialog(SnapApp.getDefault().getMainFrame(),
                sourceProduct, initSubset);
        if (subsetDialog.show() != ProductSubsetDialog.ID_OK) {
            return;
        }
        final ProductSubsetDef subsetDef = subsetDialog.getProductSubsetDef();
        if (subsetDef == null) {
            Dialogs.showInformation(Bundle.CTL_CreateSubsetFromViewAction_Title(),
                    "No product subset created.",
                    null);
            return;
        }
        try {
            final Product subset = sourceProduct.createSubset(subsetDef, subsetName,
                    sourceProduct.getDescription());
            SnapApp.getDefault().getProductManager().addProduct(subset);
            CreateSubsetAction.subsetNumber++;
        } catch (Exception e) {
            final String msg = "An error occurred while creating the product subset:\n" +
                    e.getMessage();
            SnapApp.getDefault().handleError(msg, e);
        }
    }

    public static void createSubset(Product sourceProduct, Rectangle bounds) {
        createSubset(sourceProduct, bounds, null);
    }

    private Rectangle getInitialBounds(Product product) {
        Rectangle bounds = null;
        ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view != null && view.getProduct() == product) {
            bounds = view.getVisibleImageBounds();
        } else {
            bounds = new Rectangle(0,0,product.getSceneRasterWidth(),product.getSceneRasterHeight());
        }
        return bounds;
    }



    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem menuItem = new JMenuItem(this);
        return menuItem;
    }

    @Override
    public Component getToolbarPresenter() {
        JButton button = new JButton(this);
        button.setText(null);
        return button;
    }


    public void resultChanged(LookupEvent ignored) {
        updateEnabledState();
    }

    protected void updateEnabledState() {
        ProductNode productNode = this.lookup.lookup(ProductNode.class);
        setEnabled(productNode != null );
    }


}