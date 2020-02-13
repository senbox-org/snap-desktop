package org.esa.snap.grapheditor.ui.components.interfaces;

import org.esa.snap.core.datamodel.Product;

import java.awt.*;

/**
 * Connection Interface.
 *
 * @author Martino Ferrari
 */
public interface ConnectionInterface {

    /**
     * Remove listener from the source.
     * @param l listener
     */
    void removeNodesSourceListener(NodeListener l);

    /**
     * Compute the distance between the source and a node
     * @param node target node
     * @return distance (-1 if not connected)
     */
    int distance(Object node);

    /**
     * Get source output product
     * @return cached product
     */
    Product getSourceProduct();

    /**
     * Update target input index
     * @param newIndex new input connector index
     */
    void setTargetIndex(int newIndex);

    /**
     * Draw connection line
     * @param g renderer
     */
    void draw(Graphics2D g);

    /**
     * Get source object
     * @return source object
     */
    Object getSource();

    /**
     * Get target object
     * @return target object
     */
    Object getTarget();
}
