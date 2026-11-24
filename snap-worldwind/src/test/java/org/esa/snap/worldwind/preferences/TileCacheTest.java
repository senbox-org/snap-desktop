package org.esa.snap.worldwind.preferences;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TileCacheTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSizeAndClear() throws IOException {
        final File cache = folder.newFolder(WorldWindPreferences.WORLDWIND_DATA_FOLDER);
        final File tiles = new File(cache, "Earth/Bing/5");
        assertTrue(tiles.mkdirs());
        Files.write(new File(tiles, "1.png").toPath(), new byte[1000]);
        Files.write(new File(cache, "x.dds").toPath(), new byte[500]);

        assertEquals(1500, TileCache.sizeOf(cache));

        TileCache.clear(cache);
        assertTrue("the cache folder itself stays", cache.isDirectory());
        assertEquals(0, cache.list().length);
        assertEquals(0, TileCache.sizeOf(cache));
    }

    @Test
    public void testClearRefusesOtherFolders() throws IOException {
        final File other = folder.newFolder("Documents");
        final File file = new File(other, "thesis.docx");
        Files.write(file.toPath(), new byte[10]);
        try {
            TileCache.clear(other);
            fail("only WorldWind cache folders may be cleared");
        } catch (IOException expected) {
            // expected
        }
        assertTrue(file.exists());
        try {
            TileCache.clear(null);
            fail("null is not a cache folder");
        } catch (IOException expected) {
            // expected
        }
    }

    @Test
    public void testSizeOfMissingFolder() {
        assertEquals(0, TileCache.sizeOf(new File(folder.getRoot(), "missing")));
        assertEquals(0, TileCache.sizeOf(null));
    }

    @Test
    public void testFormatSize() {
        assertEquals("1 KB", TileCache.formatSize(1500));
        assertEquals("2.0 MB", TileCache.formatSize(2 * 1024 * 1024));
        assertEquals("1.50 GB", TileCache.formatSize(1610612736L));
    }
}
