/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.actions.help;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This action launches the default browser to display the online help.
 */
@ActionID(category = "Help", id = "ShowOnlineHelpAction" )
@ActionRegistration(
        displayName = "#CTL_ShowOnlineHelpAction_MenuText",
        popupText = "#CTL_ShowOnlineHelpAction_MenuText")
@ActionReference(path = "Menu/Help", position = 205)
@NbBundle.Messages({
        "CTL_ShowOnlineHelpAction_MenuText=Online Help",
        "CTL_ShowOnlineHelpAction_ShortDescription=Browse the SNAP Toolboxes online help"
})
public class ShowOnlineHelpAction extends AbstractAction {
    /** URl for the online help. */
    private static final String DEFAULT_ONLINE_HELP_URL = "https://step.esa.int/main/doc/online-help";

    /**
     * Launches the default browser to display the tutorials.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        final String url = Config.instance().preferences().get("snap.online.help.url", DEFAULT_ONLINE_HELP_URL);
        DesktopHelper.browse(url + "?version=" + SystemUtils.getReleaseVersion());
    }
}
