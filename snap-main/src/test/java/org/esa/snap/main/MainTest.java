package org.esa.snap.main;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * Must be called from a NetBeans native platform executable.
 */
public class MainTest {

    public static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    @Test
    public void testAdaptDirArgsOnWindowsOS() throws Exception {
        assumeTrue(System.getProperty("os.name").startsWith("Win"));

        List<String> args1 = Main.adjustUserDirArgs();
        assertEquals(USER_HOME.resolve(".snap").toString(), Main.getArg(args1, "--userdir"));
        assertEquals(USER_HOME.resolve(".snap\\var\\cache").toString(), Main.getArg(args1, "--cachedir"));

        List<String> args2 = Main.adjustUserDirArgs("--userdir", "");
        assertEquals("", Main.getArg(args2, "--userdir"));
        assertEquals("var\\cache", Main.getArg(args2, "--cachedir"));

        List<String> args3 = Main.adjustUserDirArgs("--userdir", USER_HOME.resolve("AppData\\Roaming\\.snap").toString());
        assertEquals(USER_HOME.resolve(".snap").toString(), Main.getArg(args3, "--userdir"));
        assertEquals(USER_HOME.resolve(".snap\\var\\cache").toString(), Main.getArg(args3, "--cachedir"));

        List<String> args4 = Main.adjustUserDirArgs("--userdir", "..\\testuser");
        assertEquals("..\\testuser", Main.getArg(args4, "--userdir"));
        assertEquals("..\\testuser\\var\\cache", Main.getArg(args4, "--cachedir"));

        List<String> args5 = Main.adjustUserDirArgs("--userdir", "..\\testuser", "--cachedir", "..\\testcache");
        assertEquals("..\\testuser", Main.getArg(args5, "--userdir"));
        assertEquals("..\\testcache", Main.getArg(args5, "--cachedir"));

        List<String> args6 = Main.adjustUserDirArgs("--cachedir", "..\\testcache");
        assertEquals(USER_HOME.resolve(".snap").toString(), Main.getArg(args6, "--userdir"));
        assertEquals("..\\testcache", Main.getArg(args6, "--cachedir"));
    }

    @Test
    public void testAdaptDirArgsOnNonWindowsOS() throws Exception {
        assumeFalse(System.getProperty("os.name").startsWith("Win"));

        List<String> args1 = Main.adjustUserDirArgs();
        assertEquals(USER_HOME.resolve(".snap").toString(), Main.getArg(args1, "--userdir"));
        assertEquals(USER_HOME.resolve(".snap/var/cache").toString(), Main.getArg(args1, "--cachedir"));

        List<String> args2 = Main.adjustUserDirArgs("--userdir", "");
        assertEquals("", Main.getArg(args2, "--userdir"));
        assertEquals("var/cache", Main.getArg(args2, "--cachedir"));

        List<String> args3 = Main.adjustUserDirArgs("--userdir", USER_HOME.resolve("AppData/Roaming/.snap").toString());
        assertEquals(USER_HOME.resolve("AppData/Roaming/.snap").toString(), Main.getArg(args3, "--userdir"));
        assertEquals(USER_HOME.resolve("AppData/Roaming/.snap/var/cache").toString(), Main.getArg(args3, "--cachedir"));

        List<String> args4 = Main.adjustUserDirArgs("--userdir", "../testuser");
        assertEquals("../testuser", Main.getArg(args4, "--userdir"));
        assertEquals("../testuser/var/cache", Main.getArg(args4, "--cachedir"));

        List<String> args5 = Main.adjustUserDirArgs("--userdir", "../testuser", "--cachedir", "../testcache");
        assertEquals("../testuser", Main.getArg(args5, "--userdir"));
        assertEquals("../testcache", Main.getArg(args5, "--cachedir"));

        List<String> args6 = Main.adjustUserDirArgs("--cachedir", "../testcache");
        assertEquals(USER_HOME.resolve(".snap").toString(), Main.getArg(args6, "--userdir"));
        assertEquals("../testcache", Main.getArg(args6, "--cachedir"));
    }

    @Test
    public void testMainWithArgs() throws Exception {
        assertNull(System.getProperty("snap.home"));
        assertNull(System.getProperty("snap.userdir"));
        assertNull(System.getProperty("snap.cachedir"));

        String oldVal1 = System.setProperty("netbeans.home", "a/b/c/platform");
        try {
            try {
                Main.main(new String[]{"--branding", "snap", "--locale","en_GB", "--cachedir", "a/b/c/cache"});
            } catch (ClassNotFoundException e) {
                // ok
            }

            assertEquals(Paths.get("a/b/c"), Paths.get(System.getProperty("snap.home")));
            assertEquals(USER_HOME.resolve(".snap"), Paths.get(System.getProperty("snap.userdir")));
            assertEquals(Paths.get("a/b/c/cache"), Paths.get(System.getProperty("snap.cachedir")));

        } finally {
            if (oldVal1 != null) {
                System.setProperty("netbeans.home", oldVal1);
            } else {
                System.clearProperty("netbeans.home");
            }
            System.clearProperty("snap.home");
            System.clearProperty("snap.userdir");
            System.clearProperty("snap.cachedir");
        }
    }

    @Test
    public void testMainWithoutArgs() throws Exception {
        assertNull(System.getProperty("snap.home"));
        assertNull(System.getProperty("snap.userdir"));
        assertNull(System.getProperty("snap.cachedir"));

        String oldVal1 = System.setProperty("netbeans.home", "a/b/c/platform");
        String oldVal2 = System.setProperty("netbeans.user", "e/f/g");

        try {

            try {
                Main.main(new String[0]);
            } catch (ClassNotFoundException e) {
                // ok
            }

            assertEquals(Paths.get("a/b/c"), Paths.get(System.getProperty("snap.home")));
            assertEquals(Paths.get("e/f/g"), Paths.get(System.getProperty("snap.userdir")));
            assertEquals(Paths.get("e/f/g/var/cache"), Paths.get(System.getProperty("snap.cachedir")));
        } finally {
            if (oldVal1 != null) {
                System.setProperty("netbeans.home", oldVal1);
            } else {
                System.clearProperty("netbeans.home");
            }
            if (oldVal2 != null) {
                System.setProperty("netbeans.user", oldVal2);
            } else {
                System.clearProperty("netbeans.user");
            }
            System.clearProperty("snap.home");
            System.clearProperty("snap.userdir");
            System.clearProperty("snap.cachedir");
        }
    }
}
