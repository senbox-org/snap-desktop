package eu.esa.snap.rcp.bandgroup;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BandGroupsManagerTopComponentTest {

    @Test
    @STTM("SNAP-3709")
    public void testParseTextFieldContent() {
        final String newlineSeparatedList = "aot_222\naot_333\naot_444";
        String[] strings = BandGroupManagerTopComponent.parseTextFieldContent(newlineSeparatedList);
        assertEquals(3, strings.length); // Nur ein Element im Array
        assertEquals("aot_222#aot_222", strings[0]);

        final String commaSeparatedList = "rad_11, rad_14,rad_16";
        strings = BandGroupManagerTopComponent.parseTextFieldContent(commaSeparatedList);
        assertEquals(3, strings.length);
        assertEquals("rad_14#rad_14", strings[1]);

        final String mixedSeparatorList = "ref_11,ref_08\n ref_09";
        strings = BandGroupManagerTopComponent.parseTextFieldContent(mixedSeparatorList);
        assertEquals(3, strings.length);
        assertEquals("ref_09#ref_09", strings[2]);
    }

    @Test
    @STTM("SNAP-3709")
    public void testParseTextFieldContentIsTrimmed() {
        final String mixedSeparatorList = " ref_11,ref_08\n ref_09\n  ";
        final String[] strings = BandGroupManagerTopComponent.parseTextFieldContent(mixedSeparatorList);
        assertEquals(3, strings.length);
        assertEquals("ref_09#ref_09", strings[2]);
    }
}
