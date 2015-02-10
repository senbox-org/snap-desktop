package org.esa.snap.rcp.actions.file;

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
    public void testEmpty() throws Exception {
        List<String> paths = recentPaths.get();
        assertNotNull(paths);
        assertTrue(paths.isEmpty());
    }

    @Test
    public void testLastInIsFirstOut() throws Exception {
        recentPaths.add("a");
        recentPaths.add("b");
        recentPaths.add("c");
        assertEquals(Arrays.asList("c", "b", "a"), recentPaths.get());
    }

    @Test
    public void testEmptyEntriesAreNotAdded() throws Exception {
        recentPaths.add("a");
        recentPaths.add("");
        recentPaths.add("c");
        assertEquals(Arrays.asList("c", "a"), recentPaths.get());
    }

    @Test
    public void testEqualEntriesAreRemoved() throws Exception {
        recentPaths.add("a");
        recentPaths.add("b");
        recentPaths.add("a");
        recentPaths.add("a");
        assertEquals(Arrays.asList("a", "b"), recentPaths.get());
    }
}
