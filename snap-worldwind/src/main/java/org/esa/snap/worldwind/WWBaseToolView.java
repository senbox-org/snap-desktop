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

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.LayerList;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.worldwind.layers.BaseLayer;
import org.esa.snap.worldwind.layers.WWLayer;
import org.openide.windows.WindowManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.VIEW;

/**
 * Base WorldWind ToolView
 * <p>
 * All WorldWind and Swing access happens on the EDT. The view only follows the products and selections of SNAP
 * while it is open; closing it removes all products from its layers so that closed products are not kept alive.
 */
public abstract class WWBaseToolView extends ToolTopComponent implements WWView {

    private final Dimension canvasSize = new Dimension(800, 600);
    protected AppPanel wwjPanel = null;
    private Position eyePosition = null;

    private ProductManager.Listener productManagerListener;
    private SelectionSupport.Handler<ProductNode> productNodeHandler;
    private SelectionSupport.Handler<RasterDataNode> rasterHandler;
    private SelectionSupport.Handler<ProductSceneView> sceneViewHandler;

    public WWBaseToolView() {
    }

    AppPanel createWWPanel(final WorldWindowGLCanvas shareWith,
                           final boolean includeStatusBar, final boolean flatWorld, final boolean removeExtraLayers) {
        wwjPanel = new AppPanel(shareWith, includeStatusBar, flatWorld, removeExtraLayers);
        wwjPanel.setPreferredSize(canvasSize);
        return wwjPanel;
    }

    AppPanel createWWPanel(final WorldWindowGLCanvas shareWith, final boolean includeStatusBar,
                           final boolean flatWorld, final Model model) {
        wwjPanel = new AppPanel(shareWith, includeStatusBar, flatWorld, model);
        wwjPanel.setPreferredSize(canvasSize);
        return wwjPanel;
    }

    WorldWindowGLCanvas getWwd() {
        if (wwjPanel == null)
            return null;
        return wwjPanel.getWwd();
    }

    protected WorldWindowGLCanvas findWorldWindView() {
        final WWWorldViewToolView window = (WWWorldViewToolView)
                WindowManager.getDefault().findTopComponent("WWWorldMapToolView");
        if(window != null) {
            return window.getWwd();
        }
        return null;
    }

    /**
     * Replaces the content of the given panel by a message when WorldWind could not be initialised,
     * e.g. because OpenGL is not available (remote desktop, some virtual machines).
     */
    protected void showInitError(final JPanel mainPane, final String viewName, final Throwable e) {
        SystemUtils.LOG.log(Level.WARNING, "Unable to initialize " + viewName, e);
        wwjPanel = null;
        mainPane.removeAll();
        final JLabel label = new JLabel("<html><center>" + viewName + " is not available.<br>" +
                                                "WorldWind could not be initialised: " + e.getMessage() +
                                                "</center></html>", SwingConstants.CENTER);
        mainPane.add(label, BorderLayout.CENTER);
        mainPane.revalidate();
        mainPane.repaint();
    }

    /**
     * Starts following the products and selections of SNAP. Called on the EDT once the WorldWind panel exists and
     * whenever the view is reopened.
     */
    protected void connectToSnap() {
        if (getWwd() == null || productManagerListener != null || !isOpened()) {
            return;
        }
        final SnapApp snapApp = SnapApp.getDefault();
        productManagerListener = new WWProductManagerListener(this);
        productNodeHandler = (oldValue, newValue) -> setSelectedProduct(newValue != null ? newValue.getProduct() : null);
        rasterHandler = (oldValue, newValue) -> setSelectedRaster(newValue);
        sceneViewHandler = (oldValue, newValue) -> setSelectedSceneView(newValue);

        snapApp.getProductManager().addListener(productManagerListener);
        snapApp.getSelectionSupport(ProductNode.class).addHandler(productNodeHandler);
        snapApp.getSelectionSupport(RasterDataNode.class).addHandler(rasterHandler);
        snapApp.getSelectionSupport(ProductSceneView.class).addHandler(sceneViewHandler);

        setProducts(snapApp.getProductManager().getProducts());
        setSelectedProduct(snapApp.getSelectedProduct(VIEW));
        setSelectedSceneView(snapApp.getSelectedProductSceneView());
    }

    protected void disconnectFromSnap() {
        if (productManagerListener == null) {
            return;
        }
        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().removeListener(productManagerListener);
        snapApp.getSelectionSupport(ProductNode.class).removeHandler(productNodeHandler);
        snapApp.getSelectionSupport(RasterDataNode.class).removeHandler(rasterHandler);
        snapApp.getSelectionSupport(ProductSceneView.class).removeHandler(sceneViewHandler);
        productManagerListener = null;
        productNodeHandler = null;
        rasterHandler = null;
        sceneViewHandler = null;

        setSelectedProduct(null);
        for (Product product : snapApp.getProductManager().getProducts()) {
            removeProduct(product);
        }
    }

    @Override
    protected void componentOpened() {
        super.componentOpened();
        connectToSnap();
    }

    @Override
    protected void componentClosed() {
        disconnectFromSnap();
        super.componentClosed();
    }

    public Product getSelectedProduct() {
        final List<WWLayer> layerList = getWWLayers();
        for (WWLayer wwLayer : layerList) {
            return wwLayer.getSelectedProduct();
        }
        return null;
    }

