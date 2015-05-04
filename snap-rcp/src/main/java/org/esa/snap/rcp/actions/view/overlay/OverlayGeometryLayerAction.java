/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.view.overlay;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
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
import java.util.List;

/**
 * @author Marco Peters
 */
@ActionID(category = "View", id = "OverlayGeometryLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayGeometryLayerActionName", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/View/Overlay", position = 10),
        @ActionReference(path = "Toolbars/Overlay", position = 10)
})
@NbBundle.Messages({
        "CTL_OverlayGeometryLayerActionName=Toggle Geometry Overlay",
        "CTL_OverlayGeometryLayerActionToolTip=Show/hide geometry overlay for the selected image"
})
public final class OverlayGeometryLayerAction extends AbstractOverlayAction {
    // todo - does not detect if figure is added to a view or removed from it.


    private final LayerFilter geometryFilter = VectorDataLayerFilterFactory.createGeometryFilter();

    public OverlayGeometryLayerAction() {
        this(Utilities.actionsGlobalContext());
    }

    public OverlayGeometryLayerAction(Lookup lkp) {
        super(lkp);
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

}
