
package org.esa.snap.rcp.actions.layer.overlay;


import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

import java.util.ArrayList;
import java.util.List;

import static org.esa.snap.rcp.actions.window.OpenImageViewAction.getProductSceneView;

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

    public static final String  MASK_LIST_SHOW_KEY = "soft.button.mask.list.overlay.show";
    public static final String  MASK_LIST_SHOW_LABEL = "Show Mask(s)";
    public static final String  MASK_LIST_SHOW_TOOLTIP = "Shows masks from this comma or space delimited list";
    public static final String  MASK_LIST_SHOW_DEFAULT = "LAND,CLDICE,LandMask";

    public static final String  SHOW_IN_ALL_BANDS_OVERLAY_KEY = "soft.button.overlay.apply.all.view.windows";
    public static final String  SHOW_IN_ALL_BANDS_OVERLAY_LABEL = "Apply To All View Windows";
    public static final String  SHOW_IN_ALL_BANDS_OVERLAY_TOOLTIP = "Apply toggle of layer(s) to all open view windows when soft button is clicked";
    public static final boolean  SHOW_IN_ALL_BANDS_OVERLAY_DEFAULT = true;

    ArrayList<String> masksShowArrayList;
    boolean noDataOverlayShow;
    boolean graticuleOverlayShow;
    boolean metadataOverlayShow;
    boolean colorBarLegendOverlayShow;

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


