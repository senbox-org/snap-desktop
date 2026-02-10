package org.esa.snap.rcp.spectrallibrary.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.speclib.model.SpectralAxis;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;


public class SpectralAxisUtilsTest {


    @Test
    @STTM("SNAP-4128")
    public void test_axisFromBands_sortsAndFiltersInvalid() {
        List<Band> bands = List.of(
                band(700f, "reflectance"),
                band(-1f, "reflectance"),
                band(500f, "reflectance"),
                band(600f, "reflectance")
        );

        SpectralAxis axis = SpectralAxisUtils.axisFromBands(bands);

        assertEquals(3, axis.size());
        assertArrayEquals(new double[]{500, 600, 700}, axis.getWavelengths(), 0.0);
        assertEquals("nm", axis.getXUnit());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_axisFromBands_throwsWhenNoSpectralBands() {
        assertThrows(IllegalArgumentException.class, () ->
                SpectralAxisUtils.axisFromBands(List.of(band(0f, "x"), band(-5f, "x"))));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_defaultYUnitFromBands_returnsUnitIfAllSame() {
        String u = SpectralAxisUtils.defaultYUnitFromBands(List.of(
                band(500f, "reflectance"),
                band(600f, "reflectance")
        ));
        assertEquals("reflectance", u);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_defaultYUnitFromBands_returnsNullIfMixedOrEmpty() {
        assertNull(SpectralAxisUtils.defaultYUnitFromBands(List.of(
                band(500f, "reflectance"),
                band(600f, "radiance")
        )));
        assertNull(SpectralAxisUtils.defaultYUnitFromBands(List.of()));
    }

    private static Band band(float wlNm, String unit) {
        return new Band("b" + wlNm, ProductData.TYPE_FLOAT32, 1, 1) {
            @Override
            public float getSpectralWavelength() { return wlNm; }
            @Override
            public String getUnit() { return unit; }
        };
    }
}