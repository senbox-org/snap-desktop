package org.esa.snap.rcp.util;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class DateChooserButtonMain {
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Date Chooser Button Test");
            frame.setContentPane(createPanel());
            frame.setLocationByPlatform(true);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    private static Container createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new DateChooserButton(new SimpleDateFormat("dd. MMM YYYY"), Date.from(Instant.now())));
        return panel;
    }

}