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

    /**
     * The name of service provider method which gets the File Chooser for VFS.
     */
    private static final String SERVICE_METHOD_GET_JFILE_CHOOSER = "getJFileChooser";

    /**
     * Runs the service provider method specified by the given absolute name (package.class.method), with specified parameters and return the result converted to specified type.
     * The service provider method can be 'getJFileChooser'.
     *
     * @param serviceMethodName The absolute name of service provider method
     * @param returnType        The service provider method return result type
     * @param params            The service provider method parameters
     * @param <R>               The generic type of service provider method return result: can be {@code JFileChooser}
     * @return The result of the service provider method execution
     */
    @Override
    public <R> R runVFSService(String serviceMethodName, Class<? extends R> returnType, Object... params) {
        switch (serviceMethodName) {
            case SERVICE_METHOD_GET_JFILE_CHOOSER:
                if (params.length == 1) {
                    return returnType.cast(VFSJFileChooser.getJFileChooser((JFileChooser) params[0]));
                }
                break;
            default:
                break;
        }
        return null;
    }

}
