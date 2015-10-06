package org.esa.snap.ui.color;

import org.junit.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Norman on 26.03.2015.
 */
public class ColorCodesTest {
    @Test
    public void testNameListOrder() throws Exception {

        List<String> names = ColorCodes.getNames();
        assertEquals(340, names.size());

        assertEquals("Black", names.get(0));
        assertEquals("Night", names.get(1));
        assertEquals("Gunmetal", names.get(2));
        // ...
        assertEquals("Sea Shell", names.get(337));
        assertEquals("Milk White", names.get(338));
        assertEquals("White", names.get(339));
    }

    @Test
    public void testColorMap() throws Exception {

        assertEquals(new Color(0, 0, 0), ColorCodes.getColor("Black"));
        assertEquals(new Color(12, 9, 10), ColorCodes.getColor("Night"));
        assertEquals(new Color(44, 53, 57), ColorCodes.getColor("Gunmetal"));
        // ...
        assertEquals(new Color(255, 245, 238), ColorCodes.getColor("Sea Shell"));
        assertEquals(new Color(254, 252, 255), ColorCodes.getColor("Milk White"));
        assertEquals(new Color(255, 255, 255), ColorCodes.getColor("White"));
    }
}
