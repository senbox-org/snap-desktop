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
import gov.nasa.worldwind.util.TileKey;
import gov.nasa.worldwind.util.LevelSet;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.worldwind.model.GeoBounds;
import org.esa.snap.worldwind.model.ProductTileRenderer;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

    /**
     * Tiles waiting for the render threads at most; further requests wait for the next frame, which asks again for
     * the tiles still needed.
     */
    static final int MAX_QUEUED_TILES = 64;

    private static final AtomicInteger instanceCount = new AtomicInteger();

    /**
     * Product tiles are rendered on their own threads, nearest tiles first: WorldWind's shared task service (4 threads)
     * also loads the base maps and elevations, which slow product tiles would hold up.
     */
    private static final ThreadPoolExecutor RENDER_EXECUTOR = createRenderExecutor();

    private final ProductTileRenderer renderer;
    private volatile boolean disposed;
    // tiles queued or being rendered, with the last frame that requested them: a tile requested again in the next
    // frame is not rendered twice, and a queued tile no longer requested (the view moved on) is dropped
    private final Map<TileKey, Long> pending = new ConcurrentHashMap<>();
    // frames whose requests have been sent
    private final AtomicLong frames = new AtomicLong();
    // the first render failure of the layer is logged as a warning, later ones at FINE
    private final AtomicBoolean failureLogged = new AtomicBoolean();
    // tiles put into WorldWind's texture memory cache, removed from it on disposal
    private final Set<TileKey> installedTiles = ConcurrentHashMap.newKeySet();

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

    private static ThreadPoolExecutor createRenderExecutor() {
        final int threads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
        final AtomicInteger threadCount = new AtomicInteger();
        return new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS, new PriorityBlockingQueue<>(), runnable -> {
            final Thread thread = new Thread(runnable, "WorldWind product tiles " + threadCount.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
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
        // a tile without data stays without data; WorldWind's absent list still forgets it after a while
        params.setValue(AVKey.MAX_ABSENT_TILE_ATTEMPTS, 1);
        params.setValue(AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL, Integer.MAX_VALUE);
        return new LevelSet(params);
    }

    @Override
    public void dispose() {
        // pending render tasks see the flag and drop their tile
        disposed = true;
        // the tiles of a replaced image (e.g. after every colour change) would otherwise occupy WorldWind's shared
        // texture cache until evicted, pushing out the tiles still shown
        for (TileKey key : installedTiles) {
            TextureTile.getMemoryCache().remove(key);
        }
        installedTiles.clear();
        super.dispose();
    }

    /**
     * A hidden layer requests no tiles, so tiles queued before it was hidden would never become stale: they are
     * dropped here instead of rendering in the shared queue.
     */
    @Override
    public void setEnabled(final boolean enabled) {
        if (!enabled) {
            frames.addAndGet(2);
        }
        super.setEnabled(enabled);
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
        final double priority = referencePoint != null ? centroid.distanceTo3(referencePoint) : 0.0;
        getRequestQ().add(createRenderTask(tile, priority));
    }

    @Override
    protected void forceTextureLoad(final TextureTile tile) {
        // tiles are only rendered on background threads
    }

    @Override
    protected void sendRequests() {
        for (Runnable task = getRequestQ().poll(); task != null; task = getRequestQ().poll()) {
            submit(task);
        }
        frames.incrementAndGet();
    }

    /**
     * Queues a tile requested in the current frame, or notes that a queued tile is still needed. Beyond
     * {@link #MAX_QUEUED_TILES} the request is dropped; it is made again in the next frame if still needed.
     */
    void submit(final Runnable task) {
        if (!(task instanceof RenderTask)) {
            return;
        }
        final TileKey key = ((RenderTask) task).tile.getTileKey();
        final long frame = frames.get();
        if (pending.replace(key, frame) != null) {
            return; // queued or being rendered
        }
        if (RENDER_EXECUTOR.getQueue().size() < MAX_QUEUED_TILES && pending.putIfAbsent(key, frame) == null) {
            RENDER_EXECUTOR.execute(task);
        }
    }

    /**
     * True if a queued tile was not requested by the last frame drawn: the view has moved on and does not need it.
     */
    boolean isStale(final TextureTile tile) {
        final Long requested = pending.get(tile.getTileKey());
        return requested != null && requested < frames.get() - 1;
    }

    /**
     * Counts a frame as drawn, as {@link #sendRequests()} does (for tests).
     */
    void frameDrawn() {
        frames.incrementAndGet();
    }

    /**
     * Renders a tile and makes it available to the layer.
     *
     * @return true if the tile has data
     */
    boolean loadTile(final TextureTile tile) {
        final BufferedImage image = renderTile(tile);
        if (image == null) {
            getLevels().markResourceAbsent(tile);
            return false;
        }
        if (disposed) {
            return false;
        }
        installTexture(tile, image);
        installedTiles.add(tile.getTileKey());
        if (disposed) {
            // disposed while the texture was installed
            TextureTile.getMemoryCache().remove(tile.getTileKey());
            return false;
        }
        return true;
    }

    boolean hasInstalledTile(final TileKey key) {
        return installedTiles.contains(key);
    }

    BufferedImage renderTile(final TextureTile tile) {
        final Sector sector = tile.getSector();
        return renderer.render(sector.getMinLatitude().degrees, sector.getMaxLatitude().degrees,
                               sector.getMinLongitude().degrees, sector.getMaxLongitude().degrees,
                               tile.getWidth(), tile.getHeight());
    }

    /**
     * Turns a rendered tile into texture data for WorldWind (needs the OpenGL profile; replaced in tests).
     */
    void installTexture(final TextureTile tile, final BufferedImage image) {
        final TextureData textureData = AWTTextureIO.newTextureData(Configuration.getMaxCompatibleGLProfile(),
                                                                     image, isUseMipMaps());
        tile.setTextureData(textureData);
        TextureTile.getMemoryCache().add(tile.getTileKey(), tile);
    }

    void renderFailed(final TextureTile tile, final Throwable e) {
        getLevels().markResourceAbsent(tile);
        // a systematic failure would otherwise leave the image blank without trace in the log
        final Level level = failureLogged.compareAndSet(false, true) ? Level.WARNING : Level.FINE;
        SystemUtils.LOG.log(level, "WorldWind: unable to render a tile of the product image " + getName() +
                                   (level == Level.WARNING ? " (further failures of this image are logged at FINE)" : ""), e);
    }

    RenderTask createRenderTask(final TextureTile tile) {
        return createRenderTask(tile, 0.0);
    }

    RenderTask createRenderTask(final TextureTile tile, final double priority) {
        return new RenderTask(tile, priority, this);
    }

    static class RenderTask implements Runnable, Comparable<RenderTask> {

        private final TextureTile tile;
        // the distance from the eye when requested; fixed, as the executor's queue orders by it
        private final double priority;
        private final ProductImageTiledLayer layer;

        RenderTask(final TextureTile tile, final double priority, final ProductImageTiledLayer layer) {
            this.tile = tile;
            this.priority = priority;
            this.layer = layer;
        }

        @Override
        public void run() {
            try {
                if (Thread.currentThread().isInterrupted() || layer.isDisposed() || layer.isStale(tile)) {
                    return;
                }
                if (layer.loadTile(tile) && !layer.isDisposed()) {
                    layer.firePropertyChange(AVKey.LAYER, null, layer);
                }
            } catch (Throwable e) {
                // also errors such as OutOfMemoryError: the tile is marked absent instead of being requested again in
                // every frame. A product closed while its tile was rendered is not an error.
                if (!layer.isDisposed()) {
                    layer.renderFailed(tile, e);
                }
            } finally {
                layer.pending.remove(tile.getTileKey());
            }
        }

        @Override
        public int compareTo(final RenderTask that) {
            return Double.compare(this.priority, that.priority);
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
