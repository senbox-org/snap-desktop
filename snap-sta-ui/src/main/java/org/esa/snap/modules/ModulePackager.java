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

import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.dependency.Bundle;
import org.esa.snap.core.gpf.descriptor.dependency.BundleType;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.*;
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
    private static final String VERSION = "5.0.0";
    private static final String STA_MODULE = "org.esa.snap.snap.sta";
    private static final String STA_UI_MODULE = "org.esa.snap.snap.sta.ui";
    private static final String SNAP_RCP_MODULE = "org.esa.snap.snap.rcp";
    private static final String SNAP_CORE_MODULE = "org.esa.snap.snap.core";

    static {
        _manifest = new Manifest();
        Attributes attributes = _manifest.getMainAttributes();
        ATTR_DESCRIPTION_NAME = new Attributes.Name("OpenIDE-Module-Short-Description");
        ATTR_MODULE_NAME = new Attributes.Name("OpenIDE-Module");
        ATTR_MODULE_TYPE = new Attributes.Name("OpenIDE-Module-Type");
        ATTR_MODULE_IMPLEMENTATION = new Attributes.Name("OpenIDE-Module-Implementation-Version");
        ATTR_MODULE_SPECIFICATION = new Attributes.Name("OpenIDE-Module-Specification-Version");
        ATTR_MODULE_ALIAS = new Attributes.Name("OpenIDE-Module-Alias");
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(new Attributes.Name("OpenIDE-Module-Java-Dependencies"), "Java > 1.8");
        attributes.put(new Attributes.Name("OpenIDE-Module-Module-Dependencies"), "org.esa.snap.snap.sta, org.esa.snap.snap.sta.ui");
        attributes.put(new Attributes.Name("OpenIDE-Module-Display-Category"), "SNAP");
        attributes.put(ATTR_MODULE_TYPE, "STA");
        //attributes.put(new Attributes.Name("OpenIDE-Module-Layer"), LAYER_XML_PATH);
        attributes.put(ATTR_DESCRIPTION_NAME, "External tool adapter");

        modulesPath = ToolAdapterIO.getAdaptersPath().toFile();
    }

    /**
     * Packs the files associated with the given tool adapter operator descriptor into
     * a NetBeans module file (nbm)
     *
     * @param descriptor    The tool adapter descriptor
     * @param nbmFile       The target module file
     * @throws IOException
     */
    public static void packModule(ToolAdapterOperatorDescriptor descriptor, File nbmFile) throws IOException {
        byte[] byteBuffer;
        try (final ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(nbmFile))) {
            // create Info section
            ZipEntry entry = new ZipEntry("Info/info.xml");
            zipStream.putNextEntry(entry);
            InfoBuilder infoBuilder = new InfoBuilder();
            String javaVersion = System.getProperty("java.version");
            javaVersion = javaVersion.substring(0, javaVersion.indexOf("_"));
            byteBuffer = infoBuilder.moduleName(descriptor.getName())
                                    .shortDescription(descriptor.getDescription())
                                    .longDescription(descriptor.getDescription())
                                    .displayCategory("SNAP")
                                    .specificationVersion(VERSION)
                                    .implementationVersion(descriptor.getVersion())
                                    .codebase(descriptor.getName().toLowerCase())
                                    .distribution(nbmFile.getName())
                                    .downloadSize(0)
                                    .homePage("https://github.com/senbox-org/s2tbx")
                                    .needsRestart(true)
                                    .releaseDate(new Date())
                                    .isEssentialModule(false)
                                    .showInClient(true)
                                    .javaVersion(javaVersion)
                                    .dependency(STA_MODULE, VERSION)
                                    .dependency(STA_UI_MODULE, VERSION)
                                    .dependency(SNAP_RCP_MODULE, VERSION)
                                    .dependency(SNAP_CORE_MODULE, VERSION)
                        .build().getBytes();
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();

            // create META-INF section
            entry = new ZipEntry("META-INF/MANIFEST.MF");
            zipStream.putNextEntry(entry);
            ManifestBuilder manifestBuilder = new ManifestBuilder();
            byteBuffer = manifestBuilder.javaVersion(javaVersion).build().getBytes();
            zipStream.write(byteBuffer, 0, byteBuffer.length);
            zipStream.closeEntry();

            String jarName = descriptor.getName().replace(".", "-") + ".jar";

            // create config section
            entry = new ZipEntry("netbeans/config/Modules/" + descriptor.getName().replace(".", "-") + ".xml");
            zipStream.putNextEntry(entry);
            ModuleConfigBuilder mcb = new ModuleConfigBuilder();
            byteBuffer = mcb.name(descriptor.getName())
                            .autoLoad(false)
                            .eager(false)
                            .enabled(true)
                            .jarName(jarName)
                            .reloadable(false)
                        .build().getBytes();
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
            Bundle bundle = descriptor.getBundle();
            if (bundle != null && bundle.getBundleType() != BundleType.NONE &&
                    bundle.getTargetLocation() != null &&
                    bundle.getEntryPoint() != null) {
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
            // create update_tracking section
            entry = new ZipEntry("netbeans/update_tracking/");
            zipStream.putNextEntry(entry);
            zipStream.closeEntry();
        }
    }

    /**
     * Unpacks a jar file into the user modules location.
     *
     * @param jarFile   The jar file to be unpacked
     * @param unpackFolder  The destination folder. If null, then the jar name will be used
     * @throws IOException
     */
    public static void unpackAdapterJar(File jarFile, File unpackFolder) throws IOException {
        JarFile jar = new JarFile(jarFile);
        Enumeration enumEntries = jar.entries();
        if (unpackFolder == null) {
            unpackFolder = new File(modulesPath, jarFile.getName().replace(".jar", ""));
        }
        if (!unpackFolder.exists())
            unpackFolder.mkdir();
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
            if (file.isDirectory()) {
                f.mkdir();
                continue;
            } else {
                f.getParentFile().mkdirs();
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

    private static byte[] packAdapterJar(ToolAdapterOperatorDescriptor descriptor) throws IOException {
        _manifest.getMainAttributes().put(ATTR_DESCRIPTION_NAME, descriptor.getAlias());
        _manifest.getMainAttributes().put(ATTR_MODULE_NAME, descriptor.getName());
        _manifest.getMainAttributes().put(ATTR_MODULE_IMPLEMENTATION, descriptor.getVersion());
        _manifest.getMainAttributes().put(ATTR_MODULE_SPECIFICATION, VERSION);
        _manifest.getMainAttributes().put(ATTR_MODULE_ALIAS, descriptor.getAlias());
        File moduleFolder = new File(modulesPath, descriptor.getAlias());
        ByteArrayOutputStream fOut = new ByteArrayOutputStream();
        //_manifest.getMainAttributes().put(new Attributes.Name("OpenIDE-Module-Install"), ModuleInstaller.class.getName().replace('.', '/') + ".class");
        try (JarOutputStream jarOut = new JarOutputStream(fOut, _manifest)) {
            File[] files = moduleFolder.listFiles();
            if (files != null) {
                for (File child : files) {
                    try {
                        // ModuleInstaller from adapter folder should not be included
                        if (child.getName().endsWith("ModuleInstaller.class")) {
                            child.delete();
                        } else {
                            addFile(child, jarOut);
                        }
                    } catch (Exception ignored) {
                    }
                }
                /*try {
                    addFile(ModuleInstaller.class, jarOut);
                } catch (Exception ignored) {
                    // the module possibly had ModuleInsteller.class
                }*/
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
     * @throws IOException
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
     * @throws IOException
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

}
