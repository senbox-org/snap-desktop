package org.esa.snap.worldwind.productlibrary;

import java.awt.geom.Path2D;
import java.util.List;

/**
 * The listener interface for receiving left mouse click events.
 *
 * Created by jcoravu on 11/9/2019.
 */
public interface PolygonMouseListener {

    public void leftMouseButtonClicked(List<Path2D.Double> polygonPaths);
}
