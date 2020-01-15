package org.esa.snap.graphbuilder.ui.components.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;

import org.esa.snap.graphbuilder.ui.components.utils.GridUtils;

public class NodeGui {
    // private static final Color errorColor = new Color(255, 80, 80, 128);
    // private static final Color validateColor =  new Color(0, 177, 255, 128);
    private static final Color unknownColor =  new Color(177, 177, 177, 128);
    // private static final Color connectionColor = new Color(66, 66, 66, 255);

    static final private BasicStroke borderStroke = new BasicStroke(3);
    static final private BasicStroke textStroke = new BasicStroke(1);
    static final private Font textFont = new Font("Ariel", Font.BOLD, 11);

    static final private int minWidth = 60;
    
    private int x;
    private int y;
    private int width = 90;
    private int height = 30;

    private int textW = -1;
    private int textH = -1;
    
    private String title;
    
    public NodeGui (int x, int y, String title){
        this.x = x;
        this.y = y;
        this.title = title;
    }

    public void paintNode(Graphics2D g) {
        g.setFont(textFont);
        
        if (textW <= 0) {
            FontMetrics fontMetrics = g.getFontMetrics();
                    
            textH = fontMetrics.getHeight();
            textW = fontMetrics.stringWidth(title);

            width = Math.max(GridUtils.floor(textW + 20), minWidth);
        }


        g.setColor(this.color());
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setStroke(borderStroke);
        g.setColor(this.color().brighter());
        g.drawRoundRect(x, y, width, height, 8, 8);
        
        g.setStroke(textStroke);
       
        g.drawString(title, x + (width - textW) / 2 , y + (5 + textH));
    }

    private Color color() {
        return unknownColor;
    }
    
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y){
        this.x = x;
        this.y = y;
    }

    public void setPosition(Point p) {
        this.x = p.x;
        this.y = p.y;
    }

    public Point getPostion() {
        return new Point(x, y);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }


}