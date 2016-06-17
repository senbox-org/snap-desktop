package org.esa.snap.utils;

import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterRegistry;

import java.io.IOException;
import java.nio.file.*;
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

    private WatchService directoryWatcher;
    private Thread thread;
    private volatile boolean isRunning;
    AdapterWatcher() {
        try {
            directoryWatcher = FileSystems.getDefault().newWatchService();
            Path adaptersFolder = ToolAdapterIO.getUserAdapterPath();
            adaptersFolder.register(directoryWatcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            thread = new Thread(() -> {
                while (isRunning) {
                    WatchKey key;
                    try {
                         key = directoryWatcher.take();
                    } catch (InterruptedException ex) {
                        return;
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fileName = ev.context();
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            folderAdded(adaptersFolder.resolve(fileName));
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            folderDeleted(adaptersFolder.resolve(fileName));
                        }
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            });
        } catch (IOException ignored) {
        }
    }

    public void startMonitor() {
        isRunning = true;
        thread.start();
    }

    public void stopMonitor() {
        isRunning = false;
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
}
