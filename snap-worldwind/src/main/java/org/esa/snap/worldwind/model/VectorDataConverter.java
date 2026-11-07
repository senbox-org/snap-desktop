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
package org.esa.snap.worldwind.model;

import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.SystemUtils;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Converts the vector data of a product to geographic shapes (see docs/WorldWind-Review.md, section 6.3).
 * Independent of WorldWind.
 * <p>
 * Feature geometries are in the product's model coordinates. They are converted to raster pixels with the
 * image-to-model transform of the scene geo-coding, and to geo-positions through the geo-coding, which is right
 * for map-projected and non-map-projected products alike. Edges are densified so that they follow the
 * geo-coding of curved swaths.
 */
public class VectorDataConverter {

    /** Edges longer than this many pixels are split. */
    static final double DENSIFY_PIXELS = 8;
    /** Vertices per node at most, to keep the globe responsive. */
    public static final int MAX_VERTICES = 100_000;
    /** Points per node at most: each is a placemark of its own, which costs far more to draw than a vertex. */
    public static final int MAX_POINTS = 10_000;

    public enum Kind {POLYGON, LINE, POINT}

    /**
     * Style of a shape: colours with their opacity in the alpha channel, line width in pixels.
     */
    public static class Style {
        public final Color fill;
        public final Color stroke;
        public final double strokeWidth;

        Style(final Color fill, final Color stroke, final double strokeWidth) {
            this.fill = fill;
            this.stroke = stroke;
            this.strokeWidth = strokeWidth;
        }
    }

    /**
     * A converted feature part: a polygon (outer ring first, then holes; rings closed), a line or a point.
     * Positions are {lat, lon} in degrees.
     */
    public static class Shape {
        public final Kind kind;
        public final String featureId;
        public final List<List<double[]>> parts;
        public final Style style;

        Shape(final Kind kind, final String featureId, final List<List<double[]>> parts, final Style style) {
            this.kind = kind;
            this.featureId = featureId;
            this.parts = parts;
            this.style = style;
        }
    }

    /**
     * Intermediate points per edge at most: long edges are densified to follow the geo-coding, but an edge across a
     * huge raster must not make a single feature unbounded.
     */
    static final int MAX_EDGE_STEPS = 256;

    private final GeoCoding geoCoding;
    private final AffineTransform modelToImage;
    private final int maxVertices;
    private final int maxPoints;
    private int vertexCount;
    private int pointCount;
    // the vertex limit was reached: conversion stops, also in the middle of a feature
    private boolean truncated;

    /**
     * @throws IllegalArgumentException if the product has no geo-coding
     */
    public VectorDataConverter(final Product product) {
        this(product, MAX_VERTICES);
    }

    /**
     * @param maxVertices vertices at most, e.g. what is left of a limit for all vector data of the product
     */
    public VectorDataConverter(final Product product, final int maxVertices) {
        this(product, maxVertices, MAX_POINTS);
    }

    VectorDataConverter(final Product product, final int maxVertices, final int maxPoints) {
        this.maxVertices = maxVertices;
        this.maxPoints = maxPoints;
        geoCoding = product.getSceneGeoCoding();
        if (geoCoding == null || !geoCoding.canGetGeoPos()) {
            throw new IllegalArgumentException("Product " + product.getName() + " has no geo-coding");
        }
        try {
            modelToImage = Product.findImageToModelTransform(geoCoding).createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("Product " + product.getName() + " has no usable image-to-model transform", e);
        }
    }

    /**
     * @return true for the vector data that is shown: not empty, and not the pins and GCPs, which are shown
     * separately
     */
    public static boolean isShown(final Product product, final VectorDataNode node) {
        if (node == null || node.getFeatureCollection() == null || node.getFeatureCollection().isEmpty()) {
            return false;
        }
        return node != product.getPinGroup().getVectorDataNode() && node != product.getGcpGroup().getVectorDataNode();
    }

