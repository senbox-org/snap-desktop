package org.esa.snap.ui.vfs.niojfilechooser;

import org.esa.snap.core.dataio.NioFile;
import org.esa.snap.core.dataio.vfs.remote.model.VFSRemoteFileRepository;
import org.esa.snap.core.dataio.vfs.remote.object_storage.VFSPlugInActivator;
import org.esa.snap.core.dataio.vfs.remote.object_storage.VFSRemoteFileRepositoriesController;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FileSystemView component for VFS.
 * FileSystemView that handles some specific VFS specific concepts.
 *
 * @author Adrian Drăghici
 */
public class NioVFSFileSystemView extends NioFileSystemView {

    /**
     * The error message for failing authentication on VFS service.
     */
    private static final String LOGIN_FAILED_MESSAGE = " - Login failed";

    /**
     * The error message for failing accessing VFS service.
     */
    private static final String ACCESS_FAILED_MESSAGE = " - Access failed";

    private FileSystem fs;
    private static Logger logger = Logger.getLogger(NioVFSFileSystemView.class.getName());
    private String name;

    /**
     * Gets the VFS path of root
     *
     * @return The VFS path of root
     */
    Path getRoot() {
        return fs.getPath(fs.getSeparator());
    }

    /**
     * Creates the FileSystemView component for VFS.
     *
     * @param vfsRemoteFileRepository The VFS Remote File Repository
     * @throws IOException If an I/O error occurs
     */
    private NioVFSFileSystemView(VFSRemoteFileRepository vfsRemoteFileRepository) throws IOException {
        try {
            fs = VFSPlugInActivator.initAndGetVFS(vfsRemoteFileRepository);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to initialize VFS. Details: " + ex.getMessage());
            throw new IOException(ex);
        }
        name = vfsRemoteFileRepository.getName();
        UIManager.put("FileChooser.readOnly", true);
    }

    /**
     * Creates the new FileSystemView component for VFS.
     *
     * @return The FileSystemView component for VFS
     */
    static Map<String, NioVFSFileSystemView> getNioVFSFileSystemView() {
        Map<String, NioVFSFileSystemView> vfsFileSystemViews = new HashMap<>();
        for (VFSRemoteFileRepository vfsRemoteFileRepository : VFSRemoteFileRepositoriesController.getVFSRemoteFileRepositories()) {
            try {
                vfsFileSystemViews.put("/" + NioFileSystemView.getFSWRoot(vfsRemoteFileRepository.getName()), new NioVFSFileSystemView(vfsRemoteFileRepository));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unable to initialize " + vfsRemoteFileRepository.getName() + " VFS. Details: " + ex.getMessage());
            }
        }
        return vfsFileSystemViews;
    }

    /**
     * Determines if the given file is a root in the navigable tree(s).
     * Examples: Windows 98 has one root, the Desktop folder. DOS has one root per drive letter, <code>C:\</code>, <code>D:\</code>, etc. Unix has one root, the <code>"/"</code> directory.
     * <p>
     * The default implementation gets information from the <code>NioShellFolder</code> class.
     *
     * @param f a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>f</code> is a root in the navigable tree.
     * @see #isFileSystemRoot
     */
    @Override
    public boolean isRoot(File f) {
        return f != null && f.isAbsolute();
    }

    /**
     * Returns all root directories on this system. For example, on
     * EO Cloud OpenStack Swift, this would be the products folders.
     */
    @Override
    public File[] getRoots() {
        // Don't cache this array, because filesystem might change
        List<File> roots = new ArrayList<>();
        try {
            for (Path p : fs.getRootDirectories()) {
                roots.add(createFileSystemRoot(p));
            }
        } catch (IllegalArgumentException ex) {
            logger.log(Level.SEVERE, "Unable to get root directories from VFS. Details: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), name + ACCESS_FAILED_MESSAGE, JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to to get root directories from VFS. Details: " + ex.getMessage());
            if (ex.getMessage().contains("400: Bad Request")) {
                JOptionPane.showMessageDialog(null, "Wrong data given.", name + ACCESS_FAILED_MESSAGE, JOptionPane.WARNING_MESSAGE);
            } else if (ex.getMessage().contains("401: Unauthorized")) {
                JOptionPane.showMessageDialog(null, "The authentication credentials are not valid.", name + LOGIN_FAILED_MESSAGE, JOptionPane.WARNING_MESSAGE);
            } else if (ex.getMessage().contains("403: Forbidden")) {
                JOptionPane.showMessageDialog(null, "The identity was successfully authenticated but it is not authorized to perform the requested action.", name + LOGIN_FAILED_MESSAGE, JOptionPane.WARNING_MESSAGE);
            } else if (ex.getMessage().contains("404: Not Found")) {
                JOptionPane.showMessageDialog(null, "Resource not found.", name + ACCESS_FAILED_MESSAGE, JOptionPane.WARNING_MESSAGE);
            } else if (ex.getMessage().contains("UnknownHostException") || ex.getMessage().contains("ConnectException")) {
                JOptionPane.showMessageDialog(null, "Connection failed.\nCheck your internet connection or VFS service address!", name + ACCESS_FAILED_MESSAGE, JOptionPane.WARNING_MESSAGE);
            } else {
                logger.fine(ex.getMessage());
            }
        }
        return roots.toArray(new File[0]);
    }

    /**
     * Creates a new <code>File</code> object for <code>f</code> with correct behavior for a file system root directory.
     *
     * @param p a <code>File</code> object representing a file system root directory, for example "/" on Unix or "C:\" on Windows.
     * @return a new <code>File</code> object
     * @since 1.4
     */
    private NioFile createFileSystemRoot(Path p) {
        return new VFSFileSystemRoot(p);
    }

    /**
     * Providing default implementations for the remaining methods because most OS file systems will likely be able to use this code. If a given OS can't, override these methods in its implementation.
     */
    @Override
    public File getHomeDirectory() {
        File[] roots = getRoots();
        return (roots.length == 0) ? null : roots[0];
    }


    /**
     * FileSystemRoot for FSW VFS roots.
     * Used for creating custom {@code File} objects which represent the root of a VFS in FSW.
     */
    static class VFSFileSystemRoot extends NioFile {

        /**
         * Creates the FileSystemRoot for FSW VFS roots.
         *
         * @param p The target file
         */
        VFSFileSystemRoot(Path p) {
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
        @NotNull
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
        @NotNull
        public String getPath() {
            return super.getPath();
        }

    }

}
