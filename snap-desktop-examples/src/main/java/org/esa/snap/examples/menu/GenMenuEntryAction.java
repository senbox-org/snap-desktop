package org.esa.snap.examples.menu;

import org.esa.snap.rcp.SnapDialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.windows.WindowManager;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * Demonstrates how to
 * <a href="https://blogs.oracle.com/geertjan/entry/dynamically_creating_menu_items_part">dynamically create menu items</a>.
 *
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "GenMenuEntryAction"
)
@ActionRegistration(
        displayName = "Generate Menu Entry",
        menuText = "Generate Menu Entry"
)
@ActionReference(path = "Menu/Tools/Examples", position = 20)
public class GenMenuEntryAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        FileObject menuFolder = FileUtil.getConfigFile("Menu/Tools/Examples/Bibos");
        try {
            BiboAction action = new BiboAction();
            FileObject newMenuItem = menuFolder.createData(action.getName(), "instance");
            newMenuItem.setAttribute("instanceCreate", action);
            newMenuItem.setAttribute("instanceClass", action.getClass().getName());
            updateMenuUI();
        } catch (IOException e1) {
            SnapDialogs.showError("Error: " + e1.getMessage());
        }
    }

    private void updateMenuUI() {
        Frame mainWindow = WindowManager.getDefault().getMainWindow();
        if (mainWindow instanceof JFrame) {
            JFrame mainFrame = (JFrame) mainWindow;
            JMenuBar menuBar = mainFrame.getJMenuBar();
            int menuCount = menuBar.getMenuCount();
            for (int i = 0; i < menuCount; i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu != null) {
                    SwingUtilities.updateComponentTreeUI(menu);
                }
            }
        }
    }

    public static class BiboAction extends AbstractAction {
        static int counter = 0;

        public BiboAction() {
            super("Bibo " + (++counter));
        }

        public String getName() {
            return (String) getValue(NAME);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SnapDialogs.showInformation("Hello, I am " + getName(), null);
        }

    }
}
