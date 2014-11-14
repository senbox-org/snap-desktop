package org.esa.snap.tango;

import java.io.File;
import java.io.FileFilter;
import java.net.URISyntaxException;

/**
 * Generates the {@link TangoIconAccessor} class.
 *
 * @author Norman Fomferra
 */
public class Generator {
    public static void main(String[] args) throws URISyntaxException {
        File dir = new File(Generator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        File dir16 = new File(dir, "org/esa/snap/tango/16x16");

        File[] subDirs = dir16.listFiles(File::isDirectory);
        for (File subDir : subDirs) {
            System.out.println("subDir = " + subDir);
            File[] iconFiles = subDir.listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".png"));
            for (File iconFile : iconFiles) {
                System.out.println("  iconFile = " + iconFile);
            }
        }
    }
}
