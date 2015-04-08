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
        preferredID = "InformationTopComponent",
        iconBase = "org/esa/snap/rcp/icons/Information24.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 1
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.statistics.InformationTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Window/Tool Windows"),
        @ActionReference(path = "Toolbars/Analysis")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_InformationTopComponent_Name",
        preferredID = "InformationTopComponent"
)
@NbBundle.Messages({
        "CTL_InformationTopComponent_Name=Information",
        "CTL_InformationTopComponent_HelpId=informationDialog"
})
/**
 * The tool view containing the product / band information
 *
 * @author Marco Zuehlke
 */
public class InformationTopComponent extends AbstractStatisticsTopComponent {

    public static final String ID = InformationTopComponent.class.getName();

    @Override
    protected PagePanel createPagePanel() {
        return new InformationPanel(this, getHelpId());
    }

    @Override
    protected String getHelpId() {
        return Bundle.CTL_InformationTopComponent_HelpId();
    }


}
