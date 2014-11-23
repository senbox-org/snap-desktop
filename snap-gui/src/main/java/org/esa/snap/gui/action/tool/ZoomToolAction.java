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

import com.bc.ceres.swing.figure.interactions.ZoomInteractor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "SNAP",
        id = "org.esa.snap.gui.action.tool.ZoomToolAction"
)
@ActionRegistration(
        displayName = "not-used",
        lazy = false
)
@ActionReference(
        path = "Toolbars/Tools",
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
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/gui/icons/ZoomTool24.gif", false));
        putValue("helpId", "zoomTool");
        putValue("context", "image");
    }

}