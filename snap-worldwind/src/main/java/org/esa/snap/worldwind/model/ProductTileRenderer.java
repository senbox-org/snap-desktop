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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.multilevel.MultiLevelSource;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ColoredBandImageMultiLevelSource;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Renders geographic tiles of a product image: the tile's lat/lon grid is mapped to raster pixels through the
 * raster's geo-coding, and the pixels are taken from the coloured image pyramid that SNAP's image views use.
 * <p>
 * Independent of WorldWind and thread-safe; tiles may be rendered concurrently.
 */
public class ProductTileRenderer {

    // the geo-coding is inverted on a grid of nodes this many tile pixels apart, and interpolated in between
    static final int GRID_STEP = 16;

    private final GeoCoding geoCoding;
    private final int rasterWidth;
    private final int rasterHeight;
    private final MultiLevelSource imageSource;

    /**
     * @param rasters   the rasters of an image view: one, or three for RGB
     * @param imageInfo the colour settings of the view; a copy is used
     */
    public ProductTileRenderer(final RasterDataNode[] rasters, final ImageInfo imageInfo) {
        this(rasters[0].getGeoCoding(), rasters[0].getRasterWidth(), rasters[0].getRasterHeight(),
             createImageSource(rasters, imageInfo));
    }

    ProductTileRenderer(final GeoCoding geoCoding, final int rasterWidth, final int rasterHeight,
                        final MultiLevelSource imageSource) {
        if (geoCoding == null || !geoCoding.canGetPixelPos()) {
            throw new IllegalArgumentException("The raster has no geo-coding that can compute pixel positions");
        }
        this.geoCoding = geoCoding;
        this.rasterWidth = rasterWidth;
        this.rasterHeight = rasterHeight;
        this.imageSource = imageSource;
    }

    private static MultiLevelSource createImageSource(final RasterDataNode[] rasters, final ImageInfo imageInfo) {
        final ColoredBandImageMultiLevelSource source = ColoredBandImageMultiLevelSource.create(rasters, ProgressMonitor.NULL);
        if (imageInfo != null) {
            source.setImageInfo(imageInfo.clone());
        }
        return source;
    }

    public GeoCoding getGeoCoding() {
        return geoCoding;
    }

    public int getRasterWidth() {
        return rasterWidth;
    }

    public int getRasterHeight() {
        return rasterHeight;
    }

    public int getLevelCount() {
        return imageSource.getModel().getLevelCount();
    }

