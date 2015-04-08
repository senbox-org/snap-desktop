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
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

@TopComponent.Description(
        preferredID = "GeoCodingTopComponent",
        iconBase = "org/esa/snap/rcp/icons/PhiLam24.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 1
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.GeoCodingTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Window/Tool Windows"),
        @ActionReference(path = "Toolbars/Analysis")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_GeoCodingTopComponent_Name",
        preferredID = "GeoCodingTopComponent"
)
@NbBundle.Messages({
        "CTL_GeoCodingTopComponent_Name=Geo-Coding",
        "CTL_GeoCodingTopComponent_HelpId=geoCodingInfoDialog"
})
/**
 * The tool view containing geo-coding information
 *
 * @author Marco Zuehlke
 */
public class GeoCodingTopComponent extends AbstractStatisticsTopComponent {

    public static final String ID = GeoCodingTopComponent.class.getName();

    @Override
    protected PagePanel createPagePanel() {
        return new GeoCodingPanel(this, getHelpId());
    }

    @Override
    protected String getHelpId() {
        return Bundle.CTL_GeoCodingTopComponent_HelpId();
    }


}