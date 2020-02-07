package org.esa.snap.grapheditor.ui.components;

import javax.swing.*;

import org.esa.snap.grapheditor.ui.components.graph.NodeGui;
import org.esa.snap.grapheditor.ui.components.utils.GraphListener;
import org.esa.snap.ui.AppContext;

import java.awt.*;

public class OptionPanel extends JPanel implements GraphListener {

    private static final long serialVersionUID = -6760564459151183901L;
    private final JSplitPane parent;

    private NodeGui selectedNode = null;
    private AppContext context; 

    private JComponent currentOptionWidget = null;
    private SpringLayout layout;
    private final JLabel optionTitle = new JLabel("");

    private int width = 300;

    public OptionPanel(JSplitPane parent, AppContext context) {
        this.context = context;
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
    public void selected(NodeGui source) {
        this.setVisible(true);
        this.parent.setDividerLocation(parent.getWidth() - width);
        if (selectedNode != null) {
            deselected(selectedNode);
        }
        if (source != null) {
            currentOptionWidget = source.getPreferencePanel(context);
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
    public void deselected(NodeGui source) {
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
    public void updated(NodeGui source) {
        // NOTHING TO DO, it can be used to create history of the graph.
    }

    @Override
    public void created(NodeGui source) {
        // NOTHGING TO DO.
    }

    @Override
    public void deleted(NodeGui source) {
        deselected(source);
    }
}