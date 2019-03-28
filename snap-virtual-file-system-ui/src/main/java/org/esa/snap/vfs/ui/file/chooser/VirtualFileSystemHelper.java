package org.esa.snap.vfs.ui.file.chooser;

import org.esa.snap.vfs.NioFile;
import org.esa.snap.vfs.VFS;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

import javax.swing.UIManager;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * FileSystemView component for VFS.
 * FileSystemView that handles some specific VFS specific concepts.
 *
 * @author Adrian Drăghici
 */
public class VirtualFileSystemHelper {

    private final AbstractRemoteFileSystem fileSystem;

    /**
     * Creates the FileSystemView component for VFS.
     *
     * @param vfsRemoteFileRepository The VFS Remote File Repository
     */
    public VirtualFileSystemHelper(VFSRemoteFileRepository vfsRemoteFileRepository) throws URISyntaxException {
        AbstractRemoteFileSystemProvider fileSystemProvider = (AbstractRemoteFileSystemProvider)VFS.getInstance().getFileSystemProviderByScheme(vfsRemoteFileRepository.getScheme());
        URI uri = new URI(vfsRemoteFileRepository.getScheme(), vfsRemoteFileRepository.getRoot(), null);
        Map<String, ?> env = Collections.emptyMap();
        this.fileSystem = fileSystemProvider.getFileSystemOrCreate(uri, env);
    }

    /**
     * Gets the VFS path of root
     *
     * @return The VFS path of root
     */
    public Path getRoot() {
        return this.fileSystem.getRoot();
    }

    public File[] getRootDirectories() {
        List<File> roots = new ArrayList<>();
        for (Path p : this.fileSystem.getRootDirectories()) {
            roots.add(new VFSFileSystemRoot(p));
        }
        return roots.toArray(new File[roots.size()]);
    }

    /**
     * FileSystemRoot for FSW VFS roots.
     * Used for creating custom {@code File} objects which represent the root of a VFS in FSW.
     */
    private static class VFSFileSystemRoot extends NioFile {

        /**
         * Creates the FileSystemRoot for FSW VFS roots.
         *
         * @param p The target file
         */
        private VFSFileSystemRoot(Path p) {
            super(p);
        }

        /**
         * Tests whether the file denoted by this abstract pathname is a directory.
         * For our scope this method will always returns <code>true</code>.
         *
         * @return <code>true</code> if and only if the file denoted by this abstract pathname exists <em>and</em> is a directory; <code>false</code> otherwise
         * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file
         */
        @Override
        public boolean isDirectory() {
            return true;
        }

        /**
         * Tests whether this abstract pathname is absolute.  The definition of absolute pathname is system dependent.  On UNIX systems, a pathname is absolute if its prefix is <code>"/"</code>.  On Microsoft Windows systems, a pathname is absolute if its prefix is a drive specifier followed by
         * <code>"\\"</code>, or if its prefix is <code>"\\\\"</code>.
         * For our scope this method will always returns <code>false</code>.
         *
         * @return <code>true</code> if this abstract pathname is absolute,
         * <code>false</code> otherwise
         */
        @Override
        public boolean isAbsolute() {
            return false;
        }

        /**
         * Tests whether the file or directory denoted by this abstract pathname exists.
         * For our scope this method will always returns <code>true</code>.
         *
         * @return <code>true</code> if and only if the file or directory denoted by this abstract pathname exists; <code>false</code> otherwise
         * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file or directory
         */
        @Override
        public boolean exists() {
            return true;
        }

        /**
         * Returns the name of the file or directory denoted by this abstract pathname.  This is just the last name in the pathname's name sequence.  If the pathname's name sequence is empty, then the empty string is returned.
         *
         * @return The name of the file or directory denoted by this abstract pathname, or the empty string if this pathname's name sequence is empty
         */
        @Override
        public String getName() {
            String name = super.getName();
            if (name.isEmpty()) {
                name = super.getPath();
            }
            return name;
        }

        /**
         * Converts this abstract pathname into a pathname string.  The resulting string uses the {@link #separator default name-separator character} to separate the names in the name sequence.
         *
         * @return The string form of this abstract pathname
         */
        @Override
        public String getPath() {
            return super.getPath();
        }
    }
}
