package org.esa.snap.worldwind.preferences;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorldWindPreferencesTest {

    @Test
    public void testParseLines() {
        assertEquals(List.of("a", "b"), WorldWindPreferences.parseLines(" a \r\n\n b\na\n"));
        assertTrue(WorldWindPreferences.parseLines(null).isEmpty());
        assertTrue(WorldWindPreferences.parseLines("  \n").isEmpty());
    }

    @Test
    public void testUsableCacheDir() throws Exception {
        assertTrue("empty is WorldWind's default", WorldWindPreferences.isUsableCacheDir(" "));
        final java.io.File folder = java.nio.file.Files.createTempDirectory("cache").toFile();
        try {
            assertTrue(WorldWindPreferences.isUsableCacheDir(folder.getPath()));
            assertTrue("created when needed", WorldWindPreferences.isUsableCacheDir(new java.io.File(folder, "a/b").getPath()));
            final java.io.File file = new java.io.File(folder, "file.txt");
            assertTrue(file.createNewFile());
            assertFalse("a file is not a folder", WorldWindPreferences.isUsableCacheDir(file.getPath()));
            file.delete();
        } finally {
            folder.delete();
        }
    }

    @Test
    public void testDefaultServers() {
        final List<String> servers = WorldWindPreferences.parseLines(WorldWindPreferences.DEFAULT_WMS_SERVERS);
        assertEquals(2, servers.size());
        for (String server : servers) {
            assertTrue(server, server.startsWith("https://"));
        }
    }
}
