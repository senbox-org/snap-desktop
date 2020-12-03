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
        preferredID = "DensityPlotTopComponent",
        iconBase = "org/esa/snap/rcp/icons/" + PackageDefaults.DENSITY_PLOT_ICON,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = PackageDefaults.DENSITY_PLOT_WS_MODE,
        openAtStartup = PackageDefaults.DENSITY_PLOT_WS_OPEN,
        position = PackageDefaults.DENSITY_PLOT_WS_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.DensityPlotTopComponent")
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.DENSITY_PLOT_MENU_PATH,
                position = PackageDefaults.DENSITY_PLOT_MENU_POSITION),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.DENSITY_PLOT_TOOLBAR_NAME,
                position = PackageDefaults.DENSITY_PLOT_TOOLBAR_POSITION)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_DensityPlotTopComponent_Name",
        preferredID = "DensityPlotTopComponent"
)
@NbBundle.Messages({
        "CTL_DensityPlotTopComponent_Name=" + PackageDefaults.DENSITY_PLOT_NAME,
        "CTL_DensityPlotTopComponent_HelpId=densityPlotDialog"
})
/**
 * The tool view containing a density plot
 *
 * @author Marco Zuehlke
 */
public class DensityPlotTopComponent extends AbstractStatisticsTopComponent {

    @Override
    protected PagePanel createPagePanel() {
        return new DensityPlotPanel(this, Bundle.CTL_DensityPlotTopComponent_HelpId());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_DensityPlotTopComponent_HelpId());
    }

    @Override
    protected void componentOpened() {
        super.componentOpened();
    }
}
