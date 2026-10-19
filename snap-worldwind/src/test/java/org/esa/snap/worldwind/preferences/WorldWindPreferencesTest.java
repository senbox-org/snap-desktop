package org.esa.snap.worldwind.preferences;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorldWindPreferencesTest {

    @Test
    public void testParseLines() {
        assertEquals(List.of("a", "b"), WorldWindPreferences.parseLines(" a \r\n\n b\na\n"));
        assertTrue(WorldWindPreferences.parseLines(null).isEmpty());
        assertTrue(WorldWindPreferences.parseLines("  \n").isEmpty());
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
