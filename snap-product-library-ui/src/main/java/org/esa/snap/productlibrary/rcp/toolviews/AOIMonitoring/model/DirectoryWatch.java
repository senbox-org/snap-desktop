package org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model;

import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class DirectoryWatch {

    private final List<File> directoryList = new ArrayList<>();
    private final List<FileContainer> watchedFiles = new ArrayList<>();
    private final List<File> directoriesContent = new ArrayList<>();
    private final List<DirectoryWatchListener> listeners = new ArrayList<>();
    private final List<File> addedFileList = new ArrayList<>();
    private final List<File> removedFileList = new ArrayList<>();

    private Timer timer;

    public DirectoryWatch() {
    }

    public synchronized void add(File dir) {
        if (dir != null) {
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("The argument: " + dir.getPath() + " is not a directory");
            }
            if (getDirectoryIndex(dir) < 0) {
                directoryList.add(dir);
            }
        }
    }

    private synchronized void remove(File dir) {
        final int index = getDirectoryIndex(dir);
        if (index >= 0) {
            directoryList.remove(index);

            List<File> toDelete = new ArrayList<>();
            for (Object aDirectoriesContent : directoriesContent) {
                File file = (File) aDirectoriesContent;
                if (dir.equals(file.getParentFile())) {
                    toDelete.add(file);
                }
            }

            for (Object aToDelete : toDelete) {
                File file = (File) aToDelete;
                directoriesContent.remove(file);
            }
        }
    }

    synchronized public void removeAll() {
        while (!directoryList.isEmpty()) {
            remove(directoryList.get(0));
        }
    }

    synchronized public void addListener(DirectoryWatchListener listener) {
        if ((listener != null) && (!listeners.contains(listener))) {
            listeners.add(listener);
        }
    }

    public void start(long rate) {
        //System.out.println("DirWatch started");
        timer = new Timer();
        timer.scheduleAtFixedRate(new DirectoryWatchTask(), 0, rate);
    }

    public void stop() {
        //System.out.println("DirWatch stopped");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private synchronized void checkDirectories() {
        addNewFilesToWatch();
        checkForFileChanges();
        checkForRemovedFiles();
        notifyListeners();
    }

    private int getDirectoryIndex(File dir) {
        return directoryList.indexOf(dir);
    }

    private void checkForRemovedFiles() {
        removedFileList.clear();
        for (Object aDirectoriesContent : directoriesContent) {
            File file = (File) aDirectoriesContent;
            if (!file.exists()) {
                removedFileList.add(file);
            }
        }

        for (Object aRemovedFileList : removedFileList) {
            File file = (File) aRemovedFileList;
            directoriesContent.remove(file);
        }
    }

    private void checkForFileChanges() {
        final List<File> stableFiles = new ArrayList<>();

        addedFileList.clear();
        for (Object watchedFile : watchedFiles) {
            final FileContainer container = (FileContainer) watchedFile;
            final File file = container.getFile();
            final long length = file.length();
            final long lastModified = file.lastModified();

            if ((container.getLastModified() == lastModified)
                    && (container.getSize() == length)) {
                container.setStableCount(1 + container.getStableCount());
                if (container.getStableCount() > 2) {
                    stableFiles.add(file);
                }
            } else {
                container.setLastModified(lastModified);
                container.setSize(length);
                container.setStableCount(0);
            }
        }

        FileContainer comparer = new FileContainer();
        for (Object stableFile : stableFiles) {
            final File file = (File) stableFile;

            addedFileList.add(file);
            directoriesContent.add(file);
            SystemUtils.LOG.fine(file.getName() + " added to dirContents");

            comparer.setFile(file);
            watchedFiles.remove(comparer);
        }
    }

    private void notifyListeners() {
        if (!addedFileList.isEmpty()) {
            File added[] = new File[addedFileList.size()];
            added = addedFileList.toArray(added);

            for (Object listener1 : listeners) {
                DirectoryWatchListener listener = (DirectoryWatchListener) listener1;
                listener.filesAdded(added);
            }
            addedFileList.clear();
        }

        if (!removedFileList.isEmpty()) {
            File removed[] = new File[removedFileList.size()];
            removed = removedFileList.toArray(removed);

            for (Object listener1 : listeners) {
                DirectoryWatchListener listener = (DirectoryWatchListener) listener1;
                listener.filesRemoved(removed);
            }

            removedFileList.clear();
        }
    }

    private void addNewFilesToWatch() {
        final FileContainer comparer = new FileContainer();

        for (Object aDirectoryList : directoryList) {
            File dir = (File) aDirectoryList;
            File fileArray[] = dir.listFiles();
            if (fileArray == null)
                continue;
            for (File file : fileArray) {
                if (!directoriesContent.contains(file)) {
                    comparer.setFile(file);
                    if (!watchedFiles.contains(comparer)) {
                        final FileContainer container = new FileContainer();
                        container.setFile(file);
                        container.setLastModified(file.lastModified());
                        container.setSize(file.length());
                        container.setStableCount(0);
                        watchedFiles.add(container);
                    }
                }
            }
        }
    }

    private static class FileContainer {
        private File file;
        private long lastModified;
        private long size;
        private int stableCount;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public int getStableCount() {
            return stableCount;
        }

        public void setStableCount(int stableCount) {
            this.stableCount = stableCount;
        }

        /**
         * @noinspection instanceof Interfaces
         */
        public boolean equals(Object obj) {
            boolean result = false;

            if (obj instanceof FileContainer) {
                result = this.file.equals(((FileContainer) obj).getFile());
            }

            return result;
        }
    }

    private class DirectoryWatchTask extends TimerTask {
        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            checkDirectories();
        }
    }

    public interface DirectoryWatchListener {

        void filesAdded(File[] files);

        void filesRemoved(File[] files);
    }
}
