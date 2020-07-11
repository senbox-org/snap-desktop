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
import com.bc.ceres.swing.selection.AbstractSelectionContext;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.support.DefaultSelection;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import java.awt.BorderLayout;
import java.util.WeakHashMap;

@TopComponent.Description(
        preferredID = "LayerManagerTopComponent",
        iconBase = "org/esa/snap/rcp/icons/LayerManager.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = PackageDefaults.LAYER_MANAGER_MODE,
        openAtStartup = PackageDefaults.LAYER_MANAGER_OPEN,
        position = PackageDefaults.LAYER_MANAGER_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.layermanager.LayerManagerTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Layer", position = 400, separatorBefore = 399),
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_LayerManagerTopComponent_Name",
        preferredID = "LayerManagerTopComponent"
)
@NbBundle.Messages({
        "CTL_LayerManagerTopComponent_Name=" + PackageDefaults.LAYER_MANAGER_NAME,
        "CTL_LayerManagerTopComponent_HelpId=showLayerManagerWnd"
})
/**
 * Layer manager tool view.
 * <p>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Norman Fomferra
 */
public class LayerManagerTopComponent extends AbstractLayerTopComponent {

    private WeakHashMap<ProductSceneView, LayerManagerForm> layerManagerMap;
    private LayerManagerForm activeForm;

    private LayerSelectionContext selectionContext;

    @Override
    protected void initUI() {
        layerManagerMap = new WeakHashMap<>();
        selectionContext = new LayerSelectionContext();
        super.initUI();
    }

    @Override
    protected void viewClosed(ProductSceneView view) {
        layerManagerMap.remove(view);
    }

    @Override
    protected void viewSelectionChanged(ProductSceneView oldView, ProductSceneView newView) {
        realizeActiveForm();
    }

    @Override
    protected void layerSelectionChanged(Layer oldLayer, Layer selectedLayer) {
        if (activeForm != null) {
            activeForm.updateFormControl();
            selectionContext.fireSelectionChange(new DefaultSelection<>(selectedLayer));
        }
    }

    @Override
    protected String getTitle() {
        return Bundle.CTL_LayerManagerTopComponent_Name();
    }

    @Override
    protected String getHelpId() {
        return Bundle.CTL_LayerManagerTopComponent_HelpId();
    }

    private void realizeActiveForm() {
        if (getComponentCount() > 0) {
            remove(0);
        }

        if (getSelectedView() != null) {
            activeForm = getOrCreateActiveForm(getSelectedView());
            add(activeForm.getFormControl(), BorderLayout.CENTER);
        } else {
            activeForm = null;
        }

        validate();
        repaint();
    }

    protected LayerManagerForm getOrCreateActiveForm(ProductSceneView view) {
        if (layerManagerMap.containsKey(view)) {
            activeForm = layerManagerMap.get(view);
        } else {
            activeForm = new LayerManagerForm(this);
            layerManagerMap.put(view, activeForm);
        }
        return activeForm;
    }

    private class LayerSelectionContext extends AbstractSelectionContext {

        @Override
        public void setSelection(Selection selection) {
            Object selectedValue = selection.getSelectedValue();
            if (selectedValue instanceof Layer) {
                setSelectedLayer((Layer) selectedValue);
            }
        }

        @Override
        public Selection getSelection() {
            Layer selectedLayer = getSelectedLayer();
            if (selectedLayer != null) {
                return new DefaultSelection<>(selectedLayer);
            } else {
                return DefaultSelection.EMPTY;
            }
        }

        @Override
        protected void fireSelectionChange(Selection selection) {
            super.fireSelectionChange(selection);
        }
    }
}
