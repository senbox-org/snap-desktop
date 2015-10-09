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
package org.esa.snap.rcp.layermanager;

import com.bc.ceres.glayer.Layer;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.product.ProductSceneView;
import org.netbeans.api.annotations.common.NonNull;

import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Layer manager tool view.
 * <p>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
public abstract class AbstractLayerTopComponent extends ToolTopComponent {

    private ProductSceneView selectedView;
    private Layer selectedLayer;
    private final SelectedLayerPCL selectedLayerPCL;

    protected AbstractLayerTopComponent() {
        selectedLayerPCL = new SelectedLayerPCL();

        initUI();
    }

    protected ProductSceneView getSelectedView() {
        return selectedView;
    }

    protected Layer getSelectedLayer() {
        return selectedLayer;
    }

    protected abstract String getTitle();

    protected abstract String getHelpId();

    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(4, 4, 4, 4));
        setDisplayName(getTitle());
        setSelectedView(getSelectedProductSceneView());
    }

    /**
     * A view opened.
     *
     * @param view The view.
     */
    protected void viewOpened(ProductSceneView view) {
    }

    /**
     * A view closed.
     *
     * @param view The view.
     */
    protected void viewClosed(ProductSceneView view) {
    }

    /**
     * The selected view changed.
     *
     * @param oldView The old selected view. May be null.
     * @param newView The new selected view. May be null.
     */
    protected void viewSelectionChanged(ProductSceneView oldView, ProductSceneView newView) {
    }

    /**
     * The selected layer changed.
     *
     * @param oldLayer The old selected layer. May be null.
     * @param newLayer The new selected layer. May be null.
     */
    protected void layerSelectionChanged(Layer oldLayer, Layer newLayer) {
    }

    private void setSelectedView(final ProductSceneView newView) {
        ProductSceneView oldView = selectedView;
        if (newView != oldView) {
            if (oldView != null) {
                oldView.removePropertyChangeListener("selectedLayer", selectedLayerPCL);
            }
            if (newView != null) {
                newView.addPropertyChangeListener("selectedLayer", selectedLayerPCL);
            }
            selectedView = newView;
            viewSelectionChanged(oldView, newView);
            setSelectedLayer(newView != null ? newView.getSelectedLayer() : null);
        }
    }

    protected void setSelectedLayer(final Layer newLayer) {
        Layer oldLayer = selectedLayer;
        if (newLayer != oldLayer) {
            selectedLayer = newLayer;
            layerSelectionChanged(oldLayer, newLayer);
        }
    }

    @Override
    protected void productSceneViewSelected(@NonNull ProductSceneView view) {
        setSelectedView(view);
        viewOpened(view);
    }

    @Override
    protected void productSceneViewDeselected(@NonNull ProductSceneView view) {
        setSelectedView(null);
    }

    private class SelectedLayerPCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (getSelectedView() != null) {
                setSelectedLayer(getSelectedView().getSelectedLayer());
            }
        }
    }
}
