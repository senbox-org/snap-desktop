package org.esa.snap.vfs.ui.file.chooser;

import org.esa.snap.vfs.NioFile;
import org.esa.snap.vfs.NioPaths;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
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

public class VirtualFileSystemView extends FileSystemView {

    private static Logger logger = Logger.getLogger(VirtualFileSystemView.class.getName());

    /**
     * The default name for a new directory.
     */
    private static final String NEW_FOLDER_STRING = "New_Folder";

    /**
     * The default name for a next new directory.
     */
    private static final String NEW_FOLDER_NEXT_STRING = "New_Folder_({0})";

    private final FileSystemView defaultFileSystemView;
    private final Map<String, VirtualFileSystemHelper> vfsFileSystemViews;
    private final ImageIcon vfsRootIcon;
    private final ImageIcon vfsDirectoryIcon;
    private final ImageIcon vfsFileIcon;

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

        this.vfsRootIcon = loadImageIcon("org/esa/snap/ui/vfs/niojfilechooser/icons/vfs_root-23x16.png");
        this.vfsDirectoryIcon = loadImageIcon("org/esa/snap/ui/vfs/niojfilechooser/icons/vfs_folder-23x16.png");
        this.vfsFileIcon = loadImageIcon("org/esa/snap/ui/vfs/niojfilechooser/icons/vfs_file-23x16.png");
    }

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

    @Override
    public boolean isFileSystem(File file) {
        if (isVirtualFileItem(file)) {
            return isFileSystem(file.toPath());
        }
        return this.defaultFileSystemView.isFileSystem(file);
    }

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

    @Override
    public boolean isRoot(File file) {
        if (isVirtualFileItem(file)) {
            return isVirtualRoot(file.toPath());
        }
        return this.defaultFileSystemView.isRoot(file);
    }

    @Override
    public File[] getRoots() {
        File[] defaultRoots = this.defaultFileSystemView.getRoots();

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

    @Override
    public File[] getFiles(File dir, boolean useFileHiding) {
        if (isVirtualFileItem(dir)) {
            return getVirtualFiles(dir.toPath(), useFileHiding);
        }
        File[] localFiles = this.defaultFileSystemView.getFiles(dir, useFileHiding);
        return fixPaths(localFiles, useFileHiding);
    }

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

    @Override
    public File getHomeDirectory() {
        return this.defaultFileSystemView.getHomeDirectory();
    }

    @Override
    public File getDefaultDirectory() {
        return this.defaultFileSystemView.getDefaultDirectory();
    }

    @Override
    public boolean isHiddenFile(File file) {
        return this.defaultFileSystemView.isHiddenFile(file);
    }

    @Override
    public boolean isFileSystemRoot(File dir) {
        if (isVirtualFileItem(dir)) {
            return isVirtualRoot(dir.toPath());
        }
        return this.defaultFileSystemView.isFileSystemRoot(dir);
    }

    @Override
    public boolean isDrive(File dir) {
        return this.defaultFileSystemView.isDrive(dir);
    }

    @Override
    public boolean isFloppyDrive(File dir) {
        return this.defaultFileSystemView.isFloppyDrive(dir);
    }

    @Override
    public boolean isComputerNode(File dir) {
        return this.defaultFileSystemView.isComputerNode(dir);
    }

    @Override
    public File createFileObject(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected File createFileSystemRoot(File file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Icon getSystemIcon(File file) {
        if (isVirtualFileItem(file)) {
            AbstractRemoteFileSystem remoteFileSystem = (AbstractRemoteFileSystem) file.toPath().getFileSystem();
            if (remoteFileSystem.getRoot().equals(file.toPath())) {
                return this.vfsRootIcon;
            }
            if (Files.isDirectory(file.toPath())) {
                return this.vfsDirectoryIcon;
            }
            return this.vfsFileIcon;
        }
        return this.defaultFileSystemView.getSystemIcon(file);
    }

    @Override
    public String getSystemDisplayName(File file) {
        if (isVirtualFileItem(file)) {
            return file.toPath().getFileName().toString();
        }
        return this.defaultFileSystemView.getSystemDisplayName(file);
    }

    @Override
    public String getSystemTypeDescription(File file) {
        if (isVirtualFileItem(file)) {
            return null;
        }
        return this.defaultFileSystemView.getSystemTypeDescription(file);
    }

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

    private boolean isVirtualFileItem(File file) {
        String scheme = file.toURI().getScheme();
        Iterator<VirtualFileSystemHelper> it = this.vfsFileSystemViews.values().iterator();
        while (it.hasNext()) {
            VirtualFileSystemHelper vfsfileSystemView = it.next();
            URI uriRoot = vfsfileSystemView.getRoot().toUri();
            if (uriRoot.getScheme().equals(scheme)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFileSystem(Path path) {
        return !(Files.isSymbolicLink(path) && Files.isDirectory(path));
    }

    private File[] getVirtualFiles(Path dirPath, boolean useFileHiding) {
        String pathName = dirPath.toString();
        VirtualFileSystemHelper vfsFileSystemView = this.vfsFileSystemViews.get(pathName);
        if (vfsFileSystemView != null) {
            return vfsFileSystemView.getRootDirectories();
        }

        DirectoryStream<Path> stream = null;
        try {
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

    private static boolean isVirtualRoot(Path path) {
        FileSystem fileSystem = path.getFileSystem();
        if (fileSystem instanceof AbstractRemoteFileSystem) {
            AbstractRemoteFileSystem remoteFileSystem = (AbstractRemoteFileSystem) fileSystem;
            if (remoteFileSystem.getRoot().equals(path)) {
                return true;
            }
        }
        return false;
    }

    private static ImageIcon loadImageIcon(String imagePath) {
        URL imageURL = VirtualFileSystemView.class.getClassLoader().getResource(imagePath);
        return (imageURL == null) ? null : new ImageIcon(imageURL);
    }
}
