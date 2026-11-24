/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.SurfaceImage;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.logging.Level;

/**
 * Default Product Layer draws product outline
 * <p>
 * Boundary computation can read lat/lon rasters, so it runs on a background executor and the resulting renderables
 * are installed on the EDT. Products are expected to be added and removed on the EDT (see WWProductManagerListener).
 */
public class DefaultProductLayer extends BaseLayer implements WWLayer {

    // quicklooks are subsampled so that their longest side is about this many pixels
    private static final int QUICKLOOK_SIZE = 1000;

    private static final AtomicInteger threadCount = new AtomicInteger();
    private static final ExecutorService outlineExecutor = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2), r -> {
                final Thread t = new Thread(r, "WorldWind-ProductOutline-" + threadCount.incrementAndGet());
                t.setDaemon(true);
                return t;
            });

    private boolean enableSurfaceImages;
    private final ConcurrentHashMap<String, ProductOutline> outlineTable = new ConcurrentHashMap<>();
    // products whose outline is being computed; removing a product cancels its pending install
    private final Set<String> pendingProducts = ConcurrentHashMap.newKeySet();
    // the key each product was added under, so that a product renamed while open can still be removed
    private final Map<Product, String> productKeys = Collections.synchronizedMap(new IdentityHashMap<>());
    private final AtomicInteger pendingTasks = new AtomicInteger();
    private Runnable changeListener;
    private BiConsumer<Product, Boolean> productVisibilityListener;
    private DoubleConsumer opacityListener;
    // products whose quicklook is hidden, e.g. because a full-resolution image is shown instead
    private final Set<Product> hiddenQuicklooks = Collections.newSetFromMap(new IdentityHashMap<>());

    static class ProductOutline {
        Path[] productLineList = new Path[0];
        BandOutline[] bandOutlines = new BandOutline[0];
        boolean showProductBoundary = true;
        PointPlacemark label;
        SurfaceImage image;
        boolean visible = true;
    }

    static class BandOutline {
        Path[] bandLineList;
        String bandName;
        PointPlacemark bandLabel;
    }

    public DefaultProductLayer() {
        this.setName("Products");
    }

    public void setEnableSurfaceImages(final boolean enableSurfaceImages) {
        this.enableSurfaceImages = enableSurfaceImages;
    }

    /**
     * @param listener called on the EDT whenever a product outline has been installed asynchronously
     */
    public void setChangeListener(final Runnable listener) {
        this.changeListener = listener;
    }

    /**
     * @param listener called when a product is shown or hidden with {@link #setProductVisible}, so that what other
     *                 layers draw for it (e.g. its full-resolution image) can follow
     */
    public void setProductVisibilityListener(final BiConsumer<Product, Boolean> listener) {
        this.productVisibilityListener = listener;
    }

    public String[] getProductNames() {
        final List<String> list = new ArrayList<>(outlineTable.keySet());
        Collections.sort(list);
        return list.toArray(new String[0]);
    }

    @Override
    public void setOpacity(double opacity) {
        super.setOpacity(opacity);

        for (String name : outlineTable.keySet()) {
            setOpacity(name, opacity);
        }
        if (opacityListener != null) {
            opacityListener.accept(opacity);
        }
    }

    /**
     * @param listener called with the new opacity whenever the layer opacity is set, so that other layers drawing
     *                 product images (the full-resolution images) follow the same opacity
     */
    public void setOpacityListener(final DoubleConsumer listener) {
        this.opacityListener = listener;
    }

    public void setOpacity(String name, double opacity) {
        final ProductOutline outline = getOutline(name);
        if (outline != null && outline.image != null) {
            outline.image.setOpacity(opacity);
        }
    }

    /**
     * @return the opacity of the quicklook of a product, 1 for a product without quicklook and 0 for an unknown one
     */
    public double getOpacity(String name) {
        final ProductOutline outline = getOutline(name);
        if (outline == null) {
            return 0;
        }
        return outline.image != null ? outline.image.getOpacity() : 1;
    }

    private ProductOutline getOutline(final String name) {
        return name != null ? outlineTable.get(name) : null;
    }

    /**
     * @return true if the product has been added to this layer (its outline may still be being computed)
     */
    public boolean hasProduct(final Product product) {
        return product != null && productKeys.containsKey(product);
    }

    /**
     * Shows or hides everything drawn for one product: outlines, labels and image.
     */
    public void setProductVisible(final String name, final boolean visible) {
        final ProductOutline outline = getOutline(name);
        if (outline == null || outline.visible == visible) {
            return;
        }
        outline.visible = visible;
        final Product product = findProduct(name);
        if (product != null && productVisibilityListener != null) {
            productVisibilityListener.accept(product, visible);
        }
        final List<SurfaceImage> hidden = product != null && hiddenQuicklooks.contains(product) ?
                getQuicklooks(outline) : Collections.emptyList();
        for (Renderable r : getRenderables(outline)) {
            if (hidden.contains(r)) {
                continue;
            }
            if (visible) {
                addRenderable(r);
            } else {
                removeRenderable(r);
            }
        }
    }

    /**
     * Shows or hides the quicklook images of a product, keeping its outline.
     */
    public void setQuicklookVisible(final Product product, final boolean visible) {
        final boolean changed = visible ? hiddenQuicklooks.remove(product) : hiddenQuicklooks.add(product);
        if (!changed) {
            return;
        }
        final ProductOutline outline = outlineTable.get(keyOf(product));
        if (outline == null || !outline.visible) {
            return;
        }
        for (SurfaceImage image : getQuicklooks(outline)) {
            if (visible) {
                addRenderable(image);
            } else {
                removeRenderable(image);
            }
        }
    }

    public boolean isQuicklookVisible(final Product product) {
        return !hiddenQuicklooks.contains(product);
    }

    private static List<SurfaceImage> getQuicklooks(final ProductOutline outline) {
        return outline.image != null ? Collections.singletonList(outline.image) : Collections.emptyList();
    }

    public boolean isProductVisible(final String name) {
        final ProductOutline outline = getOutline(name);
        return outline != null && outline.visible;
    }

    @Override
    public void setSelectedProduct(final Product product) {
        super.setSelectedProduct(product);

        final String selName = selectedProduct != null ? keyOf(selectedProduct) : null;
        for (Map.Entry<String, ProductOutline> entry : outlineTable.entrySet()) {
            highlight(entry.getValue(), entry.getKey().equals(selName));
        }
    }

    private static void highlight(final ProductOutline productOutline, final boolean highlight) {
        if (productOutline.showProductBoundary) {
            for (Path line : productOutline.productLineList) {
                line.setHighlighted(highlight);
            }
        } else {
            for (BandOutline bandOutline : productOutline.bandOutlines) {
                for (Path line : bandOutline.bandLineList) {
                    line.setHighlighted(highlight);
                }
            }
        }
    }

    @Override
    public void setSelectedRaster(final RasterDataNode raster) {
        super.setSelectedRaster(raster);

        if (selectedRaster != null && selectedProduct != null) {
            final String selName = keyOf(selectedProduct);
            for (Map.Entry<String, ProductOutline> entry : outlineTable.entrySet()) {
                final boolean isProduct = entry.getKey().equals(selName);

                for(BandOutline bandOutline : entry.getValue().bandOutlines) {
                    boolean highlight = isProduct && bandOutline.bandName.equals(selectedRaster.getName());
                    bandOutline.bandLabel.setVisible(highlight);
                    for (Path line : bandOutline.bandLineList) {
                        line.setHighlighted(highlight);
                    }
                }
            }
        }
    }

    @Override
    public Suitability getSuitability(Product product) {
        return Suitability.SUITABLE;
    }

    private Product findProduct(final String name) {
        synchronized (productKeys) {
            for (Map.Entry<Product, String> entry : productKeys.entrySet()) {
                if (entry.getValue().equals(name)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private String keyOf(final Product product) {
        final String key = productKeys.get(product);
        return key != null ? key : getUniqueName(product);
    }

    @Override
    public void addProduct(final Product product, final WorldWindowGLCanvas wwd) {

        final String name = getUniqueName(product);
        if (outlineTable.containsKey(name) || !pendingProducts.add(name))
            return;
        productKeys.put(product, name);

        final String productType = product.getProductType();
        if ("ASA_WVW_2P".equals(productType) || "ASA_WVS_1P".equals(productType) || "ASA_WVI_1P".equals(productType)) {
            // cheap, from metadata only
            installOutline(name, product, createWaveProductOutline(product), wwd);
            return;
        }

        pendingTasks.incrementAndGet();
        outlineExecutor.execute(() -> {
            ProductOutline outline;
            try {
                outline = createOutline(product);
            } catch (Throwable e) {
                SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to compute the outline of " + product.getName(), e);
                outline = new ProductOutline();
            }
            final ProductOutline result = outline;
            SwingUtilities.invokeLater(() -> {
                try {
                    installOutline(name, product, result, wwd);
                } finally {
                    pendingTasks.decrementAndGet();
                }
            });
        });
    }

    private void installOutline(final String name, final Product product, final ProductOutline outline,
                                final WorldWindowGLCanvas wwd) {
        if (!pendingProducts.remove(name)) {
            // the product was removed while its outline was being computed
            return;
        }
        for (Renderable r : getRenderables(outline)) {
            addRenderable(r);
        }
        outlineTable.put(name, outline);
        if (selectedProduct != null) {
            highlight(outline, name.equals(keyOf(selectedProduct)));
        }

        if (enableSurfaceImages && outline.label != null && canAddSurfaceImage(product)) {
            addSurfaceImage(product, name, outline, wwd);
        }
        if (wwd != null) {
            wwd.redraw();
        }
        if (changeListener != null) {
            changeListener.run();
        }
    }

    private static List<Renderable> getRenderables(final ProductOutline outline) {
        final List<Renderable> list = new ArrayList<>();
        if (outline.showProductBoundary) {
            Collections.addAll(list, outline.productLineList);
        }
        for (BandOutline bandOutline : outline.bandOutlines) {
            Collections.addAll(list, bandOutline.bandLineList);
            list.add(bandOutline.bandLabel);
        }
        if (outline.label != null) {
            list.add(outline.label);
        }
        if (outline.image != null) {
            list.add(outline.image);
        }
        return list;
    }

    /**
     * Waits until all queued outline computations have been installed on the EDT. For tests.
     */
    boolean awaitPendingOutlines(final long timeoutMillis) throws Exception {
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

    int getRenderableCount() {
        return getNumRenderables();
    }

    private static boolean canAddSurfaceImage(final Product product) {
        return product.getSceneGeoCoding() != null && InputProductValidator.isMapProjected(product);
    }

    private void addSurfaceImage(final Product product, final String name, final ProductOutline outline,
                                 final WorldWindowGLCanvas wwd) {

        pendingTasks.incrementAndGet();
        final SwingWorker<SurfaceImage, Object> worker = new SwingWorker<>() {

            @Override
            protected SurfaceImage doInBackground() throws Exception {
                final List<LatLon> corners = getImageCorners(product.getSceneGeoCoding(),
                                                             product.getSceneRasterWidth(),
                                                             product.getSceneRasterHeight());
                if (corners == null) {
                    SystemUtils.LOG.info("WorldWind: no quicklook for " + product.getName() +
                                                 ": invalid corner geo-positions or the image crosses the antimeridian");
                    return null;
                }
                final Product newProduct = createSubsampledProduct(product);
                try {
                    final Band band = newProduct.getBandAt(0);
                    final BufferedImage image = ProductUtils.createRgbImage(new RasterDataNode[]{band},
                                                                            band.getImageInfo(com.bc.ceres.core.ProgressMonitor.NULL),
                                                                            com.bc.ceres.core.ProgressMonitor.NULL);

                    return new SurfaceImage(image, corners);
                } finally {
                    newProduct.dispose();
                }
            }

            @Override
            public void done() {
                try {
                    install();
                } finally {
                    pendingTasks.decrementAndGet();
                }
            }

            private void install() {
                final SurfaceImage si;
                try {
                    si = get();
                } catch (Exception e) {
                    SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to create the quicklook of " + name, e);
                    return;
                }
                if (si == null || outlineTable.get(name) != outline) {
                    // nothing to show, or the product was removed in the meantime (and perhaps added again, with
                    // a new outline that gets its own quicklook)
                    return;
                }
                // the opacity may have been changed while the quicklook was made
                si.setOpacity(getOpacity());
                outline.image = si;
                if (outline.visible && isQuicklookVisible(product)) {
                    addRenderable(si);
                }
                if (wwd != null) {
                    wwd.redraw();
                }
            }
        };
        worker.execute();
    }

    /**
     * Returns the geo-positions of the outer image corners in the order expected by
     * {@link SurfaceImage#SurfaceImage(Object, Iterable)}: lower-left, lower-right, upper-right, upper-left,
     * where the upper image edge is raster line 0. Returns null if a corner is invalid or the corners straddle
     * the antimeridian, which a corner-based SurfaceImage cannot represent.
     */
    static List<LatLon> getImageCorners(final GeoCoding geoCoding, final int width, final int height) {
        if (geoCoding == null) {
            return null;
        }
        final PixelPos[] pixels = {
                new PixelPos(0, height), new PixelPos(width, height), new PixelPos(width, 0), new PixelPos(0, 0)
        };
        final List<LatLon> corners = new ArrayList<>(4);
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (PixelPos pixel : pixels) {
            final GeoPos geoPos = geoCoding.getGeoPos(pixel, null);
            if (geoPos == null || !geoPos.isValid()) {
                return null;
            }
            geoPos.normalize();
            minLon = Math.min(minLon, geoPos.lon);
            maxLon = Math.max(maxLon, geoPos.lon);
            corners.add(LatLon.fromDegrees(geoPos.lat, geoPos.lon));
        }
        if (maxLon - minLon > 180) {
            return null;
        }
        return corners;
    }

    private ProductOutline createOutline(final Product product) {

        final int prodWidth = product.getSceneRasterWidth();
        final int prodHeight = product.getSceneRasterHeight();
        final int step = Math.max(50, (prodWidth + prodHeight) / 10);

        // the product boundary comes from the scene geo-coding or, failing that, from the first geo-coded band
        GeneralPath[] boundaryPaths = null;
        if (product.getSceneGeoCoding() != null) {
            boundaryPaths = org.esa.snap.core.util.GeoUtils.createGeoBoundaryPaths(product, null, step, true);
        } else {
            for (Band band : product.getBands()) {
                if (band.getGeoCoding() != null) {
                    boundaryPaths = org.esa.snap.core.util.GeoUtils.createGeoBoundaryPaths(band, null, step, true);
                    break;
                }
            }
        }

        // bands of a different size than the scene get their own outline
        final List<BandOutline> bandOutlines = new ArrayList<>();
        for (Band band : product.getBands()) {
            if (band.getGeoCoding() != null &&
                    !(band.getRasterWidth() == prodWidth && band.getRasterHeight() == prodHeight)) {
                try {
                    final BandOutline bandOutline = createBandOutline(band, step);
                    if (bandOutline != null) {
                        bandOutlines.add(bandOutline);
                    }
                } catch (Exception e) {
                    SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to compute the outline of band " + band.getName(), e);
                }
            }
        }

        final ProductOutline productOutline = new ProductOutline();
        productOutline.bandOutlines = bandOutlines.toArray(new BandOutline[0]);
        productOutline.showProductBoundary = bandOutlines.isEmpty() || bandOutlines.size() != product.getNumBands();

        final List<Path> productLineList = new ArrayList<>();
        final List<Position> allPositions = new ArrayList<>();
        if (boundaryPaths != null) {
            for (GeneralPath boundaryPath : boundaryPaths) {
                final Path polyLine = createPolyLine(boundaryPath);
                if (polyLine != null) {
                    productLineList.add(polyLine);
                    addPositions(allPositions, polyLine);
                }
            }
        }
        if (allPositions.isEmpty()) {
            for (BandOutline bandOutline : bandOutlines) {
                for (Path polyLine : bandOutline.bandLineList) {
                    addPositions(allPositions, polyLine);
                }
            }
        }
        productOutline.productLineList = productLineList.toArray(new Path[0]);

        final Position centrePos = calculateCentre(allPositions);
        if (centrePos != null) {
            final PointPlacemark ppm = getLabelPlacemark(centrePos, String.valueOf(product.getRefNo()));
            ppm.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            ppm.setEnableDecluttering(true);
            productOutline.label = ppm;
        }
        return productOutline;
    }

    private BandOutline createBandOutline(final Band band, final int step) {
        final GeneralPath[] paths = org.esa.snap.core.util.GeoUtils.createGeoBoundaryPaths(band, null, step, true);
        final List<Path> bandBoundaryPaths = new ArrayList<>();
        final List<Position> positions = new ArrayList<>();
        for (GeneralPath bandBoundaryPath : paths) {
            final Path polyLine = createPolyLine(bandBoundaryPath);
            if (polyLine != null) {
                bandBoundaryPaths.add(polyLine);
                addPositions(positions, polyLine);
            }
        }
        final Position centrePos = calculateCentre(positions);
        if (centrePos == null) {
            return null;
        }
        final PointPlacemark ppm = getLabelPlacemark(centrePos, band.getName());
        ppm.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        ppm.setEnableDecluttering(true);
        ppm.setVisible(false);

        final BandOutline bandOutline = new BandOutline();
        bandOutline.bandName = band.getName();
        bandOutline.bandLineList = bandBoundaryPaths.toArray(new Path[0]);
        bandOutline.bandLabel = ppm;
        return bandOutline;
    }

    private static void addPositions(final List<Position> list, final Path path) {
        for (Position pos : path.getPositions()) {
            list.add(pos);
        }
    }

    /**
     * Centre of a set of positions computed on the unit sphere, so that it is correct across the antimeridian and
     * near the poles. Returns null for an empty set or for positions that cancel out.
     */
    static Position calculateCentre(final List<Position> positions) {
        double x = 0, y = 0, z = 0;
        int n = 0;
        for (Position pos : positions) {
            final double lat = pos.getLatitude().getRadians();
            final double lon = pos.getLongitude().getRadians();
            if (Double.isNaN(lat) || Double.isNaN(lon)) {
                continue;
            }
            x += Math.cos(lat) * Math.cos(lon);
            y += Math.cos(lat) * Math.sin(lon);
            z += Math.sin(lat);
            n++;
        }
        final double r = Math.sqrt(x * x + y * y + z * z);
        if (n == 0 || r < 1e-9 * n) {
            return null;
        }
        return new Position(Angle.fromRadiansLatitude(Math.asin(z / r)),
                            Angle.fromRadiansLongitude(Math.atan2(y, x)), 0.0);
    }

    private Path createPolyLine(final GeneralPath boundaryPath) {
        final PathIterator it = boundaryPath.getPathIterator(null);
        final float[] floats = new float[6];
        final List<Position> positions = new ArrayList<>();

        while (!it.isDone()) {
            final int type = it.currentSegment(floats);
            if (type != PathIterator.SEG_CLOSE && !Float.isNaN(floats[0]) && !Float.isNaN(floats[1])) {
                positions.add(new Position(Angle.fromDegreesLatitude(floats[1]),
                                           Angle.fromDegreesLongitude(floats[0]), 0.0));
            }
            it.next();
        }
        if (positions.size() < 2) {
            return null;
        }
        // close the loop
        positions.add(positions.get(0));

        return createPath(positions, WHITE_MATERIAL, RED_MATERIAL);
    }

    private ProductOutline createWaveProductOutline(final Product product) {
        final ProductOutline productOutline = new ProductOutline();
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement ggADS = root != null ? root.getElement("GEOLOCATION_GRID_ADS") : null;
        if (ggADS == null) return productOutline;

        final MetadataElement[] geoElemList = ggADS.getElements();
        final List<Path> lineList = new ArrayList<>();
        final List<Position> allPositions = new ArrayList<>();

        for (MetadataElement geoElem : geoElemList) {
            final double lat = geoElem.getAttributeDouble("center_lat", 0.0) / Constants.oneMillion;
            final double lon = geoElem.getAttributeDouble("center_long", 0.0) / Constants.oneMillion;
            final double heading = geoElem.getAttributeDouble("heading", 0.0);

            final GeoUtils.LatLonHeading r1 = GeoUtils.vincenty_direct(new GeoPos(lat, lon), 5000, heading);
            final GeoUtils.LatLonHeading corner1 = GeoUtils.vincenty_direct(new GeoPos(r1.lat, r1.lon), 2500, heading - 90.0);
            final GeoUtils.LatLonHeading corner2 = GeoUtils.vincenty_direct(new GeoPos(r1.lat, r1.lon), 2500, heading + 90.0);

            final GeoUtils.LatLonHeading r2 = GeoUtils.vincenty_direct(new GeoPos(lat, lon), 5000, heading + 180.0);
            final GeoUtils.LatLonHeading corner3 = GeoUtils.vincenty_direct(new GeoPos(r2.lat, r2.lon), 2500, heading - 90.0);
            final GeoUtils.LatLonHeading corner4 = GeoUtils.vincenty_direct(new GeoPos(r2.lat, r2.lon), 2500, heading + 90.0);

            final List<Position> positions = new ArrayList<>();
            positions.add(new Position(Angle.fromDegreesLatitude(corner1.lat), Angle.fromDegreesLongitude(corner1.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner2.lat), Angle.fromDegreesLongitude(corner2.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner4.lat), Angle.fromDegreesLongitude(corner4.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner3.lat), Angle.fromDegreesLongitude(corner3.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner1.lat), Angle.fromDegreesLongitude(corner1.lon), 0.0));

            allPositions.addAll(positions.subList(0, 4));
            lineList.add(createPath(positions, WHITE_MATERIAL, RED_MATERIAL));
        }
        productOutline.productLineList = lineList.toArray(new Path[0]);

        final Position centrePos = calculateCentre(allPositions);
        if (centrePos != null) {
            final PointPlacemark ppm = getLabelPlacemark(centrePos, String.valueOf(product.getRefNo()));
            ppm.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            ppm.setEnableDecluttering(true);
            productOutline.label = ppm;
        }
        return productOutline;
    }

    private PointPlacemark getLabelPlacemark(Position pos, String label) {
        PointPlacemarkAttributes ppmAtttrs = new PointPlacemarkAttributes();
        ppmAtttrs.setLabelOffset(new Offset(0.0, 0.0, AVKey.PIXELS, AVKey.PIXELS));
        ppmAtttrs.setScale(0.0);

        PointPlacemark ppm = new PointPlacemark(pos);
        ppm.setAttributes(ppmAtttrs);
        ppm.setLabelText(label);
        return ppm;
    }

    @Override
    public void removeProduct(final Product product) {
        final String name = keyOf(product);
        productKeys.remove(product);
        pendingProducts.remove(name);
        hiddenQuicklooks.remove(product);

        final ProductOutline productOutline = outlineTable.remove(name);
        if (productOutline != null) {
            for (Renderable r : getRenderables(productOutline)) {
                removeRenderable(r);
            }
        }
        if (selectedProduct == product) {
            selectedProduct = null;
            selectedRaster = null;
        }
    }

    private static Product createSubsampledProduct(final Product product) throws IOException {

        final String quicklookBandName = ProductUtils.findSuitableQuicklookBandName(product);
        final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
        final int maxSize = Math.max(product.getSceneRasterWidth(), product.getSceneRasterHeight());
        final int scaleFactor = Math.max(1, (int) Math.ceil(maxSize / (double) QUICKLOOK_SIZE));
        productSubsetDef.setSubSampling(scaleFactor, scaleFactor);
        productSubsetDef.setTreatVirtualBandsAsRealBands(true);
        productSubsetDef.setNodeNames(new String[]{quicklookBandName});
        Product productSubset = product.createSubset(productSubsetDef, quicklookBandName, null);

        if (!InputProductValidator.isMapProjected(product) && productSubset.getSceneGeoCoding() != null) {
            try {
                final Map<String, Object> projParameters = new HashMap<>();
                Map<String, Product> projProducts = new HashMap<>();
                projProducts.put("source", productSubset);
                projParameters.put("crs", "WGS84(DD)");
                productSubset = GPF.createProduct("Reproject", projParameters, projProducts);
            } catch (Exception e) {
                SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to reproject the quicklook", e);
            }
        }

        return productSubset;
    }

    @Override
    public JPanel getControlPanel(final WorldWindowGLCanvas wwd) {
        // also sets the opacity of the full-resolution images, through the opacity listener
        final JSlider opacitySlider = new JSlider(0, 100, (int) Math.round(getOpacity() * 100));
        opacitySlider.addChangeListener(e -> {
            setOpacity(opacitySlider.getValue() / 100d);
            if (wwd != null) {
                wwd.redraw();
            }
        });

        final JPanel opacityPanel = new JPanel(new BorderLayout(5, 5));
        opacityPanel.add(new JLabel("Image Opacity"), BorderLayout.WEST);
        opacityPanel.add(opacitySlider, BorderLayout.CENTER);
        return opacityPanel;
    }
}
