package org.esa.snap.grapheditor.ui.components.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.HashSet;

import org.esa.snap.grapheditor.ui.components.graph.NodeGui;
import org.esa.snap.grapheditor.ui.components.utils.OperatorManager.SimplifiedMetadata;

import javax.swing.*;
import java.awt.Point;
import java.awt.Rectangle;

public class AddNodeWidget {
    static final private int widgetWidth = 400;
    static final private int widgetHeight = 30;
    
    static final private Color fillColor = new Color(0, 0, 0, 180); 
    static final private Color strokeColor = new Color(0, 0, 0, 255);

    private static final int fsize = 12;
    private static final Font mainFont = new Font("Ariel", Font.BOLD, fsize);
    private static final Font secondaryFont = new Font("Ariel", Font.ITALIC, fsize - 1);
    private static final int hOffset = fsize * 2 + 8;
    private static final int yOffset = 30;

    private OperatorManager operatorManager;

    private final HashSet<SimplifiedMetadata> results = new HashSet<>();

    private boolean visible = false;
    private String searchString = "";

    private int pos_y = 0;
    private int over_y = -1;

    final private JComponent parent;
    
    
    public AddNodeWidget(JComponent parent, OperatorManager opManager) {
        this.parent = parent;
        operatorManager = opManager;
    }

    public void paint(Graphics2D g) {
        if (visible) {
            int width = parent.getWidth();
            int height = parent.getHeight();
            Stroke oldStroke = g.getStroke();
            int arc = 8;
            int x = (width - widgetWidth) / 2;
            int y = yOffset;

            g.setColor(fillColor);
            g.fillRoundRect(x, y, widgetWidth, widgetHeight, arc, arc);  
            g.setStroke(new BasicStroke(6));
            g.setColor(strokeColor);
            g.drawRoundRect(x, y, widgetWidth, widgetHeight, arc, arc);
            g.setStroke(oldStroke);

            g.setColor(Color.lightGray);
            g.setFont(mainFont);

            Graphics2D textG = (Graphics2D) g.create();
            int offset = 10;

            textG.clipRect(x + 10, y + 5, widgetWidth - 20, widgetHeight - 10);
            textG.drawString(searchString, x + offset, y + widgetHeight / 2 + fsize / 2);
            textG.dispose();

            
            int nRes = results.size();
            if (nRes > 0) {  
                Graphics2D resG = (Graphics2D) g.create();   
                resG.clipRect(x - 5, y + widgetHeight - 2, widgetWidth + 10, height - y - widgetHeight + 7);

                resG.setColor(strokeColor);
                resG.setStroke(new BasicStroke(6));

                int yoff = 15;
                int h = Math.min(hOffset * nRes, height - (y + widgetHeight + 10));

                resG.fillRoundRect(x, y + widgetHeight - yoff, widgetWidth, h + yoff, 8, 8);
                resG.drawRoundRect(x, y + widgetHeight - yoff, widgetWidth, h + yoff, 8, 8);

                resG.setStroke(oldStroke);

                resG.setColor(Color.gray);
                yoff = y + widgetHeight + 10; 
                int ypos = 0;
                int i = 0;
                for (SimplifiedMetadata res: results) {
                    Color cMain = Color.gray;
                    Color cSecond = Color.darkGray;
                    if (i == pos_y) {
                        resG.setColor(Color.lightGray);
                        resG.fillRect(x, yoff + ypos - fsize / 2 - 3, widgetWidth, hOffset);
                        cMain = Color.black;
                    } else if (i == over_y) {
                        resG.setColor(Color.darkGray);
                        resG.fillRect(x, yoff + ypos - fsize / 2 - 3, widgetWidth, hOffset);
                        cSecond = Color.gray;
                        cMain = Color.lightGray;
                    }
                    resG.setColor(cMain);
                    resG.setFont(mainFont);
                    resG.drawString(res.getName(), x + offset, yoff + ypos + 4);
                    
                    resG.setFont(secondaryFont);
                    resG.setColor(cSecond);
                    resG.drawString(res.getCategory(), x + offset, yoff + ypos + fsize + 6);
                    ypos += hOffset;
                    i ++;
                }
                resG.dispose();
            }
        }
    }

