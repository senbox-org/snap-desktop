/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.utils;

import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterRegistry;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.modules.ModulePackager;
import org.openide.modules.Places;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This singleton class watches for changes (additions/deletions) in the tool adapters folder.
 *
 * @author Cosmin Cara
 */
public enum AdapterWatcher {
    INSTANCE;
    private final Logger logger = Logger.getLogger(AdapterWatcher.class.getName());

    private WatchService watcher;
    private final WatchEvent.Kind[] eventTypes = new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE };
    private Thread thread;
    private volatile boolean isRunning;
    private volatile boolean isSuspended;
    private Map<Path, String> jarAliases;
    private Map<Path, WatchKey> monitoredPaths;

    AdapterWatcher() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
            monitoredPaths = new HashMap<>();
            jarAliases = new HashMap<>();
            Path adaptersFolder = ToolAdapterIO.getAdaptersPath();
            File userDirectory = Places.getUserDirectory();
            Path nbUserModulesPath = Paths.get(userDirectory != null ? userDirectory.getAbsolutePath() : "", "modules");
            if (!Files.exists(nbUserModulesPath)) {
                Files.createDirectory(nbUserModulesPath);
            }
            monitorPath(adaptersFolder);
            monitorPath(nbUserModulesPath);
            thread = new Thread(() -> {
                while (isRunning) {
                    WatchKey key;
                    try {
                         key = watcher.take();
                    } catch (InterruptedException ex) {
                        return;
                    }
                    key.pollEvents().stream().filter(event -> !isSuspended).forEach(event -> {
                        WatchEvent.Kind<?> kind = event.kind();
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fileName = ev.context();
                        boolean isJar = fileName.toString().endsWith(".jar");
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            if (!isJar) {
                                folderAdded(adaptersFolder.resolve(fileName));
                            } else {
                                jarAdded(nbUserModulesPath.resolve(fileName));
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            if (!isJar) {
                                folderDeleted(adaptersFolder.resolve(fileName));
                            } else {
                                jarDeleted(nbUserModulesPath.resolve(fileName));
                            }
                        }
                    });
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            });
        } catch (IOException ignored) {
        }
    }

    /**
     * Enables this watch service to monitor the registered paths
     */
    public void startMonitor() {
        isRunning = true;
        isSuspended = false;
        thread.start();
    }

    /**
     * Stops this watch service to monitor the registered paths
     */
    public void stopMonitor() {
        isRunning = false;
    }

    /**
     * Pauses monitoring on the registered paths
     */
    public void suspend() {
        isSuspended = true;
    }

    /**
     * Resumes monitoring on the registered paths
     */
    public void resume() {
        isSuspended = false;
    }

    /**
     * Registers the watch service for the given path to monitor changes.
     *
     * @param path The path to monitor
     */
    public void monitorPath(Path path) throws IOException {
        if (path != null && Files.isDirectory(path)) {
            WatchKey key = path.register(watcher, eventTypes);
            monitoredPaths.put(path, key);
            SystemUtils.LOG.info(String.format("Registered %s for watching", path.toString()));
        }
    }

    /**
     * Unregisters the watch service from the given path.
     *
     * @param path  The path to unregister for
     */
    public void unmonitorPath(Path path) {
        if (path != null && Files.isDirectory(path)) {
            WatchKey key = monitoredPaths.remove(path);
            if (key != null) {
                key.cancel();
                SystemUtils.LOG.info(String.format("Unregistered %s for watching", path.toString()));
            }
        }
    }

    private void folderAdded(Path folder) {
        try {
            Thread.sleep(500);
            ToolAdapterIO.registerAdapter(folder);
        }catch (InterruptedException | OperatorException ex){
            logger.log(Level.INFO, "Could not load adapter for folder added in repository: " + folder.toString() + " (error:" + ex.getMessage());
        }
    }

    private void folderDeleted(Path folder) {
        if (folder != null) {
            String alias = folder.toFile().getName();
            ToolAdapterOperatorDescriptor operatorDescriptor = ToolAdapterRegistry.INSTANCE.findByAlias(alias);
            if (operatorDescriptor != null) {
                ToolAdapterIO.removeOperator(operatorDescriptor);
            }
        }
    }

    private void jarAdded(Path jarFile) {
        Path unpackLocation = processJarFile(jarFile);
        if (unpackLocation != null) {
            folderAdded(unpackLocation);
        } else {
            logger.warning(String.format("Jar %s could not be unpacked. See previous exception.", jarFile));
        }
    }

    private void jarDeleted(Path jarFile) {
        String alias = jarAliases.get(jarFile);
        if (alias == null) {
            String fileName = jarFile.getFileName().toString().replace(".jar", "");
            int idx = fileName.lastIndexOf(".");
            if (idx > 0) {
                alias = fileName.substring(idx + 1);
            } else {
                alias = fileName;
            }
        }
        ToolAdapterOperatorDescriptor operatorDescriptor = ToolAdapterRegistry.INSTANCE.findByAlias(alias);
        if (operatorDescriptor != null) {
            ToolAdapterIO.removeOperator(operatorDescriptor);
        } else {
            logger.warning(String.format("Cannot find adapter for %s", jarFile.toString()));
        }
    }

    private Path processJarFile(Path jarFile) {
        String unpackPath = jarFile.getFileName().toString().replace(".jar", "");
        try {
            unpackPath = ModulePackager.getAdapterAlias(jarFile.toFile());
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
        jarAliases.put(jarFile, unpackPath);
        Path destination = ToolAdapterIO.getAdaptersPath().resolve(unpackPath);
        try{
            if (!Files.exists(destination)) {
                ModulePackager.unpackAdapterJar(jarFile.toFile(), destination.toFile());
            } else {
                Path versionFile = destination.resolve("version.txt");
                if (Files.exists(versionFile)) {
                    String versionText = new String(Files.readAllBytes(versionFile));
                    String jarVersion = ModulePackager.getAdapterVersion(jarFile.toFile());
                    if (jarVersion != null && !versionText.equals(jarVersion)) {
                        ModulePackager.unpackAdapterJar(jarFile.toFile(), destination.toFile());
                        logger.info(String.format("The adapter with the name %s and version %s was replaced by version %s", unpackPath, versionText, jarVersion));
                    } else {
                        logger.info(String.format("An adapter with the name %s and version %s already exists", unpackPath, versionText));
                    }
                } else {
                    ModulePackager.unpackAdapterJar(jarFile.toFile(), destination.toFile());
                }
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return destination;
    }
}
