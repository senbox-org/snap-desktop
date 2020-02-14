package org.esa.snap.grapheditor.ui.components.interfaces;


/**
 * Simple interface listener.
 *
 * @author Martino Ferrari (CS Group)
 */
public interface GraphListener {
    /**
     * Notifies about a node selection.
     * @param source event source
     */
    void selected(NodeInterface source);

    /**
     * Notifies about a node deselection.
     * @param source event source
     */
    void deselected(NodeInterface source);

    /**
     * Notifies about a changes on a node.
     * @param source event source
     */
    void updated(NodeInterface source);

    /**
     * Notifies about a new node.
     * @param source event source
     */
    void created(NodeInterface source);

    /**
     * Notifies about a delete of a node.
     * @param source event source
     */
    void deleted(NodeInterface source);

}