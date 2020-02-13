package org.esa.snap.grapheditor.ui.components.interfaces;

import org.esa.snap.grapheditor.ui.components.graph.NodeGui;

/**
 * Simple interface listener.
 *
 * @author Martino Ferrari (CS Group)
 */
public interface GraphListener {
    /**
     * Notifies about a node selection.
     * @param source
     */
    void selected(NodeGui source);

    /**
     * Notifies about a node deselection.
     * @param source
     */
    void deselected(NodeGui source);

    /**
     * Notifies about a changes on a node.
     * @param source
     */
    void updated(NodeGui source);

    /**
     * Notifies about a new node.
     * @param source
     */
    void created(NodeGui source);

    /**
     * Notifies about a delete of a node.
     * @param source
     */
    void deleted(NodeGui source);

}