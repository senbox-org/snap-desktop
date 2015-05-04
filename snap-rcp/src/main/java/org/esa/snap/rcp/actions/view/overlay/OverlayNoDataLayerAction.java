/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.view.overlay;

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
@ActionID(category = "View", id = "OverlayNoDataLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayNoDataLayerActionName", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/View/Overlay", position = 0),
        @ActionReference(path = "Toolbars/Overlay", position = 0)
})
@NbBundle.Messages({
        "CTL_OverlayNoDataLayerActionName=Toggle No-Data Overlay",
        "CTL_OverlayNoDataLayerActionToolTip=Show/hide no-data overlay for the selected image"
})
public final class OverlayNoDataLayerAction extends AbstractOverlayAction {

    public OverlayNoDataLayerAction() {
        this(Utilities.actionsGlobalContext());
    }

    public OverlayNoDataLayerAction(Lookup lkp) {
        super(lkp);
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new OverlayNoDataLayerAction(lkp);
    }

    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlayNoDataLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/NoDataOverlay.gif", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/NoDataOverlay24.gif", false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayNoDataLayerActionToolTip());
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        return view.isNoDataOverlayEnabled();
    }

    @Override
    protected boolean getActionEnableState(ProductSceneView view) {
        return view.getRaster().isValidMaskUsed();
    }

    @Override
    protected void setOverlayEnableState(ProductSceneView view) {
        view.setNoDataOverlayEnabled(!getActionSelectionState(view));
    }


}
