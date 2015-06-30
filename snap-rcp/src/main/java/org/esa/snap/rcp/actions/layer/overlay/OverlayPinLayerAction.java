/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.layer.overlay;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

import javax.swing.Action;

/**
 * @author Marco Peters
 */
@ActionID(category = "View", id = "OverlayPinLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayPinLayerActionName", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/Layer", position = 30),
        @ActionReference(path = "Toolbars/Overlay", position = 30)
})
@NbBundle.Messages({
        "CTL_OverlayPinLayerActionName=Pin Overlay",
        "CTL_OverlayPinLayerActionToolTip=Show/hide pin overlay for the selected image"
})
public final class OverlayPinLayerAction extends AbstractOverlayAction {

    public OverlayPinLayerAction() {
        this(Utilities.actionsGlobalContext());
    }

    public OverlayPinLayerAction(Lookup lkp) {
        super(lkp);
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new OverlayPinLayerAction(lkp);
    }

    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlayPinLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/PinOverlay.gif", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/PinOverlay24.gif", false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayPinLayerActionToolTip());
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        return view.isPinOverlayEnabled();
    }

    @Override
    protected boolean getActionEnableState(ProductSceneView view) {
        final Product product = view.getProduct();
        return product != null && product.getPinGroup().getNodeCount() > 0;
    }

    @Override
    protected void setOverlayEnableState(ProductSceneView view) {
        view.setPinOverlayEnabled(!getActionSelectionState(view));
    }


}
