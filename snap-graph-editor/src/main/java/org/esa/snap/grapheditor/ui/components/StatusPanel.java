package org.esa.snap.grapheditor.ui.components;

import org.esa.snap.grapheditor.ui.components.utils.Notification;
import org.esa.snap.grapheditor.ui.components.utils.NotificationListener;
import org.esa.snap.grapheditor.ui.components.utils.NotificationManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatusPanel extends JPanel implements ActionListener, NotificationListener {

    /**
     * Generate UID
     */
    private static final long serialVersionUID = -1363239999143252182L;

    private static final Color errorColor = new Color(200, 0,0);
    private static final Color warningColor = new Color(220, 150, 0);
    private static final Color okColor = new Color(0, 128, 0);

    private JPanel topPane;
    private JButton showButton;
    private JLabel messageLabel;

    private boolean extended = false;

    public StatusPanel() {
        super();

        NotificationManager.getInstance().addNotificationListener(this);

        topPane = new JPanel();
        topPane.setPreferredSize(new Dimension(30, 30));
        topPane.setLayout(new BorderLayout(5,0));

        messageLabel = new JLabel("");
        topPane.add(messageLabel, BorderLayout.LINE_START);

        showButton = new JButton("...");
        showButton.setPreferredSize(new Dimension(30, 30));
        showButton.addActionListener(this);
        topPane.add(showButton, BorderLayout.LINE_END);

        this.setLayout(new BorderLayout(5, 0));

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

    @Override
    public void notificationIncoming(Notification n) {
        this.messageLabel.setText(n.getSource()+": "+n.getMessage());
        Color fg;
        switch (n.getLevel()) {
            case ok:
                fg = okColor;
                break;
            case error:
                fg = errorColor;
                break;
            case warning:
                fg = warningColor;
                break;
            case info:
            default:
                fg = Color.black;
        }
        this.messageLabel.setForeground(fg);
        this.messageLabel.repaint();
    }
}