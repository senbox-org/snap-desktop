package org.esa.snap.ui.vfs.niojfilechooser;

import org.esa.snap.core.dataio.NioPaths;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FileSystemView component for Windows OS.
 * FileSystemView that handles some specific Windows specific concepts.
 *
 * @author Adrian Drăghici
 */
class NioWindowsFileSystemView extends NioFileSystemView {

    /**
     * The FileSystemView component for operating system FS
     */
    private static final FileSystemView osFileSystemView = FileSystemView.getFileSystemView();

    /**
     * The list of native classes for OS used by FSW
     */
    private static Class<?>[] nativeOsFileClasses;

    private static Logger logger = Logger.getLogger(NioVFSFileSystemView.class.getName());

    static {
        try {
            nativeOsFileClasses = new Class<?>[]{
                    Class.forName("sun.awt.shell.ShellFolder")
            };
        } catch (ClassNotFoundException ex) {
            logger.log(Level.FINE, "Unable to load native os class for Files. Details: " + ex.getMessage());
        }
    }

    /**
     * Tells whether the object is a instance of native class for OS used by FSW.
     *
     * @param target The object
     * @return {@code true} if the file is a regular file with opaque content
     */
    private boolean isInstanceOfNativeOsFileClass(Object target) {
        for (Class nativeOsFileClass : nativeOsFileClasses) {
            if (nativeOsFileClass.isInstance(target)) {
                return true;
            }
        }
        return false;
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
        if (isInstanceOfNativeOsFileClass(f)) {
            return osFileSystemView.isRoot(f);
        }
        return super.isRoot(f);
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
        return isFileSystemRoot(f) || isComputerNode(f) || f.isDirectory();
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
        if (isInstanceOfNativeOsFileClass(f)) {
            return osFileSystemView.getSystemDisplayName(f);
        }
        return super.getSystemDisplayName(f);
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
        if (isInstanceOfNativeOsFileClass(f)) {
            return osFileSystemView.getSystemTypeDescription(f);
        }
        return super.getSystemTypeDescription(f);
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
        if (isInstanceOfNativeOsFileClass(f)) {
            return osFileSystemView.getSystemIcon(f);
        }
        return super.getSystemIcon(f);
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
        if (isInstanceOfNativeOsFileClass(folder)) {
            return osFileSystemView.isParent(folder, file);
        }
        return super.isParent(folder, file);
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
        if (fileName.startsWith("\\") && !fileName.startsWith("\\\\") && isFileSystem(parent)) {
            //Path is relative to the root of parent's drive
            String path;
            if (isInstanceOfNativeOsFileClass(parent)) {
                path = parent.getAbsolutePath();
            } else {
                path = parent.toPath().toString();
            }
            if (path.length() >= 2 && path.charAt(1) == ':' && Character.isLetter(path.charAt(0))) {
                return createFileObject(path.substring(0, 2) + fileName);
            }
        }
        return super.getChild(parent, fileName);
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
        if (isInstanceOfNativeOsFileClass(f)) {
            return osFileSystemView.isFileSystem(f);
        }
        return super.isFileSystem(f);
    }

    /**
     * Creates a new folder with a default folder name.
     */
    @Override
    public File createNewFolder(File containingDir) throws IOException {
        if (isInstanceOfNativeOsFileClass(containingDir)) {
            return osFileSystemView.createNewFolder(containingDir);
        }
        return super.createNewFolder(containingDir);
    }

