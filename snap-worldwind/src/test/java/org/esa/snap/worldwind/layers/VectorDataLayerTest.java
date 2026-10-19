package org.esa.snap.worldwind.layers;

import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfacePolyline;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.SwingUtilities;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VectorDataLayerTest {

    private static final GeometryFactory GF = new GeometryFactory();

    private static Product createProduct() throws Exception {
        final Product product = new Product("p", "TEST", 100, 50);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, 10.0, 50.0,
                                                   0.01, 0.01, 0.0, 0.0));
        return product;
    }

    private static VectorDataNode createNode(final Product product, final String name, final Geometry... geometries) {
        final SimpleFeatureType type = PlainFeatureFactory.createDefaultFeatureType(product.getSceneCRS());
        final VectorDataNode node = new VectorDataNode(name, type);
        for (int i = 0; i < geometries.length; i++) {
            node.getFeatureCollection().add(PlainFeatureFactory.createPlainFeature(type, name + "_" + i, geometries[i], null));
        }
        return node;
    }

    private static Geometry square(final double lon, final double lat) {
        return GF.createPolygon(new Coordinate[]{new Coordinate(lon, lat), new Coordinate(lon + 0.1, lat),
                new Coordinate(lon + 0.1, lat - 0.1), new Coordinate(lon, lat - 0.1), new Coordinate(lon, lat)});
    }

    private static void onEDT(final Runnable runnable) throws Exception {
        SwingUtilities.invokeAndWait(runnable);
    }

    @Test
    public void testABurstOfFeatureEventsConvertsTheNodeOnce() throws Exception {
        final VectorDataLayer layer = new VectorDataLayer();
        final Product product = createProduct();
        final VectorDataNode node = createNode(product, "burst", square(10.0, 50.0));
        product.getVectorDataGroup().add(node);
        onEDT(() -> layer.addProduct(product, null));
        assertTrue(layer.awaitPendingConversions(10000));
        final int before = layer.getConversionCount();

        // features added one by one, as an import does: one event each
        onEDT(() -> {
            for (int i = 0; i < 200; i++) {
                node.getFeatureCollection().add(PlainFeatureFactory.createPlainFeature(
                        node.getFeatureType(), "f" + i, square(10.0 + (i % 10) * 0.08, 49.9 - (i / 10) * 0.02), null));
            }
        });
        assertTrue(layer.awaitPendingConversions(30000));

        assertEquals(201, layer.getShapes(node).size());
        // before, every event converted the whole node again: 200 conversions of up to 200 features
        final int conversions = layer.getConversionCount() - before;
        assertTrue("conversions: " + conversions, conversions <= 3);
    }

    @Test
    public void testNodesAreShownAndFollowChanges() throws Exception {
        final VectorDataLayer layer = new VectorDataLayer();
        final Product product = createProduct();
        final VectorDataNode node = createNode(product, "shapes", square(10.2, 49.9),
                GF.createLineString(new Coordinate[]{new Coordinate(10.1, 49.6), new Coordinate(10.9, 49.6)}),
                GF.createPoint(new Coordinate(10.5, 49.75)));
        product.getVectorDataGroup().add(node);

        onEDT(() -> layer.addProduct(product, null));
        assertTrue(layer.awaitPendingConversions(10000));

        final List<Renderable> shapes = layer.getShapes(node);
        assertNotNull(shapes);
        assertEquals(3, shapes.size());
        assertTrue(shapes.get(0) instanceof SurfacePolygon);
        assertTrue(shapes.get(1) instanceof SurfacePolyline);
        assertTrue(shapes.get(2) instanceof PointPlacemark);

        // a feature added in SNAP
        onEDT(() -> node.getFeatureCollection().add(PlainFeatureFactory.createPlainFeature(
                node.getFeatureType(), "shapes_3", square(10.6, 49.9), null)));
        assertTrue(layer.awaitPendingConversions(10000));
        assertEquals(4, layer.getShapes(node).size());

        // a node added and removed
        final VectorDataNode other = createNode(product, "other", square(10.0, 50.0));
        onEDT(() -> product.getVectorDataGroup().add(other));
        assertTrue(layer.awaitPendingConversions(10000));
        assertEquals(1, layer.getShapes(other).size());

        onEDT(() -> product.getVectorDataGroup().remove(other));
        assertTrue(layer.awaitPendingConversions(10000));
        assertNull(layer.getShapes(other));
        assertEquals(1, layer.getNumNodesShown());
    }

    @Test
    public void testStyleChangeRedrawsTheNode() throws Exception {
        final VectorDataLayer layer = new VectorDataLayer();
        final Product product = createProduct();
        final VectorDataNode node = createNode(product, "shapes", square(10.2, 49.9));
        product.getVectorDataGroup().add(node);
        onEDT(() -> layer.addProduct(product, null));
        assertTrue(layer.awaitPendingConversions(10000));
        final Renderable before = layer.getShapes(node).get(0);

        onEDT(() -> node.setDefaultStyleCss("fill:#ff0000; fill-opacity:0.7; stroke:#00ff00"));
        assertTrue(layer.awaitPendingConversions(10000));

        final SurfacePolygon after = (SurfacePolygon) layer.getShapes(node).get(0);
        assertTrue(after != before);
        assertEquals(0.7, after.getAttributes().getInteriorOpacity(), 0.01);
        assertEquals(255, after.getAttributes().getInteriorMaterial().getDiffuse().getRed());
    }

    @Test
    public void testRemovingTheProductRemovesItsShapes() throws Exception {
        final VectorDataLayer layer = new VectorDataLayer();
        final Product product = createProduct();
        final VectorDataNode node = createNode(product, "shapes", square(10.2, 49.9));
        product.getVectorDataGroup().add(node);
        final int listenersBefore = product.getProductNodeListeners().length;
        onEDT(() -> layer.addProduct(product, null));
        assertTrue(layer.awaitPendingConversions(10000));
        final int renderablesWithShapes = layer.getNumRenderables();

        onEDT(() -> layer.removeProduct(product));
        assertTrue(layer.awaitPendingConversions(10000));

        assertEquals(0, layer.getNumNodesShown());
        assertEquals(renderablesWithShapes - 1, layer.getNumRenderables());
        assertEquals(listenersBefore, product.getProductNodeListeners().length);
    }

    @Test
    public void testRemovalDuringConversionDiscardsIt() throws Exception {
        final VectorDataLayer layer = new VectorDataLayer();
        final Product product = createProduct();
        product.getVectorDataGroup().add(createNode(product, "shapes", square(10.2, 49.9)));

        onEDT(() -> {
            layer.addProduct(product, null);
            layer.removeProduct(product);
        });
        assertTrue(layer.awaitPendingConversions(10000));

        assertEquals(0, layer.getNumNodesShown());
    }

    @Test
    public void testPinsAndProductsWithoutGeoCodingAreNotShown() throws Exception {
        final VectorDataLayer layer = new VectorDataLayer();
        final Product product = createProduct();
        product.getPinGroup().add(Placemark.createPointPlacemark(PinDescriptor.getInstance(), "pin", "pin", "",
                                                                 new PixelPos(5.5, 5.5), null, product.getSceneGeoCoding()));
        final Product plain = new Product("plain", "TEST", 10, 10);

        onEDT(() -> {
            layer.addProduct(product, null);
            layer.addProduct(plain, null);
        });
        assertTrue(layer.awaitPendingConversions(10000));

        assertEquals(0, layer.getNumNodesShown());
        onEDT(() -> layer.removeProduct(plain));
    }
}
