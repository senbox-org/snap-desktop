package org.esa.snap.grapheditor.ui.components;


import javafx.util.Pair;
import org.esa.snap.grapheditor.ui.components.interfaces.AddNodeListener;
import org.esa.snap.grapheditor.ui.components.utils.GraphManager;
import org.esa.snap.grapheditor.ui.components.utils.UnifiedMetadata;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

/**
 * Simple floating dialog enabling users to quickly search and add new nodes to the graph using both keyboard and mouse
 * interaction.
 *
 * @author Martino Ferrari (CS Group)
 */
class AddNodeDialog extends JDialog implements KeyListener, MouseWheelListener {
    static final private int width = 400;
    static final private int height = 48;
    private final JTextField searchField;
    private final JList<UnifiedMetadata> resultsList;
    private final DefaultListModel<UnifiedMetadata> results = new DefaultListModel<>();

    private final HashSet<AddNodeListener> listeners = new HashSet<>();

    private final JComponent parent;

    /**
     * Create and popup a new AddNodeDialog.
     *
     * @param component the component that called up the dialog
     */
    AddNodeDialog(JComponent component) {
        super(SwingUtilities.getWindowAncestor(component));
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.BLACK));
        parent = component;
        this.setUndecorated(true);

        JPanel p = new JPanel(new BorderLayout(2,2));
        searchField = new JTextField();
        searchField.setEnabled(true);
        searchField.addKeyListener(this);
        searchField.addMouseWheelListener(this);
        p.add(searchField, BorderLayout.CENTER);

        resultsList = new JList<>(results);
        resultsList.setVisible(false);
        resultsList.addMouseWheelListener(this);
        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    NodeGui node = createNode(e.getPoint());
                    if (node != null) {
                        for (AddNodeListener l: listeners) {
                            l.newNodeAddedAtCurrentPosition(node);
                        }
                    }
                    popdown();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    NodeGui node = createNode(e.getPoint());
                    if (node != null) {
                        for (AddNodeListener l: listeners) {
                            l.newNodeAddedStartDrag(node);
                        }
                    }
                    popdown();
                }
            }
        });

        p.add(resultsList, BorderLayout.PAGE_END);

        this.add(p, BorderLayout.CENTER);
        this.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                popdown();
            }
        });
        this.popup();
    }

    /**
     * Add listener to the dialog
     * @param l simple AddNodeListener
     */
    void addListener(AddNodeListener l) {
        listeners.add(l);
    }

    private NodeGui createNode(Point p){
        NodeGui node = createNode();
        if (node != null){
            Point relativeP = SwingUtilities.convertPoint(resultsList, p, parent);
            node.setPosition(relativeP.x - node.getWidth() / 2, relativeP.y - node.getHeight() / 2);
        }
        return node;
    }

    private NodeGui createNode() {
        if (results.size() == 0)
            return null;
        UnifiedMetadata meta = resultsList.getSelectedValue();
        if (meta != null) {
            return GraphManager.getInstance().newNode(meta);
        }
        return null;
    }

    private void popup() {
        int x = getOwner().getX();
        int y = getOwner().getY();
        int fwidth = getOwner().getWidth();

        searchField.setText("");
        results.removeAllElements();
        resultsList.setVisible(false);
        this.setLocation(x + (fwidth - width) / 2, y + 100);
        this.setVisible(true);
        this.setSize(width, height);
        this.revalidate();

        searchField.requestFocus();
    }

    private void popdown() {
        this.getOwner().setEnabled(true);

        this.dispose();
        if (parent != null) {
            parent.requestFocusInWindow();//requestFocusInWindow()
        }
        results.clear();
        listeners.clear();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Nothing to do..
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case (KeyEvent.VK_UP):
                up();
                break;
            case (KeyEvent.VK_DOWN):
                down();
                break;
            case (10):
                // enter
                this.enter();
                this.popdown();
                break;
            case (27):
                // escape
                this.popdown();
                break;

        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // nothing to do..
        switch (e.getKeyCode()) {
            case (KeyEvent.VK_UP):
            case (KeyEvent.VK_DOWN):
            case (10):
            case (27):
                break;
            default:
                updateResults();
                break;
        }
    }

    /**
     * Function called each time a new key is typed in the search bar, it use a simple search algorithm (it could be
     * improved in the future) to search in the Operators names as well as in the categories.
     * A simple fitness based results ordering algorithm is also implemented.
     */
    private void updateResults() {
        String searchString = searchField.getText();
        if (searchString.length() == 0) {
            resultsList.setVisible(false);
            this.setSize(width, height);
            results.clear();
            return;
        }
        ArrayList<Pair<UnifiedMetadata, Double>> searchResult= new ArrayList<>();
        final String[] normSearch = smartTokenizer(searchString);

        for (UnifiedMetadata metadata: GraphManager.getInstance().getSimplifiedMetadata()) {
            double dist = metadata.fuzzySearch(normSearch);
            if (dist >= 0) {
                searchResult.add(new Pair<>(metadata, dist));
            }
        }
        results.removeAllElements();
        if (searchResult.size() > 0) {
            int prevActive = resultsList.getSelectedIndex();
            searchResult.sort(new ResultComparator());
            for (Pair<UnifiedMetadata, Double> res: searchResult) {
                results.addElement(res.getKey());
                // results.add(res.getKey());
            }
            resultsList.setVisible(true);

            if (prevActive >= 0) {
                int currentActive = Math.min(prevActive, results.size() - 1);
                resultsList.setSelectedIndex(currentActive);
            } else {
                resultsList.setSelectedIndex(0);
            }
            this.setSize(width, height + resultsList.getPreferredSize().height + 4);
        } else {
            resultsList.setVisible(false);
            this.setSize(width, height);
        }
        this.revalidate();
    }

    private void enter() {
        NodeGui n = createNode();
        if (n != null) {
            for (AddNodeListener l: listeners){
                l.newNodeAdded(n);
            }
        }
    }

    private void down() {
        if (results.size() <= 1) return;
        if (resultsList.getSelectedIndex() < results.getSize() - 1) {
            this.resultsList.setSelectedIndex(resultsList.getSelectedIndex() + 1);
        }
    }

    private void up() {
        if (results.size() <= 1) return;
        if (resultsList.getSelectedIndex() > 0) {
            this.resultsList.setSelectedIndex(resultsList.getSelectedIndex() - 1);
        }
    }

    static private String[] smartTokenizer(final String string) {
        final HashSet<String> list = new HashSet<>();
        if (string.length() > 0) {
            StringBuilder token = new StringBuilder();
            for (int i = 0; i < string.length(); i++) {
                final char c = string.charAt(i);
                if ((c == '.' || c == ' ')) {
                    if (token.length() > 0)
                        list.add(token.toString().toLowerCase());
                    token = new StringBuilder();
                } else {
                    token.append(c);
                }
            }
            if (token.length() > 0)
                list.add(token.toString().toLowerCase());

            list.add(string.toLowerCase());
        }
        return list.toArray(new String[0]);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() > 0) {
            down();
        } else {
            up();
        }
    }

    private static class ResultComparator implements Comparator<Pair<UnifiedMetadata, Double>>
    {
        ResultComparator()
        {
        }

        @Override
        public int compare(Pair<UnifiedMetadata, Double> i1, Pair<UnifiedMetadata, Double> i2)
        {
            return -i1.getValue().compareTo(i2.getValue());
        }
    }
}
