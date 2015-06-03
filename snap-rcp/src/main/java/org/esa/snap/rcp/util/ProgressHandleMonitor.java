package org.esa.snap.rcp.util;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;
import org.openide.util.RequestProcessor;

/**
 * A progress monitor that notifies a {@code ProgressHandle} instance (of the NetBeans Progress API).
 * <p>
 * Use case 1:
 * <pre>
 *     ProgressHandleMonitor pm = ProgressHandleMonitor.create("Training");
 *     Runnable operation = () -> {
 *         pm.beginTask("Classifier training...", 100);
 *         try {
 *             session.startTraining(queryPatches, pm);
 *         } catch (Exception e) {
 *             SnapApp.getDefault().handleError("Failed to train classifier", e);
 *         } finally {
 *             pm.done();
 *         }
 *    };
 *    ProgressUtils.runOffEventThreadWithProgressDialog(operation, "Extracting Features", pm.getProgressHandle(), true, 50, 1000);
 * </pre>
 *
 * <p>
 * Use case 2:
 * <pre>
 *    RequestProcessor.getDefault().post(() -> {
 *        ProgressHandle handle = ProgressHandleFactory.createHandle("Performing time consuming task");
 *        ProgressMonitor pm = new ProgressHandleMonitor(handle);
 *        performTimeConsumingTask(pm);
 *    });
 * </pre>
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class ProgressHandleMonitor implements ProgressMonitor, Cancellable {

    private static final int F = 100;

    private ProgressHandle progressHandle;
    private Cancellable cancellable;
    private boolean canceled;
    private int totalWorkUnits;
    private int currentWorkUnits;
    private double currentWorkUnitsRational;

    public static ProgressHandleMonitor create(String displayName) {
        return create(displayName, null);
    }

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
        this.cancellable = cancellable;
    }

    /**
     * @return The progress handle.
     */
    public ProgressHandle getProgressHandle() {
        return progressHandle;
    }

    /**
     * @param progressHandle The progress handle.
     */
    public void setProgressHandle(ProgressHandle progressHandle) {
        Assert.notNull(progressHandle);
        this.progressHandle = progressHandle;
    }

    @Override
    public void beginTask(String taskName, int totalWork) {
        this.totalWorkUnits = F * totalWork;
        this.currentWorkUnits = 0;
        this.currentWorkUnitsRational = 0.0;
        if (progressHandle == null) {
            progressHandle = ProgressHandleFactory.createHandle(taskName, this);
            progressHandle.start(this.totalWorkUnits);
        } else {
            try {
                progressHandle.start(this.totalWorkUnits);
            } catch (java.lang.IllegalStateException e) {
                // if already started, use fall back
                progressHandle.switchToDeterminate(this.totalWorkUnits);
            }
            progressHandle.setDisplayName(taskName);
        }
    }

    @Override
    public void done() {
        Assert.notNull(progressHandle);
        progressHandle.finish();
    }

    @Override
    public void internalWorked(double work) {
        currentWorkUnitsRational += F * work;
        int i = (int) currentWorkUnitsRational;
        if (i > currentWorkUnits) {
            currentWorkUnits = i;
            progressHandle.progress(currentWorkUnits);
        }
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
