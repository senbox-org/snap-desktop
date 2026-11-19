package org.esa.snap.worldwind.productlibrary;

import org.junit.Test;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FootprintUtilsTest {

    private static Path2D.Double polygon(double... lonLat) {
        final Path2D.Double path = new Path2D.Double();
        path.moveTo(lonLat[0], lonLat[1]);
        for (int i = 2; i < lonLat.length; i += 2) {
            path.lineTo(lonLat[i], lonLat[i + 1]);
        }
        path.closePath();
        return path;
    }

    // footprint from lon 178 to -178 (4 degrees wide), lat 10 to 12
    private static final double[] CROSSING = {178, 10, -178, 10, -178, 12, 178, 12};
    private static final double[] NORMAL = {10, 40, 12, 40, 12, 42, 10, 42};

    @Test
    public void testCrossesAntimeridian() {
        assertTrue(FootprintUtils.crossesAntimeridian(polygon(CROSSING)));
        assertFalse(FootprintUtils.crossesAntimeridian(polygon(NORMAL)));
    }

    @Test
    public void testContainsAcrossTheAntimeridian() {
        final Path2D.Double crossing = polygon(CROSSING);
        assertTrue(FootprintUtils.contains(crossing, 179.0, 11.0));
        assertTrue(FootprintUtils.contains(crossing, -179.0, 11.0));
        // previously the planar shape spanned the whole map, so the opposite side of the globe was "inside"
        assertFalse(FootprintUtils.contains(crossing, 0.0, 11.0));
        assertFalse(FootprintUtils.contains(crossing, 179.0, 20.0));
    }

    @Test
    public void testContainsNormalFootprint() {
        final Path2D.Double normal = polygon(NORMAL);
        assertTrue(FootprintUtils.contains(normal, 11.0, 41.0));
        assertFalse(FootprintUtils.contains(normal, 13.0, 41.0));
        assertSame(normal, FootprintUtils.unwrap(normal));
    }

    @Test
    public void testCentre() {
        final Point2D.Double crossingCentre = FootprintUtils.getCentre(polygon(CROSSING));
        assertEquals(180.0, Math.abs(crossingCentre.getX()), 1e-9);
        assertEquals(11.0, crossingCentre.getY(), 1e-9);

        final Point2D.Double normalCentre = FootprintUtils.getCentre(polygon(NORMAL));
        assertEquals(11.0, normalCentre.getX(), 1e-9);
        assertEquals(41.0, normalCentre.getY(), 1e-9);
    }

    @Test
    public void testDrawablePathsOfACrossingFootprintAreSmall() {
        final Path2D.Double[] paths = FootprintUtils.getDrawablePaths(polygon(CROSSING));
        assertEquals(2, paths.length);
        final Rectangle2D east = paths[0].getBounds2D();
        final Rectangle2D west = paths[1].getBounds2D();
        assertEquals(4.0, east.getWidth(), 1e-9);
        assertEquals(178.0, east.getMinX(), 1e-9);
        assertEquals(4.0, west.getWidth(), 1e-9);
        assertEquals(-182.0, west.getMinX(), 1e-9);

        assertEquals(1, FootprintUtils.getDrawablePaths(polygon(NORMAL)).length);
    }
}
