package org.esa.snap.grapheditor.ui.components.interfaces;

import org.esa.snap.core.datamodel.Product;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Minimal Node Gui Interface
 *
 * @author Martino Ferrari (CS Group)
 */
public interface NodeInterface {
    /**
     * Get node name (id).
     * @return the node unique name
     */
    String getName();

    /**
     * Gives the chosen input connector position
     * @param connector chosen input index
     * @return absolute position of the chosen input connector
     */
    Point getInputPosition(int connector);

    /**
     * Gives the output connector position
     * @return absolute position of the output connector
     */
    Point getOutputPosition();

    /**
     * Gets cached target product
     * @return cached target product.
     */
    Product getProduct();

    int getActiveConnector();

    /**
     * Add a NodeListener.
     * This happen when connecting a new node as output.
     * @param l NodeListener to be added
     */
    void addNodeListener(NodeListener l);

    /**
     * Remove a NodeListeners.
     * This happen when disconnecting or deleting a node.
     * @param l NodeListener to be removed
     */
    void removeNodeListener(NodeListener l);

    /**
     * Compute the distance from a node in the graph.
     * The node distance is useful to evaluate the correct validation order, node at the same distance can be
     * validate in parallel, otherwise in sequence.
     * @param n node to compute the distance
     * @return  -1 if the node n is not connected or is an output, the maximum distance if the node n is an input.
     */
    int distance(NodeInterface n);

    /**
     * Checks if a certain input is available to be connected with a source node.
     * It verify that the index is free and that the two nodes are not already connected together.
     * @param other source node
     * @param index input index
     * @return connection availability
     */
    boolean isConnectionAvailable(NodeInterface other, int index);


    /**
     * Connect a new input node to the first available connection.
     * @param source object representing the connection between nodes
     * @param index input index
     */
    void addConnection(NodeInterface source, int index);

    /**
     * Get coordinate of a connector.
     * @param connectorIndex connector index
     * @return coordinate (null if the connector is not found)
     */
    Point getConnectorPosition(int connectorIndex);

    /**
     * Draw node.
     * @param g renderer
     */
    void drawNode(Graphics2D g);

    /**
     * Get current y coordinate.
     * @return top y coordinate
     */
    int getY();

    /**
     * Get current x coordinate.
     * @return left x coordinate
     */
    int getX();

    /**
     * Get current node position.
     * @return current top-left node position
     */
    Point getPosition();

    /**
     * Set current node position.
     * @param x node x position
     * @param y node y position
     */
    void setPosition(int x, int y);

    /**
     * Set current node position.
     * @param p node position
     */
    void setPosition(@NotNull Point p);

    /**
     * Returns the area to be repainted.
     * Function useful to know which region of the GraphPanel repaint.
     * @return Rectangle containing the NodeGui and its tool-tip (if visible)
     */
    Rectangle getBoundingBox();

    /**
     * Gets the OperatorUI associated to this NodeGui
     * @return OperatorUI
     */
    JComponent getPreferencePanel();

}

