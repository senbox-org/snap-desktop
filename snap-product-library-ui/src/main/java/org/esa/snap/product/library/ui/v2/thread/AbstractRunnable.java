package org.esa.snap.product.library.ui.v2.thread;

import org.esa.snap.remote.products.repository.ThreadStatus;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 23/8/2019.
 */
public abstract class AbstractRunnable<OutputType> implements Runnable, ThreadStatus {

    private static final Logger logger = Logger.getLogger(AbstractRunnable.class.getName());

    private Boolean isRunning;

    protected AbstractRunnable() {
    }

    protected abstract OutputType execute() throws Exception;

    protected abstract String getExceptionLoggingMessage();

    @Override
    public final boolean isRunning() {
        synchronized (this) {
            return (this.isRunning != null && this.isRunning.booleanValue());
        }
    }

    @Override
    public final void run() {
        try {
            startRunning();

            OutputType result = execute();

            if (isRunning()) {
                successfullyExecuting(result);
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, getExceptionLoggingMessage(), exception);

            if (isRunning()) {
                failedExecuting(exception);
            }
        } finally {
            finishRunning();
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

    protected void finishRunning() {
        setRunning(false);
    }

    private void startRunning() {
        setRunning(true);
    }

    protected final void setRunning(boolean isRunning) {
        synchronized (this) {
            this.isRunning = isRunning;
        }
    }
}
