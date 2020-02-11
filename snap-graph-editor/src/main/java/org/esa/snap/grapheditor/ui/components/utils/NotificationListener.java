package org.esa.snap.grapheditor.ui.components.utils;

public interface NotificationListener {

    void notificationIncoming(Notification n);

    void processStart();

    void processEnd();

    void progress(int value);
}
