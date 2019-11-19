package org.esa.snap.ui.product;

import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.esa.snap.core.datamodel.SceneTransformProvider;
import org.esa.snap.core.transform.MathTransform2D;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class SimpleFeaturePointFigureTest {

    private SceneTransformProvider sceneTransformProvider;

    @Test
    public void testScaling() throws Exception {
        SimpleFeatureType type = createShipTrackFeatureType();
        SimpleFeature feature = createFeature(type, 1, 53.1F, 13.2F,  0.5);

        sceneTransformProvider = new SceneTransformProvider() {
            @Override
            public MathTransform2D getModelToSceneTransform() {
                return MathTransform2D.IDENTITY;
            }

            @Override
            public MathTransform2D getSceneToModelTransform() {
                return MathTransform2D.IDENTITY;
            }
        };

        SimpleFeaturePointFigure figure = new SimpleFeaturePointFigure(feature, sceneTransformProvider, new DefaultFigureStyle());
        Coordinate coordinate = figure.getGeometry().getCoordinate();
        assertEquals(13.2F, coordinate.x, 1e-10);
        assertEquals(53.1F, coordinate.y, 1e-10);

        boolean closeTo = figure.isCloseTo(new Point2D.Double(13.2F, 53.1F), new AffineTransform());
        assertEquals(true, closeTo);

    }

    private static SimpleFeatureType createShipTrackFeatureType() {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
        ftb.setCRS(crs);
        ftb.setName("ShipTrack");
        ftb.add("index", Integer.class);
        ftb.add("point", Point.class, crs);
        ftb.add("data", Double.class);
        ftb.setDefaultGeometry("point");
        return ftb.buildFeatureType();
    }

    private static SimpleFeature createFeature(SimpleFeatureType type, int index, float lat, float lon, double data) {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
        GeometryFactory gf = new GeometryFactory();
        fb.add(index);
        fb.add(gf.createPoint(new Coordinate(lon, lat)));
        fb.add(data);
        return fb.buildFeature(Long.toHexString(System.nanoTime()));
    }

}
