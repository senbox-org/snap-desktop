package eu.esa.snap.rcp.bandgroup;

import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BandGroupsManagerControllerTest {

    @Test
    @STTM("SNAP-3709")
    public void testGetBandGroupNames_empty() {
        final String[] groupNames = BandGroupsManagerController.getBandGroupNames(new BandGroup[0]);
        assertEquals(0, groupNames.length);
    }

    @Test
    @STTM("SNAP-3709")
    public void testGetBandGroupNames_two() {
        final BandGroup[] bandGroups = new BandGroup[2];
        bandGroups[0] = new BandGroupImpl("manny", new String[0]);
        bandGroups[1] = new BandGroupImpl("horst", new String[0]);

        final String[] groupNames = BandGroupsManagerController.getBandGroupNames(bandGroups);
        assertEquals(2, groupNames.length);
        assertEquals("manny", groupNames[0]);
        assertEquals("horst", groupNames[1]);
    }

    @Test
    @STTM("SNAP-3709")
    public void testGetBandGroupNames_two_oneWithoutName() {
        final BandGroup[] bandGroups = new BandGroup[2];
        bandGroups[0] = BandGroup.parse("F*BT_*n:F*exception_*n:" +
                "F*BT_*o:F*exception_*o:" +
                "S*BT_in:S*exception_in:" +
                "S*BT_io:S*exception_io");
        bandGroups[1] = new BandGroupImpl("horst", new String[0]);

        final String[] groupNames = BandGroupsManagerController.getBandGroupNames(bandGroups);
        assertEquals(2, groupNames.length);
        assertEquals("<unnamed>(F*BT_*n ...)", groupNames[0]);
        assertEquals("horst", groupNames[1]);
    }
}
