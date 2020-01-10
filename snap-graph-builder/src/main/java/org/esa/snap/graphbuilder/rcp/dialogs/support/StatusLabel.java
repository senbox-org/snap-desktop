package org.esa.snap.graphbuilder.rcp.dialogs.support;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

public class StatusLabel extends JPanel implements ActionListener{
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    /**
     *
     */
    private enum Level {
        INFO, WARNING, ERROR
    }

    private class StatusMessage {
        public Level level;
        private String nodeID;
        private String message;
        private String timestamp;

        public StatusMessage(Level level, String id, String message) {
            this.level = level;
            this.nodeID = id;
            Date date = new Date();
            this.timestamp = sdf.format(date.getTime());
            this.message = message;
        }

        public String text() {
            return "["+this.timestamp+"]"+this.nodeID+": "+this.message;
        }
    }

    private JLabel label;
    private JButton nxtBtn;
    private JButton prvBtn;
    private JButton clrBtn;

    private int currentMessage = -1;

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
        clrBtn.addActionListener(this);

        layout.putConstraint(SpringLayout.NORTH, prvBtn, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.SOUTH, prvBtn, 0, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.EAST, prvBtn, -4, SpringLayout.WEST, clrBtn);
        prvBtn.setPreferredSize(new Dimension(45, 35));
        prvBtn.addActionListener(this);
        
        layout.putConstraint(SpringLayout.NORTH, nxtBtn, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.SOUTH, nxtBtn, 0, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.EAST, nxtBtn, -4, SpringLayout.WEST, prvBtn);
        nxtBtn.setPreferredSize(new Dimension(45, 35));
        nxtBtn.addActionListener(this);

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
        displayMessages(messages.size() -1);
    }

    public void warning(String id, String message) {
        messages.add(new StatusMessage(Level.WARNING, id, message));
        displayMessages(messages.size() -1);
    }

    public void error(String id, String message) {
        messages.add(new StatusMessage(Level.ERROR, id, message));
        displayMessages(messages.size() -1);
    }

    private void displayMessages(int index) {
        if (index < 0 || index >= messages.size()) {
            return;
        }
        StatusMessage msg = messages.get(index);

        this.currentMessage = index;
        this.setColorLevel(msg.level);
        this.label.setText(msg.text());
        if (index == 0) {
            prvBtn.setEnabled(false);
        } else {
            prvBtn.setEnabled(true);
        }
        if (index == messages.size() - 1) {
            nxtBtn.setEnabled(false);
        } else {
            nxtBtn.setEnabled(true);
        }
        if (messages.size() > 0){
            clrBtn.setEnabled(true);
        }

        this.revalidate();
    }

    private void setColorLevel(Level level) {
        switch (level) {
            case INFO:
                label.setForeground(Color.black);
                break;
            case ERROR:
                label.setForeground(Color.red);
                break;
            case WARNING:
                label.setForeground(Color.yellow);
                break;
        }
    }

    public void clearMessages() {
        this.label.setText("");
        prvBtn.setEnabled(false);
        nxtBtn.setEnabled(false);
        clrBtn.setEnabled(false);
        messages.clear();
    }


    @Override
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source.equals(clrBtn)) {
            clearMessages();
            return;
        } 
        if (source.equals(prvBtn)) {
            this.displayMessages(--currentMessage);
            return;
        }
        if (source.equals(nxtBtn)) {
            this.displayMessages(++currentMessage);
            return;
        }
    }
}