package org.esa.snap.grapheditor.ui.components.interfaces;

import org.esa.snap.core.datamodel.Product;

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

    Point getInputPosition(int connector);

    Point getOutputPosition();

    Product getProduct();

    void addNodeListener(NodeListener l);

    void removeNodeListener(NodeListener l);

    int distance(NodeInterface n);

    boolean isConnectionAvailable(NodeInterface other, int index);

    void addConnection(NodeInterface node, int index);

    Point getConnectorPosition(int conectorIndex);

    void drawNode(Graphics2D g);

    int getY();

    int getX();

    void setPosition(int x, int y);

    Rectangle getBoundingBox();

    Point getPostion();

    JComponent getPreferencePanel();

    int getActiveConnector();
}

