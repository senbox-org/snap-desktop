package org.esa.snap.worldwind.model;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Vertex limits and coordinate reference systems of the vector data conversion.
 */
public class VectorDataLimitsTest {

    private static final GeometryFactory GF = new GeometryFactory();

    private static Product createMapProduct(final double pixelSize) throws Exception {
        final Product product = new Product("map", "TEST", 1000, 1000);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 1000, 1000, 10.0, 50.0,
                                                   pixelSize, pixelSize, 0.0, 0.0));
        return product;
    }

    private static VectorDataNode node(final Product product, final CoordinateReferenceSystem crs, final Geometry geometry) {
        final SimpleFeatureType type = PlainFeatureFactory.createDefaultFeatureType(crs);
        final VectorDataNode node = new VectorDataNode("v", type);
        node.getFeatureCollection().add(PlainFeatureFactory.createPlainFeature(type, "f", geometry, null));
        product.getVectorDataGroup().add(node);
        return node;
    }

    private static int vertices(final List<VectorDataConverter.Shape> shapes) {
        int count = 0;
        for (VectorDataConverter.Shape shape : shapes) {
            for (List<double[]> part : shape.parts) {
                count += part.size();
            }
        }
        return count;
    }

    @Test
    public void testLimitAppliesInsideOneFeature() throws Exception {
        final Product product = createMapProduct(0.001);
        final Coordinate[] coordinates = new Coordinate[200];
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = new Coordinate(10.0 + i * 0.001, 49.9);
        }
        final VectorDataNode node = node(product, product.getSceneCRS(), GF.createLineString(coordinates));

        final VectorDataConverter converter = new VectorDataConverter(product, 50);
        final List<VectorDataConverter.Shape> shapes = converter.convert(node);

        assertTrue(converter.isTruncated());
        assertTrue(vertices(shapes) <= 50);
        assertEquals(50, converter.getVertexCount());
    }

    @Test
    public void testDensificationOfAnEdgeIsBounded() throws Exception {
        // 0.0001 degree pixels: an edge of 0.09 degrees is 900 pixels, and an edge longer than the raster still more
        final Product product = createMapProduct(0.0001);
        final VectorDataNode node = node(product, product.getSceneCRS(),
                                         GF.createLineString(new Coordinate[]{new Coordinate(10.0, 49.95),
                                                                              new Coordinate(15.0, 49.95)}));
        final VectorDataConverter converter = new VectorDataConverter(product);
        converter.convert(node);
        // 50000 pixels would have given 6250 intermediate points
        assertTrue(String.valueOf(converter.getVertexCount()),
                   converter.getVertexCount() <= VectorDataConverter.MAX_EDGE_STEPS + 1);
        assertFalse(converter.isTruncated());
    }

    @Test
    public void testFeaturesInAnotherCrsAreTransformed() throws Exception {
        final Product product = createMapProduct(0.001);
        final CoordinateReferenceSystem mercator = CRS.parseWKT(
                "PROJCS[\"WGS 84 / World Mercator\", GEOGCS[\"WGS 84\", DATUM[\"WGS_1984\", " +
                "SPHEROID[\"WGS 84\", 6378137, 298.257223563]], PRIMEM[\"Greenwich\", 0], " +
                "UNIT[\"degree\", 0.017453292519943295]], PROJECTION[\"Mercator_1SP\"], " +
                "PARAMETER[\"central_meridian\", 0], PARAMETER[\"scale_factor\", 1], " +
                "PARAMETER[\"false_easting\", 0], PARAMETER[\"false_northing\", 0], UNIT[\"metre\", 1]]");
        // lon 10.5, lat 49.8 on the WGS 84 ellipsoid
        final double a = 6378137, e = Math.sqrt(1 - Math.pow(1 - 1 / 298.257223563, 2));
        final double phi = Math.toRadians(49.8);
        final double x = a * Math.toRadians(10.5);
        final double y = a * Math.log(Math.tan(Math.PI / 4 + phi / 2) *
                                      Math.pow((1 - e * Math.sin(phi)) / (1 + e * Math.sin(phi)), e / 2));
        final VectorDataNode node = node(product, mercator, GF.createPoint(new Coordinate(x, y)));

        final List<VectorDataConverter.Shape> shapes = new VectorDataConverter(product).convert(node);

        assertEquals(1, shapes.size());
        final double[] position = shapes.get(0).parts.get(0).get(0);
        assertEquals(49.8, position[0], 1e-6);
        assertEquals(10.5, position[1], 1e-6);
    }

    @Test
    public void testFeaturesInAnUntransformableCrsAreNotShown() throws Exception {
        final Product product = createMapProduct(0.001);
        // plain engineering coordinates: no transformation to geographic coordinates exists
        final VectorDataNode node = node(product, DefaultEngineeringCRS.CARTESIAN_2D,
                                         GF.createPoint(new Coordinate(10.5, 49.8)));
        assertTrue(new VectorDataConverter(product).convert(node).isEmpty());
    }
}
