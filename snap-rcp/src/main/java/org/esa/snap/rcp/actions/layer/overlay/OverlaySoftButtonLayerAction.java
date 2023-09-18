
package org.esa.snap.rcp.actions.layer.overlay;


import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.VectorDataLayerFilterFactory;
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
        @ActionReference(path = "Menu/Layer", position = 22),
        @ActionReference(path = "Toolbars/Soft Button", position = 22)
})
@NbBundle.Messages({
        "CTL_OverlaySoftButtonLayerActionName=Soft Button Overlay(s)",
        "CTL_OverlaySoftButtonLayerActionToolTip=Show/hide multiple layers"
})
public final class OverlaySoftButtonLayerAction extends AbstractOverlayAction {

    public static final String STATE_UNASSIGNED = "Unassigned";
    public static final String STATE_ON_ON = "ON - ON";
    public static final String STATE_ON_OFF = "ON - OFF";
    public static final String STATE_OFF_ON = "OFF - ON";
    public static final String STATE_OFF_OFF = "OFF - OFF";


    public static enum SelectionState {
        UNASSIGNED,
        ON,
        OFF
    }


    public static final String SHOW_ANNOTATION_OVERLAY_STATE_KEY = "soft.button.annotation.overlay.show";
    public static final String SHOW_ANNOTATION_OVERLAY_STATE_LABEL = "Annotation Metadata Layer Display";
    public static final String SHOW_ANNOTATION_OVERLAY_STATE_TOOLTIP = "Assign Annotation Metadata layer display when soft button is clicked";
    public static final String SHOW_ANNOTATION_OVERLAY_STATE_DEFAULT = STATE_ON_OFF;

    public static final String SHOW_GRIDLINES_OVERLAY_STATE_KEY = "soft.button.gridlines.overlay.show";
    public static final String SHOW_GRIDLINES_OVERLAY_STATE_LABEL = "Map Gridlines Metadata Layer Display";
    public static final String SHOW_GRIDLINES_OVERLAY_STATE_TOOLTIP = "Map Gridlines layer display when soft button is clicked";
    public static final String SHOW_GRIDLINES_OVERLAY_STATE_DEFAULT = STATE_ON_OFF;


    public static final String SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY = "soft.button.colorbar.legend.overlay.show";
    public static final String SHOW_COLOR_BAR_LEGEND_OVERLAY_LABEL = "Show Color Bar Legend Layer";
    public static final String SHOW_COLOR_BAR_LEGEND_OVERLAY_TOOLTIP = "Shows Color Bar Legend layer when soft button is clicked";
    public static final String SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT = STATE_ON_OFF;

    public static final String SHOW_NO_DATA_OVERLAY_KEY = "soft.button.nodata.overlay.show";
    public static final String SHOW_NO_DATA_OVERLAY_LABEL = "Show No-Data Layer";
    public static final String SHOW_NO_DATA_OVERLAY_TOOLTIP = "Shows No-Data layer when soft button is clicked";
    public static final String SHOW_NO_DATA_OVERLAY_DEFAULT = STATE_ON_OFF;

    public static final String SHOW_PINS_OVERLAY_KEY = "soft.button.pins.overlay.show";
    public static final String SHOW_PINS_OVERLAY_LABEL = "Show Pins Layer";
    public static final String SHOW_PINS_OVERLAY_TOOLTIP = "Shows Pins layer when soft button is clicked";
    public static final String SHOW_PINS_OVERLAY_DEFAULT = STATE_UNASSIGNED;

    public static final String SHOW_GEOMETRY_OVERLAY_KEY = "soft.button.geometry.overlay.show";
    public static final String SHOW_GEOMETRY_OVERLAY_LABEL = "Show Geometry Layer";
    public static final String SHOW_GEOMETRY_OVERLAY_TOOLTIP = "Shows geometry layer when soft button is clicked";
    public static final String SHOW_GEOMETRY_OVERLAY_DEFAULT = STATE_UNASSIGNED;

    public static final String SHOW_GCP_OVERLAY_KEY = "soft.button.gcp.overlay.show";
    public static final String SHOW_GCP_OVERLAY_LABEL = "Show Ground Control Points Layer";
    public static final String SHOW_GCP_OVERLAY_TOOLTIP = "Shows ground control points layer when soft button is clicked";
    public static final String SHOW_GCP_OVERLAY_DEFAULT = STATE_UNASSIGNED;


