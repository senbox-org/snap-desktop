/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.esa.snap.core.Product;

/**
 *
 * @author Norman
 */
@ActionID(
        category = "Tools",
        id = "org.snap.gui.AnalyseProductAction"
)
@ActionRegistration(
        displayName = "Analyse Product"
)
@ActionReference(path = "Menu/Tools", position = 0)
public class AnalyseProductAction implements ActionListener {

    Product context;

    public AnalyseProductAction(Product context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(null, "Name: " + context.getName());
    }

}
