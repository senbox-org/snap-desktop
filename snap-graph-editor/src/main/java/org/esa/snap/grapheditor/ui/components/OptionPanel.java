package org.esa.snap.grapheditor.ui.components;

import javax.swing.*;

import org.esa.snap.grapheditor.ui.components.interfaces.GraphListener;
import org.esa.snap.grapheditor.ui.components.interfaces.NodeInterface;

import java.awt.*;

/**
 * Simple side panel to show and edit Node properties. It uses the GraphListener to show and hide when needed.
 *
 * @author Martino Ferrari (CS Group)
 */
public class OptionPanel extends JPanel implements GraphListener {

    private static final long serialVersionUID = -6760564459151183901L;
    private final JSplitPane parent;

    private NodeInterface selectedNode = null;

    private JComponent currentOptionWidget = null;
    private final SpringLayout layout;
    private final JLabel optionTitle = new JLabel("");

    private int width = 300;

    /**
     * Create and setup the OptionPanel. *Note* that this components must be added inside a JSplitPane.
     *
     * @param parent container of the OptionPanel
     */
    OptionPanel(JSplitPane parent) {
        this.parent = parent;

        ///// INIT GUI
        layout=  new SpringLayout();
        this.setLayout(layout);

        this.add(optionTitle);
        layout.putConstraint(SpringLayout.NORTH, optionTitle, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.WEST, optionTitle, 4, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.EAST, optionTitle, -4, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.SOUTH, optionTitle, 30, SpringLayout.NORTH, this);

        this.setMinimumSize(new Dimension(300, 300));
        this.setPreferredSize(new Dimension(300, 800));
    }


    @Override
    public void selected(NodeInterface source) {
        this.setVisible(true);
        this.parent.setDividerLocation(parent.getWidth() - width);
        if (selectedNode != null) {
            deselected(selectedNode);
        }
        if (source != null) {
            currentOptionWidget = source.getPreferencePanel();
            this.optionTitle.setText(source.getName());            
            if (currentOptionWidget != null) {
                this.add(currentOptionWidget);
                layout.putConstraint(SpringLayout.NORTH, currentOptionWidget, 4, SpringLayout.SOUTH, optionTitle);
                layout.putConstraint(SpringLayout.WEST, currentOptionWidget, 4, SpringLayout.WEST, this);
                layout.putConstraint(SpringLayout.EAST, currentOptionWidget, -4, SpringLayout.EAST, this);
                layout.putConstraint(SpringLayout.SOUTH, currentOptionWidget, 0, SpringLayout.SOUTH, this);   
            } 
        }
        
        this.revalidate();
        this.repaint();
    }

    @Override
    public void deselected(NodeInterface source) {
        selectedNode = null;    
        if (currentOptionWidget != null) {
            this.layout.removeLayoutComponent(currentOptionWidget);
            this.remove(currentOptionWidget);
            currentOptionWidget = null;
        }
        this.revalidate();
        this.repaint();
        this.optionTitle.setText("");
        width = this.getWidth();
        this.setVisible(false);
    }

    @Override
    public void updated(NodeInterface source) {
        // NOTHING TO DO, it can be used to create history of the graph.
    }

    @Override
    public void created(NodeInterface source) {
        // NOTHGING TO DO.
    }

    @Override
    public void deleted(NodeInterface source) {
        deselected(source);
    }
}