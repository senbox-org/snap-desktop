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

import org.esa.snap.rcp.placemark.InsertGcpInteractor;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

import javax.swing.*;

@ActionID(
        category = "Interactors",
        id = "org.esa.snap.rcp.action.interactors.GcpToolAction"
)
@ActionRegistration(
        displayName = "#CTL_GcpToolActionText",
        lazy = false
)
@ActionReference(
        path = "Toolbars/" + PackageDefaults.GCP_TOOL_ACTION_TOOLBAR_NAME,
        position = PackageDefaults.GCP_TOOL_ACTION_TOOLBAR_POSITION
)
@Messages({
                  "CTL_GcpToolActionText=" + PackageDefaults.GCP_TOOL_ACTION_NAME,
                  "CTL_GcpToolActionDescription=GCP placing tool"
          })
public class GcpToolAction extends ToolAction {

    @SuppressWarnings("UnusedDeclaration")
    public GcpToolAction() {
        this(null);
    }

    public GcpToolAction(Lookup lookup) {
        super(lookup, new InsertGcpInteractor());
        putValue(NAME, Bundle.CTL_GcpToolActionText());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_GcpToolActionDescription());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.GCP_TOOL_ACTION_ICON, false));
    }

    @Override
    public HelpCtx getHelpCtx() {
        // TODO: Make sure help page is available for ID
        return new HelpCtx("gcpTool");
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new GcpToolAction(actionContext);
    }
}