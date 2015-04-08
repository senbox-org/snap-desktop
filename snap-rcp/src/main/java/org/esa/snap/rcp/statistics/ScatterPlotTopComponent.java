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

import org.esa.beam.framework.ui.UIUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.Icon;

@TopComponent.Description(
        preferredID = "ScatterPlotTopComponent",
        iconBase = "org/esa/snap/rcp/icons/ScatterPlot24.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 1
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.ScatterPlotTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Window/Tool Windows"),
        @ActionReference(path = "Toolbars/Analysis")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ScatterPlotTopComponent_Name",
        preferredID = "ScatterPlotTopComponent"
)
@NbBundle.Messages({
        "CTL_ScatterPlotTopComponent_Name=Correlative Plot",
        "CTL_ScatterPlotTopComponent_HelpId=correlativePlotDialog"
})
/**
 * The tool view containing a scatter plot
 *
 * @author Marco Zuehlke
 */
public class ScatterPlotTopComponent extends AbstractStatisticsTopComponent {

    public static final String ID = ScatterPlotTopComponent.class.getName();
    public static final String tableHelpID = "tableView";

    @Override
    protected PagePanel createPagePanel() {
        final String helpId = getHelpId();
        final Icon largeIcon = UIUtils.loadImageIcon("icons/ScatterPlot24.gif");
        final String chartTitle = ScatterPlotPanel.CHART_TITLE;
        final ScatterPlotPanel scatterPlotPanel = new ScatterPlotPanel(this, helpId);
        final TableViewPagePanel tableViewPanel = new TableViewPagePanel(this, tableHelpID, chartTitle, largeIcon);
        scatterPlotPanel.setAlternativeView(tableViewPanel);
        tableViewPanel.setAlternativeView(scatterPlotPanel);
        return scatterPlotPanel;
    }

    @Override
    protected String getHelpId() {
        return Bundle.CTL_ScatterPlotTopComponent_HelpId();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(getHelpId());
    }
}
