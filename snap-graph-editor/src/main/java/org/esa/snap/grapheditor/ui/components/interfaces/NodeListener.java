package org.esa.snap.grapheditor.ui.components.interfaces;

import org.esa.snap.grapheditor.ui.components.graph.NodeGui;

/**
 * Simple listener used to notify changes inside the node.
 *
 * @author Martino Ferrari (CS Group)
 */
public interface NodeListener {

    /**
     * Notify that a source NodeGui has been deleted.
     * @param source source of the event
     */
    void sourceDeleted(NodeInterface source);

    /**
     * Notify that a new connection has been created.
     * @param source source of the event
     */
    void connectionAdded(NodeInterface source);

    /**
     * Ask for validation of the node
     * @param node node to be verified
     */
    void validateNode(NodeInterface node);
}
