package org.esa.snap.vfs.ui.file.chooser;

import org.esa.snap.ui.vfs.niojfilechooser.NioFileSystemView;
import org.esa.snap.ui.vfs.niojfilechooser.NioVFSFileSystemView;
import org.esa.snap.vfs.NioFile;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CopyOfVFSNioFileSystemView extends FileSystemView {

    private static Logger logger = Logger.getLogger(CopyOfVFSNioFileSystemView.class.getName());

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

    private final FileSystemView defaultFileSystemView;
    /**
     * The list of FileSystemView components for VFSs
     */
    private final Map<String, NioVFSFileSystemView> vfsFileSystemViews;

    public CopyOfVFSNioFileSystemView(FileSystemView defaultFileSystemView, List<VFSRemoteFileRepository> vfsRepositories) {
        super();

        this.defaultFileSystemView = defaultFileSystemView;

        this.vfsFileSystemViews = new HashMap<>();
        for (VFSRemoteFileRepository vfsRemoteFileRepository : vfsRepositories) {
            try {
                NioVFSFileSystemView vfsFileSystemView = new NioVFSFileSystemView(vfsRemoteFileRepository);
                String key = vfsFileSystemView.getRoot().toString();
                this.vfsFileSystemViews.put(key, vfsFileSystemView);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unable to initialize " + vfsRemoteFileRepository.getName() + " VFS. Details: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public File getChild(File parent, String fileName) {
        if (parent.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
            File[] children = getFiles(parent, false);
            for (File child : children) {
                if (child.getName().equals(fileName)) {
                    return child;
                }
            }
            return createFileObject(parent, fileName);

        }
        return this.defaultFileSystemView.getChild(parent, fileName);
    }

    @Override
    public boolean isFileSystem(File file) {
        if (file.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
            return isFileSystem(file.toPath());
        }
        return this.defaultFileSystemView.isFileSystem(file);
    }

    private boolean isFileSystem(Path path) {
        return !(Files.isSymbolicLink(path) && Files.isDirectory(path));
    }

    @Override
    public File createNewFolder(File containingDir) throws IOException {
        if (containingDir.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
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
        if (file.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
            return super.isRoot(file);
        }
        return this.defaultFileSystemView.isRoot(file);
    }

    @Override
    public File[] getRoots() {
        File[] defaultRoots = this.defaultFileSystemView.getRoots();

        Collection<NioVFSFileSystemView> virtualFileSystemViews = this.vfsFileSystemViews.values();
        File[] roots = new File[defaultRoots.length + virtualFileSystemViews.size()];
        System.arraycopy(defaultRoots, 0, roots, 0, defaultRoots.length);
        Iterator<NioVFSFileSystemView> it = virtualFileSystemViews.iterator();
        int index = defaultRoots.length;
        while (it.hasNext()) {
            NioVFSFileSystemView value = it.next();
            Path rootPath = value.getRoot();
            roots[index++] = new NioFile(rootPath);
        }
        return roots;
    }

    @Override
    public File[] getFiles(File dir, boolean useFileHiding) {
        if (dir.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
            String pathName = dir.toPath().toString();
            NioVFSFileSystemView vfsFileSystemView = this.vfsFileSystemViews.get(pathName);
            if (vfsFileSystemView != null) {
                return vfsFileSystemView.getRootDirectories();
            }

            DirectoryStream<Path> stream = null;
            try {
                List<File> files = new ArrayList<>();
                if (!isVirtualFileSystemRoot(dir.toPath()) && Files.isDirectory(dir.toPath())) {
                    DirectoryStream.Filter<Path> filter = entry -> !(useFileHiding && Files.isHidden(entry));
                    stream = Files.newDirectoryStream(dir.toPath(), filter);
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
        return this.defaultFileSystemView.getFiles(dir, useFileHiding);
    }

    @Override
    public File getParentDirectory(File dir) {
        if (dir == null) {
            return null;
        }
        if (dir.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
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
    public File createFileObject(String path) {
        return this.defaultFileSystemView.createFileObject(path);
    }

    @Override
    public boolean isHiddenFile(File file) {
        return this.defaultFileSystemView.isHiddenFile(file);
    }

    @Override
    public boolean isFileSystemRoot(File dir) {
        if (dir.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
            return isVirtualFileSystemRoot(dir.toPath());
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
    protected File createFileSystemRoot(File file) {
        throw new IllegalArgumentException("The argument file "+file.toString()+" is not a remote file.");
    }

    @Override
    public Icon getSystemIcon(File file) {
        if (file.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
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

    @Override
    public String getSystemDisplayName(File file) {
        if (file.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
            return file.toPath().getFileName().toString();
        }
        return this.defaultFileSystemView.getSystemDisplayName(file);
    }

    @Override
    public String getSystemTypeDescription(File file) {
        if (file.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
            return null;
        }
        return this.defaultFileSystemView.getSystemTypeDescription(file);
    }

    @Override
    public Boolean isTraversable(File file) {
        if (file.toPath().getFileSystem() instanceof AbstractRemoteFileSystem) {
            return isFileSystemRoot(file) || isComputerNode(file) || file.isDirectory();
        }
        return this.defaultFileSystemView.isTraversable(file);
    }

    private static boolean isVirtualFileSystemRoot(Path path) {
        FileSystem fileSystem = path.getFileSystem();
        if (fileSystem instanceof AbstractRemoteFileSystem) {
            AbstractRemoteFileSystem remoteFileSystem = (AbstractRemoteFileSystem) fileSystem;
            if (remoteFileSystem.getRoot().equals(path)) {
                return true;
            }
        }
        return false;
    }
}
