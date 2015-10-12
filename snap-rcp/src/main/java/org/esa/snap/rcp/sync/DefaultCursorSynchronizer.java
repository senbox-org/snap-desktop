package org.esa.snap.rcp.sync;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.ui.product.ProductSceneView;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
public class DefaultCursorSynchronizer {

    private Map<ProductSceneView, ImageCursorOverlay> psvOverlayMap;
    private boolean enabled;

    public DefaultCursorSynchronizer() {
        psvOverlayMap = new HashMap<>();
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            if (enabled) {
                initPsvOverlayMap();
            } else {
                clearPsvOverlayMap();
            }
            this.enabled = enabled;
        }
    }

    public void updateCursorOverlays(GeoPos geoPos) {
        if (!isEnabled()) {
            return;
        }
        for (Map.Entry<ProductSceneView, ImageCursorOverlay> entry : psvOverlayMap.entrySet()) {
            final ProductSceneView view = entry.getKey();
            ImageCursorOverlay overlay = entry.getValue();
            if (overlay == null) {
                    overlay = new ImageCursorOverlay(view, geoPos);
                    psvOverlayMap.put(view, overlay);
                    view.getLayerCanvas().addOverlay(overlay);
            } else {
                    overlay.setGeoPosition(geoPos);
                    view.getLayerCanvas().repaint();
            }
        }
    }

    private void initPsvOverlayMap() {
        WindowUtilities.getOpened(ProductSceneViewTopComponent.class)
                .map(ProductSceneViewTopComponent::getView)
                .forEach(this::addPPL);
    }

    private void clearPsvOverlayMap() {
        for (Map.Entry<ProductSceneView, ImageCursorOverlay> entry : psvOverlayMap.entrySet()) {
            final ProductSceneView view = entry.getKey();
            view.getLayerCanvas().removeOverlay(entry.getValue());
        }
        psvOverlayMap.clear();
    }

    private void addPPL(ProductSceneView view) {
        GeoCoding geoCoding = view.getProduct().getSceneGeoCoding();
        if (geoCoding != null && geoCoding.canGetPixelPos()) {
            psvOverlayMap.put(view, null);
        }
    }

}
