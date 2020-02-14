/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.grapheditor.gpf.ui.worldmap;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NestWorldMapPaneDataModel {

    public static final String PROPERTY_LAYER = "layer";
    public static final String PROPERTY_SELECTED_PRODUCT = "selectedProduct";
    public static final String PROPERTY_PRODUCTS = "products";
    public static final String PROPERTY_ADDITIONAL_GEO_BOUNDARIES = "additionalGeoBoundaries";
    public static final String PROPERTY_SELECTED_GEO_BOUNDARIES = "selectedGeoBoundaries";
    public static final String PROPERTY_AUTO_ZOOM_ENABLED = "autoZoomEnabled";

    private PropertyChangeSupport changeSupport;
    private static final LayerType layerType = LayerTypeRegistry.getLayerType("org.esa.snap.worldmap.BlueMarbleLayerType");
    private Layer worldMapLayer;
    private Product selectedProduct;
    private boolean autoZoomEnabled;
    private final List<Product> productList = new ArrayList<>();
    private Boundary[] additionalGeoBoundaryList;
    private Boundary[] selectedGeoBoundaryList;

    private final GeoPos selectionBoxStart = new GeoPos();
    private final GeoPos selectionBoxEnd = new GeoPos();

    public NestWorldMapPaneDataModel() {
        autoZoomEnabled = false;
    }

    public Layer getWorldMapLayer(LayerContext context) {
        if (worldMapLayer == null) {
            worldMapLayer = layerType.createLayer(context, new PropertyContainer());
        }
        return worldMapLayer;
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product product) {
        Product oldSelectedProduct = selectedProduct;
        if (oldSelectedProduct != product) {
            selectedProduct = product;
            firePropertyChange(PROPERTY_SELECTED_PRODUCT, oldSelectedProduct, selectedProduct);
        }
    }

    public void setSelectionBoxStart(final double lat, final double lon) {
        selectionBoxStart.setLocation(lat, lon);
    }

    public void setSelectionBoxEnd(final double lat, final double lon) {
        selectionBoxEnd.setLocation(lat, lon);
    }

    public GeoPos[] getSelectionBox() {
        final GeoPos[] selectionBox = new GeoPos[5];
        selectionBox[0] = selectionBoxStart;
        selectionBox[1] = new GeoPos(selectionBoxStart.getLat(), selectionBoxEnd.getLon());
        selectionBox[2] = selectionBoxEnd;
        selectionBox[3] = new GeoPos(selectionBoxEnd.getLat(), selectionBoxStart.getLon());
        selectionBox[4] = selectionBoxStart;
        return selectionBox;
    }

    public Boundary getSelectionBoundary() {
        return new Boundary(getSelectionBox());
    }

    public Product[] getProducts() {
        return productList.toArray(new Product[0]);
    }

    public void setProducts(Product[] products) {
        final Product[] oldProducts = getProducts();
        productList.clear();
        if (products != null) {
            productList.addAll(Arrays.asList(products));
        }
        firePropertyChange(PROPERTY_PRODUCTS, oldProducts, getProducts());
    }

    public Boundary[] getAdditionalGeoBoundaries() {
        if (additionalGeoBoundaryList == null) {
            additionalGeoBoundaryList = new Boundary[0];
        }
        return additionalGeoBoundaryList;
    }

    public void setAdditionalGeoBoundaries(final GeoPos[][] geoBoundarys) {
        final Boundary[] oldGeoBoundarys = getAdditionalGeoBoundaries();
        if (geoBoundarys != null) {
            final List<Boundary> boundaryList = new ArrayList<>();
            for (GeoPos[] geoBoundary : geoBoundarys) {
                boundaryList.add(new Boundary(geoBoundary));
            }
            additionalGeoBoundaryList = boundaryList.toArray(new Boundary[0]);
        }
        firePropertyChange(PROPERTY_ADDITIONAL_GEO_BOUNDARIES, oldGeoBoundarys, additionalGeoBoundaryList);
    }

    public Boundary[] getSelectedGeoBoundaries() {
        if (selectedGeoBoundaryList == null) {
            selectedGeoBoundaryList = new Boundary[0];
        }
        return selectedGeoBoundaryList;
    }

    public void setSelectedGeoBoundaries(final GeoPos[][] geoBoundarys) {
        final Boundary[] oldGeoBoundarys = getSelectedGeoBoundaries();
        if (geoBoundarys != null) {
            final List<Boundary> boundaryList = new ArrayList<>();
            for (GeoPos[] geoBoundary : geoBoundarys) {
                boundaryList.add(new Boundary(geoBoundary));
            }
            selectedGeoBoundaryList = boundaryList.toArray(new Boundary[0]);
        }
        firePropertyChange(PROPERTY_SELECTED_GEO_BOUNDARIES, oldGeoBoundarys, selectedGeoBoundaryList);
    }

    public void addModelChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public boolean isAutoZommEnabled() {
        return autoZoomEnabled;
    }

    public void setAutoZoomEnabled(boolean autoZoomEnabled) {
        final boolean oldAutoZommEnabled = isAutoZommEnabled();
        if (oldAutoZommEnabled != autoZoomEnabled) {
            this.autoZoomEnabled = autoZoomEnabled;
            firePropertyChange(PROPERTY_AUTO_ZOOM_ENABLED, oldAutoZommEnabled, autoZoomEnabled);
        }
    }

    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport != null) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public static class Boundary {
        public final boolean isClosed;
        public final GeoPos[] geoBoundary;

        public Boundary(final GeoPos[] geoBoundary) {
            ProductUtils.normalizeGeoPolygon(geoBoundary);
            this.geoBoundary = geoBoundary;
            this.isClosed = isClosedPath(geoBoundary);
        }

        private static boolean isClosedPath(final GeoPos[] geoBoundary) {
            return geoBoundary.length > 0 && geoBoundary[0].equals(geoBoundary[geoBoundary.length - 1]);
        }
    }
}
