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
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Conversions between image view pixels, raster pixels and geo-positions, for the synchronisation of the globe
 * with SNAP's image views. Independent of WorldWind.
 */
public class ImageViewGeoUtils {

    private ImageViewGeoUtils() {
    }

    /**
     * Raster pixel (level 0) of the centre of a pixel of an image pyramid level, as SNAP's image cursor
     * synchronisation computes it.
     *
     * @param levelImageToModel the image-to-model transform of the level
     * @param modelToImage      the model-to-image transform of level 0
     */
    public static PixelPos levelToRasterPixel(final AffineTransform levelImageToModel, final AffineTransform modelToImage,
                                              final int levelX, final int levelY) {
        final Point2D model = levelImageToModel.transform(new Point2D.Double(levelX + 0.5, levelY + 0.5), null);
        final Point2D image = modelToImage.transform(model, null);
        return new PixelPos(image.getX(), image.getY());
    }

    /**
     * @return the normalised geo-position of a raster pixel, or null if it has none
     */
    public static GeoPos toGeoPos(final GeoCoding geoCoding, final PixelPos pixelPos) {
        if (geoCoding == null || pixelPos == null || !pixelPos.isValid() || !geoCoding.canGetGeoPos()) {
            return null;
        }
        final GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
        if (geoPos == null || !geoPos.isValid()) {
            return null;
        }
        geoPos.normalize();
        return geoPos;
    }

    /**
     * @return the raster pixel of a geo-position, or null if the position is outside the raster
     */
    public static PixelPos toRasterPixel(final RasterDataNode raster, final GeoPos geoPos) {
        return toPixel(raster.getGeoCoding(), raster.getRasterWidth(), raster.getRasterHeight(), geoPos);
    }

    /**
     * @return the pixel of a geo-position in a raster of the given size, or null if the position is outside it
     */
    public static PixelPos toPixel(final GeoCoding geoCoding, final int width, final int height, final GeoPos geoPos) {
        if (geoCoding == null || geoPos == null || !geoPos.isValid() || !geoCoding.canGetPixelPos()) {
            return null;
        }
        final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
        if (pixelPos == null || !pixelPos.isValid() || Double.isNaN(pixelPos.x) || Double.isNaN(pixelPos.y)) {
            return null;
        }
        if (pixelPos.x < 0 || pixelPos.y < 0 || pixelPos.x >= width || pixelPos.y >= height) {
            return null;
        }
        // a geo-coding may return a pixel for a position it does not cover (e.g. extrapolated tie-points):
        // accept it only if it maps back close to the position
        final GeoPos back = geoCoding.getGeoPos(pixelPos, null);
        if (back == null || !back.isValid() || !isClose(back, geoPos)) {
            return null;
        }
        return pixelPos;
    }

    private static boolean isClose(final GeoPos a, final GeoPos b) {
        double dLon = Math.abs(a.lon - b.lon) % 360;
        if (dLon > 180) {
            dLon = 360 - dLon;
        }
        return Math.abs(a.lat - b.lat) < 1 && dLon < 1;
    }

    /**
     * @return the geo-position of a placemark: its own, or the one of its pixel position, or null
     */
    public static GeoPos placemarkGeoPos(final Placemark placemark, final GeoCoding geoCoding) {
        final GeoPos own = placemark.getGeoPos();
        if (own != null && own.isValid()) {
            final GeoPos geoPos = new GeoPos(own);
            geoPos.normalize();
            return geoPos;
        }
        return toGeoPos(geoCoding, placemark.getPixelPos());
    }
}
