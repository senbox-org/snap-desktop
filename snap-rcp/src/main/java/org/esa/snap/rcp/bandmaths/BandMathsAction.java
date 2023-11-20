/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.bandmaths;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeList;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.EXPLORER;

/**
 * This action creates a band using a mathematical expression
 * Enablement: when a product is selected
 *
 * @author Brockmann Consult
 * @author Daniel Knowles
 * @author Bing Yang
 */
//Apr2019 - Knowles/Yang - Added access to this tool in the "Raster" toolbar including enablement, tooltips and related icon.

@ActionID(
        category = "Tools",
        id = "BandMathsAction"
)
@ActionRegistration(
        displayName = "#CTL_BandMathsAction_Name",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Raster", position = 0),
        @ActionReference(path = "Toolbars/Raster", position = 10),
        @ActionReference(path = "Context/Product/Product", position = 10),
        @ActionReference(path = "Context/Product/RasterDataNode", position = 20)
})
@Messages({
        "CTL_BandMathsAction_Name=Math Band",
        "CTL_BandMathsAction_ShortDescription=Creates a new band using an arbitrary mathematical expression"
})
public class BandMathsAction extends AbstractAction implements HelpCtx.Provider, LookupListener, Presenter.Menu, Presenter.Toolbar {

    private static final String HELP_ID = "bandArithmetic";

    private Lookup lookup;
    private final Lookup.Result<ProductNode> viewResult;

    private static final String ICONS_DIRECTORY = "org/esa/snap/rcp/icons/";
    private static final String TOOL_ICON_LARGE = ICONS_DIRECTORY + "MathBand24.png";
    private static final String TOOL_ICON_SMALL = ICONS_DIRECTORY + "MathBand16.png";

    public BandMathsAction() {
        super(Bundle.CTL_BandMathsAction_Name());
        putValue(NAME, Bundle.CTL_BandMathsAction_Name()+"...");
        putValue(SHORT_DESCRIPTION, Bundle.CTL_BandMathsAction_ShortDescription());
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(TOOL_ICON_LARGE, false));
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(TOOL_ICON_SMALL, false));

        Lookup lookup = Utilities.actionsGlobalContext();
        this.lookup = lookup;
        this.viewResult = lookup.lookupResult(ProductNode.class);
        this.viewResult.addLookupListener(WeakListeners.create(LookupListener.class, this, viewResult));
        updateEnabledState();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final ProductNodeList<Product> products = new ProductNodeList<>();
        Product[] openedProducts = SnapApp.getDefault().getProductManager().getProducts();
        for (Product prod : openedProducts) {
            products.add(prod);
        }

        Product product = SnapApp.getDefault().getSelectedProduct(EXPLORER);
        if (product == null) {
            product = products.getAt(0);
        }

        Collection<? extends RasterDataNode> selectedRasters = Utilities.actionsGlobalContext().lookupAll(RasterDataNode.class);
        String expression = selectedRasters.stream().map(ProductNode::getName).collect(Collectors.joining(" + "));
        BandMathsDialog bandMathsDialog = new BandMathsDialog(product, products, expression, HELP_ID);
        bandMathsDialog.show();
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
