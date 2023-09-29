/*
 * Copyright (C) 2014-2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 *  with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.modules;

import org.esa.snap.core.gpf.descriptor.OSFamily;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.dependency.Bundle;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.openide.modules.Modules;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for creating at runtime a jar module
 * for a tool adapter, so that it can be independently deployed.
 *
 * @author Cosmin Cara
 */
public final class ModulePackager {

    private static final Manifest _manifest;
    private static final Attributes.Name ATTR_DESCRIPTION_NAME;
    private static final Attributes.Name ATTR_MODULE_NAME;
    private static final Attributes.Name ATTR_MODULE_TYPE;
    private static final Attributes.Name ATTR_MODULE_IMPLEMENTATION;
    private static final Attributes.Name ATTR_MODULE_SPECIFICATION;
    private static final Attributes.Name ATTR_MODULE_DEPENDENCIES;
    private static final Attributes.Name ATTR_MODULE_ALIAS;
    private static final File modulesPath;
    private static final String layerXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE filesystem PUBLIC \"-//NetBeans//DTD Filesystem 1.1//EN\" \"http://www.netbeans.org/dtds/filesystem-1_1.dtd\">\n" +
            "<filesystem>\n" +
            "    <folder name=\"Actions\">\n" +
            "        <folder name=\"Tools\">\n" +
            "            <file name=\"org-esa-snap-ui-tooladapter-actions-ExecuteToolAdapterAction.instance\"/>\n" +
            "            <attr name=\"displayName\" stringvalue=\"#NAME#\"/>\n" +
            "            <attr name=\"instanceCreate\" methodvalue=\"org.openide.awt.Actions.alwaysEnabled\"/>\n" +
            "        </folder>\n" +
            "    </folder>\n" +
            "    <folder name=\"Menu\">\n" +
            "        <folder name=\"Tools\">\n" +
            "            <folder name=\"External Tools\">\n" +
            "                <file name=\"org-esa-snap-ui-tooladapter-actions-ExecuteToolAdapterAction.shadow\">\n" +
            "                    <attr name=\"originalFile\" stringvalue=\"Actions/Tools/org-esa-snap-ui-tooladapter-actions-ExecuteToolAdapterAction.instance\"/>\n" +
            "                    <attr name=\"position\" intvalue=\"1000\"/>\n" +
            "                </file>\n" +
            "            </folder>\n" +
            "        </folder>\n" +
            "    </folder>\n" +
            "</filesystem>";
    private static final String LAYER_XML_PATH = "org/esa/snap/ui/tooladapter/layer.xml";
    private static final String IMPLEMENTATION_VERSION;
    private static final String SPECIFICATION_VERSION;
    private static final String STA_MODULE = "org.esa.snap.snap.sta";
    private static final String STA_UI_MODULE = "org.esa.snap.snap.sta.ui";
    private static final String SNAP_RCP_MODULE = "org.esa.snap.snap.rcp";
    private static final String SNAP_CORE_MODULE = "org.esa.snap.snap.core";

