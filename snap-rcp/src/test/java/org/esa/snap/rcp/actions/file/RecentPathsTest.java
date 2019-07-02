package org.esa.snap.rcp.actions.file;

import com.bc.ceres.core.runtime.internal.Platform;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Norman
 */
public class RecentPathsTest {

    private RecentPaths recentPaths;

    @Before
    public void setUp() throws Exception {
        Preferences preferences = Preferences.userNodeForPackage(RecentPathsTest.class).node("test");
        preferences.remove("recents");
        recentPaths = new RecentPaths(Preferences.userNodeForPackage(RecentPathsTest.class).node("test"), "recents", false);
    }

    @Test
    public void testEmpty() {
        List<String> paths = recentPaths.get();
        assertNotNull(paths);
        assertTrue(paths.isEmpty());
    }

    @Test
    public void testLastInIsFirstOut() {
        recentPaths.add("a");
        recentPaths.add("b");
        recentPaths.add("c");
        assertEquals(Arrays.asList("c", "b", "a"), recentPaths.get());
    }

    @Test
    public void testEmptyEntriesAreNotAdded() {
        recentPaths.add("a");
        recentPaths.add("");
        recentPaths.add("c");
        assertEquals(Arrays.asList("c", "a"), recentPaths.get());
    }

    @Test
    public void testEntriesWithIllegalCharsAreNotAdded() {
        Assume.assumeTrue(Platform.ID.win.equals(Platform.getCurrentPlatform().getId()));
        recentPaths.add("a");
        recentPaths.add("C:\\Users\\Anton\\AppData\\Local\\Temp\\AERONET:AOD@551(@ESA)-2015-10-10.tif");
        recentPaths.add("c");
        assertEquals(Arrays.asList("c", "a"), recentPaths.get());
    }

    @Test
    public void testEqualEntriesAreRemoved() {
        recentPaths.add("a");
        recentPaths.add("b");
        recentPaths.add("a");
        recentPaths.add("a");
        assertEquals(Arrays.asList("a", "b"), recentPaths.get());
    }
}
