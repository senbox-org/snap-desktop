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
package org.esa.snap.worldwind.layers;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.LevelSet;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.worldwind.model.GeoBounds;
import org.esa.snap.worldwind.model.ProductTileRenderer;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Tiled image layer whose tiles are rendered from a product on demand, instead of being read from a server
 * or the file cache. Tiles follow WorldWind's global tiling, so that they line up with the other layers, and
 * are kept in WorldWind's texture tile memory cache under a cache name that is unique to this layer.
 */
class ProductImageTiledLayer extends TiledImageLayer {

    static final int TILE_SIZE = 512;
    static final double LEVEL_ZERO_DELTA_DEG = 36;
    static final int MAX_LEVELS = 22;

    private static final AtomicInteger instanceCount = new AtomicInteger();

    private final ProductTileRenderer renderer;
    private volatile boolean disposed;

    ProductImageTiledLayer(final ProductTileRenderer renderer, final GeoBounds bounds, final double resolutionDeg) {
        super(createLevelSet(bounds, resolutionDeg));
        this.renderer = renderer;
        setUseTransparentTextures(true);
        setUseMipMaps(true);
        setPickEnabled(false);
        // level 0 tiles are rendered like any other, on a background thread
        setForceLevelZeroLoads(false);
        setRetainLevelZeroTiles(false);
    }

    static LevelSet createLevelSet(final GeoBounds bounds, final double resolutionDeg) {
        final AVList params = new AVListImpl();
        params.setValue(AVKey.TILE_WIDTH, TILE_SIZE);
        params.setValue(AVKey.TILE_HEIGHT, TILE_SIZE);
        // unique, so that tiles of another image (or of new colours) are never taken from the memory cache
        params.setValue(AVKey.DATA_CACHE_NAME, "SNAP/ProductImage/" + instanceCount.incrementAndGet());
        params.setValue(AVKey.DATASET_NAME, "productImage");
        params.setValue(AVKey.FORMAT_SUFFIX, ".png");
        params.setValue(AVKey.NUM_LEVELS, GeoBounds.numLevels(resolutionDeg, LEVEL_ZERO_DELTA_DEG, TILE_SIZE, MAX_LEVELS));
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA,
                        new LatLon(Angle.fromDegrees(LEVEL_ZERO_DELTA_DEG), Angle.fromDegrees(LEVEL_ZERO_DELTA_DEG)));
        params.setValue(AVKey.TILE_ORIGIN, new LatLon(Angle.NEG90, Angle.NEG180));
        params.setValue(AVKey.SECTOR, Sector.fromDegrees(bounds.minLat, bounds.maxLat, bounds.minLon, bounds.maxLon));
        // a tile without data stays without data: never request it again
        params.setValue(AVKey.MAX_ABSENT_TILE_ATTEMPTS, 1);
        params.setValue(AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL, Integer.MAX_VALUE);
        return new LevelSet(params);
    }

    @Override
    public void dispose() {
        // pending render tasks see the flag and drop their tile
        disposed = true;
        super.dispose();
    }

    boolean isDisposed() {
        return disposed;
    }

    @Override
    protected void requestTexture(final DrawContext dc, final TextureTile tile) {
        if (disposed) {
            return;
        }
        final Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
        final Vec4 referencePoint = getReferencePoint(dc);
        if (referencePoint != null) {
            tile.setPriority(centroid.distanceTo3(referencePoint));
        }
        getRequestQ().add(new RenderTask(tile, this));
    }

    @Override
    protected void forceTextureLoad(final TextureTile tile) {
        // tiles are only rendered on background threads
    }

    /**
     * Renders a tile and makes it available to the layer.
     *
     * @return true if the tile has data
     */
    boolean loadTile(final TextureTile tile) {
        final Sector sector = tile.getSector();
        final BufferedImage image = renderer.render(sector.getMinLatitude().degrees, sector.getMaxLatitude().degrees,
                                                    sector.getMinLongitude().degrees, sector.getMaxLongitude().degrees,
                                                    tile.getWidth(), tile.getHeight());
        if (image == null) {
            getLevels().markResourceAbsent(tile);
            return false;
        }
        final TextureData textureData = AWTTextureIO.newTextureData(Configuration.getMaxCompatibleGLProfile(),
                                                                     image, isUseMipMaps());
        tile.setTextureData(textureData);
        TextureTile.getMemoryCache().add(tile.getTileKey(), tile);
        return true;
    }

    private static class RenderTask implements Runnable, Comparable<RenderTask> {

        private final TextureTile tile;
        private final ProductImageTiledLayer layer;

        RenderTask(final TextureTile tile, final ProductImageTiledLayer layer) {
            this.tile = tile;
            this.layer = layer;
        }

        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted() || layer.isDisposed()) {
                return;
            }
            try {
                if (layer.loadTile(tile) && !layer.isDisposed()) {
                    layer.firePropertyChange(AVKey.LAYER, null, layer);
                }
            } catch (Exception e) {
                // e.g. the product was closed while the tile was rendered
                if (!layer.isDisposed()) {
                    SystemUtils.LOG.log(Level.FINE, "WorldWind: unable to render tile " + tile, e);
                    layer.getLevels().markResourceAbsent(tile);
                }
            }
        }

        @Override
        public int compareTo(final RenderTask that) {
            return Double.compare(this.tile.getPriority(), that.tile.getPriority());
        }

        @Override
        public boolean equals(final Object o) {
            // requests for the same tile are merged by WorldWind's task service
            return o instanceof RenderTask && tile.equals(((RenderTask) o).tile);
        }

        @Override
        public int hashCode() {
            return tile.hashCode();
        }
    }
}
