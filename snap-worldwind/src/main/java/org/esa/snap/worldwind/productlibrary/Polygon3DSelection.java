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
package org.esa.snap.worldwind.productlibrary;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfacePolyline;
import org.esa.snap.remote.products.repository.geometry.PolygonArea;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Polygon selection of an area of interest on the 3D globe: each click adds a vertex, a double click closes the
 * polygon. The clicks used for drawing are consumed, so that they do not also select footprints.
 */
public class Polygon3DSelection extends MouseAdapter {

    static final String ANTIMERIDIAN_MESSAGE = "Polygons crossing the antimeridian (180\u00b0) are not supported as " +
            "area of interest.\nKeep drawing on one side of it, or choose 'Clear selection' in the right-click menu.";

    private final WorldWindow wwd;
    private final RenderableLayer layer = new RenderableLayer();
    private final SurfacePolygon polygonShape = new SurfacePolygon();
    private final SurfacePolyline outlineShape = new SurfacePolyline();

    private boolean drawing;
    private final List<Point.Double> vertices = new ArrayList<>();
    private Point.Double cursor;
    private PolygonArea polygonArea;
    private boolean consumeClick;

    public Polygon3DSelection(WorldWindow wwd) {
        this.wwd = wwd;
        this.layer.setName("Polygon Selection");
        this.layer.setPickEnabled(false);

        final ShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setInteriorMaterial(new Material(WorldMapPanelWrapper.SELECTION_FILL_COLOR));
        attributes.setInteriorOpacity(WorldMapPanelWrapper.SELECTION_FILL_COLOR.getAlpha() / 255.0);
        attributes.setOutlineMaterial(new Material(WorldMapPanelWrapper.SELECTION_BORDER_COLOR));
        attributes.setOutlineWidth(WorldMapPanelWrapper.SELECTION_LINE_WIDTH);
        this.polygonShape.setAttributes(attributes);
        this.outlineShape.setAttributes(attributes);
        // straight edges in longitude and latitude, as the area is searched, not great circles
        this.polygonShape.setPathType(AVKey.LINEAR);
        this.outlineShape.setPathType(AVKey.LINEAR);

        wwd.getInputHandler().addMouseListener(this);
        wwd.getInputHandler().addMouseMotionListener(this);
    }

    public RenderableLayer getLayer() {
        return this.layer;
    }

    /**
     * Starts drawing a new polygon, replacing the current one.
     */
    public void start() {
        this.polygonArea = null;
        this.vertices.clear();
        this.cursor = null;
        this.drawing = true;
        update();
    }

    public void clear() {
        this.drawing = false;
        this.polygonArea = null;
        this.vertices.clear();
        this.cursor = null;
        update();
    }

    public boolean isDrawing() {
        return this.drawing;
    }

    /**
     * The finished polygon, or null.
     */
    public PolygonArea getSelectedArea() {
        return this.polygonArea;
    }

    /**
     * Shows a polygon area; any other area (a rectangle, or null) removes the polygon.
     */
    public void setSelectedArea(Rectangle2D area) {
        this.drawing = false;
        this.vertices.clear();
        this.cursor = null;
        this.polygonArea = (area instanceof PolygonArea) ? (PolygonArea) area : null;
        update();
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (!this.drawing || !SwingUtilities.isLeftMouseButton(event)) {
            return;
        }
        event.consume();
        this.consumeClick = true;
        if (event.getClickCount() >= 2) {
            if (crossesAntimeridian(this.vertices)) {
                // the polygon is kept in plain longitudes: across 180 degrees it would enclose the rest of the world
                Map2DMouseHandler.polygonRejected.accept(ANTIMERIDIAN_MESSAGE);
            } else {
                final PolygonArea polygon = Map2DMouseHandler.buildPolygonArea(this.vertices);
                if (polygon != null) {
                    this.polygonArea = polygon;
                    this.drawing = false;
                    this.vertices.clear();
                    this.cursor = null;
                } else if (this.vertices.size() >= 3) {
                    Map2DMouseHandler.polygonRejected.accept(Map2DMouseHandler.INVALID_POLYGON_MESSAGE);
                }
            }
        } else {
            final Point.Double position = toLonLat(event.getPoint());
            if (position != null) {
                this.vertices.add(position);
            }
        }
        update();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (this.drawing) {
            event.consume();
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (this.consumeClick) {
            this.consumeClick = false;
            event.consume();
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        if (this.drawing) {
            this.cursor = toLonLat(event.getPoint());
            update();
        }
    }

    /**
     * True if an edge of the closed polygon is shorter going across the antimeridian than along the parallels.
     */
    static boolean crossesAntimeridian(List<Point.Double> vertices) {
        for (int i = 0; i < vertices.size(); i++) {
            final Point.Double next = vertices.get((i + 1) % vertices.size());
            if (Math.abs(next.x - vertices.get(i).x) > 180.0) {
                return true;
            }
        }
        return false;
    }

    /**
     * The outline of a polygon area as WorldWind should draw it. The area is in plain longitudes, so an edge spanning
     * more than 180 degrees (possible on the 2D map) goes the long way round; WorldWind would take it across the
     * antimeridian. Such edges get intermediate points, no edge spanning more than 90 degrees.
     */
    static List<LatLon> displayLocations(PolygonArea area) {
        final List<LatLon> locations = new ArrayList<>();
        final int count = area.getVertexCount();
        for (int i = 0; i < count; i++) {
            final double lon = area.getLongitude(i);
            final double lat = area.getLatitude(i);
            locations.add(LatLon.fromDegrees(lat, lon));
            final double nextLon = area.getLongitude((i + 1) % count);
            final double nextLat = area.getLatitude((i + 1) % count);
            final int steps = (int) Math.ceil(Math.abs(nextLon - lon) / 90.0);
            for (int k = 1; k < steps; k++) {
                final double f = (double) k / steps;
                locations.add(LatLon.fromDegrees(lat + f * (nextLat - lat), lon + f * (nextLon - lon)));
            }
        }
        return locations;
    }

    private Point.Double toLonLat(Point screenPoint) {
        final Position position = this.wwd.getView().computePositionFromScreenPoint(screenPoint.getX(), screenPoint.getY());
        if (position == null) {
            return null; // off the globe
        }
        return new Point.Double(position.getLongitude().degrees, position.getLatitude().degrees);
    }

    private void update() {
        this.layer.removeAllRenderables();
        if (this.polygonArea != null) {
            this.polygonShape.setLocations(displayLocations(this.polygonArea));
            this.layer.addRenderable(this.polygonShape);
        } else if (this.drawing && !this.vertices.isEmpty()) {
            final List<LatLon> locations = new ArrayList<>();
            for (Point.Double vertex : this.vertices) {
                locations.add(LatLon.fromDegrees(vertex.y, vertex.x));
            }
            if (this.cursor != null) {
                locations.add(LatLon.fromDegrees(this.cursor.y, this.cursor.x));
            }
            if (locations.size() >= 2) {
                this.outlineShape.setLocations(locations);
                this.layer.addRenderable(this.outlineShape);
            }
        }
        final LayerList layers = this.wwd.getModel().getLayers();
        if (!layers.contains(this.layer)) {
            layers.add(this.layer);
        }
        this.wwd.redraw();
    }
}
