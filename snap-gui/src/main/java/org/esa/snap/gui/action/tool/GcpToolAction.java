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

package org.esa.snap.gui.action.tool;

import org.esa.snap.gui.placemark.InsertGcpInteractor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

import javax.swing.Action;

@ActionID(
        category = "SNAP",
        id = "org.esa.snap.gui.action.tool.GcpToolAction"
)
@ActionRegistration(
        displayName = "not-used",
        lazy = false
)
@ActionReference(
        path = "Toolbars/Tools",
        position = 140
)
@Messages({
                  "CTL_GcpToolActionText=GCP Tool",
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
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/gui/icons/GcpTool24.gif", false));
        putValue("helpId", "gcpTool");
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new GcpToolAction(actionContext);
    }
}