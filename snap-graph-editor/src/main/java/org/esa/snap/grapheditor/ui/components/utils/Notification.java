package org.esa.snap.grapheditor.ui.components.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Simple notification class, with priority level.
 *
 * @author Martino Ferrari
 */
public class Notification {
    /**
     * Notification priority level enumerator:
     *  - info: low priority information
     *  - warning: low priority problem (e.g. incomplete node)
     *  - error: ops, something small bad!
     *  - ok: yes!
     */
    public enum Level {
        info,
        error,
        warning,
        ok
    }

    private final Level level;
    private final String source;
    private final String message;

    /**
     * Create an error notification.
     *
     * @param source identifier of the sender
     * @param message error message
     * @return the notification
     */
    static public Notification error(@NotNull String source, @NotNull String message) {
        return new Notification(Level.error, source, message);
    }

    /**
     * Create a warning notification.
     *
     * @param source identifier of the sender
     * @param message warning message
     * @return the notification
     */
    static public Notification warning(@NotNull String source, @NotNull String message) {
        return new Notification(Level.warning, source, message);
    }

    /**
     * Create a validation notification.
     *
     * @param source identifier of the sender
     * @param message congratulation message :)
     * @return the notification
     */
    static public Notification ok(@NotNull String source, @NotNull String message) {
        return new Notification(Level.ok, source, message);
    }

    /**
     * Create an informative notification.
     * @param source identifier of the sender
     * @param message message
     * @return the notification
     */
    static Notification info(@NotNull String source, @NotNull String message) {
        return new Notification(Level.info, source, message);
    }

    /**
     * Private constructor.
     * @param level notification level
     * @param source identifier of the sender
     * @param message the message
     */
    private Notification(Level level, String source, String message) {
        this.level = level;
        this.source = source;
        this.message = message;
    }

    /**
     * Get the notification level.
     * @return the priority level
     */
    public Level getLevel(){
        return level;
    }

    /**
     * Get the sender identifier.
     * @return the sender id string
     */
    public String getSource(){
        return source;
    }

    /**
     * Get the content of the notification.
     * @return the message
     */
    public String getMessage() {
        return  message;
    }

}
