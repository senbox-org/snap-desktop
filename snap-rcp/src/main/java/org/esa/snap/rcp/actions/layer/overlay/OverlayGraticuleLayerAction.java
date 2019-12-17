/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.layer.overlay;

import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * @author Marco Peters
 * @author Muhammad.bc
 */
@ActionID(category = "View", id = "OverlayGraticuleLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayGraticuleLayerActionName")
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.OVERLAY_GRATICULE_MENU_PATH,
                position = PackageDefaults.OVERLAY_GRATICULE_MENU_POSITION),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.OVERLAY_GRATICULE_TOOLBAR_NAME,
                position = PackageDefaults.OVERLAY_GRATICULE_TOOLBAR_POSITION)
})
@NbBundle.Messages({
        "CTL_OverlayGraticuleLayerActionName=" + PackageDefaults.OVERLAY_GRATICULE_NAME,
        "CTL_OverlayGraticuleLayerActionToolTip=" + PackageDefaults.OVERLAY_GRATICULE_DESCRIPTION
})
public final class OverlayGraticuleLayerAction extends AbstractOverlayAction {

    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlayGraticuleLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/GraticuleOverlay.gif", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.OVERLAY_GRATICULE_ICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayGraticuleLayerActionToolTip());
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        return view.isGraticuleOverlayEnabled();
    }

    @Override
    protected boolean getActionEnabledState(ProductSceneView view) {
        return ProductUtils.canGetPixelPos(view.getRaster());
    }

    @Override
    protected void setOverlayEnableState(ProductSceneView view) {
        view.setGraticuleOverlayEnabled(!getActionSelectionState(view));
    }


}
