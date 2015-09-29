/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.preferences.general.UiBehaviorController;
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
import java.util.prefs.Preferences;

import static org.esa.snap.rcp.actions.file.OpenProductAction.getRecentProductPaths;

/**
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "ReopenProductAction"
)
@ActionRegistration(
        displayName = "#CTL_ReopenProductActionName",
        menuText = "#CTL_ReopenProductActionMenuText",
        lazy = false
)
@ActionReference(path = "Menu/File", position = 10)
@NbBundle.Messages({
        "CTL_ReopenProductActionName=Reopen Product",
        "CTL_ReopenProductActionMenuText=Reopen Product",
        "CTL_ClearListActionMenuText=Clear List"
})
public final class ReopenProductAction extends AbstractAction implements Presenter.Toolbar, Presenter.Menu, Presenter.Popup {

    private final int DEFAULT_MAX_FILE_LIST_REOPEN = 10;

    @Override
    public JMenuItem getMenuPresenter() {

        List<File> openedFiles = OpenProductAction.getOpenedProductFiles();
        List<String> pathList = getRecentProductPaths().get();

        final Preferences preference = SnapApp.getDefault().getPreferences();
        int maxFileList = preference.getInt(UiBehaviorController.PREFERENCE_KEY_LIST_FILES_TO_REOPEN,
                DEFAULT_MAX_FILE_LIST_REOPEN);

        // Add "open recent product file" actions
        JMenu menu = new JMenu(Bundle.CTL_ReopenProductActionMenuText());


        pathList.stream().limit(maxFileList).forEach(path->{
            if (!openedFiles.contains(new File(path))) {
                JMenuItem menuItem = new JMenuItem(path);
                OpenProductAction openProductAction = new OpenProductAction();
                openProductAction.setFile(new File(path));
                menuItem.addActionListener(openProductAction);
                menu.add(menuItem);
            }
        });


        // Add "Clear List" action
        if (menu.getComponentCount() > 0 || pathList.size() > 0) {
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
