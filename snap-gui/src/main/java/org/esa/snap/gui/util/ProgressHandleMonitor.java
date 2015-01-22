package org.esa.snap.gui.util;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;

/**
 * A progress monitor that notifies a {@code ProgressHandle} instance (of the NetBeans Progress API).
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class ProgressHandleMonitor implements ProgressMonitor, Cancellable {
    private ProgressHandle progressHandle;
    private Cancellable cancellable;
    private boolean canceled;
    private int workUnitI;
    private double workUnitD;
    private int totalWork;

    public static ProgressHandleMonitor create(String displayName, Cancellable cancellable) {
        ProgressHandleMonitor progressMonitor = new ProgressHandleMonitor(cancellable);
        ProgressHandle progressHandle = ProgressHandleFactory.createHandle(displayName, progressMonitor);
        progressMonitor.setProgressHandle(progressHandle);
        return progressMonitor;
    }

    public ProgressHandleMonitor(ProgressHandle progressHandle) {
        Assert.notNull(progressHandle);
        this.progressHandle = progressHandle;
    }

    private ProgressHandleMonitor(Cancellable cancellable) {
        Assert.notNull(cancellable);
        this.cancellable = cancellable;
    }

    /**
     * @return The progress handle.
     */
    public ProgressHandle getProgressHandle() {
        return progressHandle;
    }

    /**
     * Sets the progress handle which is assumed to be started.
     *
     * @param progressHandle The progress handle.
     */
    public void setProgressHandle(ProgressHandle progressHandle) {
        Assert.notNull(progressHandle);
        this.progressHandle = progressHandle;
    }

    @Override
    public void beginTask(String taskName, int totalWork) {
        this.totalWork = totalWork;
        if (progressHandle == null) {
            progressHandle = ProgressHandleFactory.createHandle(taskName, this);
            progressHandle.start(totalWork);
        } else {
            progressHandle.start(totalWork);
            progressHandle.setDisplayName(taskName);
            //progressHandle.switchToDeterminate(totalWork);
        }
        workUnitI = 0;
        workUnitD = 0.0;
    }

    @Override
    public void done() {
        Assert.notNull(progressHandle);
        progressHandle.finish();
    }

    @Override
    public void internalWorked(double work) {
        workUnitD += work;
        int i = (int) workUnitD;
        if (i > workUnitI) {
            workUnitI = i;
            progressHandle.progress(workUnitI);
        }
        System.out.println("totalWork = " + totalWork + ", workUnitD = " + workUnitD + ", workUnitI = " + workUnitI + ", work = " + work);
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public void setTaskName(String taskName) {
        Assert.notNull(progressHandle);
        progressHandle.setDisplayName(taskName);
    }

    @Override
    public void setSubTaskName(String subTaskName) {
        Assert.notNull(progressHandle);
        progressHandle.progress(subTaskName);
    }

    @Override
    public void worked(int work) {
        internalWorked(work);
    }

    @Override
    public boolean cancel() {
        if (cancellable != null) {
            setCanceled(cancellable.cancel());
        } else {
            setCanceled(true);
        }
        return isCanceled();
    }
}
