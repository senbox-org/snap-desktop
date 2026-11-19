package org.esa.snap.worldwind.model;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProductTileRendererTest {

    /**
     * A 200 x 100 map-projected product from lon 10..12, lat 51..50, whose band holds the given expression.
     */
    private static Band createMapProduct(final String expression, final double max) throws Exception {
        final Product product = new Product("map", "TEST", 200, 100);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 200, 100, 10.0, 51.0,
                                                   0.01, 0.01, 0.0, 0.0));
        return addBand(product, expression, max);
    }

    private static Band addBand(final Product product, final String expression, final double max) {
        final VirtualBand band = new VirtualBand("v", ProductData.TYPE_FLOAT32, product.getSceneRasterWidth(),
                                                 product.getSceneRasterHeight(), expression);
        product.addBand(band);
        band.setImageInfo(new ImageInfo(new ColorPaletteDef(0, max)));
        return band;
    }

    private static final double ANGLE = Math.toRadians(30);

    /**
     * Pixel to geo-position of a rotated swath: 0.01 degrees per pixel, rotated by 30 degrees.
     */
    private static GeoPos swathGeoPos(final double x, final double y) {
        final double lon = 20 + 0.01 * (x * Math.cos(ANGLE) + y * Math.sin(ANGLE));
        final double lat = 50 - 0.01 * (-x * Math.sin(ANGLE) + y * Math.cos(ANGLE));
        return new GeoPos(lat, lon);
    }

    private static Band createSwathProduct(final String expression, final double max) {
        final int w = 200, h = 100, step = 20;
        final Product product = new Product("swath", "TEST", w, h);
        final int gw = w / step + 1, gh = h / step + 1;
        final float[] lats = new float[gw * gh];
        final float[] lons = new float[gw * gh];
        for (int j = 0; j < gh; j++) {
            for (int i = 0; i < gw; i++) {
                final GeoPos pos = swathGeoPos(i * step, j * step);
                lats[j * gw + i] = (float) pos.lat;
                lons[j * gw + i] = (float) pos.lon;
            }
        }
        final TiePointGrid latGrid = new TiePointGrid("lat", gw, gh, 0, 0, step, step, lats);
        final TiePointGrid lonGrid = new TiePointGrid("lon", gw, gh, 0, 0, step, step, lons);
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        return addBand(product, expression, max);
    }

    private static ProductTileRenderer renderer(final Band band) {
        return new ProductTileRenderer(new RasterDataNode[]{band}, band.getImageInfo());
    }

    /**
     * The tile pixel that contains a geo-position.
     */
    private static Color colourAt(final BufferedImage tile, final GeoBounds bounds, final double lat, final double lon) {
        final int x = (int) ((lon - bounds.minLon) / (bounds.maxLon - bounds.minLon) * tile.getWidth());
        final int y = (int) ((bounds.maxLat - lat) / (bounds.maxLat - bounds.minLat) * tile.getHeight());
        return new Color(tile.getRGB(x, y), true);
    }

    private static BufferedImage render(final ProductTileRenderer renderer, final GeoBounds b, final int size) {
        return renderer.render(b.minLat, b.maxLat, b.minLon, b.maxLon, size, size);
    }

    @Test
    public void testMapProductColoursFollowTheColumns() throws Exception {
        final Band band = createMapProduct("X", 200);
        final GeoBounds bounds = new GeoBounds(50, 51, 10, 12);

        final BufferedImage tile = render(renderer(band), bounds, 256);

        assertNotNull(tile);
        // lon 10.5 is column 50, lon 11.5 column 150
        final Color west = colourAt(tile, bounds, 50.5, 10.505);
        final Color east = colourAt(tile, bounds, 50.5, 11.505);
        assertEquals(255, west.getAlpha());
        assertEquals(50.5 / 200 * 255, west.getRed(), 4);
        assertEquals(150.5 / 200 * 255, east.getRed(), 4);
    }

    @Test
    public void testNorthIsUp() throws Exception {
        // raster line 0 is the northern edge of the product
        final Band band = createMapProduct("Y", 100);
        final GeoBounds bounds = new GeoBounds(50, 51, 10, 12);

        final BufferedImage tile = render(renderer(band), bounds, 256);

        assertTrue(colourAt(tile, bounds, 50.95, 11).getRed() < 30);
        assertTrue(colourAt(tile, bounds, 50.05, 11).getRed() > 225);
    }

    @Test
    public void testRotatedSwathIsPlacedThroughItsGeoCoding() {
        final Band band = createSwathProduct("X", 200);
        final ProductTileRenderer renderer = renderer(band);
        final GeoBounds bounds = GeoBounds.of(band);
        assertNotNull(bounds);

        final BufferedImage tile = render(renderer, bounds, 512);
        assertNotNull(tile);

        for (double[] pixel : new double[][]{{150.5, 30.5}, {20.5, 80.5}, {100.5, 50.5}}) {
            final GeoPos pos = swathGeoPos(pixel[0], pixel[1]);
            final Color colour = colourAt(tile, bounds, pos.lat, pos.lon);
            assertEquals("pixel " + pixel[0], 255, colour.getAlpha());
            assertEquals("pixel " + pixel[0], pixel[0] / 200 * 255, colour.getRed(), 8);
        }
        // the bounding box corners are outside the rotated swath
        assertEquals(0, colourAt(tile, bounds, bounds.maxLat - 0.001, bounds.minLon + 0.001).getAlpha());
    }

    @Test
    public void testTileOutsideTheProduct() throws Exception {
        final ProductTileRenderer renderer = renderer(createMapProduct("X", 200));

        assertNull(renderer.render(0, 1, 0, 1, 64, 64));
    }

    @Test
    public void testTileOverlappingTheProductEdgeIsTransparentOutside() throws Exception {
        final ProductTileRenderer renderer = renderer(createMapProduct("X", 200));
        final GeoBounds bounds = new GeoBounds(50, 51, 11, 13);

        final BufferedImage tile = render(renderer, bounds, 128);

        assertNotNull(tile);
        assertEquals(255, colourAt(tile, bounds, 50.5, 11.9).getAlpha());
        assertEquals(0, colourAt(tile, bounds, 50.5, 12.1).getAlpha());
    }

    @Test
    public void testSwathEdgeCellsAreComputedExactly() {
        // a tile mostly outside a swath: grid cells with a node outside the geo-coding are inverted per pixel,
        // so the swath is not cut back to whole grid cells
        final Band band = createSwathProduct("X", 200);
        final ProductTileRenderer renderer = renderer(band);
        final GeoPos inside = swathGeoPos(1.5, 50.5);
        final GeoBounds bounds = new GeoBounds(inside.lat - 0.2, inside.lat + 0.2, inside.lon - 0.2, inside.lon + 0.2);

        final BufferedImage tile = render(renderer, bounds, 256);

        assertNotNull(tile);
        assertEquals(255, colourAt(tile, bounds, inside.lat, inside.lon).getAlpha());
    }

    @Test
    public void testNoDataIsTransparent() throws Exception {
        final Band band = createMapProduct("X < 100 ? NaN : X", 200);
        band.setNoDataValueUsed(true);
        band.setNoDataValue(Double.NaN);
        final GeoBounds bounds = new GeoBounds(50, 51, 10, 12);

        final BufferedImage tile = render(renderer(band), bounds, 128);

        assertEquals(0, colourAt(tile, bounds, 50.5, 10.5).getAlpha());
        assertEquals(255, colourAt(tile, bounds, 50.5, 11.5).getAlpha());
    }

    @Test
    public void testCoarseTilesUseCoarserPyramidLevels() {
        assertEquals(0, ProductTileRenderer.levelForRatio(0.5, 5));
        assertEquals(0, ProductTileRenderer.levelForRatio(1.9, 5));
        assertEquals(1, ProductTileRenderer.levelForRatio(2, 5));
        assertEquals(3, ProductTileRenderer.levelForRatio(9, 5));
        assertEquals(4, ProductTileRenderer.levelForRatio(1000, 5));
        assertEquals(0, ProductTileRenderer.levelForRatio(Double.NaN, 5));
    }

    @Test
    public void testLargeProductRendersFromACoarseLevel() throws Exception {
        final Product product = new Product("big", "TEST", 8000, 8000);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 8000, 8000, 10.0, 51.0,
                                                   0.0001, 0.0001, 0.0, 0.0));
        final Band band = addBand(product, "X", 8000);
        final ProductTileRenderer renderer = renderer(band);
        assertTrue(renderer.getLevelCount() > 3);

        final GeoBounds bounds = new GeoBounds(50.2, 51, 10, 10.8);
        final BufferedImage tile = render(renderer, bounds, 256);

        assertNotNull(tile);
        assertEquals(4000.0 / 8000 * 255, colourAt(tile, bounds, 50.6, 10.4).getRed(), 4);
    }

    @Test
    public void testNodePositions() {
        assertEquals(1, ProductTileRenderer.nodePositions(1).length);
        final int[] nodes = ProductTileRenderer.nodePositions(512);
        assertEquals(0, nodes[0]);
        assertEquals(16, nodes[1]);
        assertEquals(511, nodes[nodes.length - 1]);
        assertEquals(nodes[nodes.length - 2] + 15, nodes[nodes.length - 1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRasterWithoutGeoCodingIsRejected() {
        final Product product = new Product("p", "TEST", 10, 10);
        final Band band = addBand(product, "X", 10);
        renderer(band);
    }

    @Test
    public void testGeoBounds() throws Exception {
        final Band band = createMapProduct("X", 200);
        final GeoBounds bounds = GeoBounds.of(band);
        assertEquals(50.0, bounds.minLat, 0.011);
        assertEquals(51.0, bounds.maxLat, 0.011);
        assertEquals(10.0, bounds.minLon, 0.011);
        assertEquals(12.0, bounds.maxLon, 0.011);
        // as an arc: 0.01 degrees of longitude at latitude 50.5
        assertEquals(0.01 * Math.cos(Math.toRadians(50.5)), GeoBounds.resolutionDeg(band.getGeoCoding(), 200, 100), 1e-4);

        final GeoBounds crossing = GeoBounds.of(new GeoPos[]{new GeoPos(10, 179), new GeoPos(10, -179),
                new GeoPos(11, -179), new GeoPos(11, 179)});
        assertTrue(crossing.crossesAntimeridian());
    }

    @Test
    public void testPyramidLevels() {
        // level 0: 36 degrees over 512 pixels = 0.0703 deg/pixel; 0.01 deg/pixel needs 3 more levels, plus one
        assertEquals(5, GeoBounds.numLevels(0.01, 36, 512, 22));
        assertEquals(22, GeoBounds.numLevels(1e-9, 36, 512, 22));
        assertEquals(1, GeoBounds.numLevels(10, 36, 512, 22));
        assertEquals(10, GeoBounds.numLevels(Double.NaN, 36, 512, 22));
    }

    @Test
    public void testArgbConversion() {
        assertEquals(0xFF102030, ProductTileRenderer.toArgb(new int[]{0x10, 0x20, 0x30}, 3));
        assertEquals(0x80102030, ProductTileRenderer.toArgb(new int[]{0x10, 0x20, 0x30, 0x80}, 4));
        assertEquals(0xFF404040, ProductTileRenderer.toArgb(new int[]{0x40}, 1));
    }
}
