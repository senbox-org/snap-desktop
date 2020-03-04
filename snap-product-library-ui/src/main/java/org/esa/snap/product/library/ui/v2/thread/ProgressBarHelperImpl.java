package org.esa.snap.product.library.ui.v2.thread;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.beans.PropertyChangeListener;

/**
 * Created by jcoravu on 12/9/2019.
 */
public abstract class ProgressBarHelperImpl implements ProgressBarHelper {

    public static final String VISIBLE_PROGRESS_BAR = "visibleProgressBar";

    private final JButton stopButton;
    private final JProgressBar progressBar;
    private final JLabel messageLabel;

    private int currentThreadId;

    public ProgressBarHelperImpl(int progressBarWidth, int progressBarHeight) {
        this.messageLabel = new JLabel("", JLabel.CENTER);

        Dimension progressBarSize = new Dimension(progressBarWidth, progressBarHeight);
        this.progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
        this.progressBar.setLayout(new BorderLayout());
        this.progressBar.add(this.messageLabel, BorderLayout.CENTER);
        this.progressBar.setIndeterminate(true);
        this.progressBar.setPreferredSize(progressBarSize);
        this.progressBar.setMinimumSize(progressBarSize);
        this.progressBar.setMaximumSize(progressBarSize);

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
                this.messageLabel.setText(""); // reset the message text
                boolean oldVisible = this.progressBar.isVisible();
                // hide the progress bar
                setProgressPanelVisible(false);
                if (oldVisible) {
                    // the progress bar was visible
                    setParametersEnabledWhileDownloading(true);
                }
                return true;
            }
            return false;
        } else {
            throw new IllegalStateException("The method must be invoked from the AWT dispatch thread.");
        }
    }

    @Override
    public boolean showProgressPanel(int threadId, String message) {
        if (EventQueue.isDispatchThread()) {
            if (this.currentThreadId == threadId) {
                if (message != null) {
                    this.messageLabel.setText(message); // the message may be null or empty
                }
                boolean oldVisible = this.progressBar.isVisible();
                // show the progress bar
                setProgressPanelVisible(true);
                if (!oldVisible) {
                    // the progress bar was hidden
                    setParametersEnabledWhileDownloading(false);
                }
                return true;
            }
            return false;
        } else {
            throw new IllegalStateException("The method must be invoked from the AWT dispatch thread.");
        }
    }

    /**
     * Update the visible text from the progress bar. The progress bar is not displayed if it is hidden.
     * @param threadId
     * @param message
     * @return
     */
    @Override
    public boolean updateProgressBarText(int threadId, String message) {
        if (EventQueue.isDispatchThread()) {
            if (this.currentThreadId == threadId) {
                this.messageLabel.setText(message);
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

    public void addVisiblePropertyChangeListener(PropertyChangeListener changeListener) {
        this.progressBar.addPropertyChangeListener(VISIBLE_PROGRESS_BAR, changeListener);
    }

    private void setProgressPanelVisible(boolean visible) {
        boolean oldVisible = this.progressBar.isVisible();
        this.progressBar.setVisible(visible);
        this.stopButton.setVisible(visible);
        if (oldVisible != visible) {
            this.progressBar.firePropertyChange(VISIBLE_PROGRESS_BAR, oldVisible, visible);
        }
    }
}
