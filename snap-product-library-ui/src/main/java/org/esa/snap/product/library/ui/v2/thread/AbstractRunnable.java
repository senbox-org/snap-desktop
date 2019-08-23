package org.esa.snap.product.library.ui.v2.thread;

import org.esa.snap.product.library.v2.IThread;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 23/8/2019.
 */
public abstract class AbstractRunnable<OutputType> implements Runnable, IThread {

    private static final Logger logger = Logger.getLogger(AbstractRunnable.class.getName());

    private Boolean isRunning;

    protected AbstractRunnable() {
    }

    protected abstract OutputType execute() throws Exception;

    protected abstract String getExceptionLoggingMessage();

    @Override
    public boolean isRunning() {
        synchronized (this) {
            return (this.isRunning != null && this.isRunning.booleanValue());
        }
    }

    @Override
    public final void run() {
        try {
            startRunning();

            OutputType result = execute();

            if (!isStopped()) {
                successfullyExecuting(result);
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, getExceptionLoggingMessage(), exception);

            if (!isStopped()) {
                failedExecuting(exception);
            }
        } finally {
            stopRunning();
        }
    }

    protected void failedExecuting(Exception exception) {
    }

    protected void successfullyExecuting(OutputType result) {
    }

    public void executeAsync() {
        Thread thread = new Thread(this);
        thread.start(); // start the thread
    }

    public void stopRunning() {
        synchronized (this) {
            this.isRunning = false;
        }
    }

    protected final boolean isStopped() {
        synchronized (this) {
            return (this.isRunning != null && !this.isRunning.booleanValue());
        }
    }

    private void startRunning() {
        synchronized (this) {
            this.isRunning = true;
        }
    }
}
