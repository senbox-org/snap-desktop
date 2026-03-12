package org.esa.snap.rcp.spectrallibrary.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class BandSelectionUtilsTest {


    @Test
    @STTM("SNAP-4128")
    public void test_nullProduct_returnsEmptyList() {
        assertEquals(List.of(), BandSelectionUtils.getSpectralBands(null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_productWithNoBands_returnsEmptyList() {
        Product p = mock(Product.class);
        when(p.getBands()).thenReturn(new Band[0]);

        assertTrue(BandSelectionUtils.getSpectralBands(p).isEmpty());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_nullBandsAreIgnored() {
        Product p = mock(Product.class);
        Band valid = band(false, 500f);
        when(p.getBands()).thenReturn(new Band[]{null, valid});

        List<Band> out = BandSelectionUtils.getSpectralBands(p);

        assertEquals(1, out.size());
        assertSame(valid, out.get(0));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_flagBandsAreFilteredOut() {
        Product p = mock(Product.class);
        Band flag = band(true, 500f);
        Band valid = band(false, 600f);
        when(p.getBands()).thenReturn(new Band[]{flag, valid});

        List<Band> out = BandSelectionUtils.getSpectralBands(p);

        assertEquals(1, out.size());
        assertSame(valid, out.get(0));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_wavelengthZeroOrNegativeAreFilteredOut() {
        Product p = mock(Product.class);
        Band zero = band(false, 0f);
        Band negative = band(false, -10f);
        Band valid = band(false, 1f);
        when(p.getBands()).thenReturn(new Band[]{zero, negative, valid});

        List<Band> out = BandSelectionUtils.getSpectralBands(p);

        assertEquals(1, out.size());
        assertSame(valid, out.get(0));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_alidBandsAreReturnedSortedAscendingByWavelength() {
        Product p = mock(Product.class);
        Band b700 = band(false, 700f);
        Band b400 = band(false, 400f);
        Band b500 = band(false, 500f);
        when(p.getBands()).thenReturn(new Band[]{b700, b400, b500});

        List<Band> out = BandSelectionUtils.getSpectralBands(p);

        assertEquals(3, out.size());
        assertSame(b400, out.get(0));
        assertSame(b500, out.get(1));
        assertSame(b700, out.get(2));
        assertTrue(out.get(0).getSpectralWavelength() <= out.get(1).getSpectralWavelength());
        assertTrue(out.get(1).getSpectralWavelength() <= out.get(2).getSpectralWavelength());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_duplicateWavelengths_areBothKept() {
        Product p = mock(Product.class);
        Band a = band(false, 500f);
        Band b = band(false, 500f);
        when(p.getBands()).thenReturn(new Band[]{a, b});

        List<Band> out = BandSelectionUtils.getSpectralBands(p);

        assertEquals(2, out.size());
        assertTrue(out.contains(a));
        assertTrue(out.contains(b));
    }

    private static Band band(boolean isFlag, float wl) {
        Band b = mock(Band.class);
        when(b.isFlagBand()).thenReturn(isFlag);
        when(b.getSpectralWavelength()).thenReturn(wl);
        return b;
    }
}