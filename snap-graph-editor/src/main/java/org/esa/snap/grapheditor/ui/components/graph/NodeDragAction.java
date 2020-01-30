package org.esa.snap.grapheditor.ui.components.graph;

import java.awt.*;

public class NodeDragAction {
    public enum Type {
        DRAG,
        CONNECT,
        DISCONNECT
    }

    private Type type;
    private NodeGui source;
    private Point origin;
    private Connection connection = null;

    private static Point diff(Point a, Point b) {
        return new Point(a.x - b.x, a.y - b.y);
    }

    public NodeDragAction(NodeGui source, Point origin) {
        this.type = Type.DRAG;
        this.source = source;
        this.origin = diff(origin, source.getPostion());
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
            int x = p.x - this.origin.x;
            int y = p.y - this.origin.y;
            this.source.setPosition(x, y);
        } else {
            this.connection.setEndPoint(p);
        }
    }

    public void draw(Graphics2D g) {
        if (this.connection != null) {
            this.connection.draw(g);
        }
    }
}
