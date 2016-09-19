/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.rcp.actions.help;

import org.esa.snap.rcp.util.Dialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author muhammad.bc.
 */
@ActionID(category = "Help", id = "ShowLogFileInExplorerAction")
@ActionRegistration(
        displayName = "#CTL_ShowLogFileInExplorerAction_MenuText")
@ActionReference(path = "Menu/Help", position = 300)
@NbBundle.Messages({
        "CTL_ShowLogFileInExplorerAction_MenuText=Show log File"
})
public class ShowLogInExplorer extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        openLogFile();
    }

    private void openLogFile() {
        String os = System.getProperty("os.name");
        Path userHomeDir = Paths.get(System.getProperty("user.home"));
        Path resolve = null;
        try {
            if (os.equals("Darwin")) {
                Dialogs.showInformation("Not yet implemented for Mac OS");
            } else if (os.startsWith("Linux") || os.startsWith("LINUX")) {
                resolve = userHomeDir.resolve("var").resolve("log");
            } else if (os.startsWith("Windows")) {
                resolve = userHomeDir.resolve("AppData").resolve("Roaming").resolve("SNAP").resolve("var").resolve("log");
            }
            if (resolve.isAbsolute()) {
                Desktop.getDesktop().open(resolve.toFile());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
