package org.esa.snap.product.library.ui.v2;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

/**
 * Created by jcoravu on 16/8/2019.
 */
public class IntersectSelectionRectangleFilter extends AbstractFilterProducts {

    public IntersectSelectionRectangleFilter() {

    }

    @Override
    public String getName() {
        return "Intersect";
    }

    @Override
    public boolean matches(Path2D.Double productPath, Rectangle2D.Double selectionRectangle) {
        boolean intersect = false;
        if (productPath.contains(selectionRectangle.getX(), selectionRectangle.getY())) {
            intersect = true; // the left top corner
        } else if (productPath.contains(selectionRectangle.getX() + selectionRectangle.getWidth(), selectionRectangle.getY())) {
            intersect = true; // the right top corner
        } else if (productPath.contains(selectionRectangle.getX() + selectionRectangle.getWidth(), selectionRectangle.getY()+selectionRectangle.getHeight())) {
            intersect = true; // the right bottom corner
        } else if (productPath.contains(selectionRectangle.getX(), selectionRectangle.getY()+selectionRectangle.getHeight())) {
            intersect = true; // the left bottom corner
        } else {
            double[] point = new double[2];
            for (PathIterator it = productPath.getPathIterator(null); !it.isDone(); it.next()) {
                it.currentSegment(point);
                if (selectionRectangle.contains(point[0], point[1])) {
                    intersect = true;
                    break;
                }
            }
        }
        return intersect;
    }
}
