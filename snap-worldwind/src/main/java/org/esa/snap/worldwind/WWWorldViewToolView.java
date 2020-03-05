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

import gov.nasa.worldwind.layers.Earth.MSVirtualEarthLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.worldwind.layers.WWLayer;
import org.esa.snap.worldwind.layers.WWLayerDescriptor;
import org.esa.snap.worldwind.layers.WWLayerRegistry;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.*;

@TopComponent.Description(
        preferredID = "WWWorldMapToolView",
        iconBase = "org/esa/snap/icons/earth.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = PackageDefaults.WORLD_VIEW_MODE,
        openAtStartup = PackageDefaults.WORLD_VIEW_OPEN,
        position = PackageDefaults.WORLD_VIEW_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.worldwind.WWWorldMapToolView")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_WorldWindTopComponentName",
        preferredID = "WWWorldMapToolView"
)
@NbBundle.Messages({
        "CTL_WorldWindTopComponentName=" + PackageDefaults.WORLD_VIEW_NAME,
        "CTL_WorldWindTopComponentDescription=WorldWind World View",
})
/**
 * The window displaying the world map.
 */
public class WWWorldViewToolView extends WWBaseToolView implements WWView {

    public static String useFlatEarth = "snap.worldwind.useFlatEarth";

    private ProductSceneView currentView;

    private static final boolean includeStatusBar = true;
    private final boolean flatWorld;

    public WWWorldViewToolView() {
        setDisplayName(Bundle.CTL_WorldWindTopComponentName());
        flatWorld = Config.instance().preferences().getBoolean(useFlatEarth, false);
        initComponents();
        SnapApp.getDefault().getSelectionSupport(ProductSceneView.class).addHandler((oldValue, newValue) -> setCurrentView(newValue));
    }

    private void initComponents() {
        setLayout(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        add(createControl(), BorderLayout.CENTER);
    }

    public JComponent createControl() {

        final Window windowPane = SwingUtilities.getWindowAncestor(this);
        if (windowPane != null)
            windowPane.setSize(300, 300);
        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setSize(new Dimension(300, 300));

        // world wind canvas
        initialize(mainPane);

        return mainPane;
    }

    private void initialize(final JPanel mainPane) {
        final WWView toolView = this;

        final SwingWorker worker = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                // Create the WorldWindow.
                try {
                    createWWPanel(null, includeStatusBar, flatWorld, true);

                    // Put the pieces together.
                    mainPane.add(wwjPanel, BorderLayout.CENTER);

                    final LayerList layerList = getWwd().getModel().getLayers();

                    final MSVirtualEarthLayer virtualEarthLayerA = new MSVirtualEarthLayer(MSVirtualEarthLayer.LAYER_AERIAL);
                    virtualEarthLayerA.setName("MS Bing Aerial");
                    layerList.add(virtualEarthLayerA);

                    final WWLayerDescriptor[] wwLayerDescriptors = WWLayerRegistry.getInstance().getWWLayerDescriptors();
                    for (WWLayerDescriptor layerDescriptor : wwLayerDescriptors) {
                        if (layerDescriptor.showInWorldMapToolView()) {
                            final WWLayer wwLayer = layerDescriptor.createWWLayer();
                            layerList.add(wwLayer);

                            wwLayer.setOpacity(1.0);
                            wwLayer.setPickEnabled(false);
                        }
                    }

                    final Layer placeNameLayer = layerList.getLayerByName("Place Names");
                    placeNameLayer.setEnabled(true);

                    SnapApp.getDefault().getProductManager().addListener(new WWProductManagerListener(toolView));
                    SnapApp.getDefault().getSelectionSupport(ProductNode.class).addHandler(new SelectionSupport.Handler<ProductNode>() {
                        @Override
                        public void selectionChange(@NullAllowed ProductNode oldValue, @NullAllowed ProductNode newValue) {
                            if (newValue != null) {
                                setSelectedProduct(newValue.getProduct());
                            } else {
                                setSelectedProduct(null);
                            }
                        }
                    });

                    setProducts(SnapApp.getDefault().getProductManager().getProducts());
                    setSelectedProduct(SnapApp.getDefault().getSelectedProduct(VIEW));
                } catch (Throwable e) {
                    SnapApp.getDefault().handleError("Unable to initialize WWWorldMapToolView: " + e.getMessage(), e);
                }
                return null;
            }
        };
        worker.execute();
    }

    public void setCurrentView(final ProductSceneView newView) {
        if (currentView != newView) {
            currentView = newView;
        }
    }
}
