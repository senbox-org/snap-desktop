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
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwindx.examples.WMSLayersPanel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.worldwind.layers.DefaultProductLayer;
import org.esa.snap.worldwind.layers.FixingPlaceNameLayer;
import org.esa.snap.worldwind.layers.WWLayer;
import org.esa.snap.worldwind.layers.WWLayerDescriptor;
import org.esa.snap.worldwind.layers.WWLayerRegistry;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URISyntaxException;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.VIEW;

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

    private LayerPanel layerPanel = null;
    private ProductPanel productPanel = null;

    private final Dimension wmsPanelSize = new Dimension(400, 600);

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private int previousTabIndex = 0;

    private static final boolean includeStatusBar = true;
    private static final boolean includeLayerPanel = false;
    private static final boolean includeProductPanel = true;
    private static final boolean includeWMSPanel = false;

    private static final String[] servers = new String[]
            {
                    "http://neowms.sci.gsfc.nasa.gov/wms/wms",
                    //"http://mapserver.flightgear.org/cgi-bin/landcover",
                    "http://wms.jpl.nasa.gov/wms.cgi"
            };

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

        // world wind canvas
        initialize(mainPane);

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
        SystemUtils.LOG.info("INITIALIZE IN WWAnalysisToolView CALLED" + " includeLayerPanel " + includeLayerPanel +
                " includeProductPanel " + includeProductPanel);

        // share resources from existing WorldWind Canvas
        WorldWindowGLCanvas shareWith = findWorldWindView();

        final WWView toolView = this;
        final SwingWorker worker = new SwingWorker() {
            @Override
            protected Object doInBackground() {
                // Create the WorldWindow.
                try {

                    createWWPanel(shareWith, includeStatusBar, false, false);
                    wwjPanel.addLayerPanelLayer();
                    wwjPanel.addElevation();

                    // Put the pieces together.
                    mainPane.add(wwjPanel, BorderLayout.CENTER);

                    final LayerList layerList = getWwd().getModel().getLayers();

                    final Layer bingLayer = layerList.getLayerByName("Bing Imagery");
                    bingLayer.setEnabled(true);

//                    final OSMMapnikLayer streetLayer = new OSMMapnikLayer();
//                    streetLayer.setOpacity(0.7);
//                    streetLayer.setEnabled(false);
//                    streetLayer.setName("Open Street Map");
//                    insertTiledLayer(getWwd(), streetLayer);

                    final WWLayerDescriptor[] wwLayerDescriptors = WWLayerRegistry.getInstance().getWWLayerDescriptors();
                    for (WWLayerDescriptor layerDescriptor : wwLayerDescriptors) {
                        if (layerDescriptor.showIn3DToolView()) {
                            final WWLayer wwLayer = layerDescriptor.createWWLayer();
                            insertTiledLayer(getWwd(), wwLayer);

                            wwLayer.setOpacity(0.8);
                            // CHANGED: otherwise the objects in the product layer won't react to the select listener
                            // wwLayer.setPickEnabled(false);

                            if (wwLayer instanceof DefaultProductLayer) {
                                ((DefaultProductLayer) wwLayer).setEnableSurfaceImages(true);
                            }
                        }
                    }

                    // Instead of the default Place Name layer we use special implementation to replace
                    // wrong names in the original layer. https://senbox.atlassian.net/browse/SNAP-1476
                    final Layer placeNameLayer = layerList.getLayerByName("Place Names");
                    layerList.remove(placeNameLayer);

                    final FixingPlaceNameLayer fixingPlaceNameLayer = new FixingPlaceNameLayer();
                    layerList.add(fixingPlaceNameLayer);
                    fixingPlaceNameLayer.setEnabled(true);

                    if (includeLayerPanel) {
                        layerPanel = new LayerPanel(wwjPanel.getWwd(), null);
                        mainPane.add(layerPanel, BorderLayout.WEST);

                        layerPanel.add(makeControlPanel(), BorderLayout.SOUTH);
                        layerPanel.update(getWwd());
                    }
                    if (includeProductPanel) {
                        Layer layer = layerList.getLayerByName("Products");

                        productPanel = new ProductPanel(wwjPanel.getWwd(), (DefaultProductLayer) layer);
                        mainPane.add(productPanel, BorderLayout.WEST);

                        productPanel.add(makeControlPanel(), BorderLayout.SOUTH);

                        productPanel.update(getWwd());
                    }
                    if (includeWMSPanel) {
                        tabbedPane.add(new JPanel());
                        tabbedPane.setTitleAt(0, "+");
                        tabbedPane.addChangeListener(new ChangeListener() {
                            public void stateChanged(ChangeEvent changeEvent) {
                                if (tabbedPane.getSelectedIndex() != 0) {
                                    previousTabIndex = tabbedPane.getSelectedIndex();
                                    return;
                                }

                                final String server = JOptionPane.showInputDialog("Enter WMS server URL");
                                if (server == null || server.length() < 1) {
                                    tabbedPane.setSelectedIndex(previousTabIndex);
                                    return;
                                }

                                // Respond by adding a new WMSLayerPanel to the tabbed pane.
                                if (addTab(tabbedPane.getTabCount(), server.trim()) != null)
                                    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
                            }
                        });

                        // Create a tab for each server and add it to the tabbed panel.
                        for (int i = 0; i < servers.length; i++) {
                            addTab(i + 1, servers[i]); // i+1 to place all server tabs to the right of the Add Server tab
                        }

                        // Display the first server pane by default.
                        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() > 0 ? 1 : 0);
                        previousTabIndex = tabbedPane.getSelectedIndex();

                        mainPane.add(tabbedPane, BorderLayout.EAST);
                    }

                    wwjPanel.getWwd().addSelectListener(new SelectListener() {

                        public void selected(SelectEvent event) {
                            /*
                            System.out.println("event.getTopObject() " + event.getTopObject());
                            if (event.getTopObject() instanceof AnalyticSurface.AnalyticSurfaceObject) {
                                System.out.println("pick point: " + event.getPickPoint());
                                Point pickPoint = event.getPickPoint();
                                if (pickPoint != null) {
                                    System.out.println("position: " + wwjPanel.getWwd().getView().computePositionFromScreenPoint(pickPoint.getX(), pickPoint.getY()));
                                }

                                //AnalyticSurface surface = (AnalyticSurface) event.getTopObject();
                                //System.out.println("dimensions " + surface.getDimensions());
                                //System.out.println("getCorners " + surface.getSector().getCorners());
                            }
                            */
                            final LayerList layerList = getWwd().getModel().getLayers();
                            for (Layer layer : layerList) {
                                if (layer instanceof WWLayer) {
                                    final WWLayer wwLayer = (WWLayer) layer;
                                    wwLayer.updateInfoAnnotation(event);
                                }
                            }
                        }
                    });

                    // update world map window with the information of the currently activated product scene view.
                    final SnapApp snapApp = SnapApp.getDefault();
                    snapApp.getProductManager().addListener(new WWProductManagerListener(toolView));
                    snapApp.getSelectionSupport(ProductNode.class).addHandler((oldValue, newValue) -> {
                        if (newValue != null) {
                            setSelectedProduct(newValue.getProduct());
                        } else {
                            setSelectedProduct(null);
                        }
                    });

                    snapApp.getSelectionSupport(RasterDataNode.class).addHandler((oldValue, newValue) -> {
                        setSelectedRaster(newValue);
                    });

                    setProducts(snapApp.getProductManager().getProducts());
                    setSelectedProduct(snapApp.getSelectedProduct(VIEW));

                } catch (Throwable e) {
                    SnapApp.getDefault().handleError("Unable to initialize WWAnalysisToolView: " + e.getMessage(), e);
                }
                return null;
            }
        };
        worker.execute();
    }

    private JPanel makeControlPanel() {

        final JPanel controlPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        controlPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        final LayerList layerList = getWwd().getModel().getLayers();
        for (Layer layer : layerList) {
            if (layer instanceof WWLayer) {
                final WWLayer wwLayer = (WWLayer) layer;
                final JPanel layerControlPanel = wwLayer.getControlPanel(getWwd());
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

    private WMSLayersPanel addTab(final int position, final String server) {
        // Add a server to the tabbed dialog.
        try {
            final WMSLayersPanel layersPanel = new WMSLayersPanel(wwjPanel.getWwd(), server, wmsPanelSize);
            this.tabbedPane.add(layersPanel, BorderLayout.CENTER);
            final String title = layersPanel.getServerDisplayString();
            this.tabbedPane.setTitleAt(position, title != null && title.length() > 0 ? title : server);

            // Add a listener to notice wms layer selections and tell the layer panel to reflect the new state.
            layersPanel.addPropertyChangeListener("LayersPanelUpdated", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    layerPanel.update(wwjPanel.getWwd());
                }
            });

            return layersPanel;
        } catch (URISyntaxException e) {
            Dialogs.showError("Invalid Server URL", "Server URL is invalid");
            tabbedPane.setSelectedIndex(previousTabIndex);
            return null;
        }
    }
}
