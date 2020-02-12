package org.esa.snap.grapheditor.ui.components.utils;

import org.esa.snap.grapheditor.ui.components.graph.NodeGui;

public interface NodeListener {
    /**
     * Update the inputs node of the connected nodes.
     * @param source source of the event
     */
    void outputChanged(NodeGui source);

    /**
     * Notify that a source NodeGui has been deleted.
     * @param source source of the event
     */
    void sourceDeleted(NodeGui source);

    void connectionAdded(NodeGui source);
}
