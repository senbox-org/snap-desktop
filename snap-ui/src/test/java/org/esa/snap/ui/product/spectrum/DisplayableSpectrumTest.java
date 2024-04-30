package org.esa.snap.ui.product.spectrum;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by E1001827 on 21.2.2014.
 */
public class DisplayableSpectrumTest {

    @Test
    public void testNewDisplayableSpectrumIsSetupCorrectly() {
        String spectrumName = "name";
        DisplayableSpectrum displayableSpectrum = new DisplayableSpectrum(spectrumName, 1);

        assertEquals(spectrumName, displayableSpectrum.getName());
        assertEquals(DisplayableSpectrum.NO_UNIT, displayableSpectrum.getUnit());
        assertNull(displayableSpectrum.getLineStyle());
        assertEquals(SpectrumShapeProvider.DEFAULT_SCALE_GRADE, displayableSpectrum.getSymbolSize());
        assertEquals(SpectrumShapeProvider.getScaledShape(1, SpectrumShapeProvider.DEFAULT_SCALE_GRADE),
                displayableSpectrum.getScaledShape());
        assertEquals(1, displayableSpectrum.getSymbolIndex());
        assertTrue(displayableSpectrum.isSelected());
        assertFalse(displayableSpectrum.isRemainingBandsSpectrum());
        assertFalse(displayableSpectrum.hasBands());
        assertEquals(0, displayableSpectrum.getSpectralBands().length);
        assertEquals(0, displayableSpectrum.getSelectedBands().length);
    }

    @Test
    public void testNewDisplayableSpectrumIsSetUpCorrectlyWithBands() {
        String spectrumName = "name";
        SpectrumBand[] spectralBands = new SpectrumBand[2];
        for (int i = 0; i < spectralBands.length; i++) {
            Band band = createBand(i);
            band.setUnit("unit");
            spectralBands[i] = new SpectrumBand(band, true);
        }
        DisplayableSpectrum displayableSpectrum = new DisplayableSpectrum(spectrumName, spectralBands, 1);

        assertEquals(spectrumName, displayableSpectrum.getName());
        assertEquals("unit", displayableSpectrum.getUnit());
        assertTrue(displayableSpectrum.hasBands());
        assertEquals(2, displayableSpectrum.getSpectralBands().length);
        assertEquals(2, displayableSpectrum.getSelectedBands().length);
        assertTrue(displayableSpectrum.isBandSelected(0));
        assertTrue(displayableSpectrum.isBandSelected(1));
    }

    @Test
    public void testBandsAreAddedCorrectlyToDisplayableSpectrum() {
        String spectrumName = "name";
        DisplayableSpectrum displayableSpectrum = new DisplayableSpectrum(spectrumName, 1);
        SpectrumBand[] bands = new SpectrumBand[3];
        for (int i = 0; i < bands.length; i++) {
            Band band = createBand(i);
            band.setUnit("unit" + i);
            bands[i] = new SpectrumBand(band, i % 2 == 0);
            displayableSpectrum.addBand(bands[i]);
        }

        assertEquals(spectrumName, displayableSpectrum.getName());
        assertEquals(DisplayableSpectrum.MIXED_UNITS, displayableSpectrum.getUnit());
        assertTrue(displayableSpectrum.hasBands());
        assertEquals(3, displayableSpectrum.getSpectralBands().length);
        assertEquals(bands[0].getOriginalBand(), displayableSpectrum.getSpectralBands()[0]);
        assertEquals(bands[1].getOriginalBand(), displayableSpectrum.getSpectralBands()[1]);
        assertEquals(bands[2].getOriginalBand(), displayableSpectrum.getSpectralBands()[2]);
        assertEquals(2, displayableSpectrum.getSelectedBands().length);
        assertEquals(bands[0].getOriginalBand(), displayableSpectrum.getSpectralBands()[0]);
        assertEquals(bands[2].getOriginalBand(), displayableSpectrum.getSpectralBands()[2]);
        assertTrue(displayableSpectrum.isBandSelected(0));
        assertFalse(displayableSpectrum.isBandSelected(1));
        assertTrue(displayableSpectrum.isBandSelected(2));
    }

    private Band createBand(int number) {
        return new Band("name" + number, ProductData.TYPE_INT8, 1, 1);
    }
}
