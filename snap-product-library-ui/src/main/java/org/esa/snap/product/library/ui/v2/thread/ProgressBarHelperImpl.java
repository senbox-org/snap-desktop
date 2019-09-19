package org.esa.snap.product.library.ui.v2.thread;

import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import java.awt.Dimension;
import java.awt.EventQueue;

/**
 * Created by jcoravu on 12/9/2019.
 */
public abstract class ProgressBarHelperImpl implements ProgressBarHelper {

    private final JButton stopButton;
    private final JProgressBar progressBar;

    private int currentThreadId;

    public ProgressBarHelperImpl(int progressBarWidth, int progressBarHeight) {
        this.progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
        this.progressBar.setIndeterminate(true);
        this.progressBar.setPreferredSize(new Dimension(progressBarWidth, progressBarHeight));
        this.progressBar.setMinimumSize(new Dimension(progressBarWidth, progressBarHeight));
        this.progressBar.setMaximumSize(new Dimension(progressBarWidth, progressBarHeight));

        Dimension buttonSize = new Dimension(progressBarHeight, progressBarHeight);
        this.stopButton = RepositorySelectionPanel.buildButton("/org/esa/snap/productlibrary/icons/stop20.gif", null, buttonSize, 1);
        this.stopButton.setToolTipText("Stop");

        this.currentThreadId = 0;

        setProgressPanelVisible(false);
    }

    protected abstract void setParametersEnabledWhileDownloading(boolean enabled);

    @Override
    public boolean isCurrentThread(int threadId) {
        if (EventQueue.isDispatchThread()) {
            return (this.currentThreadId == threadId);
        } else {
            throw new IllegalStateException("The method must be invoked from the AWT dispatch thread.");
        }
    }

    @Override
    public boolean hideProgressPanel(int threadId) {
        if (EventQueue.isDispatchThread()) {
            if (this.currentThreadId == threadId) {
                this.currentThreadId++;
                setProgressPanelVisible(false);
                setParametersEnabledWhileDownloading(true);
                return true;
            }
            return false;
        } else {
            throw new IllegalStateException("The method must be invoked from the AWT dispatch thread.");
        }
    }

    @Override
    public boolean showProgressPanel(int threadId) {
        if (EventQueue.isDispatchThread()) {
            if (this.currentThreadId == threadId) {
                setProgressPanelVisible(true);
                setParametersEnabledWhileDownloading(false);
                return true;
            }
            return false;
        } else {
            throw new IllegalStateException("The method must be invoked from the AWT dispatch thread.");
        }
    }

    @Override
    public boolean updateProgressBarText(int threadId, String message) {
        if (EventQueue.isDispatchThread()) {
            if (this.currentThreadId == threadId) {
                this.progressBar.setString(message);
                return true;
            }
            return false;
        } else {
            throw new IllegalStateException("The method must be invoked from the AWT dispatch thread.");
        }
    }

    public JButton getStopButton() {
        return stopButton;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public void hideProgressPanel() {
        if (EventQueue.isDispatchThread()) {
            this.currentThreadId++;
            setProgressPanelVisible(false);
            setParametersEnabledWhileDownloading(true);
        } else {
            throw new IllegalStateException("The method must be invoked from the AWT dispatch thread.");
        }
    }

    public final int incrementAndGetCurrentThreadId() {
        if (EventQueue.isDispatchThread()) {
            return ++this.currentThreadId;
        } else {
            throw new IllegalStateException("The method must be invoked from the AWT dispatch thread.");
        }
    }

    private void setProgressPanelVisible(boolean visible) {
        this.progressBar.setVisible(visible);
        this.stopButton.setVisible(visible);
    }
}
