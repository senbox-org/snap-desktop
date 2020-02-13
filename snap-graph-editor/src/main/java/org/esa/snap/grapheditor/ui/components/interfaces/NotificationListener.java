package org.esa.snap.grapheditor.ui.components.interfaces;

import org.esa.snap.grapheditor.ui.components.utils.Notification;

/**
 * Listener for incoming notification as well as to manage the progress of the processes.
 *
 * @author Martino Ferrari (CS Group)
 */
public interface NotificationListener {
    /**
     * New incoming notification.
     * @param n the incoming notification
     */
    void notificationIncoming(Notification n);

    /**
     * Starts the process progress monitoring.
     */
    void processStart();

    /**
     * Ends the process progress monitoring.
     */
    void processEnd();

    /**
     * Update the progress of the process.
     * @param value current process progress
     */
    void progress(int value);
}