//    @Override
//    protected void setOverlayEnableState(ProductSceneView view) {
//
//        // todo set this in preferences
//        boolean setAllBands = true;
//        List<RasterDataNode> rasters = view.getProduct().getRasterDataNodes();
//
//
//        PropertyMap configuration = view.getSceneImage().getConfiguration();
//
//        boolean noDataOverlayShow = configuration.getPropertyBool(SHOW_NO_DATA_OVERLAY_KEY, SHOW_NO_DATA_OVERLAY_DEFAULT);
//        if (noDataOverlayShow) {
//            view.setNoDataOverlayEnabled(!getActionSelectionState(view));
//        }
//
//        boolean graticuleOverlayShow = configuration.getPropertyBool(SHOW_GRIDLINES_OVERLAY_KEY, SHOW_GRIDLINES_OVERLAY_DEFAULT);
//        if (graticuleOverlayShow && ProductUtils.canGetPixelPos(view.getRaster())) {
//            view.setGraticuleOverlayEnabled(!getActionSelectionState(view));
//        }
//
//        boolean metadataOverlayShow = configuration.getPropertyBool(SHOW_ANNOTATION_OVERLAY_KEY, SHOW_ANNOTATION_OVERLAY_DEFAULT);
//        if (metadataOverlayShow) {
//            view.setMetaDataOverlayEnabled(!getActionSelectionState(view));
//        }
//
//        boolean colorBarLegendOverlayShow = configuration.getPropertyBool(SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY, SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT);
//        if (colorBarLegendOverlayShow) {
//            if (setAllBands) {
//                for (RasterDataNode raster : rasters) {
//                    ProductSceneView productSceneView = getProductSceneView(raster);
//                    if (productSceneView != null) {
//                        productSceneView.setColorBarOverlayEnabled(!getActionSelectionState(view));
//                    }
//                }
//            } else {
//                view.setColorBarOverlayEnabled(!getActionSelectionState(view));
//            }
//        }
//
//
//
//        String maskListsShow = configuration.getPropertyString(MASK_LIST_SHOW_KEY, MASK_LIST_SHOW_DEFAULT);
//
//        ArrayList<String> masksShowArrayList = getVariablesArrayList(maskListsShow);
//
//        if (masksShowArrayList != null && masksShowArrayList.size() > 0) {
//            if (!view.isMaskOverlayEnabled()) {
//                view.setMaskOverlayEnabled(true);
//            }
////            view.setMaskOverlayEnabled(!getActionSelectionState(view));
//        }
//
//        for (String maskName : masksShowArrayList) {
//            Mask mask = view.getProduct().getMaskGroup().get(maskName);
//
//            if (mask != null) {
//                if (!getActionSelectionState(view)) {
//                    view.getRaster().getOverlayMaskGroup().add(mask);
//                } else {
//                    view.getRaster().getOverlayMaskGroup().remove(mask);
//                }
//            }
//        }
//
//
//
//
//
//
////        VectorDataNode vd = view.getProduct().getVectorDataGroup().get("geometry");
//
//
//
//
//
//        view.setSoftButtonEnabled(!getActionSelectionState(view));
//    }



    @Override
    protected void setOverlayEnableState(ProductSceneView view) {

        PropertyMap configuration = view.getSceneImage().getConfiguration();
        boolean setAllBands = configuration.getPropertyBool(SHOW_IN_ALL_BANDS_OVERLAY_KEY, SHOW_IN_ALL_BANDS_OVERLAY_DEFAULT);

        noDataOverlayShow = configuration.getPropertyBool(SHOW_NO_DATA_OVERLAY_KEY, SHOW_NO_DATA_OVERLAY_DEFAULT);
        graticuleOverlayShow = configuration.getPropertyBool(SHOW_GRIDLINES_OVERLAY_KEY, SHOW_GRIDLINES_OVERLAY_DEFAULT);
        metadataOverlayShow = configuration.getPropertyBool(SHOW_ANNOTATION_OVERLAY_KEY, SHOW_ANNOTATION_OVERLAY_DEFAULT);
        colorBarLegendOverlayShow = configuration.getPropertyBool(SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY, SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT);

        String maskListsShow = configuration.getPropertyString(MASK_LIST_SHOW_KEY, MASK_LIST_SHOW_DEFAULT);
        masksShowArrayList = getVariablesArrayList(maskListsShow);

        boolean desiredEnableState = !getActionSelectionState(view);

        List<RasterDataNode> rasters = view.getProduct().getRasterDataNodes();

        if (setAllBands) {
            for (RasterDataNode raster : rasters) {
                ProductSceneView productSceneView = getProductSceneView(raster);
                if (productSceneView != null) {
                    setOverlayEnableStateSubMethod(desiredEnableState, productSceneView);
                }
            }
        } else {
            setOverlayEnableStateSubMethod(desiredEnableState, view);
        }
        }



     private void setOverlayEnableStateSubMethod(boolean desiredEnableState, ProductSceneView view) {

        if (noDataOverlayShow) {
            view.setNoDataOverlayEnabled(desiredEnableState);
        }

        if (graticuleOverlayShow && ProductUtils.canGetPixelPos(view.getRaster())) {
            view.setGraticuleOverlayEnabled(desiredEnableState);
        }

        if (metadataOverlayShow) {
            view.setMetaDataOverlayEnabled(desiredEnableState);
        }

        if (colorBarLegendOverlayShow) {
            view.setColorBarOverlayEnabled(desiredEnableState);
        }


        // turn on parent level masks folder if any masks are in list
        if (masksShowArrayList != null && masksShowArrayList.size() > 0) {
            if (!view.isMaskOverlayEnabled()) {
                view.setMaskOverlayEnabled(true);
            }
        }

        for (String maskName : masksShowArrayList) {


            // Make sure 2 entries didn't ever get in place.  Delete all entries for this maskName
            Mask mask = view.getRaster().getOverlayMaskGroup().get(maskName);
            while (mask != null) {
                view.getRaster().getOverlayMaskGroup().remove(mask);
                mask = view.getRaster().getOverlayMaskGroup().get(maskName);
            }

            // Add mask if maskName is a mask
            if (desiredEnableState) {
                mask = view.getProduct().getMaskGroup().get(maskName);
                if (mask != null) {
                    view.getRaster().getOverlayMaskGroup().add(mask);
                }
            }


                if (desiredEnableState) {

                    // don't add mask twice
                     mask = view.getRaster().getOverlayMaskGroup().get(maskName);
                    if (mask == null) {

                        // mask not in group to add it if it is a mask
                        mask = view.getProduct().getMaskGroup().get(maskName);
                        if (mask != null) {
                            view.getRaster().getOverlayMaskGroup().add(mask);
                        }
                    }

                } else {
                     mask = view.getRaster().getOverlayMaskGroup().get(maskName);
                    if (mask != null) {
                        view.getRaster().getOverlayMaskGroup().remove(mask);
                    }
                }
        }

        view.setSoftButtonEnabled(desiredEnableState);


//        VectorDataNode vd = view.getProduct().getVectorDataGroup().get("geometry");
    }




    private ArrayList<String> getVariablesArrayList(String variables) {
        ArrayList<String> variablesList = new ArrayList<String>();
        if (variables != null && variables.trim() != null && variables.trim().length() > 0) {
            String[] paramsArray = variables.split("[ ,]+");
            for (String currentParam : paramsArray) {
                if (currentParam != null && currentParam.trim() != null && currentParam.trim().length() > 0) {
                    variablesList.add(currentParam.trim());
                }
            }
        }
        return variablesList;
    }


}
