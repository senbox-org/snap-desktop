package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.esa.snap.remote.products.repository.ThreadStatus;

/**
 * Created by jcoravu on 29/1/2020.
 */
public abstract class AbstractBackgroundDownloadRunnable implements Runnable, ThreadStatus {

    private Boolean isRunning;

    protected AbstractBackgroundDownloadRunnable() {
    }

    @Override
    public boolean isRunning() {
        synchronized (this) {
            return (this.isRunning != null && this.isRunning.booleanValue());
        }
    }

    public void stopRunning() {
        synchronized (this) {
            this.isRunning = false;
        }
    }

    protected void startRunning() {
        synchronized (this) {
            this.isRunning = true;
        }
    }
}
