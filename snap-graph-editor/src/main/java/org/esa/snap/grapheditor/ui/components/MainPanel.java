package org.esa.snap.grapheditor.ui.components;

import javax.swing.JSplitPane;

import org.esa.snap.ui.AppContext;

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

    private OptionPanel optionPanel;
   

    private GraphPanel graphPanel;


    public MainPanel(AppContext context) {
        super(JSplitPane.HORIZONTAL_SPLIT);

        graphPanel = new GraphPanel();
        optionPanel = new OptionPanel(context);
        graphPanel.addGraphListener(optionPanel);
        this.setLeftComponent(graphPanel);
        this.setRightComponent(optionPanel);

        this.setOneTouchExpandable(true);
        this.setDividerLocation(800);
    }

}