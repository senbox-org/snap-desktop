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

import org.esa.snap.ui.UIUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.Icon;

@TopComponent.Description(
        preferredID = "ScatterPlot3DTopComponent",
        iconBase = "org/esa/snap/rcp/icons/ScatterPlot.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "3DScatterPlot",
        openAtStartup = false,
        position = 5
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.ScatterPlot3DTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Analysis",position = 15),
        @ActionReference(path = "Toolbars/Analysis")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_3DScatterPlotTopComponent_Name",
        preferredID = "ScatterPlot3DTopComponent"
)
@NbBundle.Messages({
        "CTL_3DScatterPlotTopComponent_Name=3D Scatter Plot",
        "CTL_3DScatterPlotTopComponent_HelpId=3DScatterPlotDialog"
})
/**
 * The tool view containing a 3D scatter plot
 *
 * @author Tonio Fincke
 */
public class ScatterPlot3DTopComponent extends AbstractStatisticsTopComponent {


    @Override
    protected PagePanel createPagePanel() {
        final Icon largeIcon = UIUtils.loadImageIcon("icons/ScatterPlot24.gif");
        final String chartTitle = ScatterPlotPanel.CHART_TITLE;
        final ScatterPlot3DPlotPanel scatterPlot3DPanel =
                new ScatterPlot3DPlotPanel(this, Bundle.CTL_3DScatterPlotTopComponent_HelpId());
        final TableViewPagePanel tableViewPanel = new TableViewPagePanel(this,
                Bundle.CTL_3DScatterPlotTopComponent_HelpId(), chartTitle, largeIcon);
        scatterPlot3DPanel.setAlternativeView(tableViewPanel);
        tableViewPanel.setAlternativeView(scatterPlot3DPanel);
        return scatterPlot3DPanel;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_3DScatterPlotTopComponent_HelpId());
    }
}
