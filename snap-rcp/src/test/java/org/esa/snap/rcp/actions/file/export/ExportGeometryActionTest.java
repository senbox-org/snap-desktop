package org.esa.snap.rcp.actions.file.export;

import com.bc.ceres.core.ProgressMonitor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.actions.vector.VectorDataNodeImporter;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.esa.snap.core.datamodel.PlainFeatureFactory.createPlainFeature;
import static org.esa.snap.core.datamodel.PlainFeatureFactory.createPlainFeatureType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Marco Peters
 */
public class ExportGeometryActionTest {

    private static Product product;
    private static File tempDir;

    @BeforeClass
    public static void setUpTestClass() throws Exception {
        product = new Product("world", "myWorld", 20, 10);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 20, 10, -180, 90, 18, 18, 0.0, 0.0));
        File tempTempFile = File.createTempFile("temp", null);
        tempDir = new File(tempTempFile.getParentFile(), "ExportGeometryActionTest");
        tempDir.mkdir();
        tempTempFile.delete();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteTree(tempDir);
    }

    @Test
    public void changeGeometryType() throws Exception {
        SimpleFeatureType defaultFeatureType = PlainFeatureFactory.createDefaultFeatureType();
        assertEquals(Geometry.class, defaultFeatureType.getGeometryDescriptor().getType().getBinding());

        SimpleFeatureType changedFeatureType = ExportGeometryAction.changeGeometryType(defaultFeatureType, Polygon.class);
        assertEquals(Polygon.class, changedFeatureType.getGeometryDescriptor().getType().getBinding());
    }

    @Test
    public void testWritingShapeFile_Pins() throws Exception {
        Placemark pin = Placemark.createPointPlacemark(PinDescriptor.getInstance(),
                                                       "name1",
                                                       "label1",
                                                       "",
                                                       new PixelPos(0, 0), new GeoPos(52.0, 10.0),
                                                       null);

        ArrayList<SimpleFeature> features = new ArrayList<>();
        features.add(pin.getFeature());
        Class<Point> geomType = Point.class;
        doExportImport(features, geomType);
    }

    @Test
    public void testWritingShapeFile_Geometry() throws Exception {
        SimpleFeatureType sft = createPlainFeatureType("Polygon", Geometry.class, DefaultGeographicCRS.WGS84);

        GeometryFactory gf = new GeometryFactory();
        Polygon polygon = gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(0, 1),
                new Coordinate(0, 0),
        }), null);
        SimpleFeature polygonFeature = createPlainFeature(sft, "_1", polygon, "");


        ArrayList<SimpleFeature> features = new ArrayList<>();
        features.add(polygonFeature);
        Class<Polygon> geomType = Polygon.class;
        doExportImport(features, geomType);
    }

    private void doExportImport(ArrayList<SimpleFeature> features, Class<? extends Geometry> geomType) throws IOException {
        File tempFile = File.createTempFile("pins", null, tempDir);
        try {
            ExportGeometryAction.writeEsriShapefile(geomType, features, tempFile);
            assertTrue(tempFile.exists());
            VectorDataNode vectorDataNode = readIn(new File(String.format("%s_%s.shp", tempFile.getAbsolutePath(), geomType.getSimpleName())), product);
            assertEquals(1, vectorDataNode.getFeatureCollection().getCount());
            try (SimpleFeatureIterator readFeatures = vectorDataNode.getFeatureCollection().features()) {
                while (readFeatures.hasNext()) {
                    SimpleFeature next = readFeatures.next();
                    assertNotNull(next.getDefaultGeometry());
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            fail(String.format("Throwable '%s: %s' not expected", t.getClass().getSimpleName(), t.getMessage()));
        } finally {
            boolean deleted = tempFile.delete();
            if (!deleted) {
                tempFile.deleteOnExit();
            }
        }
    }

    private VectorDataNode readIn(File file, Product product) throws IOException, FactoryException, TransformException {
        DefaultFeatureCollection featureCollection = FeatureUtils.loadShapefileForProduct(file,
                                                                                          product,
                                                                                          new DummyFeatureCrsProvider(),
                                                                                          ProgressMonitor.NULL);
        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
        String name = VectorDataNodeImporter.findUniqueVectorDataNodeName(featureCollection.getSchema().getName().getLocalPart(),
                                                                          vectorDataGroup);
        return new VectorDataNode(name, featureCollection);
    }

    private static class DummyFeatureCrsProvider implements FeatureUtils.FeatureCrsProvider {
        @Override
        public CoordinateReferenceSystem getFeatureCrs(Product product) {
            return product.getSceneCRS();
        }

        @Override
        public boolean clipToProductBounds() {
            return true;
        }
    }
}