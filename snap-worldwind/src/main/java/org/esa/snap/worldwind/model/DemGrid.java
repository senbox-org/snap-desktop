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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A DEM as a regular lat/lon grid of heights, for use as globe terrain: the first row is the northernmost, the outer
 * rows and columns lie on the edges of the bounds. Large DEMs are subsampled.
 */
public final class DemGrid {

    static final int STRIP_HEIGHT = 256;

    public final double minLat;
    public final double maxLat;
    public final double minLon;
    public final double maxLon;
    public final int width;
    public final int height;
    /**
     * Heights in metres, row by row from north to south; {@link Float#NaN} where there is no data.
     */
    public final float[] heights;

    DemGrid(double minLat, double maxLat, double minLon, double maxLon, int width, int height, float[] heights) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.width = width;
        this.height = height;
        this.heights = heights;
    }

    /**
     * Where a DEM product lies: its bounds through the centres of the outer pixels, longitudes in -180..180, and the
     * direction of its rows and columns.
     */
    private static final class Placement {
        double minLat;
        double maxLat;
        double minLon;
        double maxLon;
        boolean northUp;
        boolean westFirst;
    }

    /**
     * The bounds of a DEM product, as {minLat, maxLat, minLon, maxLon} through the centres of the outer pixels, with
     * longitudes in -180..180. Returns null if the product is not on a lat/lon grid or crosses the antimeridian.
     */
    public static double[] bounds(final Product product) {
        final Placement placement = placement(product);
        return placement != null ? new double[]{placement.minLat, placement.maxLat, placement.minLon, placement.maxLon} : null;
    }

    private static Placement placement(final Product product) {
        if (product.getNumBands() == 0) {
            return null;
        }
        final Band band = product.getBandAt(0);
        final GeoCoding geoCoding = band.getGeoCoding() != null ? band.getGeoCoding() : product.getSceneGeoCoding();
        if (!isLatLonGrid(geoCoding)) {
            return null;
        }
        final int rasterWidth = band.getRasterWidth();
        final int rasterHeight = band.getRasterHeight();
        if (rasterWidth < 2 || rasterHeight < 2) {
            return null;
        }
        final GeoPos first = geoCoding.getGeoPos(new PixelPos(0.5, 0.5), null);
        final GeoPos second = geoCoding.getGeoPos(new PixelPos(1.5, 0.5), null);
        final GeoPos last = geoCoding.getGeoPos(new PixelPos(rasterWidth - 0.5, rasterHeight - 0.5), null);
        if (first == null || second == null || last == null || !first.isValid() || !second.isValid() || !last.isValid()) {
            return null;
        }
        final Placement placement = new Placement();
        placement.northUp = first.lat > last.lat;
        // from neighbouring columns: comparing the outer ones fails for longitudes beyond 180
        placement.westFirst = wrap(second.lon - first.lon) > 0;
        final double west = placement.westFirst ? first.lon : last.lon;
        final double east = placement.westFirst ? last.lon : first.lon;
        // longitudes as in 0..360 are moved to -180..180, both by the same amount
        final double shift = wrap(west) - west;
        placement.minLon = west + shift;
        placement.maxLon = east + shift;
        if (placement.maxLon < placement.minLon || placement.maxLon > 180.0 + 1e-9) {
            return null; // across the antimeridian: WorldWind sectors do not wrap
        }
        placement.minLat = Math.min(first.lat, last.lat);
        placement.maxLat = Math.max(first.lat, last.lat);
        return placement;
    }

    /**
     * A longitude difference or longitude in -180..180.
     */
    private static double wrap(final double degrees) {
        return ((degrees + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
    }

    /**
     * Reads the first band of a DEM product in geographic coordinates (e.g. a Copernicus or SRTM tile), keeping at most
     * {@code maxSize} samples per row and column. Returns null if the product is not on a lat/lon grid, crosses the
     * antimeridian or holds no data.
     */
    public static DemGrid fromProduct(final Product product, final int maxSize) throws IOException {
        final Placement placement = placement(product);
        if (placement == null) {
            return null;
        }
        final Band band = product.getBandAt(0);
        final int rasterWidth = band.getRasterWidth();
        final int rasterHeight = band.getRasterHeight();
        final int[] columns = samples(rasterWidth, maxSize);
        final int[] rows = samples(rasterHeight, maxSize);
        final boolean northUp = placement.northUp;
        final boolean westFirst = placement.westFirst;

        // the heights are read as floats: compare with the no-data value as a float
        final float noData = band.isNoDataValueUsed() ? (float) band.getGeophysicalNoDataValue() : Float.NaN;
        final float[] heights = new float[columns.length * rows.length];
        // rows are read in strips: reading them one by one decodes each image tile many times
        final int stripHeight = Math.min(STRIP_HEIGHT, rasterHeight);
        final float[] strip = new float[rasterWidth * stripHeight];
        int stripStart = -1;
        boolean anyData = false;
        for (int r = 0; r < rows.length; r++) {
            // output row 0 is the north
            final int sourceRow = northUp ? rows[r] : rows[rows.length - 1 - r];
            if (stripStart < 0 || sourceRow < stripStart || sourceRow >= stripStart + stripHeight) {
                // the strip extends in the direction of the following rows
                stripStart = northUp ? Math.min(sourceRow, rasterHeight - stripHeight)
                                     : Math.max(0, sourceRow - stripHeight + 1);
                band.readPixels(0, stripStart, rasterWidth, stripHeight, strip);
            }
            final int offset = (sourceRow - stripStart) * rasterWidth;
            for (int c = 0; c < columns.length; c++) {
                // output column 0 is the west
                final int sourceColumn = westFirst ? columns[c] : columns[columns.length - 1 - c];
                final float value = strip[offset + sourceColumn];
                final boolean missing = Float.isNaN(value) || value == noData;
                heights[r * columns.length + c] = missing ? Float.NaN : value;
                anyData |= !missing;
            }
        }
        if (!anyData) {
            return null;
        }
        return new DemGrid(placement.minLat, placement.maxLat, placement.minLon, placement.maxLon,
                           columns.length, rows.length, heights);
    }

    /**
     * True for a map geo-coding in geographic coordinates whose rows and columns follow the parallels and meridians.
     * Tie-point and pixel geo-codings also report a geographic map CRS, but their pixels are not on a regular grid.
     */
    static boolean isLatLonGrid(final GeoCoding geoCoding) {
        if (!(geoCoding instanceof CrsGeoCoding) || !(geoCoding.getMapCRS() instanceof GeographicCRS)) {
            return false;
        }
        final MathTransform imageToMap = geoCoding.getImageToMapTransform();
        if (!(imageToMap instanceof AffineTransform)) {
            return false;
        }
        final AffineTransform affine = (AffineTransform) imageToMap;
        return affine.getShearX() == 0 && affine.getShearY() == 0 && affine.getScaleX() != 0 && affine.getScaleY() != 0;
    }

    /**
     * Indices of at most {@code maxSize} evenly spaced samples of {@code size}, always including the first and last.
     */
    static int[] samples(final int size, final int maxSize) {
        final int count = Math.max(2, Math.min(size, maxSize));
        final int[] indices = new int[count];
        for (int i = 0; i < count; i++) {
            indices[i] = (int) Math.round(i * (size - 1) / (double) (count - 1));
        }
        return indices;
    }

    /**
     * GeoTIFF files in a folder and its sub-folders, sorted by path, at most {@code max}.
     */
    public static List<File> findDemFiles(final File folder, final int max) {
        final List<File> files = new ArrayList<>();
        collect(folder, files, new HashSet<>());
        Collections.sort(files);
        return files.size() > max ? new ArrayList<>(files.subList(0, max)) : files;
    }

    private static void collect(final File folder, final List<File> files, final Set<File> visited) {
        // links and junctions may lead back to a folder already searched
        try {
            if (!visited.add(folder.getCanonicalFile())) {
                return;
            }
        } catch (IOException e) {
            return;
        }
        final File[] children = folder.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, files, visited);
            } else {
                final String name = child.getName().toLowerCase(Locale.ENGLISH);
                if (name.endsWith(".tif") || name.endsWith(".tiff")) {
                    files.add(child);
                }
            }
        }
    }
}
