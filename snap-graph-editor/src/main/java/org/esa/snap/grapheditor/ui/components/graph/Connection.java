package org.esa.snap.grapheditor.ui.components.graph;

import java.awt.*;

public class Connection {
    private static final Color connectedColor = new Color(66, 66, 66, 255);
    private static final Color activeColor = Color.gray;

    private static final BasicStroke smallStroke = new BasicStroke(1);
    private static final BasicStroke bigStroke = new BasicStroke(3);

    private static final int handlerSize = 4;
    private static final int handlerHalfSize = handlerSize / 2;


    private NodeGui source;
    private NodeGui target = null;
    private int targetIndex = -1;
    private Point endPoint = null;

    public Connection(NodeGui from, NodeGui to, int index) {
        source = from;
        target = to;
        targetIndex = index;
    }

    public Connection(NodeGui from, Point to) {
        source = from;
        endPoint = to;
    }

    public Connection(NodeGui to, int index, Point from) {
        source = null;
        target = to;
        targetIndex = index;
        endPoint = from;
    }

    public NodeGui getSource() {
        return source;
    }

    public NodeGui getTarget() {
        return target;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point p) {
        endPoint = p;
    }


    public void draw(Graphics2D base) {
        Graphics2D g = (Graphics2D) base.create();
        g.setColor(connectedColor);

        Point end;
        Point start;

        if (target == null) {
            end = endPoint;
            g.setColor(activeColor);
        } else {
            end = target.getInputPosition(targetIndex);
        }
        if (source == null) {
            start = endPoint;
            g.setColor(activeColor);
        } else {
            start = source.getOutputPosition();
        }

        g.setStroke(bigStroke);
        g.drawLine(start.x, start.y, end.x, end.y);

        g.setStroke(smallStroke);
        g.fillRect(start.x - handlerHalfSize, start.y - handlerHalfSize, handlerSize, handlerSize);
        g.drawRect(start.x - handlerHalfSize, start.y - handlerHalfSize, handlerSize, handlerSize);
        g.fillOval(end.x - handlerHalfSize, end.y - handlerHalfSize, handlerSize, handlerSize);
        g.drawOval(end.x - handlerHalfSize, end.y - handlerHalfSize, handlerSize, handlerSize);

        g.dispose();
    }

    public boolean connect(NodeGui node) {
        int connection = node.getConnectionAt(endPoint);
        if (connection == NodeGui.CONNECTION_NONE)
            return false;
        return true;
    }
}
