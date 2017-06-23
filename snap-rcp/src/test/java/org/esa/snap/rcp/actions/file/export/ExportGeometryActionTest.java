package org.esa.snap.rcp.actions.file.export;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class ExportGeometryActionTest {
    @Test
    public void changeGeometryType() throws Exception {
        SimpleFeatureType defaultFeatureType = PlainFeatureFactory.createDefaultFeatureType();
        assertEquals(Geometry.class, defaultFeatureType.getGeometryDescriptor().getType().getBinding());

        SimpleFeatureType changedFeatureType = ExportGeometryAction.changeGeometryType(defaultFeatureType, Polygon.class);
        assertEquals(Polygon.class, changedFeatureType.getGeometryDescriptor().getType().getBinding());
    }

    @Test
    public void testWritingShapeFile() throws Exception {
        Placemark pin = Placemark.createPointPlacemark(PinDescriptor.getInstance(),
                                                       "name1",
                                                       "label1",
                                                       "",
                                                       new PixelPos(0, 0), null,
                                                       null);

        ArrayList<SimpleFeature> features = new ArrayList<>();
        features.add(pin.getFeature());
        File tempFile = File.createTempFile("test", "shp");
        try {
            ExportGeometryAction.writeEsriShapefile(Point.class, features, tempFile);
            assertTrue(tempFile.exists());
        }catch (Throwable t) {
            fail(String.format("Throwable '%s' not expected", t.getMessage()));
        } finally {
            boolean deleted = tempFile.delete();
            if (!deleted) {
                tempFile.deleteOnExit();
            }
        }
    }
}