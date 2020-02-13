package org.esa.snap.grapheditor.ui.components.utils;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.grapheditor.ui.components.interfaces.NotificationListener;

import java.util.ArrayList;
import java.util.HashSet;

public class NotificationManager implements ProgressMonitor {
    static private NotificationManager instance = null;

    private HashSet<NotificationListener> listeners = new HashSet<>();
    private ArrayList<Notification> notifications = new ArrayList<>();

    private NotificationManager() {}

    private void notify(Notification n) {
        this.notifications.add(n);
        for (NotificationListener l: listeners) {
            l.notificationIncoming(n);
        }
    }

    public ArrayList<Notification> getNotifications() {
        return notifications;
    }

    public void addNotificationListener(NotificationListener l){
        listeners.add(l);
    }

    public void removeNotificationListener(NotificationListener l) {
        listeners.remove(l);
    }

    public void info(String source, String message) {
        notify(Notification.info(source, message));
    }

    public void warning(String source, String message) {
        notify(Notification.warning(source, message));
    }

    public void error(String source, String message) {
        notify(Notification.error(source, message));
    }

    public void ok(String source, String message) {
        notify(Notification.ok(source, message));
    }

    public void processStart() {
        for (NotificationListener l : listeners) {
            l.processStart();
        }
    }

    public void processEnd() {
        for (NotificationListener l : listeners) {
            l.processEnd();
        }
    }

    public void progress(int value) {
        for (NotificationListener l : listeners) {
            l.progress(value);
        }
    }

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
