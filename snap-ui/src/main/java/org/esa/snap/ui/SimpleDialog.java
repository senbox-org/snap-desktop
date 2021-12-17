package org.esa.snap.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SimpleDialog extends JDialog {

    public SimpleDialog(String title, String message, Component component) {

        JButton okayButton = new JButton("Okay");
        okayButton.setPreferredSize(okayButton.getPreferredSize());
        okayButton.setMinimumSize(okayButton.getPreferredSize());
        okayButton.setMaximumSize(okayButton.getPreferredSize());
        okayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        JLabel jLabel = new JLabel(message);


        JPanel jPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10,10,10,10);

        jPanel.add(jLabel, gbc);

        gbc.insets.bottom = 10;
        gbc.gridy = 1;

        jPanel.add(okayButton, gbc);

        add(jPanel);

        setModalityType(ModalityType.APPLICATION_MODAL);


        setTitle(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(component);
        pack();


        setPreferredSize(getPreferredSize());
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setSize(getPreferredSize());

    }
}