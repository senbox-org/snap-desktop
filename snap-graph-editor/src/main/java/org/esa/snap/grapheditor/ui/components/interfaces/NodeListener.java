package org.esa.snap.grapheditor.ui.components.interfaces;

import org.esa.snap.grapheditor.ui.components.graph.NodeGui;

/**
 * Simple listener used to notify changes inside the node.
 *
 * @author Martino Ferrari (CS Group)
 */
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

    /**
     * Notify that a new connection has been created.
     * @param source source of the event
     */
    void connectionAdded(NodeGui source);
}
