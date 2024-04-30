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
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

@TopComponent.Description(
        preferredID = "HistogramPlotTopComponent",
        iconBase = "org/esa/snap/rcp/icons/" + PackageDefaults.HISTOGRAM_PLOT_ICON,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = PackageDefaults.HISTOGRAM_PLOT_WS_MODE,
        openAtStartup = false,
        position = 40
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.HistogramPlotTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Analysis",position = 50),
        @ActionReference(path = "Toolbars/Analysis", position = 30)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_HistogramPlotTopComponent_Name",
        preferredID = "HistogramPlotTopComponent"
)
@NbBundle.Messages({
        "CTL_HistogramPlotTopComponent_Name=Histogram",
        "CTL_HistogramPlotTopComponent_HelpId=histogramDialog"
})
/**
 * The tool view containing the histogram of a band
 *
 * @author Marco Zuehlke
 */
public class HistogramPlotTopComponent extends AbstractStatisticsTopComponent {

    public static final String ID = HistogramPlotTopComponent.class.getName();

    @Override
    protected PagePanel createPagePanel() {
        return new HistogramPanel(this, Bundle.CTL_HistogramPlotTopComponent_HelpId());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_HistogramPlotTopComponent_HelpId());
    }
}
