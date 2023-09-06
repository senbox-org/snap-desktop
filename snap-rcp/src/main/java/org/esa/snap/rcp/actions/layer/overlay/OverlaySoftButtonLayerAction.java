
package org.esa.snap.rcp.actions.layer.overlay;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * @author Daniel Knowles
 */
@ActionID(category = "View", id = "OverlaySoftButtonLayerAction")
@ActionRegistration(displayName = "#CTL_OverlaySoftButtonLayerActionName")
@ActionReferences({
        @ActionReference(path = "Menu/Layer", position = 21),
        @ActionReference(path = "Toolbars/Soft Button", position = 21)
})
@NbBundle.Messages({
        "CTL_OverlaySoftButtonLayerActionName=Soft Button Overlay(s)",
        "CTL_OverlaySoftButtonLayerActionToolTip=Show/hide multiple layers"
})
public final class OverlaySoftButtonLayerAction extends AbstractOverlayAction {

    public static final String  SHOW_ANNOTATION_OVERLAY_KEY = "soft.button.annotation.overlay.show";
    public static final String  SHOW_ANNOTATION_OVERLAY_LABEL = "Show Annotation Metadata Layer";
    public static final String  SHOW_ANNOTATION_OVERLAY_TOOLTIP = "Shows Annotation Metadata layer when soft button is clicked";
    public static final boolean  SHOW_ANNOTATION_OVERLAY_DEFAULT = true;

    public static final String  SHOW_GRIDLINES_OVERLAY_KEY = "soft.button.gridlines.overlay.show";
    public static final String  SHOW_GRIDLINES_OVERLAY_LABEL = "Show Map Gridlines Layer";
    public static final String  SHOW_GRIDLINES_OVERLAY_TOOLTIP = "Shows Map Gridlines layer when soft button is clicked";
    public static final boolean  SHOW_GRIDLINES_OVERLAY_DEFAULT = true;

    public static final String  SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY = "soft.button.colorbar.legend.overlay.show";
    public static final String  SHOW_COLOR_BAR_LEGEND_OVERLAY_LABEL = "Show Color Bar Legend Layer";
    public static final String  SHOW_COLOR_BAR_LEGEND_OVERLAY_TOOLTIP = "Shows Color Bar Legend layer when soft button is clicked";
    public static final boolean  SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT = true;

    public static final String  SHOW_NO_DATA_OVERLAY_KEY = "soft.button.nodata.overlay.show";
    public static final String  SHOW_NO_DATA_OVERLAY_LABEL = "Show No-Data Layer";
    public static final String  SHOW_NO_DATA_OVERLAY_TOOLTIP = "Shows No-Data layer when soft button is clicked";
    public static final boolean  SHOW_NO_DATA_OVERLAY_DEFAULT = true;


    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlaySoftButtonLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonLayer24.png", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonLayer24.png", false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlaySoftButtonLayerActionToolTip());
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        return view.isSoftButtonEnabled();
    }

    @Override
    protected boolean getActionEnabledState(ProductSceneView view) {
        return (view.getRaster() != null);
    }


    @Override
    protected void setOverlayEnableState(ProductSceneView view) {

        PropertyMap configuration = view.getSceneImage().getConfiguration();


        boolean graticuleOverlayShow = configuration.getPropertyBool(SHOW_GRIDLINES_OVERLAY_KEY, SHOW_GRIDLINES_OVERLAY_DEFAULT);
        if (graticuleOverlayShow && ProductUtils.canGetPixelPos(view.getRaster())) {
            view.setGraticuleOverlayEnabled(!getActionSelectionState(view));
//            updateActionState();

        }

        boolean colorBarLegendOverlayShow = configuration.getPropertyBool(SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY, SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT);
        if (colorBarLegendOverlayShow) {
            view.setColorBarOverlayEnabled(!getActionSelectionState(view));
//            updateActionState();

//            Layer layer = LayerUtils.getChildLayerById(view.getRootLayer(), "OverlayColorBarLayerAction");
//            Layer layer = LayerUtils.getChildLayerById(view.getRootLayer(), view.COLORBAR_LAYER_ID);
//            if (getActionSelectionState(view)) {
//                view.setSelectedLayer(layer);
//            } else {
//                view.setSelectedLayer(null);
//            }
//
//            if (layer != null) {
//                layer.getParent().getChildren().remove(layer);
//                layer.dispose();
//            }
        }

        boolean metadataOverlayShow = configuration.getPropertyBool(SHOW_ANNOTATION_OVERLAY_KEY, SHOW_ANNOTATION_OVERLAY_DEFAULT);
        if (metadataOverlayShow) {
            view.setMetaDataOverlayEnabled(!getActionSelectionState(view));
//            updateActionState();
        }

        boolean noDataOverlayShow = configuration.getPropertyBool(SHOW_NO_DATA_OVERLAY_KEY, SHOW_NO_DATA_OVERLAY_DEFAULT);
        if (noDataOverlayShow) {
            view.setNoDataOverlayEnabled(!getActionSelectionState(view));
//            updateActionState();
//
//
//            view.setNoDataOverlayEnabled(getActionSelectionState(view));
//            view.setNoDataOverlayEnabled(!getActionSelectionState(view));
        }

        if (metadataOverlayShow) {
            view.setMetaDataOverlayEnabled(!getActionSelectionState(view));
//            updateActionState();
//
//            view.setMetaDataOverlayEnabled(getActionSelectionState(view));
//            view.setMetaDataOverlayEnabled(!getActionSelectionState(view));
        }

//
//        view.updateImage();
//        view.updateUI();
//
//        Layer layer = LayerUtils.getChildLayerById(view.getRootLayer(), view.NO_DATA_LAYER_ID);
//        view.setSelectedLayer(layer);
//        updateActionState();
//
//        view.firePropertyChange("OverlayMetaDataLayerAction", false, true);
//        view.firePropertyChange("visible", false, true);
        view.setSoftButtonEnabled(!getActionSelectionState(view));
    }


}
