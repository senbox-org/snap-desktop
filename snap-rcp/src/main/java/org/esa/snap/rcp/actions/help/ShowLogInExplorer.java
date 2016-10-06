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

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author muhammad.bc.
 */
@ActionID(category = "Help", id = "ShowLogFileInExplorerAction")
@ActionRegistration(displayName = "#CTL_ShowLogFileInExplorerAction_MenuText")
@ActionReference(path = "Menu/Help", position = 300)
@NbBundle.Messages({"CTL_ShowLogFileInExplorerAction_MenuText=Show Log Directory"})
public class ShowLogInExplorer extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        openLogFile();
    }

    private void openLogFile() {
        String os = System.getProperty("os.name").toLowerCase();
        Path userHomeDir = SystemUtils.getUserHomeDir().toPath();
        Path logDir = null;
        if (isLinuxOrMac(os)) {
            logDir = userHomeDir.resolve(".snap/system/var/log");
        } else if (os.startsWith("windows")) {
            logDir = userHomeDir.resolve("AppData/Roaming/SNAP/var/log");
        }
        if (logDir != null && Files.exists(logDir)) {
            try {
                Desktop.getDesktop().open(logDir.toFile());
            } catch (IOException e) {
                Dialogs.showError("Could not open log directory!");
            }
        }
    }

    private boolean isLinuxOrMac(String os) {
        return os.startsWith("darwin") || os.startsWith("mac") || os.startsWith("linux");
    }
}
