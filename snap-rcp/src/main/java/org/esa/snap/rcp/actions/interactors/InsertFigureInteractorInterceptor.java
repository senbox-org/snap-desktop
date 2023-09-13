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

package org.esa.snap.rcp.actions.interactors;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.figure.AbstractInteractorInterceptor;
import com.bc.ceres.swing.figure.Interactor;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.rcp.actions.vector.CreateVectorDataNodeAction;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.VectorDataLayer;
import org.esa.snap.ui.product.VectorDataLayerFilterFactory;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.util.List;


public class InsertFigureInteractorInterceptor extends AbstractInteractorInterceptor {

    @Override
    public boolean interactionAboutToStart(Interactor interactor, InputEvent inputEvent) {
        final ProductSceneView productSceneView = getProductSceneView(inputEvent);

        if (!productSceneView.isVectorOverlayEnabled()) {
            productSceneView.setVectorOverlayEnabled(true);
        }
//        if (productSceneView != null) {
//            List<Layer> childLayers = getGeometryLayers(productSceneView);
////                    childLayers.stream().forEach(layer -> layer.setVisible(isSelected()));
//            childLayers.stream().forEach(layer -> layer.setVisible(true));
//        }

        return getActiveVectorDataLayer(productSceneView) != null;


    }

    private List<Layer> getGeometryLayers(ProductSceneView sceneView) {
        final LayerFilter geometryFilter = VectorDataLayerFilterFactory.createGeometryFilter();

        return LayerUtils.getChildLayers(sceneView.getRootLayer(), LayerUtils.SEARCH_DEEP, geometryFilter);
    }

    public static VectorDataLayer getActiveVectorDataLayer(ProductSceneView productSceneView) {
        if (productSceneView == null) {
            return null;
        }

        if (!productSceneView.isVectorOverlayEnabled()) {
            productSceneView.setVectorOverlayEnabled(true);
        }

        final LayerFilter geometryFilter = VectorDataLayerFilterFactory.createGeometryFilter();

        Layer layer = productSceneView.getSelectedLayer();
        if (geometryFilter.accept(layer)) {
            layer.setVisible(true);
            return (VectorDataLayer) layer;
        }

        List<Layer> layers = LayerUtils.getChildLayers(productSceneView.getRootLayer(),
                                                       LayerUtils.SEARCH_DEEP, geometryFilter);

        VectorDataLayer vectorDataLayer;
        if (layers.isEmpty()) {
            VectorDataNode vectorDataNode = CreateVectorDataNodeAction.createDefaultVectorDataNode(productSceneView.getProduct());
            LayerFilter nodeFilter = VectorDataLayerFilterFactory.createNodeFilter(vectorDataNode);
            productSceneView.getVectorDataCollectionLayer(true);
            vectorDataLayer = (VectorDataLayer) LayerUtils.getChildLayer(productSceneView.getRootLayer(),
                                                                         LayerUtils.SEARCH_DEEP, nodeFilter);
        } else if (layers.size() == 1) {
            vectorDataLayer = (VectorDataLayer) layers.get(0);
        } else {
            vectorDataLayer = showSelectLayerDialog(productSceneView, layers);
        }
        if (vectorDataLayer == null) {
            // = Cancel
            return null;
        }
        productSceneView.setSelectedLayer(vectorDataLayer);
        if (productSceneView.getSelectedLayer() == vectorDataLayer) {
            vectorDataLayer.setVisible(true);
            return vectorDataLayer;
        }
        return null;
    }

    static private VectorDataLayer showSelectLayerDialog(ProductSceneView productSceneView, List<Layer> layers) {
        String[] layerNames = new String[layers.size()];
        for (int i = 0; i < layerNames.length; i++) {
            layerNames[i] = layers.get(i).getName();
        }
        JList<String> listBox = new JList<>(layerNames);
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new JLabel("Please select a vector data container:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(listBox), BorderLayout.CENTER);
        ModalDialog dialog = new ModalDialog(SwingUtilities.getWindowAncestor(productSceneView),
                                             "Select Vector Data Container",
                                             ModalDialog.ID_OK_CANCEL_HELP, "");
        dialog.setContent(panel);
        int i = dialog.show();
        if (i == ModalDialog.ID_OK) {
            final int index = listBox.getSelectedIndex();
            if (index >= 0) {
                return (VectorDataLayer) layers.get(index);
            }
        }
        return null;
    }

    private ProductSceneView getProductSceneView(InputEvent event) {
        ProductSceneView productSceneView = null;
        Component component = event.getComponent();
        while (component != null) {
            if (component instanceof ProductSceneView) {
                productSceneView = (ProductSceneView) component;
                break;
            }
            component = component.getParent();
        }
        return productSceneView;
    }

}
