package org.esa.snap.ui.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeoCodingUtilTest {

    @Test
    @STTM("SNAP-1506")
    public void testTiePointGridsFromGeoCoding() {

        double subSamplingX = 2.0;
        double subSamplingY = 1.0;

        ComponentGeoCoding geoCoding = mock(ComponentGeoCoding.class);
        GeoRaster geoRaster1 = new GeoRaster(new double[] {54.0, 54.5}, new double[] {10.0, 10.4}, "longitude", "latitude", 2, 2, 2, 2, 100, 0, 0, subSamplingX, subSamplingY);
        GeoRaster geoRaster2 = new GeoRaster( new double[] {54.0, 54.5}, new double[] {10.0, 10.4}, "longitude", "latitude", 2, 2, 2, 2, 100, 0, 0, subSamplingX, subSamplingX);
        GeoRaster geoRaster3 = new GeoRaster( new double[] {54.0, 54.5}, new double[] {10.0, 10.4}, "longitude", "latitude", 2, 2, 2, 2, 100, 0, 0, subSamplingY, subSamplingY);

        when(geoCoding.getGeoRaster()).thenReturn(geoRaster1);
        assertArrayEquals(GeoCodingUtil.getTiePointGridsFromGeoCoding(geoCoding), new String[]{"longitude"});

        when(geoCoding.getGeoRaster()).thenReturn(geoRaster2);
        assertArrayEquals(GeoCodingUtil.getTiePointGridsFromGeoCoding(geoCoding), new String[]{"longitude", "latitude"});

        when(geoCoding.getGeoRaster()).thenReturn(geoRaster3);
        assertArrayEquals(GeoCodingUtil.getTiePointGridsFromGeoCoding(geoCoding), new String[0]);

    }
}