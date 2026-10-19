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

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ScreenAnnotation;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfacePolyline;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.worldwind.model.VectorDataConverter;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Shows the vector data of the products on the globe (see docs/WorldWind-Review.md, section 6.3), with the
 * colours of their SNAP styles, following changes of the features and nodes.
 * <p>
 * The conversion runs on a background thread and the shapes are installed on the EDT; products are added and
 * removed on the EDT.
 */
public class VectorDataLayer extends BaseLayer implements WWLayer {

    private static final AtomicInteger threadCount = new AtomicInteger();
    private static final ExecutorService vectorExecutor = Executors.newSingleThreadExecutor(r -> {
        final Thread t = new Thread(r, "WorldWind-VectorData-" + threadCount.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private final Set<Product> products = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Product, ProductNodeListenerAdapter> nodeListeners = new IdentityHashMap<>();
    private final Map<VectorDataNode, List<Renderable>> shapes = new IdentityHashMap<>();
    // the latest requested conversion of each node; an older conversion is discarded
    private final Map<VectorDataNode, Integer> generations = new IdentityHashMap<>();
    // the same, readable by the conversion thread: a conversion that is already superseded when it starts is skipped,
    // so that a burst of feature events (one per added feature) converts the node once, not once per event
    private final Map<VectorDataNode, Integer> requestedGenerations = Collections.synchronizedMap(new IdentityHashMap<>());
    // conversions actually run, for tests
    private final AtomicInteger conversions = new AtomicInteger();
    // nodes whose conversion is about to be submitted (EDT only)
    private final Set<VectorDataNode> scheduledNodes = Collections.newSetFromMap(new IdentityHashMap<>());
    private final AtomicInteger pendingTasks = new AtomicInteger();
    private final ScreenAnnotation infoAnnotation;
    private WorldWindowGLCanvas wwd;

    public VectorDataLayer() {
        setName("Vector Data");
        infoAnnotation = new ScreenAnnotation("", new Point(0, 0));
        infoAnnotation.getAttributes().setSize(new Dimension(Integer.MAX_VALUE, 0));
        infoAnnotation.getAttributes().setAdjustWidthToText(AVKey.SIZE_FIT_TEXT);
        infoAnnotation.getAttributes().setTextColor(Color.WHITE);
        infoAnnotation.getAttributes().setBackgroundColor(new Color(0f, 0f, 0f, .5f));
        infoAnnotation.getAttributes().setInsets(new Insets(4, 4, 4, 4));
        infoAnnotation.getAttributes().setVisible(false);
        infoAnnotation.setPickEnabled(false);
        addRenderable(infoAnnotation);
    }

    @Override
    public boolean isComplementary() {
        return true;
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
        if (product.getSceneGeoCoding() == null) {
            // nothing can be placed, but the product is remembered so that it is not added again
            return;
        }
        final ProductNodeListenerAdapter listener = new VectorDataListener(product);
        product.addProductNodeListener(listener);
        nodeListeners.put(product, listener);
        for (VectorDataNode node : product.getVectorDataGroup().toArray(new VectorDataNode[0])) {
            update(product, node);
        }
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
        for (VectorDataNode node : new ArrayList<>(generations.keySet())) {
            if (node.getProduct() == product || node.getProduct() == null) {
                generations.remove(node);
                requestedGenerations.remove(node);
            }
        }
        for (VectorDataNode node : new ArrayList<>(shapes.keySet())) {
            if (node.getProduct() == product || node.getProduct() == null) {
                removeShapes(node);
            }
        }
        infoAnnotation.getAttributes().setVisible(false);
        if (selectedProduct == product) {
            selectedProduct = null;
            selectedRaster = null;
        }
        redraw();
    }

    private class VectorDataListener extends ProductNodeListenerAdapter {

        private final Product product;

        VectorDataListener(final Product product) {
            this.product = product;
        }

        @Override
        public void nodeAdded(final ProductNodeEvent event) {
            handle(event.getSourceNode(), false);
        }

        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            // features added, changed or removed, or the style changed
            handle(event.getSourceNode(), false);
        }

        @Override
        public void nodeRemoved(final ProductNodeEvent event) {
            handle(event.getSourceNode(), true);
        }

        private void handle(final ProductNode node, final boolean removed) {
            if (!(node instanceof VectorDataNode)) {
                return;
            }
            final VectorDataNode vectorDataNode = (VectorDataNode) node;
            runOnEDT(() -> {
                if (!products.contains(product)) {
                    return;
                }
                if (removed) {
                    generations.remove(vectorDataNode);
                    requestedGenerations.remove(vectorDataNode);
                    removeShapes(vectorDataNode);
                    redraw();
                } else {
                    update(product, vectorDataNode);
                }
            });
        }
    }

    /**
     * Converts a node in the background and replaces its shapes on the EDT. Changes arriving before the conversion
     * is submitted are taken into the same conversion: the submission waits until the EDT has handled the current
     * events, e.g. all features of an import.
     */
    private void update(final Product product, final VectorDataNode node) {
        final int generation = generations.merge(node, 1, Integer::sum);
        requestedGenerations.put(node, generation);
        if (!VectorDataConverter.isShown(product, node)) {
            removeShapes(node);
            redraw();
            return;
        }
        if (!scheduledNodes.add(node)) {
            return; // a conversion is about to be submitted and will see this change
        }
        pendingTasks.incrementAndGet();
        SwingUtilities.invokeLater(() -> {
            scheduledNodes.remove(node);
            final Integer latest = generations.get(node);
            if (latest == null || !products.contains(product)) {
                pendingTasks.decrementAndGet();
                return;
            }
            convert(product, node, latest);
        });
    }

    private void convert(final Product product, final VectorDataNode node, final int generation) {
        vectorExecutor.execute(() -> {
            List<Renderable> renderables = null;
            try {
                final Integer requested = requestedGenerations.get(node);
                if (requested != null && requested == generation) {
                    conversions.incrementAndGet();
                    renderables = createRenderables(node, new VectorDataConverter(product).convert(node));
                }
            } catch (Throwable e) {
                SystemUtils.LOG.log(Level.WARNING, "WorldWind: unable to show the vector data " + node.getName(), e);
            }
            final List<Renderable> result = renderables;
            SwingUtilities.invokeLater(() -> {
                try {
                    final Integer latest = generations.get(node);
                    if (result == null || latest == null || latest != generation || !products.contains(product)) {
                        return;
                    }
                    removeShapes(node);
                    for (Renderable renderable : result) {
                        addRenderable(renderable);
                    }
                    shapes.put(node, result);
                    redraw();
                } finally {
                    pendingTasks.decrementAndGet();
                }
            });
        });
    }

    int getConversionCount() {
        return conversions.get();
    }

    static List<Renderable> createRenderables(final VectorDataNode node, final List<VectorDataConverter.Shape> converted) {
        final List<Renderable> renderables = new ArrayList<>();
        for (VectorDataConverter.Shape shape : converted) {
            final String info = node.getName() + ": " + shape.featureId;
            switch (shape.kind) {
                case POLYGON: {
                    final SurfacePolygon polygon = new SurfacePolygon(createAttributes(shape.style, true),
                                                                      toLatLons(shape.parts.get(0)));
                    for (int i = 1; i < shape.parts.size(); i++) {
                        polygon.addInnerBoundary(toLatLons(shape.parts.get(i)));
                    }
                    polygon.setPathType(AVKey.LINEAR);
                    polygon.setValue(AVKey.DISPLAY_NAME, info);
                    renderables.add(polygon);
                    break;
                }
                case LINE: {
                    for (List<double[]> part : shape.parts) {
                        final SurfacePolyline line = new SurfacePolyline(createAttributes(shape.style, false),
                                                                         toLatLons(part));
                        line.setPathType(AVKey.LINEAR);
                        line.setValue(AVKey.DISPLAY_NAME, info);
                        renderables.add(line);
                    }
                    break;
                }
                case POINT: {
                    final double[] pos = shape.parts.get(0).get(0);
                    final PointPlacemark placemark = new PointPlacemark(Position.fromDegrees(pos[0], pos[1], 0));
                    placemark.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                    final PointPlacemarkAttributes attributes = new PointPlacemarkAttributes();
                    attributes.setUsePointAsDefaultImage(true);
                    attributes.setLineMaterial(new Material(opaque(shape.style.stroke)));
                    attributes.setScale(6.0);
                    attributes.setImageOffset(new Offset(0.5, 0.5, AVKey.FRACTION, AVKey.FRACTION));
                    placemark.setAttributes(attributes);
                    placemark.setValue(AVKey.DISPLAY_NAME, info);
                    renderables.add(placemark);
                    break;
                }
            }
        }
        return renderables;
    }

    private static ShapeAttributes createAttributes(final VectorDataConverter.Style style, final boolean fill) {
        final ShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setDrawInterior(fill && style.fill.getAlpha() > 0);
        attributes.setInteriorMaterial(new Material(opaque(style.fill)));
        attributes.setInteriorOpacity(style.fill.getAlpha() / 255.0);
        attributes.setDrawOutline(style.stroke.getAlpha() > 0);
        attributes.setOutlineMaterial(new Material(opaque(style.stroke)));
        attributes.setOutlineOpacity(style.stroke.getAlpha() / 255.0);
        attributes.setOutlineWidth(style.strokeWidth);
        attributes.setEnableAntialiasing(true);
        return attributes;
    }

    private static Color opaque(final Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static List<LatLon> toLatLons(final List<double[]> positions) {
        final List<LatLon> latLons = new ArrayList<>(positions.size());
        for (double[] pos : positions) {
            latLons.add(LatLon.fromDegrees(pos[0], pos[1]));
        }
        return latLons;
    }

    private void removeShapes(final VectorDataNode node) {
        final List<Renderable> renderables = shapes.remove(node);
        if (renderables != null) {
            for (Renderable renderable : renderables) {
                removeRenderable(renderable);
            }
        }
    }

    @Override
    public void updateInfoAnnotation(final SelectEvent event) {
        final Object top = event.getTopObject();
        if (event.getEventAction().equals(SelectEvent.ROLLOVER) && isOwnShape(top)) {
            infoAnnotation.setText(String.valueOf(((AVList) top).getValue(AVKey.DISPLAY_NAME)));
            final Point point = event.getPickPoint();
            if (point != null && wwd != null) {
                // screen annotations use a lower-left origin
                infoAnnotation.setScreenPoint(new Point(point.x + 12, wwd.getHeight() - point.y + 12));
            }
            infoAnnotation.getAttributes().setVisible(true);
        } else if (event.getEventAction().equals(SelectEvent.ROLLOVER)) {
            infoAnnotation.getAttributes().setVisible(false);
        }
    }

    private boolean isOwnShape(final Object object) {
        if (!(object instanceof AVList)) {
            return false;
        }
        for (List<Renderable> renderables : shapes.values()) {
            if (renderables.contains(object)) {
                return true;
            }
        }
        return false;
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

    List<Renderable> getShapes(final VectorDataNode node) {
        return shapes.get(node);
    }

    int getNumNodesShown() {
        return shapes.size();
    }

    /**
     * Waits until all queued conversions have been installed on the EDT. For tests.
     */
    boolean awaitPendingConversions(final long timeoutMillis) throws Exception {
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
