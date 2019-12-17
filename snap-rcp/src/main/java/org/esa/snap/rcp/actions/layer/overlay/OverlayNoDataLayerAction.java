/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.layer.overlay;

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
@ActionID(category = "View", id = "OverlayNoDataLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayNoDataLayerActionName")
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.OVERLAY_NO_DATA_MENU_PATH,
                position = PackageDefaults.OVERLAY_NO_DATA_MENU_POSITION),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.OVERLAY_NO_DATA_TOOLBAR_NAME,
                position = PackageDefaults.OVERLAY_NO_DATA_TOOLBAR_POSITION)
})
@NbBundle.Messages({
        "CTL_OverlayNoDataLayerActionName=" + PackageDefaults.OVERLAY_NO_DATA_NAME,
        "CTL_OverlayNoDataLayerActionToolTip=" + PackageDefaults.OVERLAY_NO_DATA_DESCRIPTION
})
public final class OverlayNoDataLayerAction extends AbstractOverlayAction {
    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlayNoDataLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/NoDataOverlay.gif", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.OVERLAY_NO_DATA_ICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayNoDataLayerActionToolTip());
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        return view.isNoDataOverlayEnabled();
    }

    @Override
    protected boolean getActionEnabledState(ProductSceneView view) {
        return view.getRaster().isValidMaskUsed();
    }

    @Override
    protected void setOverlayEnableState(ProductSceneView view) {
        view.setNoDataOverlayEnabled(!getActionSelectionState(view));
    }


}
