package org.esa.snap.graphbuilder.rcp.dialogs.support;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import java.awt.Dimension;

public class StatusLabel extends JPanel {

    /**
     *
     */
    private enum Level {
        INFO, WARNING, ERROR
    }

    private class StatusMessage {
        public Level level;
        public String nodeID;
        public String message;
        
        public StatusMessage(Level level, String id, String message) {
            this.level = level;
            this.nodeID = id;
            this.message = message;
        }
    }

    private JLabel label;
    private JButton nxtBtn;
    private JButton prvBtn;
    private JButton clrBtn;

    private static final long serialVersionUID = 8208638351016455964L;
    private ArrayList<StatusMessage> messages = new ArrayList<>();

    public StatusLabel() {
        super();
        this.initUI();
    }
    
    private void initUI() {
        SpringLayout layout = new SpringLayout();
        this.setLayout(layout);
        this.label = new JLabel("");
        this.add(label); 
        nxtBtn = new JButton("↓");
        prvBtn = new JButton("↑");
        clrBtn = new JButton("x");
        this.add(nxtBtn);
        this.add(prvBtn);
        this.add(clrBtn);

        layout.putConstraint(SpringLayout.NORTH, clrBtn, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.SOUTH, clrBtn, 0, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.EAST, clrBtn, -2, SpringLayout.EAST, this);
        clrBtn.setPreferredSize(new Dimension(45, 35));

        layout.putConstraint(SpringLayout.NORTH, prvBtn, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.SOUTH, prvBtn, 0, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.EAST, prvBtn, -4, SpringLayout.WEST, clrBtn);
        prvBtn.setPreferredSize(new Dimension(45, 35));
        
        layout.putConstraint(SpringLayout.NORTH, nxtBtn, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.SOUTH, nxtBtn, 0, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.EAST, nxtBtn, -4, SpringLayout.WEST, prvBtn);
        nxtBtn.setPreferredSize(new Dimension(45, 35));

        layout.putConstraint(SpringLayout.NORTH, label, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.SOUTH, label, 0, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.EAST, label, -2, SpringLayout.WEST, nxtBtn);
        layout.putConstraint(SpringLayout.WEST, label, 2, SpringLayout.WEST, this);

        this.setPreferredSize(new Dimension(150, 36));
        this.setMinimumSize(new Dimension(100, 40));

        nxtBtn.setEnabled(false);
        prvBtn.setEnabled(false);
        clrBtn.setEnabled(false);
    }

    public void info(String id, String message) {
        messages.add(new StatusMessage(Level.INFO, id, message));
        displayMessages();
    }

    public void warning(String id, String message) {
        messages.add(new StatusMessage(Level.WARNING, id, message));
        displayMessages();
    }

    public void error(String id, String message) {
        messages.add(new StatusMessage(Level.ERROR, id, message));
        displayMessages();
    }

    private void displayMessages() {
       
        StatusMessage msg = messages.get(messages.size() - 1);

        this.setColorLevel(msg.level);
        this.label.setText(msg.nodeID + ": " + msg.message);
        this.revalidate();
    }

    private void setColorLevel(Level level) {
        switch (level) {
            case INFO:
                setForeground(Color.black);
                break;
            case ERROR:
                setForeground(Color.red);
                break;
            case WARNING:
                setForeground(Color.yellow);
                break;
        }
    }

    public void clearMessages() {
        this.label.setText("");
        messages.clear();
    }

}