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

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.render.DrawContext;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.worldwind.model.GeoBounds;
import org.esa.snap.worldwind.model.ProductTileRenderer;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * Shows product images at full resolution, placed through the products' geo-codings and coloured like SNAP's
 * image views (see docs/WorldWind-Review.md, section 6.1).
 * <p>
 * Each product shows the image of its last selected image view: the view's rasters with its colour settings.
 * Colour changes in the view refresh the image. The tiles are rendered on demand by a
 * {@link ProductImageTiledLayer} per product. Products are added and removed, and views selected, on the EDT.
 */
public class ProductImageLayer extends BaseLayer implements WWLayer {

    // colour manipulation fires many changes while a slider is dragged
    static final int IMAGE_INFO_DELAY_MS = 300;

    private static final AtomicInteger threadCount = new AtomicInteger();
    private static final ExecutorService imageExecutor = Executors.newSingleThreadExecutor(r -> {
        final Thread t = new Thread(r, "WorldWind-ProductImage-" + threadCount.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    static class ProductImage {
        final RasterDataNode[] rasters;
        final ImageInfo imageInfo;
        final WeakReference<ProductSceneView> view;
        ProductImageTiledLayer tiledLayer;
        PropertyChangeListener imageInfoListener;
        Timer imageInfoTimer;

        ProductImage(final RasterDataNode[] rasters, final ImageInfo imageInfo, final ProductSceneView view) {
            this.rasters = rasters;
            this.imageInfo = imageInfo;
            this.view = new WeakReference<>(view);
        }
    }

    private final Set<Product> products = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Product, ProductImage> images = new IdentityHashMap<>();
    // the latest requested image of each product; an image built for an older request is discarded
    private final Map<Product, Integer> generations = new IdentityHashMap<>();
    private final Map<Product, ProductNodeListenerAdapter> nodeListeners = new IdentityHashMap<>();
    private final AtomicInteger pendingTasks = new AtomicInteger();
    private BiConsumer<Product, Boolean> imageShownListener;
    private WorldWindowGLCanvas wwd;

    public ProductImageLayer() {
        setName("Product Images");
        setPickEnabled(false);
    }

    /**
     * @param listener called on the EDT with true when a product starts showing a full-resolution image, and with
     *                 false when it stops
     */
    public void setImageShownListener(final BiConsumer<Product, Boolean> listener) {
        this.imageShownListener = listener;
    }

    @Override
    public Suitability getSuitability(final Product product) {
        return Suitability.SUITABLE;
    }

    @Override
    public void addProduct(final Product product, final WorldWindowGLCanvas wwd) {
        if (wwd != null) {
            this.wwd = wwd;
        }
        if (!products.add(product)) {
            return;
        }
        final ProductNodeListenerAdapter listener = new ProductNodeListenerAdapter() {
            @Override
            public void nodeRemoved(final ProductNodeEvent event) {
                // the image cannot be shown without its rasters
                final ProductImage image = images.get(product);
                if (image != null && Arrays.asList(image.rasters).contains(event.getSourceNode())) {
                    runOnEDT(() -> removeImage(product));
                }
            }
        };
        product.addProductNodeListener(listener);
        nodeListeners.put(product, listener);
    }

    @Override
    public void removeProduct(final Product product) {
        if (!products.remove(product)) {
            return;
        }
        final ProductNodeListenerAdapter listener = nodeListeners.remove(product);
        if (listener != null) {
            product.removeProductNodeListener(listener);
        }
        generations.remove(product);
        removeImage(product);
        if (selectedProduct == product) {
            selectedProduct = null;
            selectedRaster = null;
        }
    }

    @Override
    public void setSelectedSceneView(final ProductSceneView view) {
        if (view == null || view.getRaster() == null) {
            return;
        }
        final Product product = view.getProduct();
        if (product == null || !products.contains(product)) {
            return;
        }
        final ProductImage current = images.get(product);
        if (current != null && current.view.get() == view &&
                Arrays.equals(current.rasters, view.getRasters())) {
            return;
        }
        showImage(product, view);
    }

    /**
     * Builds the image of a view in the background and installs it on the EDT.
     */
    private void showImage(final Product product, final ProductSceneView view) {
        final RasterDataNode[] rasters = view.getRasters().clone();
        final ImageInfo imageInfo = view.getImageInfo() != null ? view.getImageInfo().clone() : null;
        final int generation = generations.merge(product, 1, Integer::sum);

        pendingTasks.incrementAndGet();
        imageExecutor.execute(() -> {
            ProductImageTiledLayer tiledLayer = null;
            try {
                tiledLayer = createTiledLayer(rasters, imageInfo);
            } catch (Throwable e) {
                SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to show the image of " + product.getName(), e);
            }
            final ProductImageTiledLayer result = tiledLayer;
            SwingUtilities.invokeLater(() -> {
                try {
                    final Integer latest = generations.get(product);
                    if (result == null || latest == null || latest != generation || !products.contains(product)) {
                        // the product was removed, or another view selected, in the meantime
                        if (result != null) {
                            result.dispose();
                        }
                        return;
                    }
                    installImage(product, new ProductImage(rasters, imageInfo, view), result);
                } finally {
                    pendingTasks.decrementAndGet();
                }
            });
        });
    }

    /**
     * Prepares the colour settings (which may compute statistics) and the pyramid of a product image.
     *
     * @return the layer, or null if the rasters have no geo-coding that can compute pixel positions
     */
    static ProductImageTiledLayer createTiledLayer(final RasterDataNode[] rasters, final ImageInfo imageInfo) {
        final RasterDataNode raster = rasters[0];
        if (raster.getGeoCoding() == null || !raster.getGeoCoding().canGetPixelPos()) {
            SystemUtils.LOG.info("WorldWind: no full-resolution image of " + raster.getDisplayName() +
                                         ": its geo-coding cannot compute pixel positions");
            return null;
        }
        final GeoBounds bounds = GeoBounds.of(raster);
        if (bounds == null) {
            SystemUtils.LOG.info("WorldWind: no full-resolution image of " + raster.getDisplayName() +
                                         ": no valid geo-positions");
            return null;
        }
        final double resolution = GeoBounds.resolutionDeg(raster.getGeoCoding(), raster.getRasterWidth(),
                                                          raster.getRasterHeight());
        final ProductTileRenderer renderer = new ProductTileRenderer(rasters, imageInfo);
        return new ProductImageTiledLayer(renderer, bounds, resolution);
    }

    private void installImage(final Product product, final ProductImage image, final ProductImageTiledLayer tiledLayer) {
        final boolean wasShown = images.containsKey(product);
        disposeImage(images.remove(product));

        image.tiledLayer = tiledLayer;
        tiledLayer.setName(getName() + " " + product.getName());
        tiledLayer.setOpacity(getOpacity());
        tiledLayer.setEnabled(true);
        // tiles are rendered on WorldWind's task threads; redraw when one arrives
        tiledLayer.addPropertyChangeListener(evt -> firePropertyChange(AVKey.LAYER, null, this));
        images.put(product, image);

        final ProductSceneView view = image.view.get();
        if (view != null) {
            image.imageInfoTimer = new Timer(IMAGE_INFO_DELAY_MS, e -> {
                final ProductSceneView v = image.view.get();
                if (images.get(product) == image && v != null && products.contains(product)) {
                    showImage(product, v);
                }
            });
            image.imageInfoTimer.setRepeats(false);
            image.imageInfoListener = evt -> image.imageInfoTimer.restart();
            view.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_IMAGE_INFO, image.imageInfoListener);
        }

        if (!wasShown && imageShownListener != null) {
            imageShownListener.accept(product, true);
        }
        redraw();
    }

    private void removeImage(final Product product) {
        final ProductImage image = images.remove(product);
        if (image == null) {
            return;
        }
        disposeImage(image);
        if (imageShownListener != null) {
            imageShownListener.accept(product, false);
        }
        redraw();
    }

    private static void disposeImage(final ProductImage image) {
        if (image == null) {
            return;
        }
        if (image.imageInfoTimer != null) {
            image.imageInfoTimer.stop();
        }
        final ProductSceneView view = image.view.get();
        if (view != null && image.imageInfoListener != null) {
            view.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_IMAGE_INFO, image.imageInfoListener);
        }
        if (image.tiledLayer != null) {
            image.tiledLayer.dispose();
        }
    }

    private void redraw() {
        if (wwd != null) {
            wwd.redraw();
        }
    }

    private static void runOnEDT(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    @Override
    public void setOpacity(final double opacity) {
        super.setOpacity(opacity);
        for (ProductImage image : images.values()) {
            if (image.tiledLayer != null) {
                image.tiledLayer.setOpacity(opacity);
            }
        }
    }

    @Override
    protected void doRender(final DrawContext dc) {
        super.doRender(dc);
        for (ProductImage image : new ArrayList<>(images.values())) {
            if (image.tiledLayer != null) {
                image.tiledLayer.render(dc);
            }
        }
    }

    @Override
    public JPanel getControlPanel(final WorldWindowGLCanvas wwd) {
        return null;
    }

    boolean hasImage(final Product product) {
        return images.containsKey(product);
    }

    ProductImage getImage(final Product product) {
        return images.get(product);
    }

    List<Product> getProductsWithImages() {
        return new ArrayList<>(images.keySet());
    }

    /**
     * Waits until all queued images have been installed on the EDT. For tests.
     */
    boolean awaitPendingImages(final long timeoutMillis) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("must not be called on the EDT");
        }
        final long end = System.currentTimeMillis() + timeoutMillis;
        while (pendingTasks.get() > 0) {
            if (System.currentTimeMillis() > end) {
                return false;
            }
            SwingUtilities.invokeAndWait(() -> { });
            TimeUnit.MILLISECONDS.sleep(10);
        }
        return true;
    }
}
