package org.esa.snap.product.library.ui.v2.worldwind;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;

import java.util.Iterator;

/**
 * Created by jcoravu on 10/9/2019.
 */
public class Polygons3DLayer extends AbstractLayer {

    private final PolygonsLayerModel polygonsLayerModel;

    public Polygons3DLayer(PolygonsLayerModel polygonsLayerModel) {
        this.polygonsLayerModel = polygonsLayerModel;
    }

    @Override
    protected void doRender(DrawContext drawContext) {
        Iterator<CustomPolyline> var3 = this.polygonsLayerModel.getRenderables().iterator();

        while(var3.hasNext()) {
            Renderable renderable = var3.next();
            renderable.render(drawContext);
        }
    }
}
