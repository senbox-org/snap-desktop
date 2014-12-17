/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.file;

import org.esa.snap.gui.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;

/**
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.gui.actions.file.ReopenProductAction"
)
@ActionRegistration(
        displayName = "#CTL_ReopenProductActionName",
        menuText = "#CTL_ReopenProductActionMenuText"
)
@ActionReference(path = "Menu/File", position = 1)
@NbBundle.Messages({
        "CTL_ReopenProductActionName=Reopen Product",
        "CTL_ReopenProductActionMenuText=Reopen Product"
})
public final class ReopenProductAction extends AbstractAction implements Presenter.Toolbar, Presenter.Menu, Presenter.Popup {

    @Override
    public JMenuItem getMenuPresenter() {
        JMenu menu = new JMenu(Bundle.CTL_ReopenProductActionMenuText());
        Preferences preferences = SnapApp.getInstance().getPreferences();
        // todo - extract last open files from preferences
        String[] paths = {"a.nc", "b.nc", "c.nc"};
        for (String path : paths) {
            JMenuItem menuItem = new JMenuItem(path);
            menuItem.addActionListener(e -> open(path));
            menu.add(menuItem);
        }
        return menu;
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return getMenuPresenter();
    }

    @Override
    public Component getToolbarPresenter() {
        return getMenuPresenter();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // do nothing
    }

    private void open(String path) {
        System.out.println("open: path = " + path);
    }
}
