package org.esa.snap.worldwind;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MeasurePanelTest {

    @Test
    public void testFormatLength() {
        assertEquals("850 m", MeasurePanel.formatLength(850.2));
        assertEquals("12.35 km", MeasurePanel.formatLength(12345));
        assertEquals("1235 km", MeasurePanel.formatLength(1234567));
    }

    @Test
    public void testFormatArea() {
        assertEquals("5000 m²", MeasurePanel.formatArea(5000));
        assertEquals("2.50 km²", MeasurePanel.formatArea(2.5e6));
        assertEquals("1500 km²", MeasurePanel.formatArea(1.5e9));
    }
}
