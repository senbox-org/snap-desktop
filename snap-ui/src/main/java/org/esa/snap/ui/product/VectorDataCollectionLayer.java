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

package org.esa.snap.ui.product;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.layer.ProductLayerContext;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VectorDataCollectionLayer extends CollectionLayer {

    public static final String ID = VectorDataCollectionLayer.class.getName();

    private final ProductNodeListener pnl;
    private final transient WeakReference<ProductNodeGroup<VectorDataNode>> reference;
    private final ProductLayerContext plc;

    public VectorDataCollectionLayer(VectorDataCollectionLayerType layerType,
                                     ProductNodeGroup<VectorDataNode> vectorDataGroup,
                                     PropertySet configuration,
                                     ProductLayerContext plc) {
        super(layerType, configuration, "Vector data");
        Assert.notNull(vectorDataGroup, "vectorDataGroup");

        reference = new WeakReference<>(vectorDataGroup);
        pnl = new PNL();
        this.plc = plc;

        setId(ID);
        Product product = vectorDataGroup.getProduct();
        if (product != null) {
            product.addProductNodeListener(pnl);
        }
    }

    @Override
    public void disposeLayer() {
        ProductNodeGroup<VectorDataNode> productNodeGroup = reference.get();
        if (productNodeGroup != null) {
            Product product = productNodeGroup.getProduct();
            if (product != null) {
                product.removeProductNodeListener(pnl);
            }
        }
        reference.clear();
    }

    private Layer createLayer(final VectorDataNode vectorDataNode) {
        final Layer layer = VectorDataLayerType.createLayer(plc, vectorDataNode);
        layer.setVisible(false);
        return layer;
    }

    private Layer getLayer(final VectorDataNode vectorDataNode) {
        LayerFilter layerFilter = VectorDataLayerFilterFactory.createNodeFilter(vectorDataNode);
        return LayerUtils.getChildLayer(LayerUtils.getRootLayer(this), LayerUtils.SEARCH_DEEP, layerFilter);
    }

    synchronized void updateChildren() {
        final ProductNodeGroup<VectorDataNode> vectorDataGroup = reference.get();
        if (vectorDataGroup == null) {
            return;
        }
        // Collect all current vector layers
        LayerFilter layerFilter = layer -> {
            PropertySet conf = layer.getConfiguration();
            return conf.isPropertyDefined(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA) && conf.getValue(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA) != null;
        };
        List<Layer> vectorLayers = LayerUtils.getChildLayers(LayerUtils.getRootLayer(this), LayerUtils.SEARCH_DEEP, layerFilter);
        final Map<VectorDataNode, Layer> currentLayers = new HashMap<>();
        for (final Layer child : vectorLayers) {
            final String name = child.getConfiguration().getValue(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA);
            final VectorDataNode vectorDataNode = vectorDataGroup.get(name);
            currentLayers.put(vectorDataNode, child);
        }

        // Align vector layers with available vectors
        final Set<Layer> unusedLayers = new HashSet<>(vectorLayers);
        VectorDataNode[] vectorDataNodes = vectorDataGroup.toArray(new VectorDataNode[vectorDataGroup.getNodeCount()]);
        for (final VectorDataNode vectorDataNode : vectorDataNodes) {
            Layer layer = currentLayers.get(vectorDataNode);
            if (layer != null) {
                unusedLayers.remove(layer);
            } else {
                layer = createLayer(vectorDataNode);
                getChildren().add(layer);
            }
        }

        // Remove unused layers
        for (Layer layer : unusedLayers) {
            layer.dispose();
            Layer layerParent = layer.getParent();
            if (layerParent != null) {
                layerParent.getChildren().remove(layer);
            }
        }
    }

    private class PNL implements ProductNodeListener {

        @Override
        public synchronized void nodeChanged(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof VectorDataNode) {
                final VectorDataNode vectorDataNode = (VectorDataNode) sourceNode;
                final Layer layer = getLayer(vectorDataNode);
                if (layer != null) {
                    layer.regenerate();
                }
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            nodeChanged(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            updateChildren();
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            updateChildren();
        }
    }
}
