package org.esa.snap.main;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Must be called from a NetBeans native platform executable.
 */
public class MainTest {

    @Test
    public void testMainWithArgs() throws Exception {
        assertNull(System.getProperty("snap.home"));

        String oldVal1 = System.setProperty("netbeans.home", "a/b/c/platform");
        try {
            try {
                Main.main(new String[]{"--branding", "snap", "--locale", "en_GB"});
            } catch (ClassNotFoundException e) {
                // ok
            }
            assertEquals(Paths.get("a/b/c"), Paths.get(System.getProperty("snap.home")));
        } finally {
            if (oldVal1 != null) {
                System.setProperty("netbeans.home", oldVal1);
            } else {
                System.clearProperty("netbeans.home");
            }
            System.clearProperty("snap.home");
        }
    }

}
