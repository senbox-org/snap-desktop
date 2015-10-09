/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.snap.runtime.Config;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * This action launches the default browser to display the project web page.
 */
@ActionID(
        category = "Help",
        id = "ShowHomePageAction"
)
@ActionRegistration(
        displayName = "#CTL_ShowHomePageAction_MenuText",
        popupText = "#CTL_ShowHomePageAction_MenuText",
        lazy = true
)
@ActionReferences({
        @ActionReference(
                path = "Menu/Help",
                position = 300
        )
})
@NbBundle.Messages({
        "CTL_ShowHomePageAction_MenuText=SNAP Home Page",
        "CTL_ShowHomePageAction_ShortDescription=Browse the SNAP Toolboxes home page"
})
public class ShowHomePageAction extends AbstractAction {
    private static final String DEFAULT_PAGE_URL = "http://step.esa.int";

    /**
     * Launches the default browser to display the web site.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        DesktopHelper.browse(Config.instance().preferences().get("snap.homePageUrl", DEFAULT_PAGE_URL));
    }
}
