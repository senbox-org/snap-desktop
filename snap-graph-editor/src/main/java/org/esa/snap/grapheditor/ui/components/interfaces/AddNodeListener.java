package org.esa.snap.grapheditor.ui.components.interfaces;

import org.esa.snap.grapheditor.ui.components.NodeGui;

/**
 * Simple listener for the AddNodeDialog, enabling to add nodes and different kind of interactions.
 *
 * @author  Martino Ferrari (CS Group)
 */
public interface AddNodeListener {
    /**
     * Add new node in the default position.
     * @param node new node
     */
    void newNodeAdded(NodeGui node);

    /**
     * Add new node at the current mouse position.
     * @param node new node
     */
    void newNodeAddedAtCurrentPosition(NodeGui node);

    /**
     * Add new node at the current mouse position and start a drag action.
     * @param node new node
     */
    void newNodeAddedStartDrag(NodeGui node);
}
