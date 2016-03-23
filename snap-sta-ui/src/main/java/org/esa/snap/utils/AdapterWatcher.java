package org.esa.snap.utils;

import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kraftek on 3/8/2016.
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
            File adaptersFolder = ToolAdapterIO.getUserAdapterPath();
            Path path = adaptersFolder.toPath();
            path.register(directoryWatcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
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
                            folderAdded(path.resolve(fileName).toFile());
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            folderDeleted(path.resolve(fileName).toFile());
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

    private void folderAdded(File folder) {
        try {
            ToolAdapterIO.registerAdapter(folder);
        }catch (OperatorException ex){
            logger.log(Level.INFO, "Could not load adapter for folder added in repository: " + folder.toString() + " (error:" + ex.getMessage());
        }
    }

    private void folderDeleted(File folder) {
        if (folder != null) {
            String alias = folder.getName();
            ToolAdapterOperatorDescriptor operatorDescriptor = ToolAdapterRegistry.INSTANCE.findByAlias(alias);
            if (operatorDescriptor != null) {
                ToolAdapterIO.removeOperator(operatorDescriptor);
            }
        }
    }
}
