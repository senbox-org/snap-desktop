package org.esa.snap.worldwind.model;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.image.ColoredBandImageMultiLevelSource;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FootprintTest {

    private static final double ANGLE = Math.toRadians(30);

    /**
     * A 200 x 100 swath rotated by 30 degrees from lon 20, lat 50 (the corners are about (20, 50), (21.73, 51),
     * (22.23, 50.13), (20.5, 49.13)).
     */
    private static Product createSwath() {
        final int w = 200, h = 100, step = 20;
        final Product product = new Product("swath", "TEST", w, h);
        final int gw = w / step + 1, gh = h / step + 1;
        final float[] lats = new float[gw * gh];
        final float[] lons = new float[gw * gh];
        for (int j = 0; j < gh; j++) {
            for (int i = 0; i < gw; i++) {
                final double x = i * step, y = j * step;
                lons[j * gw + i] = (float) (20 + 0.01 * (x * Math.cos(ANGLE) + y * Math.sin(ANGLE)));
                lats[j * gw + i] = (float) (50 - 0.01 * (-x * Math.sin(ANGLE) + y * Math.cos(ANGLE)));
            }
        }
        final TiePointGrid latGrid = new TiePointGrid("lat", gw, gh, 0, 0, step, step, lats);
        final TiePointGrid lonGrid = new TiePointGrid("lon", gw, gh, 0, 0, step, step, lons);
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        product.addBand(new VirtualBand("v", ProductData.TYPE_FLOAT32, w, h, "X"));
        return product;
    }

    @Test
    public void testTileOutsideASwathCostsNoPixelInversions() {
        final Product product = createSwath();
        final GeoCoding geoCoding = spy(product.getSceneGeoCoding());
        final ProductTileRenderer renderer = new ProductTileRenderer(geoCoding, 200, 100,
                ColoredBandImageMultiLevelSource.create(new RasterDataNode[]{product.getBandAt(0)}, ProgressMonitor.NULL));

        // inside the swath's bounding box, north-west of the swath
        assertNull(renderer.render(50.7, 50.9, 20.0, 20.2, 512, 512));
        // before the footprint, every one of the 262144 tile pixels was inverted
        verify(geoCoding, atMost(0)).getPixelPos(any(GeoPos.class), any(PixelPos.class));

        // a tile on the swath is still rendered
        assertNotNull(renderer.render(49.9, 50.1, 20.9, 21.1, 64, 64));
    }

    @Test
    public void testSwathFootprint() {
        final Footprint footprint = Footprint.of(createSwath().getSceneGeoCoding(), 200, 100);
        assertNotNull(footprint);
        assertTrue("centre", footprint.mayIntersect(49.95, 50.05, 21.0, 21.1));
        assertFalse("north-west of the swath", footprint.mayIntersect(50.7, 50.9, 20.0, 20.2));
        assertFalse("far away", footprint.mayIntersect(10, 11, 100, 101));
        assertTrue("an area containing the whole swath", footprint.mayIntersect(40, 60, 10, 30));
    }

    @Test
    public void testFootprintAcrossTheAntimeridian() throws Exception {
        // lon 179.5 .. 180.5
        final GeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 100, 179.5, 10.0, 0.01, 0.01, 0.0, 0.0);
        final Footprint footprint = Footprint.of(geoCoding, 100, 100);
        assertNotNull(footprint);
        assertTrue("west of 180", footprint.mayIntersect(9.4, 9.6, 179.7, 179.9));
        assertTrue("east of 180, given in negative longitudes", footprint.mayIntersect(9.4, 9.6, -179.9, -179.7));
        assertFalse("elsewhere in the latitude band", footprint.mayIntersect(9.4, 9.6, 0, 10));
    }

    @Test
    public void testNoFootprintAroundAPole() {
        // a polar image: the north pole in the centre, latitude falling with the distance from it
        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.getGeoPos(any(PixelPos.class), any())).thenAnswer(invocation -> {
            final PixelPos p = invocation.getArgument(0);
            final double dx = p.x - 50, dy = p.y - 50;
            return new GeoPos(90 - 0.1 * Math.hypot(dx, dy), Math.toDegrees(Math.atan2(dy, dx)));
        });
        assertNull("the outline goes around the pole", Footprint.of(geoCoding, 100, 100));
    }

    @Test
    public void testNoFootprintWithInvalidBoundary() {
        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.getGeoPos(any(PixelPos.class), any())).thenReturn(new GeoPos(Double.NaN, Double.NaN));
        assertNull(Footprint.of(geoCoding, 100, 100));
    }
}