    static {
        String implementationVersion = Modules.getDefault().ownerOf(ModulePackager.class).getImplementationVersion();
        IMPLEMENTATION_VERSION = implementationVersion.indexOf("-") > 0 ?
                implementationVersion.substring(implementationVersion.indexOf("-") + 1) :
                implementationVersion;
        SPECIFICATION_VERSION = implementationVersion.indexOf("-") > 0 ?
                implementationVersion.substring(0, implementationVersion.indexOf("-")) :
                implementationVersion;
        _manifest = new Manifest();
        Attributes attributes = _manifest.getMainAttributes();
        ATTR_DESCRIPTION_NAME = new Attributes.Name("OpenIDE-Module-Short-Description");
        ATTR_MODULE_NAME = new Attributes.Name("OpenIDE-Module");
        ATTR_MODULE_TYPE = new Attributes.Name("OpenIDE-Module-Type");
        ATTR_MODULE_IMPLEMENTATION = new Attributes.Name("OpenIDE-Module-Implementation-Version");
        ATTR_MODULE_SPECIFICATION = new Attributes.Name("OpenIDE-Module-Specification-Version");
        ATTR_MODULE_ALIAS = new Attributes.Name("OpenIDE-Module-Alias");
        ATTR_MODULE_DEPENDENCIES = new Attributes.Name("OpenIDE-Module-Module-Dependencies");
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(new Attributes.Name("OpenIDE-Module-Java-Dependencies"), "Java > 1.8");
        attributes.put(ATTR_MODULE_DEPENDENCIES, "org.esa.snap.snap.sta, org.esa.snap.snap.sta.ui");
        attributes.put(new Attributes.Name("OpenIDE-Module-Display-Category"), "SNAP");
        attributes.put(ATTR_MODULE_TYPE, "STA");
        attributes.put(ATTR_DESCRIPTION_NAME, "External tool adapter");

        modulesPath = ToolAdapterIO.getAdaptersPath().toFile();
    }
    public static void packModules(ModuleSuiteDescriptor suiteDescriptor, File suiteFile, Map<OSFamily, Bundle> bundles, ToolAdapterOperatorDescriptor... descriptors) throws IOException {
        if (suiteFile != null && descriptors != null && descriptors.length > 0) {
            if (descriptors.length == 1) {
                packModule(descriptors[0], suiteFile);
            } else {
                Path suiteFilePath = suiteFile.toPath();
                if (!Files.isDirectory(suiteFilePath)) {
                    suiteFilePath = suiteFilePath.getParent();
                }
                Map<String, String> dependentModules = new HashMap<>();
                UpdateBuilder updateBuilder = new UpdateBuilder();
                for (ToolAdapterOperatorDescriptor descriptor : descriptors) {
                    updateBuilder.moduleManifest(
                            packModule(descriptor, suiteFilePath.resolve(descriptor.getAlias() + ".nbm").toFile(), true));
                    dependentModules.put(normalize(descriptor.getName()), SPECIFICATION_VERSION);// descriptor.getVersion());
                }
                if (bundles != null) {
                    Arrays.stream(descriptors).forEach(d -> d.setBundles(bundles));
                }
                packSuite(suiteDescriptor, suiteFile, dependentModules, bundles);
                Files.write(suiteFilePath.resolve("updates.xml"), updateBuilder.build(true).getBytes());
            }
        }
    }
    /**
     * Packs the files associated with the given tool adapter operator descriptor into
     * a NetBeans module file (nbm)
     *
     * @param descriptor    The tool adapter descriptor
     * @param nbmFile       The target module file
     */
    public static String packModule(ToolAdapterOperatorDescriptor descriptor, File nbmFile) throws IOException {
        return packModule(descriptor, nbmFile, false);
    }

