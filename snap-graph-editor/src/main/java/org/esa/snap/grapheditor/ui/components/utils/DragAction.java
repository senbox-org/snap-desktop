package org.esa.snap.grapheditor.ui.components.utils;

import org.esa.snap.grapheditor.ui.components.interfaces.NodeInterface;

import java.awt.*;

/**
 * Simple class to handle different kind of Drag user interactions.
 *
 * @author Martino Ferrari (CS Group)
 */
public class DragAction {

    /**
     * Drag Action type enum to identify the two major types of interactions.
     *  - DRAG: drag a node
     *  - CONNECT: drag/create a connection line
     */
    public enum Type {
        DRAG,
        CONNECT,
    }

    private final Type type;
    private final NodeInterface source;
    private final Point origin;
    private Point current;
    private final int connectorIndex;

    /**
     * Constructor for the Node drag action case.
     *
     * @param source node to be dragged
     * @param origin mouse click point
     */
    public DragAction(NodeInterface source, Point origin) {
        this.type = Type.DRAG;
        this.source = source;
        this.origin = GraphicalUtils.diff(origin, source.getPosition());
        this.current = origin;
        this.connectorIndex = -202;
    }

    /**
     * Constructor for the Connection drag action case.
     */
    public DragAction(NodeInterface source, int conectorIndex, Point p) {
        this.source = source;
        this.origin = source.getConnectorPosition(conectorIndex);
        this.current = p;
        this.type = Type.CONNECT;
        this.connectorIndex = conectorIndex;
    }

    /**
     * Get the type of DragAction.
     * @return the DragAction type
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Get the dragged node
     * @return the dragged node
     */
    public NodeInterface getNode() {
        return source;
    }


    /**
     * Move the dragged object to a new position
     * @param p new drag position
     */
    public void move(Point p){
        current = p;
    }

    /**
     * Draw current dragged element.
     * @param g renderer to be used
     */
    public void draw(Graphics2D g) {
        if (this.type == Type.CONNECT) {
            GraphicalUtils.drawConnection(g, origin, current, GraphicalUtils.connectionActiveColor);
        } else {
            int dx = current.x - (source.getX() + origin.x);
            int dy = current.y - (source.getY() + origin.y);
            g.translate(dx, dy);
            AlphaComposite c = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.5f);
            g.setComposite(c);
            this.source.drawNode(g);
        }

    }

    /**
     * Conclude the drag action,
     */
    public void drop() {
        if (type == Type.DRAG) {
            int x = this.current.x - this.origin.x;
            int y = this.current.y - this.origin.y;
            this.source.setPosition(x, y);
        }
    }

    /**
     * Compute the bounding box of the DragAction.
     *
     * @return area impacted by the DragAction
     */
    public Rectangle getBoundingBox() {
        if (type == Type.DRAG) {
            Rectangle r = source.getBoundingBox();
            int x = (current.x - origin.x) - 8;
            int y = (current.y - origin.y) - 8;

            return new Rectangle(x, y, r.width, r.height);
        }
        int x = Math.min(current.x, origin.x) - 5;
        int y = Math.min(current.y, origin.y) - 5;
        int w = Math.abs(current.x - origin.x) + 10;
        int h = Math.abs(current.y - origin.y) + 10;
        return new Rectangle(x, y, w, h);
    }

    /**
     * Connect the source node to a second node.
     * @param other the other node to connect
     */
    public void connect(NodeInterface other) {
        if (type == Type.CONNECT) {
            if (this.connectorIndex == Constants.CONNECTION_OUTPUT
                    && other.isConnectionAvailable(source, other.getActiveConnector())) {
                other.addConnection(this.source, other.getActiveConnector());
            } else if (this.connectorIndex >= 0
                    && source.isConnectionAvailable(other, connectorIndex)) {
                this.source.addConnection(other, this.connectorIndex);
            }
        }
    }
}
