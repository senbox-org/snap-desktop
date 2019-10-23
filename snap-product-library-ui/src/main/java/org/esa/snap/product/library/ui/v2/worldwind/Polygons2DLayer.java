package org.esa.snap.product.library.ui.v2.worldwind;

import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Rendering;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import org.esa.snap.remote.products.repository.Polygon2D;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * Created by jcoravu on 10/9/2019.
 */
public class Polygons2DLayer implements LayerCanvas.Overlay {

    private final PolygonsLayerModel polygonsLayerModel;

    public Polygons2DLayer(PolygonsLayerModel polygonsLayerModel) {
        this.polygonsLayerModel = polygonsLayerModel;
    }

    @Override
    public void paintOverlay(LayerCanvas canvas, Rendering rendering) {
        Iterator<CustomPolyline> iterator = this.polygonsLayerModel.getRenderables().iterator();
        Graphics2D g2d = rendering.getGraphics();
        AffineTransform modelToViewTransform = canvas.getViewport().getModelToViewTransform();
        while (iterator.hasNext()) {
            CustomPolyline polygon = iterator.next();
            Path2D.Double path = (Path2D.Double) polygon.getPath().clone();
            path.transform(modelToViewTransform);

            if (polygon.isHighlighted()) {
                g2d.setColor(WorldMapPanelWrapper.POLYGON_HIGHLIGHT_BORDER_COLOR);
                g2d.setStroke(new BasicStroke(WorldMapPanelWrapper.POLYGON_LINE_WIDTH * 2.0f));
                g2d.draw(path);
            }

            g2d.setColor(WorldMapPanelWrapper.POLYGON_BORDER_COLOR);
            g2d.setStroke(new BasicStroke(WorldMapPanelWrapper.POLYGON_LINE_WIDTH));
            g2d.draw(path);
        }
    }
}
