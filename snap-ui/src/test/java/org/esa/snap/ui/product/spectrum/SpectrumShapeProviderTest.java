package org.esa.snap.ui.product.spectrum;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class SpectrumShapeProviderTest {

    @Test
    public void testGetValidIndex_WithoutEmptySymbol() throws Exception {
        assertEquals(1, SpectrumShapeProvider.getValidIndex(0, false));
        assertEquals(2, SpectrumShapeProvider.getValidIndex(1, false));
        assertEquals(3, SpectrumShapeProvider.getValidIndex(2, false));
        assertEquals(4, SpectrumShapeProvider.getValidIndex(3, false));
        assertEquals(5, SpectrumShapeProvider.getValidIndex(4, false));
        assertEquals(6, SpectrumShapeProvider.getValidIndex(5, false));
        assertEquals(7, SpectrumShapeProvider.getValidIndex(6, false));
        assertEquals(8, SpectrumShapeProvider.getValidIndex(7, false));
        assertEquals(9, SpectrumShapeProvider.getValidIndex(8, false));
        assertEquals(10, SpectrumShapeProvider.getValidIndex(9, false));
        assertEquals(1, SpectrumShapeProvider.getValidIndex(10, false));
        assertEquals(2, SpectrumShapeProvider.getValidIndex(11, false));
        assertEquals(3, SpectrumShapeProvider.getValidIndex(12, false));

    }

    @Test
    public void testGetValidIndex_WithEmptySymbol() throws Exception {
        assertEquals(0, SpectrumShapeProvider.getValidIndex(0, true));
        assertEquals(1, SpectrumShapeProvider.getValidIndex(1, true));
        assertEquals(2, SpectrumShapeProvider.getValidIndex(2, true));
        assertEquals(3, SpectrumShapeProvider.getValidIndex(3, true));
        assertEquals(4, SpectrumShapeProvider.getValidIndex(4, true));
        assertEquals(5, SpectrumShapeProvider.getValidIndex(5, true));
        assertEquals(6, SpectrumShapeProvider.getValidIndex(6, true));
        assertEquals(7, SpectrumShapeProvider.getValidIndex(7, true));
        assertEquals(8, SpectrumShapeProvider.getValidIndex(8, true));
        assertEquals(9, SpectrumShapeProvider.getValidIndex(9, true));
        assertEquals(10, SpectrumShapeProvider.getValidIndex(10, true));
        assertEquals(0, SpectrumShapeProvider.getValidIndex(11, true));
        assertEquals(1, SpectrumShapeProvider.getValidIndex(12, true));

    }

}