package org.esa.snap.grapheditor.ui.components;


import javafx.util.Pair;
import org.esa.snap.grapheditor.ui.components.graph.NodeGui;
import org.esa.snap.grapheditor.ui.components.utils.AddNodeListener;
import org.esa.snap.grapheditor.ui.components.utils.AddNodeWidget;
import org.esa.snap.grapheditor.ui.components.utils.GraphManager;
import org.esa.snap.grapheditor.ui.components.utils.UnifiedMetadata;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class AddNodeDialog extends JDialog implements KeyListener {
    static final private int width = 400;
    static final private int height = 48;
    private final JTextField searchField;
    private final JList<UnifiedMetadata> resultsList;
    private final DefaultListModel<UnifiedMetadata> results = new DefaultListModel<>();

    private HashSet<AddNodeListener> listeners = new HashSet<>();

    private int currentActive = -1;
    private JComponent parent;

    public AddNodeDialog(JComponent component) {
        super((JFrame) SwingUtilities.getWindowAncestor(component));
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.BLACK));
        parent = component;
        this.setUndecorated(true);

        JPanel p = new JPanel(new BorderLayout(2,2));
        searchField = new JTextField();
        searchField.setEnabled(true);
        searchField.addKeyListener(this);
        p.add(searchField, BorderLayout.CENTER);

        resultsList = new JList<>(results);
        resultsList.setVisible(false);
        resultsList.addListSelectionListener(e-> {
            if (e.getFirstIndex() != currentActive) {
                currentActive = e.getFirstIndex();
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
        this.popup((JFrame) SwingUtilities.getWindowAncestor(component));
    }

    public  void addListener(AddNodeListener l) {
        listeners.add(l);
    }

    private void popup(JFrame topFrame) {
        int x = topFrame.getX();
        int y = topFrame.getY();
        int fwidth = topFrame.getWidth();

        currentActive = -1;
        this.parent = parent;
        searchField.setText("");
        results.removeAllElements();
        resultsList.setVisible(false);
        this.setLocation(x + (fwidth - width) / 2, y + 100);
        this.setVisible(true);
        this.setSize(width, height);
        this.revalidate();

        searchField.requestFocus();
    }

    public void popdown() {
        this.getOwner().setEnabled(true);

        this.dispose();
        if (parent != null) {
            parent.requestFocusInWindow();//requestFocusInWindow()
        }

        listeners.clear();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        updateResults();
    }

    private void updateResults() {
        String searchString = searchField.getText();
        if (searchString.length() == 0) {
            currentActive = -1;
            resultsList.setVisible(false);
            results.clear();
            return;
        }
        ArrayList<Pair<UnifiedMetadata, Double>> searchResult= new ArrayList<>();
        if (searchString.length() > 0) {
            final String[] normSearch = smartTokenizer(searchString);

            for (UnifiedMetadata metadata: GraphManager.getInstance().getSimplifiedMetadata()) {
                double dist = metadata.fuzzySearch(normSearch);
                if (dist >= 0) {
                    searchResult.add(new Pair<>(metadata, dist));
                }
            }
            results.removeAllElements();
            if (searchResult.size() > 0) {
                searchResult.sort(new ResultComparator());
                for (Pair<UnifiedMetadata, Double> res: searchResult) {
                    results.addElement(res.getKey());
                   // results.add(res.getKey());
                }
                if (currentActive < 0) {
                    currentActive = 0;
                }
                resultsList.setVisible(true);
                resultsList.setSelectedIndex(currentActive);
                this.setSize(width, height + resultsList.getPreferredSize().height + 4);
            } else {
                currentActive = -1;
                resultsList.setVisible(false);
                this.setSize(width, height);

            }
            this.revalidate();
        }
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
        }
    }


    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case (10):
//                // return
//                this.addNode(this.addNodeWidget.enter());
                this.enter();
                this.popdown();
                break;
            case (27):
                // escape
                this.popdown();
                break;
        }
    }

    private void enter() {
        if (currentActive < 0 || currentActive > results.getSize() - 1) {
            return;
        }
        UnifiedMetadata meta = results.get(currentActive);
        System.out.println(meta.getName());
        NodeGui n = GraphManager.getInstance().newNode(meta);
        for (AddNodeListener l: listeners){
            l.newNodeAdded(n);
        }
    }

    private void down() {
        if (currentActive < 0) return;
        if (currentActive < results.getSize() - 1) {
            currentActive++;
            this.resultsList.setSelectedIndex(currentActive);
        }
    }

    private void up() {
        if (currentActive < 0) return;
        if (currentActive > 0) {
            currentActive --;
            this.resultsList.setSelectedIndex(currentActive);
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
