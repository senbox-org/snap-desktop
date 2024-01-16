package org.esa.snap.worldwind.productlibrary;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Rendering;
import org.esa.snap.remote.products.repository.geometry.GeometryUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The panel containing the 2D earth globe.
 *
 * Created by jcoravu on 21/10/2019.
 */
public class WorldMap2DPanel extends LayerCanvas implements WorldMap {

    private static final LayerType LAYER_TYPE = LayerTypeRegistry.getLayerType("org.esa.snap.worldmap.BlueMarbleLayerType");

    private final Map2DMouseHandler map2DSelector;

    public WorldMap2DPanel(PolygonsLayerModel polygonsLayerModel) {
        super();

        getModel().getViewport().setModelYAxisDown(false);

        this.map2DSelector = new Map2DMouseHandler(this);

        addMouseListener(this.map2DSelector);
        addMouseMotionListener(this.map2DSelector);
        addMouseWheelListener(this.map2DSelector);

        addOverlay(new SelectionAreaOverlay());

        addOverlay(new Polygons2DLayer(polygonsLayerModel));

        Layer rootLayer = getLayer();
        Layer worldMapLayer = LAYER_TYPE.createLayer(new WorldMapLayerContext(rootLayer), new PropertyContainer());
        rootLayer.getChildren().add(worldMapLayer);
        getViewport().zoom(worldMapLayer.getModelBounds());
    }

    @Override
    public void setSelectedArea(Rectangle2D selectedArea) {
        this.map2DSelector.setSelectedArea(selectedArea);
    }

    @Override
    public void refresh() {
        repaint();
    }

    @Override
    public void enableSelection() {
        this.map2DSelector.enable();
    }

    @Override
    public void disableSelection() {
        this.map2DSelector.disable();
    }

    @Override
    public Rectangle2D getSelectedArea() {
        return this.map2DSelector.getSelectedArea();
    }

    @Override
    public Point.Double convertPointToDegrees(Point point) {
        AffineTransform viewToModelTransform = getViewport().getViewToModelTransform();
        Point.Double clickedPoint = new Point.Double();
        viewToModelTransform.transform(point, clickedPoint);
        return clickedPoint;
    }

    private static class WorldMapLayerContext implements LayerContext {

        private final Layer rootLayer;

        private WorldMapLayerContext(Layer rootLayer) {

            this.rootLayer = rootLayer;
        }

        @Override
        public Object getCoordinateReferenceSystem() {
            return DefaultGeographicCRS.WGS84;
        }

        @Override
        public Layer getRootLayer() {
            return rootLayer;
        }
    }

    private class SelectionAreaOverlay implements LayerCanvas.Overlay {

        public SelectionAreaOverlay() {
        }

        @Override
        public void paintOverlay(LayerCanvas canvas, Rendering rendering) {
            Rectangle2D selectionArea = map2DSelector.getSelectedArea();
            if (selectionArea != null) {
                Graphics2D g2d = rendering.getGraphics();
                AffineTransform transform = canvas.getViewport().getModelToViewTransform();

                Path2D.Double path = GeometryUtils.buildPath(selectionArea);
                path.transform(transform);

                g2d.setColor(WorldMapPanelWrapper.SELECTION_FILL_COLOR);
                g2d.fill(path);

                g2d.setColor(WorldMapPanelWrapper.SELECTION_BORDER_COLOR);
                g2d.setStroke(new BasicStroke(WorldMapPanelWrapper.SELECTION_LINE_WIDTH));
                g2d.draw(path);
            }
        }
    }
}
