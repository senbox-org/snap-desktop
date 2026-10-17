package org.esa.snap.worldwind.productlibrary;

import com.bc.ceres.glayer.swing.LayerCanvas;
import org.apache.commons.math3.util.FastMath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

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
        if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            if (isSelectionCursor()) {
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
        this.mousePressedPosition = null;
        setDefaultCursor();
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
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
        if (selectedArea == null) {
            this.selectionStart = null;
            this.selectionEnd = null;
        } else {
            this.selectionStart = new Point.Double(selectedArea.getX(), selectedArea.getY());
            this.selectionEnd = new Point.Double(selectedArea.getX() + selectedArea.getWidth(), selectedArea.getY() + selectedArea.getHeight());
        }
    }

    public Rectangle2D getSelectedArea() {
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
        setSelectionCursor();
    }

    public void disable() {
        setDefaultCursor();
        this.selectionStart = null;
        this.selectionEnd = null;
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