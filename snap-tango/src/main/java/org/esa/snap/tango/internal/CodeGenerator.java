package org.esa.snap.tango.internal;

import com.bc.ceres.core.runtime.internal.IOHelper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Generates the {@link org.esa.snap.tango.TangoIcons} class.
 *
 * @author Norman Fomferra
 */
public class CodeGenerator {
    public static void main(String[] args) throws URISyntaxException, IOException {

        Path mainTemplateFile = Paths.get("src", "main", "resources", "org", "esa", "snap", "tango", "internal", "TangoIcons.vm");
        Path testTemplateFile = Paths.get("src", "main", "resources", "org", "esa", "snap", "tango", "internal", "TangoIconsTest.vm");

        Path mainClassFile = Paths.get("src", "main", "java", "org", "esa", "snap", "tango", "TangoIcons.java");
        Path testClassFile = Paths.get("src", "test", "java", "org", "esa", "snap", "tango", "TangoIconsTest.java");

        String mainTemplate = new String(Files.readAllBytes(mainTemplateFile));
        String testTemplate = new String(Files.readAllBytes(testTemplateFile));

        Files.createDirectories(mainClassFile.getParent());
        Files.createDirectories(testClassFile.getParent());

        final Path dir16 = Paths.get("src", "main", "resources", "tango", "16x16");

        StringBuilder mainCode = new StringBuilder();
        StringBuilder testCode = new StringBuilder();

        final Collection<Path> subDirsList = IOHelper.listFiles(dir16, Files::isDirectory);
        final Collection<Path> subDirs = subDirsList == null ? new ArrayList<>() : subDirsList;
        for (Path subDir : subDirs) {
            //System.out.println("subDir = " + subDir);
            final Collection<Path> iconFilesList =  IOHelper.listFiles(subDir, path-> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".png"));
            final Collection<Path> iconFiles = iconFilesList == null ? new ArrayList<>() : iconFilesList;
            String dirName = subDir.getFileName().toString();
            for (Path iconFile : iconFiles) {
                String fileName = iconFile.getFileName().toString();
                String methodName = dirName.toLowerCase() + "_" + fileName.substring(0, fileName.length() - 4).replace("-", "_");
                mainCode.append(String.format("    public static ImageIcon %s(Res res) { return getIcon(\"%s/%s\", res); }\n", methodName, dirName, fileName));
                testCode.append(String.format("        assertNotNull(TangoIcons.%s(TangoIcons.R16));\n", methodName));
                testCode.append(String.format("        assertNotNull(TangoIcons.%s(TangoIcons.R22));\n", methodName));
                testCode.append(String.format("        assertNotNull(TangoIcons.%s(TangoIcons.R32));\n", methodName));
                testCode.append("\n");
            }
        }

        mainTemplate = mainTemplate.replace("${code}", mainCode.toString());
        testTemplate = testTemplate.replace("${code}", testCode.toString());

        Files.write(mainClassFile, mainTemplate.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Written " + mainClassFile);
        Files.write(testClassFile, testTemplate.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Written " + testClassFile);
    }
}
