package org.esa.snap.graphbuilder.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatusPanel extends JPanel implements ActionListener {

    /**
     * Generate UID
     */
    private static final long serialVersionUID = -1363239999143252182L;

    private JPanel topPane;
    private JButton showButton;
    private JLabel messageLabel;

    private boolean extended = false;

    public StatusPanel() {
        super();

        topPane = new JPanel();
        topPane.setPreferredSize(new Dimension(30, 30));
        topPane.setLayout(new BorderLayout());

        messageLabel = new JLabel("");
        messageLabel.setForeground(Color.white);
        topPane.add(messageLabel, BorderLayout.LINE_START);

        showButton = new JButton("↑");
        showButton.setPreferredSize(new Dimension(30, 30));
        showButton.addActionListener(this);
        topPane.add(showButton, BorderLayout.LINE_END);

        this.setLayout(new BorderLayout());

        this.add(topPane, BorderLayout.PAGE_START);

    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (extended) {
            unextend();
        } else {
            extend();
        }
    }

    private void extend() {
        extended = true;
        this.setPreferredSize(new Dimension(30, 200));  
        this.showButton.setText("↓");
    }

    private void unextend() {
        extended = false;
        this.setPreferredSize(new Dimension(30, 30));
        this.showButton.setText("↑");
    }
}