    private static String packModule(ToolAdapterOperatorDescriptor descriptor, File nbmFile, boolean isPartOfSuite) throws IOException {
        byte[] byteBuffer;
        String manifestXml = null;
        try (final ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(nbmFile))) {
            // create Info section
            ZipEntry entry = new ZipEntry("Info/info.xml");
            zipStream.putNextEntry(entry);
            InfoBuilder infoBuilder = new InfoBuilder();
            String javaVersion = System.getProperty("java.version");
            try {
                javaVersion = javaVersion.substring(0, javaVersion.indexOf("_"));
            }catch(Exception ex){
                javaVersion = javaVersion.substring(0, javaVersion.indexOf("."));
            }
            String descriptorName = normalize(descriptor.getName());
            String description = descriptor.getDescription();

            infoBuilder.moduleName(descriptorName)
                                    .shortDescription(description)
                                    .longDescription(description)
                                    .displayCategory("SNAP")
                                    .specificationVersion(SPECIFICATION_VERSION)
                                    .implementationVersion(SPECIFICATION_VERSION)//descriptor.getVersion())
                                    .codebase(descriptorName.toLowerCase())
                                    .distribution(nbmFile.getName())
                                    .downloadSize(0)
                                    .homePage("https://github.com/senbox-org/s2tbx")
                                    .needsRestart(true)
                                    .releaseDate(new Date())
                                    .isEssentialModule(false)
                                    .showInClient(!isPartOfSuite)
                                    .javaVersion(javaVersion)
                                    .dependency(STA_MODULE, SPECIFICATION_VERSION)
                                    .dependency(STA_UI_MODULE, SPECIFICATION_VERSION)
                                    .dependency(SNAP_RCP_MODULE, SPECIFICATION_VERSION)
                                    .dependency(SNAP_CORE_MODULE, SPECIFICATION_VERSION);
            byteBuffer = infoBuilder.build(true).getBytes();
            manifestXml = infoBuilder.build(false);
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();

            // create META-INF section
            entry = new ZipEntry("META-INF/MANIFEST.MF");
            zipStream.putNextEntry(entry);
            byteBuffer = new ManifestBuilder().build(true).getBytes();
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();

            String jarName = descriptorName.replace(".", "-") + ".jar";

            // create config section
            entry = new ZipEntry("netbeans/config/Modules/" + descriptorName.replace(".", "-") + ".xml");
            zipStream.putNextEntry(entry);
            ModuleConfigBuilder mcb = new ModuleConfigBuilder();
            byteBuffer = mcb.name(descriptorName)
                            .autoLoad(false)
                            .eager(false)
                            .enabled(true)
                            .jarName(jarName)
                            .reloadable(false)
                        .build(true).getBytes();
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();
            // create modules section
            entry = new ZipEntry("netbeans/modules/ext/");
            zipStream.putNextEntry(entry);
            zipStream.closeEntry();
            entry = new ZipEntry("netbeans/modules/" + jarName);
            zipStream.putNextEntry(entry);
            zipStream.write(packAdapterJar(descriptor));
            zipStream.closeEntry();
            Map<OSFamily, Bundle> bundles = descriptor.getBundles();
            if (!isPartOfSuite && bundles != null) {
                for (Bundle bundle : bundles.values()) {
                    if (bundle.isLocal() && bundle.getTargetLocation() != null && bundle.getEntryPoint() != null) {
                        // lib folder
                        entry = new ZipEntry("netbeans/modules/lib/");
                        zipStream.putNextEntry(entry);
                        zipStream.closeEntry();
                        // bundle
                        String entryPoint = bundle.getEntryPoint();
                        File entryPointPath = bundle.getSource();
                        if (entryPointPath.exists()) {
                            entry = new ZipEntry("netbeans/modules/lib/" + entryPoint);
                            zipStream.putNextEntry(entry);
                            zipStream.write(Files.readAllBytes(entryPointPath.toPath()));
                            zipStream.closeEntry();
                        }
                    }
                }
            }
            // create update_tracking section
            entry = new ZipEntry("netbeans/update_tracking/");
            zipStream.putNextEntry(entry);
            zipStream.closeEntry();
        }
        return manifestXml;
    }

    /**
     * Unpacks a jar file into the user modules location.
     *
     * @param jarFile   The jar file to be unpacked
     * @param unpackFolder  The destination folder. If null, then the jar name will be used
     */
    public static void unpackAdapterJar(File jarFile, File unpackFolder) throws IOException {
        JarFile jar = new JarFile(jarFile);
        Enumeration enumEntries = jar.entries();
        if (unpackFolder == null) {
            unpackFolder = new File(modulesPath, jarFile.getName().replace(".jar", ""));
        }
        if (!unpackFolder.exists()) {
            Files.createDirectories(unpackFolder.toPath());
        }
        Attributes attributes = jar.getManifest().getMainAttributes();
        if (attributes.containsKey(ATTR_MODULE_IMPLEMENTATION)) {
            String version = attributes.getValue(ATTR_MODULE_IMPLEMENTATION);
            File versionFile = new File(unpackFolder, "version.txt");
            try (FileOutputStream fos = new FileOutputStream(versionFile)) {
                fos.write(version.getBytes());
                fos.close();
            }
        }
        while (enumEntries.hasMoreElements()) {
            JarEntry file = (JarEntry) enumEntries.nextElement();
            File f = new File(unpackFolder, file.getName());
            if (!f.toPath().normalize().startsWith(unpackFolder.toPath().normalize())) {
                throw new IOException("Bad zip entry");
            }
            if (file.isDirectory()) {
                Files.createDirectories(f.toPath());
                continue;
            } else {
                Files.createDirectories(f.getParentFile().toPath());
            }
            try (InputStream is = jar.getInputStream(file)) {
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    while (is.available() > 0) {
                        fos.write(is.read());
                    }
                    fos.close();
                }
                is.close();
            }
        }
    }

    public static String getAdapterVersion(File jarFile) throws IOException {
        String version = null;
        JarFile jar = new JarFile(jarFile);
        Attributes attributes = jar.getManifest().getMainAttributes();
        if (attributes.containsKey(ATTR_MODULE_IMPLEMENTATION)) {
            version = attributes.getValue(ATTR_MODULE_IMPLEMENTATION);
        }
        jar.close();
        return version;
    }

    public static String getAdapterAlias(File jarFile) throws IOException {
        String version = null;
        JarFile jar = new JarFile(jarFile);
        Attributes attributes = jar.getManifest().getMainAttributes();
        if (attributes.containsKey(ATTR_MODULE_ALIAS)) {
            version = attributes.getValue(ATTR_MODULE_ALIAS);
        }
        jar.close();
        return version;
    }

    private static void packSuite(ModuleSuiteDescriptor descriptor, File nbmFile, Map<String, String> dependencies, Map<OSFamily, Bundle> bundles) throws IOException {
        byte[] byteBuffer;
        try (final ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(nbmFile))) {
            // create Info section
            ZipEntry entry = new ZipEntry("Info/info.xml");
            zipStream.putNextEntry(entry);
            InfoBuilder infoBuilder = new InfoBuilder();
            String javaVersion = System.getProperty("java.version");
            try {
                javaVersion = javaVersion.substring(0, javaVersion.indexOf("_"));
            }catch(Exception ex){
                javaVersion = javaVersion.substring(0, javaVersion.indexOf("."));
            }
            String descriptorName = descriptor.getName();
            String description = descriptor.getDescription();

            infoBuilder.moduleName(descriptorName)
                    .shortDescription(description)
                    .longDescription(description)
                    .displayCategory("SNAP")
                    .specificationVersion(SPECIFICATION_VERSION)
                    .implementationVersion(IMPLEMENTATION_VERSION)
                    .codebase(descriptorName.toLowerCase())
                    .distribution(nbmFile.getName())
                    .downloadSize(0)
                    .homePage("https://github.com/senbox-org/s2tbx")
                    .needsRestart(true)
                    .releaseDate(new Date())
                    .isEssentialModule(false)
                    .showInClient(true)
                    .javaVersion(javaVersion)
                    .dependency(STA_MODULE, SPECIFICATION_VERSION)
                    .dependency(STA_UI_MODULE, SPECIFICATION_VERSION)
                    .dependency(SNAP_RCP_MODULE, SPECIFICATION_VERSION)
                    .dependency(SNAP_CORE_MODULE, SPECIFICATION_VERSION);
            if (dependencies != null) {
                for (Map.Entry<String, String> mapEntry : dependencies.entrySet()) {
                    infoBuilder.dependency(mapEntry.getKey(), mapEntry.getValue());
                }
            }
            byteBuffer = infoBuilder.build(true).getBytes();
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();

            // create META-INF section
            entry = new ZipEntry("META-INF/MANIFEST.MF");
            zipStream.putNextEntry(entry);
            byteBuffer = new ManifestBuilder().build(true).getBytes();
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();

            String jarName = descriptorName.replace(".", "-") + ".jar";

            // create config section
            entry = new ZipEntry("netbeans/config/Modules/" + descriptorName.replace(".", "-") + ".xml");
            zipStream.putNextEntry(entry);
            ModuleConfigBuilder mcb = new ModuleConfigBuilder();
            byteBuffer = mcb.name(descriptorName)
                    .autoLoad(false)
                    .eager(false)
                    .enabled(true)
                    .jarName(jarName)
                    .reloadable(false)
                    .build(true).getBytes();
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();
            entry = new ZipEntry("netbeans/modules/" + jarName);
            zipStream.putNextEntry(entry);
            zipStream.write(packSuiteJar(descriptor, dependencies));
            zipStream.closeEntry();
            if (bundles != null) {
                for (Bundle bundle : bundles.values()) {
                    if (bundle.isLocal() && bundle.getTargetLocation() != null && bundle.getEntryPoint() != null) {
                        // lib folder
                        entry = new ZipEntry("netbeans/modules/lib/");
                        zipStream.putNextEntry(entry);
                        zipStream.closeEntry();
                        // bundle
                        String entryPoint = bundle.getEntryPoint();
                        File entryPointPath = bundle.getSource();
                        if (entryPointPath.exists()) {
                            entry = new ZipEntry("netbeans/modules/lib/" + entryPoint);
                            zipStream.putNextEntry(entry);
                            zipStream.write(Files.readAllBytes(entryPointPath.toPath()));
                            zipStream.closeEntry();
                        }
                    }
                }
            }
            // create update_tracking section
            entry = new ZipEntry("netbeans/update_tracking/");
            zipStream.putNextEntry(entry);
            zipStream.closeEntry();
        }
    }

    private static byte[] packSuiteJar(ModuleSuiteDescriptor descriptor, Map<String, String> modules) throws IOException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(ATTR_MODULE_NAME, descriptor.getName());
        attributes.put(ATTR_DESCRIPTION_NAME, descriptor.getDescription());
        attributes.put(ATTR_MODULE_SPECIFICATION, SPECIFICATION_VERSION);
        attributes.put(new Attributes.Name("OpenIDE-Module-Java-Dependencies"), "Java > 1.8");
        attributes.put(new Attributes.Name("OpenIDE-Module-Display-Category"), "SNAP");
        attributes.put(ATTR_MODULE_TYPE, "STA");
        String dependenciesValue = "org.esa.snap.snap.sta, org.esa.snap.snap.sta.ui";
        for (Map.Entry<String, String> entry : modules.entrySet()) {
            dependenciesValue += ", " + entry.getKey() + " > " + entry.getValue();
        }
        attributes.put(ATTR_MODULE_DEPENDENCIES, dependenciesValue);

        ByteArrayOutputStream fOut = new ByteArrayOutputStream();
        try (JarOutputStream jarOut = new JarOutputStream(fOut, manifest)) {
            jarOut.close();
        }
        return fOut.toByteArray();
    }

    private static byte[] packAdapterJar(ToolAdapterOperatorDescriptor descriptor) throws IOException {
        _manifest.getMainAttributes().put(ATTR_DESCRIPTION_NAME, descriptor.getAlias());
        _manifest.getMainAttributes().put(ATTR_MODULE_NAME, descriptor.getName());
        _manifest.getMainAttributes().put(ATTR_MODULE_IMPLEMENTATION, SPECIFICATION_VERSION);//descriptor.getVersion());
        _manifest.getMainAttributes().put(ATTR_MODULE_SPECIFICATION, SPECIFICATION_VERSION);
        _manifest.getMainAttributes().put(ATTR_MODULE_ALIAS, descriptor.getAlias());
        File moduleFolder = new File(modulesPath, descriptor.getAlias());
        ByteArrayOutputStream fOut = new ByteArrayOutputStream();
        try (JarOutputStream jarOut = new JarOutputStream(fOut, _manifest)) {
            File[] files = moduleFolder.listFiles();
            if (files != null) {
                for (File child : files) {
                    try {
                        // ModuleInstaller from adapter folder should not be included
                        if (child.getName().endsWith("ModuleInstaller.class")) {
                            //noinspection ResultOfMethodCallIgnored
                            child.delete();
                        } else {
                            addFile(child, jarOut);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            try {
                String contents = layerXml.replace("#NAME#", descriptor.getLabel());
                JarEntry entry = new JarEntry(LAYER_XML_PATH);
                jarOut.putNextEntry(entry);
                byte[] buffer = contents.getBytes();
                jarOut.write(buffer, 0, buffer.length);
                jarOut.closeEntry();
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
            jarOut.close();
        }
        return fOut.toByteArray();
    }

    /**
     * Adds a file to the target jar stream.
     *
     * @param source    The file to be added
     * @param target    The target jar stream
     */
    private static void addFile(File source, JarOutputStream target) throws IOException {
        String entryName = source.getPath().replace(modulesPath.getAbsolutePath(), "").replace("\\", "/").substring(1);
        entryName = entryName.substring(entryName.indexOf("/") + 1);
        if (!entryName.toLowerCase().endsWith("manifest.mf")) {
            if (source.isDirectory()) {
                if (!entryName.isEmpty()) {
                    if (!entryName.endsWith("/")) {
                        entryName += "/";
                    }
                    JarEntry entry = new JarEntry(entryName);
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                File[] files = source.listFiles();
                if (files != null) {
                    for (File nestedFile : files) {
                        addFile(nestedFile, target);
                    }
                }
                return;
            }
            JarEntry entry = new JarEntry(entryName);
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            writeBytes(source, target);
            target.closeEntry();
        }
    }

    /**
     * Adds a compiled class file to the target jar stream.
     *
     * @param fromClass     The class to be added
     * @param target        The target jar stream
     */
    private static void addFile(Class fromClass, JarOutputStream target) throws IOException {
        String classEntry = fromClass.getName().replace('.', '/') + ".class";
        URL classURL = fromClass.getClassLoader().getResource(classEntry);
        if (classURL != null) {
            JarEntry entry = new JarEntry(classEntry);
            target.putNextEntry(entry);
            if (!classURL.toString().contains("!")) {
                String fileName = classURL.getFile();
                writeBytes(fileName, target);
            } else {
                try (InputStream stream = fromClass.getClassLoader().getResourceAsStream(classEntry)) {
                    writeBytes(stream, target);
                }
            }
            target.closeEntry();
        }
    }

    private static void writeBytes(String fileName, JarOutputStream target) throws IOException {
        writeBytes(new File(fileName), target);
    }

    private static void writeBytes(File file, JarOutputStream target) throws IOException {
        try (FileInputStream fileStream = new FileInputStream(file)) {
            try (BufferedInputStream inputStream = new BufferedInputStream(fileStream)) {
                byte[] buffer = new byte[1024];
                while (true) {
                    int count = inputStream.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    target.write(buffer, 0, count);
                }
            }
        }
    }

    private static void writeBytes(InputStream stream, JarOutputStream target) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int count = stream.read(buffer);
            if (count == -1) {
                break;
            }
            target.write(buffer, 0, count);
        }
    }

    private static String normalize(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Empty value");
        }
        return input.replace("-", ".").replace(" ", "_");
    }

}
