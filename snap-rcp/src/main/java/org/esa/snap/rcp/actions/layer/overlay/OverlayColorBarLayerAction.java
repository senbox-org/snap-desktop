/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.layer.overlay;

import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.layer.ColorBarLayerType;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * @author Daniel Knowles
 */
@ActionID(category = "View", id = "OverlayColorBarLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayColorBarLayerActionName")
@ActionReferences({
        @ActionReference(path = "Menu/Layer", position = 20),
        @ActionReference(path = "Toolbars/Overlay", position = 20)
})
@NbBundle.Messages({
        "CTL_OverlayColorBarLayerActionName=" + ColorBarLayerType.COLOR_BAR_LAYER_NAME + " Overlay",
        "CTL_OverlayColorBarLayerActionToolTip=Show/hide color bar overlay for the selected image"
})
public final class OverlayColorBarLayerAction extends AbstractOverlayAction {

    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlayColorBarLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/ColorbarVertical24.png", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/ColorbarVertical24.png", false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayColorBarLayerActionToolTip());
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        return view.isColorBarOverlayEnabled();
    }

    @Override
    protected boolean getActionEnabledState(ProductSceneView view) {
        return (view.getRaster() != null);
    }

    @Override
    protected void setOverlayEnableState(ProductSceneView view) {
        view.setColorBarOverlayEnabled(!getActionSelectionState(view));
    }


}
