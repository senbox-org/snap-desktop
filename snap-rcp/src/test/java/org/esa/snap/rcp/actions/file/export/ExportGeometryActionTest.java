package org.esa.snap.rcp.actions.file.export;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

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

}