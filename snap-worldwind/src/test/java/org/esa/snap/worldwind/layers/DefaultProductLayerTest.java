package org.esa.snap.worldwind.layers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultProductLayerTest {

    private static Product createProduct(String name, double westLon, double northLat, double pixelSize) throws Exception {
        final Product product = new Product(name, "TEST", 100, 50);
        // reference pixel (0, 0) is the upper-left corner of the image
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, westLon, northLat,
                                                   pixelSize, pixelSize, 0.0, 0.0));
        product.addBand("b1", ProductData.TYPE_FLOAT32);
        return product;
    }

    private static void onEDT(Runnable runnable) throws Exception {
        SwingUtilities.invokeAndWait(runnable);
    }

    @Test
    public void testImageCornersAreOuterEdgesInSurfaceImageOrder() throws Exception {
        final Product product = createProduct("p", 10.0, 50.0, 0.1);

        final List<LatLon> corners = DefaultProductLayer.getImageCorners(product.getSceneGeoCoding(), 100, 50);

        assertNotNull(corners);
        assertEquals(4, corners.size());
        // lower-left, lower-right, upper-right, upper-left
        assertLatLon(45.0, 10.0, corners.get(0));
        assertLatLon(45.0, 20.0, corners.get(1));
        assertLatLon(50.0, 20.0, corners.get(2));
        assertLatLon(50.0, 10.0, corners.get(3));
    }

    @Test
    public void testImageCornersAcrossTheAntimeridianAreRejected() throws Exception {
        final Product product = createProduct("p", 175.0, 10.0, 0.1);

        assertNull(DefaultProductLayer.getImageCorners(product.getSceneGeoCoding(), 100, 50));
        assertNull(DefaultProductLayer.getImageCorners(null, 100, 50));
    }

    @Test
    public void testCentreAcrossTheAntimeridian() {
        final List<Position> positions = new ArrayList<>();
        positions.add(Position.fromDegrees(10, 179));
        positions.add(Position.fromDegrees(10, -179));
        positions.add(Position.fromDegrees(12, 179));
        positions.add(Position.fromDegrees(12, -179));

        final Position centre = DefaultProductLayer.calculateCentre(positions);

        assertNotNull(centre);
        assertEquals(11.0, centre.getLatitude().getDegrees(), 0.01);
        assertEquals(180.0, Math.abs(centre.getLongitude().getDegrees()), 0.01);
    }

    @Test
    public void testCentreOfEmptyPositionsIsNull() {
        assertNull(DefaultProductLayer.calculateCentre(Collections.emptyList()));
        assertNull(DefaultProductLayer.calculateCentre(Collections.singletonList(
                new Position(Angle.fromDegrees(Double.NaN), Angle.fromDegrees(Double.NaN), 0))));
    }

    @Test
    public void testAddInstallsOutlineAndRemoveClearsIt() throws Exception {
        final DefaultProductLayer layer = new DefaultProductLayer();
        final Product product = createProduct("p", 10.0, 50.0, 0.1);

        onEDT(() -> layer.addProduct(product, null));
        assertTrue(layer.awaitPendingOutlines(10000));

        assertEquals(1, layer.getProductNames().length);
        assertTrue(layer.getRenderableCount() > 0);

        onEDT(() -> layer.removeProduct(product));
        assertEquals(0, layer.getProductNames().length);
        assertEquals(0, layer.getRenderableCount());
    }

    @Test
    public void testRemoveBeforeOutlineIsInstalledLeavesNoGhostOutline() throws Exception {
        final DefaultProductLayer layer = new DefaultProductLayer();
        final Product product = createProduct("p", 10.0, 50.0, 0.1);

        // add and remove in the same EDT task: the outline computed in the background must not be installed
        onEDT(() -> {
            layer.addProduct(product, null);
            layer.removeProduct(product);
        });
        assertTrue(layer.awaitPendingOutlines(10000));

        assertEquals(0, layer.getProductNames().length);
        assertEquals(0, layer.getRenderableCount());
    }

    @Test
    public void testAddingTheSameProductTwiceAddsOneOutline() throws Exception {
        final DefaultProductLayer layer = new DefaultProductLayer();
        final Product product = createProduct("p", 10.0, 50.0, 0.1);

        onEDT(() -> layer.addProduct(product, null));
        onEDT(() -> layer.addProduct(product, null));
        assertTrue(layer.awaitPendingOutlines(10000));
        final int count = layer.getRenderableCount();
        onEDT(() -> layer.addProduct(product, null));
        assertTrue(layer.awaitPendingOutlines(10000));

        assertEquals(1, layer.getProductNames().length);
        assertEquals(count, layer.getRenderableCount());
    }

    @Test
    public void testRenamedProductCanStillBeRemoved() throws Exception {
        final DefaultProductLayer layer = new DefaultProductLayer();
        final Product product = createProduct("p", 10.0, 50.0, 0.1);

        onEDT(() -> layer.addProduct(product, null));
        assertTrue(layer.awaitPendingOutlines(10000));
        product.setName("renamed");
        onEDT(() -> layer.removeProduct(product));

        assertEquals(0, layer.getRenderableCount());
    }

    @Test
    public void testProductVisibilityAffectsOnlyThatProduct() throws Exception {
        final DefaultProductLayer layer = new DefaultProductLayer();
        final Product p1 = createProduct("p1", 10.0, 50.0, 0.1);
        final Product p2 = createProduct("p2", 30.0, 50.0, 0.1);

        onEDT(() -> {
            layer.addProduct(p1, null);
            layer.addProduct(p2, null);
        });
        assertTrue(layer.awaitPendingOutlines(10000));
        final int total = layer.getRenderableCount();
        final String[] names = layer.getProductNames();
        assertEquals(2, names.length);

        onEDT(() -> layer.setProductVisible(names[1], false));
        assertFalse(layer.isProductVisible(names[1]));
        assertTrue(layer.isProductVisible(names[0]));
        assertTrue(layer.isEnabled());
        assertEquals(total / 2, layer.getRenderableCount());

        onEDT(() -> layer.setProductVisible(names[1], true));
        assertEquals(total, layer.getRenderableCount());
    }

    @Test
    public void testWaveProductWithoutMetadataDoesNotBreakTheLayer() throws Exception {
        final DefaultProductLayer layer = new DefaultProductLayer();
        final Product wave = new Product("wave", "ASA_WVW_2P", 10, 10);

        onEDT(() -> {
            layer.addProduct(wave, null);
            layer.setSelectedProduct(wave);
            layer.setSelectedRaster(null);
            layer.getOpacity(layer.getProductNames()[0]);
            layer.setOpacity(0.5);
            layer.removeProduct(wave);
        });
        assertEquals(0, layer.getProductNames().length);
        assertEquals(0, layer.getRenderableCount());
    }

    @Test
    public void testProductWithoutGeoCodingGetsNoLabel() throws Exception {
        final DefaultProductLayer layer = new DefaultProductLayer();
        final Product product = new Product("nogeo", "TEST", 10, 10);
        product.addBand("b1", ProductData.TYPE_FLOAT32);

        onEDT(() -> layer.addProduct(product, null));
        assertTrue(layer.awaitPendingOutlines(10000));

        // nothing is drawn (previously a label was placed at lat 0, lon 0), but the product is known
        assertEquals(1, layer.getProductNames().length);
        assertEquals(0, layer.getRenderableCount());
        onEDT(() -> layer.removeProduct(product));
        assertEquals(0, layer.getProductNames().length);
    }

    @Test
    public void testProductWithBandGeoCodingOnlyGetsAnOutline() throws Exception {
        final DefaultProductLayer layer = new DefaultProductLayer();
        final Product product = new Product("bandgeo", "TEST", 100, 50);
        final Band band = product.addBand("b1", ProductData.TYPE_FLOAT32);
        band.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, 10.0, 50.0, 0.1, 0.1, 0.0, 0.0));

        onEDT(() -> layer.addProduct(product, null));
        assertTrue(layer.awaitPendingOutlines(10000));

        assertTrue(layer.getRenderableCount() > 0);
    }

    private static void assertLatLon(double lat, double lon, LatLon actual) {
        assertEquals(lat, actual.getLatitude().getDegrees(), 1e-6);
        assertEquals(lon, actual.getLongitude().getDegrees(), 1e-6);
    }
}