    private void gotoProduct(final Product product) {
        if (product == null) return;

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if (geoCoding == null) {
            for (RasterDataNode band : product.getBands()) {
                if (band.getGeoCoding() != null) {
                    gotoCentre(band.getGeoCoding(), band.getRasterWidth(), band.getRasterHeight());
                    return;
                }
            }
            return;
        }
        gotoCentre(geoCoding, product.getSceneRasterWidth(), product.getSceneRasterHeight());
    }

    private void gotoRaster(final RasterDataNode raster) {
        if (raster == null) return;

        final GeoCoding geoCoding = raster.getGeoCoding();
        if(geoCoding == null) {
            gotoProduct(raster.getProduct());
            return;
        }
        gotoCentre(geoCoding, raster.getRasterWidth(), raster.getRasterHeight());
    }

    private void gotoCentre(final GeoCoding geoCoding, final int width, final int height) {
        final View theView = getWwd().getView();
        final Position origPos = theView.getEyePosition();
        if (origPos == null) {
            return;
        }
        final GeoPos centre = geoCoding.getGeoPos(new PixelPos(width / 2.0, height / 2.0), null);
        if (centre == null || !centre.isValid()) {
            return;
        }
        centre.normalize();
        theView.setEyePosition(Position.fromDegrees(centre.getLat(), centre.getLon(), origPos.getElevation()));
    }

    public void setSelectedProduct(final Product product) {
        final WorldWindowGLCanvas wwd = getWwd();
        if (wwd == null)
            return;
        if (product == getSelectedProduct() && eyePosition != null && eyePosition.equals(wwd.getView().getEyePosition()))
            return;

        List<WWLayer> wwLayers = getWWLayers();
        for(WWLayer wwLayer : wwLayers) {
            wwLayer.setSelectedProduct(product);
        }

        if (isVisible()) {
            gotoProduct(product);
            wwd.redraw();
            eyePosition = wwd.getView().getEyePosition();
        }
    }

    public void setSelectedRaster(final RasterDataNode raster) {
        final WorldWindowGLCanvas wwd = getWwd();
        if (wwd == null)
            return;

        List<WWLayer> wwLayers = getWWLayers();
        for(WWLayer wwLayer : wwLayers) {
            wwLayer.setSelectedRaster(raster);
        }

        if (isVisible()) {
            gotoRaster(raster);
            wwd.redraw();
            eyePosition = wwd.getView().getEyePosition();
        }
    }

    public void setSelectedSceneView(final ProductSceneView view) {
        final WorldWindowGLCanvas wwd = getWwd();
        if (wwd == null)
            return;

        for (WWLayer wwLayer : getWWLayers()) {
            try {
                wwLayer.setSelectedSceneView(view);
            } catch (Exception e) {
                SystemUtils.LOG.log(Level.WARNING, "WorldWind unable to follow the selected image view", e);
            }
        }
        if (isVisible()) {
            wwd.redraw();
        }
    }

    protected List<WWLayer> getWWLayers() {
        final WorldWindowGLCanvas wwd = getWwd();
        if (wwd == null) {
            return Collections.emptyList();
        }
        final List<WWLayer> wwLayers = new ArrayList<>();
        final LayerList layerList = wwd.getModel().getLayers();
        layerList.stream().filter(layer -> layer instanceof WWLayer).forEach(layer -> {
            wwLayers.add((WWLayer) layer);
        });
        return wwLayers;
    }

    /**
     * The layers that show a product: the layers intended for it, or else the suitable ones; complementary layers
     * (pins, vector data, full-resolution images) show it in either case.
     */
    static List<WWLayer> getSuitableLayers(List<WWLayer> wwLayers, final Product product) {

        final List<WWLayer> intendedLayers = new ArrayList<>();
        final List<WWLayer> suitableLayers = new ArrayList<>();
        final List<WWLayer> complementaryLayers = new ArrayList<>();
        for (WWLayer wwLayer : wwLayers) {
            BaseLayer.Suitability suitability = wwLayer.getSuitability(product);
            if (suitability == BaseLayer.Suitability.INTENDED) {
                intendedLayers.add(wwLayer);
            } else if (suitability == BaseLayer.Suitability.SUITABLE) {
                suitableLayers.add(wwLayer);
                if (wwLayer.isComplementary()) {
                    complementaryLayers.add(wwLayer);
                }
            }
        }
        if (intendedLayers.isEmpty()) {
            return suitableLayers;
        }
        intendedLayers.addAll(complementaryLayers);
        return intendedLayers;
    }

    public void setProducts(final Product[] products) {
        WorldWindowGLCanvas wwd = getWwd();
        if (wwd == null)
            return;
        List<WWLayer> wwLayers = getWWLayers();
        for (Product prod : products) {
            List<WWLayer> suitableLayers = getSuitableLayers(wwLayers, prod);
            for(WWLayer wwLayer : suitableLayers) {
                try {
                    wwLayer.addProduct(prod, wwd);
                } catch (Exception e) {
                    SnapApp.getDefault().handleError("WorldWind unable to add product " + prod.getName(), e);
                }
            }
        }
    }

    public void removeProduct(final Product product) {
        final WorldWindowGLCanvas wwd = getWwd();
        if (wwd == null)
            return;
        if (getSelectedProduct() == product)
            setSelectedProduct(null);

        List<WWLayer> wwLayers = getWWLayers();
        for(WWLayer wwLayer : wwLayers) {
            try {
                wwLayer.removeProduct(product);
            } catch (Exception e) {
                SystemUtils.LOG.log(Level.WARNING, "WorldWind unable to remove product " + product.getName(), e);
            }
        }

        if (isVisible()) {
            wwd.redraw();
        }
    }
}