    public static final String SHOW_MASK_PARENT_OVERLAY_KEY = "soft.button.mask.parent.overlay.show";
    public static final String SHOW_MASK_PARENT_OVERLAY_LABEL = "Show Masks Parent Layers";
    public static final String SHOW_MASK_PARENT_OVERLAY_TOOLTIP = "Shows masks parent layers when soft button is clicked";
    public static final String SHOW_MASK_PARENT_OVERLAY_DEFAULT = STATE_ON_OFF;

    public static final String SHOW_VECTOR_PARENT_OVERLAY_KEY = "soft.button.vector.parent.overlay.show";
    public static final String SHOW_VECTOR_PARENT_OVERLAY_LABEL = "Show Vectors Parent Layers";
    public static final String SHOW_VECTOR_PARENT_OVERLAY_TOOLTIP = "Shows vectors parent layers when soft button is clicked";
    public static final String SHOW_VECTOR_PARENT_OVERLAY_DEFAULT = STATE_UNASSIGNED;

    public static final String SHOW_MASK_LIST_OVERLAY_KEY = "soft.button.mask.list.overlay.show";
    public static final String SHOW_MASK_LIST_OVERLAY_LABEL = "Show Specific Masks Layers";
    public static final String SHOW_MASK_LIST_OVERLAY_TOOLTIP = "Shows specific masks layers when soft button is clicked";
    public static final String SHOW_MASK_LIST_OVERLAY_DEFAULT = STATE_ON_OFF;


    public static final String MASK_LIST_KEY = "soft.button.mask.list.overlay";
    public static final String MASK_LIST_LABEL = "Show Mask(s)";
    public static final String MASK_LIST_TOOLTIP = "Shows masks from this comma or space delimited list";
    public static final String MASK_LIST_DEFAULT = "LAND,CLDICE,LandMask";

    public static final String SHOW_IN_ALL_BANDS_OVERLAY_KEY = "soft.button.overlay.apply.all.view.windows";
    public static final String SHOW_IN_ALL_BANDS_OVERLAY_LABEL = "Apply To All View Windows";
    public static final String SHOW_IN_ALL_BANDS_OVERLAY_TOOLTIP = "Apply toggle of layer(s) to all open view windows when soft button is clicked";
    public static final boolean SHOW_IN_ALL_BANDS_OVERLAY_DEFAULT = false;

    ArrayList<String> masksArrayList;

    SelectionState metadataOverlayDesiredState;
    SelectionState graticuleOverlayDesiredState;
    SelectionState colorBarLegendOverlayDesiredState;
    SelectionState noDataOverlayDesiredState;
    SelectionState pinsOverlayDesiredState;
    SelectionState gcpOverlayDesiredState;
    SelectionState geometryOverlayDesiredState;
    SelectionState maskParentOverlayDesiredState;
    SelectionState maskListOverlayDesiredState;
    SelectionState vectorParentOverlayDesiredState;


    private final LayerFilter geometryFilter = VectorDataLayerFilterFactory.createGeometryFilter();


    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlaySoftButtonLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonBlueWhite24.png", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonBlueWhite24.png", false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlaySoftButtonLayerActionToolTip());
    }

    @Override
