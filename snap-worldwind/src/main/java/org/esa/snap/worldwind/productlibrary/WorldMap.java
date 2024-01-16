package org.esa.snap.worldwind.productlibrary;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * The interface contains method to access information from an world panel.
 *
 * Created by jcoravu on 22/10/2019.
 */
public interface WorldMap {

    public Point.Double convertPointToDegrees(Point point);

    public void setSelectedArea(Rectangle2D selectedArea);

    public void refresh();

    public void enableSelection();

    public void disableSelection();

    public Rectangle2D getSelectedArea();
}
