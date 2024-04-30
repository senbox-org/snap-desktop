package org.esa.snap.rcp.spectrum;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SpectraExportActionTest {

    @Test
    @STTM("SNAP-3586")
    public void testGetEnergies() {
        final Band[] bands = new Band[3];
        bands[0] = new Band("one", ProductData.TYPE_INT32, 5, 5);
        bands[1] = new Band("two", ProductData.TYPE_INT32, 5, 5);
        bands[2] = new Band("three", ProductData.TYPE_INT32, 5, 5);

        final Map<Band, Double> bandEnergyMap = new HashMap<>();
        bandEnergyMap.put(bands[0], 0.0);
        bandEnergyMap.put(bands[1], 1.0);
        bandEnergyMap.put(bands[2], 2.0);

        final double[] energies = SpectraExportAction.getEnergies(bands, bandEnergyMap);
        assertEquals(0.0, energies[0], 1e-8);
        assertEquals(1.0, energies[1], 1e-8);
        assertEquals(2.0, energies[2], 1e-8);
    }
}
