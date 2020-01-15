package org.esa.snap.graphbuilder.ui.components.helpers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.HashSet;
import java.util.Set;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.graphbuilder.ui.components.graph.NodeGui;

import java.awt.Rectangle;

public class AddNodeWidget {
    static final private int widgetWidth = 400;
    static final private int widgetHeight = 30;
    
    static final private Color fillColor = new Color(0, 0, 0, 180); 
    static final private Color strokeColor = new Color(0, 0, 0, 255);

    private static final int fsize = 12;
    private static final Font font = new Font("Ariel", Font.BOLD, fsize);

    private Set<String> operatorList;
    private final HashSet<String> results = new HashSet<String>();

    private boolean visible = false;
    private String searchString = "";

    private final GPF gpf;
    private final OperatorSpiRegistry operatorSpiRegistry;

    private int pos_y = 0;
    
    @SuppressWarnings("unchecked")
    public AddNodeWidget() {
        gpf = GPF.getDefaultInstance();
        operatorSpiRegistry = gpf.getOperatorSpiRegistry();

        operatorList = (Set<String>)operatorSpiRegistry.getAliases();
    }

    public void paint(final int width, final int height, final Graphics2D g) {
        if (visible) {
            final Stroke oldStroke = g.getStroke();
            final int arc = 8;
            final int x = (width - widgetWidth) / 2;
            final int y = 30;

            g.setColor(fillColor);
            g.fillRoundRect(x, y, widgetWidth, widgetHeight, arc, arc);  
            g.setStroke(new BasicStroke(6));
            g.setColor(strokeColor);
            g.drawRoundRect(x, y, widgetWidth, widgetHeight, arc, arc);
            g.setStroke(oldStroke);

            g.setColor(Color.lightGray);
            g.setFont(font);

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
                int h = Math.min((fsize + 10) * nRes, height - (y + widgetHeight + 10));

                resG.fillRoundRect(x, y + widgetHeight - yoff, widgetWidth, h + yoff, 8, 8);
                resG.drawRoundRect(x, y + widgetHeight - yoff, widgetWidth, h + yoff, 8, 8);

                resG.setStroke(oldStroke);

                resG.setColor(Color.gray);
                yoff = y + widgetHeight + 10; 
                int ypos = 0;
                int i = 0;
                for (String res: results) {
                    if (i == pos_y) {
                        resG.setColor(Color.lightGray);
                        resG.fillRect(x, yoff + ypos - fsize / 2 - 3, widgetWidth, fsize + 10);
                        resG.setColor(Color.black);
                    } else {
                        resG.setColor(Color.gray);
                    }
                    resG.drawString(res, x + offset, yoff + ypos + fsize / 2);
                    ypos += fsize + 10;
                    i ++;
                }
                resG.dispose();
            }
        }
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
        searchString = "";
        results.clear();
    }

    public NodeGui enter() {
        NodeGui n = null;
        if (results.size() > 0) {
            String opName = results.toArray(new String[results.size()])[pos_y];
            n = new NodeGui(10, 10, opName);
        }
        hide();
        return n;
    }

    public void changeStatus() {
        if (isVisible()) {
            hide();
        } else {
            show();
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
                || (key == '.') || (key == '-')) {
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
            for (final String alias : operatorList) {
                final String lowcase = alias.toLowerCase();
                for (final String tag : normSearch) {
                    if (lowcase.contains(tag)) {
                        results.add(alias);
                        break;
                    }
                }
            }
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
}