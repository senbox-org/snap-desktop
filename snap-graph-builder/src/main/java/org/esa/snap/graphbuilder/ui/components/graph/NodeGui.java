package org.esa.snap.graphbuilder.ui.components.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class NodeGui {
    static final private BasicStroke borderStroke = new BasicStroke(3);
    static final private BasicStroke textStroke = new BasicStroke(1);
    static final private Font textFont = new Font("Ariel", Font.BOLD, 8);

    private int x;
    private int y;
    private int width = 90;
    private int height = 30;

    private String title;
    
    public NodeGui (int x, int y, String title){
        this.x = x;
        this.y = y;
        this.title = title;
    }

    public void paintNode(Graphics2D g) {
        
        g.setColor(this.color());
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setStroke(borderStroke);
        g.setColor(this.color().darker());
        g.drawRoundRect(x, y, width, height, 8, 8);
        
        g.setColor(Color.black);
        g.setStroke(textStroke);
        g.setFont(textFont);
        g.drawString(title, x + 2 , y + 8);
    }

    private Color color() {
        return Color.gray;
    }
}