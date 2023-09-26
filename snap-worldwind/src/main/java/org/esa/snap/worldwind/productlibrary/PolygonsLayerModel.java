package org.esa.snap.worldwind.productlibrary;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The layer containing the polygon coordinates of the repository products.
 *
 * Created by jcoravu on 10/9/2019.
 */
public class PolygonsLayerModel {

    private final Collection<CustomPolyline> renderables;

    public PolygonsLayerModel() {
        this.renderables = new ArrayList<>();
    }

    public List<Path2D.Double> findPolygonsContainsPoint(double longitude, double latitude) {
        List<Path2D.Double> polygonPaths = new ArrayList<>();
        Iterator<CustomPolyline> it1 = this.renderables.iterator();
        while (it1.hasNext()) {
            CustomPolyline polyline = it1.next();
            if (polyline.getPath().contains(longitude, latitude)) {
                polygonPaths.add(polyline.getPath());
            }
        }
        return polygonPaths;
    }

    public Collection<CustomPolyline> getRenderables() {
        return renderables;
    }

    public void setPolygons(Path2D.Double[] polygonPaths) {
        List<CustomPolyline> polygonsToRemove = new ArrayList<>();
        Iterator<CustomPolyline> it1 = this.renderables.iterator();
        while (it1.hasNext()) {
            CustomPolyline polyline = it1.next();
            boolean found = false;
            for (int i=0; i<polygonPaths.length && !found; i++) {
                if (polyline.getPath() == polygonPaths[i]) {
                    found = true;
                }
            }
            if (!found) {
                polygonsToRemove.add(polyline);
            }
        }
        for (int i=0; i<polygonsToRemove.size(); i++) {
            this.renderables.remove(polygonsToRemove.get(i));
        }

        for (int i=0; i<polygonPaths.length; i++) {
            Iterator<CustomPolyline> it2 = this.renderables.iterator();
            boolean found = false;
            while (it2.hasNext() && !found) {
                CustomPolyline polyline = it2.next();
                if (polyline.getPath() == polygonPaths[i]) {
                    found = true;
                }
            }
            if (!found) {
                CustomPolyline polyline = new CustomPolyline(polygonPaths[i]);
                polyline.setFollowTerrain(true);
                polyline.setColor(WorldMapPanelWrapper.POLYGON_BORDER_COLOR);
                polyline.setHighlightColor(WorldMapPanelWrapper.POLYGON_HIGHLIGHT_BORDER_COLOR);
                polyline.setLineWidth(WorldMapPanelWrapper.POLYGON_LINE_WIDTH);
                this.renderables.add(polyline);
            }
        }
    }

    public void highlightPolygons(Path2D.Double[] polygonPaths) {
        Iterator<CustomPolyline> it1 = this.renderables.iterator();
        while (it1.hasNext()) {
            CustomPolyline polyline = it1.next();
            boolean found = false;
            for (int i=0; i<polygonPaths.length && !found; i++) {
                if (polyline.getPath() == polygonPaths[i]) {
                    found = true;
                }
            }
            if (!found) {
                polyline.setHighlighted(false);
            }
        }

        for (int i=0; i<polygonPaths.length; i++) {
            highlightPolygon(polygonPaths[i]);
        }
    }

    private boolean highlightPolygon(Path2D.Double polygonPath) {
        Iterator<CustomPolyline> it = this.renderables.iterator();
        while (it.hasNext()) {
            CustomPolyline polyline = it.next();
            if (polyline.getPath() == polygonPath) {
                polyline.setHighlighted(true);
                return true; // the polygon exists
            }
        }
        return false;
    }
}
