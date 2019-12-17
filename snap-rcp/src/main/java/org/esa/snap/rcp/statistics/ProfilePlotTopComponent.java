/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.statistics;

import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.UIUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.Icon;

@TopComponent.Description(
        preferredID = "ProfilePlotTopComponent",
        iconBase = "org/esa/snap/rcp/icons/" + PackageDefaults.PROFILE_PLOT_ICON,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = PackageDefaults.PROFILE_PLOT_WS_MODE,
        openAtStartup = PackageDefaults.PROFILE_PLOT_WS_OPEN,
        position = PackageDefaults.PROFILE_PLOT_WS_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.ProfilePlotTopComponent")
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.PROFILE_PLOT_MENU_PATH,
                position = PackageDefaults.PROFILE_PLOT_MENU_POSITION),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.PROFILE_PLOT_TOOLBAR_NAME,
                position = PackageDefaults.PROFILE_PLOT_TOOLBAR_POSITION)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ProfilePlotTopComponent_Name",
        preferredID = "ProfilePlotTopComponent"
)
@NbBundle.Messages({
        "CTL_ProfilePlotTopComponent_Name=" + PackageDefaults.PROFILE_PLOT_NAME,
        "CTL_ProfilePlotTopComponent_HelpId=profilePlotDialog"
})
/**
 * The tool view containing a profile plot
 *
 * @author Marco Zuehlke
 */
public class ProfilePlotTopComponent extends AbstractStatisticsTopComponent {

    public static final String ID = ProfilePlotTopComponent.class.getName();
    public static final String tableHelpID = "tableView";

    @Override
    protected PagePanel createPagePanel() {
        final String helpId = Bundle.CTL_ProfilePlotTopComponent_HelpId();
        final String chartTitle = ProfilePlotPanel.CHART_TITLE;
        final Icon largeIcon = UIUtils.loadImageIcon("icons/ProfilePlot24.gif");
        ProfilePlotPanel profilePlotPanel = new ProfilePlotPanel(this, helpId);
        final TableViewPagePanel tableViewPagePanel = new TableViewPagePanel(this, tableHelpID, chartTitle, largeIcon);
        profilePlotPanel.setAlternativeView(tableViewPagePanel);
        tableViewPagePanel.setAlternativeView(profilePlotPanel);
        return profilePlotPanel;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_ProfilePlotTopComponent_HelpId());
    }

}
