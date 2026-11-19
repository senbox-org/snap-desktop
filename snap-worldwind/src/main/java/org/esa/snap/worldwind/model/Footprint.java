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

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.GeoUtils;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The outline of a raster in longitude and latitude, to decide cheaply whether an area can touch the raster before
 * inverting the geo-coding pixel by pixel. Longitudes are unwrapped along the outline, so a raster crossing the
 * antimeridian has one continuous outline (tested at the area's longitude and 360° either side).
 * <p>
 * The test is conservative: it errs towards "may touch". An outline that cannot be built reliably (invalid boundary
 * positions, or an outline around a pole) gives no footprint, and the caller then tests every pixel.
 */
final class Footprint {

    // boundary points around the raster
    static final int BOUNDARY_POINTS = 400;

    private final Path2D.Double outline;
    private final double margin;

    private Footprint(final Path2D.Double outline, final double margin) {
        this.outline = outline;
        this.margin = margin;
    }

    /**
     * @return the footprint of a raster, or null if it cannot be built reliably
     */
    static Footprint of(final GeoCoding geoCoding, final int width, final int height) {
        final int step = Math.max(1, 2 * (width + height) / BOUNDARY_POINTS);
        final PixelPos[] pixels = GeoUtils.createPixelBoundary(width, height, null, step);
        if (pixels.length < 3) {
            return null;
        }
        final Path2D.Double outline = new Path2D.Double();
        double previousLon = Double.NaN;
        double previousLat = Double.NaN;
        double firstLon = Double.NaN;
        double maxEdge = 0;
        final GeoPos geoPos = new GeoPos();
        for (PixelPos pixel : pixels) {
            // keep the outline inside the raster: the boundary of createPixelBoundary lies on the outer pixel edges
            pixel.setLocation(Math.min(Math.max(pixel.x, 0.5), width - 0.5), Math.min(Math.max(pixel.y, 0.5), height - 0.5));
            final GeoPos pos = geoCoding.getGeoPos(pixel, geoPos);
            if (pos == null || !pos.isValid()) {
                return null;
            }
            double lon = pos.lon;
            if (Double.isNaN(previousLon)) {
                firstLon = lon;
                outline.moveTo(lon, pos.lat);
            } else {
                // unwrap across the antimeridian
                while (lon - previousLon > 180) {
                    lon -= 360;
                }
                while (lon - previousLon < -180) {
                    lon += 360;
                }
                outline.lineTo(lon, pos.lat);
                maxEdge = Math.max(maxEdge, Math.hypot(lon - previousLon, pos.lat - previousLat));
            }
            previousLon = lon;
            previousLat = pos.lat;
        }
        if (Math.abs(previousLon - firstLon) > 180) {
            return null; // the outline goes around a pole: not a closed area in longitude and latitude
        }
        outline.closePath();
        // the edges between boundary points are straight here but curved on the ground: allow for that
        return new Footprint(outline, 2 * maxEdge);
    }

    /**
     * True if the area may touch the raster; false only if it certainly does not.
     */
    boolean mayIntersect(final double minLat, final double maxLat, final double minLon, final double maxLon) {
        for (double shift = -360; shift <= 360; shift += 360) {
            final Rectangle2D area = new Rectangle2D.Double(minLon + shift - margin, minLat - margin,
                                                            maxLon - minLon + 2 * margin, maxLat - minLat + 2 * margin);
            if (outline.intersects(area)) {
                return true;
            }
        }
        return false;
    }
}
