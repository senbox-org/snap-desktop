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
        preferredID = "ScatterPlotTopComponent",
        iconBase = "org/esa/snap/rcp/icons/" + PackageDefaults.SCATTER_PLOT_ICON,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = PackageDefaults.SCATTER_PLOT_WS_MODE,
        openAtStartup = PackageDefaults.SCATTER_PLOT_WS_OPEN,
        position = PackageDefaults.SCATTER_PLOT_WS_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.ScatterPlotTopComponent")
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.SCATTER_PLOT_MENU_PATH,
                position = PackageDefaults.SCATTER_PLOT_MENU_POSITION),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.SCATTER_PLOT_TOOLBAR_NAME,
                position = PackageDefaults.SCATTER_PLOT_TOOLBAR_POSITION)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ScatterPlotTopComponent_Name",
        preferredID = "ScatterPlotTopComponent"
)
@NbBundle.Messages({
        "CTL_ScatterPlotTopComponent_Name=" + PackageDefaults.SCATTER_PLOT_NAME,
        "CTL_ScatterPlotTopComponent_HelpId=correlativePlotDialog"
})
/**
 * The tool view containing a scatter plot
 *
 * @author Marco Zuehlke
 */
public class ScatterPlotTopComponent extends AbstractStatisticsTopComponent {


    @Override
    protected PagePanel createPagePanel() {
        final Icon largeIcon = UIUtils.loadImageIcon("icons/seadas/CorrelativePlot24.png");
        final String chartTitle = ScatterPlotPanel.CHART_TITLE;
        final ScatterPlotPanel scatterPlotPanel = new ScatterPlotPanel(this, Bundle.CTL_ScatterPlotTopComponent_HelpId());
        final TableViewPagePanel tableViewPanel = new TableViewPagePanel(this, Bundle.CTL_ScatterPlotTopComponent_HelpId(), chartTitle, largeIcon);
        scatterPlotPanel.setAlternativeView(tableViewPanel);
        tableViewPanel.setAlternativeView(scatterPlotPanel);
        return scatterPlotPanel;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_ScatterPlotTopComponent_HelpId());
    }
}
