package org.esa.snap.examples.menu;

import org.esa.snap.rcp.SnapDialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import javax.swing.AbstractAction;
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
        category = "Tools",
        id = "GenerateMenuItemAction"
)
@ActionRegistration(
        displayName = "Generate Menu Item",
        menuText = "Generate Menu Item"
)
@ActionReference(path = "Menu/Tools/Examples", position = 50)
public class GenerateMenuItemAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        FileObject menuFolder = FileUtil.getConfigFile("Menu/Tools/Examples");
        try {
            FileObject itemsFolder = menuFolder.getFileObject("Generated Items");
            if (itemsFolder == null) {
                itemsFolder = menuFolder.createFolder("Generated Items");
                itemsFolder.setAttribute("position", 51);
            }
            GeneratedItemAction action = new GeneratedItemAction();
            FileObject newMenuItem = itemsFolder.createData(action.getName(), "instance");
            newMenuItem.setAttribute("instanceCreate", action);
            newMenuItem.setAttribute("instanceClass", action.getClass().getName());
        } catch (IOException e1) {
            SnapDialogs.showError("Error: " + e1.getMessage());
        }
    }

    public static class GeneratedItemAction extends AbstractAction {
        static int counter = 0;

        public GeneratedItemAction() {
            super("Generated Item " + (++counter));
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
