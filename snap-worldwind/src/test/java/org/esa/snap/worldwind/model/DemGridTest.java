package org.esa.snap.worldwind.model;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DemGridTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * A 100 x 50 DEM of 0.01 degree pixels from lon 10 eastwards, whose height is column + 1000 * row.
     */
    private static Product createDem(final double northing, final double pixelSizeY) throws Exception {
        final Product product = new Product("dem", "TEST", 100, 50);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, 10.0, northing,
                                                   0.01, pixelSizeY, 0.0, 0.0));
        final Band band = new VirtualBand("height", ProductData.TYPE_FLOAT32, 100, 50, "floor(X) + 1000 * floor(Y)");
        product.addBand(band);
        return product;
    }

    @Test
    public void testSamples() {
        assertArrayEquals(new int[]{0, 1, 2, 3}, DemGrid.samples(4, 10));
        assertArrayEquals(new int[]{0, 50, 99}, DemGrid.samples(100, 3));
        final int[] samples = DemGrid.samples(3601, 1024);
        assertEquals(1024, samples.length);
        assertEquals(0, samples[0]);
        assertEquals(3600, samples[1023]);
    }

    @Test
    public void testFullResolution() throws Exception {
        final DemGrid grid = DemGrid.fromProduct(createDem(46.0, 0.01), 1000);
        assertNotNull(grid);
        assertEquals(100, grid.width);
        assertEquals(50, grid.height);
        // bounds through the outer pixel centres
        assertEquals(10.005, grid.minLon, 1e-9);
        assertEquals(10.995, grid.maxLon, 1e-9);
        assertEquals(45.505, grid.minLat, 1e-9);
        assertEquals(45.995, grid.maxLat, 1e-9);
        // first row is the north, first column the west
        assertEquals(0, grid.heights[0], 0);
        assertEquals(99, grid.heights[99], 0);
        assertEquals(1000, grid.heights[100], 0);
        assertEquals(49099, grid.heights[grid.heights.length - 1], 0);
    }

    @Test
    public void testSubsampled() throws Exception {
        final DemGrid grid = DemGrid.fromProduct(createDem(46.0, 0.01), 3);
        assertNotNull(grid);
        assertEquals(3, grid.width);
        assertEquals(3, grid.height);
        // the corners are kept, so the bounds do not change
        assertEquals(10.005, grid.minLon, 1e-9);
        assertEquals(45.505, grid.minLat, 1e-9);
        assertEquals(0, grid.heights[0], 0);
        assertEquals(99, grid.heights[2], 0);
        assertEquals(49099, grid.heights[8], 0);
    }

    @Test
    public void testSouthUpIsFlipped() throws Exception {
        // row 0 in the south
        final DemGrid grid = DemGrid.fromProduct(createDem(45.5, -0.01), 1000);
        assertNotNull(grid);
        assertEquals(45.505, grid.minLat, 1e-9);
        assertEquals(45.995, grid.maxLat, 1e-9);
        // the northernmost row is the last source row
        assertEquals(49000, grid.heights[0], 0);
        assertEquals(0, grid.heights[grid.heights.length - 100], 0);
    }

    @Test
    public void testLongitudesBeyond180AreMoved() throws Exception {
        final Product product = new Product("dem", "TEST", 100, 50);
        // a grid in 0..360 longitudes: 190..191 is -170..-169
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, 190.0, 46.0,
                                                   0.01, 0.01, 0.0, 0.0));
        product.addBand(new VirtualBand("height", ProductData.TYPE_FLOAT32, 100, 50, "floor(X) + 1000 * floor(Y)"));
        final DemGrid grid = DemGrid.fromProduct(product, 1000);
        assertNotNull(grid);
        assertEquals(-169.995, grid.minLon, 1e-9);
        assertEquals(-169.005, grid.maxLon, 1e-9);
        // north and west first, as in the source
        assertEquals(0, grid.heights[0], 0);
        assertEquals(99, grid.heights[99], 0);
        assertArrayEquals(new double[]{45.505, 45.995, -169.995, -169.005}, DemGrid.bounds(product), 1e-9);
    }

    @Test
    public void testGridAcrossTheAntimeridianIsNotUsed() throws Exception {
        final Product product = new Product("dem", "TEST", 100, 50);
        // 179.5 .. 180.5
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, 179.5, 46.0,
                                                   0.01, 0.01, 0.0, 0.0));
        product.addBand(new VirtualBand("height", ProductData.TYPE_FLOAT32, 100, 50, "1"));
        assertNull(DemGrid.fromProduct(product, 1000));
        assertNull(DemGrid.bounds(product));
    }

    @Test
    public void testManyStripsInBothDirections() throws Exception {
        // more rows than one strip, north up and south up
        for (double pixelSizeY : new double[]{0.001, -0.001}) {
            final Product product = new Product("dem", "TEST", 10, 600);
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 10, 600, 10.0,
                                                       pixelSizeY > 0 ? 46.0 : 45.4, 0.01, pixelSizeY, 0.0, 0.0));
            product.addBand(new VirtualBand("height", ProductData.TYPE_FLOAT32, 10, 600, "floor(Y)"));
            final DemGrid grid = DemGrid.fromProduct(product, 1000);
            assertNotNull(grid);
            assertEquals(600, grid.height);
            for (int row = 0; row < 600; row++) {
                // output row 0 is the north
                final float expected = pixelSizeY > 0 ? row : 599 - row;
                assertEquals("row " + row, expected, grid.heights[row * 10], 0);
            }
        }
    }

    @Test
    public void testEastToWestIsFlipped() throws Exception {
        final Product product = new Product("dem", "TEST", 100, 50);
        // column 0 in the east
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, 11.0, 46.0,
                                                   -0.01, 0.01, 0.0, 0.0));
        product.addBand(new VirtualBand("height", ProductData.TYPE_FLOAT32, 100, 50, "floor(X) + 1000 * floor(Y)"));
        final DemGrid grid = DemGrid.fromProduct(product, 1000);
        assertNotNull(grid);
        assertEquals(10.005, grid.minLon, 1e-9);
        assertEquals(10.995, grid.maxLon, 1e-9);
        // the westernmost column is the last source column
        assertEquals(99, grid.heights[0], 0);
        assertEquals(0, grid.heights[99], 0);
    }

    @Test
    public void testFloatNoDataValue() throws Exception {
        // 0.1 is not exactly representable as a float
        final Product product = new Product("dem", "TEST", 10, 10);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 10, 10, 10.0, 46.0, 0.01, 0.01, 0.0, 0.0));
        final Band band = new VirtualBand("height", ProductData.TYPE_FLOAT32, 10, 10, "X < 5 ? 0.1 : 7");
        band.setNoDataValue(0.1);
        band.setNoDataValueUsed(true);
        product.addBand(band);
        final DemGrid grid = DemGrid.fromProduct(product, 1000);
        assertNotNull(grid);
        assertTrue(Float.isNaN(grid.heights[0]));
        assertEquals(7, grid.heights[9], 0);
    }

    @Test
    public void testNoDataOnlyIsNotADem() throws Exception {
        final Product product = new Product("dem", "TEST", 10, 10);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 10, 10, 10.0, 46.0, 0.01, 0.01, 0.0, 0.0));
        final Band band = new VirtualBand("height", ProductData.TYPE_FLOAT32, 10, 10, "-9999");
        band.setNoDataValue(-9999);
        band.setNoDataValueUsed(true);
        product.addBand(band);
        assertNull(DemGrid.fromProduct(product, 1000));
    }

    @Test
    public void testNoData() throws Exception {
        final Product product = createDem(46.0, 0.01);
        product.getBandAt(0).setNoDataValue(5);
        product.getBandAt(0).setNoDataValueUsed(true);
        final DemGrid grid = DemGrid.fromProduct(product, 1000);
        assertNotNull(grid);
        assertTrue(Float.isNaN(grid.heights[5]));
        assertEquals(6, grid.heights[6], 0);
    }

    @Test
    public void testNotALatLonGrid() throws Exception {
        final Product product = new Product("swath", "TEST", 100, 50);
        final TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0.5, 0.5, 99, 49, new float[]{46, 46, 45, 45});
        final TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0.5, 0.5, 99, 49, new float[]{10, 11, 10, 11});
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setSceneGeoCoding(new TiePointGeoCoding(lat, lon));
        product.addBand(new VirtualBand("height", ProductData.TYPE_FLOAT32, 100, 50, "1"));
        // tie-point geo-codings report a geographic map CRS too, but are not a regular grid
        assertNull(DemGrid.fromProduct(product, 1000));

        final Product noGeoCoding = new Product("plain", "TEST", 10, 10);
        noGeoCoding.addBand(new VirtualBand("height", ProductData.TYPE_FLOAT32, 10, 10, "1"));
        assertNull(DemGrid.fromProduct(noGeoCoding, 1000));
    }

    @Test
    public void testFindDemFiles() throws Exception {
        final File sub = folder.newFolder("Copernicus 30m Global DEM");
        assertTrue(new File(sub, "b.tif").createNewFile());
        assertTrue(new File(sub, "a.TIFF").createNewFile());
        assertTrue(new File(sub, "readme.txt").createNewFile());
        assertTrue(folder.newFile("c.tif").exists());

        final List<File> all = DemGrid.findDemFiles(folder.getRoot(), 10);
        assertEquals(3, all.size());
        assertEquals(2, DemGrid.findDemFiles(folder.getRoot(), 2).size());
        assertTrue(DemGrid.findDemFiles(new File(folder.getRoot(), "missing"), 10).isEmpty());
    }
}
