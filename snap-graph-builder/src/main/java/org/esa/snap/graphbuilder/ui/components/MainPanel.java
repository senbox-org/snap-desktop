package org.esa.snap.graphbuilder.ui.components;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.esa.snap.graphbuilder.ui.components.graph.NodeGui;
import org.esa.snap.graphbuilder.ui.components.utils.GraphListener;
import org.esa.snap.ui.AppContext;

/**
 * Main panel of the graph builder composed by a the graph panel and the
 * settings panel.
 * 
 * @author Martino Ferrari (CS Group)
 */
public class MainPanel extends JSplitPane implements GraphListener {

    /**
     * Generated UID
     */
    private static final long serialVersionUID = -8426716511820611531L;

    private JPanel optionPanel;
    private JLabel optionTitle;
    private JPanel emptyPanel = new JPanel();
    private JComponent currentOptionWidget = null;

    private GraphPanel graphPanel;

    private NodeGui selectedNode = null;

    private AppContext context; 

    public MainPanel(AppContext context) {
        super(JSplitPane.HORIZONTAL_SPLIT);
        this.context = context;

        graphPanel = new GraphPanel();
        graphPanel.addGraphListener(this);


        optionTitle = new JLabel();
        optionPanel = new JPanel();
        currentOptionWidget = emptyPanel;

        optionPanel.add(optionTitle, BorderLayout.PAGE_START);
        optionPanel.add(currentOptionWidget, BorderLayout.CENTER);


        this.setRightComponent(graphPanel);
        this.setLeftComponent(optionPanel);

        this.setOneTouchExpandable(true);
        this.setDividerLocation(300);
    }

    @Override
    public void selected(NodeGui source) {
        if (selectedNode != null && selectedNode != source) {
            deselected(selectedNode);
        }
        if (source != null) {
            int location = this.getDividerLocation();
            currentOptionWidget = source.getPreferencePanel(context);
            if (currentOptionWidget == null) {
                currentOptionWidget = emptyPanel;
            } 
            this.optionTitle.setText(source.getName());
            this.optionPanel.add(currentOptionWidget, BorderLayout.CENTER);
            this.setDividerLocation(location);
        }
    }

    @Override
    public void deselected(NodeGui source) {
        selectedNode = null;    
        this.optionPanel.remove(currentOptionWidget);
        currentOptionWidget = emptyPanel;
        this.optionPanel.add(emptyPanel, BorderLayout.CENTER);
        this.optionTitle.setText("");
    }

    @Override
    public void updated(NodeGui source) {
        // NOTHING TO DO, it can be used to create history of the graph.
    }

    @Override
    public void created(NodeGui source) {
        // NOTHGING TO DO.
    }

    @Override
    public void deleted(NodeGui source) {
        if (selectedNode == source) {
            deselected(source);
        }
    }
}