//    protected boolean getActionSelectionState(ProductSceneView view) {
//        return view.isSoftButtonEnabled();
//    }
    protected boolean getActionSelectionState(ProductSceneView view) {
        if (view.isSoftButtonEnabled()) {
            putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonBlueGreen24.png", false));
            putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonBlueGreen24.png", false));
        } else {
            putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonBlueWhite24.png", false));
            putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonBlueWhite24.png", false));
        }

        return false;
    }

    @Override
    protected boolean getActionEnabledState(ProductSceneView view) {
        return (view.getRaster() != null);
    }


    @Override
    protected void setOverlayEnableState(ProductSceneView view) {

        PropertyMap configuration = view.getSceneImage().getConfiguration();
        boolean setAllBands = configuration.getPropertyBool(SHOW_IN_ALL_BANDS_OVERLAY_KEY, SHOW_IN_ALL_BANDS_OVERLAY_DEFAULT);


        String metadataOverlayStatePattern = configuration.getPropertyString(SHOW_ANNOTATION_OVERLAY_STATE_KEY, SHOW_ANNOTATION_OVERLAY_STATE_DEFAULT);
        String graticuleOverlayStatePattern = configuration.getPropertyString(SHOW_GRIDLINES_OVERLAY_STATE_KEY, SHOW_GRIDLINES_OVERLAY_STATE_DEFAULT);
        String colorBarLegendStatePattern = configuration.getPropertyString(SHOW_COLOR_BAR_LEGEND_OVERLAY_KEY, SHOW_COLOR_BAR_LEGEND_OVERLAY_DEFAULT);
        String noDataOverlayStatePattern = configuration.getPropertyString(SHOW_NO_DATA_OVERLAY_KEY, SHOW_NO_DATA_OVERLAY_KEY);
        String pinsOverlayStatePattern = configuration.getPropertyString(SHOW_PINS_OVERLAY_KEY, SHOW_PINS_OVERLAY_DEFAULT);
        String gcpOverlayStatePattern = configuration.getPropertyString(SHOW_GCP_OVERLAY_KEY, SHOW_GCP_OVERLAY_DEFAULT);
        String geometryOverlayStatePattern = configuration.getPropertyString(SHOW_GEOMETRY_OVERLAY_KEY, SHOW_GEOMETRY_OVERLAY_DEFAULT);
        String maskParentOverlayStatePattern = configuration.getPropertyString(SHOW_MASK_PARENT_OVERLAY_KEY, SHOW_MASK_PARENT_OVERLAY_DEFAULT);
        String maskListOverlayStatePattern = configuration.getPropertyString(SHOW_MASK_LIST_OVERLAY_KEY, SHOW_MASK_LIST_OVERLAY_DEFAULT);
        String vectorParentOverlayStatePattern = configuration.getPropertyString(SHOW_VECTOR_PARENT_OVERLAY_KEY, SHOW_VECTOR_PARENT_OVERLAY_DEFAULT);

        String maskListsShow = configuration.getPropertyString(MASK_LIST_KEY, MASK_LIST_DEFAULT);
        masksArrayList = getVariablesArrayList(maskListsShow);

//        boolean desiredEnableState = !getActionSelectionState(view);
        boolean desiredEnableState = !view.isSoftButtonEnabled();

        metadataOverlayDesiredState = getDesiredState(desiredEnableState, metadataOverlayStatePattern);
        graticuleOverlayDesiredState = getDesiredState(desiredEnableState, graticuleOverlayStatePattern);
        colorBarLegendOverlayDesiredState = getDesiredState(desiredEnableState, colorBarLegendStatePattern);
        noDataOverlayDesiredState = getDesiredState(desiredEnableState, noDataOverlayStatePattern);
        pinsOverlayDesiredState = getDesiredState(desiredEnableState, pinsOverlayStatePattern);
        gcpOverlayDesiredState = getDesiredState(desiredEnableState, gcpOverlayStatePattern);
        geometryOverlayDesiredState = getDesiredState(desiredEnableState, geometryOverlayStatePattern);
        maskParentOverlayDesiredState = getDesiredState(desiredEnableState, maskParentOverlayStatePattern);
        maskListOverlayDesiredState = getDesiredState(desiredEnableState, maskListOverlayStatePattern);
        vectorParentOverlayDesiredState = getDesiredState(desiredEnableState, vectorParentOverlayStatePattern);


        if (desiredEnableState) {
            putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonBlueGreen24.png", false));
            putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonBlueGreen24.png", false));
        } else {
            putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonBlueWhite24.png", false));
            putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/seadas/SoftButtonBlueWhite24.png", false));
        }

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


    private SelectionState getDesiredState(boolean desiredButtonState, String userSelection) {
        SelectionState desiredState = SelectionState.UNASSIGNED;

        if (desiredButtonState) {
            if (STATE_ON_ON.equals(userSelection) || STATE_ON_OFF.equals(userSelection)) {
                desiredState = SelectionState.ON;
            } else if (STATE_OFF_OFF.equals(userSelection) || STATE_OFF_ON.equals(userSelection)) {
                desiredState = SelectionState.OFF;
            }
        } else {
            if (STATE_ON_ON.equals(userSelection) || STATE_OFF_ON.equals(userSelection)) {
                desiredState = SelectionState.ON;
            } else if (STATE_OFF_OFF.equals(userSelection) || STATE_ON_OFF.equals(userSelection)) {
                desiredState = SelectionState.OFF;
            }
        }

        return desiredState;
    }


    private void setOverlayEnableStateSubMethod(boolean desiredEnableState, ProductSceneView view) {


        if (noDataOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (noDataOverlayDesiredState == SelectionState.ON) {
                view.setNoDataOverlayEnabled(true);
            } else {
                view.setNoDataOverlayEnabled(false);
            }
        }

        if (graticuleOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (graticuleOverlayDesiredState == SelectionState.ON) {
                view.setGraticuleOverlayEnabled(true);
            } else {
                view.setGraticuleOverlayEnabled(false);
            }
        }


        if (metadataOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (metadataOverlayDesiredState == SelectionState.ON) {
                view.setMetaDataOverlayEnabled(true);
            } else {
                view.setMetaDataOverlayEnabled(false);
            }
        }

        if (colorBarLegendOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (colorBarLegendOverlayDesiredState == SelectionState.ON) {
                view.setColorBarOverlayEnabled(true);
            } else {
                view.setColorBarOverlayEnabled(false);
            }
        }


        if (pinsOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (pinsOverlayDesiredState == SelectionState.ON) {
                if (!view.isVectorOverlayEnabled()) {
                    view.setVectorOverlayEnabled(true);
                }
                view.setPinOverlayEnabled(true);
            } else {
                view.setPinOverlayEnabled(false);
            }
        }

        if (gcpOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (gcpOverlayDesiredState == SelectionState.ON) {
                if (!view.isVectorOverlayEnabled()) {
                    view.setVectorOverlayEnabled(true);
                }
                view.setGcpOverlayEnabled(true);
            } else {
                view.setGcpOverlayEnabled(false);
            }
        }

        if (geometryOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (geometryOverlayDesiredState == SelectionState.ON) {
                if (!view.isVectorOverlayEnabled()) {
                    view.setVectorOverlayEnabled(true);
                }
                if (view != null) {
                    List<Layer> childLayers = getGeometryLayers(view);
//                    childLayers.stream().forEach(layer -> layer.setVisible(isSelected()));
                    childLayers.stream().forEach(layer -> layer.setVisible(true));
                }
            } else {
                if (view != null) {
                    List<Layer> childLayers = getGeometryLayers(view);
                    childLayers.stream().forEach(layer -> layer.setVisible(false));
                }
            }
        }


        if (maskParentOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (maskParentOverlayDesiredState == SelectionState.ON) {
                view.setMaskOverlayEnabled(true);
            } else {
                view.setMaskOverlayEnabled(false);
            }
        }


        if (vectorParentOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (vectorParentOverlayDesiredState == SelectionState.ON) {
                view.setVectorOverlayEnabled(true);
            } else {
                view.setVectorOverlayEnabled(false);
            }
        }


        // turn on parent level masks folder if any masks are in list
        if (maskListOverlayDesiredState == SelectionState.ON && masksArrayList != null && masksArrayList.size() > 0) {
            if (!view.isMaskOverlayEnabled()) {
                view.setMaskOverlayEnabled(true);
            }

            for (String maskName : masksArrayList) {
                // Make sure 2 entries didn't ever get in place.  Delete all entries for this maskName
                Mask mask = view.getRaster().getOverlayMaskGroup().get(maskName);
                while (mask != null) {
                    view.getRaster().getOverlayMaskGroup().remove(mask);
                    mask = view.getRaster().getOverlayMaskGroup().get(maskName);
                }

                if (maskListOverlayDesiredState == SelectionState.ON) {

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
        }

        view.setSoftButtonEnabled(desiredEnableState);


//        VectorDataNode vd = view.getProduct().getVectorDataGroup().get("geometry");
    }


    private List<Layer> getGeometryLayers(ProductSceneView sceneView) {
        return LayerUtils.getChildLayers(sceneView.getRootLayer(), LayerUtils.SEARCH_DEEP, geometryFilter);
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