    /**
     * Returns whether a file is hidden or not.
     */
    @Override
    public boolean isHiddenFile(File f) {
        if (isInstanceOfNativeOsFileClass(f)) {
            return osFileSystemView.isHiddenFile(f);
        }
        return super.isHiddenFile(f);
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
        if (isInstanceOfNativeOsFileClass(dir)) {
            return osFileSystemView.isFileSystemRoot(dir);
        }
        return super.isFileSystemRoot(dir);
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
        if (isInstanceOfNativeOsFileClass(dir)) {
            return osFileSystemView.isDrive(dir);
        } else {
            String path = AccessController.doPrivileged((PrivilegedAction<String>) () -> dir.toPath().toString());
            if (path != null && (path.length() <= 3 && path.charAt(1) == ':' && Character.isLetter(path.charAt(0)))) {
                return true;
            } else {
                return super.isDrive(dir);
            }
        }
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
    public boolean isFloppyDrive(final File dir) {
        String path;
        if (isInstanceOfNativeOsFileClass(dir)) {
            return osFileSystemView.isFloppyDrive(dir);
        } else {
            path = AccessController.doPrivileged((PrivilegedAction<String>) () -> dir.toPath().toString());
        }
        return path != null && (path.equals("A:\\") || path.equals("B:\\"));
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
        if (dir != null) {
            if (isInstanceOfNativeOsFileClass(dir)) {
                return osFileSystemView.isComputerNode(dir);
            } else {
                return (dir.getPath().startsWith("\\\\\\\\") && (dir.getPath().indexOf('\\', 2) < 0 || dir.getPath().split("\\\\").length == 1));
            }
        }
        return false;
    }

    /**
     * Returns all root partitions on this system. For example, on
     * Windows, this would be the "Desktop" folder, while on DOS this would be the A: through Z: drives.
     */
    @Override
    public File[] getRoots() {
        List<File> roots = new ArrayList<>();
        try {
            roots.addAll(Arrays.asList((File[]) nativeOsFileClasses[0].getMethod("get", String.class).invoke(null, "fileChooserComboBoxFolders")));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            logger.log(Level.SEVERE,"Unable to get roots for FSW. Details: " + ex.getMessage());
        }
        roots.addAll(Arrays.asList(super.getRoots()));
        return roots.toArray(new File[0]);
    }

    /**
     * Providing default implementations for the remaining methods because most OS file systems will likely be able to use this code. If a given OS can't, override these methods in its implementation.
     */
    @Override
    public File getHomeDirectory() {
        return osFileSystemView.getHomeDirectory();
    }

    /**
     * Return the user's default starting directory for the file chooser.
     *
     * @return a <code>File</code> object representing the default starting folder
     * @since 1.4
     */
    @Override
    public File getDefaultDirectory() {
        return osFileSystemView.getDefaultDirectory();
    }

    /**
     * Returns a File object constructed from the given path string.
     */
    @Override
    public File createFileObject(String path) {
        // Check for missing backslash after drive letter such as "C:" or "C:filename"
        if (path.length() >= 2 && path.charAt(1) == ':' && Character.isLetter(path.charAt(0))) {
            if (path.length() == 2) {
                path += "\\";
            } else if (path.charAt(2) != '\\') {
                path = path.substring(0, 2) + '\\' + path.substring(2);
            }
        }
        return super.createFileObject(path);
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
        // Problem: Removable drives on Windows return false on f.exists()
        // Workaround: Override exists() to always return true.
        return new FileSystemRoot(f) {
            @Override
            public boolean exists() {
                return true;
            }
        };
    }

    /**
     * Converts given file paths from native class for OS used by FSW (which is considered to not use NIO API) to base {@code File}.
     * Fix the issue with large paths (>256 characters) on Windows OS. (the goal)
     *
     * @param filesPaths    The list of file paths
     * @param useFileHiding The flag for hiding
     * @return The list of converted file paths
     */
    private File[] fixPaths(File[] filesPaths, boolean useFileHiding) {
        List<File> files = new ArrayList<>();
        for (File filePath : filesPaths) {
            if (!useFileHiding || !isHiddenFile(filePath)) {
                if (filePath.getPath().startsWith("\\\\")) {//is a windows network path
                    String[] fParts = filePath.getPath().split("\\\\");
                    if (fParts.length == 4) {
                        String host = fParts[2];
                        String share = fParts[3];
                        filePath = NioPaths.get("\\\\" + host + '\\' + share).toFile();
                    }
                } else {
                    filePath = new File(filePath.getPath());
                }
                if (filePath.exists()) {
                    files.add(filePath);
                }
            }
        }
        return files.toArray(new File[0]);
    }

    /**
     * Gets the list of shown (i.e. not hidden) files.
     */
    @Override
    public File[] getFiles(File dir, boolean useFileHiding) {
        if (isInstanceOfNativeOsFileClass(dir)) {
            return fixPaths(osFileSystemView.getFiles(dir, useFileHiding), useFileHiding);
        } else {
            return super.getFiles(dir, useFileHiding);
        }
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
        if (!(dir.getPath().startsWith("\\\\") && dir.getPath().split("\\\\").length == 4) && isInstanceOfNativeOsFileClass(dir)) {
            return osFileSystemView.getParentDirectory(dir);
        }
        return super.getParentDirectory(dir);
    }
}

