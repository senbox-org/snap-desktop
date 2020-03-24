package org.esa.snap.product.library.ui.v2.thread;

import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.EventQueue;

/**
 * Created by jcoravu on 23/8/2019.
 */
public abstract class AbstractProgressTimerRunnable<OutputType> extends AbstractRunnable<OutputType> {

    private final ProgressBarHelper progressPanel;
    private final java.util.Timer timer;
    private final int timerDelayInMilliseconds;
    private final int threadId;

    protected AbstractProgressTimerRunnable(ProgressBarHelper progressPanel, int threadId, int timerDelayInMilliseconds) {
        super();

        this.progressPanel = progressPanel;
        this.threadId = threadId;
        this.timerDelayInMilliseconds = timerDelayInMilliseconds;
        this.timer = (this.timerDelayInMilliseconds > 0) ? new java.util.Timer() : null;
    }

    @Override
    protected final void failedExecuting(Exception exception) {
        GenericRunnable<Exception> runnable = new GenericRunnable<Exception>(exception) {
            @Override
            protected void execute(Exception threadException) {
                if (onHideProgressPanelLater()) {
                    onFailed(threadException);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    @Override
    protected final void successfullyExecuting(OutputType result) {
        GenericRunnable<OutputType> runnable = new GenericRunnable<OutputType>(result) {
            @Override
            protected void execute(OutputType item) {
                if (onHideProgressPanelLater()) {
                    onSuccessfullyFinish(item);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    @Override
    protected final void finishRunning() {
        super.finishRunning();

        stopTimer();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                onFinishRunning();
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    public void cancelRunning() {
        setRunning(false);

        stopTimer();
    }

    @Override
    public final void executeAsync() {
        startTimerIfDefined();

        super.executeAsync();
    }

    protected boolean onHideProgressPanelLater() {
        return this.progressPanel.hideProgressPanel(this.threadId);
    }

    protected void onFinishRunning() {
    }

    protected void onFailed(Exception exception) {
    }

    protected void onSuccessfullyFinish(OutputType result) {
    }

    protected boolean onTimerWakeUp(String message) {
        return this.progressPanel.showProgressPanel(this.threadId, message);
    }

    protected final boolean onUpdateProgressBarText(String message) {
        return this.progressPanel.updateProgressBarText(this.threadId, message);
    }

    protected final boolean isCurrentProgressPanelThread() {
        if (EventQueue.isDispatchThread()) {
            return this.progressPanel.isCurrentThread(this.threadId);
        } else {
            throw new IllegalStateException("The method must be invoked from the current AWT thread.");
        }
    }

    protected final void onShowErrorMessageDialog(JComponent parentDialogComponent, String message, String title) {
        JOptionPane.showMessageDialog(parentDialogComponent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    protected final void updateProgressBarTextLater(String text) {
        GenericRunnable<String> runnable = new GenericRunnable<String>(text) {
            @Override
            protected void execute(String message) {
                onUpdateProgressBarText(message);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void startTimerIfDefined() {
        if (this.timerDelayInMilliseconds > 0) {
            java.util.TimerTask timerTask = new java.util.TimerTask() {
                @Override
                public void run() {
                    if (!isFinished()) {
                        notifyTimerWakeUpLater();
                    }
                }
            };
            this.timer.schedule(timerTask, this.timerDelayInMilliseconds);
        }
    }

    private void stopTimer() {
        if (this.timer != null) {
            this.timer.cancel();
        }
    }

    private void notifyTimerWakeUpLater() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinished()) {
                    onTimerWakeUp(""); // set empty string for progress bar message
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}
