/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.layer.overlay;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorAware;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductManager;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.SnapApp;
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
public final class OverlayPinLayerAction extends AbstractOverlayAction{

    private ProductSceneView productSceneView;
    private final LayerListener layerListener;
    private WeakReference<ProductSceneView> lastView;

    public OverlayPinLayerAction() {
        this(Utilities.actionsGlobalContext());
    }

    public OverlayPinLayerAction(Lookup lkp) {
        super(lkp);
        layerListener = new PinLayerListener();
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


    private class PinLayerListener extends AbstractLayerListener {
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
