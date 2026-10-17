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

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.grender.Viewport;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkGroup;
import org.esa.snap.core.datamodel.PlacemarkNameFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.worldwind.model.ImageViewGeoUtils;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Links the globe with SNAP's image views (see docs/WorldWind-Review.md, section 6.2): shows the pins and GCPs of
 * all products and the cursor of the selected image view, and places and selects pins from the globe.
 * <p>
 * All methods are called on the EDT.
 */
public class ImageViewSyncLayer extends BaseLayer implements WWLayer {

    static final String PIN_IMAGE = "images/pushpins/plain-yellow.png";
    static final String GCP_IMAGE = "images/pushpins/plain-teal.png";
    static final String CURSOR_IMAGE = "images/32x32-crosshair-simple.png";

    private final Set<Product> products = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Product, ProductNodeListenerAdapter> nodeListeners = new IdentityHashMap<>();
    private final Map<Placemark, PointPlacemark> placemarks = new IdentityHashMap<>();
    private final PointPlacemarkAttributes pinAttributes = createPlacemarkAttributes(PIN_IMAGE);
    private final PointPlacemarkAttributes gcpAttributes = createPlacemarkAttributes(GCP_IMAGE);

    private final PointPlacemark cursor;
    private ProductSceneView cursorView;
    private final PixelPositionListener cursorListener = new CursorListener();
    private WorldWindowGLCanvas wwd;

    public ImageViewSyncLayer() {
        setName("Pins and Cursor");
        cursor = createCursor();
    }

    private static PointPlacemarkAttributes createPlacemarkAttributes(final String image) {
        final PointPlacemarkAttributes attributes = new PointPlacemarkAttributes();
        attributes.setImageAddress(image);
        attributes.setScale(0.6);
        // the tip of the pushpin is at the bottom left of its image
        attributes.setImageOffset(new Offset(0.3, 0.0, AVKey.FRACTION, AVKey.FRACTION));
        attributes.setLabelOffset(new Offset(0.9, 0.6, AVKey.FRACTION, AVKey.FRACTION));
        return attributes;
    }

    private static PointPlacemark createCursor() {
        final PointPlacemarkAttributes attributes = new PointPlacemarkAttributes();
        attributes.setImageAddress(CURSOR_IMAGE);
        attributes.setScale(1.0);
        attributes.setImageOffset(new Offset(0.5, 0.5, AVKey.FRACTION, AVKey.FRACTION));
        final PointPlacemark cursor = new PointPlacemark(Position.ZERO);
        cursor.setAttributes(attributes);
        cursor.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        cursor.setVisible(false);
        cursor.setEnableDecluttering(false);
        return cursor;
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
        if (!containsCursor()) {
            addRenderable(cursor);
        }
        final ProductNodeListenerAdapter listener = new PlacemarkListener(product);
        product.addProductNodeListener(listener);
        nodeListeners.put(product, listener);

        for (Placemark placemark : product.getPinGroup().toArray(new Placemark[0])) {
            updatePlacemark(product, placemark);
        }
        for (Placemark placemark : product.getGcpGroup().toArray(new Placemark[0])) {
            updatePlacemark(product, placemark);
        }
        redraw();
    }

    private boolean containsCursor() {
        for (Object renderable : getRenderables()) {
            if (renderable == cursor) {
                return true;
            }
        }
        return false;
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
        for (Placemark placemark : placemarks.keySet().toArray(new Placemark[0])) {
            if (placemark.getProduct() == product || placemark.getProduct() == null) {
                removePlacemark(placemark);
            }
        }
        if (cursorView != null && cursorView.getProduct() == product) {
            setCursorView(null);
        }
        if (selectedProduct == product) {
            selectedProduct = null;
            selectedRaster = null;
        }
        redraw();
    }

    @Override
    public void setSelectedSceneView(final ProductSceneView view) {
        setCursorView(view != null && products.contains(view.getProduct()) ? view : null);
    }

    private void setCursorView(final ProductSceneView view) {
        if (view == cursorView) {
            return;
        }
        if (cursorView != null) {
            cursorView.removePixelPositionListener(cursorListener);
        }
        cursorView = view;
        if (cursorView != null) {
            cursorView.addPixelPositionListener(cursorListener);
        }
        setCursor(null);
    }

    /**
     * Shows the image view cursor at a geo-position, or hides it.
     */
    void setCursor(final GeoPos geoPos) {
        if (geoPos == null || !geoPos.isValid()) {
            if (cursor.isVisible()) {
                cursor.setVisible(false);
                redraw();
            }
            return;
        }
        cursor.setPosition(Position.fromDegrees(geoPos.lat, geoPos.lon, 0));
        cursor.setVisible(true);
        redraw();
    }

    PointPlacemark getCursor() {
        return cursor;
    }

    ProductSceneView getCursorView() {
        return cursorView;
    }

    private class CursorListener implements PixelPositionListener {

        @Override
        public void pixelPosChanged(final ImageLayer baseImageLayer, final int pixelX, final int pixelY,
                                    final int currentLevel, final boolean pixelPosValid, final MouseEvent e) {
            final ProductSceneView view = cursorView;
            if (!pixelPosValid || view == null || view.getRaster() == null) {
                setCursor(null);
                return;
            }
            final AffineTransform i2m = baseImageLayer.getImageToModelTransform(currentLevel);
            final AffineTransform m2i = baseImageLayer.getModelToImageTransform();
            final PixelPos pixelPos = ImageViewGeoUtils.levelToRasterPixel(i2m, m2i, pixelX, pixelY);
            setCursor(ImageViewGeoUtils.toGeoPos(view.getRaster().getGeoCoding(), pixelPos));
        }

