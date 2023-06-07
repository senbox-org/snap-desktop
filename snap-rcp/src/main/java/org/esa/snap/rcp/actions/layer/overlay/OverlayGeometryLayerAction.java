/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.layer.overlay;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.VectorDataLayerFilterFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

import java.util.List;

/**
 * @author Marco Peters
 * @author Norman Fomferra
 * @author Muhammad.bc
 */
@ActionID(category = "View", id = "OverlayGeometryLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayGeometryLayerActionName")
@ActionReferences({
        @ActionReference(path = "Menu/Layer", position = 10),
        @ActionReference(path = "Toolbars/" + PackageDefaults.GEOMETRY_TOGGLE_TOOLBAR, position = 10),
})
@NbBundle.Messages({
        "CTL_OverlayGeometryLayerActionName=Geometry Overlay",
        "CTL_OverlayGeometryLayerActionToolTip=Show/hide geometry overlay for the selected image"
})
public final class OverlayGeometryLayerAction extends AbstractOverlayAction {

    private final LayerFilter geometryFilter = VectorDataLayerFilterFactory.createGeometryFilter();


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
    protected boolean getActionEnabledState(ProductSceneView view) {
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
