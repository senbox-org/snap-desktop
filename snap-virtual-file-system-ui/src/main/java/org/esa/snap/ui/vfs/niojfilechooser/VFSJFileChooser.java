package org.esa.snap.ui.vfs.niojfilechooser;

import javax.swing.JFileChooser;
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
            if (target == null) {
                target = new JFileChooser();
            }
            target.setFileSystemView(NioFileSystemView.getFileSystemView());
            target.updateUI();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to update the JFileChooser with custom FileSystemView compatible with VFS. Details: " + ex.getMessage());
        }
        return target;
    }
}
