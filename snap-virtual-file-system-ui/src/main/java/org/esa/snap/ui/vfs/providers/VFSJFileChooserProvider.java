package org.esa.snap.ui.vfs.providers;


import org.esa.snap.ui.vfs.niojfilechooser.VFSJFileChooser;
import org.esa.snap.core.vfsspi.VFSProvider;

import javax.swing.*;

/**
 * File Chooser Provider for VFS.
 * VFS Service provider for access VFSJFileChooser methods from outside of snap-virtual-file-system-ui module via SPI.
 *
 * @author Adrian Drăghici
 */
public final class VFSJFileChooserProvider implements VFSProvider {

    private static final String SERVICE_METHOD_GET_JFILE_CHOOSER = "getJFileChooser";

    public VFSJFileChooserProvider() {
    }

    @Override
    public <R> R runVFSService(String serviceMethodName, Class<? extends R> returnType, Object... params) {
        switch (serviceMethodName) {
            case SERVICE_METHOD_GET_JFILE_CHOOSER:
                if (params.length == 1) {
                    return returnType.cast(VFSJFileChooser.getJFileChooser((JFileChooser) params[0]));
                }
                break;
        }
        return null;
    }

}