    public void show(Point position) {
        mouseMoved(position);
        visible = true;
    }

    public void hide() {
        visible = false;
        pos_y = 0;
        over_y = -1;
        searchString = "";
        results.clear();
    }

    private NodeGui createNode(int index) {
        SimplifiedMetadata opMetaData = results.toArray(new SimplifiedMetadata[results.size()])[index];
        return operatorManager.newNode(opMetaData);
    }

    public NodeGui enter() {
        NodeGui n = null;
        if (results.size() > 0) {
           n = createNode(pos_y);
        }
        hide();
        return n;
    }

    public void changeStatus(Point position) {
        if (isVisible()) {
            hide();
        } else {
            show(position);
        }
    }

    public Rectangle getBoundingRect(final int width, final int height) {
        return new Rectangle((width - widgetWidth) / 2 - 5, 30 - 5, widgetWidth + 10,
                widgetHeight + 5 + (fsize + 10) * results.size());
    }

    public boolean isVisible() {
        return visible;
    }

    public void type(final char key) {
        if ((key >= 'a' && key <= 'z') || (key >= 'A' && key <= 'Z') || (key >= '0' && key <= '9') || (key == ' ')
                || (key == '.') || (key == '-') || (key == '/') || (key == '_')) {
            searchString += key;
            updateSearch();
        }
    }

    public void backspace() {
        if (searchString != null && searchString.length() > 0) {
            searchString = searchString.substring(0, searchString.length() - 1);
            updateSearch();
        }
    }

    static private String[] smartTokenizer(final String string) {
        final HashSet<String> list = new HashSet<>();
        if (string.length() > 0) {
            String token = "";
            for (int i = 0; i < string.length(); i++) {
                final char c = string.charAt(i);
                if ((c == '.' || c == ' ')) {
                    if (token.length() > 0)
                        list.add(token.toLowerCase());
                    token = "";
                } else {
                    token += c;
                }
            }
            if (token.length() > 0)
                list.add(token.toLowerCase());

            list.add(string.toLowerCase());
        }
        return list.toArray(new String[list.size()]);
    }

    private void updateSearch() {
        results.clear();
        if (searchString.length() > 0) {
            final String normSearch[] = smartTokenizer(searchString);
            for (SimplifiedMetadata metadata: operatorManager.getSimplifiedMetadatas()) {
                
                for (final String tag : normSearch) {
                    if (metadata.find(tag)) {
                        results.add(metadata);
                        break;
                    }
                }
            }
        }
        if (pos_y >= results.size()) {
            pos_y = results.size() - 1;
        }
        if (pos_y < 0) {
            pos_y = 0;
        }
    }

    public void up() {
        if (pos_y > 0) {
            pos_y --;
        }   
    }

    public void down() {
        if (pos_y < results.size() - 1) {
            pos_y ++;
        }
    }

    public NodeGui click(Point p) {
        NodeGui n = null;
        if (isVisible()) {
            mouseMoved(p);
            if (over_y >= 0 && over_y < results.size()) {
                n = createNode(over_y);
            } 
            hide();
        }
        return n;
    }

    public boolean mouseMoved(Point p) {
        int x = p.x - (parent.getWidth() - widgetWidth) / 2;
        int y = p.y - (yOffset + widgetHeight);
        int old_y = over_y;
        over_y = -1;
        if (x >= 0 && p.x <= widgetWidth && y > 0) {
            int y_ind = (int)Math.ceil(y / hOffset);
            if (y_ind < results.size()) {
                over_y = y_ind;
            }
        } 
        return over_y != old_y;
    }
}