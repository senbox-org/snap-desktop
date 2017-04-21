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

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 * The tool view containing a density plot
 *
 */
@TopComponent.Description(
        preferredID = "MetadataPlotTopComponent",
        iconBase = "org/esa/snap/rcp/icons/MetadataPlot.png"
)
@TopComponent.Registration(
        mode = "MetadataPlot",
        openAtStartup = false,
        position = 50
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.MetadataPlotTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Analysis",position = 70),
        @ActionReference(path = "Toolbars/Analysis")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_MetadataPlotTopComponent_Name",
        preferredID = "MetadataPlotTopComponent"
)
@NbBundle.Messages({
        "CTL_MetadataPlotTopComponent_Name=Metadata Plot",
        "CTL_MetadataPlotTopComponent_HelpId=metadataPlotDialog"
})
public class MetadataPlotTopComponent extends AbstractStatisticsTopComponent {

    @Override
    protected PagePanel createPagePanel() {
        return new MetadataPlotPanel(this, Bundle.CTL_MetadataPlotTopComponent_HelpId());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_MetadataPlotTopComponent_HelpId());
    }

    @Override
    protected void componentOpened() {
        super.componentOpened();
    }
}
