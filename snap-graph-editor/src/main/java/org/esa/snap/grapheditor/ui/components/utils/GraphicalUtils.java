package org.esa.snap.grapheditor.ui.components.utils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Utility class to managage graphical components such as background, grid size and normalizing positions and sizes.
 *
 * @author Martino Ferrari (CS Group)
 */
public class GraphicalUtils {
    private static final int gridSize = 15;
    private static final int gridMajor = 5;
    private static final Color gridMajorColor = new Color(0, 0, 0, 30);
    private static final Color gridMinorColor = new Color(0, 0, 0, 15);

    private static final BasicStroke smallStroke = new BasicStroke(1);
    private static final BasicStroke bigStroke = new BasicStroke(3);

    private static final int handlerSize = 4;
    private static final int handlerHalfSize = handlerSize / 2;

    public static final Color connectionConnectedColor = new Color(66, 66, 66, 255);
    public static final Color connectionActiveColor = Color.gray;

    /**
     * Create a new grid pattern of the given size.
     * @param width width of the grid pattern
     * @param height height of the grid pattern
     * @return grid pattern as BufferedImage
     */
    static public BufferedImage gridPattern(int width, int height) {
        // initialise gridPattern image buffer
        BufferedImage gridPattern = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D gbuff = (Graphics2D) gridPattern.getGraphics();

        int nCol = (int) Math.ceil(width / (float) gridSize);
        int nRow = (int) Math.ceil(height / (float) gridSize);
        int nMax = Math.max(nCol, nRow);

        BasicStroke majorStroke = new BasicStroke(2);
        BasicStroke minorStroke = new BasicStroke(1);

        for (int i = 0; i <= nMax; i++) {
            int pos = i * gridSize;

            if (i % gridMajor == 0) {
                // set style for major lines
                gbuff.setStroke(majorStroke);
                gbuff.setColor(gridMajorColor);
            } else {
                // set style for minor lines
                gbuff.setStroke(minorStroke);
                gbuff.setColor(gridMinorColor);
            }

            if (i <= nRow) {
                // draw row
                gbuff.drawLine(0, pos, width, pos);
            }
            if (i <= nCol) {
                // draw col
                gbuff.drawLine(pos, 0, pos, height);
            }
        }
        return gridPattern;
    }

    /**
     * Normalize position on the grid.
     * @param p current position
     * @return normalized position
     */
    static public Point normalize(Point p) {
        int x = Math.max(0 ,Math.round(p.x / (float)gridSize) * gridSize);
        int y = Math.max(0, Math.round(p.y / (float)gridSize) * gridSize);
        return new Point(x, y);
    }

    /**
     * Normalize dimension to the grid spacing.
     * @param dim original dimension
     * @return normalized dimension
     */
    static public int normalizeDimension(int dim) {
        return (int)Math.floor(dim / (float)gridSize) * gridSize;
    }

    /**
     * Union of two rectangles, creates a rectangle containing the two inputs.
     * @param a first rectangle
     * @param b second rectangle
     * @return unified rectangle
     */
    static public Rectangle union(Rectangle a, Rectangle b) {
        int tlx = Math.min(a.x, b.x);
        int tly = Math.min(a.y, b.y);
        int brx = Math.max(a.x + a.width, b.x + b.width);
        int bry = Math.max(a.y + a.height, b.y + b.height);
        return new Rectangle(tlx, tly, brx - tlx, bry - tly);
    }

    /**
     * Simple function to compute the difference between two points.
     * @param a first point
     * @param b second point
     * @return difference between point a and point b
     */
    public static Point diff(Point a, Point b) {
        return new Point(a.x - b.x, a.y - b.y);
    }

    /**
     * Draw a connection line from two given points with a given color.
     *
     * @param g2 renderer
     * @param a start point
     * @param b end point
     * @param c color
     */
    public static void drawConnection(Graphics2D g2, Point a, Point b, Color c){
        Graphics2D g = (Graphics2D) g2.create();
        g.setColor(c);

        g.setStroke(bigStroke);
        g.drawLine(a.x, a.y, b.x, b.y);

        g.setStroke(smallStroke);
        g.fillRect(a.x - handlerHalfSize, a.y - handlerHalfSize, handlerSize, handlerSize);
        g.drawRect(a.x - handlerHalfSize, a.y - handlerHalfSize, handlerSize, handlerSize);
        g.fillOval(b.x - handlerHalfSize, b.y - handlerHalfSize, handlerSize, handlerSize);
        g.drawOval(b.x - handlerHalfSize, b.y - handlerHalfSize, handlerSize, handlerSize);

        g.dispose();
    }
}