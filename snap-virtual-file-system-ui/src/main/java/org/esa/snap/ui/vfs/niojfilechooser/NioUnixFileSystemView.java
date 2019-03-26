package org.esa.snap.ui.vfs.niojfilechooser;

import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * FileSystemView component for Unix OS.
 * FileSystemView that handles some specific Unix specific concepts.
 *
 * @author Adrian Drăghici
 */
class NioUnixFileSystemView extends NioFileSystemView {

    /**
     * The FileSystemView component for operating system FS
     */
    private static final FileSystemView osFileSystemView = FileSystemView.getFileSystemView();

    NioUnixFileSystemView(List<VFSRemoteFileRepository> vfsRepositories) {
        super(vfsRepositories);
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
    @Override
    public boolean isDrive(File dir) {
        return isFloppyDrive(dir);
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
    @Override
    public boolean isFloppyDrive(File dir) {
        return osFileSystemView.isFloppyDrive(dir);
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
    @Override
    public boolean isComputerNode(File dir) {
        return osFileSystemView.isComputerNode(dir);
    }

    /**
     * Returns all root partitions on this system. For example, on
     * Windows, this would be the "Desktop" folder, while on DOS this
     * would be the A: through Z: drives.
     */
    @Override
    public File[] getRoots() {
        List<File> roots = new ArrayList<>();
        roots.addAll(Arrays.asList(osFileSystemView.getRoots()));
        roots.addAll(Arrays.asList(getVirtualRoots()));
        return roots.toArray(new File[0]);
    }

}

