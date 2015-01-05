package org.esa.snap.netbeans.docwin;

import java.util.List;

/**
 * A container for windows of type {@code T} which are usually {@code TopComponent}s.
 *
 * @author Norman Fomferra
 * @since 1.0
 */
public interface WindowContainer<T> {

    /**
     * @return The selected window, or {@code null}.
     */
    T getSelectedWindow();

    /**
     * @return The list of opened windows. List may be empty.
     */
    List<T> getOpenedWindows();
}
