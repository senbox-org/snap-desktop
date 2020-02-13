package org.esa.snap.grapheditor.ui.components;

import org.esa.snap.grapheditor.ui.components.utils.Notification;
import org.esa.snap.grapheditor.ui.components.interfaces.NotificationListener;
import org.esa.snap.grapheditor.ui.components.utils.NotificationManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * Simple expansible status panel composed of a status label, a progress bar an expansion button and a expansible
 * history.
 *
 * @author Martino Ferrari (CS Group)
 */
public class StatusPanel extends JPanel implements ActionListener, NotificationListener {

    /**
     * Generate UID
     */
    private static final long serialVersionUID = -1363239999143252182L;

    // Color Palette
    private static final Color errorColor = new Color(200, 0,0);
    private static final Color warningColor = new Color(220, 150, 0);
    private static final Color okColor = new Color(0, 128, 0);

    // Date formatter
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    // Basic components
    private final JScrollPane scrollPane;
    private final JProgressBar progressBar;
    private final JButton showButton;
    private final JLabel messageLabel;
    private final JEditorPane historyPane;

    // History string
    private String history = "";

    // Status flag
    private boolean extended = false;

    /**
     * Create and setup a new StatusPanel. The default status is collapsed.
     */
    public StatusPanel() {
        super();

        NotificationManager.getInstance().addNotificationListener(this);

        JPanel topPane = new JPanel();
        topPane.setPreferredSize(new Dimension(30, 30));
        SpringLayout topLayout = new SpringLayout();
        topPane.setLayout(topLayout);//new BorderLayout(5,0));

        messageLabel = new JLabel("");
        topPane.add(messageLabel);

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(200, 30));
        topPane.add(progressBar);

        showButton = new JButton("+");
        showButton.setPreferredSize(new Dimension(30, 30));
        showButton.addActionListener(this);
        topPane.add(showButton);

        // SETUP LAYOUT

        topLayout.putConstraint(SpringLayout.EAST, showButton, -2, SpringLayout.EAST, topPane);
        topLayout.putConstraint(SpringLayout.WEST, showButton, -30, SpringLayout.EAST, showButton);
        topLayout.putConstraint(SpringLayout.NORTH, showButton, 2, SpringLayout.NORTH, topPane);
        topLayout.putConstraint(SpringLayout.SOUTH, showButton, -2, SpringLayout.SOUTH, topPane);

        topLayout.putConstraint(SpringLayout.EAST, progressBar, -4, SpringLayout.WEST, showButton);
        topLayout.putConstraint(SpringLayout.WEST, progressBar, -150, SpringLayout.EAST, progressBar);
        topLayout.putConstraint(SpringLayout.NORTH, progressBar, 2, SpringLayout.NORTH, topPane);
        topLayout.putConstraint(SpringLayout.SOUTH, progressBar, -2, SpringLayout.SOUTH, topPane);

        topLayout.putConstraint(SpringLayout.EAST, messageLabel, -4, SpringLayout.WEST, progressBar);
        topLayout.putConstraint(SpringLayout.WEST, messageLabel, 2, SpringLayout.WEST, topPane);
        topLayout.putConstraint(SpringLayout.NORTH, messageLabel, 2, SpringLayout.NORTH, topPane);
        topLayout.putConstraint(SpringLayout.SOUTH, messageLabel, -2, SpringLayout.SOUTH, topPane);

        this.setLayout(new BorderLayout(5, 0));

        this.add(topPane, BorderLayout.PAGE_START);

        historyPane = new JEditorPane();
        historyPane.setEditable(false);
        HTMLEditorKit kit = new HTMLEditorKit();
        historyPane.setEditorKit(kit);
        historyPane.setContentType("text/html");
        scrollPane = new JScrollPane(historyPane);
        this.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setVisible(false);
        initStylesheet(kit);

        unextend();

    }

    private void initStylesheet(HTMLEditorKit kit) {
        StyleSheet css = kit.getStyleSheet();
        css.addRule(".error{color: #C80000; font-weight: bold}");
        css.addRule(".warning{color: #DC9600; font-weight: bold}");
        css.addRule(".ok{color:#008000; font-weight: bold}");
        css.addRule(".info{color: #000;}");
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
        this.setPreferredSize(new Dimension(30, 250));
        this.showButton.setText("-");
        this.scrollPane.setVisible(true);
    }

    private void unextend() {
        extended = false;
        this.setPreferredSize(new Dimension(30, 30));
        this.showButton.setText("+");
        this.scrollPane.setVisible(false);
    }

    @Override
    public void notificationIncoming(Notification n) {
        this.messageLabel.setText(n.getSource()+": "+n.getMessage());
        Color fg;
        String cssClass;
        switch (n.getLevel()) {
            case ok:
                cssClass = "ok";
                fg = okColor;
                break;
            case error:
                fg = errorColor;
                cssClass = "error";
                break;
            case warning:
                cssClass = "warning";
                fg = warningColor;
                break;
            case info:
            default:
                cssClass = "info";
                fg = Color.black;
        }
        this.messageLabel.setForeground(fg);
        this.messageLabel.repaint();
        history += timeStamp() ;
        history += "<span class=\"" + cssClass +"\"> "+ n.getSource() + ": " + n.getMessage() + "</span><br>";
        historyPane.setText(history);
    }

    @Override
    public void processStart() {
        this.progressBar.setIndeterminate(true);
    }

    @Override
    public void processEnd() {
        this.progressBar.setIndeterminate(false);
        this.progressBar.setValue(0);
    }

    @Override
    public void progress(int value) {
        this.progressBar.setIndeterminate(false);
        this.progressBar.setValue(value);
    }

    private String timeStamp() {
        LocalDateTime now = LocalDateTime.now();
        return "[" +dtf.format(now) + "]";
    }
}