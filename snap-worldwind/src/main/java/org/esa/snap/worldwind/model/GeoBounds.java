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
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.GeoUtils;

/**
 * Geographic bounding box and ground resolution of a raster, in degrees. Independent of WorldWind.
 */
public class GeoBounds {

    public final double minLat;
    public final double maxLat;
    public final double minLon;
    public final double maxLon;

    public GeoBounds(final double minLat, final double maxLat, final double minLon, final double maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }

    /**
     * Bounds of the raster's boundary. A raster crossing the antimeridian gets the full longitude range, as a
     * bounding box cannot cross it.
     *
     * @return the bounds, or null if the raster has no valid geo-positions on its boundary
     */
    public static GeoBounds of(final RasterDataNode raster) {
        if (raster.getGeoCoding() == null) {
            return null;
        }
        final int step = Math.max(1, (raster.getRasterWidth() + raster.getRasterHeight()) / 100);
        final GeoPos[] boundary = GeoUtils.createGeoBoundary(raster, null, step, true);
        return of(boundary);
    }

    static GeoBounds of(final GeoPos[] boundary) {
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        boolean crossesAntimeridian = false;
        GeoPos previous = null;
        for (GeoPos pos : boundary) {
            if (pos == null || !pos.isValid()) {
                continue;
            }
            final double lon = normalizeLon(pos.lon);
            minLat = Math.min(minLat, pos.lat);
            maxLat = Math.max(maxLat, pos.lat);
            minLon = Math.min(minLon, lon);
            maxLon = Math.max(maxLon, lon);
            if (previous != null && Math.abs(lon - normalizeLon(previous.lon)) > 180) {
                crossesAntimeridian = true;
            }
            previous = pos;
        }
        if (minLat > maxLat) {
            return null;
        }
        if (crossesAntimeridian) {
            minLon = -180;
            maxLon = 180;
        }
        return new GeoBounds(Math.max(-90, minLat), Math.min(90, maxLat), minLon, maxLon);
    }

    static double normalizeLon(double lon) {
        while (lon > 180) {
            lon -= 360;
        }
        while (lon < -180) {
            lon += 360;
        }
        return lon;
    }

    public boolean crossesAntimeridian() {
        return minLon == -180 && maxLon == 180;
    }

    /**
     * Ground resolution in degrees of latitude per pixel at the centre of the raster: the smaller of the distances
     * to the next pixel along and across, as great-circle arcs.
     *
     * @return the resolution, or NaN if it cannot be computed
     */
    public static double resolutionDeg(final GeoCoding geoCoding, final int width, final int height) {
        final double cx = width / 2.0, cy = height / 2.0;
        final GeoPos centre = geoCoding.getGeoPos(new PixelPos(cx, cy), null);
        final GeoPos right = geoCoding.getGeoPos(new PixelPos(cx + 1, cy), null);
        final GeoPos down = geoCoding.getGeoPos(new PixelPos(cx, cy + 1), null);
        if (centre == null || !centre.isValid()) {
            return Double.NaN;
        }
        double res = Double.MAX_VALUE;
        if (right != null && right.isValid()) {
            res = Math.min(res, arcDeg(centre, right));
        }
        if (down != null && down.isValid()) {
            res = Math.min(res, arcDeg(centre, down));
        }
        return res == Double.MAX_VALUE || !(res > 0) ? Double.NaN : res;
    }

    static double arcDeg(final GeoPos a, final GeoPos b) {
        final double lat1 = Math.toRadians(a.lat), lat2 = Math.toRadians(b.lat);
        final double dLat = lat2 - lat1, dLon = Math.toRadians(b.lon - a.lon);
        final double h = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return Math.toDegrees(2 * Math.asin(Math.min(1, Math.sqrt(h))));
    }

    /**
     * Number of levels of a tile pyramid whose level 0 tiles are levelZeroDeltaDeg wide and tileSize pixels, so
     * that the last level resolves the given ground resolution, plus one level of oversampling.
     */
    public static int numLevels(final double resolutionDeg, final double levelZeroDeltaDeg, final int tileSize,
                                final int maxLevels) {
        if (!(resolutionDeg > 0)) {
            return Math.min(maxLevels, 10);
        }
        final double levelZeroRes = levelZeroDeltaDeg / tileSize;
        final int last = (int) Math.ceil(Math.log(levelZeroRes / resolutionDeg) / Math.log(2));
        return Math.max(1, Math.min(maxLevels, last + 2));
    }

    @Override
    public String toString() {
        return "GeoBounds[" + minLat + ".." + maxLat + ", " + minLon + ".." + maxLon + "]";
    }
}
