/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.MissingResourceException;

/**
 * Displays the {@link AboutPanel} in a modal dialog.
 *
 * @author Norman Fomferra
 */
@ActionID(category = "Help", id = "org.esa.snap.rcp.about.AboutAction")
//@ActionRegistration(displayName = "#CTL_AboutAction_Name" )
@ActionReference(path = "Menu/Help", position = 1600, separatorBefore = 1550)
//@Messages({
//        "CTL_AboutAction_Name=About SNAP...",
////        "CTL_AboutAction_Title=About SNAP",
//})
public final class AboutAction implements ActionListener {
    public static AboutAction create(Map<String, Object> configuration) {
        AboutAction aboutAction = new AboutAction();
        return aboutAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String aboutTitle;
        try {
            aboutTitle = NbBundle.getBundle("org.netbeans.core.ui.Bundle").getString("CTL_AboutAction_Title");
        } catch (MissingResourceException e1) {
            aboutTitle = "About " + SnapApp.getDefault().getInstanceName();
        }

        JDialog dialog = new JDialog(WindowManager.getDefault().getMainWindow(), aboutTitle, true);
        dialog.setContentPane(new AboutPanel());
        dialog.pack();
        dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

}
