package org.esa.snap.grapheditor.ui.components.utils;

import org.esa.snap.grapheditor.ui.components.graph.Connection;
import org.esa.snap.grapheditor.ui.components.graph.NodeGui;

import java.awt.*;

public class NodeDragAction {
    public enum Type {
        DRAG,
        CONNECT,
    }

    private Type type;
    private NodeGui source;
    private Point origin;
    private Point current;
    private Connection connection = null;

    private static Point diff(Point a, Point b) {
        return new Point(a.x - b.x, a.y - b.y);
    }

    public NodeDragAction(NodeGui source, Point origin) {
        this.type = Type.DRAG;
        this.source = source;
        this.origin = diff(origin, source.getPostion());
        this.current = origin;
    }

    public NodeDragAction(Connection c) {
        connection = new Connection(source, origin);
        this.source = c.getSource() != null ? c.getSource() : c.getTarget();
        this.connection = c;
        this.origin = c.getEndPoint();
        this.type = Type.CONNECT;
    }

    public Type getType() {
        return this.type;
    }

    public NodeGui getSource() {
        return source;
    }

    public Connection getConnection() {
        return connection;
    }

    public void move(Point p){
        if (type == Type.DRAG) {
            current = p;
        } else {
            this.connection.setEndPoint(p);
        }
    }

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

    public void drop() {
        if (type == Type.DRAG) {
            int x = this.current.x - this.origin.x;
            int y = this.current.y - this.origin.y;
            this.source.setPosition(x, y);
        }
    }

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
