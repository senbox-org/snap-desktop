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
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * This action launches the default browser to display the web page explaining how to best report an issue.
 */
@ActionID(category = "Help", id = "ReportIssueAction")
@ActionRegistration(
        displayName = "#CTL_ReportIssueAction_MenuText",
        popupText = "#CTL_ReportIssueAction_MenuText"
)
@ActionReference(path = "Menu/Help", position = 305)
@NbBundle.Messages({
        "CTL_ReportIssueAction_MenuText=Report an Issue",
        "CTL_ReportIssueAction_ShortDescription=Opens a web page explaining how to report an issue"
})
public class ReportIssueAction extends AbstractAction {

    private static final String DEFAULT_REPORT_ISSUE_PAGE_URL = "https://seadas.gsfc.nasa.gov/help/issue-reporting/";

    /**
     * Launches the default browser to display the web page.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        DesktopHelper.browse(Config.instance().preferences().get("snap.reportIssuePageUrl", DEFAULT_REPORT_ISSUE_PAGE_URL));
    }
}
