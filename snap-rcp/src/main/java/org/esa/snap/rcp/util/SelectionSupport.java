package org.esa.snap.rcp.util;

import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;

/**
 * Utility which allows for registering handlers which are informed about single selection changes.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public interface SelectionSupport<T> {
    /**
     * Adds a new handler.
     *
     * @param handler The handler.
     */
    void addHandler(@NonNull Handler<T> handler);

    /**
     * Removes an existing handler.
     *
     * @param handler The handler.
     */
    void removeHandler(@NonNull Handler<T> handler);

    /**
     * Handles single selection changes.
     *
     * @param <T> The type of the selection
     */
    interface Handler<T> {
        /**
         * Called if a selection changed.
         *
         * @param oldValue The old selection, or {@code null} if no such exists
         * @param newValue The new selection, or {@code null} if no such exists
         */
        void selectionChange(@NullAllowed T oldValue, @NullAllowed T newValue);
    }
}
