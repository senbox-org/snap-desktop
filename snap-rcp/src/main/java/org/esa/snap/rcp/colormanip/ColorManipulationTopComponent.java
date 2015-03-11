/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.colormanip;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import java.awt.*;

@TopComponent.Description(
        preferredID = "ColorManipulationTopComponent",
        iconBase = "org/esa/snap/rcp/icons/Navigation16.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = true,
        position = 1
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.colormanip.ColorManipulationTopComponent")
@ActionReference(path = "Menu/Window/Tool Windows")
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ColorManipulationTopComponent_Name",
        preferredID = "ColorManipulationTopComponent"
)
@NbBundle.Messages({
        "CTL_ColorManipulationTopComponent_Name=Colour Manipulation"
})
/**
 * The color manipulation tool window.
 */
public class ColorManipulationTopComponent extends TopComponent implements HelpCtx.Provider {

    public ColorManipulationTopComponent() {
        ColorManipulationForm cmf = new ColorManipulationForm(this, new FormModel());
        setLayout(new BorderLayout());
        add(cmf.getContentPanel(), BorderLayout.CENTER);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("showColorManipulationWnd");
    }
}