        @Override
        public void pixelPosNotAvailable() {
            setCursor(null);
        }
    }

    private class PlacemarkListener extends ProductNodeListenerAdapter {

        private final Product product;

        PlacemarkListener(final Product product) {
            this.product = product;
        }

        @Override
        public void nodeAdded(final ProductNodeEvent event) {
            handle(event, false);
        }

        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            handle(event, false);
        }

        @Override
        public void nodeRemoved(final ProductNodeEvent event) {
            handle(event, true);
        }

        private void handle(final ProductNodeEvent event, final boolean removed) {
            final ProductNode node = event.getSourceNode();
            if (!(node instanceof Placemark)) {
                return;
            }
            final Placemark placemark = (Placemark) node;
            runOnEDT(() -> {
                if (!products.contains(product)) {
                    return;
                }
                if (removed || !isPinOrGcp(product, placemark)) {
                    removePlacemark(placemark);
                } else {
                    updatePlacemark(product, placemark);
                }
                redraw();
            });
        }
    }

    private static boolean isPinOrGcp(final Product product, final Placemark placemark) {
        return product.getPinGroup().contains(placemark) || product.getGcpGroup().contains(placemark);
    }

    /**
     * Creates or moves the globe placemark of a pin or GCP.
     */
    void updatePlacemark(final Product product, final Placemark placemark) {
        final GeoPos geoPos = ImageViewGeoUtils.placemarkGeoPos(placemark, product.getSceneGeoCoding());
        if (geoPos == null) {
            removePlacemark(placemark);
            return;
        }
        PointPlacemark ppm = placemarks.get(placemark);
        if (ppm == null) {
            ppm = new PointPlacemark(Position.fromDegrees(geoPos.lat, geoPos.lon, 0));
            ppm.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            ppm.setEnableDecluttering(true);
            // picked objects lead back to the SNAP placemark
            ppm.setValue(AVKey.CONTEXT, placemark);
            placemarks.put(placemark, ppm);
            addRenderable(ppm);
        } else {
            ppm.setPosition(Position.fromDegrees(geoPos.lat, geoPos.lon, 0));
        }
        ppm.setAttributes(placemark.getDescriptor() instanceof GcpDescriptor ? gcpAttributes : pinAttributes);
        ppm.setLabelText(placemark.getLabel());
    }

    private void removePlacemark(final Placemark placemark) {
        final PointPlacemark ppm = placemarks.remove(placemark);
        if (ppm != null) {
            removeRenderable(ppm);
        }
    }

    PointPlacemark getGlobePlacemark(final Placemark placemark) {
        return placemarks.get(placemark);
    }

    int getNumPlacemarks() {
        return placemarks.size();
    }

    /**
     * @return the SNAP placemark of a picked globe object, or null
     */
    public static Placemark getPlacemark(final Object pickedObject) {
        if (pickedObject instanceof PointPlacemark) {
            final Object context = ((PointPlacemark) pickedObject).getValue(AVKey.CONTEXT);
            if (context instanceof Placemark) {
                return (Placemark) context;
            }
        }
        return null;
    }

    /**
     * Selects a pin or GCP in an image view of its product.
     *
     * @return true if it was selected
     */
    public static boolean selectInView(final Placemark placemark, final ProductSceneView view) {
        final Product product = placemark.getProduct();
        if (view == null || product == null || view.getProduct() != product) {
            return false;
        }
        if (product.getPinGroup().contains(placemark)) {
            view.selectPins(new Placemark[]{placemark});
        } else if (product.getGcpGroup().contains(placemark)) {
            view.selectGcps(new Placemark[]{placemark});
        } else {
            return false;
        }
        return true;
    }

    /**
     * Adds a pin at a geo-position to a product.
     *
     * @return the pin, or null if the position is outside the product
     */
    public static Placemark placePin(final Product product, final GeoPos geoPos) {
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        final PixelPos pixelPos = ImageViewGeoUtils.toPixel(geoCoding, product.getSceneRasterWidth(),
                                                            product.getSceneRasterHeight(), geoPos);
        if (pixelPos == null) {
            return null;
        }
        final String[] nameAndLabel = PlacemarkNameFactory.createUniqueNameAndLabel(PinDescriptor.getInstance(), product);
        final Placemark pin = Placemark.createPointPlacemark(PinDescriptor.getInstance(), nameAndLabel[0],
                                                             nameAndLabel[1], "", pixelPos, null, geoCoding);
        final PlacemarkGroup pinGroup = product.getPinGroup();
        pinGroup.add(pin);
        return pin;
    }

    /**
     * Centres an image view on a geo-position, keeping its zoom.
     *
     * @return true if the position is inside the view's raster
     */
    public static boolean centreView(final ProductSceneView view, final GeoPos geoPos) {
        if (view == null || view.getRaster() == null) {
            return false;
        }
        final PixelPos pixelPos = ImageViewGeoUtils.toRasterPixel(view.getRaster(), geoPos);
        if (pixelPos == null) {
            return false;
        }
        final Point2D model = view.getBaseImageLayer().getImageToModelTransform()
                .transform(new Point2D.Double(pixelPos.x, pixelPos.y), null);
        final Viewport viewport = view.getLayerCanvas().getViewport();
        viewport.setZoomFactor(viewport.getZoomFactor(), model.getX(), model.getY());
        return true;
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
    public JPanel getControlPanel(final WorldWindowGLCanvas wwd) {
        return null;
    }
}
