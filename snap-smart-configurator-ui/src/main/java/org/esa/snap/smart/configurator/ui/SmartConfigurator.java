/*
 * Copyright (C) 2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.smart.configurator.ui;

import org.esa.snap.core.util.SystemUtils;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Launcher for the performance optimisation out of SNAP.
 *
 * @author Nicolas Ducoin
 */
public class SmartConfigurator extends javax.swing.JFrame implements PropertyChangeListener {

    private PerformancePanel performancePanel;
    PerformanceOptionsPanelController controller;
    JButton okButton;

    /**
     * Creates new form SmartConfigurator
     */
    public SmartConfigurator() {
        initComponents();
    }



    private void initComponents() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            SystemUtils.LOG.warning("Could not set up look&feel: " + e.getMessage());
        }

        JPanel jPanel1 = new JPanel();
        okButton = new JButton();
        JButton cancelButton = new JButton();
        controller = new PerformanceOptionsPanelController();
        controller.addPropertyChangeListener(this);
        performancePanel = new PerformancePanel(controller);
        performancePanel.load();

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        org.openide.awt.Mnemonics.setLocalizedText(okButton, org.openide.util.NbBundle.getMessage(SmartConfigurator.class, "SmartConfigurator.okButton.text"));
        okButton.addActionListener(this::okButtonActionPerformed);
        jPanel1.add(okButton);

        org.openide.awt.Mnemonics.setLocalizedText(cancelButton, org.openide.util.NbBundle.getMessage(SmartConfigurator.class, "SmartConfigurator.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(this::cancelButtonActionPerformed);
        jPanel1.add(cancelButton);

        getContentPane().add(jPanel1, BorderLayout.SOUTH);
        getContentPane().add(performancePanel, java.awt.BorderLayout.CENTER);


        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        setIconImage(new ImageIcon(getClass().getResource("SNAP_icon_16.png")).getImage());
        setTitle("SNAP Performance Configuration Optimisation");

        pack();
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        performancePanel.store();
        System.exit(0);
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(performancePanel.valid()) {
            okButton.setEnabled(true);
        } else {
            okButton.setEnabled(false);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new SmartConfigurator().setVisible(true));
    }
}
