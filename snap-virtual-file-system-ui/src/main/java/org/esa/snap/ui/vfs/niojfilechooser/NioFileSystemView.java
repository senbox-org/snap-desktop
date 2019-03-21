package org.esa.snap.ui.vfs.niojfilechooser;

import com.sun.javafx.PlatformUtil;
import org.esa.snap.core.dataio.NioFile;
import org.esa.snap.core.dataio.NioPaths;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.esa.snap.core.dataio.NioPaths.isVirtualFileSystemRoot;

/**
 * FileSystemView component for VFS.
 *
 * @author Adrian Drăghici
 */
class NioFileSystemView extends FileSystemView {

    /**
     * The pattern for name.
     */
    private static final String ROOT_NAME = "%root_name%";

    /**
     * The path name of root for showing on file chooser.
     */
    private static final String FSW_ROOT = ROOT_NAME + ":/";

    /**
     * The FileSystemView component for Windows operating system FS
     */
    private static NioFileSystemView nioWindowsFileSystemView = null;

    /**
     * The FileSystemView component for Unix operating system FS
     */
    private static NioFileSystemView nioLinuxFileSystemView = null;

    /**
     * The list of FileSystemView components for VFSs
     */
    private static Map<String, NioVFSFileSystemView> vfsFileSystemViews = null;

    /**
     * The icon for VFS root
     */
    private static ImageIcon vfsRootIcon;

    /**
     * The icon for VFS directory
     */
    private static ImageIcon vfsDirectoryIcon;

    /**
     * The icon for VFS file
     */
    private static ImageIcon vfsFileIcon;

    private static Logger logger = Logger.getLogger(NioVFSFileSystemView.class.getName());

    static {
        try {
            vfsRootIcon = new ImageIcon(NioFileSystemView.class.getResource("/org/esa/snap/ui/vfs/niojfilechooser/icons/vfs_root.png"));
            vfsDirectoryIcon = new ImageIcon(NioFileSystemView.class.getResource("/org/esa/snap/ui/vfs/niojfilechooser/icons/vfs_folder.png"));
            vfsFileIcon = new ImageIcon(NioFileSystemView.class.getResource("/org/esa/snap/ui/vfs/niojfilechooser/icons/vfs_file.png"));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Unable to load image resource. Details: " + ex.getMessage());
        }
    }

    /**
     * The default name for a new directory.
     */
    private static final String NEW_FOLDER_STRING = "New_Folder";

    /**
     * The default name for a next new directory.
     */
    private static final String NEW_FOLDER_NEXT_STRING = "New_Folder_({0})";

    /**
     * Creates the new FileSystemView component for VFS
     */
    NioFileSystemView() {
    }

