package org.esa.snap.rcp.spectrum;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;
import static junit.framework.TestCase.assertEquals;

public class SpectrumGraphTest {

    @Test
    @STTM("SNAP-3586")
    public void testSetEnergies() {
        final double[] energies = {0.34, 0.44, 0.26, 0.126, 0.55};

        final SpectrumGraph spectrumGraph = new SpectrumGraph();
        spectrumGraph.setEnergies(energies);

        assertEquals(0.126, spectrumGraph.getYMin(), 1e-8);
        assertEquals(0.55, spectrumGraph.getYMax(), 1e-8);
    }
}
