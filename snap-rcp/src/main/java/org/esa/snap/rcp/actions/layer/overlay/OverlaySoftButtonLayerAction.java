
package org.esa.snap.rcp.actions.layer.overlay;


import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
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
    public static final String STATE_UNASSIGNED_ON = "Unassigned - ON";
    public static final String STATE_UNASSIGNED_OFF = "Unassigned - OFF";
    public static final String STATE_ON_ON = "ON - ON";
    public static final String STATE_ON_OFF = "ON - OFF";
    public static final String STATE_OFF_ON = "OFF - ON";
    public static final String STATE_OFF_OFF = "OFF - OFF";


    public static final String STATE_UNASSIGNED_ZOOM_DEFAULT = "Unassigned - DEFAULT";
    public static final String STATE_UNASSIGNED_ZOOM_ALL = "Unassigned - ALL";
    public static final String STATE_UNASSIGNED_ZOOM1 = "Unassigned - POS_ZOOM1";

    public static final String STATE_ZOOM_DEFAULT_DEFAULT = "DEFAULT - DEFAULT";
    public static final String STATE_ZOOM_DEFAULT_ALL = "DEFAULT - ALL";
    public static final String STATE_ZOOM_DEFAULT_ZOOM1 = "DEFAULT - ZOOM_1";

    public static final String STATE_ZOOM_ALL_ZOOM_DEFAULT = "ALL - DEFAULT";
    public static final String STATE_ZOOM_ALL_ALL = "ALL - ALL";
    public static final String STATE_ZOOM_ALL_ZOOM1 = "ALL - ZOOM1";

    public static final String STATE_ZOOM1_ZOOM_DEFAULT = "ZOOM1 - DEFAULT";
    public static final String STATE_ZOOM1_ALL = "ZOOM1 - ALL";
    public static final String STATE_ZOOM1_ZOOM1 = "POS_ZOOM1 - POS_ZOOM1";

    public static final String STATE_ZOOM1_ZOOM2 = "POS_ZOOM1 - POS_ZOOM2";

    public static final String STATE_UNASSIGNED_ZOOM2 = "Unassigned - POS_ZOOM2";
    public static final String STATE_ZOOM2_ZOOM1 = "POS_ZOOM2 - POS_ZOOM1";
    public static final String STATE_ZOOM2_ZOOM2 = "POS_ZOOM2 - POS_ZOOM2";


    public static enum SelectionState {
        UNASSIGNED,
        ON,
        OFF,
        ZOOM1,
        ZOOM2,
        ZOOM_DEFAULT,
        ZOOM_ALL
    }


    public static final String SHOW_ANNOTATION_OVERLAY_STATE_KEY = "soft.button.annotation.overlay.show";
    public static final String SHOW_ANNOTATION_OVERLAY_STATE_LABEL = "Show Annotation Metadata Layer";
    public static final String SHOW_ANNOTATION_OVERLAY_STATE_TOOLTIP = "Assign Annotation Metadata layer display when soft button is clicked";
    public static final String SHOW_ANNOTATION_OVERLAY_STATE_DEFAULT = STATE_ON_OFF;

    public static final String SHOW_GRIDLINES_OVERLAY_STATE_KEY = "soft.button.gridlines.overlay.show";
    public static final String SHOW_GRIDLINES_OVERLAY_STATE_LABEL = "Show Map Gridlines Layer";
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
    public static final String SHOW_MASK_PARENT_OVERLAY_LABEL = "Show Masks Layers";
    public static final String SHOW_MASK_PARENT_OVERLAY_TOOLTIP = "Shows masks parent layers when soft button is clicked";
    public static final String SHOW_MASK_PARENT_OVERLAY_DEFAULT = STATE_UNASSIGNED;

    public static final String SHOW_VECTOR_PARENT_OVERLAY_KEY = "soft.button.vector.parent.overlay.show";
    public static final String SHOW_VECTOR_PARENT_OVERLAY_LABEL = "Show Vectors Layers";
    public static final String SHOW_VECTOR_PARENT_OVERLAY_TOOLTIP = "Shows vectors parent layers when soft button is clicked";
    public static final String SHOW_VECTOR_PARENT_OVERLAY_DEFAULT = STATE_UNASSIGNED;

    public static final String SHOW_MASK_LIST_OVERLAY_KEY = "soft.button.favorite.masks.show";
    public static final String SHOW_MASK_LIST_OVERLAY_LABEL = "Show Favorite Masks";
    public static final String SHOW_MASK_LIST_OVERLAY_TOOLTIP = "Shows specific masks layers when soft button is clicked";
    public static final String SHOW_MASK_LIST_OVERLAY_DEFAULT = STATE_UNASSIGNED;

    public static final String MASK_LIST_KEY = "soft.button.favorite.mask.list";
    public static final String MASK_LIST_LABEL = "Favorite Masks";
    public static final String MASK_LIST_TOOLTIP = "Shows masks from this comma or space delimited list";
    public static final String MASK_LIST_DEFAULT = "";



    public static final String SET_ZOOM1_SECTION_KEY = "soft.button.set.zoom1.section";
    public static final String SET_ZOOM1_SECTION_LABEL = "Position/Zoom (POS_ZOOM1)";
    public static final String SET_ZOOM1_SECTION_TOOLTIP = "Fields associated with POS_ZOOM1";

    public static final String SET_ZOOM2_SECTION_KEY = "soft.button.set.zoom2.section";
    public static final String SET_ZOOM2_SECTION_LABEL = "Position/Zoom (POS_ZOOM2)";
    public static final String SET_ZOOM2_SECTION_TOOLTIP = "Fields associated with POS_ZOOM1";

    public static final String SET_ZOOM_FACTOR_STATE_KEY = "soft.button.set.zoom";
    public static final String SET_ZOOM_FACTOR_STATE_LABEL = "Set Scene Image Position/Zoom";
    public static final String SET_ZOOM_FACTOR_STATE_TOOLTIP = "Sets image zoom based on Zoom 1 and Zoom 2";
    public static final String SET_ZOOM_FACTOR_STATE_DEFAULT = STATE_ZOOM1_ZOOM2;

    public static final String SET_ZOOM_FACTOR_1_KEY = "soft.button.zoom1";
    public static final String SET_ZOOM_FACTOR_1_LABEL = "POS_ZOOM1: Image Zoom";
    public static final String SET_ZOOM_FACTOR_1_TOOLTIP = "Zoom factor used if Image Zoom is assigned";
    public static final double SET_ZOOM_FACTOR_1_DEFAULT = 75;

    public static final String SET_ZOOM_FACTOR_2_KEY = "soft.button.zoom2";
    public static final String SET_ZOOM_FACTOR_2_LABEL = "POS_ZOOM2: Image Zoom";
    public static final String SET_ZOOM_FACTOR_2_TOOLTIP = "Zoom factor used if Image Zoom is assigned";
    public static final double SET_ZOOM_FACTOR_2_DEFAULT = 100;

    public static final String SET_CENTERX_1_KEY = "soft.button.centerx.1";
    public static final String SET_CENTERX_1_LABEL = "POS_ZOOM1: Center Image (Horizontal)";
    public static final String SET_CENTERX_1_TOOLTIP = "Center image horizontally for Zoom-Pos1";
    public static final boolean SET_CENTERX_1_DEFAULT = true;

    public static final String SET_CENTERX_2_KEY = "soft.button.centerx.2";
    public static final String SET_CENTERX_2_LABEL = "POS_ZOOM2: Center Image (Horizontal)";
    public static final String SET_CENTERX_2_TOOLTIP = "Center image horizontally for Zoom-Pos2";
    public static final boolean SET_CENTERX_2_DEFAULT = true;

    public static final String SET_CENTERY_1_KEY = "soft.button.centery.1";
    public static final String SET_CENTERY_1_LABEL = "POS_ZOOM1: Center Image (Vertical)";
    public static final String SET_CENTERY_1_TOOLTIP = "Center image vertically for Zoom-Pos1";
    public static final boolean SET_CENTERY_1_DEFAULT = true;

    public static final String SET_CENTERY_2_KEY = "soft.button.centery.2";
    public static final String SET_CENTERY_2_LABEL = "POS_ZOOM2: Center Image (Vertical)";
    public static final String SET_CENTERY_2_TOOLTIP = "Center image vertically for Zoom-Pos2";
    public static final boolean SET_CENTERY_2_DEFAULT = true;

    public static final String SET_SHIFTX_1_KEY = "soft.button.shiftx.1";
    public static final String SET_SHIFTX_1_LABEL = "POS_ZOOM1: Image Shift (Horizontal)";
    public static final String SET_SHIFTX_1_TOOLTIP = "Shift image horizontally rightwards if Zoom-Pos 1 is assigned";
    public static final double SET_SHIFTX_1_DEFAULT = 0.0;

    public static final String SET_SHIFTX_2_KEY = "soft.button.shiftx.2";
    public static final String SET_SHIFTX_2_LABEL = "POS_ZOOM2: Image Shift (Horizontal)";
    public static final String SET_SHIFTX_2_TOOLTIP = "Shift image horizontally rightwards if Zoom-Pos 2 is assigned";
    public static final double SET_SHIFTX_2_DEFAULT = 0.0;

    public static final String SET_SHIFTY_1_KEY = "soft.button.shifty.1";
    public static final String SET_SHIFTY_1_LABEL = "POS_ZOOM1: Image Shift Image (Vertical)";
    public static final String SET_SHIFTY_1_TOOLTIP = "Shift image vertically downwards if Zoom-Pos 1 is assigned";
    public static final double SET_SHIFTY_1_DEFAULT = 0.0;

    public static final String SET_SHIFTY_2_KEY = "soft.button.shifty.2";
    public static final String SET_SHIFTY_2_LABEL = "POS_ZOOM2: Image Shift (Vertical)";
    public static final String SET_SHIFTY_2_TOOLTIP = "Shift image vertically downwards if Zoom-Pos 2 is assigned";
    public static final double SET_SHIFTY_2_DEFAULT = 0.0;


    public static final String SHOW_IN_ALL_BANDS_OVERLAY_KEY = "soft.button.overlay.apply.all.view.windows";
    public static final String SHOW_IN_ALL_BANDS_OVERLAY_LABEL = "Apply to all Open View Windows";
    public static final String SHOW_IN_ALL_BANDS_OVERLAY_TOOLTIP = "Apply toggle of layer(s) to all open view windows when soft button is clicked";
    public static final boolean SHOW_IN_ALL_BANDS_OVERLAY_DEFAULT = false;


    ArrayList<String> masksArrayList;

    SelectionState metadataOverlayDesiredState;
    SelectionState graticuleOverlayDesiredState;
    SelectionState colorBarLegendOverlayDesiredState;
    SelectionState noDataOverlayDesiredState;
    SelectionState zoomDesiredState;
    SelectionState pinsOverlayDesiredState;
    SelectionState gcpOverlayDesiredState;
    SelectionState geometryOverlayDesiredState;
    SelectionState maskParentOverlayDesiredState;
    SelectionState maskListOverlayDesiredState;
    SelectionState vectorParentOverlayDesiredState;
    double zoomFactor1;
    double zoomFactor2;
    boolean centerX1;
    boolean centerX2;
    boolean centerY1;
    boolean centerY2;
    double shiftX1;
    double shiftX2;
    double shiftY1;
    double shiftY2;



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
        String noDataOverlayStatePattern = configuration.getPropertyString(SHOW_NO_DATA_OVERLAY_KEY, SHOW_NO_DATA_OVERLAY_DEFAULT);
        String zoomStatePattern = configuration.getPropertyString(SET_ZOOM_FACTOR_STATE_KEY, SET_ZOOM_FACTOR_STATE_DEFAULT);
        String pinsOverlayStatePattern = configuration.getPropertyString(SHOW_PINS_OVERLAY_KEY, SHOW_PINS_OVERLAY_DEFAULT);
        String gcpOverlayStatePattern = configuration.getPropertyString(SHOW_GCP_OVERLAY_KEY, SHOW_GCP_OVERLAY_DEFAULT);
        String geometryOverlayStatePattern = configuration.getPropertyString(SHOW_GEOMETRY_OVERLAY_KEY, SHOW_GEOMETRY_OVERLAY_DEFAULT);
        String maskParentOverlayStatePattern = configuration.getPropertyString(SHOW_MASK_PARENT_OVERLAY_KEY, SHOW_MASK_PARENT_OVERLAY_DEFAULT);
        String maskListOverlayStatePattern = configuration.getPropertyString(SHOW_MASK_LIST_OVERLAY_KEY, SHOW_MASK_LIST_OVERLAY_DEFAULT);
        String vectorParentOverlayStatePattern = configuration.getPropertyString(SHOW_VECTOR_PARENT_OVERLAY_KEY, SHOW_VECTOR_PARENT_OVERLAY_DEFAULT);
        zoomFactor1 = configuration.getPropertyDouble(SET_ZOOM_FACTOR_1_KEY, SET_ZOOM_FACTOR_1_DEFAULT);
        zoomFactor2 = configuration.getPropertyDouble(SET_ZOOM_FACTOR_2_KEY, SET_ZOOM_FACTOR_2_DEFAULT);
        centerX1 = configuration.getPropertyBool(SET_CENTERX_1_KEY, SET_CENTERX_1_DEFAULT);
        centerX2 = configuration.getPropertyBool(SET_CENTERX_2_KEY, SET_CENTERX_2_DEFAULT);
        centerY1 = configuration.getPropertyBool(SET_CENTERY_1_KEY, SET_CENTERY_1_DEFAULT);
        centerY2 = configuration.getPropertyBool(SET_CENTERY_2_KEY, SET_CENTERY_2_DEFAULT);
        shiftX1 = configuration.getPropertyDouble(SET_SHIFTX_1_KEY, SET_SHIFTX_1_DEFAULT);
        shiftX2 = configuration.getPropertyDouble(SET_SHIFTX_2_KEY, SET_SHIFTX_2_DEFAULT);
        shiftY1 = configuration.getPropertyDouble(SET_SHIFTY_1_KEY, SET_SHIFTY_1_DEFAULT);
        shiftY2 = configuration.getPropertyDouble(SET_SHIFTY_2_KEY, SET_SHIFTY_2_DEFAULT);


        String maskListsShow = configuration.getPropertyString(MASK_LIST_KEY, MASK_LIST_DEFAULT);
        masksArrayList = getVariablesArrayList(maskListsShow);

