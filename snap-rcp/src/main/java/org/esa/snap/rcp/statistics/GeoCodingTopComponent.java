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
        preferredID = "GeoCodingTopComponent",
        iconBase = "org/esa/snap/rcp/icons/" + PackageDefaults.GEO_CODING_ICON,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = "GeoCodingMode",
        openAtStartup = false,
        position = 30
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.GeoCodingTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Analysis",position = 40),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.GEO_CODING_TOOLBAR_NAME,
                position = PackageDefaults.GEO_CODING_TOOLBAR_POSITION
        )
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_GeoCodingTopComponent_Name",
        preferredID = "GeoCodingTopComponent"
)
@NbBundle.Messages({
        "CTL_GeoCodingTopComponent_Name=" + PackageDefaults.GEO_CODING_NAME,
        "CTL_GeoCodingTopComponent_HelpId=geoCodingInfoDialog"
})
/**
 * The tool view containing geo-coding information
 *
 * @author Marco Zuehlke
 */
public class GeoCodingTopComponent extends AbstractStatisticsTopComponent {

    @Override
    protected PagePanel createPagePanel() {
        return new GeoCodingPanel(this, Bundle.CTL_GeoCodingTopComponent_HelpId());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_GeoCodingTopComponent_HelpId());
    }


}