    /**
     * Renders a tile. Tile pixel (0, 0) is the north-west corner; pixel centres are sampled.
     *
     * @return the tile, transparent where the product has no data, or null if the tile does not touch the product
     */
    public BufferedImage render(final double minLat, final double maxLat, final double minLon, final double maxLon,
                                final int width, final int height) {
        final double dLat = (maxLat - minLat) / height;
        final double dLon = (maxLon - minLon) / width;

        // inverse geo-coding on the grid nodes
        final int[] nodeX = nodePositions(width);
        final int[] nodeY = nodePositions(height);
        final double[][] nodePx = new double[nodeY.length][nodeX.length];
        final double[][] nodePy = new double[nodeY.length][nodeX.length];
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos();
        boolean anyInside = false;
        for (int r = 0; r < nodeY.length; r++) {
            for (int c = 0; c < nodeX.length; c++) {
                geoPos.setLocation(maxLat - (nodeY[r] + 0.5) * dLat, minLon + (nodeX[c] + 0.5) * dLon);
                final PixelPos pp = toPixel(geoPos, pixelPos);
                nodePx[r][c] = pp != null ? pp.x : Double.NaN;
                nodePy[r][c] = pp != null ? pp.y : Double.NaN;
                anyInside |= pp != null && isInside(pp.x, pp.y);
            }
        }

        // pixel positions of all tile pixels
        final double[] px = new double[width * height];
        final double[] py = new double[width * height];
        for (int r = 0; r + 1 < nodeY.length || (r == 0 && nodeY.length == 1); r++) {
            final int r1 = Math.min(r + 1, nodeY.length - 1);
            final int y0 = nodeY[r];
            final int y1 = r1 > r ? nodeY[r1] : y0;
            for (int c = 0; c + 1 < nodeX.length || (c == 0 && nodeX.length == 1); c++) {
                final int c1 = Math.min(c + 1, nodeX.length - 1);
                final int x0 = nodeX[c];
                final int x1 = c1 > c ? nodeX[c1] : x0;
                final boolean exact = Double.isNaN(nodePx[r][c]) || Double.isNaN(nodePx[r][c1]) ||
                        Double.isNaN(nodePx[r1][c]) || Double.isNaN(nodePx[r1][c1]);
                // the last row and column of cells include their far edge
                final int yEnd = r1 == nodeY.length - 1 ? y1 : y1 - 1;
                final int xEnd = c1 == nodeX.length - 1 ? x1 : x1 - 1;
                for (int y = y0; y <= yEnd; y++) {
                    final double fy = y1 > y0 ? (y - y0) / (double) (y1 - y0) : 0;
                    for (int x = x0; x <= xEnd; x++) {
                        final int i = y * width + x;
                        if (exact) {
                            // a node outside the geo-coding (swath edge): compute this pixel exactly
                            geoPos.setLocation(maxLat - (y + 0.5) * dLat, minLon + (x + 0.5) * dLon);
                            final PixelPos pp = toPixel(geoPos, pixelPos);
                            px[i] = pp != null ? pp.x : Double.NaN;
                            py[i] = pp != null ? pp.y : Double.NaN;
                            anyInside |= pp != null && isInside(pp.x, pp.y);
                        } else {
                            final double fx = x1 > x0 ? (x - x0) / (double) (x1 - x0) : 0;
                            px[i] = bilinear(nodePx[r][c], nodePx[r][c1], nodePx[r1][c], nodePx[r1][c1], fx, fy);
                            py[i] = bilinear(nodePy[r][c], nodePy[r][c1], nodePy[r1][c], nodePy[r1][c1], fx, fy);
                        }
                    }
                }
            }
        }
        if (!anyInside && !anyPixelInside(px, py)) {
            return null;
        }

        final int level = chooseLevel(nodePx, nodePy, nodeX, nodeY);
        return sample(px, py, width, height, level);
    }

    private PixelPos toPixel(final GeoPos geoPos, final PixelPos pixelPos) {
        final PixelPos pp = geoCoding.getPixelPos(geoPos, pixelPos);
        if (pp == null || !pp.isValid() || Double.isNaN(pp.x) || Double.isNaN(pp.y)) {
            return null;
        }
        return pp;
    }

    private boolean isInside(final double x, final double y) {
        return x >= 0 && y >= 0 && x < rasterWidth && y < rasterHeight;
    }

