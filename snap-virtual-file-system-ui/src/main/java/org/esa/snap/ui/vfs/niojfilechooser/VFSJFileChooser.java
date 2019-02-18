package org.esa.snap.ui.vfs.niojfilechooser;

import javax.swing.JFileChooser;

/**
 * File Chooser provider for VFS.
 * Provide a custom File Chooser using current File Chooser.
 *
 * @author Adrian Drăghici
 */
public final class VFSJFileChooser {

    private VFSJFileChooser() {
    }

    public static JFileChooser getJFileChooser(JFileChooser target) {
        try {
            if (target == null ) {
                target = new JFileChooser();
            }
            target.setFileSystemView(NioFileSystemView.getFileSystemView());
            target.updateUI();
        } catch (Exception ignored){
        }
        return target;
    }
}
