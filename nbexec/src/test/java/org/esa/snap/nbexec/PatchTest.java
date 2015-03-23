package org.esa.snap.nbexec;

import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class PatchTest {
    @Test
    public void testParseOk1() throws Exception {
        Launcher.Patch patch = Launcher.Patch.parse("C:\\Users\\Norman\\JavaProjects\\senbox\\s3tbx\\$\\target\\classes");
        assertEquals(Paths.get("C:\\Users\\Norman\\JavaProjects\\senbox\\s3tbx"), patch.getDir());
        assertEquals("target\\classes", patch.getSubPath());
    }

    @Test
    public void testParseOk2() throws Exception {
        Launcher.Patch patch = Launcher.Patch.parse("/home/norman/projects/senbox/s3tbx/$/target/classes");
        assertEquals(Paths.get("/home/norman/projects/senbox/s3tbx"), patch.getDir());
        assertEquals("target/classes", patch.getSubPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseError1() {
        Launcher.Patch.parse("C:\\Users\\Norman\\JavaProjects\\senbox\\s3tbx\\target\\classes");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseError2() {
        Launcher.Patch.parse("/home/norman/projects/senbox/s3tbx/target/classes");
    }
}
