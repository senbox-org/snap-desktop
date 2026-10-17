package org.esa.snap.worldwind.model;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.Color;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VectorDataConverterTest {

    private static final GeometryFactory GF = new GeometryFactory();

    /**
     * 100 x 50 pixels of 0.01 degrees from lon 10, lat 50: model coordinates are lon/lat.
     */
    private static Product createMapProduct() throws Exception {
        final Product product = new Product("map", "TEST", 100, 50);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 50, 10.0, 50.0,
                                                   0.01, 0.01, 0.0, 0.0));
        return product;
    }

    /**
     * A non-map-projected product: model coordinates are pixels; lon = 20 + 0.01 x + 0.002 y, lat = 40 - 0.01 y.
     */
    private static Product createSwathProduct() {
        final int w = 100, h = 50, step = 10;
        final Product product = new Product("swath", "TEST", w, h);
        final int gw = w / step + 1, gh = h / step + 1;
        final float[] lats = new float[gw * gh], lons = new float[gw * gh];
        for (int j = 0; j < gh; j++) {
            for (int i = 0; i < gw; i++) {
                lons[j * gw + i] = (float) (20 + 0.01 * i * step + 0.002 * j * step);
                lats[j * gw + i] = (float) (40 - 0.01 * j * step);
            }
        }
        final TiePointGrid latGrid = new TiePointGrid("lat", gw, gh, 0, 0, step, step, lats);
        final TiePointGrid lonGrid = new TiePointGrid("lon", gw, gh, 0, 0, step, step, lons);
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        return product;
    }

    private static VectorDataNode addNode(final Product product, final String name, final String css,
                                          final Geometry... geometries) {
        final SimpleFeatureType type = PlainFeatureFactory.createDefaultFeatureType(product.getSceneCRS());
        final VectorDataNode node = new VectorDataNode(name, type);
        for (int i = 0; i < geometries.length; i++) {
            node.getFeatureCollection().add(PlainFeatureFactory.createPlainFeature(type, name + "_" + i, geometries[i], css));
        }
        product.getVectorDataGroup().add(node);
        return node;
    }

    private static LinearRing ring(final double... xy) {
        final Coordinate[] coordinates = new Coordinate[xy.length / 2 + 1];
        for (int i = 0; i < xy.length / 2; i++) {
            coordinates[i] = new Coordinate(xy[2 * i], xy[2 * i + 1]);
        }
        coordinates[coordinates.length - 1] = coordinates[0];
        return GF.createLinearRing(coordinates);
    }

    private static void assertContains(final List<double[]> positions, final double lat, final double lon) {
        for (double[] pos : positions) {
            if (Math.abs(pos[0] - lat) < 1e-6 && Math.abs(pos[1] - lon) < 1e-6) {
                return;
            }
        }
        throw new AssertionError("no vertex at " + lat + ", " + lon);
    }

    @Test
    public void testPolygonWithHoleLineAndPointOfAMapProduct() throws Exception {
        final Product product = createMapProduct();
        final VectorDataNode node = addNode(product, "geoms", null,
                GF.createPolygon(ring(10.2, 49.9, 10.4, 49.9, 10.4, 49.7, 10.2, 49.7),
                                 new LinearRing[]{ring(10.25, 49.85, 10.3, 49.85, 10.3, 49.8, 10.25, 49.8)}),
                GF.createLineString(new Coordinate[]{new Coordinate(10.1, 49.6), new Coordinate(10.9, 49.6)}),
                GF.createPoint(new Coordinate(10.5, 49.75)));

        final List<VectorDataConverter.Shape> shapes = new VectorDataConverter(product).convert(node);

        assertEquals(3, shapes.size());
        final VectorDataConverter.Shape polygon = shapes.get(0);
        assertEquals(VectorDataConverter.Kind.POLYGON, polygon.kind);
        assertEquals(2, polygon.parts.size());
        assertContains(polygon.parts.get(0), 49.9, 10.2);
        assertContains(polygon.parts.get(0), 49.7, 10.4);
        assertContains(polygon.parts.get(1), 49.8, 10.3);
        // closed
        final List<double[]> shell = polygon.parts.get(0);
        assertEquals(shell.get(0)[0], shell.get(shell.size() - 1)[0], 0.0);
        assertEquals("geoms_0", polygon.featureId);

        final VectorDataConverter.Shape line = shapes.get(1);
        assertEquals(VectorDataConverter.Kind.LINE, line.kind);
        // 80 pixels long: densified to edges of at most 8 pixels
        assertTrue(line.parts.get(0).size() >= 11);
        assertContains(line.parts.get(0), 49.6, 10.5);

        final VectorDataConverter.Shape point = shapes.get(2);
        assertEquals(VectorDataConverter.Kind.POINT, point.kind);
        assertEquals(49.75, point.parts.get(0).get(0)[0], 1e-6);
        assertEquals(10.5, point.parts.get(0).get(0)[1], 1e-6);
    }

    @Test
    public void testSwathGeometriesFollowTheGeoCoding() {
        final Product product = createSwathProduct();
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        // a line along the first image row, in pixel (model) coordinates
        final VectorDataNode node = addNode(product, "line", null,
                GF.createLineString(new Coordinate[]{new Coordinate(0.5, 0.5), new Coordinate(80.5, 40.5)}));

        final List<VectorDataConverter.Shape> shapes = new VectorDataConverter(product).convert(node);

        assertEquals(1, shapes.size());
        final List<double[]> vertices = shapes.get(0).parts.get(0);
        // the middle vertex is where the geo-coding puts pixel (40.5, 20.5)
        final GeoPos middle = geoCoding.getGeoPos(new PixelPos(40.5, 20.5), null);
        assertContains(vertices, middle.lat, middle.lon);
        final GeoPos end = geoCoding.getGeoPos(new PixelPos(80.5, 40.5), null);
        assertContains(vertices, end.lat, end.lon);
    }

    @Test
    public void testMultiGeometriesAreSplitIntoShapes() throws Exception {
        final Product product = createMapProduct();
        final VectorDataNode node = addNode(product, "multi", null,
                GF.createMultiPolygon(new org.locationtech.jts.geom.Polygon[]{
                        GF.createPolygon(ring(10.1, 49.9, 10.2, 49.9, 10.2, 49.8)),
                        GF.createPolygon(ring(10.5, 49.9, 10.6, 49.9, 10.6, 49.8))}));

        final List<VectorDataConverter.Shape> shapes = new VectorDataConverter(product).convert(node);

        assertEquals(2, shapes.size());
        assertEquals("multi_0", shapes.get(1).featureId);
    }

    @Test
    public void testInvalidVerticesSplitLines() {
        // a geo-coding that has no geo-positions right of x = 50
        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.canGetGeoPos()).thenReturn(true);
        when(geoCoding.getGeoPos(any(PixelPos.class), any())).thenAnswer(inv -> {
            final PixelPos p = inv.getArgument(0);
            return p.x > 50 && p.x < 70 ? new GeoPos(Double.NaN, Double.NaN) : new GeoPos(40 - p.y * 0.01, 20 + p.x * 0.01);
        });
        final Product product = new Product("p", "TEST", 100, 50);
        product.setSceneGeoCoding(geoCoding);
        final VectorDataNode node = addNode(product, "line", null,
                GF.createLineString(new Coordinate[]{new Coordinate(0.5, 10.5), new Coordinate(99.5, 10.5)}));

        final List<VectorDataConverter.Shape> shapes = new VectorDataConverter(product).convert(node);

        assertEquals(1, shapes.size());
        // the part across the gap is not drawn to 0, 0
        assertEquals(2, shapes.get(0).parts.size());
        for (List<double[]> part : shapes.get(0).parts) {
            for (double[] pos : part) {
                assertFalse(Double.isNaN(pos[0]));
                assertEquals(40 - 0.105, pos[0], 1e-9);
            }
        }
    }

    @Test
    public void testPinsAndGcpsAndEmptyNodesAreNotShown() throws Exception {
        final Product product = createMapProduct();
        final VectorDataNode empty = addNode(product, "empty", null);
        final VectorDataNode filled = addNode(product, "filled", null, GF.createPoint(new Coordinate(10.5, 49.75)));

        assertFalse(VectorDataConverter.isShown(product, product.getPinGroup().getVectorDataNode()));
        assertFalse(VectorDataConverter.isShown(product, product.getGcpGroup().getVectorDataNode()));
        assertFalse(VectorDataConverter.isShown(product, empty));
        assertTrue(VectorDataConverter.isShown(product, filled));
    }

    @Test
    public void testStyles() {
        final VectorDataConverter.Style style = VectorDataConverter.parseStyle(
                "fill:#ff0000; fill-opacity:0.5; stroke:#0000ff; stroke-width:3.0", null);
        assertEquals(new Color(255, 0, 0, 128), style.fill);
        assertEquals(Color.BLUE, style.stroke);
        assertEquals(3.0, style.strokeWidth, 0.0);

        // the feature's own style wins
        final VectorDataConverter.Style own = VectorDataConverter.parseStyle(
                "fill:#ff0000; stroke:#0000ff", "fill:#00ff00");
        assertEquals(0, own.fill.getRed());
        assertEquals(255, own.fill.getGreen());
        assertEquals(Color.BLUE, own.stroke);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProductWithoutGeoCodingIsRejected() {
        new VectorDataConverter(new Product("p", "TEST", 10, 10));
    }
}
