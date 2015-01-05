/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.file;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import static org.esa.snap.gui.actions.file.OpenProductAction.getRecentProductPaths;

/**
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.gui.actions.file.ReopenProductAction"
)
@ActionRegistration(
        displayName = "#CTL_ReopenProductActionName",
        menuText = "#CTL_ReopenProductActionMenuText",
        lazy = false
)
@ActionReference(path = "Menu/File", position = 20)
@NbBundle.Messages({
        "CTL_ReopenProductActionName=Reopen Product",
        "CTL_ReopenProductActionMenuText=Reopen Product",
        "CTL_ClearListActionMenuText=Clear List"
})
public final class ReopenProductAction extends AbstractAction implements Presenter.Toolbar, Presenter.Menu, Presenter.Popup {

    @Override
    public JMenuItem getMenuPresenter() {

        List<File> openedFiles = OpenProductAction.getOpenedProductFiles();
        List<String> pathList = getRecentProductPaths().get();

        // Add "open recent product file" actions
        JMenu menu = new JMenu(Bundle.CTL_ReopenProductActionMenuText());
        pathList.forEach(path -> {
            if (!openedFiles.contains(new File(path))) {
                JMenuItem menuItem = new JMenuItem(path);
                menuItem.addActionListener(e -> OpenProductAction.openProductFile(null, new File(path)));
                menu.add(menuItem);
            }
        });

        // Add "Clear List" action
        if (menu.getComponentCount() > 0) {
            menu.addSeparator();
            JMenuItem menuItem = new JMenuItem(Bundle.CTL_ClearListActionMenuText());
            menuItem.addActionListener(e -> getRecentProductPaths().clear());
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
}
