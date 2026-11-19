/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.worldwind;

import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.worldwind.model.ImageViewGeoUtils;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Shows the globe cursor in SNAP's image views (see docs/WorldWind-Review.md, section 6.2): a crosshair overlay
 * in every open image view whose raster contains the geo-position under the mouse on the globe.
 * <p>
 * Called on the EDT.
 */
public class GlobeCursorSync {

    private final Map<ProductSceneView, CursorOverlay> overlays = new WeakHashMap<>();
    private final Supplier<List<ProductSceneView>> openViews;
    private boolean enabled = true;

    public GlobeCursorSync() {
        this(GlobeCursorSync::findOpenViews);
    }

    GlobeCursorSync(final Supplier<List<ProductSceneView>> openViews) {
        this.openViews = openViews;
    }

    static List<ProductSceneView> findOpenViews() {
        final List<ProductSceneView> views = new ArrayList<>();
        for (TopComponent component : WindowManager.getDefault().getRegistry().getOpened()) {
            if (component instanceof ProductSceneViewTopComponent) {
                final ProductSceneView view = ((ProductSceneViewTopComponent) component).getView();
                if (view != null) {
                    views.add(view);
                }
            }
        }
        return views;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clear();
        }
    }

    /**
     * @param geoPos the position under the mouse on the globe, or null if the mouse is not over the globe
     */
    public void setGlobePosition(final GeoPos geoPos) {
        if (!enabled || geoPos == null || !geoPos.isValid()) {
            clear();
            return;
        }
        final List<ProductSceneView> views = openViews.get();
        // views closed in the meantime
        for (ProductSceneView view : new ArrayList<>(overlays.keySet())) {
            if (!views.contains(view)) {
                removeOverlay(view);
            }
        }
        for (ProductSceneView view : views) {
            final PixelPos pixelPos = view.getRaster() != null ?
                    ImageViewGeoUtils.toRasterPixel(view.getRaster(), geoPos) : null;
            if (pixelPos == null) {
                removeOverlay(view);
                continue;
            }
            CursorOverlay overlay = overlays.get(view);
            if (overlay == null) {
                overlay = new CursorOverlay(view);
                overlays.put(view, overlay);
                view.getLayerCanvas().addOverlay(overlay);
            }
            overlay.pixelPos = pixelPos;
            view.getLayerCanvas().repaint();
        }
    }

    public void clear() {
        for (ProductSceneView view : new ArrayList<>(overlays.keySet())) {
            removeOverlay(view);
        }
    }

    private void removeOverlay(final ProductSceneView view) {
        final CursorOverlay overlay = overlays.remove(view);
        if (overlay != null) {
            view.getLayerCanvas().removeOverlay(overlay);
            view.getLayerCanvas().repaint();
        }
    }

    PixelPos getCursorPixel(final ProductSceneView view) {
        final CursorOverlay overlay = overlays.get(view);
        return overlay != null ? overlay.pixelPos : null;
    }

    /**
     * Crosshair at the centre of a raster pixel, drawn in the view's colours inverted so that it stays visible.
     */
    static class CursorOverlay implements LayerCanvas.Overlay {

        private static final int HALF_SIZE = 10;
        private static final int GAP = 3;

        private final ProductSceneView view;
        PixelPos pixelPos;

        CursorOverlay(final ProductSceneView view) {
            this.view = view;
        }

        @Override
        public void paintOverlay(final LayerCanvas canvas, final Rendering rendering) {
            final PixelPos pp = pixelPos;
            if (pp == null) {
                return;
            }
            final Viewport viewport = canvas.getViewport();
            final AffineTransform i2v = new AffineTransform(viewport.getModelToViewTransform());
            i2v.concatenate(view.getBaseImageLayer().getImageToModelTransform());
            final Point2D centre = i2v.transform(new Point2D.Double(Math.floor(pp.x) + 0.5, Math.floor(pp.y) + 0.5), null);

            final Graphics2D graphics = rendering.getGraphics();
            graphics.setStroke(new BasicStroke(1F));
            graphics.setColor(Color.WHITE);
            graphics.setXORMode(Color.BLACK);
            final double x = centre.getX(), y = centre.getY();
            graphics.draw(new Line2D.Double(x - HALF_SIZE, y, x - GAP, y));
            graphics.draw(new Line2D.Double(x + GAP, y, x + HALF_SIZE, y));
            graphics.draw(new Line2D.Double(x, y - HALF_SIZE, x, y - GAP));
            graphics.draw(new Line2D.Double(x, y + GAP, x, y + HALF_SIZE));
            graphics.setPaintMode();
        }
    }
}
