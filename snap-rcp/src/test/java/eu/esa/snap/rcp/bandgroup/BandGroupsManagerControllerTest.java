package eu.esa.snap.rcp.bandgroup;

import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupImpl;
import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import org.junit.Test;

import static org.junit.Assert.*;

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

    @Test
    @STTM("SNAP-3709")
    public void testGroupExists() {
        final BandGroupsManager bandGroupsManager = new BandGroupsManager();
        final BandGroupsManagerController controller = new BandGroupsManagerController(bandGroupsManager);

        assertFalse(controller.groupExists("unknown"));

        bandGroupsManager.add(new BandGroupImpl("yes_its_me", new String[] {"martha", "agatha"}));
        assertFalse(controller.groupExists("unknown"));
        assertTrue(controller.groupExists("yes_its_me"));
    }

    @Test
    @STTM("SNAP-3709")
    public void testRemoveGroup_notExisting() {
        final BandGroupsManager bandGroupsManager = new BandGroupsManager();
        bandGroupsManager.add(new BandGroupImpl("Im_in", new String[] {"one", "two"}));
        final BandGroupsManagerController controller = new BandGroupsManagerController(bandGroupsManager);

        controller.removeGroup("unknown_group");

        final BandGroup[] bandGroups = bandGroupsManager.get();
        assertEquals(1, bandGroups.length);
    }

    @Test
    @STTM("SNAP-3709")
    public void testRemoveGroup() {
        final BandGroupsManager bandGroupsManager = new BandGroupsManager();
        bandGroupsManager.add(new BandGroupImpl("to_be_removed", new String[] {"one", "two"}));
        bandGroupsManager.add(new BandGroupImpl("stays_in", new String[] {"three", "four"}));
        final BandGroupsManagerController controller = new BandGroupsManagerController(bandGroupsManager);

        controller.removeGroup("to_be_removed");

        final BandGroup[] bandGroups = bandGroupsManager.get();
        assertEquals(1, bandGroups.length);
        assertEquals("stays_in", bandGroups[0].getName());
    }
}
