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

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.worldwind.layers.BaseLayer;
import org.esa.snap.worldwind.layers.WWLayer;
import org.openide.windows.WindowManager;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Base WorldWind ToolView
 */
public abstract class WWBaseToolView extends ToolTopComponent {

    private final Dimension canvasSize = new Dimension(800, 600);
    protected AppPanel wwjPanel = null;
    private Position eyePosition = null;

    public WWBaseToolView() {
    }

    AppPanel createWWPanel(final WorldWindowGLCanvas shareWith,
                           final boolean includeStatusBar, final boolean flatWorld, final boolean removeExtraLayers) {
        wwjPanel = new AppPanel(shareWith, includeStatusBar, flatWorld, removeExtraLayers);
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

    public Product getSelectedProduct() {
        final List<WWLayer> layerList = getWWLayers();
        for (WWLayer wwLayer : layerList) {
            return wwLayer.getSelectedProduct();
        }
        return null;
    }

    private void gotoProduct(final Product product) {
        if (product == null) return;

        final View theView = getWwd().getView();
        final Position origPos = theView.getEyePosition();
        if(origPos != null) {
            final GeoCoding geoCoding = product.getSceneGeoCoding();
            if(geoCoding == null && product.getBandAt(0).getGeoCoding() != null) {
                gotoRaster(product.getBandAt(0));
                return;
            }
            if (geoCoding != null) {
                final GeoPos centre = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() / 2.0,
                        product.getSceneRasterHeight() / 2.0), null);
                centre.normalize();
                theView.setEyePosition(Position.fromDegrees(centre.getLat(), centre.getLon(), origPos.getElevation()));
            }
        }
    }

    private void gotoRaster(final RasterDataNode raster) {
        if (raster == null) return;

        final GeoCoding geoCoding = raster.getGeoCoding();
        if(geoCoding == null) {
            gotoProduct(raster.getProduct());
            return;
        }

        final View theView = getWwd().getView();
        final Position origPos = theView.getEyePosition();
        if (origPos != null) {
            final GeoPos centre = geoCoding.getGeoPos(new PixelPos(raster.getRasterWidth() / 2.0,
                    raster.getRasterHeight() / 2.0), null);
            centre.normalize();
            theView.setEyePosition(Position.fromDegrees(centre.getLat(), centre.getLon(), origPos.getElevation()));
        }
    }

    public void setSelectedProduct(final Product product) {
        if (product == getSelectedProduct() && eyePosition == getWwd().getView().getEyePosition())
            return;

        List<WWLayer> wwLayers = getWWLayers();
        for(WWLayer wwLayer : wwLayers) {
            wwLayer.setSelectedProduct(product);
        }

        if (isVisible()) {
            gotoProduct(product);
            getWwd().redrawNow();
            eyePosition = getWwd().getView().getEyePosition();
        }
    }

    public void setSelectedRaster(final RasterDataNode raster) {

        List<WWLayer> wwLayers = getWWLayers();
        for(WWLayer wwLayer : wwLayers) {
            wwLayer.setSelectedRaster(raster);
        }

        if (isVisible()) {
            gotoRaster(raster);
            getWwd().redrawNow();
            eyePosition = getWwd().getView().getEyePosition();
        }
    }

    protected List<WWLayer> getWWLayers() {
        final List<WWLayer> wwLayers = new ArrayList<>();
        final LayerList layerList = getWwd().getModel().getLayers();
        layerList.stream().filter(layer -> layer instanceof WWLayer).forEach(layer -> {
            wwLayers.add((WWLayer) layer);
        });
        return wwLayers;
    }

    private List<WWLayer> getSuitableLayers(List<WWLayer> wwLayers, final Product product) {

        final List<WWLayer> intendedLayers = new ArrayList<>();
        final List<WWLayer> suitableLayers = new ArrayList<>();
        for (WWLayer wwLayer : wwLayers) {
            BaseLayer.Suitability suitability = wwLayer.getSuitability(product);
            if (suitability == BaseLayer.Suitability.INTENDED) {
                intendedLayers.add(wwLayer);
            } else if (suitability == BaseLayer.Suitability.SUITABLE) {
                suitableLayers.add(wwLayer);
            }
        }
        return intendedLayers.isEmpty() ? suitableLayers : intendedLayers;
    }

    public void setProducts(final Product[] products) {
        WorldWindowGLCanvas wwd = getWwd();
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
        if (getSelectedProduct() == product)
            setSelectedProduct(null);

        List<WWLayer> wwLayers = getWWLayers();
        for(WWLayer wwLayer : wwLayers) {
            wwLayer.removeProduct(product);
        }

        if (isVisible()) {
            getWwd().redrawNow();
        }
    }
}
