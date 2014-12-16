package org.esa.snap.gui.util.progress;

import com.bc.ceres.core.ProgressMonitor;

/**
 * @author Norman Fomferra
 * @since 2.0
 */
public interface ProgressMonitorProvider {
    ProgressMonitor getProgressMonitor();
}
