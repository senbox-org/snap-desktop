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
 * @author Muhammad.bc
 */
@ActionID(category = "View", id = "OverlayPinLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayPinLayerActionName")
@ActionReferences({
        @ActionReference(path = "Menu/Layer", position = 30),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.OVERLAY_PINS_TOOLBAR_NAME,
                position = PackageDefaults.OVERLAY_PINS_TOOLBAR_POSITION)
})
@NbBundle.Messages({
        "CTL_OverlayPinLayerActionName=" + PackageDefaults.OVERLAY_PINS_NAME,
        "CTL_OverlayPinLayerActionToolTip=Show/hide pin overlay for the selected image"
})
public final class OverlayPinLayerAction extends AbstractOverlayAction {


    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlayPinLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/PinOverlay.gif", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.OVERLAY_PINS_ICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayPinLayerActionToolTip());
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        return view.isPinOverlayEnabled();
    }

    @Override
    protected boolean getActionEnabledState(ProductSceneView view) {
        final Product product = view.getProduct();
        return product != null && product.getPinGroup().getNodeCount() > 0;
    }


    @Override
    protected void setOverlayEnableState(ProductSceneView view) {
        view.setPinOverlayEnabled(!getActionSelectionState(view));
    }

}
