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
import com.bc.ceres.glayer.support.AbstractLayerListener;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.layer.LayerEditor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

@TopComponent.Description(
        preferredID = "LayerEditorTopComponent",
        iconBase = "org/esa/snap/rcp/icons/LayerEditor.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = PackageDefaults.LAYER_EDITOR_MODE,
        openAtStartup = PackageDefaults.LAYER_EDITOR_OPEN,
        position = PackageDefaults.LAYER_EDITOR_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.layermanager.LayerEditorTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Menu/Layer", position = 410)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_LayerEditorTopComponent_Name",
        preferredID = "LayerEditorTopComponent"
)
@NbBundle.Messages({
        "CTL_LayerEditorTopComponent_Name=" + PackageDefaults.LAYER_EDITOR_NAME,
        "CTL_LayerEditorTopComponent_HelpId=showLayerEditorWnd"
})
/**
 * Layer manager tool view.
 * <p>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Norman Fomferra
 */
public class LayerEditorTopComponent extends AbstractLayerTopComponent {

    private LayerEditor activeEditor;
    private LayerHandler layerHandler;

    @Override
    protected String getTitle() {
        return Bundle.CTL_LayerEditorTopComponent_Name();
    }

    @Override
    protected String getHelpId() {
        return Bundle.CTL_LayerEditorTopComponent_HelpId();
    }

    protected void initUI() {
        layerHandler = new LayerHandler();
        super.initUI();
//        add(activeEditor.createControl(newLayer), BorderLayout.CENTER);
    }

    @Override
    protected void layerSelectionChanged(Layer oldLayer, Layer newLayer) {
        if (oldLayer != null) {
            oldLayer.removeListener(layerHandler);
        }

        if (getComponentCount() > 0) {
            remove(0);
        }

        LayerEditor oldEditor = activeEditor;

        if (newLayer != null) {
            activeEditor = getLayerEditor(newLayer);
            setDisplayName("Layer Editor - " + newLayer.getName());
        } else {
            activeEditor = LayerEditor.EMPTY;
            setDisplayName("Layer Editor");
        }

        if (oldEditor != null) {
            oldEditor.handleEditorDetached();
        }
        add(activeEditor.createControl(newLayer), BorderLayout.CENTER);
        activeEditor.handleEditorAttached();
        activeEditor.handleLayerContentChanged();

        validate();
        repaint();

        if (newLayer != null) {
            newLayer.addListener(layerHandler);
        }

    }

    private LayerEditor getLayerEditor(Layer layer) {
        LayerEditor layerEditor = layer.getExtension(LayerEditor.class);
        if (layerEditor != null) {
            return layerEditor;
        }

        layerEditor = layer.getLayerType().getExtension(LayerEditor.class);
        if (layerEditor != null) {
            return layerEditor;
        }

        return LayerEditor.EMPTY;
    }

    private class LayerHandler extends AbstractLayerListener {

        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
            activeEditor.handleLayerContentChanged();
        }

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            activeEditor.handleLayerContentChanged();
        }

        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            activeEditor.handleLayerContentChanged();
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
            activeEditor.handleLayerContentChanged();
        }
    }
}