    /**
     * Creates the new FileSystemView component for VFS
     * Initialize internal FileSystemView components.
     *
     * @return The FileSystemView component for operating system
     */
    public static NioFileSystemView getFileSystemView() {
        vfsFileSystemViews = NioVFSFileSystemView.getNioVFSFileSystemView();
        if (PlatformUtil.isWindows()) {
            if (nioWindowsFileSystemView == null) {
                nioWindowsFileSystemView = new NioWindowsFileSystemView();
            }
            return nioWindowsFileSystemView;
        }
        if (PlatformUtil.isLinux() || PlatformUtil.isMac()) {
            if (nioLinuxFileSystemView == null) {
                nioLinuxFileSystemView = new NioUnixFileSystemView();
            }
            return nioLinuxFileSystemView;
        }
        throw new UnsupportedOperationException("Unsupported operating system detected");
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
        if (f == null) {
            return false;
        }
        File[] roots = getRoots();
        for (File root : roots) {
            if (root.equals(f)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the file (directory) can be visited.
     * Returns false if the directory cannot be traversed.
     *
     * @param f the <code>File</code>
     * @return <code>true</code> if the file/directory can be traversed, otherwise <code>false</code>
     * @see JFileChooser#isTraversable
     * @see FileView#isTraversable
     * @since 1.4
     */
    @Override
    public Boolean isTraversable(File f) {
        return Files.isDirectory(f.toPath());
    }

    /**
     * Name of a file, directory, or folder as it would be displayed in a system file browser. Example from Windows: the "M:\" directory displays as "CD-ROM (M:)"
     * <p>
     * The default implementation gets information from the NioShellFolder class.
     *
     * @param f a <code>File</code> object
     * @return the file name as it would be displayed by a native file chooser
     * @see JFileChooser#getName
     * @since 1.4
     */
    @Override
    public String getSystemDisplayName(File f) {
        if (f == null) {
            return null;
        }
        String name = f.getName() + (isVirtualFileSystemRoot(f) ? f.toPath().getFileSystem().getSeparator() : "");
        if (name.length() < 1 || (!name.equals("..") && !name.equals(".") && !isVirtualFileSystemRoot(f) && !f.getPath().startsWith("\\\\") && (!isFileSystem(f) || isFileSystemRoot(f)) && Files.exists(f.toPath()))) {
            name = f.getPath(); // e.g. "/"
        }
        return name;
    }

    /**
     * Type description for a file, directory, or folder as it would be displayed in a system file browser. Example from Windows: the "Desktop" folder is described as "Desktop".
     * <p>
     * Override for platforms with native NioShellFolder implementations.
     *
     * @param f a <code>File</code> object
     * @return the file type description as it would be displayed by a native file chooser or null if no native information is available.
     * @see JFileChooser#getTypeDescription
     * @since 1.4
     */
    @Override
    public String getSystemTypeDescription(File f) {
        if (f == null) {
            return null;
        }
        return Files.isDirectory(f.toPath()) ? "File Folder" : "File";
    }

    /**
     * Icon for a file, directory, or folder as it would be displayed in a system file browser. Example from Windows: the "M:\" directory displays a CD-ROM icon.
     * <p>
     * The default implementation gets information from the NioShellFolder class.
     *
     * @param f a <code>File</code> object
     * @return an icon as it would be displayed by a native file chooser
     * @see JFileChooser#getIcon
     * @since 1.4
     */
    @Override
    public Icon getSystemIcon(File f) {
        if (f == null) {
            return null;
        }
        if (NioPaths.isVirtualFileSystemPath(f.getPath())) {
            if (NioPaths.isVirtualFileSystemRoot(f)) {
                return vfsRootIcon;
            } else {
                if (f.isDirectory()) {
                    return vfsDirectoryIcon;
                } else {
                    return vfsFileIcon;
                }
            }
        } else {
            return UIManager.getIcon(Files.isDirectory(f.toPath()) ? "FileView.directoryIcon" : "FileView.fileIcon");
        }
    }

    /**
     * On Windows, a file can appear in multiple folders, other than its parent directory in the filesystem. Folder could for example be the
     * "Desktop" folder which is not the same as file.getParentFile().
     *
     * @param folder a <code>File</code> object representing a directory or special folder
     * @param file   a <code>File</code> object
     * @return <code>true</code> if <code>folder</code> is a directory or special folder and contains <code>file</code>.
     * @since 1.4
     */
    @Override
    public boolean isParent(File folder, File file) {
        if (folder == null || file == null) {
            return false;
        } else {
            File parent = file.toPath().getParent().toFile();
            if (parent != null && parent.equals(folder)) {
                return true;
            }
            File[] children = getFiles(folder, false);
            for (File child : children) {
                if (file.equals(child)) {
                    return true;
                }
            }
            return folder.equals(parent);
        }
    }

    /**
     * @param parent   a <code>File</code> object representing a directory or special folder
     * @param fileName a name of a file or folder which exists in <code>parent</code>
     * @return a File object. This is normally constructed with <code>new
     * File(parent, fileName)</code> except when parent and child are both special folders, in which case the <code>File</code> is a wrapper containing a <code>NioShellFolder</code> object.
     * @since 1.4
     */
    @Override
    public File getChild(File parent, String fileName) {
        File[] children = getFiles(parent, false);
        for (File child : children) {
            if (child.getName().equals(fileName)) {
                return child;
            }
        }
        return createFileObject(parent, fileName);
    }

    /**
     * Checks if <code>f</code> represents a real directory or file as opposed to a special folder such as <code>"Desktop"</code>. Used by UI classes to decide if a folder is selectable when doing directory choosing.
     *
     * @param f a <code>File</code> object
     * @return <code>true</code> if <code>f</code> is a real file or directory.
     * @since 1.4
     */
    @Override
    public boolean isFileSystem(File f) {
        return !(Files.isSymbolicLink(f.toPath()) && Files.isDirectory(f.toPath()));
    }

    /**
     * Creates a new folder with a default folder name.
     */
    public File createNewFolder(File containingDir) throws IOException {
        if (containingDir == null) {
            throw new IOException("Containing directory is null:");
        } else {
            File newFolder = createFileObject(containingDir, NEW_FOLDER_STRING);
            int i = 1;
            while (Files.exists(newFolder.toPath())) {
                newFolder = createFileObject(containingDir, MessageFormat.format(NEW_FOLDER_NEXT_STRING, i++));
            }
            Files.createDirectory(newFolder.toPath());
            return newFolder;
        }
    }

    /**
     * Returns whether a file is hidden or not.
     */
    @Override
    public boolean isHiddenFile(File f) {
        try {
            return Files.isHidden(f.toPath());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to check whether a file is hidden. Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Is dir the root of a tree in the file system, such as a drive or partition. Example: Returns true for "C:\" on Windows 98.
     *
     * @param dir a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>f</code> is a root of a filesystem
     * @see #isRoot
     * @since 1.4
     */
    @Override
    public boolean isFileSystemRoot(File dir) {
        Path parent = dir.toPath().getParent();
        return isVirtualFileSystemRoot(dir) || parent == null || !dir.toPath().startsWith(parent);
    }

    /**
     * Used by UI classes to decide whether to display a special icon for drives or partitions, e.g. a "hard disk" icon.
     * <p>
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    @Override
    public boolean isDrive(File dir) {
        return isVirtualFileSystemRoot(dir);
    }

    /**
     * Used by UI classes to decide whether to display a special icon for a floppy disk. Implies isDrive(dir).
     * <p>
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    @Override
    public boolean isFloppyDrive(File dir) {
        return false;
    }

    /**
     * Used by UI classes to decide whether to display a special icon for a computer node, e.g. "My Computer" or a network server.
     * <p>
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    @Override
    public boolean isComputerNode(File dir) {
        return false;
    }

    private static File[] getVirtualRoots(File dir) {
        NioVFSFileSystemView vfsFileSystemView = vfsFileSystemViews.get(dir.getPath());
        if (vfsFileSystemView != null) {
            return vfsFileSystemView.getRoots();
        }
        return new File[0];
    }

    /**
     * Returns all root partitions on this system. For example, on
     * Windows, this would be the "Desktop" folder, while on DOS this would be the A: through Z: drives.
     */
    @Override
    public File[] getRoots() {
        List<NioFile> roots = new ArrayList<>();
        for (Map.Entry<String, NioVFSFileSystemView> vfsFSWRoot : vfsFileSystemViews.entrySet()) {
            roots.add(new NioFile(vfsFSWRoot.getValue().getRoot()));
        }
        return roots.toArray(new NioFile[0]);
    }

    /**
     * Providing default implementations for the remaining methods because most OS file systems will likely be able to use this code. If a given OS can't, override these methods in its implementation.
     */
    @Override
    public File getHomeDirectory() {
        return createFileObject(System.getProperty("user.home"));
    }

    /**
     * Return the user's default starting directory for the file chooser.
     *
     * @return a <code>File</code> object representing the default starting folder
     * @since 1.4
     */
    @Override
    public File getDefaultDirectory() {
        return getHomeDirectory();
    }

    /**
     * Returns a File object constructed in dir from the given filename.
     */
    @Override
    public File createFileObject(File dir, String filename) {
        if (dir == null) {
            return new File(filename);
        } else {
            return new File(dir, filename);
        }
    }

    /**
     * Returns a File object constructed from the given path string.
     */
    @Override
    public File createFileObject(String path) {
        File f;
        if (NioPaths.isVirtualFileSystemPath(path)) {
            f = new NioFile(path);
        } else {
            f = new File(path);
            if (isFileSystemRoot(f)) {
                f = createFileSystemRoot(f);
            }
        }
        return f;
    }

    /**
     * Gets the list of shown (i.e. not hidden) files.
     */
    @Override
    public File[] getFiles(File dir, boolean useFileHiding) {
        DirectoryStream<Path> stream = null;
        try {
            List<File> files = new ArrayList<>();
            File[] virtualRoots = getVirtualRoots(dir);
            if (virtualRoots != null && virtualRoots.length > 0) {
                return virtualRoots;
            } else if (!isVirtualFileSystemRoot(dir) && Files.isDirectory(dir.toPath())) {
                DirectoryStream.Filter<Path> filter = entry -> !(useFileHiding && Files.isHidden(entry));
                stream = Files.newDirectoryStream(dir.toPath(), filter);
                for (Path path : stream) {
                    files.add(path.toFile());
                }
                return files.toArray(new File[0]);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to get files. Details: " + ex.getMessage());
            if (ex.getMessage().contains("UnsupportedOperationException")) {
                JOptionPane.showMessageDialog(null, "HTTP Server not supported.", "Http - Access failed", JOptionPane.WARNING_MESSAGE);
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Unable to close the stream. Details: " + ex.getMessage());
                }
            }
        }
        return new File[0];
    }

    /**
     * Returns the parent directory of <code>dir</code>.
     *
     * @param dir the <code>File</code> being queried
     * @return the parent directory of <code>dir</code>, or
     * <code>null</code> if <code>dir</code> is <code>null</code>
     */
    @Override
    public File getParentDirectory(File dir) {
        if (dir == null || !Files.exists(dir.toPath())) {
            return null;
        }
        Path psf = dir.toPath().getParent();
        if (psf == null) {
            return null;
        }
        if (isFileSystem(psf.toFile())) {
            Path f = psf;
            if (!Files.exists(f)) {
                // This could be a node under "Network Neighborhood".
                File ppsf = psf.getParent().toFile();
                if (ppsf == null || !isFileSystem(ppsf)) {
                    // We're mostly after the exists() override for windows below.
                    f = createFileSystemRoot(f.toFile()).toPath();
                }
            }
            return f.toFile();
        } else {
            return psf.toFile();
        }
    }

    /**
     * Creates a new <code>File</code> object for <code>f</code> with correct behavior for a file system root directory.
     *
     * @param f a <code>File</code> object representing a file system root directory, for example "/" on Unix or "C:\" on Windows.
     * @return a new <code>File</code> object
     * @since 1.4
     */
    @Override
    protected File createFileSystemRoot(File f) {
        return new FileSystemRoot(f);
    }

    /**
     * Gets the path name of root for showing on file chooser, using given name of root.
     *
     * @param root The name of root
     * @return The path name of root
     */
    static String getFSWRoot(String root) {
        return FSW_ROOT.replace(ROOT_NAME, root);
    }

    /**
     * FileSystemRoot for FSW roots.
     * Used for creating custom {@code File} objects which represent the root of a FS in FSW.
     */
    static class FileSystemRoot extends File {


        /**
         * Creates the FileSystemRoot for FSW roots.
         *
         * @param f The target file
         */
        FileSystemRoot(File f) {
            super(f, "");
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
    }

}

