package org.esa.snap.product.library.ui.v2;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Created by jcoravu on 16/8/2019.
 */
public class NoSelectionRectangleFilter extends AbstractFilterProducts {

    public NoSelectionRectangleFilter() {

    }

    @Override
    public String getName() {
        return " ";
    }

    @Override
    public boolean matches(Path2D.Double item, Rectangle2D.Double filterValue) {
        return true;
    }
}
