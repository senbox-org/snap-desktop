package org.esa.snap.grapheditor.ui.components.utils;

import org.esa.snap.grapheditor.ui.components.graph.Connection;
import org.esa.snap.grapheditor.ui.components.graph.NodeGui;

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
    private NodeGui source;
    private Point origin;
    private Point current;
    private Connection connection = null;

    /**
     * Constructor for the Node drag action case.
     *
     * @param source node to be dragged
     * @param origin mouse click point
     */
    public DragAction(NodeGui source, Point origin) {
        this.type = Type.DRAG;
        this.source = source;
        this.origin = GraphicalUtils.diff(origin, source.getPostion());
        this.current = origin;
    }

    /**
     * Constructor for the Connection drag action case.
     * @param c connection line to be dragged
     */
    public DragAction(Connection c) {
        this.source = c.getSource() != null ? c.getSource() : c.getTarget();
        this.connection = c;
        this.origin = c.getEndPoint();
        this.type = Type.CONNECT;
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
    public NodeGui getNode() {
        return source;
    }

    /**
     * Get the dragged connection
     * @return the dragged connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Move the dragged object to a new position
     * @param p new drag position
     */
    public void move(Point p){
        if (type == Type.DRAG) {
            current = p;
        } else {
            this.connection.setEndPoint(p);
        }
    }

    /**
     * Draw current dragged element.
     * @param g renderer to be used
     */
    public void draw(Graphics2D g) {
        if (this.connection != null) {
            this.connection.draw(g);
        } else {
            int dx = current.x - (source.getX() + origin.x);
            int dy = current.y - (source.getY() + origin.y);
            g.translate(dx, dy);
            AlphaComposite c = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.5f);
            g.setComposite(c);
            this.source.paintNode(g);
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
        return this.connection.getBoundingBox();
    }
}
