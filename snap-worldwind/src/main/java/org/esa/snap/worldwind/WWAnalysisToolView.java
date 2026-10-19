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

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.LayerList;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.worldwind.layers.DefaultProductLayer;
import org.esa.snap.worldwind.layers.ImageViewSyncLayer;
import org.esa.snap.worldwind.layers.ProductImageLayer;
import org.esa.snap.worldwind.layers.WWLayer;
import org.esa.snap.worldwind.layers.WWLayerDescriptor;
import org.esa.snap.worldwind.layers.WWLayerRegistry;
import org.esa.snap.worldwind.model.ImageViewGeoUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private final GlobeCursorSync globeCursorSync = new GlobeCursorSync();

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
            ProductImageLayer imageLayer = null;
            final WWLayerDescriptor[] wwLayerDescriptors = WWLayerRegistry.getInstance().getWWLayerDescriptors();
            for (WWLayerDescriptor layerDescriptor : wwLayerDescriptors) {
                if (layerDescriptor.showIn3DToolView()) {
                    final WWLayer wwLayer = layerDescriptor.createWWLayer();
                    LayerConfiguration.insertBelowOverlays(layerList, wwLayer);

                    wwLayer.setOpacity(0.8);
                    // CHANGED: otherwise the objects in the product layer won't react to the select listener
                    // wwLayer.setPickEnabled(false);

                    if (wwLayer instanceof DefaultProductLayer) {
                        productLayer = (DefaultProductLayer) wwLayer;
                        productLayer.setEnableSurfaceImages(true);
                    } else if (wwLayer instanceof ProductImageLayer) {
                        imageLayer = (ProductImageLayer) wwLayer;
                        // full-resolution images are opaque by default; the quicklooks keep the layer opacity
                        imageLayer.setOpacity(1.0);
                    }
                }
            }

            AppPanel.addReferenceLayers(layerList);

            if (productLayer != null && imageLayer != null) {
                // a full-resolution image replaces the quicklook of its product
                final DefaultProductLayer quicklookLayer = productLayer;
                imageLayer.setImageShownListener((product, shown) -> quicklookLayer.setQuicklookVisible(product, !shown));
            }

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
                if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
                    selectPlacemarkInView(ImageViewSyncLayer.getPlacemark(event.getTopObject()));
                }
            });
            installImageViewSync(wwjPanel.getWwd());

            // update world map window with the information of the currently activated product scene view.
            connectToSnap();

        } catch (Throwable e) {
            productPanel = null;
            showInitError(mainPane, "WorldWind Analysis View", e);
        }
    }

    /**
     * Globe to image view synchronisation: the globe cursor in the image views, and a context menu to place pins
     * and centre the image view.
     */
    private void installImageViewSync(final WorldWindowGLCanvas wwd) {
        wwd.addPositionListener(event -> {
            final Position position = event.getPosition();
            globeCursorSync.setGlobePosition(position != null ?
                    new GeoPos(position.getLatitude().degrees, position.getLongitude().degrees) : null);
        });
        wwd.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(final MouseEvent e) {
                globeCursorSync.clear();
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(final MouseEvent e) {
                final Position position = wwd.getCurrentPosition();
                if (e.isPopupTrigger() && position != null) {
                    createPopupMenu(new GeoPos(position.getLatitude().degrees, position.getLongitude().degrees))
                            .show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private JPopupMenu createPopupMenu(final GeoPos geoPos) {
        final JPopupMenu menu = new JPopupMenu();
        final Product product = getSelectedProduct();
        final JMenuItem placePin = new JMenuItem("Place Pin Here");
        placePin.setEnabled(product != null && ImageViewGeoUtils.toPixel(product.getSceneGeoCoding(),
                product.getSceneRasterWidth(), product.getSceneRasterHeight(), geoPos) != null);
        placePin.addActionListener(e -> ImageViewSyncLayer.placePin(product, geoPos));
        menu.add(placePin);

        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        final JMenuItem centreView = new JMenuItem("Centre Image View Here");
        centreView.setEnabled(view != null && view.getRaster() != null &&
                                      ImageViewGeoUtils.toRasterPixel(view.getRaster(), geoPos) != null);
        centreView.addActionListener(e -> ImageViewSyncLayer.centreView(view, geoPos));
        menu.add(centreView);
        return menu;
    }

    private void selectPlacemarkInView(final Placemark placemark) {
        if (placemark == null || placemark.getProduct() == null) {
            return;
        }
        final ProductSceneView selected = SnapApp.getDefault().getSelectedProductSceneView();
        if (ImageViewSyncLayer.selectInView(placemark, selected)) {
            return;
        }
        for (ProductSceneView view : GlobeCursorSync.findOpenViews()) {
            if (ImageViewSyncLayer.selectInView(placemark, view)) {
                return;
            }
        }
    }

    @Override
    protected void componentClosed() {
        globeCursorSync.clear();
        super.componentClosed();
    }

    private JPanel makeControlPanel() {

        final JPanel controlPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        controlPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        final JCheckBox syncCursor = new JCheckBox("Sync cursor with image views", globeCursorSync.isEnabled());
        syncCursor.addActionListener(e -> globeCursorSync.setEnabled(syncCursor.isSelected()));
        controlPanel.add(syncCursor);

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
