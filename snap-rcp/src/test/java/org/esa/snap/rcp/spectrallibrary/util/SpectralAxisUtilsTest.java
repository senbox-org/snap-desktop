package org.esa.snap.rcp.spectrallibrary.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.speclib.model.SpectralAxis;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class SpectralAxisUtilsTest {


    @Test
    @STTM("SNAP-4128")
    public void test_axisFromBands_sortsAndFiltersInvalid_includingNullAndFlag() {
        Band flag = mock(Band.class);
        when(flag.isFlagBand()).thenReturn(true);
        when(flag.getSpectralWavelength()).thenReturn(550f);

        List<Band> bands = new ArrayList<>();
        bands.add(band(700f, "reflectance"));
        bands.add(null);
        bands.add(band(-1f, "reflectance"));
        bands.add(band(500f, "reflectance"));
        bands.add(flag);
        bands.add(band(600f, "reflectance"));

        SpectralAxis axis = SpectralAxisUtils.axisFromBands(bands);

        assertEquals(3, axis.size());
        assertArrayEquals(new double[]{500, 600, 700}, axis.getWavelengths(), 0.0);
        assertEquals("nm", axis.getXUnit());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_axisFromBands_throwsWhenNoSpectralBands_afterFiltering() {
        Band flag = mock(Band.class);
        when(flag.isFlagBand()).thenReturn(true);
        when(flag.getSpectralWavelength()).thenReturn(500f);

        List<Band> bands = new ArrayList<>();
        bands.add(null);
        bands.add(band(0f, "x"));
        bands.add(band(-5f, "x"));
        bands.add(flag);

        assertThrows(IllegalArgumentException.class, () ->
                SpectralAxisUtils.axisFromBands(bands));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_axisFromBands_throwsOnNullList() {
        assertThrows(NullPointerException.class, () -> SpectralAxisUtils.axisFromBands(null));
    }


    @Test
    @STTM("SNAP-4128")
    public void test_defaultYUnitFromBands_returnsFirstNonBlankUnit_ofValidSpectralBands() {
        Band flag = mock(Band.class);
        when(flag.isFlagBand()).thenReturn(true);
        when(flag.getSpectralWavelength()).thenReturn(600f);
        when(flag.getUnit()).thenReturn("radiance");

        Band nonSpectral = mock(Band.class);
        when(nonSpectral.isFlagBand()).thenReturn(false);
        when(nonSpectral.getSpectralWavelength()).thenReturn(0f);
        when(nonSpectral.getUnit()).thenReturn("reflectance");

        List<Band> bands = new ArrayList<>();
        bands.add(null);
        bands.add(band(500f, "   "));
        bands.add(flag);
        bands.add(nonSpectral);
        bands.add(band(650f, "reflectance"));
        bands.add(band(700f, "radiance"));

        String u = SpectralAxisUtils.defaultYUnitFromBands(bands);

        assertEquals("reflectance", u);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_defaultYUnitFromBands_returnsNullIfNoNonBlankUnitFound() {
        Band flagBlank = mock(Band.class);
        when(flagBlank.isFlagBand()).thenReturn(true);
        when(flagBlank.getSpectralWavelength()).thenReturn(500f);
        when(flagBlank.getUnit()).thenReturn("reflectance");

        List<Band> bands = new ArrayList<>();
        bands.add(null);
        bands.add(band(500f, ""));
        bands.add(band(600f, "   "));
        bands.add(band(-1f, "reflectance"));
        bands.add(flagBlank);

        assertNull(SpectralAxisUtils.defaultYUnitFromBands(bands));
        assertNull(SpectralAxisUtils.defaultYUnitFromBands(List.of()));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_defaultYUnitFromBands_throwsOnNullList() {
        assertThrows(NullPointerException.class, () -> SpectralAxisUtils.defaultYUnitFromBands(null));
    }


    @Test
    @STTM("SNAP-4128")
    public void test_axisFromReferenceSpectralGroup_sortsFiltersAndUniqifies() {
        Band flag = mock(Band.class);
        when(flag.isFlagBand()).thenReturn(true);
        when(flag.getSpectralWavelength()).thenReturn(600f);

        List<Band> bands = new ArrayList<>();
        bands.add(null);
        bands.add(band(-1f, "x"));
        bands.add(flag);
        bands.add(band(700f, "x"));
        bands.add(band(500f, "x"));
        bands.add(band(500f, "x"));
        bands.add(band(600f, "x"));
        bands.add(band(600f, "x"));

        SpectralAxis axis = SpectralAxisUtils.axisFromReferenceSpectralGroup(bands);

        assertEquals(3, axis.size());
        assertArrayEquals(new double[]{500, 600, 700}, axis.getWavelengths(), 0.0);
        assertEquals("nm", axis.getXUnit());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_axisFromReferenceSpectralGroup_throwsWhenNoSpectralBands() {
        Band flag = mock(Band.class);
        when(flag.isFlagBand()).thenReturn(true);
        when(flag.getSpectralWavelength()).thenReturn(500f);

        List<Band> bands = new ArrayList<>();
        bands.add(null);
        bands.add(band(0f, "x"));
        bands.add(band(-5f, "x"));
        bands.add(flag);

        assertThrows(IllegalArgumentException.class, () -> {
                SpectralAxisUtils.axisFromReferenceSpectralGroup(bands);
            }
        );
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