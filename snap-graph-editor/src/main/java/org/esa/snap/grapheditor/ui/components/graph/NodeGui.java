package org.esa.snap.grapheditor.ui.components.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JComponent;

import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.grapheditor.gpf.ui.OperatorUI;
import org.esa.snap.grapheditor.ui.components.utils.GridUtils;
import org.esa.snap.grapheditor.ui.components.utils.OperatorManager.SimplifiedMetadata;
import org.esa.snap.ui.AppContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class NodeGui {
    public static final int STATUS_MASK_OVER = 1 << 1;
    public static final int STATUS_MASK_SELECTED = 1 << 2;

    private static final int MAX_LINE_LENGTH = 45;
    


    // private static final Color errorColor = new Color(255, 80, 80, 128);
    // private static final Color validateColor =  new Color(0, 177, 255, 128);
    private static final Color unknownColor =  new Color(233, 229, 225, 230); //Color
    // private static final Color connectionColor = new Color(66, 66, 66, 255);
    private static final Color activeColor = new Color(254, 223, 176, 180);

    private static final Color tooltipBackground = new Color(0, 0, 0, 180);
    private static final Color tooltipBorder = Color.black;
    private static final Color tooltipColor = Color.lightGray;

    static final private BasicStroke borderStroke = new BasicStroke(3);
    static final private BasicStroke textStroke = new BasicStroke(1);
    static final private BasicStroke activeStroke = new BasicStroke(6);

    static final private int connectionSize = 10;
    static final private int connectionHalfSize = connectionSize / 2;
    static final private int connectionOffset = 15;

    static final private Font textFont = new Font("Ariel", Font.BOLD, 11);

    static final private int minWidth = 60;

    static final private int TOOLTIP_NONE = -202;
    static final private int TOOLTIP_OUTPUT = -1;

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
    private String[] tooltipText_ = null;
    private boolean tooltipVisible_ = false;
    private int tooltipIndex_ = TOOLTIP_NONE;


    public NodeGui (Node node, Map<String, Object> configuration, SimplifiedMetadata metadata, OperatorUI operatorUI){
        this.x = 0;
        this.y = 0;
        this.metadata = metadata;
        this.operatorUI = operatorUI;
        this.node = node;
        this.name = this.node.getId();
        this.configuration = configuration;
        height = Math.max(height, connectionOffset * (metadata.getMinNumberOfInputs() + 1));
    }

    public void paintNode(Graphics2D g) {
        g.setFont(textFont);
        
        if (textW <= 0) {
            FontMetrics fontMetrics = g.getFontMetrics();
                    
            textH = fontMetrics.getHeight();
            textW = fontMetrics.stringWidth(name);

            width = Math.max(GridUtils.floor(textW + 30), minWidth);
        }

        if ((this.status & STATUS_MASK_SELECTED) > 0) {
            Graphics2D gactive = (Graphics2D)g.create();
            gactive.setColor(activeColor);
            gactive.setStroke(activeStroke);
            gactive.drawRoundRect(x-2, y-2, width + 4,  height + 4, 8, 8);
            gactive.dispose();
        }

        g.setColor(this.color());
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setStroke(borderStroke);
        g.setColor(this.borderColor());
        g.drawRoundRect(x, y, width, height, 8, 8);
        
        g.setStroke(textStroke);
        g.setColor(Color.darkGray);

        g.drawString(name, x + (width - textW) / 2 , y + (5 + textH));
        
        paintInputs(g);
        paintOutput(g);

    }

    @NotNull
    static private ArrayList<String> split_line(@NotNull String line) {
        ArrayList<String> result = new ArrayList<>();

        if (line.length() <= MAX_LINE_LENGTH) {
            result.add(line);
        } else {
            int start = 0;
            int N = (int)Math.ceil((double)line.length() / MAX_LINE_LENGTH);
            for (int i = 0; i < N; i++) {
                int end = Math.min(start + MAX_LINE_LENGTH, line.length());
                String subline = line.substring(start, end);
                if (end < line.length()
                        && Character.isLetter(line.charAt(end-1))
                        && Character.isLetter(line.charAt(end))) {
                    subline += "-";
                }
                result.add(subline);
                start = end;
            }
        }
        return  result;
    }

    @Contract("null -> null")
    static private String[] split_text(String input) {
        if (input == null) return null;
        ArrayList<String> result = new ArrayList<>();
        for (String line: input.split("\n")) {
            result.addAll(split_line(line));
        }
        return result.toArray(new String[0]);
    }


    public void tooltip(Graphics2D g) {
        if (tooltipVisible_ && tooltipText_ != null) {
            FontMetrics fontMetrics = g.getFontMetrics();

            int textH = fontMetrics.getHeight();
            int tooltipH = textH + (textH+4) * (tooltipText_.length - 1) + 8;

            int tooltipW = fontMetrics.stringWidth(tooltipText_[0]) + 8;
            for (int i = 1; i < tooltipText_.length; i++) {
                tooltipW = Math.max(tooltipW, fontMetrics.stringWidth(tooltipText_[i]) + 8);
            }

            int tx;
            int ty;
            if (tooltipIndex_ == TOOLTIP_OUTPUT) {
                tx = x + width + connectionSize;
                ty = y + connectionOffset - (tooltipH / 2);
            } else {
                tx = x - tooltipW - connectionSize;
                ty = y + (tooltipIndex_ + 1) * connectionOffset - (tooltipH / 2);
            }
            g.setColor(tooltipBackground);
            g.fillRoundRect(tx, ty, tooltipW, tooltipH, 8, 8);
            g.setStroke(borderStroke);
            g.setColor(tooltipBorder);
            g.drawRoundRect(tx, ty, tooltipW, tooltipH, 8, 8);
            g.setStroke(textStroke);
            g.setColor(tooltipColor);
            int stringY = ty + 8 + (textH / 2);
            for (String line: tooltipText_){
                g.drawString(line, tx + 4, stringY);
                stringY += textH + 4;
            }
        }
    }

    private void paintInputs(Graphics2D g) {
        if (metadata.hasInputs()) {
            int xc = x - connectionHalfSize;
            int yc = y + connectionOffset - connectionHalfSize;
            for (int i = 0; i < numInputs(); i++) {
                g.setColor(Color.white);
                g.fillOval(xc, yc, connectionSize, connectionSize);
                g.setStroke(borderStroke);
                g.setColor(borderColor());
                g.drawOval(xc, yc, connectionSize, connectionSize);
                yc += connectionOffset;
            }
        }
    }

    private int numInputs() { 
        return metadata.getMinNumberOfInputs(); 
    }  

    private void paintOutput(Graphics2D g) {
        if (metadata.hasOutput()) {
            int xc = x + width - connectionHalfSize;
            int yc = y + connectionOffset - connectionHalfSize;
            g.setColor(Color.white);
            g.fillOval(xc, yc, connectionSize, connectionSize);
            g.setStroke(borderStroke);
            g.setColor(borderColor());
            g.drawOval(xc, yc, connectionSize, connectionSize);
        }        
    }

    private Color color() {
        if ((this.status & STATUS_MASK_OVER) > 0) {
            return unknownColor.brighter();
        }
        return unknownColor;
    }
    
    private Color borderColor() {
        return color().darker().darker();
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
        boolean inside = (dx >= 0 && dy >= 0 && dx <= width && dy <= height);
        if (inside)
            return true;
        // check if is over a connection input
        if (Math.abs(dx) <= connectionHalfSize && dy > 0) {
            for (int i = 0; i < numInputs(); i++) {
                int cy = connectionOffset * (i+1);
                if (Math.abs(dy - cy) <= connectionHalfSize) {
                    return true;
                }
            }
        }
        // check if is over a connection output
        return (metadata.hasOutput() &&  Math.abs(dx - width) <= connectionHalfSize && Math.abs(dy - connectionOffset) <= connectionHalfSize);
    }

    public void over(Point p) {
        if ((status & STATUS_MASK_OVER) == 0) 
            status += STATUS_MASK_OVER;
        int dx = p.x - x;
        int dy = p.y - y;
        if (Math.abs(dx) <= connectionHalfSize) {
            for (int i = 0; i < numInputs(); i++) {
                int cy = connectionOffset * (i+1);
                if (Math.abs(dy - cy) <= connectionHalfSize) {
                    show_tooltip(i);
                    return;
                }
            }
        } else if (metadata.hasOutput() && Math.abs(dx - width) <= connectionHalfSize && Math.abs(dy - connectionOffset) <= connectionHalfSize) {
            show_tooltip(TOOLTIP_OUTPUT);
            return;
        }
        hide_tooltip();
    }

    public void none() {
        if ((status & STATUS_MASK_OVER) > 0) 
            status -= STATUS_MASK_OVER;
        hide_tooltip();
    }

    public void select() {
        if ((status & STATUS_MASK_SELECTED) == 0) 
            status += STATUS_MASK_SELECTED; 
    } 

    public void deselect() {
        if ((status & STATUS_MASK_SELECTED) > 0) 
            status -= STATUS_MASK_SELECTED; 
    }

    private void hide_tooltip() {
        tooltipVisible_ = false;
        tooltipText_ = null;
        tooltipIndex_ = TOOLTIP_NONE;
    }

    private void show_tooltip(int connectionIndex) {
        if (connectionIndex == TOOLTIP_OUTPUT && metadata.hasOutput()) {
            // OUTPUT
            tooltipVisible_ = true;
            tooltipText_ = split_text(metadata.getOutputDescription());
            tooltipIndex_ = connectionIndex;
        } else if (connectionIndex >= 0 && metadata.hasInputs()) {
            // INPUT
            tooltipVisible_ = true;
            tooltipText_ = split_text(metadata.getInputDescription(connectionIndex));
            tooltipIndex_ = connectionIndex;
        } else {
            hide_tooltip();
        }
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