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

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.worldwind.layers.DefaultProductLayer;
import org.esa.snap.worldwind.layers.WWLayer;
import org.esa.snap.worldwind.layers.WWLayerDescriptor;
import org.esa.snap.worldwind.layers.WWLayerRegistry;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

@TopComponent.Description(
        preferredID = "WWAnalysisToolView",
        iconBase = "org/esa/snap/worldwind/icons/worldwind.png",
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(
        mode = "editor",
        openAtStartup = false,
        position = 60
)
@ActionID(category = "Window", id = "org.esa.snap.worldwind.WWAnalysisToolView")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows", position = 70),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_WorldWindAnalysisTopComponentName",
        preferredID = "WWAnalysisToolView"
)
@NbBundle.Messages({
        "CTL_WorldWindAnalysisTopComponentName=WorldWind Analysis View",
        "CTL_WorldWindAnalysisTopComponentDescription=WorldWind Analysis World Map",
})

/**
 * The window displaying the full WorldWind 3D for analysis.
 *
 */
public class WWAnalysisToolView extends WWBaseToolView implements WWView {

    private ProductPanel productPanel = null;

    private static final boolean includeStatusBar = true;
    private static final boolean includeProductPanel = true;

    public WWAnalysisToolView() {
        setDisplayName("WorldWind Analysis");
        setLayout(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        add(createControl(), BorderLayout.CENTER);
    }

    public JComponent createControl() {

        final Window windowPane = SwingUtilities.getWindowAncestor(this);
        if (windowPane != null)
            windowPane.setSize(800, 400);
        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setSize(new Dimension(300, 300));

        // world wind canvas, created on the EDT once this component exists
        SwingUtilities.invokeLater(() -> initialize(mainPane));

        return mainPane;
    }

    private static void insertTiledLayer(final WorldWindow wwd, final Layer layer) {
        int position = 0;
        final LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers) {
            if (l instanceof PlaceNameLayer) {
                position = layers.indexOf(l);
                break;
            }
        }
        layers.add(position, layer);
    }

    private void initialize(final JPanel mainPane) {
        SystemUtils.LOG.fine("INITIALIZE IN WWAnalysisToolView CALLED includeProductPanel " + includeProductPanel);

        try {
            // share resources from existing WorldWind Canvas
            final WorldWindowGLCanvas shareWith = findWorldWindView();

            createWWPanel(shareWith, includeStatusBar, false, false);
            wwjPanel.addLayerPanelLayer();

            final LayerList layerList = getWwd().getModel().getLayers();
            AppPanel.enableLayer(layerList, "Bing Imagery");

            DefaultProductLayer productLayer = null;
            final WWLayerDescriptor[] wwLayerDescriptors = WWLayerRegistry.getInstance().getWWLayerDescriptors();
            for (WWLayerDescriptor layerDescriptor : wwLayerDescriptors) {
                if (layerDescriptor.showIn3DToolView()) {
                    final WWLayer wwLayer = layerDescriptor.createWWLayer();
                    insertTiledLayer(getWwd(), wwLayer);

                    wwLayer.setOpacity(0.8);
                    // CHANGED: otherwise the objects in the product layer won't react to the select listener
                    // wwLayer.setPickEnabled(false);

                    if (wwLayer instanceof DefaultProductLayer) {
                        productLayer = (DefaultProductLayer) wwLayer;
                        productLayer.setEnableSurfaceImages(true);
                    }
                }
            }

            AppPanel.replacePlaceNameLayer(layerList);

            // Put the pieces together.
            mainPane.add(wwjPanel, BorderLayout.CENTER);

            if (includeProductPanel && productLayer != null) {
                productPanel = new ProductPanel(wwjPanel.getWwd(), productLayer);
                mainPane.add(productPanel, BorderLayout.WEST);

                productPanel.add(makeControlPanel(), BorderLayout.SOUTH);

                productPanel.update(getWwd());
                // outlines are installed asynchronously
                productLayer.setChangeListener(() -> productPanel.update(getWwd()));
            }
            mainPane.revalidate();

            wwjPanel.getWwd().addSelectListener(event -> {
                final List<WWLayer> wwLayers = getWWLayers();
                for (WWLayer wwLayer : wwLayers) {
                    wwLayer.updateInfoAnnotation(event);
                }
            });

            // update world map window with the information of the currently activated product scene view.
            connectToSnap();

        } catch (Throwable e) {
            productPanel = null;
            showInitError(mainPane, "WorldWind Analysis View", e);
        }
    }

    private JPanel makeControlPanel() {

        final JPanel controlPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        controlPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        final List<WWLayer> layerList = getWWLayers();
        for (WWLayer wwLayer : layerList) {
            final JPanel layerControlPanel = wwLayer.getControlPanel(getWwd());
            if (layerControlPanel != null) {
                controlPanel.add(layerControlPanel);
            }
        }

        return controlPanel;
    }

    @Override
    public void setSelectedProduct(final Product product) {
        super.setSelectedProduct(product);

        if (productPanel != null)
            productPanel.update(getWwd());
    }

    @Override
    public void setProducts(final Product[] products) {
        super.setProducts(products);

        if (productPanel != null)
            productPanel.update(getWwd());
    }

    @Override
    public void removeProduct(final Product product) {
        super.removeProduct(product);

        if (productPanel != null)
            productPanel.update(getWwd());
    }
}
