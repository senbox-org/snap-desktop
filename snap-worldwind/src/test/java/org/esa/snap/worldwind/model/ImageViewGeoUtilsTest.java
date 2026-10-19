package org.esa.snap.worldwind.model;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImageViewGeoUtilsTest {

    /**
     * A geo-coding that, like extrapolated tie-points, returns pixel (50, 50) inside the raster for any position, while
     * that pixel lies at lat 10, lon 20.
     */
    private static GeoCoding extrapolatingGeoCoding() {
        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.canGetPixelPos()).thenReturn(true);
        when(geoCoding.canGetGeoPos()).thenReturn(true);
        when(geoCoding.getPixelPos(any(GeoPos.class), any())).thenReturn(new PixelPos(50, 50));
        when(geoCoding.getGeoPos(any(PixelPos.class), any())).thenReturn(new GeoPos(10, 20));
        return geoCoding;
    }

    @Test
    public void testPositionFarOutsideIsRejectedByTheRoundTrip() {
        // the pixel is inside the raster, so only the round trip can reject the position
        assertNull(ImageViewGeoUtils.toPixel(extrapolatingGeoCoding(), 100, 100, new GeoPos(30, 40)));
    }

    @Test
    public void testPositionThatMapsBackIsAccepted() {
        final PixelPos pixel = ImageViewGeoUtils.toPixel(extrapolatingGeoCoding(), 100, 100, new GeoPos(10.2, 20.3));
        assertNotNull(pixel);
        assertEquals(50, pixel.x, 0);
    }

    @Test
    public void testRoundTripAcrossTheAntimeridian() {
        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.canGetPixelPos()).thenReturn(true);
        when(geoCoding.getPixelPos(any(GeoPos.class), any())).thenReturn(new PixelPos(50, 50));
        when(geoCoding.getGeoPos(any(PixelPos.class), any())).thenReturn(new GeoPos(10, 179.9));
        assertNotNull(ImageViewGeoUtils.toPixel(geoCoding, 100, 100, new GeoPos(10, -179.9)));
    }
}
