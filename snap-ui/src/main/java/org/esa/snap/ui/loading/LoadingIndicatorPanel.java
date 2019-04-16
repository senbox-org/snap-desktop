package org.esa.snap.ui.loading;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Created by jcoravu on 28/12/2018.
 */
public class LoadingIndicatorPanel extends JPanel implements ILoadingIndicator {

    private final Object lockObject;
    private final IComponentsEnabled componentsEnabled;
    private final CircularProgressIndicatorLabel circularProgressLabel;
    private final JLabel messageLabel;

    private boolean isRunning;
    private int currentThreadId;

    public LoadingIndicatorPanel() {
        this(null);
    }

    public LoadingIndicatorPanel(IComponentsEnabled componentsEnabled) {
        super(new GridBagLayout());

        this.componentsEnabled = componentsEnabled;

        this.circularProgressLabel = new CircularProgressIndicatorLabel();
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER, 1, 1, new Insets(1, 1, 1, 1));
        add(this.circularProgressLabel, c);

        this.messageLabel = new JLabel();
        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 1, 1, new Insets(1, 3, 1, 3));
        add(this.messageLabel, c);

        super.setBorder(new LineBorder(Color.gray, 1, false));
        super.setOpaque(true); // the loading indicator panel is not transparent

        stopAndHide();

        this.isRunning = false;
        this.lockObject = new Object();
        this.currentThreadId = 0;
    }

    @Override
    public final void setOpaque(boolean aIsOpaque) {
        // do nothing
    }

    @Override
    public final void setBorder(Border aBorder) {
        // do nothing
    }

    @Override
    public final void removeNotify() {
        setRunningAndIncreaseThreadId(false);

        super.removeNotify();
    }

    @Override
    public final Color getBackground() {
        return Color.white;
    }

    @Override
    public final boolean isRunning(int threadId) {
        synchronized (this.lockObject) {
            return this.isRunning && (this.currentThreadId == threadId);
        }
    }

    @Override
    public final boolean onDisplay(int threadId, String messageToDisplay) {
        if (isRunning(threadId)) {
            try {
                setEnabledControls(false);
            } finally {
                showAndStart(threadId, messageToDisplay);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onHide(int threadId) {
        if (isRunning(threadId)) {
            setRunningAndIncreaseThreadId(false);
            try {
                setEnabledControls(true);
            } finally {
                stopAndHide();
            }
            return true;
        }
        return false;
    }

    public final int getNewCurrentThreadId() {
        return setRunningAndIncreaseThreadId(true);
    }

    private void showAndStart(int threadId, String messageToDisplay) {
        boolean visibleMessage = (messageToDisplay != null && messageToDisplay.trim().length() > 0);
        this.messageLabel.setText(messageToDisplay);
        this.messageLabel.setVisible(visibleMessage);

        Runnable action = new IntRunnable(threadId) {
            @Override
            protected void run(int inputThreadId) {
                if (isRunning(inputThreadId)) {
                    circularProgressLabel.setRunning(true);
                    setVisible(true);
                }
            }
        };
        SwingUtilities.invokeLater(action);
    }

    private int setRunningAndIncreaseThreadId(boolean value) {
        synchronized (this.lockObject) {
            this.isRunning = value;
            return ++this.currentThreadId;
        }
    }

    private void stopAndHide() {
        this.circularProgressLabel.setRunning(false);
        setVisible(false);
    }

    private void setEnabledControls(boolean enabled) {
        if (this.componentsEnabled != null) {
            this.componentsEnabled.setComponentsEnabled(enabled);
        }
    }

    private static abstract class IntRunnable implements Runnable {

        private final int value;

        IntRunnable(int value) {
            this.value = value;
        }

        protected abstract void run(int value);

        @Override
        public void run() {
            run(this.value);
        }
    }
}
