package org.esa.snap.graphbuilder.rcp.dialogs.support;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.Timer;

public class StatusLabel extends JLabel {

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

    private StatusMessage currentMsg = null;

    private static final long serialVersionUID = 8208638351016455964L;
    private ArrayList<StatusMessage> messages = new ArrayList<>();

    public StatusLabel() {
        super("");
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
        if (messages.size() == 0) {
            setText("");
            return;
        }
        StatusMessage msg = messages.get(messages.size() - 1);
        messages.remove(messages.size() - 1);
        if (currentMsg != null) {
            messages.add(currentMsg);
        }

        this.setColorLevel(msg.level);
        this.setText(msg.nodeID + ": " + msg.message);
        this.revalidate();      
        
        ActionListener reDisplay = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                currentMsg = null;
                displayMessages();    
            }

        };
        // wait 10s and remove the label
        Timer timer = new Timer(10000, reDisplay);
        timer.setRepeats(false);
        timer.start();    
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
        currentMsg = null;
        setText("");
        messages.clear();
    }

}