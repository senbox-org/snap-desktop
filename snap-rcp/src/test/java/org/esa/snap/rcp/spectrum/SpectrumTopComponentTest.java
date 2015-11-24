package org.esa.snap.rcp.spectrum;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.ui.product.spectrum.DisplayableSpectrum;
import org.esa.snap.ui.product.spectrum.SpectrumBand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @author Tonio Fincke
 */
public class SpectrumTopComponentTest {

    @Test
    public void testCreateSpectraFromUngroupedBands_noUnits() throws Exception {
        final Band band_1 = new Band("cfsvbzt", ProductData.TYPE_INT8, 1, 1);
        final Band band_2 = new Band("cgvg", ProductData.TYPE_INT8, 1, 1);
        final Band band_3 = new Band("hbhn", ProductData.TYPE_INT8, 1, 1);
        final Band band_4 = new Band("nhbjz", ProductData.TYPE_INT8, 1, 1);
        final Band band_5 = new Band("tjbu", ProductData.TYPE_INT8, 1, 1);
        SpectrumBand[] spectrumBands = new SpectrumBand[]{new SpectrumBand(band_1, false),
                new SpectrumBand(band_2, false), new SpectrumBand(band_3, false),
                new SpectrumBand(band_4, false), new SpectrumBand(band_5, false)
        };

        final DisplayableSpectrum[] spectraFromUngroupedBands =
                SpectrumTopComponent.createSpectraFromUngroupedBands(spectrumBands, 1, 0);

        assertNotNull(spectraFromUngroupedBands);
        assertEquals(1, spectraFromUngroupedBands.length);
        final Band[] spectralBands = spectraFromUngroupedBands[0].getSpectralBands();
        assertEquals(DisplayableSpectrum.DEFAULT_SPECTRUM_NAME, spectraFromUngroupedBands[0].getName());
        assertEquals(5, spectralBands.length);
        assertSame(band_1, spectralBands[0]);
        assertSame(band_2, spectralBands[1]);
        assertSame(band_3, spectralBands[2]);
        assertSame(band_4, spectralBands[3]);
        assertSame(band_5, spectralBands[4]);
    }

    @Test
    public void testCreateSpectraFromUngroupedBands() throws Exception {
        final Band band_1 = new Band("cfsvbzt", ProductData.TYPE_INT8, 1, 1);
        band_1.setUnit("dvgf");
        final Band band_2 = new Band("cgvg", ProductData.TYPE_INT8, 1, 1);
        band_2.setUnit("bzhui");
        final Band band_3 = new Band("hbhn", ProductData.TYPE_INT8, 1, 1);
        band_3.setUnit("drstf");
        final Band band_4 = new Band("nhbjz", ProductData.TYPE_INT8, 1, 1);
        band_4.setUnit("dvgf");
        final Band band_5 = new Band("tjbu", ProductData.TYPE_INT8, 1, 1);
        final Band band_6 = new Band("fgzvf", ProductData.TYPE_INT8, 1, 1);
        band_6.setUnit("drstf");
        SpectrumBand[] spectrumBands = new SpectrumBand[]{new SpectrumBand(band_1, false),
                new SpectrumBand(band_2, false), new SpectrumBand(band_3, false),
                new SpectrumBand(band_4, false), new SpectrumBand(band_5, false),
                new SpectrumBand(band_6, false)
        };

        final DisplayableSpectrum[] spectraFromUngroupedBands =
                SpectrumTopComponent.createSpectraFromUngroupedBands(spectrumBands, 1, 0);

        assertNotNull(spectraFromUngroupedBands);
        assertEquals(4, spectraFromUngroupedBands.length);
        assertEquals("Bands measured in dvgf", spectraFromUngroupedBands[0].getName());
        assertEquals(2, spectraFromUngroupedBands[0].getSpectralBands().length);
        assertSame(band_1, spectraFromUngroupedBands[0].getSpectralBands()[0]);
        assertSame(band_4, spectraFromUngroupedBands[0].getSpectralBands()[1]);
        assertEquals("Bands measured in bzhui", spectraFromUngroupedBands[1].getName());
        assertEquals(1, spectraFromUngroupedBands[1].getSpectralBands().length);
        assertSame(band_2, spectraFromUngroupedBands[1].getSpectralBands()[0]);
        assertEquals("Bands measured in drstf", spectraFromUngroupedBands[2].getName());
        assertEquals(2, spectraFromUngroupedBands[2].getSpectralBands().length);
        assertSame(band_3, spectraFromUngroupedBands[2].getSpectralBands()[0]);
        assertSame(band_6, spectraFromUngroupedBands[2].getSpectralBands()[1]);
        assertEquals(DisplayableSpectrum.REMAINING_BANDS_NAME, spectraFromUngroupedBands[3].getName());
        assertEquals(1, spectraFromUngroupedBands[3].getSpectralBands().length);
        assertSame(band_5, spectraFromUngroupedBands[3].getSpectralBands()[0]);
    }
}