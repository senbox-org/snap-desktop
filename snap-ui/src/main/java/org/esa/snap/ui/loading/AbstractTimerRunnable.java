package org.esa.snap.ui.loading;

import javax.swing.SwingUtilities;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 31/12/2018.
 */
public abstract class AbstractTimerRunnable<OutputType> implements Runnable {

    private static final Logger logger = Logger.getLogger(AbstractTimerRunnable.class.getName());

    private final Timer timer;
    private final int timerDelayInMiliseconds;
    private final int threadId;
    private final ILoadingIndicator loadingIndicator;

    protected AbstractTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, int timerDelayInMiliseconds) {
        this.loadingIndicator = loadingIndicator;
        this.threadId = threadId;
        this.timerDelayInMiliseconds = timerDelayInMiliseconds;
        this.timer = (this.timerDelayInMiliseconds > 0) ? new Timer() : null;
    }

    protected abstract OutputType execute() throws Exception;

    protected abstract String getExceptionLoggingMessage();

    @Override
    public final void run() {
        try {
            OutputType result = execute();

            GenericRunnable<OutputType> runnable = new GenericRunnable<OutputType>(result) {
                @Override
                protected void execute(OutputType item) {
                    if (loadingIndicator.onHide(threadId)) {
                        onSuccessfullyFinish(item);
                    }
                }
            };
            SwingUtilities.invokeLater(runnable);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, getExceptionLoggingMessage(), exception);

            GenericRunnable<Exception> runnable = new GenericRunnable<Exception>(exception) {
                @Override
                protected void execute(Exception threadException) {
                    if (loadingIndicator.onHide(threadId)) {
                        onFailed(threadException);
                    }
                }
            };
            SwingUtilities.invokeLater(runnable);
        } finally {
            stopTimer();
        }
    }

    protected void onFailed(Exception exception) {
    }

    protected void onSuccessfullyFinish(OutputType result) {
    }

    public final void executeAsync() {
        startTimerIfDefined();

        Thread thread = new Thread(this);
        thread.start(); // start the thread
    }

    private void startTimerIfDefined() {
        if (this.timerDelayInMiliseconds > 0) {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (isRunning()) {
                        timerWakeUp();
                    }
                }
            };
            this.timer.schedule(timerTask, this.timerDelayInMiliseconds);
        }
    }

    protected void onTimerWakeUp(String messageToDisplay) {
        onDisplayLoadingIndicatorMessage(messageToDisplay);
    }

    protected final boolean onDisplayLoadingIndicatorMessage(String messageToDisplay) {
        return this.loadingIndicator.onDisplay(this.threadId, messageToDisplay);
    }

    protected final boolean isRunning() {
        return this.loadingIndicator.isRunning(this.threadId);
    }

    private void stopTimer() {
        if (this.timer != null) {
            this.timer.cancel();
        }
    }

    private void timerWakeUp() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                onTimerWakeUp(null);
            }
        });
    }
}
