package org.esa.snap.grapheditor.ui.components;

import javax.swing.*;

import java.awt.*;

/**
 * Main panel of the graph builder composed by a the graph panel and the
 * settings panel.
 * 
 * @author Martino Ferrari (CS Group)
 */
public class MainPanel extends JSplitPane {

    /**
     * Generated UID
     */
    private static final long serialVersionUID = -8426716511820611531L;

    private final GraphPanel graphPanel;

    public MainPanel() {
        super(JSplitPane.HORIZONTAL_SPLIT);

        graphPanel = new GraphPanel();
        graphPanel.setPreferredSize(new Dimension(2000, 1500));
        JScrollPane scrollPane = new JScrollPane(graphPanel);
        scrollPane.setPreferredSize(new Dimension(300, 300));

        OptionPanel optionPanel = new OptionPanel(this);
        graphPanel.addGraphListener(optionPanel);
        this.setLeftComponent(scrollPane);
        this.setRightComponent(optionPanel);
        optionPanel.setVisible(false);

        this.setOneTouchExpandable(true);
        this.setDividerLocation(800);
    }

    public GraphPanel getGraphPanel() {
        return graphPanel;
    }


}