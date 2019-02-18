package org.esa.snap.ui.vfs.niojfilechooser;

import org.esa.snap.core.dataio.vfs.remote.object_storage.aws.S3FileSystemProvider;
import org.esa.snap.core.dataio.vfs.remote.object_storage.http.HttpFileSystemProvider;
import org.esa.snap.core.dataio.vfs.remote.object_storage.swift.SwiftFileSystemProvider;
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

import static org.esa.snap.core.dataio.NioPaths.getVFSRoots;
import static org.esa.snap.core.dataio.NioPaths.isVirtualFileSystemRoot;

/**
 * FileSystemView component for VFS.
 *
 * @author Adrian Drăghici
 */
class NioFileSystemView extends FileSystemView {

    private static NioFileSystemView osFileSystemView = null;
    private static NioFileSystemView swiftFileSystemView = null;
    private static NioFileSystemView httpFileSystemView = null;
    private static NioFileSystemView s3FileSystemView = null;

    private static final String newFolderString = "New_Folder";
    private static final String newFolderNextString = "New_Folder_({0})";

    NioFileSystemView() {
    }

    public static NioFileSystemView getFileSystemView() {
        swiftFileSystemView = null;
        httpFileSystemView = null;
        s3FileSystemView = null;
        if (osFileSystemView == null) {
            osFileSystemView = new NioOsFileSystemView();
        }
        return osFileSystemView;
    }

    /**
     * Determines if the given file is a root in the navigable tree(s).
     * Examples: Windows 98 has one root, the Desktop folder. DOS has one root
     * per drive letter, <code>C:\</code>, <code>D:\</code>, etc. Unix has one root,
     * the <code>"/"</code> directory.
     * <p>
     * The default implementation gets information from the <code>NioShellFolder</code> class.
     *
     * @param f a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>f</code> is a root in the navigable tree.
     * @see #isFileSystemRoot
     */
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
    public Boolean isTraversable(File f) {
        return Files.isDirectory(f.toPath());
    }

    /**
     * Name of a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "M:\" directory
     * displays as "CD-ROM (M:)"
     * <p>
     * The default implementation gets information from the NioShellFolder class.
     *
     * @param f a <code>File</code> object
     * @return the file name as it would be displayed by a native file chooser
     * @see JFileChooser#getName
     * @since 1.4
     */
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
     * Type description for a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "Desktop" folder
     * is described as "Desktop".
     * <p>
     * Override for platforms with native NioShellFolder implementations.
     *
     * @param f a <code>File</code> object
     * @return the file type description as it would be displayed by a native file chooser
     * or null if no native information is available.
     * @see JFileChooser#getTypeDescription
     * @since 1.4
     */
    public String getSystemTypeDescription(File f) {
        if (f == null) {
            return null;
        }
        return Files.isDirectory(f.toPath()) ? "File Folder" : "File";
    }

    /**
     * Icon for a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "M:\" directory
     * displays a CD-ROM icon.
     * <p>
     * The default implementation gets information from the NioShellFolder class.
     *
     * @param f a <code>File</code> object
     * @return an icon as it would be displayed by a native file chooser
     * @see JFileChooser#getIcon
     * @since 1.4
     */
    public Icon getSystemIcon(File f) {
        if (f == null) {
            return null;
        } else {
            return UIManager.getIcon(Files.isDirectory(f.toPath()) ? "FileView.directoryIcon" : "FileView.fileIcon");
        }
    }

