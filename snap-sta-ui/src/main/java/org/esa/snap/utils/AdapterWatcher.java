package org.esa.snap.utils;

import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * Created by kraftek on 3/8/2016.
 */
public enum AdapterWatcher {
    INSTANCE;

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
                            folderAdded(fileName.toFile());
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            folderDeleted(fileName.toFile());
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
        ToolAdapterIO.registerAdapter(folder);
    }

    private void folderDeleted(File folder) {
        // NO OP
    }
}
