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

import com.bc.ceres.swing.figure.interactions.ZoomInteractor;
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
        id = "org.esa.snap.rcp.action.interactors.ZoomToolAction"
)
@ActionRegistration(
        displayName = "#CTL_ZoomToolActionText",
        lazy = false
)
@ActionReference(
        path = "Toolbars/" + PackageDefaults.ZOOM_TOOL_TOOLBAR_NAME,
        position = 120
)
@Messages({
                  "CTL_ZoomToolActionText=Zoom",
                  "CTL_ZoomToolActionDescription=Zooming tool"
          })
public class ZoomToolAction extends ToolAction {

    @SuppressWarnings("UnusedDeclaration")
    public ZoomToolAction() {
        this(null);
    }

    public ZoomToolAction(Lookup lookup) {
        super(lookup, new ZoomInteractor());
        putValue(NAME, Bundle.CTL_ZoomToolActionText());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_ZoomToolActionDescription());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/ZoomTool24.gif", false));
    }

    @Override
    public HelpCtx getHelpCtx() {
        // TODO: Make sure help page is available for ID
        return new HelpCtx("zoomTool");
    }
}