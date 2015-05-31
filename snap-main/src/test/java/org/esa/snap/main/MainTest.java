package org.esa.snap.main;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Must be called from a NetBeans native platform executable.
 */
public class MainTest {

    @Test
    public void testMain() throws Exception {

        String oldVal1 = System.setProperty("netbeans.home", "a/b/c/platform");
        String oldVal2 = System.setProperty("netbeans.user", "e/f/g");

        try {
            assertNull(System.getProperty("snap.home"));
            assertNull(System.getProperty("snap.userdir"));

            try {
                Main.main(new String[0]);
            } catch (ClassNotFoundException e) {
                // ok
            }

            assertEquals(Paths.get("a/b/c").toString(), System.getProperty("snap.home"));
            assertEquals(Paths.get("e/f/g").toString(), System.getProperty("snap.userdir"));
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
        }
    }
}
