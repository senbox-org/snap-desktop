package org.esa.snap.grapheditor.ui.components.utils;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.grapheditor.ui.components.interfaces.NotificationListener;

import java.util.HashSet;

/**
 * Simple Notification Manager, used to dispatch the notification to all interested parties. It also implements the
 * ProgressMonitor interface to be compatible with the GraphExecutor.
 *
 * @author Martino Ferrari
 */
public class NotificationManager implements ProgressMonitor {
    static private NotificationManager instance = null;

    private final HashSet<NotificationListener> listeners = new HashSet<>();
    // Maybe for future applications. In case we want to store log history.
    // private ArrayList<Notification> notifications = new ArrayList<>();

    /**
     * Private constructor. Using singleton pattern.
     */
    private NotificationManager() {}

    /**
     * internal function to notify all listeners of a new incoming notification.
     * @param n new notification
     */
    private void notify(Notification n) {
        // this.notifications.add(n); // see line #13
        for (NotificationListener l: listeners) {
            l.notificationIncoming(n);
        }
    }

    /**
     * Add new listener to the notification Manager
     * @param l new listener
     */
    public void addNotificationListener(NotificationListener l){
        listeners.add(l);
    }

    /**
     * Informative notification
     * @param source sender
     * @param message content
     */
    void info(String source, String message) {
        notify(Notification.info(source, message));
    }

    /**
     * Warning notification
     * @param source sender
     * @param message content
     */
    public void warning(String source, String message) {
        notify(Notification.warning(source, message));
    }

    /**
     * Error notification
     * @param source sender
     * @param message content
     */
    public void error(String source, String message) {
        notify(Notification.error(source, message));
    }

    /**
     * Validated notification
     * @param source sender
     * @param message content
     */
    public void ok(String source, String message) {
        notify(Notification.ok(source, message));
    }

    /**
     * A process started.
     */
    void processStart() {
        for (NotificationListener l : listeners) {
            l.processStart();
        }
    }

    /**
     * A process ended.
     */
    void processEnd() {
        for (NotificationListener l : listeners) {
            l.processEnd();
        }
    }

    /**
     * Update the progress of a process.
     * @param value new progress value (0-100)
     */
    void progress(int value) {
        for (NotificationListener l : listeners) {
            l.progress(value);
        }
    }

    /**
     * Get the NotificationManager
     * @return the notification manager instance.
     */
    static public NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    @Override
    public void beginTask(String s, int i) {
        info("Graph Processor", "start task `"+s+"`");
        processStart();
    }

    @Override
    public void done() {
        processEnd();
        info("Graph Processor", "done");
    }

    @Override
    public void internalWorked(double v) {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setCanceled(boolean b) {
        processEnd();
    }

    @Override
    public void setTaskName(String s) {
        info("Graph Processor", "processing `"+s+"`");
    }

    @Override
    public void setSubTaskName(String s) {

    }

    @Override
    public void worked(int i) {
    }
}