//        boolean desiredEnableState = !getActionSelectionState(view);
        boolean desiredEnableState = !view.isSoftButtonEnabled();

        metadataOverlayDesiredState = getDesiredState(desiredEnableState, metadataOverlayStatePattern);
        graticuleOverlayDesiredState = getDesiredState(desiredEnableState, graticuleOverlayStatePattern);
        colorBarLegendOverlayDesiredState = getDesiredState(desiredEnableState, colorBarLegendStatePattern);
        noDataOverlayDesiredState = getDesiredState(desiredEnableState, noDataOverlayStatePattern);
        zoomDesiredState = getDesiredZoomState(desiredEnableState, zoomStatePattern);
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
            if (STATE_ON_ON.equals(userSelection) || STATE_OFF_ON.equals(userSelection) || STATE_UNASSIGNED_ON.equals(userSelection)) {
                desiredState = SelectionState.ON;
            } else if (STATE_OFF_OFF.equals(userSelection) || STATE_ON_OFF.equals(userSelection) || STATE_UNASSIGNED_OFF.equals(userSelection)) {
                desiredState = SelectionState.OFF;
            }
        }

        return desiredState;
    }


    private SelectionState getDesiredZoomState(boolean desiredButtonState, String userSelection) {
        SelectionState desiredState = SelectionState.UNASSIGNED;

        if (desiredButtonState) {
            if (STATE_ZOOM1_ZOOM2.equals(userSelection)
                    || STATE_ZOOM1_ALL.equals(userSelection)
                    || STATE_ZOOM1_ZOOM1.equals(userSelection)
                    || STATE_ZOOM1_ZOOM2.equals(userSelection)
                    || STATE_ZOOM1_ZOOM_DEFAULT.equals(userSelection)
            ) {
                desiredState = SelectionState.ZOOM1;
            } else if (STATE_ZOOM2_ZOOM1.equals(userSelection)
                    || STATE_ZOOM2_ZOOM2.equals(userSelection)) {
                desiredState = SelectionState.ZOOM2;
            } else if (STATE_ZOOM_DEFAULT_ZOOM1.equals(userSelection)
                    || STATE_ZOOM_DEFAULT_ALL.equals(userSelection)
                    || STATE_ZOOM_DEFAULT_DEFAULT.equals(userSelection)
            ) {
                desiredState = SelectionState.ZOOM_DEFAULT;
            } else if (STATE_ZOOM_ALL_ZOOM_DEFAULT.equals(userSelection)
                    || STATE_ZOOM_ALL_ALL.equals(userSelection)
                    || STATE_ZOOM_ALL_ZOOM1.equals(userSelection)
            ) {
                desiredState = SelectionState.ZOOM_ALL;
            }
        } else {
            if (STATE_ZOOM1_ZOOM1.equals(userSelection)
                    || STATE_ZOOM_ALL_ZOOM1.equals(userSelection)
                    || STATE_ZOOM2_ZOOM1.equals(userSelection)
                    || STATE_UNASSIGNED_ZOOM1.equals(userSelection)
                    || STATE_ZOOM_DEFAULT_ZOOM1.equals(userSelection)
            ) {
                desiredState = SelectionState.ZOOM1;
            } else if (STATE_ZOOM1_ZOOM2.equals(userSelection)
                    || STATE_ZOOM2_ZOOM2.equals(userSelection)
                    || STATE_UNASSIGNED_ZOOM2.equals(userSelection)) {
                desiredState = SelectionState.ZOOM2;
            } else if (STATE_ZOOM1_ZOOM_DEFAULT.equals(userSelection)
                    || STATE_UNASSIGNED_ZOOM_DEFAULT.equals(userSelection)
                    || STATE_ZOOM_ALL_ZOOM_DEFAULT.equals(userSelection)
                    || STATE_ZOOM_DEFAULT_DEFAULT.equals(userSelection)
            ) {
                desiredState = SelectionState.ZOOM_DEFAULT;
            } else if (STATE_ZOOM_DEFAULT_ALL.equals(userSelection)
                    || STATE_UNASSIGNED_ZOOM_ALL.equals(userSelection)
                    || STATE_ZOOM_ALL_ALL.equals(userSelection)
                    || STATE_ZOOM1_ALL.equals(userSelection)
            ) {
                desiredState = SelectionState.ZOOM_ALL;
            }
        }


        return desiredState;
    }

    public void zoomWithDefaultAspect(ProductSceneView view) {
        if (view != null) {
            view.getLayerCanvas().zoomWithDefaultAspect();
        }
    }




    public void shiftCenter(final boolean centerX, boolean centerY, double shiftX, double shiftY, ProductSceneView view) {
        if (view != null) {
            double offsetX;
            if (centerX) {
                offsetX = view.getLayerCanvas().getViewport().getOffsetX() - (view.getLayerCanvas().getMaxVisibleModelBounds().getWidth() * shiftX)/100.0;
            } else {
                offsetX = - view.getLayerCanvas().getMaxVisibleModelBounds().getWidth()*shiftX/100.0;
            }

            double offsetY;
            if (centerY) {
                offsetY = view.getLayerCanvas().getViewport().getOffsetY() - (view.getLayerCanvas().getMaxVisibleModelBounds().getHeight() * shiftY)/100.0;
            } else {
                offsetY = - view.getLayerCanvas().getMaxVisibleModelBounds().getHeight()*shiftY/100.0;
            }

            view.getLayerCanvas().getViewport().setOffset(offsetX,offsetY);
        }
    }



    public void zoom(final double zoomFactor, ProductSceneView view) {
        if (view != null && zoomFactor > 0) {
            zoomAll(view);
            if (zoomFactor != 100.0) {
                double zoomAllFactor = view.getLayerCanvas().getViewport().getZoomFactor()/100;
                view.getLayerCanvas().getViewport().setZoomFactor(zoomFactor * zoomAllFactor);
            }
//            maybeSynchronizeCompatibleProductViews();
        }
    }


    public void zoomAll(ProductSceneView view) {
        if (view != null) {
            view.getLayerCanvas().zoomAll();
//            maybeSynchronizeCompatibleProductViews();
        }
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
                if (ProductUtils.canGetPixelPos(view.getRaster())) {
                    if (view.validGeoCorners()) {
                        view.setGraticuleOverlayEnabled(true);
                    }
                }
            } else {
                view.setGraticuleOverlayEnabled(false);
            }
        }


        if (metadataOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (metadataOverlayDesiredState == SelectionState.ON) {
                if (!view.isRGB()) {
                    view.setMetaDataOverlayEnabled(true);
                }
            } else {
                view.setMetaDataOverlayEnabled(false);
            }
        }

        if (colorBarLegendOverlayDesiredState != SelectionState.UNASSIGNED) {
            if (colorBarLegendOverlayDesiredState == SelectionState.ON) {
                if (!view.isRGB()) {
                    view.setColorBarOverlayEnabled(true);
                }
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


        if (maskListOverlayDesiredState != SelectionState.UNASSIGNED && masksArrayList != null && masksArrayList.size() > 0) {

            // turn on parent level masks folder if any masks are in list
            if (maskListOverlayDesiredState == SelectionState.ON) {
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
            } else {
                for (String maskName : masksArrayList) {
                    // Delete all entries for this maskName
                    Mask mask = view.getRaster().getOverlayMaskGroup().get(maskName);
                    while (mask != null) {
                        view.getRaster().getOverlayMaskGroup().remove(mask);
                        mask = view.getRaster().getOverlayMaskGroup().get(maskName);
                    }
                }
            }
        }


        if (zoomDesiredState != SelectionState.UNASSIGNED) {
            if (zoomDesiredState == SelectionState.ZOOM1) {
                zoom(zoomFactor1, view);
                shiftCenter(centerX1,centerY1,shiftX1,shiftY1,view);

            } else if (zoomDesiredState == SelectionState.ZOOM2) {
                zoom(zoomFactor2, view);
                shiftCenter(centerX2,centerY2,shiftX2,shiftY2,view);

            } else if (zoomDesiredState == SelectionState.ZOOM_DEFAULT) {
                zoomWithDefaultAspect(view);
            } else if (zoomDesiredState == SelectionState.ZOOM_ALL) {
                zoomAll(view);
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