    /**
     * On Windows, a file can appear in multiple folders, other than its
     * parent directory in the filesystem. Folder could for example be the
     * "Desktop" folder which is not the same as file.getParentFile().
     *
     * @param folder a <code>File</code> object representing a directory or special folder
     * @param file   a <code>File</code> object
     * @return <code>true</code> if <code>folder</code> is a directory or special folder and contains <code>file</code>.
     * @since 1.4
     */
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
     * File(parent, fileName)</code> except when parent and child are both
     * special folders, in which case the <code>File</code> is a wrapper containing
     * a <code>NioShellFolder</code> object.
     * @since 1.4
     */
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
     * Checks if <code>f</code> represents a real directory or file as opposed to a
     * special folder such as <code>"Desktop"</code>. Used by UI classes to decide if
     * a folder is selectable when doing directory choosing.
     *
     * @param f a <code>File</code> object
     * @return <code>true</code> if <code>f</code> is a real file or directory.
     * @since 1.4
     */
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
            File newFolder = createFileObject(containingDir, newFolderString);
            int i = 1;
            while (Files.exists(newFolder.toPath())) {
                newFolder = createFileObject(containingDir, MessageFormat.format(newFolderNextString, i++));
            }
            Files.createDirectory(newFolder.toPath());
            return newFolder;
        }
    }

    /**
     * Returns whether a file is hidden or not.
     */
    public boolean isHiddenFile(File f) {
        try {
            return Files.isHidden(f.toPath());
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Is dir the root of a tree in the file system, such as a drive
     * or partition. Example: Returns true for "C:\" on Windows 98.
     *
     * @param dir a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>f</code> is a root of a filesystem
     * @see #isRoot
     * @since 1.4
     */
    public boolean isFileSystemRoot(File dir) {
        Path parent = dir.toPath().getParent();
        return isVirtualFileSystemRoot(dir) || parent == null || !dir.toPath().startsWith(parent);
    }

    /**
     * Used by UI classes to decide whether to display a special icon
     * for drives or partitions, e.g. a "hard disk" icon.
     * <p>
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    public boolean isDrive(File dir) {
        return isVirtualFileSystemRoot(dir);
    }

    /**
     * Used by UI classes to decide whether to display a special icon
     * for a floppy disk. Implies isDrive(dir).
     * <p>
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    public boolean isFloppyDrive(File dir) {
        return false;
    }

    /**
     * Used by UI classes to decide whether to display a special icon
     * for a computer node, e.g. "My Computer" or a network server.
     * <p>
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    public boolean isComputerNode(File dir) {
        return false;
    }

    private File[] getVirtualRoots(File dir) {
        switch (dir.getPath()) {
            case "/" + SwiftFileSystemProvider.SWIFT_ROOT:
                if (swiftFileSystemView == null) {
                    swiftFileSystemView = NioSwiftS3FileSystemView.getNioSwiftFileSystemView();
                }
                if (swiftFileSystemView != null) {
                    return swiftFileSystemView.getRoots();
                }
                break;
            case "/" + HttpFileSystemProvider.HTTP_ROOT:
                if (httpFileSystemView == null) {
                    httpFileSystemView = NioHttpFileSystemView.getNioHttpFileSystemView();
                }
                if (httpFileSystemView != null) {
                    return httpFileSystemView.getRoots();
                }
                break;
            case "/" + S3FileSystemProvider.S3_ROOT:
                if (s3FileSystemView == null) {
                    s3FileSystemView = NioAmazonS3FileSystemView.getNioS3FileSystemView();
                }
                if (s3FileSystemView != null) {
                    return s3FileSystemView.getRoots();
                }
                break;
        }
        return null;
    }

    /**
     * Returns all root partitions on this system. For example, on
     * Windows, this would be the "Desktop" folder, while on DOS this
     * would be the A: through Z: drives.
     */
    public File[] getRoots() {
        return getVFSRoots();
    }

    /**
     * Providing default implementations for the remaining methods
     * because most OS file systems will likely be able to use this
     * code. If a given OS can't, override these methods in its
     * implementation.
     */
    public File getHomeDirectory() {
        return createFileObject(System.getProperty("user.home"));
    }

    /**
     * Return the user's default starting directory for the file chooser.
     *
     * @return a <code>File</code> object representing the default
     * starting folder
     * @since 1.4
     */
    public File getDefaultDirectory() {
        return getHomeDirectory();
    }

    /**
     * Returns a File object constructed in dir from the given filename.
     */
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
    public File createFileObject(String path) {
        File f = new File(path);
        if (isFileSystemRoot(f)) {
            f = createFileSystemRoot(f);
        }
        return f;
    }

    /**
     * Gets the list of shown (i.e. not hidden) files.
     */
    public File[] getFiles(File dir, boolean useFileHiding) {
        try {
            List<File> files = new ArrayList<>();
            File[] virtualRoots = getVirtualRoots(dir);
            if (virtualRoots != null) {
                return virtualRoots;
            } else if (!isVirtualFileSystemRoot(dir) && Files.isDirectory(dir.toPath())) {
                DirectoryStream.Filter<Path> filter = entry -> !(useFileHiding && Files.isHidden(entry));
                DirectoryStream<Path> stream = Files.newDirectoryStream(dir.toPath(), filter);
                for (Path path : stream) {
                    files.add(path.toFile());
                }
                return files.toArray(new File[0]);
            }
        } catch (Exception e) {
            if (e.getMessage().contains("UnsupportedOperationException")) {
                JOptionPane.showMessageDialog(null, "HTTP Server not supported.", "Http - Access failed", JOptionPane.WARNING_MESSAGE);
            }
            e.printStackTrace();
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
     * Creates a new <code>File</code> object for <code>f</code> with correct
     * behavior for a file system root directory.
     *
     * @param f a <code>File</code> object representing a file system root
     *          directory, for example "/" on Unix or "C:\" on Windows.
     * @return a new <code>File</code> object
     * @since 1.4
     */
    protected File createFileSystemRoot(File f) {
        return new FileSystemRoot(f);
    }

    static class FileSystemRoot extends File {
        FileSystemRoot(File f) {
            super(f, "");
        }

        public boolean isDirectory() {
            return true;
        }

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

