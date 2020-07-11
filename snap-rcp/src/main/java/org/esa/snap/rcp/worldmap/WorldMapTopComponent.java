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
package org.esa.snap.rcp.worldmap;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.WorldMapPane;
import org.esa.snap.ui.WorldMapPaneDataModel;
import org.esa.snap.ui.product.ProductSceneView;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.*;

@TopComponent.Description(
        preferredID = "WorldMapTopComponent",
        iconBase = "org/esa/snap/rcp/icons/WorldMap.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = PackageDefaults.WORLD_MAP_MODE,
        openAtStartup = PackageDefaults.WORLD_MAP_OPEN,
        position = PackageDefaults.WORLD_MAP_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.worldmap.WorldMapTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_WorldMapTopComponent_Name",
        preferredID = "WorldMapTopComponent"
)
@NbBundle.Messages({
        "CTL_WorldMapTopComponent_Name=" + PackageDefaults.WORLD_MAP_NAME,
        "CTL_WorldMapTopComponent_HelpId=showWorldMapWnd"
})
/**
 * The window displaying the world map.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class WorldMapTopComponent extends ToolTopComponent {

    public static final String ID = WorldMapTopComponent.class.getName();

    protected WorldMapPaneDataModel worldMapDataModel;

    public WorldMapTopComponent() {
        setDisplayName(Bundle.CTL_WorldMapTopComponent_Name());
        initUI();
    }

    public void initUI() {
        setLayout(new BorderLayout());

        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setPreferredSize(new Dimension(320, 160));

        worldMapDataModel = new WorldMapPaneDataModel();
        final WorldMapPane worldMapPane = new WorldMapPane(worldMapDataModel);
        worldMapPane.setNavControlVisible(true);
        mainPane.add(worldMapPane, BorderLayout.CENTER);

        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(new WorldMapProductManagerListener());
        snapApp.getSelectionSupport(ProductNode.class).addHandler(new SelectionSupport.Handler<ProductNode>() {
            @Override
            public void selectionChange(@NullAllowed ProductNode oldValue, @NullAllowed ProductNode newValue) {
                if(newValue != null) {
                    setSelectedProduct(newValue.getProduct());
                }
            }
        });
        setProducts(snapApp.getProductManager().getProducts());
        setSelectedProduct(snapApp.getSelectedProduct(VIEW));

        add(mainPane, BorderLayout.CENTER);
    }

    public void setSelectedProduct(Product product) {
        worldMapDataModel.setSelectedProduct(product);
    }

    public Product getSelectedProduct() {
        return worldMapDataModel.getSelectedProduct();
    }


    public void setProducts(Product[] products) {
        worldMapDataModel.setProducts(products);
    }

    @Override
    protected void productSceneViewSelected(@NonNull ProductSceneView view) {
        setSelectedProduct(view.getProduct());
    }

    private class WorldMapProductManagerListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            final Product product = event.getProduct();
            worldMapDataModel.addProduct(product);
            setSelectedProduct(product);
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            final Product product = event.getProduct();
            if (getSelectedProduct() == product) {
                setSelectedProduct(null);
            }
            worldMapDataModel.removeProduct(product);
        }
    }

}
