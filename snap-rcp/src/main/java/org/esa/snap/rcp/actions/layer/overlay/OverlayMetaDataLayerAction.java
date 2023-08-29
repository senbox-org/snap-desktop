
package org.esa.snap.rcp.actions.layer.overlay;

import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * @author Daniel Knowles
 */
@ActionID(category = "View", id = "OverlayMetaDataLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayMetaDataLayerActionName")
@ActionReferences({
        @ActionReference(path = "Menu/Layer", position = 21),
        @ActionReference(path = "Toolbars/Overlay", position = 21)
})
@NbBundle.Messages({
        "CTL_OverlayMetaDataLayerActionName=Annotation Metadata Overlay",
        "CTL_OverlayMetaDataLayerActionToolTip=Show/hide Annotation Metadata Overlay for the selected image"
})
public final class OverlayMetaDataLayerAction extends AbstractOverlayAction {

    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlayMetaDataLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/AnnotationLayer24.png", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/AnnotationLayer24.png", false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayMetaDataLayerActionToolTip());
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        return view.isMetaDataOverlayEnabled();
    }

    @Override
    protected boolean getActionEnabledState(ProductSceneView view) {
        return (view.getRaster() != null);
    }


    @Override
    protected void setOverlayEnableState(ProductSceneView view) {
        view.setMetaDataOverlayEnabled(!getActionSelectionState(view));
    }


}
