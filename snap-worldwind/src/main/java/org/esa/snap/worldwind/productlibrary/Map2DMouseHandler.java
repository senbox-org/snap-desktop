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
                this.layerCanvas.updateUI();
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
                if (selectionPoint.x >= -180.0d && selectionPoint.y <= 90.0d) {
                    if (selectionPoint.x <= 180.0d && selectionPoint.y >= -90.0d) {
                        this.selectionEnd = selectionPoint;
                        this.layerCanvas.updateUI();
                    }
                }
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
        if (this.selectionStart != null && this.selectionEnd != null) {
            if (this.selectionStart.x > this.selectionEnd.x || this.selectionStart.y > this.selectionEnd.y) {
                double x = this.selectionEnd.x;
                double y = this.selectionEnd.y;
                double width = this.selectionStart.x - this.selectionEnd.x;
                double height = this.selectionStart.y - this.selectionEnd.y;
                return new Rectangle2D.Double(x, y, width, height);
            }
            double x = this.selectionStart.x;
            double y = this.selectionStart.y;
            double width = this.selectionEnd.x - this.selectionStart.x;
            double height = this.selectionEnd.y - this.selectionStart.y;
            return new Rectangle2D.Double(x, y, width, height);
        }
        return null;
    }

    public void enable() {
        setSelectionCursor();
    }

    public void disable() {
        setDefaultCursor();
        this.selectionStart = null;
        this.selectionEnd = null;
        this.layerCanvas.updateUI();
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