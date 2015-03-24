package org.esa.snap.nbexec;

import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class PatchTest {

    @Test
    public void testParseOkWin() throws Exception {
        assumeTrue(System.getProperty("os.name").startsWith("Win"));
        Launcher.Patch patch = Launcher.Patch.parse("C:\\Users\\Norman\\Projects\\my-snap-module\\$\\target\\classes");
        assertEquals(Paths.get("C:\\Users\\Norman\\Projects\\my-snap-module"), patch.getDir());
        assertEquals("target\\classes", patch.getSubPath());
    }

    @Test
    public void testParseOkNoneWin() throws Exception {
        assumeTrue(!System.getProperty("os.name").startsWith("Win"));
        Launcher.Patch patch = Launcher.Patch.parse("/home/norman/projects/my-snap-module/$/target/classes");
        assertEquals(Paths.get("/home/norman/projects/my-snap-module"), patch.getDir());
        assertEquals("target/classes", patch.getSubPath());
    }

    @Test
    public void testParseErrorsWin() {
        assumeTrue(System.getProperty("os.name").startsWith("Win"));
        try {
            Launcher.Patch.parse("C:\\Users\\Norman\\Projects\\my-snap-module\\target\\classes");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            Launcher.Patch.parse("C:\\Users\\Norman\\Projects\\my-snap-module\\$\\target\\classes\\$");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testParseErrorsNoneWin() {
        assumeTrue(!System.getProperty("os.name").startsWith("Win"));
        try {
            Launcher.Patch.parse("/home/norman/projects/snap-module/target/classes");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            Launcher.Patch.parse("/home/norman/projects/snap-module/$/target/classes/$");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
}
