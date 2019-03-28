package org.esa.snap.ui.vfs.niojfilechooser;

import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepositoriesController;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.ui.file.chooser.CopyOfVFSNioFileSystemView;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File Chooser provider for VFS.
 * Provide a custom File Chooser using current File Chooser.
 *
 * @author Adrian Drăghici
 */
public final class VFSJFileChooser {

    private static Logger logger = Logger.getLogger(VFSJFileChooser.class.getName());

    /**
     * Creates the new File Chooser provider for VFS.
     */
    private VFSJFileChooser() {
    }

    /**
     * Gets the updated File Chooser with custom FileSystemView for VFS integration.
     *
     * @param target The File Chooser to update
     * @return The updated File Chooser
     */
    public static JFileChooser getJFileChooser(JFileChooser target) {
        try {
            List<VFSRemoteFileRepository> vfsRepositories = VFSRemoteFileRepositoriesController.getVFSRemoteFileRepositories();
            target.setFileSystemView(NioFileSystemView.getFileSystemView(vfsRepositories));
            target.updateUI();
            UIManager.put("FileChooser.readOnly", true);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to update the JFileChooser with custom FileSystemView compatible with VFS. Details: " + ex.getMessage());
        }
        return target;
    }
}
