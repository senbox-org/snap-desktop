package org.esa.snap.graphbuilder.ui.components.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Map;

import javax.swing.JComponent;

import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUI;
import org.esa.snap.graphbuilder.ui.components.utils.GridUtils;
import org.esa.snap.graphbuilder.ui.components.utils.OperatorManager.SimplifiedMetadata;
import org.esa.snap.ui.AppContext;

public class NodeGui {
    public static final int STATUS_MASK_OVER = 1 << 1;
    public static final int STATUS_MASK_SELECTED = 1 << 2;
    


    // private static final Color errorColor = new Color(255, 80, 80, 128);
    // private static final Color validateColor =  new Color(0, 177, 255, 128);
    private static final Color unknownColor =  new Color(177, 177, 177, 128);
    // private static final Color connectionColor = new Color(66, 66, 66, 255);

    static final private BasicStroke borderStroke = new BasicStroke(3);
    static final private BasicStroke textStroke = new BasicStroke(1);
    static final private BasicStroke activeStroke = new BasicStroke(6);

    static final private Font textFont = new Font("Ariel", Font.BOLD, 11);

    static final private int minWidth = 60;
    
    private int x;
    private int y;
    private int width = 90;
    private int height = 30;

    private int textW = -1;
    private int textH = -1;
    
    private String name;
    
    private int status = 0;

    private final SimplifiedMetadata metadata;
    private final OperatorUI operatorUI;
    private final Node node;
    private final Map<String, Object> configuration;
    private JComponent preferencePanel = null;


    public NodeGui (Node node, Map<String, Object> configuration, SimplifiedMetadata metadata, OperatorUI operatorUI){
        this.x = 0;
        this.y = 0;
        this.metadata = metadata;
        this.operatorUI = operatorUI;
        this.node = node;
        this.name = this.node.getId();
        this.configuration = configuration;
    }

    public void paintNode(Graphics2D g) {
        g.setFont(textFont);
        
        if (textW <= 0) {
            FontMetrics fontMetrics = g.getFontMetrics();
                    
            textH = fontMetrics.getHeight();
            textW = fontMetrics.stringWidth(name);

            width = Math.max(GridUtils.floor(textW + 20), minWidth);
        }

        if ((this.status & STATUS_MASK_SELECTED) > 0) {
            Graphics2D gactive = (Graphics2D)g.create();
            gactive.setColor(this.color().brighter().brighter());
            gactive.setStroke(activeStroke);
            gactive.drawRoundRect(x-2, y-2, width + 4,  height + 4, 8, 8);
            gactive.dispose();
        }

        g.setColor(this.color());
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setStroke(borderStroke);
        g.setColor(this.color().brighter());
        g.drawRoundRect(x, y, width, height, 8, 8);
        
        g.setStroke(textStroke);
       
        g.drawString(name, x + (width - textW) / 2 , y + (5 + textH));
    }

    private Color color() {
        if ((this.status & STATUS_MASK_OVER) > 0) {
            return unknownColor.brighter();
        }
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

    public String getName() {
        return name;
    }

    public boolean contains(Point p) {
        int dx = p.x - x;
        int dy = p.y - y;
        return (dx >= 0 && dy >= 0 && dx <= width && dy <= height);
    }

    public void over() {
        if ((status & STATUS_MASK_OVER) == 0) 
            status += STATUS_MASK_OVER;
    }

    public void none() {
        if ((status & STATUS_MASK_OVER) > 0) 
            status -= STATUS_MASK_OVER;
    }

    public void select() {
        if ((status & STATUS_MASK_SELECTED) == 0) 
            status += STATUS_MASK_SELECTED; 
    } 

    public void deselect() {
        if ((status & STATUS_MASK_SELECTED) > 0) 
            status -= STATUS_MASK_SELECTED; 
    }


    public JComponent getPreferencePanel(AppContext context){
        if (preferencePanel == null) {
            try {
                preferencePanel = operatorUI.CreateOpTab(this.metadata.getName(), configuration ,context);
            } catch (Exception e) {
                SystemUtils.LOG.info(e.getMessage());
                preferencePanel = null;
                return null;
            }
        }
        return preferencePanel;
    }


}