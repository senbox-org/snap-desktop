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
    public void testGeoCodingUsesTiePointGrid() {

        ComponentGeoCoding geoCoding = mock(ComponentGeoCoding.class);
        GeoRaster geoRaster = mock(GeoRaster.class);

        when(geoCoding.getGeoRaster()).thenReturn(geoRaster);
        when(geoRaster.getSubsamplingX()).thenReturn(2.0);
        when(geoRaster.getSubsamplingY()).thenReturn(1.0);
        when(geoRaster.getLonVariableName()).thenReturn("longitude");
        when(geoRaster.getLatVariableName()).thenReturn("latitude");

        assertTrue(GeoCodingUtil.geoCodingUsesTiePointGrid(geoCoding, "longitude"));
        assertFalse(GeoCodingUtil.geoCodingUsesTiePointGrid(geoCoding, "latitude"));
        assertFalse(GeoCodingUtil.geoCodingUsesTiePointGrid(geoCoding, "someOtherName"));
    }
}