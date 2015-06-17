/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.view.overlay;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.framework.ui.product.VectorDataLayerFilterFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

import javax.swing.Action;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @author Marco Peters
 * @author Norman Fomferra
 */
@ActionID(category = "View", id = "OverlayGeometryLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayGeometryLayerActionName", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/View/Overlay", position = 10),
        @ActionReference(path = "Toolbars/Overlay", position = 10),
        @ActionReference(path = "Context/View", position = 10)
})
@NbBundle.Messages({
        "CTL_OverlayGeometryLayerActionName=Toggle Geometry Overlay",
        "CTL_OverlayGeometryLayerActionToolTip=Show/hide geometry overlay for the selected image"
})
public final class OverlayGeometryLayerAction extends AbstractOverlayAction {

    private final LayerFilter geometryFilter = VectorDataLayerFilterFactory.createGeometryFilter();
    private final LayerListener layerListener;
    private WeakReference<ProductSceneView> lastView;


    public OverlayGeometryLayerAction() {
        this(Utilities.actionsGlobalContext());
    }

    public OverlayGeometryLayerAction(Lookup lkp) {
        super(lkp);
        layerListener = new MyLayerListener();
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new OverlayGeometryLayerAction(lkp);
    }

    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlayGeometryLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/ShapeOverlay.gif", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/ShapeOverlay24.gif", false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayGeometryLayerActionToolTip());
    }

    @Override
    protected void selectedProductSceneViewChanged(ProductSceneView newView) {
        ProductSceneView oldView = lastView != null ? lastView.get() : null;
        if (oldView != null) {
            oldView.getRootLayer().removeListener(layerListener);
        }
        if (newView != null) {
            newView.getRootLayer().addListener(layerListener);
        }

        if (newView != null) {
            lastView = new WeakReference<>(newView);
        } else {
            if (lastView != null) {
                lastView.clear();
                lastView = null;
            }
        }
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        List<Layer> childLayers = getGeometryLayers(view);
        return childLayers.stream().filter(Layer::isVisible).findAny().isPresent();
    }

    @Override
    protected boolean getActionEnableState(ProductSceneView view) {
        List<Layer> childLayers = getGeometryLayers(view);
        return !childLayers.isEmpty();
    }

    @Override
    protected void setOverlayEnableState(ProductSceneView view) {
        if (view != null) {
            List<Layer> childLayers = getGeometryLayers(view);
            childLayers.stream().forEach(layer -> layer.setVisible(isSelected()));
        }

    }

    private List<Layer> getGeometryLayers(ProductSceneView sceneView) {
        return LayerUtils.getChildLayers(sceneView.getRootLayer(), LayerUtils.SEARCH_DEEP, geometryFilter);
    }

    private class MyLayerListener extends AbstractLayerListener {
        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            updateActionState();
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
            updateActionState();
        }
    }
}
