package org.esa.snap.grapheditor.ui.components.utils;

import org.opengis.filter.Not;

public class Notification {
    public enum Level {
        info,
        error,
        warning,
        ok
    }

    private final Level level;
    private final String source;
    private final String message;

    static public Notification error(String source, String message) {
        return new Notification(Level.error, source, message);
    }

    static public Notification warning(String source, String message) {
        return new Notification(Level.warning, source, message);
    }

    static public Notification ok(String source, String message) {
        return new Notification(Level.ok, source, message);
    }

    static public Notification info(String source, String message) {
        return new Notification(Level.info, source, message);
    }

    private Notification(Level level, String source, String message) {
        this.level = level;
        this.source = source;
        this.message = message;
    }

    public Level getLevel(){
        return level;
    }

    public String getSource(){
        return source;
    }

    public String getMessage() {
        return  message;
    }

}
