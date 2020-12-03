/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.actions.interactors;

import org.esa.snap.rcp.placemark.InsertPinInteractor;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Interactors",
        id = "org.esa.snap.rcp.action.interactors.PinToolAction"
)
@ActionRegistration(
        displayName = "#CTL_PinToolActionText",
        lazy = false
)
@ActionReference(
        path = "Toolbars/" + PackageDefaults.PIN_TOOL_TOOLBAR_NAME,
        position = PackageDefaults.PIN_TOOL_TOOLBAR_POSITION
)
@Messages({
                  "CTL_PinToolActionText=" + PackageDefaults.PIN_TOOL_NAME,
                  "CTL_PinToolActionDescription=" + PackageDefaults.PIN_TOOL_DESCRIPTION
          })
public class PinToolAction extends ToolAction {

    @SuppressWarnings("UnusedDeclaration")
    public PinToolAction() {
        this(null);
    }

    public PinToolAction(Lookup lookup) {
        super(lookup, new InsertPinInteractor());
        putValue(NAME, Bundle.CTL_PinToolActionText());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_PinToolActionDescription());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.PIN_TOOL_ICON, false));
    }

    @Override
    public HelpCtx getHelpCtx() {
        // TODO: Make sure help page is available for ID
        return new HelpCtx("pinTool");
    }
}