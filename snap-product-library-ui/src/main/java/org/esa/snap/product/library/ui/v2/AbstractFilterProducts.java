package org.esa.snap.product.library.ui.v2;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Created by jcoravu on 16/8/2019.
 */
public abstract class AbstractFilterProducts implements IFilterItems<Path2D.Double, Rectangle2D.Double> {

    protected AbstractFilterProducts() {

    }

    public abstract String getName();
}
