/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.layer.overlay;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.snap.core.dataop.maptransf.MapTransformDescriptor;
import org.esa.snap.core.layer.WorldMapLayerType;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.product.ProductSceneView;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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
@ActionID(category = "View", id = "OverlayWorldMapLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayWorldMapLayerActionName")
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.OVERLAY_WORLD_MAP_MENU_PATH,
                position = PackageDefaults.OVERLAY_WORLD_MAP_MENU_POSITION),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.OVERLAY_WORLD_MAP_TOOLBAR_NAME,
                position = PackageDefaults.OVERLAY_WORLD_MAP_TOOLBAR_POSITION)
})
@NbBundle.Messages({
        "CTL_OverlayWorldMapLayerActionName=" + PackageDefaults.OVERLAY_WORLD_MAP_NAME,
        "CTL_OverlayWorldMapLayerActionToolTip=" + PackageDefaults.OVERLAY_WORLD_MAP_DESCRIPTION
})
public final class OverlayWorldMapLayerAction extends AbstractOverlayAction {

    private static final String WORLDMAP_TYPE_PROPERTY_NAME = "worldmap.type";
    private static final String DEFAULT_LAYER_TYPE = "BlueMarbleLayerType";

    @Override
    protected void initActionProperties() {
        putValue(NAME, Bundle.CTL_OverlayWorldMapLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/WorldMapOverlay.png", false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.OVERLAY_WORLD_MAP_ICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayWorldMapLayerActionToolTip());
    }

    @Override
    protected boolean getActionSelectionState(ProductSceneView view) {
        Layer worldMapLayer = findWorldMapLayer(view);
        return worldMapLayer != null && worldMapLayer.isVisible();
    }

    @Override
    protected boolean getActionEnabledState(ProductSceneView view) {
        RasterDataNode raster = view.getRaster();
        return isGeographicLatLon(raster.getGeoCoding());
    }

    @Override
    protected void setOverlayEnableState(ProductSceneView view) {
        if (view != null) {
            Layer rootLayer = view.getRootLayer();
            Layer worldMapLayer = findWorldMapLayer(view);
            if (isSelected()) {
                if (worldMapLayer == null) {
                    worldMapLayer = createWorldMapLayer();
                    rootLayer.getChildren().add(worldMapLayer);
                }
                worldMapLayer.setVisible(true);
            } else {
                worldMapLayer.getParent().getChildren().remove(worldMapLayer);
            }
        }
    }


    private Layer createWorldMapLayer() {
        final LayerType layerType = getWorldMapLayerType();
        final PropertySet template = layerType.createLayerConfig(null);
        return layerType.createLayer(null, template);
    }

    private LayerType getWorldMapLayerType() {
        final SnapApp visatApp = SnapApp.getDefault();
        String layerTypeClassName = visatApp.getPreferences().get(WORLDMAP_TYPE_PROPERTY_NAME, DEFAULT_LAYER_TYPE);
        return LayerTypeRegistry.getLayerType(layerTypeClassName);
    }

    private Layer findWorldMapLayer(ProductSceneView view) {
        return LayerUtils.getChildLayer(view.getRootLayer(), LayerUtils.SearchMode.DEEP, layer -> layer.getLayerType() instanceof WorldMapLayerType);
    }

    private boolean isGeographicLatLon(GeoCoding geoCoding) {
        if (geoCoding instanceof MapGeoCoding) {
            MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
            MapTransformDescriptor transformDescriptor = mapGeoCoding.getMapInfo()
                    .getMapProjection().getMapTransform().getDescriptor();
            String typeID = transformDescriptor.getTypeID();
            if (typeID.equals(IdentityTransformDescriptor.TYPE_ID)) {
                return true;
            }
        } else if (geoCoding instanceof CrsGeoCoding) {
            return CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
        }
        return false;
    }


}
