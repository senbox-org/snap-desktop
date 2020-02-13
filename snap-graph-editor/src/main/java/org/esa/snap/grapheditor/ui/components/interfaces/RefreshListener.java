package org.esa.snap.grapheditor.ui.components.interfaces;

/**
 * Super simple interface use to notify the need to refresh the UI after some types of events.
 *
 * @author Martino Ferrari (CS Group)
 */
public interface RefreshListener {
    /**
     * Tell the listener to refresh the UI.
     */
    void refresh();
}