    private boolean anyPixelInside(final double[] px, final double[] py) {
        for (int i = 0; i < px.length; i++) {
            if (isInside(px[i], py[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tile pixel positions of the grid nodes: every GRID_STEP pixels and the last pixel.
     */
    static int[] nodePositions(final int size) {
        final int n = (size - 1 + GRID_STEP - 1) / GRID_STEP + 1;
        final int[] positions = new int[Math.max(1, n)];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = Math.min(i * GRID_STEP, size - 1);
        }
        return positions;
    }

    private static double bilinear(final double v00, final double v10, final double v01, final double v11,
                                   final double fx, final double fy) {
        final double top = v00 + (v10 - v00) * fx;
        final double bottom = v01 + (v11 - v01) * fx;
        return top + (bottom - top) * fy;
    }

    /**
     * The pyramid level whose pixels best match the tile pixels: source pixels per tile pixel estimated from the
     * spacing of the grid nodes, taking the larger of the two directions so that the tile does not alias.
     */
    int chooseLevel(final double[][] nodePx, final double[][] nodePy, final int[] nodeX, final int[] nodeY) {
        double sum = 0;
        int count = 0;
        for (int r = 0; r < nodeY.length; r++) {
            for (int c = 0; c < nodeX.length; c++) {
                if (Double.isNaN(nodePx[r][c])) {
                    continue;
                }
                if (c + 1 < nodeX.length && !Double.isNaN(nodePx[r][c + 1]) && nodeX[c + 1] > nodeX[c]) {
                    sum += Math.hypot(nodePx[r][c + 1] - nodePx[r][c], nodePy[r][c + 1] - nodePy[r][c]) /
                            (nodeX[c + 1] - nodeX[c]);
                    count++;
                }
                if (r + 1 < nodeY.length && !Double.isNaN(nodePx[r + 1][c]) && nodeY[r + 1] > nodeY[r]) {
                    sum += Math.hypot(nodePx[r + 1][c] - nodePx[r][c], nodePy[r + 1][c] - nodePy[r][c]) /
                            (nodeY[r + 1] - nodeY[r]);
                    count++;
                }
            }
        }
        if (count == 0) {
            return 0;
        }
        return levelForRatio(sum / count, getLevelCount());
    }

    /**
     * @param ratio source pixels per tile pixel at level 0
     */
    static int levelForRatio(final double ratio, final int levelCount) {
        if (!(ratio > 1)) {
            return 0;
        }
        final int level = (int) Math.floor(Math.log(ratio) / Math.log(2));
        return Math.max(0, Math.min(levelCount - 1, level));
    }

    private BufferedImage sample(final double[] px, final double[] py, final int width, final int height,
                                 final int level) {
        final double scale = imageSource.getModel().getScale(level);
        final RenderedImage levelImage = imageSource.getImage(level);
        final Rectangle levelBounds = new Rectangle(levelImage.getMinX(), levelImage.getMinY(),
                                                    levelImage.getWidth(), levelImage.getHeight());

        // the level pixels needed by the tile
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        final int[] lx = new int[px.length];
        final int[] ly = new int[px.length];
        for (int i = 0; i < px.length; i++) {
            if (!isInside(px[i], py[i])) {
                lx[i] = Integer.MIN_VALUE;
                continue;
            }
            lx[i] = Math.min(levelBounds.x + levelBounds.width - 1, (int) Math.floor(px[i] / scale));
            ly[i] = Math.min(levelBounds.y + levelBounds.height - 1, (int) Math.floor(py[i] / scale));
            minX = Math.min(minX, lx[i]);
            minY = Math.min(minY, ly[i]);
            maxX = Math.max(maxX, lx[i]);
            maxY = Math.max(maxY, ly[i]);
        }

        final BufferedImage tile = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (minX > maxX) {
            return tile;
        }
        final Rectangle region = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1).intersection(levelBounds);
        if (region.isEmpty()) {
            return tile;
        }
        final Raster data = levelImage.getData(region);
        final int numBands = data.getNumBands();
        final int[] sample = new int[numBands];
        final int[] argb = new int[width * height];
        for (int i = 0; i < argb.length; i++) {
            if (lx[i] == Integer.MIN_VALUE || !region.contains(lx[i], ly[i])) {
                continue;
            }
            data.getPixel(lx[i], ly[i], sample);
            argb[i] = toArgb(sample, numBands);
        }
        tile.setRGB(0, 0, width, height, argb, 0, width);
        return tile;
    }

    static int toArgb(final int[] sample, final int numBands) {
        final int r, g, b, a;
        if (numBands >= 3) {
            r = sample[0];
            g = sample[1];
            b = sample[2];
            a = numBands >= 4 ? sample[3] : 255;
        } else {
            r = g = b = sample[0];
            a = numBands == 2 ? sample[1] : 255;
        }
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }
}
