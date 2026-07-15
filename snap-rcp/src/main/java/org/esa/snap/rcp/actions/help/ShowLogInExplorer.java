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
import org.openide.modules.Places;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * @author muhammad.bc.
 */
@ActionID(category = "Help", id = "ShowLogFileInExplorerAction")
@ActionRegistration(displayName = "#CTL_ShowLogFileInExplorerAction_MenuText")
@ActionReference(path = "Menu/Help", position = 400)
@NbBundle.Messages({"CTL_ShowLogFileInExplorerAction_MenuText=Show Log Directory"})
public class ShowLogInExplorer extends AbstractAction {

    private static final Logger LOG = Logger.getLogger(ShowLogInExplorer.class.getName());

    @Override
    public void actionPerformed(ActionEvent e) {
        openLogFile();
    }

    private void openLogFile() {
        File userDir = Places.getUserDirectory();
        Path logDir = userDir != null ? userDir.toPath().resolve("var").resolve("log") : null;
        if (logDir != null && Files.exists(logDir)) {
            try {
                Desktop.getDesktop().open(logDir.toFile());
                LOG.info("Opened log directory: " + logDir);
            } catch (IOException e) {
                Dialogs.showError("Could not open log directory!");
            }
        } else {
            Dialogs.showError("Log directory does not exist: " + logDir);
        }
    }
}
