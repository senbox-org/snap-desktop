package org.esa.snap.vfs.ui.file.chooser;

import org.apache.commons.lang.SystemUtils;
import org.esa.snap.vfs.NioFile;
import org.esa.snap.vfs.NioPaths;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
public class VirtualFileSystemView extends FileSystemView {

    /**
     * The default name for a new directory.
     */
    private static final String NEW_FOLDER_STRING = "New_Folder";

    /**
     * The default name for a next new directory.
     */
    private static final String NEW_FOLDER_NEXT_STRING = "New_Folder_({0})";
    private static final ImageIcon vfsRootIcon;
    private static final ImageIcon vfsDirectoryIcon;
    private static final ImageIcon vfsFileIcon;
    private static Logger logger = Logger.getLogger(VirtualFileSystemView.class.getName());

    static {
        vfsRootIcon = loadImageIcon("icons/vfs_root-23x16.png");
        vfsDirectoryIcon = loadImageIcon("icons/vfs_folder-23x16.png");
        vfsFileIcon = loadImageIcon("icons/vfs_file-23x16.png");
    }

    private final FileSystemView defaultFileSystemView;
    private final Map<String, VirtualFileSystemHelper> vfsFileSystemViews;

    /**
     * Creates the FileSystemView component for VFS.
     *
     * @param defaultFileSystemView FileSystemView component for OS
     * @param vfsRepositories       The VFS Remote File Repositories
     */
    public VirtualFileSystemView(FileSystemView defaultFileSystemView, List<VFSRemoteFileRepository> vfsRepositories) {
        super();

        this.defaultFileSystemView = defaultFileSystemView;

        this.vfsFileSystemViews = new HashMap<>();
        for (VFSRemoteFileRepository vfsRemoteFileRepository : vfsRepositories) {
            try {
                VirtualFileSystemHelper vfsFileSystemView = new VirtualFileSystemHelper(vfsRemoteFileRepository);
                String key = vfsFileSystemView.getRoot().toString();
                this.vfsFileSystemViews.put(key, vfsFileSystemView);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unable to initialize " + vfsRemoteFileRepository.getName() + " VFS. Details: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Checks whether the given path is root for a VFS.
     *
     * @param path the path
     * @return {@code True} if the given path is root for a VFS
     */
    private static boolean isVirtualRoot(Path path) {
        FileSystem fileSystem = path.getFileSystem();
        if (fileSystem instanceof AbstractRemoteFileSystem) {
            AbstractRemoteFileSystem remoteFileSystem = (AbstractRemoteFileSystem) fileSystem;
            return remoteFileSystem.getRoot().equals(path);
        }
        return false;
    }

    /**
     * Loads the icon from a path.
     *
     * @param imagePath the icon location path
     * @return the icon
     */
    private static ImageIcon loadImageIcon(String imagePath) {
        URL imageURL = VirtualFileSystemView.class.getResource(imagePath);
        return (imageURL == null) ? null : new ImageIcon(imageURL);
    }

    protected void notifyUser(String title, String message) {
        logger.log(Level.FINE, () -> title + ": " + message);
    }

    private void notifyVFSError(Exception e) {
        Throwable error = e;
        while (error.getCause() != null) {
            error = e.getCause();
        }
        if (error instanceof ConnectException || error instanceof UnknownHostException) {
            notifyUser("VFS connection failure", "Unable to contact the VFS service.\nPlease check the connection or teh Remote File Repository Address.");
        } else if (error instanceof IOException) {
            String report = error.getMessage().replaceAll("(.*)response code (.*)", "$2");
            notifyUser("VFS access failure", "The VFS service reported \"" + report + "\" error.\nPlease check the Remote File Repository Configurations.");
        } else {
            notifyUser("VFS error", error.getMessage());
        }
    }

    /**
     * Gets the child of a file.
     *
     * @param parent   a <code>File</code> object representing a directory or special folder
     * @param fileName a name of a file or folder which exists in <code>parent</code>
     * @return a File object. This is normally constructed with <code>new
     * File(parent, fileName)</code> except when parent and child are both special folders, in which case the <code>File</code> is a wrapper containing a <code>NioShellFolder</code> object.
     * @since 1.4
     */
    @Override
    public File getChild(File parent, String fileName) {
        if (isVirtualFileItem(parent)) {
            Path path = parent.toPath();
            File[] children = getVirtualFiles(path, false);
            for (File child : children) {
                if (child.getName().equals(fileName)) {
                    return child;
                }
            }
            return new NioFile(path.resolve(fileName));
        }
        return this.defaultFileSystemView.getChild(parent, fileName);
    }

    /**
     * Checks if <code>f</code> represents a real directory or file as opposed to a special folder such as <code>"Desktop"</code>. Used by UI classes to decide if a folder is selectable when doing directory choosing.
     *
     * @param file a <code>File</code> object
     * @return <code>true</code> if <code>f</code> is a real file or directory.
     * @since 1.4
     */
    @Override
    public boolean isFileSystem(File file) {
        if (isVirtualFileItem(file)) {
            return isFileSystem(file.toPath());
        }
        return this.defaultFileSystemView.isFileSystem(file);
    }

    /**
     * Creates a new folder with a default folder name.
     */
    @Override
    public File createNewFolder(File containingDir) throws IOException {
        if (isVirtualFileItem(containingDir)) {
            File newFolder = createFileObject(containingDir, NEW_FOLDER_STRING);
            int i = 1;
            while (Files.exists(newFolder.toPath())) {
                newFolder = createFileObject(containingDir, MessageFormat.format(NEW_FOLDER_NEXT_STRING, i++));
            }
            Files.createDirectory(newFolder.toPath());
            return newFolder;
        }
        return this.defaultFileSystemView.createNewFolder(containingDir);
    }

    /**
     * Determines if the given file is a root in the navigable tree(s).
     * Examples: Windows 98 has one root, the Desktop folder. DOS has one root per drive letter, <code>C:\</code>, <code>D:\</code>, etc. Unix has one root, the <code>"/"</code> directory.
     * <p>
     * The default implementation gets information from the <code>NioShellFolder</code> class.
     *
     * @param file a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>f</code> is a root in the navigable tree.
     * @see #isFileSystemRoot
     */
    @Override
    public boolean isRoot(File file) {
        if (isVirtualFileItem(file)) {
            return isVirtualRoot(file.toPath());
        }
        return this.defaultFileSystemView.isRoot(file);
    }

    private File[] getOSRoots() {
        File[] osRoots = this.defaultFileSystemView.getRoots();
        if (SystemUtils.IS_OS_WINDOWS) {
            try {
                Class<?> nativeOsFileClass = Class.forName("sun.awt.shell.ShellFolder");
                osRoots = (File[]) nativeOsFileClass.getMethod("get", String.class).invoke(null, "fileChooserComboBoxFolders");
            } catch (Exception ignored) {
                //leave default
            }
        }
        return osRoots;
    }

    /**
     * Gets all root directories on this system. For example, on OpenStack Swift, this would be the container directories.
     */
    @Override
    public File[] getRoots() {
        File[] defaultRoots = getOSRoots();

        Collection<VirtualFileSystemHelper> virtualFileSystemViews = this.vfsFileSystemViews.values();
        File[] roots = new File[defaultRoots.length + virtualFileSystemViews.size()];
        System.arraycopy(defaultRoots, 0, roots, 0, defaultRoots.length);
        Iterator<VirtualFileSystemHelper> it = virtualFileSystemViews.iterator();
        int index = defaultRoots.length;
        while (it.hasNext()) {
            VirtualFileSystemHelper value = it.next();
            Path rootPath = value.getRoot();
            roots[index++] = new NioFile(rootPath);
        }
        return roots;
    }

    /**
     * Gets the list of shown (i.e. not hidden) files.
     *
     * @param dir           the target dir
     * @param useFileHiding the hiding flag
     * @return the list of files
     */
    @Override
    public File[] getFiles(File dir, boolean useFileHiding) {
        if (isVirtualFileItem(dir)) {
            return getVirtualFiles(dir.toPath(), useFileHiding);
        }
        File[] localFiles = this.defaultFileSystemView.getFiles(dir, useFileHiding);
        return fixPaths(localFiles, useFileHiding);
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
        if (dir == null) {
            return null;
        }
        if (isVirtualFileItem(dir)) {
            if (!Files.exists(dir.toPath())) {
                return null;
            }
            Path parentPath = dir.toPath().getParent();
            if (parentPath == null) {
                return null;
            }
            if (isFileSystem(parentPath)) {
                Path p = parentPath;
                if (!Files.exists(p)) {
                    File ppsf = parentPath.getParent().toFile();
                    if (ppsf == null || !isFileSystem(ppsf)) {
                        p = createFileSystemRoot(p.toFile()).toPath();
                    }
                }
                return p.toFile();
            } else {
                return parentPath.toFile();
            }
        }
        return this.defaultFileSystemView.getParentDirectory(dir);
    }

    /**
     * Gets the Home Directory of FSW.
     *
     * @return the home directory
     */
    @Override
    public File getHomeDirectory() {
        return this.defaultFileSystemView.getHomeDirectory();
    }

    /**
     * Return the user's default starting directory for the file chooser.
     *
     * @return a <code>File</code> object representing the default starting folder
     * @since 1.4
     */
    @Override
    public File getDefaultDirectory() {
        return this.defaultFileSystemView.getDefaultDirectory();
    }

    /**
     * Returns whether a file is hidden or not.
     */
    @Override
    public boolean isHiddenFile(File file) {
        return this.defaultFileSystemView.isHiddenFile(file);
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
        if (isVirtualFileItem(dir)) {
            return isVirtualRoot(dir.toPath());
        }
        return this.defaultFileSystemView.isFileSystemRoot(dir);
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
        if (isVirtualFileItem(dir)) {
            return false;
        }
        return this.defaultFileSystemView.isDrive(dir);
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
        if (isVirtualFileItem(dir)) {
            return false;
        }
        return this.defaultFileSystemView.isFloppyDrive(dir);
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
        if (isVirtualFileItem(dir)) {
            return false;
        }
        return this.defaultFileSystemView.isComputerNode(dir);
    }

    /**
     * Returns a File object constructed from the given path string.
     */
    @Override
    public File createFileObject(String path) {
        Path filePath = NioPaths.get(path);
        return filePath.toFile();
    }

    /**
     * Returns a File object constructed in dir from the given filename.
     */
    @Override
    public File createFileObject(File dir, String filename) {
        Path filePath = dir.toPath().resolve(filename);
        return filePath.toFile();
    }

    /**
     * Creates a new <code>File</code> object for <code>f</code> with correct behavior for a file system root directory.
     *
     * @param file a <code>File</code> object representing a file system root directory, for example "/" on Unix or "C:\" on Windows.
     * @return a new <code>File</code> object
     * @since 1.4
     */
    @Override
    protected File createFileSystemRoot(File file) {
        throw new UnsupportedOperationException();
    }

    /**
     * Icon for a file, directory, or folder as it would be displayed in a system file browser. Example from Windows: the "M:\" directory displays a CD-ROM icon.
     * <p>
     * The default implementation gets information from the NioShellFolder class.
     *
     * @param file a <code>File</code> object
     * @return an icon as it would be displayed by a native file chooser
     * @see JFileChooser#getIcon
     * @since 1.4
     */
    @Override
    public Icon getSystemIcon(File file) {
        if (isVirtualFileItem(file)) {
            AbstractRemoteFileSystem remoteFileSystem = (AbstractRemoteFileSystem) file.toPath().getFileSystem();
            if (remoteFileSystem.getRoot().equals(file.toPath())) {
                return vfsRootIcon;
            }
            if (Files.isDirectory(file.toPath())) {
                return vfsDirectoryIcon;
            }
            return vfsFileIcon;
        }
        return this.defaultFileSystemView.getSystemIcon(file);
    }

    /**
     * Name of a file, directory, or folder as it would be displayed in a system file browser. Example from Windows: the "M:\" directory displays as "CD-ROM (M:)"
     * <p>
     * The default implementation gets information from the NioShellFolder class.
     *
     * @param file a <code>File</code> object
     * @return the file name as it would be displayed by a native file chooser
     * @see JFileChooser#getName
     * @since 1.4
     */
    @Override
    public String getSystemDisplayName(File file) {
        if (isVirtualFileItem(file)) {
            return file.toPath().getFileName().toString();
        }
        return this.defaultFileSystemView.getSystemDisplayName(file);
    }

    /**
     * Type description for a file, directory, or folder as it would be displayed in a system file browser. Example from Windows: the "Desktop" folder is described as "Desktop".
     * <p>
     * Override for platforms with native NioShellFolder implementations.
     *
     * @param file a <code>File</code> object
     * @return the file type description as it would be displayed by a native file chooser or null if no native information is available.
     * @see JFileChooser#getTypeDescription
     * @since 1.4
     */
    @Override
    public String getSystemTypeDescription(File file) {
        if (isVirtualFileItem(file)) {
            return null;
        }
        return this.defaultFileSystemView.getSystemTypeDescription(file);
    }

    /**
     * Returns true if the file (directory) can be visited.
     * Returns false if the directory cannot be traversed.
     *
     * @param file the <code>File</code>
     * @return <code>true</code> if the file/directory can be traversed, otherwise <code>false</code>
     * @see JFileChooser#isTraversable
     * @see javax.swing.filechooser.FileView#isTraversable
     * @since 1.4
     */
    @Override
    public Boolean isTraversable(File file) {
        if (isVirtualFileItem(file)) {
            return isFileSystemRoot(file) || isComputerNode(file) || file.isDirectory();
        }
        return this.defaultFileSystemView.isTraversable(file);
    }

    /**
     * Converts given file paths from native class for OS used by FSW (which is considered to not use NIO API) to base {@code File}.
     * Fix the issue with large paths (>256 characters) on Windows OS. (the goal)
     */
    private File[] fixPaths(File[] filesPaths, boolean useFileHiding) {
        List<File> files = new ArrayList<>();
        for (File filePath : filesPaths) {
            if (!useFileHiding || !isHiddenFile(filePath)) {
                String windowsShareStartPath = "\\\\";
                if (filePath.getPath().startsWith(windowsShareStartPath)) { // is a Windows network path
                    String[] fParts = filePath.getPath().split(windowsShareStartPath);
                    if (fParts.length == 4) {
                        String host = fParts[2];
                        String share = fParts[3];
                        filePath = NioPaths.get(windowsShareStartPath + host + '\\' + share).toFile();
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
     * Checks whether the given file path is VFS path.
     *
     * @param file the path
     * @return {@code True} if the given file path is VFS path
     */
    private boolean isVirtualFileItem(File file) {
        String scheme = file.toURI().getScheme();
        for (VirtualFileSystemHelper vfsFileSystemView : this.vfsFileSystemViews.values()) {
            URI uriRoot = vfsFileSystemView.getRoot().toUri();
            if (uriRoot.getScheme().equals(scheme)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given path represents a File System
     *
     * @param path the path
     * @return {@code True} if the given file path represents a File System
     */
    private boolean isFileSystem(Path path) {
        return !(Files.isSymbolicLink(path) && Files.isDirectory(path));
    }

    /**
     * Gets the list of shown (i.e. not hidden) VFS files.
     *
     * @param dirPath       the target dir path
     * @param useFileHiding the hiding flag
     * @return the list of VFS files
     */
    private File[] getVirtualFiles(Path dirPath, boolean useFileHiding) {
        String pathName = dirPath.toString();
        DirectoryStream<Path> stream = null;
        try {
            VirtualFileSystemHelper vfsFileSystemView = this.vfsFileSystemViews.get(pathName);
            if (vfsFileSystemView != null) {
                return vfsFileSystemView.getRootDirectories();
            }
            List<File> files = new ArrayList<>();
            if (!isVirtualRoot(dirPath) && Files.isDirectory(dirPath)) {
                DirectoryStream.Filter<Path> filter = entry -> !(useFileHiding && Files.isHidden(entry));
                stream = Files.newDirectoryStream(dirPath, filter);
                for (Path path : stream) {
                    files.add(path.toFile());
                }
                return files.toArray(new File[0]);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to get files. Details: " + ex.getMessage(), ex);
            notifyVFSError(ex);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Unable to close the stream. Details: " + ex.getMessage(), ex);
                }
            }
        }
        return new File[0];
    }
}
