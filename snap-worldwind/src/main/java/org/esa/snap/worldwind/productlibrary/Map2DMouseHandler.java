package org.esa.snap.worldwind.productlibrary;

import com.bc.ceres.glayer.swing.LayerCanvas;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.remote.products.repository.geometry.PolygonArea;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class for receiving mouse events for 2D earth globe.
 *
 * Created by jcoravu on 21/10/2019.
 */
public class Map2DMouseHandler extends MouseAdapter {

    private final LayerCanvas layerCanvas;

    private Point mousePressedPosition;
    private Point.Double selectionStart;
    private Point.Double selectionEnd;

    // polygon selection: vertices while drawing, the mouse position for the next edge, and the finished polygon
    private boolean drawingPolygon;
    private final List<Point.Double> polygonVertices = new ArrayList<>();
    private Point.Double polygonCursor;
    private PolygonArea polygonArea;
    // the click of a press that drew the polygon, not to be taken as a click on the map
    private boolean consumeClick;

    public Map2DMouseHandler(LayerCanvas layerCanvas) {
        this.layerCanvas = layerCanvas;
        setDefaultCursor();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        int wheelRotation = -mouseWheelEvent.getWheelRotation();
        double newZoomFactor = this.layerCanvas.getViewport().getZoomFactor() * FastMath.pow(1.1, wheelRotation);
        this.layerCanvas.getViewport().setZoomFactor(newZoomFactor);
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if (this.drawingPolygon && SwingUtilities.isLeftMouseButton(mouseEvent)) {
            this.consumeClick = true;
            mouseEvent.consume();
            if (mouseEvent.getClickCount() >= 2) {
                finishPolygon();
            } else {
                this.polygonVertices.add(toModel(mouseEvent.getPoint()));
            }
            this.layerCanvas.repaint();
            return;
        }
        if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            if (isSelectionCursor()) {
                this.polygonArea = null; // a new rectangle replaces a polygon
                AffineTransform viewToModelTransform = this.layerCanvas.getViewport().getViewToModelTransform();
                this.selectionStart = new Point.Double();
                this.selectionEnd = null;
                viewToModelTransform.transform(mouseEvent.getPoint(), this.selectionStart);
                clampToWorld(this.selectionStart);
                this.layerCanvas.repaint();
            }
            this.mousePressedPosition = mouseEvent.getPoint();
        } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            this.mousePressedPosition = mouseEvent.getPoint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        if (this.drawingPolygon) {
            mouseEvent.consume();
            return;
        }
        this.mousePressedPosition = null;
        setDefaultCursor();
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (this.consumeClick) {
            this.consumeClick = false;
            mouseEvent.consume();
        }
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        if (this.drawingPolygon) {
            this.polygonCursor = toModel(mouseEvent.getPoint());
            this.layerCanvas.repaint();
        }
    }

    private Point.Double toModel(Point viewPoint) {
        Point.Double modelPoint = new Point.Double();
        this.layerCanvas.getViewport().getViewToModelTransform().transform(viewPoint, modelPoint);
        clampToWorld(modelPoint);
        return modelPoint;
    }

    private void finishPolygon() {
        PolygonArea polygon = buildPolygonArea(this.polygonVertices);
        if (polygon == null) {
            return; // fewer than three vertices: keep drawing
        }
        this.polygonArea = polygon;
        this.selectionStart = null;
        this.selectionEnd = null;
        stopDrawingPolygon();
    }

    private void stopDrawingPolygon() {
        this.drawingPolygon = false;
        this.polygonVertices.clear();
        this.polygonCursor = null;
        setDefaultCursor();
    }

    /**
     * The polygon of the given vertices, or null if they do not make one (fewer than three distinct vertices, or no
     * area).
     */
    static PolygonArea buildPolygonArea(List<Point.Double> vertices) {
        List<Point.Double> distinct = new ArrayList<>();
        for (Point.Double vertex : vertices) {
            if (distinct.isEmpty() || !distinct.get(distinct.size() - 1).equals(vertex)) {
                distinct.add(vertex);
            }
        }
        if (distinct.size() > 1 && distinct.get(0).equals(distinct.get(distinct.size() - 1))) {
            distinct.remove(distinct.size() - 1);
        }
        if (distinct.size() < 3) {
            return null;
        }
        double[] lons = new double[distinct.size()];
        double[] lats = new double[distinct.size()];
        for (int i = 0; i < distinct.size(); i++) {
            lons[i] = distinct.get(i).x;
            lats[i] = distinct.get(i).y;
        }
        // shoelace formula: collinear vertices enclose no area
        double twiceArea = 0;
        for (int i = 0; i < lons.length; i++) {
            int next = (i + 1) % lons.length;
            twiceArea += lons[i] * lats[next] - lons[next] * lats[i];
        }
        return Math.abs(twiceArea) > 1.0e-12 ? new PolygonArea(lons, lats) : null;
    }

    /**
     * The outline being drawn, up to the mouse position, or null when no polygon is being drawn.
     */
    Path2D.Double getPolygonInProgress() {
        if (!this.drawingPolygon || this.polygonVertices.isEmpty()) {
            return null;
        }
        Path2D.Double path = new Path2D.Double();
        path.moveTo(this.polygonVertices.get(0).x, this.polygonVertices.get(0).y);
        for (int i = 1; i < this.polygonVertices.size(); i++) {
            path.lineTo(this.polygonVertices.get(i).x, this.polygonVertices.get(i).y);
        }
        if (this.polygonCursor != null) {
            path.lineTo(this.polygonCursor.x, this.polygonCursor.y);
        }
        return path;
    }

    boolean isDrawingPolygon() {
        return this.drawingPolygon;
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        if (this.drawingPolygon) {
            return;
        }
        if (this.mousePressedPosition != null) {
            if (isSelectionCursor()) {
                AffineTransform viewToModelTransform = this.layerCanvas.getViewport().getViewToModelTransform();
                Point.Double selectionPoint = new Point.Double();
                viewToModelTransform.transform(mouseEvent.getPoint(), selectionPoint);
                // the map can be panned beyond the world extent: keep the selection inside valid coordinates
                clampToWorld(selectionPoint);
                this.selectionEnd = selectionPoint;
                this.layerCanvas.repaint();
            } else {
                Point newMouseDraggedPosition = mouseEvent.getPoint();
                double dx = newMouseDraggedPosition.x - this.mousePressedPosition.x;
                double dy = newMouseDraggedPosition.y - this.mousePressedPosition.y;
                this.layerCanvas.getViewport().moveViewDelta(dx, dy);
                this.mousePressedPosition = newMouseDraggedPosition;
            }
        }
    }

    public void setSelectedArea(Rectangle2D selectedArea) {
        if (this.drawingPolygon) {
            stopDrawingPolygon();
        }
        this.polygonArea = (selectedArea instanceof PolygonArea) ? (PolygonArea) selectedArea : null;
        if (selectedArea == null || this.polygonArea != null) {
            this.selectionStart = null;
            this.selectionEnd = null;
        } else {
            this.selectionStart = new Point.Double(selectedArea.getX(), selectedArea.getY());
            this.selectionEnd = new Point.Double(selectedArea.getX() + selectedArea.getWidth(), selectedArea.getY() + selectedArea.getHeight());
        }
    }

    public Rectangle2D getSelectedArea() {
        if (this.polygonArea != null) {
            return this.polygonArea;
        }
        return buildSelectedArea(this.selectionStart, this.selectionEnd);
    }

    /**
     * Builds the selection rectangle spanned by two corner points dragged in any direction, or null if the
     * selection is incomplete or has no area.
     */
    static Rectangle2D buildSelectedArea(Point.Double start, Point.Double end) {
        if (start == null || end == null) {
            return null;
        }
        final double minX = Math.min(start.x, end.x);
        final double minY = Math.min(start.y, end.y);
        final double width = Math.abs(end.x - start.x);
        final double height = Math.abs(end.y - start.y);
        if (width <= 0.0d || height <= 0.0d) {
            return null;
        }
        return new Rectangle2D.Double(minX, minY, width, height);
    }

    static void clampToWorld(Point.Double point) {
        point.x = Math.max(-180.0d, Math.min(180.0d, point.x));
        point.y = Math.max(-90.0d, Math.min(90.0d, point.y));
    }

    public void enable() {
        if (this.drawingPolygon) {
            stopDrawingPolygon();
        }
        setSelectionCursor();
    }

    /**
     * Starts drawing a polygon: each click adds a vertex, a double click closes the polygon.
     */
    public void enablePolygon() {
        this.selectionStart = null;
        this.selectionEnd = null;
        this.polygonArea = null;
        this.polygonVertices.clear();
        this.polygonCursor = null;
        this.drawingPolygon = true;
        setSelectionCursor();
        this.layerCanvas.repaint();
    }

    public void disable() {
        stopDrawingPolygon();
        this.selectionStart = null;
        this.selectionEnd = null;
        this.polygonArea = null;
        this.layerCanvas.repaint();
    }

    private boolean isSelectionCursor() {
        return (this.layerCanvas.getCursor() == WorldMapPanelWrapper.SELECTION_CURSOR);
    }

    private void setSelectionCursor() {
        this.layerCanvas.setCursor(WorldMapPanelWrapper.SELECTION_CURSOR);
    }

    private void setDefaultCursor() {
        this.layerCanvas.setCursor(WorldMapPanelWrapper.DEFAULT_CURSOR);
    }
}