/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.layer.overlay;

import org.esa.snap.core.datamodel.Product;
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
 */
@ActionID(category = "View", id = "OverlayGcpLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayGcpLayerActionName")
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.OVERLAY_GCP_MENU_PATH,
                position = PackageDefaults.OVERLAY_GCP_MENU_POSITION),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.OVERLAY_GCP_TOOLBAR_NAME,
                position = PackageDefaults.OVERLAY_GCP_TOOLBAR_POSITION)
})
@NbBundle.Messages({
        "CTL_OverlayGcpLayerActionName=" + PackageDefaults.OVERLAY_GCP_NAME,
        "CTL_OverlayGcpLayerActionToolTip=" + PackageDefaults.OVERLAY_GCP_DESCRIPTION
})
public final class OverlayGcpLayerAction extends AbstractOverlayAction {
    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlayGcpLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/GcpOverlay.gif", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.OVERLAY_GCP_ICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayGcpLayerActionToolTip());
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        return view.isGcpOverlayEnabled();
    }

    @Override
    protected boolean getActionEnabledState(ProductSceneView view) {
        Product product = view.getProduct();
        return product != null && product.getGcpGroup().getNodeCount() > 0;
    }

    @Override
    protected void setOverlayEnableState(ProductSceneView view) {
        view.setGcpOverlayEnabled(!getActionSelectionState(view));
    }

}
