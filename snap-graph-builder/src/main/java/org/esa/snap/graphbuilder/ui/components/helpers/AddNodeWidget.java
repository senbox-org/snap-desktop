package org.esa.snap.graphbuilder.ui.components.helpers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.HashSet;
import java.util.Set;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpiRegistry;

import java.awt.Rectangle;

public class AddNodeWidget {
    static final private int widgetWidth = 400;
    static final private int widgetHeight = 60;
    
    static final private Color fillColor = new Color(0, 0, 0, 180); 
    static final private Color strokeColor = new Color(0, 0, 0, 255);

    private static final Font font = new Font("Ariel", Font.BOLD, 25);

    private Set<String> operatorList;
    private final HashSet<String> results = new HashSet<String>();

    private boolean visible = false;
    private String searchString = "";

    private final GPF gpf;
    private final OperatorSpiRegistry operatorSpiRegistry;
    
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

            textG.clipRect(x + 10, y + 10, widgetWidth - 20, widgetHeight - 20);
            textG.drawString(searchString, x + 10, y + widgetHeight / 2 + 10);
            textG.dispose();

            if (results.size() > 0) {  
                Graphics2D resG = (Graphics2D) g.create();   
                resG.clipRect(x, y + widgetHeight, widgetWidth, height - y - widgetHeight);

                resG.setColor(fillColor);
                int h = Math.min(height - y - widgetHeight - 10, 30 * results.size());
                resG.fillRect(x + 10, y + widgetHeight, widgetWidth - 20, h);
                resG.setColor(strokeColor);
                resG.setStroke(new BasicStroke(6));
                resG.drawRect(x + 10, y + widgetHeight, widgetWidth - 20, h);
                resG.setStroke(oldStroke);

                resG.setColor(Color.gray);
                int ypos = y + widgetHeight + 10;
                for (String res: results) {
                    System.out.println(res);
                    resG.drawString(res, x + 20, ypos + 20);
                    ypos += 40;
                    if (ypos > h) break;
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

    public void changeStatus() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    public Rectangle getBoundingRect(final int width, final int height) {
        return new Rectangle((width - widgetWidth) / 2 - 5, 30 - 5, widgetWidth + 10,
                widgetHeight + 5 + 30 * results.size());
    }

    public boolean isVisible() {
        return visible;
    }

    public void type(final char key) {
        if ((key >= 'a' && key <= 'z') || (key >= 'A' && key <= 'Z') || (key >= '0' && key <= '9') || (key == ' ')
                || (key == '.') || (key == '-')) {
            searchString += key;
            updateSearch();
            System.out.println(results);
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
}