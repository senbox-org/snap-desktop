package org.esa.snap.product.library.ui.v2.worldwind;

import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Created by jcoravu on 22/10/2019.
 */
public interface WorldMap {

    public Point.Double convertPointToDegrees(Point point);

    public void setSelection(Rectangle2D selectionArea);

    public void refresh();

    public void enableSelection();

    public void disableSelection();

    public Rectangle2D getSelectedArea();
}
