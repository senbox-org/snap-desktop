package org.esa.snap.product.library.ui.v2.worldwind;

import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Renderable;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jcoravu on 10/9/2019.
 */
public class PolygonLayer extends RenderableLayer {

    public PolygonLayer() {
    }

    public List<Path2D.Double> findPolygonsContainsPoint(double longitude, double latitude) {
        List<Path2D.Double> polygonPaths = new ArrayList<>();
        Iterator<Renderable> it1 = this.renderables.iterator();
        while (it1.hasNext()) {
            CustomPolyline polyline = (CustomPolyline)it1.next();
            if (polyline.getPath().contains(longitude, latitude)) {
                polygonPaths.add(polyline.getPath());
            }
        }
        return polygonPaths;
    }

    public void setPolygons(Path2D.Double[] polygonPaths) {
        List<CustomPolyline> polygonsToRemove = new ArrayList<>();
        Iterator<Renderable> it1 = this.renderables.iterator();
        while (it1.hasNext()) {
            CustomPolyline polyline = (CustomPolyline)it1.next();
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
            removeRenderable(polygonsToRemove.get(i));
        }

        for (int i=0; i<polygonPaths.length; i++) {
            Iterator<Renderable> it2 = this.renderables.iterator();
            boolean found = false;
            while (it2.hasNext() && !found) {
                CustomPolyline polyline = (CustomPolyline)it2.next();
                if (polyline.getPath() == polygonPaths[i]) {
                    found = true;
                }
            }
            if (!found) {
                CustomPolyline polyline = new CustomPolyline(polygonPaths[i]);
                polyline.setFollowTerrain(true);
                polyline.setColor(new Color(1f, 1f, 1f, 0.99f));
                polyline.setHighlightColor(Color.RED);
                polyline.setLineWidth(1);
                addRenderable(polyline);
            }
        }
    }

    public void highlightPolygons(Path2D.Double[] polygonPaths) {
        Iterator<Renderable> it1 = this.renderables.iterator();
        while (it1.hasNext()) {
            CustomPolyline polyline = (CustomPolyline)it1.next();
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
            Iterator<Renderable> it2 = this.renderables.iterator();
            boolean found = false;
            while (it2.hasNext() && !found) {
                CustomPolyline polyline = (CustomPolyline)it2.next();
                if (polyline.getPath() == polygonPaths[i]) {
                    polyline.setHighlighted(true);
                    found = true;
                }
            }
        }
    }

    public boolean highlightPolygon(Path2D.Double polygonPath) {
        Iterator<Renderable> it = this.renderables.iterator();
        while (it.hasNext()) {
            CustomPolyline polyline = (CustomPolyline)it.next();
            if (polyline.getPath() == polygonPath) {
                polyline.setHighlighted(true);
                return true; // the polygon exists
            }
        }
        return false;
    }
}
