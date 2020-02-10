package org.esa.snap.grapheditor.ui.components.utils;

import org.opengis.filter.Not;

import java.util.ArrayList;
import java.util.HashSet;

public class NotificationManager {
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

    static public NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }
}