    /**
     * Converts the features of a node. Features without a usable geometry are skipped; conversion stops at
     * {@link #MAX_VERTICES}.
     */
    public List<Shape> convert(final VectorDataNode node) {
        vertexCount = 0;
        pointCount = 0;
        truncated = false;
        final List<Shape> shapes = new ArrayList<>();
        final MathTransform featureToModel;
        try {
            featureToModel = findFeatureToModelTransform(node);
        } catch (FactoryException e) {
            // drawing the coordinates as if they were model coordinates would put them in the wrong place
            SystemUtils.LOG.log(Level.WARNING, "WorldWind: vector data " + node.getName() + " is not shown: no " +
                                               "transformation from its coordinate reference system to the product's", e);
            return shapes;
        }
        final String defaultCss = node.getDefaultStyleCss();
        final SimpleFeature[] features = node.getFeatureCollection().toArray(new SimpleFeature[0]);
        boolean transformFailureLogged = false;
        for (SimpleFeature feature : features) {
            if (truncated) {
                break;
            }
            final Object value = feature.getDefaultGeometry();
            if (!(value instanceof Geometry) || ((Geometry) value).isEmpty()) {
                continue;
            }
            Geometry geometry = (Geometry) value;
            if (featureToModel != null) {
                try {
                    geometry = JTS.transform(geometry, featureToModel);
                } catch (Exception e) {
                    SystemUtils.LOG.log(transformFailureLogged ? Level.FINE : Level.WARNING,
                                        "WorldWind: feature " + feature.getID() + " of " + node.getName() +
                                        " is not shown: it cannot be transformed to the product's coordinates", e);
                    transformFailureLogged = true;
                    continue;
                }
            }
            final Style style = parseStyle(defaultCss, styleCss(feature));
            convert(geometry, feature.getID(), style, shapes);
        }
        if (truncated) {
            SystemUtils.LOG.warning("WorldWind: vector data " + node.getName() + " has more than " + maxVertices +
                                    " vertices or " + maxPoints + " points to show; the rest is not shown");
        }
        return shapes;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    /**
     * @return the transform from the features' CRS to the model CRS, or null if they are the same
     * @throws FactoryException if the features' CRS cannot be transformed to the model CRS
     */
    private MathTransform findFeatureToModelTransform(final VectorDataNode node) throws FactoryException {
        final CoordinateReferenceSystem featureCrs = node.getFeatureType().getCoordinateReferenceSystem();
        final CoordinateReferenceSystem modelCrs = Product.findModelCRS(geoCoding);
        if (featureCrs == null || modelCrs == null || CRS.equalsIgnoreMetadata(featureCrs, modelCrs)) {
            return null;
        }
        return CRS.findMathTransform(featureCrs, modelCrs, true);
    }

    private static String styleCss(final SimpleFeature feature) {
        final Object css = feature.getFeatureType().getDescriptor(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS) != null ?
                feature.getAttribute(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS) : null;
        return css instanceof String ? (String) css : null;
    }

    private void convert(final Geometry geometry, final String id, final Style style, final List<Shape> shapes) {
        if (truncated) {
            return;
        }
        if (geometry instanceof Polygon) {
            final List<List<double[]>> rings = new ArrayList<>();
            final List<double[]> shell = toRing(((Polygon) geometry).getExteriorRing().getCoordinates());
            if (shell == null) {
                return;
            }
            rings.add(shell);
            for (int i = 0; i < ((Polygon) geometry).getNumInteriorRing(); i++) {
                final List<double[]> hole = toRing(((Polygon) geometry).getInteriorRingN(i).getCoordinates());
                if (hole != null) {
                    rings.add(hole);
                }
            }
            if (truncated) {
                // cut off at the limit: closing what was converted would show a shape the feature does not have
                return;
            }
            shapes.add(new Shape(Kind.POLYGON, id, rings, style));
        } else if (geometry instanceof LineString) {
            final List<List<double[]>> lines = toLines(geometry.getCoordinates());
            if (!lines.isEmpty()) {
                shapes.add(new Shape(Kind.LINE, id, lines, style));
            }
        } else if (geometry instanceof Point) {
            if (vertexCount >= maxVertices || pointCount >= maxPoints) {
                truncated = true;
                return;
            }
            final double[] pos = toGeo(((Point) geometry).getX(), ((Point) geometry).getY());
            if (pos != null) {
                final List<List<double[]>> parts = new ArrayList<>();
                final List<double[]> point = new ArrayList<>();
                point.add(pos);
                parts.add(point);
                vertexCount++;
                pointCount++;
                shapes.add(new Shape(Kind.POINT, id, parts, style));
            }
        } else {
            // multi-geometries and collections
            for (int i = 0; i < geometry.getNumGeometries() && !truncated; i++) {
                final Geometry part = geometry.getGeometryN(i);
                if (part != geometry) {
                    convert(part, id, style, shapes);
                }
            }
        }
    }

    /**
     * Densified ring, closed, without invalid vertices.
     *
     * @return the ring, or null if fewer than three valid vertices remain
     */
    private List<double[]> toRing(final Coordinate[] coordinates) {
        final List<double[]> ring = new ArrayList<>();
        for (List<double[]> part : toLines(coordinates)) {
            ring.addAll(part);
        }
        if (ring.size() < 3) {
            return null;
        }
        final double[] first = ring.get(0), last = ring.get(ring.size() - 1);
        if (first[0] != last[0] || first[1] != last[1]) {
            ring.add(first.clone());
        }
        return ring;
    }

    /**
     * Densified line in geo-positions, split at vertices that have no geo-position.
     */
    private List<List<double[]>> toLines(final Coordinate[] coordinates) {
        final List<List<double[]>> lines = new ArrayList<>();
        List<double[]> current = new ArrayList<>();
        Point2D previous = null;
        for (Coordinate coordinate : coordinates) {
            if (truncated) {
                break;
            }
            final Point2D pixel = modelToImage.transform(new Point2D.Double(coordinate.x, coordinate.y), null);
            if (previous != null) {
                // intermediate points of a long edge
                final double length = previous.distance(pixel);
                final int steps = Math.min(MAX_EDGE_STEPS, (int) Math.ceil(length / DENSIFY_PIXELS));
                for (int s = 1; s < steps; s++) {
                    final double f = s / (double) steps;
                    current = addVertex(previous.getX() + (pixel.getX() - previous.getX()) * f,
                                        previous.getY() + (pixel.getY() - previous.getY()) * f, current, lines);
                }
            }
            current = addVertex(pixel.getX(), pixel.getY(), current, lines);
            previous = pixel;
        }
        if (current.size() >= 2) {
            lines.add(current);
        }
        return lines;
    }

    /**
     * @return the line to continue: the current one, or a new one after an invalid vertex ended it
     */
    private List<double[]> addVertex(final double x, final double y, final List<double[]> current,
                                     final List<List<double[]>> lines) {
        if (vertexCount >= maxVertices) {
            truncated = true;
            return current;
        }
        final double[] pos = toGeoFromPixel(x, y);
        if (pos == null) {
            // the line ends here; a single vertex is not a line
            if (current.size() >= 2) {
                lines.add(current);
            }
            return new ArrayList<>();
        }
        current.add(pos);
        vertexCount++;
        return current;
    }

    private double[] toGeo(final double modelX, final double modelY) {
        final Point2D pixel = modelToImage.transform(new Point2D.Double(modelX, modelY), null);
        return toGeoFromPixel(pixel.getX(), pixel.getY());
    }

    private double[] toGeoFromPixel(final double x, final double y) {
        final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
        if (geoPos == null || !geoPos.isValid()) {
            return null;
        }
        geoPos.normalize();
        return new double[]{geoPos.lat, geoPos.lon};
    }

    /**
     * Style of a feature: its own CSS on top of the node's default CSS.
     */
    static Style parseStyle(final String defaultCss, final String featureCss) {
        final DefaultFigureStyle style = new DefaultFigureStyle();
        if (defaultCss != null && !defaultCss.isEmpty()) {
            style.fromCssString(defaultCss);
        }
        if (featureCss != null && !featureCss.isEmpty()) {
            style.fromCssString(featureCss);
        }
        final Color fill = withOpacity(style.getFillColor(), style.getFillOpacity(), new Color(255, 255, 255, 128));
        final Color stroke = withOpacity(style.getStrokeColor(), style.getStrokeOpacity(), Color.YELLOW);
        final double width = style.getStrokeWidth() > 0 ? style.getStrokeWidth() : 1.0;
        return new Style(fill, stroke, width);
    }

    private static Color withOpacity(final Color color, final double opacity, final Color fallback) {
        if (color == null) {
            return fallback;
        }
        final double o = Double.isNaN(opacity) ? 1.0 : Math.max(0, Math.min(1, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) Math.round(o * color.getAlpha()));
    }
}
